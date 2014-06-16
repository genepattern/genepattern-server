package org.genepattern.server.executor.drm;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.drm.JobRunner;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.executor.CommandExecutor2;
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.executor.CommandProperties;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.JobStatus;


/**
 * A generic CommandExecutor which uses the newer JobRunner API for submitting jobs to a queuing systems.
 * 
 * To integrate a queuing system (aka job runner aka distributed resource manager (drm)) into GP, create a new java class 
 * which implements the JobRunner interface. 
 * 
 * This executor will take care of the mapping between GenePattern job id and the job id on a specific queuing system 
 * (such as LSF, SGE, or PBS). It saves the state of the lookup table (to a DB or the file system). 
 * It also polls for job status and calls back to the GP server on job completion. 
 * 
 * 
 * @author pcarr
 *
 */
public class JobExecutor implements CommandExecutor2 {
    private static final Logger log = Logger.getLogger(JobExecutor.class);
    
    /**
     * The fully qualified classname of a class which implements the JobRunner interface.
     * E.g.
     * <pre>
           jobRunnerClassname: org.genepattern.drm.impl.local.LocalJobRunner
     * </pre>
     */
    public static final String PROP_JOB_RUNNER_CLASSNAME="jobRunnerClassname";
    /**
     * An optional unique name for this executor, so that it can be referred to in the config_yaml file.
     * E.g.
     * <pre>
           jobRunnerName: LocalJobRunner
     * </pre>
     */
    public static final String PROP_JOB_RUNNER_NAME="jobRunnerName";
    
    /**
     * The amount of time in milliseconds to wait in between subsequent calls to check the status for particular running job.
     * The poller will call JobRunner#getStatus for each open job in order.
     */
    public static final String PROP_FIXED_DELAY="fixedDelay";
    
    /**
     * For debugging, optionally set the persistence option for the job status records.
     * By default it saved to the GP database. It can optionally be set to an in-memory hash.
     * <pre>
           lookupType: DB
           # lookupType: HASHMAP
     * </pre>
     * 
     * @see DrmLookupFactory.Type
     */
    public static final String PROP_LOOKUP_TYPE="lookupType";
    
    private String jobRunnerClassname;
    private String jobRunnerName;
    private JobRunner jobRunner;
    private DrmLookup jobLookupTable;
    
    private long fixedDelay=2000L;

    private static final int BOUND = 100000;
    private BlockingQueue<DrmJobRecord> runningJobs;
    private Thread jobHandlerThread;
    
    // wait for a fixed delay before putting a job back on the status checking queue
    private ScheduledExecutorService svcDelay=Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(final Runnable r) {
            final Thread t=new Thread(r);
            t.setDaemon(true);
            t.setName("JobExecutor-0");
            return t;
        }
    });

    // call to JobRunner.getStatus in a separate thread so that it can be killed after a timeout period
    private ScheduledExecutorService jobStatusService=Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(final Runnable r) {
            final Thread t=new Thread(r);
            t.setDaemon(true);
            t.setName("JobExecutor-1");
            return t;
        }
    });

    // call to JobRunner.cancelJob in a separate thread
    private ScheduledExecutorService jobCancellationService=Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(final Runnable r) {
            final Thread t=new Thread(r);
            t.setDaemon(true);
            t.setName("JobRunner-cancel");
            return t;
        }
    });

    /**
     * Load JobRunner from classname.
     * @param classname
     * @return
     */
    private static JobRunner initJobRunner(final String classname) { 
        log.debug("initializing JobRunner from classname="+classname);
        try {
            final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            final Class<?> svcClass = Class.forName(classname, false, classLoader);
            if (!JobRunner.class.isAssignableFrom(svcClass)) {
                log.error(""+svcClass.getCanonicalName()+" does not implement "+JobRunner.class.getCanonicalName());
            }
            final JobRunner jobRunner = (JobRunner) svcClass.newInstance();
            return jobRunner;
        }
        catch (Throwable t) {
            log.error("Error loading JobRunner for classname: "+classname+", "+t.getLocalizedMessage(), t);
            return new JobRunner() {

                @Override
                public void stop() {
                }

                @Override
                public String startJob(final DrmJobSubmission drmJobSubmission) throws CommandExecutorException {
                    throw new CommandExecutorException("Server configuration error: the jobRunner was not initialized from classname="+classname);
                }

                @Override
                public DrmJobStatus getStatus(DrmJobRecord drmJobRecord) {
                    return null;
                }

                @Override
                public boolean cancelJob(final DrmJobRecord drmJobRecord) throws Exception {
                    throw new Exception("Server configuration error: the jobRunner was not initialized from classname="+classname);
                }
            };
        }
    }
    
    private void handleCompletedJob(final DrmJobRecord jobRecord, final DrmJobStatus jobStatus) {
        log.debug("handleCompletedJob, gpJobNo="+jobRecord.getGpJobNo()+",extJobId="+jobRecord.getExtJobId()+", jobStatus="+jobStatus);
        final Integer gpJobNo=jobRecord.getGpJobNo();
        try {
            final int exitCode;
            if (jobStatus.getExitCode()==null) {
                exitCode=-1;
            }
            else {
                exitCode=jobStatus.getExitCode();
            }
            String errorMessage=null;
            if (exitCode != 0) {
                errorMessage=jobStatus.getJobStatusMessage();
            }
            if (errorMessage != null) {
                GenePatternAnalysisTask.handleJobCompletion(gpJobNo, exitCode, errorMessage);
            }
            else {
                GenePatternAnalysisTask.handleJobCompletion(gpJobNo, exitCode);
            }
        }
        catch (NumberFormatException e) {
            log.error("Unexpected error getting gp job number as an integer, gpJobId="+gpJobNo, e);
        }
        catch (Throwable t) {
            log.error("Unexpected error in handleCompletedJob for drmJobId="+jobStatus.getDrmJobId()+", gpJobId="+gpJobNo, t);
        }
    }
    
    private static final long SEC=1000L; 
    private static final long MINUTE=60L*SEC;
    
    /**
     * For a running job, get the number of milliseconds to sleep before adding the job back 
     * to the runningJobs queue.
     * 
     * @return the number of milliseconds to delay
     */
    private long getDelay(final DrmJobRecord drmJobRecord, final DrmJobStatus drmJobStatus) {
        return fixedDelay;
    }
    
    // more intelligent delay
    private long getDelay_proposed(final DrmJobRecord drmJobRecord, final DrmJobStatus drmJobStatus) {
        if (drmJobStatus==null) {
            log.error("drmJobStatus==null, returning hard-coded value");
            return 1000L;
        }
        final Date now=new Date();
        final Date submitTime=drmJobStatus.getSubmitTime();
        final Date startTime=drmJobStatus.getStartTime();
        long delta;
        if (startTime != null) {
            delta=now.getTime()-startTime.getTime();
        }
        else if (submitTime != null) {
            delta=now.getTime()-submitTime.getTime();
        }
        else {
            delta=SEC;
        }
        
        //hard-coded rule as a function of how long the job has been 'pending' or 'running'
        if (delta < MINUTE) {
            return SEC;
        }
        else if (delta < 2L*MINUTE) {
            return 2L * SEC;
        }
        else if (delta < 5L*MINUTE) {
            return 10L * SEC;
        }
        else if (delta < 10L*MINUTE) {
            return 30L * SEC;
        }
        return 60L * SEC;
    }
    
    @Override
    public void setConfigurationFilename(final String filename) {
        log.error("Ignoring setConfigurationFilename, filename="+filename);
    }
    
    @Override
    public void setConfigurationProperties(final CommandProperties properties) {
        log.debug("setting configuration properties ...");
        this.jobRunnerClassname=properties.getProperty(PROP_JOB_RUNNER_CLASSNAME);
        if (jobRunnerClassname==null || jobRunnerClassname.length()==0) {
            throw new IllegalArgumentException("jobRunnerClassname is not set!");
        }
        this.jobRunnerName=properties.getProperty(PROP_JOB_RUNNER_NAME, "0");
        String lookupTypeStr=properties.getProperty(PROP_LOOKUP_TYPE, DrmLookupFactory.Type.DB.name());
        DrmLookupFactory.Type lookupType=null;
        try {
            lookupType=DrmLookupFactory.Type.valueOf(lookupTypeStr);
        }
        catch (Throwable t) {
            log.error("Error initializing value from config file, "+PROP_LOOKUP_TYPE+": "+lookupTypeStr, t);
        }

        String fixedDelayStr=properties.getProperty(PROP_FIXED_DELAY);
        if (fixedDelayStr != null && fixedDelayStr.trim().length()>0) {
            if (log.isDebugEnabled()) {
                log.debug(PROP_FIXED_DELAY+"="+fixedDelay);
            }
            try {
                this.fixedDelay=Long.parseLong(fixedDelayStr);
            }
            catch (Throwable t) {
                log.error("Error initializing value from config file, "+PROP_FIXED_DELAY+": "+fixedDelayStr, t);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug(PROP_JOB_RUNNER_CLASSNAME+"="+jobRunnerClassname);
            log.debug(PROP_JOB_RUNNER_NAME+"="+jobRunnerName);
            log.debug(PROP_LOOKUP_TYPE+"="+lookupType);
            log.debug(PROP_FIXED_DELAY+"="+fixedDelay);
        }

        this.jobRunner=JobExecutor.initJobRunner(jobRunnerClassname);
        this.jobLookupTable=DrmLookupFactory.initializeDrmLookup(lookupType, jobRunnerClassname, jobRunnerName);
        log.info("Initialized jobRunner from classname="+jobRunnerClassname+", jobRunnerName="+jobRunnerName);
    }

    @Override
    public void start() {
        log.info("starting job executor: "+jobRunnerName+" ( "+jobRunnerName+" ) ...");
        runningJobs=new LinkedBlockingQueue<DrmJobRecord>(BOUND);
        initJobHandler();
        initJobsOnStartup(runningJobs);
    }

    private void initJobsOnStartup(final BlockingQueue<DrmJobRecord> toQueue) {
        log.info("initializing jobs on startup: "+jobRunnerName+" ( "+jobRunnerName+" ) ...");
        final List<DrmJobRecord> jobs=jobLookupTable.getRunningDrmJobRecords();
        log.info("found "+jobs.size()+ " running job(s)");
        for(final DrmJobRecord drmJobId : jobs) {
            log.debug("adding drmJobId="+drmJobId+" to list of running jobs");
            boolean success=runningJobs.offer(drmJobId);
            if (!success) {
                log.error("Failed to add drmJobId to runningJobs, drmJobId="+drmJobId+". The queue is at capacity");
            }
        }
    }
    
    private void updateStatus(final DrmJobRecord drmJobRecord, final DrmJobStatus drmJobStatus) {
        if (drmJobStatus==null) {
            //ignore
            log.debug("drmJobStatus==null");
            return;
        }
        if (drmJobRecord==null) {
            //ignore
            log.debug("drmJobRecord==null");
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("recording status for gpJobId="+drmJobRecord.getGpJobNo()+
                    ", drmJobStatus="+drmJobStatus);
        }
        
        //record this to the lookup table
        jobLookupTable.updateJobStatus(drmJobRecord, drmJobStatus);
    }

    private DrmJobStatus getJobStatus(final DrmJobRecord drmJobRecord) throws InterruptedException {
        return getJobStatus(drmJobRecord, 60000L); //60 seconds
    }

    private DrmJobStatus getJobStatus(final DrmJobRecord drmJobRecord, final long getJobStatus_delay_ms) throws InterruptedException {
        Future<DrmJobStatus> f=jobStatusService.submit(new Callable<DrmJobStatus>() {
            @Override
            public DrmJobStatus call() throws Exception {
                final DrmJobStatus drmJobStatus=jobRunner.getStatus(drmJobRecord);
                return drmJobStatus;
            }
        });
        try {
            return f.get(getJobStatus_delay_ms, TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException e) {
            if (log.isDebugEnabled()) { 
                log.debug("checkStatus(gpJobNo="+drmJobRecord.getGpJobNo()+") timed out after "+getJobStatus_delay_ms+" "+TimeUnit.MILLISECONDS);
            }
            //report it as running so that we try again
            return new DrmJobStatus.Builder(drmJobRecord.getExtJobId(), DrmJobState.RUNNING).build();
        }
        catch (ExecutionException e) {
            final String jobStatusMessage="ExecutionException in checkStatus(gpJobNo="+drmJobRecord.getGpJobNo()+")";
            log.error(jobStatusMessage, e);
            return new DrmJobStatus.Builder(drmJobRecord.getExtJobId(), DrmJobState.UNDETERMINED)
                .jobStatusMessage(jobStatusMessage+": "+e.getLocalizedMessage())
                .build();
        }
    }

    private void checkJobStatus(final DrmJobRecord drmJobRecord) throws InterruptedException {
        //check it's status
        final DrmJobStatus drmJobStatus=getJobStatus(drmJobRecord);
        updateStatus(drmJobRecord, drmJobStatus);
        if (drmJobStatus != null && drmJobStatus.getJobState().is(DrmJobState.TERMINATED)) {
            log.debug("job is terminated, drmJobStatus.jobState="+drmJobStatus.getJobState());
            handleCompletedJob(drmJobRecord, drmJobStatus);
        }
        else if (drmJobStatus != null && drmJobStatus.getJobState().is(DrmJobState.UNDETERMINED)) {
            log.debug("unexpected result from jobRunner.getStatus, jobState="+drmJobStatus.getJobState());
            handleCompletedJob(drmJobRecord, drmJobStatus);
        }
        else {
            final long delay;
            if (drmJobStatus==null) {
                log.error("unexpected result from jobRunner.getStatus, drmJobStatus=null, retry in 30 minutes, gpJobNo="+drmJobRecord.getGpJobNo());
                delay=30L*60L*1000L;
            }
            else {
                //put it back onto the queue
                delay=getDelay(drmJobRecord, drmJobStatus);
            }
            svcDelay.schedule(new Runnable() {
                @Override
                public void run() {
                    try {
                        runningJobs.put(drmJobRecord);
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            },
            delay,
            TimeUnit.MILLISECONDS);
        }
    }

    private void initJobHandler() {
        jobHandlerThread=new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    while(true) {
                        //take the next job from the queue
                        final DrmJobRecord drmJobRecord=runningJobs.take();
                        //check it's status
                        checkJobStatus(drmJobRecord);
                    }
                }
                catch (InterruptedException e) {
                    log.info("job poller interupted, shutting down job poller");
                    Thread.currentThread().interrupt();
                }
                catch (Throwable t) {
                    log.error("Unexpected error polling for job status, shutting down job poller", t);
                }
                jobRunner.stop();
            }
        });
        jobHandlerThread.setDaemon(true);
        jobHandlerThread.start();
    }
    
    @Override
    public void stop() {
        log.info("stopping job executor: "+jobRunnerName+" ( "+jobRunnerName+" ) ...");
        if (jobHandlerThread!=null) {
            jobHandlerThread.interrupt();
        }
        if (jobRunner != null) {
            jobRunner.stop();
        }
        jobStatusService.shutdownNow();
        svcDelay.shutdownNow();
        jobCancellationService.shutdownNow();
        log.info("stopped job executor: "+jobRunnerName+" ( "+jobRunnerName+" )");
    }
    
    public static final boolean isSet(final String str) {
        return str!=null && str.length()>0;
    }
    
    @Override
    public void runCommand(final String[] commandLine, final Map<String, String> environmentVariables, final File runDir, final File stdoutFile, final File stderrFile, final JobInfo jobInfo, final File stdinFile) throws CommandExecutorException {
        GpContext jobContext=GpContext.getContextForJob(jobInfo);
        runCommand(jobContext, commandLine, environmentVariables, runDir, stdoutFile, stderrFile, stdinFile);
    }

    @Override
    public void runCommand(final GpContext jobContext, String[] commandLine, Map<String, String> environmentVariables, File runDir, File stdoutFile, File stderrFile, File stdinFile) throws CommandExecutorException {
        if (jobContext==null) {
            throw new IllegalArgumentException("jobContext==null");
        }
        if (jobContext.getJobInfo()==null) {
            throw new IllegalArgumentException("jobContext.jobInfo==null");
        }
        final Integer gpJobNo=jobContext.getJobNumber();
        log.debug(jobRunnerName+" runCommand, gpJobNo="+gpJobNo);
        
        final String logFilename=ServerConfigurationFactory.instance().getGPProperty(jobContext, JobRunner.PROP_LOGFILE);
        DrmJobSubmission.Builder builder=new DrmJobSubmission.Builder(runDir)
            .jobContext(jobContext)
            .commandLine(commandLine)
            .environmentVariables(environmentVariables)
            .stdoutFile(stdoutFile)
            .stderrFile(stderrFile)
            .stdinFile(stdinFile)
            .logFilename(logFilename);
        final DrmJobSubmission drmJobSubmission=builder.build();
        
        jobLookupTable.insertJobRecord(drmJobSubmission);
        //TODO: make fault tolerant in the event that (1) startJob gets hung or (2) startJob throws an exception
        final String extJobId=jobRunner.startJob(drmJobSubmission);
        final DrmJobRecord drmJobRecord = new DrmJobRecord.Builder(extJobId, drmJobSubmission)
            .build();
        if (!isSet(extJobId)) {
            final DrmJobStatus drmJobStatus = new DrmJobStatus.Builder(extJobId, DrmJobState.FAILED).build();
            jobLookupTable.updateJobStatus(drmJobRecord, drmJobStatus);
            throw new CommandExecutorException("invalid drmJobId returned from startJob, gpJobId="+gpJobNo);
        }
        else {
            jobLookupTable.updateJobStatus(drmJobRecord, new DrmJobStatus.Builder(extJobId, DrmJobState.QUEUED).build());
            try {
                runningJobs.put(drmJobRecord);
            }
            catch (InterruptedException e) {
                //we were cancelled while trying to add another job to the runtime queue
                log.error("Interrupted, extJobId="+extJobId);
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Terminate the job from a new thread ... wait for 5 seconds to give the job a chance to 
     * cancel (via JobRunner callback) ... otherwise make a direct call to GPAT.handleJobCompletion
     * so that the UI indicates that the task was cancelled.
     * 
     * @param drmJobRecord
     * @throws InterruptedException
     */
    @Override
    public void terminateJob(final JobInfo jobInfo) {
        if (jobInfo==null) {
            throw new IllegalArgumentException("jobInfo==null");
        }
        int gpJobNo=jobInfo.getJobNumber();
        log.debug(jobRunnerName+" terminateJob, gpJobNo="+gpJobNo);
        final DrmJobRecord drmJobRecord=jobLookupTable.lookupJobRecord(gpJobNo);
        boolean cancelled=doCancel(drmJobRecord);
        if (log.isDebugEnabled()) {
            log.debug("cancelled, gpJobNo="+gpJobNo+": "+cancelled);
        }
    }
    
    private boolean doCancel(final DrmJobRecord drmJobRecord) {
        try {
            boolean cancelled=cancelJobInThread(drmJobRecord);
            //Thread.sleep(2000); // brief hard-coded delay to allow for the jobrunner to properly cancel the job
            //check the status of the job
            DrmJobStatus cancelledStatus = getJobStatus(drmJobRecord, 5000L); // 5 seconds
            if (!cancelledStatus.getJobState().is(DrmJobState.TERMINATED)) {
                cancelledStatus = new DrmJobStatus.Builder(cancelledStatus)
                     .jobStatusMessage("Cancellation requested from GenePattern user")
                .build();
            }
            updateStatus(drmJobRecord, cancelledStatus);
            // send callback to GPAT.handleJobCompletion
            handleCompletedJob(drmJobRecord, cancelledStatus);
            return cancelled;
        }
        catch (InterruptedException e) {
            //one of these methods was interrupted
            Thread.currentThread().interrupt();
        }
        return false;
    }

    private Boolean cancelJobInThread(final DrmJobRecord drmJobRecord) throws InterruptedException {
        if (drmJobRecord==null) {
            throw new IllegalArgumentException("drmJobRecord==null");
        }
        int gpJobNo=drmJobRecord.getGpJobNo();
        log.debug(jobRunnerName+" terminateJob, gpJobNo="+gpJobNo);
        
        Boolean cancelled=null;
        Future<Boolean> f = jobCancellationService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                boolean cancelled=jobRunner.cancelJob(drmJobRecord);
                return cancelled;
            }
        });
        
        try {
            //hard-coded ... wait at most 5 seconds to cancel the job before falling back to manually updating the job status record
            cancelled=f.get(5, TimeUnit.SECONDS);
        }
        catch (ExecutionException e) {
            log.error("Error cancelling job="+gpJobNo, e);
        }
        catch (TimeoutException e) {
            log.debug("Timeout while cancelling job="+gpJobNo, e);
        }

        log.debug("jobRunner.cancelJob returned "+cancelled);
        return cancelled;
    }

    /**
     * Don't do anything in response to this call. This class will check for previously started jobs.
     */
    @Override
    public int handleRunningJob(JobInfo jobInfo) throws Exception {
        final DrmJobRecord drmJobRecord=jobLookupTable.lookupJobRecord(jobInfo.getJobNumber());
        if (drmJobRecord==null || drmJobRecord.getExtJobId()==null) {
            //no match found, what to do?
            log.error("No matching drmJobId found for gpJobId="+jobInfo.getJobNumber());
            return JobStatus.JOB_ERROR;
        }
        if (log.isDebugEnabled()) {
            log.debug(jobRunnerName+" handleRunningJob, gpJobNo="+jobInfo.getJobNumber()+", extJobId="+drmJobRecord.getExtJobId());
        }
        final DrmJobStatus drmJobStatus=jobRunner.getStatus(drmJobRecord);
        
        if (drmJobStatus==null) {
            log.error("No matching drmJobStatus for gpJobId="+jobInfo.getJobNumber()+", extJobId="+drmJobRecord.getExtJobId());
            return JobStatus.JOB_ERROR;
        }
        // an rval < 0 indicates to the calling method to ignore this
        if (drmJobStatus.getJobState().is(DrmJobState.TERMINATED)) {
            handleCompletedJob(drmJobRecord, drmJobStatus);
            return -1;
        }
        //it's still processing
        return -1;
    }

}
