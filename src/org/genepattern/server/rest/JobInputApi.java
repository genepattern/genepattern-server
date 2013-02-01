package org.genepattern.server.rest;

import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.job.input.JobInput;

public interface JobInputApi {
    //String requestJobId() throws Exception;

    
    /**
     * Add a job to the queue.
     * 
     * @param jobContext, must have a valid userId
     * @param jobInput
     * @return
     */
    String postJob(Context jobContext, JobInput jobInput) throws GpServerException;
}
