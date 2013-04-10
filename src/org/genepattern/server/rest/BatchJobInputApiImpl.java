package org.genepattern.server.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
    
    @Override
    public String postJob(Context jobContext, JobInput jobInput) throws GpServerException {
        try {
            JobReceipt receipt=doBatch(jobContext, jobInput);
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
            JobReceipt receipt=doBatch(jobContext, jobInput);
            return receipt;
        }
        catch (Throwable t) {
            log.error(t);
            throw new GpServerException("Error preparing job", t);
        }
    }
    
    private JobReceipt doBatch(final Context userContext, final JobInput jobInput) throws Exception {
        JobInput batchTemplate=inferBatchParameters(jobInput);
        List<JobInput> batchInputs=createBatchInputs(batchTemplate);
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
    private JobInput inferBatchParameters(JobInput jobInput) { 
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
    
    /**
     * Create a List of JobInput instances, one for each batch job to run.
     * Use the given batchTemplate to ...
     *     1) identify which parameters are batch parameters
     *        Can be inferred or declared.
     *     2) set non-default values for the remaining non-batch parameters
     * 
     * This is based on the pre-existing 3.5.0 and earlier method. Which has the following limitations:
     *     1) can only batch on input files, not other types
     *     2) the value for a batch input parameter must be to a directory on the file system.
     *     3) when there are multiple batch input parameters, the union of all matching parameters determines
     *        how many jobs to run. Matches are based on file basename.
     * 
     * @param batchTemplate, the jobInput values entered in the job input form, 
     *     including any flags for batch parameters.
     * @return a list JobInput, each item in the list is a new job which should be run.
     * 
     * @throws Exception
     */
    private List<JobInput> createBatchInputs(JobInput batchTemplate) throws Exception {
        final List<JobInput> batchInputs=new ArrayList<JobInput>();
        BatchInputHelper bih=new BatchInputHelper(jobContext, getTaskStrategy, batchTemplate);
        BatchInputHelper.Data batchData=bih.prepareBatchInput();
        int numJobs=batchData.getNumBatchJobs();
        if (numJobs==0) {
            //it's not a batch job
            batchInputs.add(batchTemplate);
            return batchInputs;            
        }
        
        //it is a batch job
        for(int idx=0; idx<numJobs; ++idx) {
            JobInput nextJob=batchData.prepareJobInput(idx, batchTemplate);
            batchInputs.add(nextJob);
        }
        return batchInputs;
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
    
    static class BatchInputHelper {
        static interface Data {
            int getNumBatchJobs();
            Set<ParamId> getBatchParameters();
            ParamValue getValue(ParamId paramId, int idx);
            boolean isBatchParam(ParamId paramId);
            
            JobInput prepareJobInput(int idx, JobInput template);
        }
        
        public Data prepareBatchInput() throws Exception {
            final Map<ParamId,List<ParamValue>> m=initBatchParams();
            //final int numBatchParameters=m.size();
            return new Data() {
                @Override
                public boolean isBatchParam(final ParamId paramId) {
                    return m.containsKey(paramId);
                }

                @Override
                public int getNumBatchJobs() {
                    //TODO: implement more efficient method
                    if (m==null || m.size()==0) {
                        return 0;
                    }
                    for(List<ParamValue> values : m.values()) {
                        return values.size();
                    }
                    return 0;
                }

                @Override
                public ParamValue getValue(final ParamId paramId, final int idx) {
                    List<ParamValue> values=m.get(paramId);
                    return values.get(idx);
                }

                @Override
                public Set<ParamId> getBatchParameters() {
                    return m.keySet();
                }

                @Override
                public JobInput prepareJobInput(final int idx, final JobInput template) {
                    JobInput batchInput = new JobInput();
                    batchInput.setLsid(template.getLsid());
                    for(final Entry<ParamId,Param> entry : template.getParams().entrySet()) {
                        if (isBatchParam(entry.getKey())) {
                            final ParamValue batchValue=getValue(entry.getKey(), idx);
                            batchInput.addValue(entry.getKey().getFqName(), batchValue.getValue());
                        }
                        else {
                            for (ParamValue pv : entry.getValue().getValues()) {
                                batchInput.addValue(entry.getKey().getFqName(), pv.getValue());
                            }
                        }
                    }
                    return batchInput;
                }
            };

        }
        
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
        
        /**
         * Helper method for preparing a batch of jobs.
         * The expected output is a table of values, where each column is a batch parameter id,
         * and each row are the input values for a batch job.
         * 
         * @return
         * @throws Exception
         */
        private Map<ParamId,List<ParamValue>> initBatchParams() throws Exception {
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

            //TODO: for multiple batch parameters, create a union based on base filenames
            int numJobs=-1;
            for(Entry<ParamId,List<ParamValue>> entry : batchParamMap.entrySet()) {
                int size=entry.getValue().size();
                if (numJobs==-1) {
                    numJobs=size;
                }
                if (numJobs != size) {
                    throw new IllegalArgumentException("Union of batch inputs not yet implemented!");
                }
            }
            return batchParamMap;
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
