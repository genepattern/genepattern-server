/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.batch;

import java.util.List;

import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.rest.GpServerException;
import org.genepattern.server.rest.JobReceipt;

/**
 * Submit a batch of jobs to the queue.
 * 
 * @author pcarr
 *
 */
public interface BatchSubmitter {
    /**
     * Submit a batch of jobs to the queue.
     * 
     * @param batchInputs
     * @return
     * @throws GpServerException
     */
    JobReceipt submitBatch(final List<JobInput> batchInputs) throws GpServerException;
}
