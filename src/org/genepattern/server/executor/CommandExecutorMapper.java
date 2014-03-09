package org.genepattern.server.executor;

import org.genepattern.webservice.JobInfo;

/**
 * Provides a mapping between GenePattern jobs and CommandExecutor instances.
 * 
 * @author pcarr
 */
public interface CommandExecutorMapper {
    /**
     * The reserved executorId for pipelines.
     */
    public static final String PIPELINE_EXEC_ID = "PipelineExec";

    /**
     * Get the command executor for the given job.
     * 
     * @param jobInfo
     * @return the CommandExecutor instance with which to run the job.
     */
    CommandExecutor getCommandExecutor(JobInfo jobInfo) throws CommandExecutorNotFoundException;
    
    /**
     * Get additional runtime properties to be passed to the command executor for the given job.
     * 
     * For example, by default send my lsf jobs to the 'broad' queue. I'd like to send all GISTIC jobs to the 'priority' queue.
     * The implementor of this interface has the option to provide additional properties at runtime. 
     * The implementor of the CommandExecutor interface for LSF (e.g. LsfCommandExecutor) can use these properties to construct the bsub -q <lsf.queue> command line
     * on a job specific basis.
     *
     * @param jobInfo
     * @return
     * 
     * @deprecated, newer implementations of the CommandExecutor interface should call GpConfig.getValue(GpContext gpContext, String key) instead.
     */
    CommandProperties getCommandProperties(JobInfo jobInfo);
}
