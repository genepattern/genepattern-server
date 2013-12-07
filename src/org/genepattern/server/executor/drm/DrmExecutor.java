package org.genepattern.server.executor.drm;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.JobRunner;
import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.server.executor.CommandExecutor;
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.executor.CommandProperties;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.JobStatus;


/**
 * Generic implementation of the CommandExecutor interface which makes it easier to integrate a queuing system 
 * into GenePattern without the need to fully implement the CommandExecutor API.
 * 
 * To integrate a queuing system (aka job runner aka distributed resource manager (drm)) into GP, create a new java class 
 * which implements the QueuingSystem interface. 
 * 
 * This DrmExecutor class will take care of the rest. This executor takes care of the mapping between GenePattern job id 
 * and the job id on a specific queuing system (such as LSF, SGE, or PBS). It saves the state of the lookup table (to a DB or the file system). 
 * It also polls for job status and calls back to the GP server on job completion. 
 * 
 * 
 * @author pcarr
 *
 */
public class DrmExecutor implements CommandExecutor {
    private static final Logger log = Logger.getLogger(DrmExecutor.class);
    
    private String jobRunnerClassname;
    private String jobRunnerName;
    /** 
     * optional param, when set it is the name of a log file (e.g. '.lsf.out') for saving meta data about the completed job.
     *  For the LSF executor the outpout from the bsub command is streamed from stdout into this file.
     */
    private String logFilename;
    private JobRunner jobRunner;
    private DrmLookup jobLookupTable;

    private static final int BOUND = 20000;
    private BlockingQueue<String> runningJobs;
    private Thread jobHandlerThread;
    
    private ScheduledExecutorService svc=Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(final Runnable r) {
            final Thread t=new Thread(r);
            t.setDaemon(true);
            t.setName("DrmExecutor-0");
            return t;
        }
    });

    /**
     * Load JobRunner from classname.
     * @param classname
     * @return
     */
    private static JobRunner initJobRunner(final String classname) {        
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
            //TODO: refactor into abstract job runner
            return new JobRunner() {

                @Override
                public void stop() {
                }

                @Override
                public String startJob(DrmJobSubmission drmJobSubmit) throws CommandExecutorException {
                    throw new CommandExecutorException("Server configuration error: the jobRunner was not initialized from classname="+classname);
                }

                @Override
                public DrmJobStatus getStatus(String drmJobId) {
                    return null;
                }

                @Override
                public void cancelJob(String drmJobId, JobInfo jobInfo) throws Exception {
                    throw new Exception("Server configuration error: the jobRunner was not initialized from classname="+classname);
                }
            };
        }
    }
    
    private void handleCompletedJob(final DrmJobStatus jobStatus) {
        final Integer gpJobNo=jobLookupTable.lookupGpJobNo(jobStatus.getDrmJobId());
        try {
            final int exitCode;
            if (jobStatus.getExitCode()==null) {
                exitCode=-1;
            }
            else {
                exitCode=jobStatus.getExitCode();
            }
            GenePatternAnalysisTask.handleJobCompletion(gpJobNo, exitCode);
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
     * For a running job, get the amount of time to wait before putting it back
     * onto the runningJobs queue.
     * 
     * We should wait longer for long running jobs. See the incrementSleep method in GPClient.java for an example
     * 
     * 
     * @return the number of milliseconds to sleep before adding the job back to the runningJobsQueue
     */
    private long getDelay(final String drmJobId, final DrmJobStatus drmJobStatus) {
        return 1000L;
    }
    
    // more intelligent delay
    private long getDelay_proposed(final String drmJobId, final DrmJobStatus drmJobStatus) {
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
            delta=now.getTime()-startTime.getTime();
        }
        else {
            //TODO: save the timestamp of the last time we called getStatus for this particular job
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
        this.jobRunnerClassname=properties.getProperty("jobRunnerClassname");
        if (jobRunnerClassname==null || jobRunnerClassname.length()==0) {
            throw new IllegalArgumentException("jobRunnerClassname is not set!");
        }
        this.jobRunnerName=properties.getProperty("jobRunnerName", "0");
        this.logFilename=properties.getProperty("logFilename", null);
        this.jobRunner=DrmExecutor.initJobRunner(jobRunnerClassname);
        String lookupTypeStr=properties.getProperty("lookupType", DrmLookupFactory.Type.DB.name());
        DrmLookupFactory.Type lookupType=null;
        try {
            lookupType=DrmLookupFactory.Type.valueOf(lookupTypeStr);
        }
        catch (Throwable t) {
            log.error("Error initializing lookupType from config file, lookupType="+lookupTypeStr, t);
        }
        this.jobLookupTable=DrmLookupFactory.initializeDrmLookup(lookupType, jobRunnerClassname, jobRunnerName);
    }

    @Override
    public void start() {
        runningJobs=new LinkedBlockingQueue<String>(BOUND);
        initJobHandler();
        initJobsOnStartup(runningJobs);
    }

    //should call this in a new thread because it can 
    private void initJobsOnStartup(final BlockingQueue<String> toQueue) {
        final List<String> jobs=jobLookupTable.getRunningDrmJobIds();
        for(final String drmJobId : jobs) {
            boolean success=runningJobs.offer(drmJobId);
            if (!success) {
                log.error("Failed to add drmJobId to runningJobs, drmJobId="+drmJobId+". The queue is at capacity");
            }
        }
    }
    
    private void updateStatus(final DrmJobStatus drmJobStatus) {
        if (drmJobStatus==null) {
            //ignore
            log.debug("drmJobStatus==null");
            return;
        }
        
        final Integer gpJobNo=jobLookupTable.lookupGpJobNo(drmJobStatus.getDrmJobId());
        if (log.isDebugEnabled()) {
            log.debug("recording status for gpJobId="+gpJobNo+
                    ", drmJobId="+drmJobStatus.getDrmJobId()+
                    ", exitCode="+drmJobStatus.getExitCode()+
                    ", jobState="+drmJobStatus.getJobState()+
                    ", jobStatusMessage="+drmJobStatus.getJobStatusMessage());
        }
        
        //record this to the DB (aka lookup table)
        jobLookupTable.updateJobStatus(gpJobNo, drmJobStatus);
    }
    
    private void initJobHandler() {
        jobHandlerThread=new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    while(true) {
                        //take the next job from the queue
                        final String drmJobId=runningJobs.take();
                        //check it's status
                        final DrmJobStatus drmJobStatus=jobRunner.getStatus(drmJobId);
                        updateStatus(drmJobStatus);
                        if (drmJobStatus != null && drmJobStatus.getJobState().is(DrmJobState.TERMINATED)) {
                            handleCompletedJob(drmJobStatus);
                        }
                        else {
                            if (drmJobStatus==null) {
                                log.error("unexpected resul from queuingSystem.getStatus, drmJobStatus=null");
                            }
                            //put it back onto the queue
                            final long delay=getDelay(drmJobId, drmJobStatus);
                            svc.schedule(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        runningJobs.put(drmJobId);
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
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                jobRunner.stop();
            }
        });
        jobHandlerThread.setDaemon(true);
        jobHandlerThread.start();
    }
    
    @Override
    public void stop() {
        if (jobHandlerThread!=null) {
            jobHandlerThread.interrupt();
        }
    }
    
    public static final boolean isSet(final String str) {
        return str!=null && str.length()>0;
    }
    
    @Override
    public void runCommand(final String[] commandLine, final Map<String, String> environmentVariables, final File runDir, final File stdoutFile, final File stderrFile, final JobInfo jobInfo, final File stdinFile) throws CommandExecutorException {
        final Integer gpJobNo=jobInfo.getJobNumber();
        final DrmJobSubmission drmJobSubmission=new DrmJobSubmission.Builder(jobInfo, runDir)
            .commandLine(commandLine)
            .environmentVariables(environmentVariables)
            .stdoutFile(stdoutFile)
            .stderrFile(stderrFile)
            .stdinFile(stdinFile)
            .logFilename(logFilename)
            .build();
        
        jobLookupTable.insertJobRecord(drmJobSubmission);
        //TODO: make fault tolerant in the event that (1) startJob gets hung or (2) startJob throws an exception
        //TODO: consider modifying the API so that startJob returns a DrmJobStatus instance
        final String drmJobId=jobRunner.startJob(drmJobSubmission);
        if (!isSet(drmJobId)) {
            final DrmJobStatus drmJobStatus = new DrmJobStatus.Builder(drmJobId, DrmJobState.FAILED).build();
            jobLookupTable.updateJobStatus(gpJobNo, drmJobStatus);
            throw new CommandExecutorException("invalid drmJobId returned from startJob, gpJobId="+jobInfo.getJobNumber());
        }
        else {
            jobLookupTable.updateJobStatus(gpJobNo, new DrmJobStatus.Builder(drmJobId, DrmJobState.QUEUED).build());
            try {
                runningJobs.put(drmJobId);
            }
            catch (InterruptedException e) {
                //we were cancelled while trying to add another job to the runtime queue
                log.error("Interrupted, drmJobId="+drmJobId);
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    @Override
    public void terminateJob(JobInfo jobInfo) throws Exception {
        final String drmJobId=jobLookupTable.lookupDrmJobId(jobInfo.getJobNumber());
        jobRunner.cancelJob(drmJobId, jobInfo);
    }

    /**
     * Don't do anything in response to this call. This class will check for previously started jobs.
     */
    @Override
    public int handleRunningJob(JobInfo jobInfo) throws Exception {
        final String drmJobId=jobLookupTable.lookupDrmJobId(jobInfo.getJobNumber());
        if (drmJobId==null) {
            //no match found, what to do?
            log.error("No matching drmJobId found for gpJobId="+jobInfo.getJobNumber());
            return JobStatus.JOB_ERROR;
        }
        
        final DrmJobStatus drmJobStatus=jobRunner.getStatus(drmJobId);
        
        if (drmJobStatus==null) {
            log.error("No matching drmJobStatus for gpJobId="+jobInfo.getJobNumber()+", drmJobId="+drmJobId);
            return JobStatus.JOB_ERROR;
        }
        // an rval < 0 indicates to the calling method to ignore this
        if (drmJobStatus.getJobState().is(DrmJobState.TERMINATED)) {
            handleCompletedJob(drmJobStatus);
            return -1;
        }
        //it's still processing
        return -1;
    }

}
