package org.genepattern.server.executor.drm;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.genepattern.server.executor.CommandExecutor;
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.executor.CommandProperties;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.JobStatus;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * Second generation of the GenePattern CommandExecutor API, developed as a an abstract class
 * which implements the CommandExecutor interface.
 * 
 * This executor takes care of mapping between GenePattern job id and the job id on a specific
 * queuing system (such as LSF, SGE, or PBS). It also takes care of polling from the GP server runtime
 * to the queuing system.
 * 
 * Concepts:
 * A CommandExecutor in GenePattern is synonymous with a Job Scheduler. 
 * 
 *     Each GenePattern Job has a jobId (gpJobId).
 * 
 * Queuing system id, the id for a particular instance of a queuing system. For example ("BroadLSF")
 * Queuing system typeId, a unique id identifying the type of queuing system. By convention this
 *     is the fully qualified class name of a class which extends this abstract class.
 * 
 * Working directory, the working directory of the job.
 * 
 * GP Database tables
 * table: DRM_RUNNING_JOB
 * gp_job_id
 * working_dir
 * drm_id
 * drm_type
 * drm_job_id
 * drm_job_state
 * date_queued
 * date_updated
 * 
 * table: DRM_JOB_LOG
 * 
 * 
 * The process ...
 *      On startup, initialize an internal queue (using java concurrency package) of running jobs ...
 *      
 *      
 *      Loop through each item on the queue, polling for completion status.
 *      
 *      As new jobs are added, call runJob immediately, and on success add the new job to the queue.
 *      
 *      On shutdown ... shutdown the poller.
 * 
 * @author pcarr
 *
 */
public class DrmExecutor implements CommandExecutor {
    private static final Logger log = Logger.getLogger(DrmExecutor.class);

    public static interface QueuingSystem {
        /**
         * Replacement for the {@link CommandExecutor#runCommand(String[], Map, File, File, File, JobInfo, File)} method,
         * which also returns the queuing system specific jobId.
         * @return the jobId (or serialized representation of thejob) resulting from adding the GP job to the queue.
         */
        String startJob(String[] commandLine, Map<String, String> environmentVariables, File runDir, File stdoutFile, File stderrFile, JobInfo jobInfo, File stdinFile) throws CommandExecutorException;

        DrmJobStatus getStatus(String drmJobId);

        void cancelJob(String drmJobId, JobInfo jobInfo) throws Exception;
    }
    
    public static class ExampleQueuingSystem implements QueuingSystem {
        private Map<String,DrmJobStatus> statusMap=new LinkedHashMap<String,DrmJobStatus>();

        @Override
        public String startJob(String[] commandLine, Map<String, String> environmentVariables, File runDir, File stdoutFile, File stderrFile, JobInfo jobInfo, File stdinFile) throws CommandExecutorException {
            // TODO Auto-generated method stub
            final String drmJobId="DRM_"+jobInfo.getJobNumber();
            final DrmJobStatus drmJobStatus=new DrmJobStatus();
            drmJobStatus.drmJobId=drmJobId;
            drmJobStatus.jobState=JobState.QUEUED;
            statusMap.put(drmJobId, drmJobStatus);
            return drmJobId;
        }

        @Override
        public DrmJobStatus getStatus(String drmJobId) {
            return statusMap.get(drmJobId);
        }

        @Override
        public void cancelJob(String drmJobId, JobInfo jobInfo) throws Exception {
            throw new Exception("cancelJob not implemented, gpJobId="+jobInfo.getJobNumber()+", drmJobId="+drmJobId);
        }
    }
    
    public static interface Persistance {
        List<String> getRunningJobsFromDb();
    
        String lookupDrmJobId(final JobInfo jobInfo);
        String lookupGpJobId(final String drmJobId);
    
        void createJobRecord(final JobInfo jobInfo);
    
        void recordDrmId(final String drmJobId, final JobInfo jobInfo);
    }
    
    public static class PersistanceImpl implements Persistance {
        //map of gpJobId -> drmJobId
        private BiMap<String, String> lookup=HashBiMap.create();

        @Override
        public List<String> getRunningJobsFromDb() {
            return new ArrayList<String>(lookup.values());
        }

        @Override
        public String lookupDrmJobId(JobInfo jobInfo) {
            return lookup.get(""+jobInfo.getJobNumber());
        }
        
        @Override
        public String lookupGpJobId(final String drmJobId) {
            return lookup.inverse().get(drmJobId);
        }

        @Override
        public void createJobRecord(JobInfo jobInfo) {
            lookup.put(""+jobInfo.getJobNumber(), null);
        }

        @Override
        public void recordDrmId(String drmJobId, JobInfo jobInfo) {
            lookup.put(""+jobInfo.getJobNumber(), drmJobId);
        }
    }

    /**
     * Based on the DRMAA state model (http://www.ogf.org/documents/GFD.194.pdf).
     * Implemented as a hierarchical enum.
     * @author pcarr
     *
     */
    public enum JobState {
        /** The job status cannot be determined. This is a permanent issue, not being solvable by asking again for the job state. */
        UNDETERMINED(null),
        IS_QUEUED(null),
          /** The job is queued for being scheduled and executed. */
          QUEUED(IS_QUEUED),
          /** The job has been placed on hold by the system, the administrator, or the submitting user. */
          QUEUED_HELD(IS_QUEUED),
        STARTED(null),
          /** The job is running on an execution host. */
          RUNNING(STARTED),
          /** The job has been suspended by the user, the system or the administrator. */
          SUSPENDED(STARTED),
          /** The job was re-queued by the DRM system, and is eligible to run. */
          REQUEUED(STARTED),
          /** The job was re-queued by the DRM system, and is currently placed on hold by the system, the administrator, or the submitting user. */
          REQUEUED_HELD(STARTED),
        TERMINATED(null),
          /** The job finished without an error. */
          DONE(TERMINATED),
          /** The job exited abnormally before finishing. */
          FAILED(TERMINATED)
        ;
        
        private final JobState parent;
        private JobState(JobState parent) {
            this.parent=parent;
        }

        /**
         * So that DONE.is(TERMINATED) == true ...
         * So that UNDETERMINED.is(STARTED) == false ...
         * 
         * @param other
         * @return
         */
        public boolean is(JobState other) {
            if (other==null) {
                return false;
            }
            for (JobState jobState = this; jobState != null; jobState = jobState.parent) {
                if (other==jobState) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class DrmJobStatus {
        public String drmJobId;
        public JobState jobState;
        public Integer exitCode;
    }
    
    private QueuingSystem queuingSystem;
    private Persistance persistance=new PersistanceImpl();
    private static final int BOUND = 20000;
    private BlockingQueue<String> runningJobs;
    private Thread jobHandlerThread;

    //helper methods
    /**
     * Load queuing system from classname.
     * @param classname
     * @return
     */
    void initQueuingSystem(final String classname) {
        try {
            final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            final Class svcClass = Class.forName(classname, false, classLoader);
            if (!QueuingSystem.class.isAssignableFrom(svcClass)) {
                log.error(""+svcClass.getCanonicalName()+" does not implement "+QueuingSystem.class.getCanonicalName());
            }
            this.queuingSystem = (QueuingSystem) svcClass.newInstance();
        }
        catch (Throwable t) {
            log.error("Error loading QueuingSystem for classname: "+classname+", "+t.getLocalizedMessage(), t);
        }
        this.queuingSystem=new ExampleQueuingSystem();
    }
    
    void handleCompletedJob(final DrmJobStatus jobStatus) {
        final String gpJobId=persistance.lookupGpJobId(jobStatus.drmJobId);
        try {
            int jobNumber=new Integer(gpJobId);
            GenePatternAnalysisTask.handleJobCompletion(jobNumber, jobStatus.exitCode);
        }
        catch (NumberFormatException e) {
            log.error("Unexpected error getting gp job number as an integer, gpJobId="+gpJobId, e);
        }
        catch (Throwable t) {
            log.error("Unexpected error in handleCompletedJob for drmJobId="+jobStatus.drmJobId+", gpJobId="+gpJobId, t);
        }
    }
    
    @Override
    public void setConfigurationFilename(final String filename) {
    }
    
    @Override
    public void setConfigurationProperties(final CommandProperties properties) {
        final String queuingSystemClass=properties.getProperty("classname", ExampleQueuingSystem.class.getName());
        initQueuingSystem(queuingSystemClass);
    }

    @Override
    public void start() {
        runningJobs=new LinkedBlockingQueue<String>(BOUND);
        initJobHandler();
        initJobsOnStartup(runningJobs);
    }

    //should call this in a new thread because it can 
    private void initJobsOnStartup(final BlockingQueue<String> toQueue) {
        final List<String> jobs=persistance.getRunningJobsFromDb();
        for(final String drmJobId : jobs) {
            boolean success=runningJobs.offer(drmJobId);
        }
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
                        DrmJobStatus status=queuingSystem.getStatus(drmJobId);
                        if (status==null) {
                            //TODO: this is an error, could implement a retry count
                        }
                        if (status.jobState.is(JobState.TERMINATED)) {
                            handleCompletedJob(status);
                        }
                        else {
                            //put it back onto the queue
                            runningJobs.put(drmJobId);
                        }
                    }
                }
                catch (InterruptedException e) {
                }
            }
        });
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
    public void runCommand(String[] commandLine, Map<String, String> environmentVariables, File runDir, File stdoutFile, File stderrFile, JobInfo jobInfo, File stdinFile) throws CommandExecutorException {
        persistance.createJobRecord(jobInfo);
        String drmJobId=queuingSystem.startJob(commandLine, environmentVariables, runDir, stdoutFile, stderrFile, jobInfo, stdinFile);
        if (!isSet(drmJobId)) {
            //TODO: handle error
            throw new CommandExecutorException("invalid drmJobId returned from startJob, gpJobId="+jobInfo.getJobNumber());
        }
        else {
            persistance.recordDrmId(drmJobId, jobInfo);
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
        final String drmJobId=persistance.lookupDrmJobId(jobInfo);
        queuingSystem.cancelJob(drmJobId, jobInfo);
    }

    /**
     * Don't do anything in response to this call. This class will check for previously started jobs.
     */
    @Override
    public int handleRunningJob(JobInfo jobInfo) throws Exception {
        final String drmJobId=persistance.lookupDrmJobId(jobInfo);
        if (drmJobId==null) {
            //no match found, what to do?
            log.error("No matching drmJobId found for gpJobId="+jobInfo.getJobNumber());
            return JobStatus.JOB_ERROR;
        }
        
        final DrmJobStatus drmJobStatus=queuingSystem.getStatus(drmJobId);
        if (drmJobStatus==null) {
            log.error("No matching drmJobStatus for gpJobId="+jobInfo.getJobNumber()+", drmJobId="+drmJobId);
            return JobStatus.JOB_ERROR;
        }
        // an rval < 0 indicates to the calling method to ignore this
        if (drmJobStatus.jobState.is(JobState.TERMINATED)) {
            handleCompletedJob(drmJobStatus);
            return -1;
        }
        //it's still processing
        return -1;
    }
    
    
}
