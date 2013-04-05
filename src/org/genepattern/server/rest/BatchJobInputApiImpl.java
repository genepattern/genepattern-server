package org.genepattern.server.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.JobInput.Param;
import org.genepattern.server.job.input.JobInput.ParamId;
import org.genepattern.server.job.input.JobInput.ParamValue;
import org.genepattern.server.webapp.BatchSubmit;
import org.genepattern.server.webapp.BatchSubmit.MultiFileParameter;

public class BatchJobInputApiImpl implements JobInputApi {
    ServerConfiguration.Context jobContext;
    JobInputApi singleJobInputApi;
    
    public BatchJobInputApiImpl(final ServerConfiguration.Context jobContext, final JobInputApi singleJobInputApi) {
        this.jobContext=jobContext;
        this.singleJobInputApi=singleJobInputApi;
    }
    
    private Map<ParamId,List<ParamValue>> initBatchParams(final JobInput jobInput) throws Exception {
        if (!jobInput.isBatchJob()) {
            //it's not a batch job
            return Collections.emptyMap();
        }
        final Map<ParamId,List<ParamValue>> batchParamMap=new LinkedHashMap<ParamId, List<ParamValue>>();
        final List<Param> batchParams=jobInput.getBatchParams();
        //part 1: for each batch input parameter, if the actual value is a directory
        //    need to 'ls' the directory and initialize the actual values
        for(final Param batchParam : batchParams) {
            final List<ParamValue> values=new ArrayList<ParamValue>();
            for(final ParamValue paramValue : batchParam.getValues()) {
                List<ParamValue> out=initParamValues(paramValue);
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
    
    private List<ParamValue> initParamValues(final ParamValue in) throws Exception { 
        //assume that the value is a directory on the server  
        List<ParamValue> out=new ArrayList<ParamValue>();
        
        boolean isAdmin=jobContext.isAdmin();
        String dirUrl=in.getValue();
        MultiFileParameter mfp = BatchSubmit.getMultiFileParameter(isAdmin, jobContext, dirUrl);
        for(GpFilePath file : mfp.getFiles()) {
            out.add(new ParamValue(file.getUrl().toExternalForm()));
        }
        return out;
    }
    
    private JobInput prepareBatchInput(ParamId batchParamId, ParamValue batchParamValue, JobInput jobInput) {
        //TODO: should implement JobInput.deepCopy method
        //    first, make a deep copy of job input
        //    then, remove all of the batch params from the copy
        //    the add the batchParamValues to the copy
        
        JobInput batchInput = new JobInput();
        batchInput.setLsid(jobInput.getLsid());        
        for(final Entry<ParamId,Param> entry : jobInput.getParams().entrySet()) {
            if (entry.getKey().equals(batchParamId)) {
                //this is the batch parameter (not expecting a list of lists, but we can pass it along)
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
        Map<ParamId,List<ParamValue>> batchParamMap=null;
        try {
            batchParamMap=initBatchParams(jobInput);
        }
        catch (Throwable t) {
            throw new GpServerException("Error initializing batch params", t);
        }
        if (batchParamMap==null || batchParamMap.size()==0) {
            //it's not a batch job
            return singleJobInputApi.postJob(jobContext, jobInput);
        }
        
        //it is a batch job
        if (batchParamMap.size()==1) {
            String firstBatchJobId=null;
            for(Entry<ParamId,List<ParamValue>> entry : batchParamMap.entrySet()) {
                for(ParamValue value : entry.getValue()) {
                    JobInput nextBatchInput = prepareBatchInput(entry.getKey(), value, jobInput);
                    String nextBatchJobId = singleJobInputApi.postJob(jobContext, nextBatchInput);
                    if (firstBatchJobId==null) {
                        firstBatchJobId=nextBatchJobId;
                    }
                }
            }
            return firstBatchJobId;
        }
        
        //TODO: implement this method
        throw new GpServerException("Batch job submission not implemented for more than one batch parameter!");
    }

}
