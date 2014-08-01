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
 * In GenePattern version <= 3.8.3, additional methods can be invoked, if they are defined.
 * <p>
 * If your JobRunner declares a 'setCommandProperties' method, it will be invoked on system startup before any jobs are submitted.
 * The CommandProperties argument is loaded from the 'configuration.properties' of the yaml file for the given JobRunner.
 * <pre>
     public void setCommandProperties(CommandProperties properties);
 * </pre>
 * If your JobRunner declares a 'start' method, it will be invoked on system startup before any jobs are submitted.
 * <pre>
   public void start();
 * </pre>
 * 
 * The JobExecutor uses reflection to check for this additional methods, so that older implementations of the JobRunner
 * are still compatible.
 * 
 * @author pcarr
 *
 */
public interface JobRunner {
    public static final String PROP_PREFIX="job.";
    public static final String PROP_JOB_INPUT_PARAMS="job.inputParams";
    /** 
     * optional param, when set it is the name of a log file for saving meta data about the completed job.
     * For example, the LsfJobRunner streams the output from the bsub command to the '.lsf.out' file.
     */
    public static final String PROP_LOGFILE="job.logFile";
    public static final String PROP_QUEUE="job.queue";
    /** 
     * The 'job.virtualQueue' property, when set, is used for estimating waiting times for pending jobs. 
     * It was added as a feature for the LSF integration. 
     * The 'bsub' command line uses the 'job.queue' value, the GP  database uses the 'job.virtualQueue' value. 
     * It can be used as an alias to the actual queue name used by the external system.
     */
    public static final String PROP_VIRTUAL_QUEUE="job.virtualQueue";
    public static final String PROP_MEMORY="job.memory";
    public static final String PROP_JAVA_XMX="job.javaXmx";
    public static final String PROP_WALLTIME="job.walltime";
    public static final String PROP_NODE_COUNT="job.nodeCount";
    public static final String PROP_CPU_COUNT="job.cpuCount";
    public static final String PROP_EXTRA_ARGS="job.extraArgs";
    
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
    DrmJobStatus getStatus(DrmJobRecord drmJobRecord);

    /**
     * This method is called when the GP server wants to cancel a job before it has completed on the queuing system. 
     * For example when a user terminates a job from the web ui.
     * 
     * @param drmJobRecord, contains a record of the job
     * @return true if the job was successfully cancelled, false otherwise.
     * @throws Exception
     */  
    boolean cancelJob(DrmJobRecord drmJobRecord) throws Exception;

}
