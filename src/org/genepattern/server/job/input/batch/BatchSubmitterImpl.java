package org.genepattern.server.job.input.batch;

import java.util.List;

import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.AnalysisJobDAO;
import org.genepattern.server.domain.BatchJob;
import org.genepattern.server.domain.BatchJobDAO;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.rest.GpServerException;
import org.genepattern.server.rest.JobInputApi;
import org.genepattern.server.rest.JobInputApiFactory;
import org.genepattern.server.rest.JobReceipt;

/**
 * Default implementation of the batch submitter interface, submits jobs to the 
 * standard GP job queue, and records batch jobs to the GP DB.
 * 
 * @author pcarr
 *
 */
public class BatchSubmitterImpl implements BatchSubmitter {

    private final GpContext userContext;
    private final JobInputApi jobInputApi;
    
    public BatchSubmitterImpl(final GpContext userContext) {
        this(userContext, null);
    }
    public BatchSubmitterImpl(final GpContext userContext, final JobInputApi jobInputApiIn) {
        this.userContext=userContext;
        if (jobInputApiIn == null) {
            this.jobInputApi=JobInputApiFactory.createJobInputApi(userContext);
        }
        else {
            this.jobInputApi=jobInputApiIn;
        }
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
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            BatchJob batchJob = new BatchJob(userContext.getUserId());

            for(final String jobId : jobReceipt.getJobIds()) {
                int jobNumber = Integer.parseInt(jobId);
                batchJob.getBatchJobs().add(new AnalysisJobDAO().findById(jobNumber));
            }
            new BatchJobDAO().save(batchJob);
            batchId = ""+batchJob.getJobNo();
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        catch (Throwable t) {
            HibernateUtil.rollbackTransaction();
            throw new GpServerException("Error recording batch jobs to DB", t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
        return batchId;
    }

}
