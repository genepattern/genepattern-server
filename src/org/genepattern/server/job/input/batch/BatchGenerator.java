/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.batch;

import java.util.List;

import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.rest.GpServerException;

/**
 * Generate a list of zero or more batch jobs based on user-input values.
 * 
 * @author pcarr
 */
public interface BatchGenerator {
    /*
     * Types of filters:
     * 1) naive, run all 
     */
    
    /**
     * Generate a list of zero or more batch jobs to run, based on user-input defined in the given
     * batchInputTemplate instance.
     * 
     * Use-case: This abstraction is 
     * 
     * @param batchInputTemplate
     * @return
     */
    List<JobInput> prepareBatch(final JobInput batchInputTemplate) throws GpServerException;
}