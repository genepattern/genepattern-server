package org.genepattern.server.queue;

import org.genepattern.webservice.JobInfo;

/**
 * Provides a mapping between GenePattern jobs and CommandExecutor instances.
 * 
 * @author pcarr
 */
public interface CommandExecutorMapper {
    /**
     * @param jobInfo
     * @return the CommandExecutor instance with which to run the job.
     */
    CommandExecutor getCommandExecutor(JobInfo jobInfo) throws CommandExecutorNotFoundException;
}
