/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.rest;

import org.genepattern.server.config.GpContext;
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
    String postJob(GpContext jobContext, JobInput jobInput) throws GpServerException;

}
