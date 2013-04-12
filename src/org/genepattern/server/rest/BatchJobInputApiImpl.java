package org.genepattern.server.rest;

import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.AnalysisJobDAO;
import org.genepattern.server.domain.BatchJob;
import org.genepattern.server.domain.BatchJobDAO;
import org.genepattern.server.eula.GetTaskStrategy;
import org.genepattern.server.eula.GetTaskStrategyDefault;
import org.genepattern.server.job.input.BatchInputHelper;
import org.genepattern.server.job.input.JobInput;

public class BatchJobInputApiImpl implements JobInputApi {
    final static private Logger log = Logger.getLogger(BatchJobInputApiImpl.class);

    private ServerConfiguration.Context jobContext;
    private JobInputApi singleJobInputApi;
    final GetTaskStrategy getTaskStrategy;

    public BatchJobInputApiImpl(final ServerConfiguration.Context jobContext, final JobInputApi singleJobInputApi) {
        this(jobContext, singleJobInputApi, null);
    }            
    public BatchJobInputApiImpl(final ServerConfiguration.Context jobContext, final JobInputApi singleJobInputApi, final GetTaskStrategy getTaskStrategyIn) {
        this.jobContext=jobContext;
        this.singleJobInputApi=singleJobInputApi;
        if (getTaskStrategyIn == null) {
            getTaskStrategy=new GetTaskStrategyDefault();
        }
        else {
            getTaskStrategy=getTaskStrategyIn;
        }
    }
    
    @Override
    public String postJob(Context jobContext, JobInput jobInput) throws GpServerException {
        try {
            JobReceipt receipt=postBatchJob(jobContext, jobInput);
            return receipt.getJobIds().get(0);
        }
        catch (Throwable t) {
            log.error(t);
            throw new GpServerException("Error preparing job", t);
        }
    }
    
    @Override
    public JobReceipt postBatchJob(Context jobContext, JobInput jobInput) throws GpServerException {
        try {
            final JobReceipt receipt=doBatch(jobContext, jobInput);
            return receipt;
        }
        catch (Throwable t) {
            log.error(t);
            throw new GpServerException("Error preparing job", t);
        }
    }
    
    private JobReceipt doBatch(final Context userContext, final JobInput jobInput) throws GpServerException {
        try {
            BatchInputHelper batchJobInput=BatchInputHelper.initBatchJobInput(jobContext, getTaskStrategy, jobInput);
            List<JobInput> batchInputs=batchJobInput.prepareBatch();
            JobReceipt receipt=new JobReceipt();
            for(JobInput batchInput : batchInputs) {
                String jobId = singleJobInputApi.postJob(userContext, batchInput);
                receipt.addJobId(jobId);
            }
        
            if (receipt.getJobIds().size()>1) {
                //record batch job to DB, optionally assign custom batch id
                final String batchId=recordBatchJob(userContext, receipt);
                receipt.setBatchId(batchId);
            }
            return receipt;
        }
        catch (Throwable t) {
            log.error(t);
            throw new GpServerException("Error preparing job", t);
        }
    }
    
    private String recordBatchJob(final Context userContext, final JobReceipt jobReceipt) throws GpServerException {
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
