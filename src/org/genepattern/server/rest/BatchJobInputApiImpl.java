package org.genepattern.server.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.domain.AnalysisJobDAO;
import org.genepattern.server.domain.BatchJob;
import org.genepattern.server.domain.BatchJobDAO;
import org.genepattern.server.eula.GetTaskStrategy;
import org.genepattern.server.eula.GetTaskStrategyDefault;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.JobInput.Param;
import org.genepattern.server.job.input.JobInput.ParamId;
import org.genepattern.server.job.input.JobInput.ParamValue;
import org.genepattern.server.job.input.ParamListHelper;
import org.genepattern.server.rest.JobInputApiLegacy.ParameterInfoRecord;
import org.genepattern.server.webapp.BatchSubmit;
import org.genepattern.server.webapp.BatchSubmit.MultiFileParameter;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

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
    
    //preferred implementation
    private JobReceipt doBatch(final Context userContext, final JobInput jobInput) throws GpServerException {
        JobInput batchTemplate=checkForBatchParams(jobInput);
        List<JobInput> batchInputs=initBatchInputs(batchTemplate);
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

    /**
     * Any input parameter which accepts one and only one file, but which is given a directory input parameter
     * should be flagged as a batch input parameter.
     * 
     * @param jobInput
     * @return
     */
    private JobInput checkForBatchParams(JobInput jobInput) { 
        if (jobInput.getBatchParams().size()>0) {
            return jobInput;
        }
        
        TaskInfo taskInfo=getTaskStrategy.getTaskInfo(jobInput.getLsid());
        final Map<String,ParameterInfoRecord> paramInfoMap=ParameterInfoRecord.initParamInfoMap(taskInfo);
        
        for(Entry<ParamId,Param> entry : jobInput.getParams().entrySet()) {
            final ParameterInfoRecord record=paramInfoMap.get(entry.getKey().getFqName());
            final Param param=entry.getValue();
            if (record.getFormal().isInputFile()) {
                ParamListHelper plh=new ParamListHelper(jobContext, record, param);
                boolean canCreateBatch=plh.canCreateCreateBatchJob();
                if (canCreateBatch) {
                    param.setBatchParam(true);
                }
            }
        }
        return jobInput;
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
    
    private List<JobInput> initBatchInputs(JobInput batchTemplate) throws GpServerException {
        Map<ParamId,List<ParamValue>> batchParamMap=null;
        try {
            batchParamMap=initBatchParams(batchTemplate);
        }
        catch (Throwable t) {
            throw new GpServerException("Error initializing batch params", t);
        }
        if (batchParamMap==null || batchParamMap.size()==0) {
            //it's not a batch job
            List<JobInput> batchInputs=new ArrayList<JobInput>();
            batchInputs.add(batchTemplate);
            return batchInputs;
        }
        
        //it is a batch job
        if (batchParamMap.size()==1) {
            List<JobInput> batchInputs=new ArrayList<JobInput>();
            for(Entry<ParamId,List<ParamValue>> entry : batchParamMap.entrySet()) {
                for(ParamValue value : entry.getValue()) {
                    JobInput nextBatchInput = prepareBatchInput(entry.getKey(), value, batchTemplate);
                    batchInputs.add(nextBatchInput);
                }
            }
            return batchInputs;
        }
        
        //TODO: handle more than one batch input parameter
        throw new GpServerException("Batch job submission not implemented for more than one batch parameter!");
    }

    private Map<ParamId,List<ParamValue>> initBatchParams(final JobInput jobInput) throws Exception {
        BatchInputHelper bih=new BatchInputHelper(jobContext, getTaskStrategy, jobInput);
        return bih.initBatchParams();
    }
    
    private JobInput prepareBatchInput(ParamId batchParamId, ParamValue batchParamValue, JobInput jobInput) {
        //TODO: should implement JobInput.deepCopy method
        //    first, make a deep copy of job input
        //    then, remove all of the batch params from the copy
        //    then add the batchParamValues to the copy
        
        JobInput batchInput = new JobInput();
        batchInput.setLsid(jobInput.getLsid());        
        for(final Entry<ParamId,Param> entry : jobInput.getParams().entrySet()) {
            if (entry.getKey().equals(batchParamId)) {
                //this is the batch parameter 
                batchInput.addValue(batchParamId.getFqName(), batchParamValue.getValue());
            }
            else {
                for (ParamValue pv : entry.getValue().getValues()) {
                    batchInput.addValue(entry.getKey().getFqName(), pv.getValue());
                }
            }
        }
        return batchInput;
    }

    @Override
    public String postJob(Context jobContext, JobInput jobInput) throws GpServerException {
        JobReceipt receipt=doBatch(jobContext, jobInput);
        return receipt.getJobIds().get(0);
    }
    
    @Override
    public JobReceipt postBatchJob(Context jobContext, JobInput jobInput) throws GpServerException {
        JobReceipt receipt=doBatch(jobContext, jobInput);
        return receipt;
    }
    
    static class BatchInputHelper {
        private JobInput jobInput;
        private Context jobContext;
        private GetTaskStrategy getTaskStrategy;
        private TaskInfo taskInfo;
        private Map<String,ParameterInfoRecord> paramInfoMap;

        public BatchInputHelper(final Context jobContext, final GetTaskStrategy getTaskStrategyIn, final JobInput jobInput) {
            this.jobInput=jobInput;
            this.jobContext=jobContext;
            if (getTaskStrategyIn == null) {
                getTaskStrategy=new GetTaskStrategyDefault();
            }
            else {
                getTaskStrategy=getTaskStrategyIn;
            }
            taskInfo=getTaskStrategy.getTaskInfo(jobInput.getLsid());
            paramInfoMap=ParameterInfoRecord.initParamInfoMap(taskInfo);
            inferBatchParams();
        }
        
        /**
         * If necessary, setBatchParam on any input parameter for which
         * we should run a batch job. This is for the case when we infer 
         * batch parameters rather than declare them.
         */
        private void inferBatchParams() { 
            if (jobInput.getBatchParams().size()>0) {
                return;
            }
                
            for(Entry<ParamId,Param> entry : jobInput.getParams().entrySet()) {
                final ParameterInfoRecord record=paramInfoMap.get(entry.getKey().getFqName());
                final Param param=entry.getValue();
                if (record.getFormal().isInputFile()) {
                    ParamListHelper plh=new ParamListHelper(jobContext, record, param);
                    boolean canCreateBatch=plh.canCreateCreateBatchJob();
                    if (canCreateBatch) {
                        param.setBatchParam(true);
                    }
                }
            }
        }
        
        public Map<ParamId,List<ParamValue>> initBatchParams() throws Exception {
            if (!jobInput.isBatchJob()) {
                //it's not a batch job
                return Collections.emptyMap();
            }
            final Map<ParamId,List<ParamValue>> batchParamMap=new LinkedHashMap<ParamId, List<ParamValue>>();
            final List<Param> batchParams=jobInput.getBatchParams();
            //part 1: for each batch input parameter, if the actual value is a directory
            //    need to 'ls' the directory and initialize the actual values
            for(final Param batchParam : batchParams) {
                final ParameterInfoRecord record=paramInfoMap.get(batchParam.getParamId().getFqName());
                final ParameterInfo pinfo=record.getFormal();
                final List<ParamValue> values=new ArrayList<ParamValue>();
                for(final ParamValue paramValue : batchParam.getValues()) {
                    List<ParamValue> out=initParamValues(pinfo, paramValue);
                    values.addAll(out);
                }
                batchParamMap.put(batchParam.getParamId(), values);
            }
        
            //validate number of inputs
            if (batchParamMap.size()<=1) {
                //TODO: could do a check for max number of jobs per batch, currently it's unlimited
                return batchParamMap;
            }

            //TODO: validate number of inputs when there are more than one batch input parameters
            throw new IllegalArgumentException("Multiple batch parameters not yet implemented!");
        }

        private List<ParamValue> initParamValues(final ParameterInfo pinfo, final ParamValue in) throws Exception { 
            //assume that the value is a directory on the server  
            List<ParamValue> out=new ArrayList<ParamValue>();
        
            boolean isAdmin=jobContext.isAdmin();
            String dirUrl=in.getValue();
            MultiFileParameter mfp = BatchSubmit.getMultiFileParameter(isAdmin, jobContext, dirUrl);
            for(GpFilePath file : mfp.getFiles()) {
                boolean accepted=accept(pinfo, file);
                if (accepted) {
                    final String value=file.getUrl().toExternalForm();
                    out.add(new ParamValue(value));
                }
            }
            return out;
        }

        /**
         * Is the given input value a valid batch input parameter for the given pinfo?
         * 
         * @param pinfo
         * @param in
         * @return
         */
        private boolean accept(final ParameterInfo pinfo, final GpFilePath inputValue) {
            if (inputValue.isDirectory()) {
                //special-case for directory inputs
                if (pinfo._isDirectory()) {
                    return true;
                }
                else {
                    return false;
                }
            }
            List<String> fileFormats = SemanticUtil.getFileFormats(pinfo);
            if (fileFormats.size()==0) {
                //no declared fileFormats, acceptAll
                return true;
            }
            final String kind=inputValue.getKind();
            if (fileFormats.contains(kind)) {
                return true;
            }
            final String ext = inputValue.getExtension();
            if (fileFormats.contains(ext)) {
                return true;
            }
            return false;
        }
    }

}
