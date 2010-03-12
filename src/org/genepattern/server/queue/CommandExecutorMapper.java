package org.genepattern.server.queue;

import org.genepattern.webservice.JobInfo;

/**
 * Provides a mapping between jobs and command executor services. The purpose of this interface is to enable configurable mapping of
 * GenePattern jobs to different instances of the CommandExecutorService interface.
 * 
 * @author pcarr
 */
public interface CommandExecutorMapper {
    /**
     * 
     * @param jobInfo
     * @return the CommandExecutorService implementation with which to run the job.
     */
    CommandExecutor getCommandExecutor(JobInfo jobInfo) throws CommandExecutorNotFoundException;
}
