/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.batch;

import java.util.List;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.domain.AnalysisJobDAO;
import org.genepattern.server.domain.BatchJob;
import org.genepattern.server.domain.BatchJobDAO;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.rest.GpServerException;
import org.genepattern.server.rest.JobInputApiImplV2;
import org.genepattern.server.rest.JobReceipt;

/**
 * Default implementation of the batch submitter interface, submits jobs to the 
 * standard GP job queue, and records batch jobs to the GP DB.
 * 
 * @author pcarr
 *
 */
public class BatchSubmitterImpl implements BatchSubmitter {

    private final HibernateSessionManager mgr;
    private final GpContext userContext;
    private final JobInputApiImplV2 jobInputApi;
    
    public BatchSubmitterImpl(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext userContext) {
        this.mgr=mgr;
        this.userContext=userContext;
        this.jobInputApi=new JobInputApiImplV2();
    }
    
    /**
     * Submit your jobs to the GP server.
     * Use the list of JobInput from the prepareBatch() method as input to this method.
     * 
     * @param batchInputs
     * @return
     * @throws GpServerException
     */
    @Override
    public JobReceipt submitBatch(final List<JobInput> batchInputs) throws GpServerException {
        JobReceipt receipt=new JobReceipt();
        for(JobInput batchInput : batchInputs) {
            String jobId = jobInputApi.postJob(userContext, batchInput);
            receipt.addJobId(jobId);
        }
        
        if (receipt.getJobIds().size()>1) {
            //record batch job to DB, optionally assign custom batch id
            final String batchId=recordBatchJob(userContext, receipt);
            receipt.setBatchId(batchId);
        }
        return receipt;
    }

    private String recordBatchJob(final GpContext userContext, final JobReceipt jobReceipt) throws GpServerException {
        //legacy implementation, based on code in SubmitJobServlet
        String batchId="";
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            BatchJob batchJob = new BatchJob(userContext.getUserId());

            for(final String jobId : jobReceipt.getJobIds()) {
                int jobNumber = Integer.parseInt(jobId);
                batchJob.getBatchJobs().add(new AnalysisJobDAO(mgr).findById(jobNumber));
            }
            new BatchJobDAO(mgr).save(batchJob);
            batchId = ""+batchJob.getJobNo();
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
        }
        catch (Throwable t) {
            mgr.rollbackTransaction();
            throw new GpServerException("Error recording batch jobs to DB", t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
        return batchId;
    }

}
