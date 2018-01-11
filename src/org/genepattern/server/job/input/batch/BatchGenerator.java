/*******************************************************************************
 * Copyright (c) 2003-2018 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.batch;

import java.util.List;

import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.rest.GpServerException;
import org.genepattern.server.rest.JobReceipt;

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
     * <p/>
     * Use-case: This abstraction is
     *
     * @param batchInputTemplate
     * @return
     */
    List<JobInput> prepareBatch(final JobInput batchInputTemplate) throws GpServerException;

    JobReceipt submitBatch(final List<JobInput> batchInputs) throws GpServerException;
}