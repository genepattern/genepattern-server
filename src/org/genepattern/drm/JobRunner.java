package org.genepattern.drm;

import org.genepattern.server.executor.CommandExecutorException;

/**
 * Service provider interface for integrating a queuing system, aka job runner, into GenePattern, for example for PBS/Torque.
 * 
 * The GenePattern Server will call startJob for each new GP job. It will poll for completion status by calling the
 * getStatus method, until the returned status indicates job completion.
 * 
 * The GP server maintains a lookup table of drm jobId to GenePattern jobId.
 * 
 * @author pcarr
 *
 */
public interface JobRunner {
    public static final String PROP_EXECUTOR_INPUT_PARAMS="executor.inputParams";

    public static final String PROP_PREFIX="job.";
    public static final String PROP_QUEUE="job.queue";
    public static final String PROP_MEMORY="job.memory";
    public static final String PROP_WALLTIME="job.walltime";
    public static final String PROP_NODE_COUNT="job.nodeCount";
    public static final String PROP_CPU_COUNT="job.cpuCount";
    public static final String PROP_EXTRA_ARGS="job.extraArgs";
    public static final String PROP_WORKER_NAME="job.workerName";
    
    /**
     * Set a boolean value, when true, it means flag the job as ERROR if there is stderr output. 
     * Example config file entry
     * <pre>
     *     job.error_status.stderr: true
     * </pre>
     */
    public static final String PROP_ERROR_STATUS_STDERR="job.error_status.stderr";

    /**
     * Set a boolean value, when true, it means flag the job as ERROR if ther is a non-zero exit code.
     * Example config file entry
     * <pre>
     *     job.error_status.exit_value: true
     * </pre>
     */
    public static final String PROP_ERROR_STATUS_EXIT_VALUE="job.error_status.exit_value";
    
    /** 
     * Service shutdown, clean up resources. 
     * This is called when the parent JobExecutor is shut down.
     * 
     */
    void stop();
    
    /**
     * The GenePattern Server calls this when it is ready to submit the job to the queue.
     * Submit the job to the queue and return immediately.
     * The drm jobId returned by this method is used as the key into a 
     * lookup table mapping the gp jobId to the drm jobId.
     * 
     * @return the drm jobId resulting from adding the job to the queue.
     */
    String startJob(DrmJobSubmission drmJobSubmission) throws CommandExecutorException;

    /**
     * Get the status of the job.
     * @param drmJobId
     * @return
     */
    DrmJobStatus getStatus(String drmJobId);

    /**
     * This method is called when the GP server wants to cancel a job before it has completed on the queuing system. 
     * For example when a user terminates a job from the web ui.
     * 
     * @param drmJobId
     * @param jobInfo
     * @return true if the job was successfully cancelled, false otherwise.
     * @throws Exception
     */  
    boolean cancelJob(String drmJobId, DrmJobSubmission drmJobSubmission) throws Exception;

}
