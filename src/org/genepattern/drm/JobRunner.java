/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.drm;

import org.genepattern.server.executor.CommandExecutorException;

/**
 * Interface for integrating an external queuing system with a GenePattern installation.
 * 
 * The GenePattern Server will call {@link #startJob(DrmJobSubmission)} for each new GP job. 
 * It will poll for completion status by calling {@link #getStatus(DrmJobRecord)} until the returned status indicates job completion.
 * 
 * The server maintains a lookup table of external jobId to GenePattern jobId.
 * 
 * In GenePattern version >= 3.9.0, additional methods can be invoked, if they are defined.
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
    /**
     * The 'job.memory' property, when set, declares the amount of RAM to request before starting 
     * a job on a compute node. For example,
     *     job.memory: 8 Gb
     * 
     * On most queuing systems, the job will not start until a node with the requested RAM becomes available.
     * The job will also be terminated when it exceeds that amount.
     * 
     * For java modules, the 'job.memory' value will be used to generate the '-Xmx' command line arg.
     * By default, you should not declare 'job.javaXmx' property or set it in the module command line.
     * Additional customizations for java modules:
     *     Use 'job.javaXmx' to set a different -Xmx flag than the 'job.memory' value;
     *     Use 'job.javaXmxMin' to set a global minimum -Xmx value;
     *     Use 'job.javaXmxPad' to set pad the requested RAM on the compute node;
     */
    public static final String PROP_MEMORY="job.memory";
    
    /**
     * The 'job.javaXmx' property, when set, is used to set the '-Xmx' flag on the java command line.
     */
    public static final String PROP_JAVA_XMX="job.javaXmx";
    /**
     * The 'job.javaXmxMin' property, defines a minimum '-Xmx' value for the java command line.
     * For example,
     *     job.memory: 512 Mb
     *     job.javaXmxMin: 1 Gb
     * Will result in the following command line:
     *     java -Xmx1g ... 
     */
    public static final String PROP_JAVA_XMX_MIN="job.javaXmxMin";
    /**
     * The 'job.javaXmxPad' specifies an additional amount of RAM which must be requested on compute nodes.
     * For example,
     *     job.memory: 8 Gb
     *     job.javaXmxPad: 4 Gb
     * Will require a 12 Gb RAM compute node.
     */
    public static final String PROP_JAVA_XMX_PAD="job.javaXmxPad";
    
    /**
     * The 'job.walltime' runtime limit for the job in d-hh:mm:ss format.
     * i.e.
     * <pre>
     * <code>job.walltime: 1-00:00:00</code>
     * </pre>
     */
    public static final String PROP_WALLTIME="job.walltime";
    public static final String PROP_NODE_COUNT="job.nodeCount";
    /** 'job.cpuCount', the number of cpus requested for the given job  */
    public static final String PROP_CPU_COUNT="job.cpuCount";
    /** The 'job.priority' for the job as a numerical value */
    public static final String PROP_PRIORITY="job.priority";
    /** 'job.extraArgs', additional command line arguments passed to the queuing system when submitting a job. */
    public static final String PROP_EXTRA_ARGS="job.extraArgs";
    /** The optional 'job.project' property specifies a queuing system specific project name for the job, for example the '-P' arg to the LSF bsub command. */
    public static final String PROP_PROJECT="job.project";
    
    /**
     * Set a boolean value, when true, it means flag the job as ERROR if there is stderr output. 
     * Example config file entry
     * <pre>
     *     job.error_status.stderr: true
     * </pre>
     */
    public static final String PROP_ERROR_STATUS_STDERR="job.error_status.stderr";

    /**
     * Set a boolean value, when true, it means flag the job as ERROR if there is a non-zero exit code.
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
