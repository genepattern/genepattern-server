package org.genepattern.server.rest;

public interface JobInputApi {
    //String requestJobId() throws Exception;

    
    /**
     * Add a job to the queue.
     * 
     * @param currentUser
     * @param jobInput
     * @return
     */
    String postJob(String currentUser, JobInput jobInput) throws GpServerException;
}
