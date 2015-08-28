package org.genepattern.server.job.input.batch;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamValue;
import org.genepattern.server.rest.GpServerException;
import org.genepattern.server.rest.JobInputApi;
import org.genepattern.server.rest.JobInputApiFactory;
import org.genepattern.server.rest.JobReceipt;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nazaire on 8/25/15.
 */
public class SimpleBatchGenerator implements BatchGenerator
{
    private final GpConfig gpConfig;
    private final GpContext userContext;
    private final JobInputApi jobInputApi;

    public SimpleBatchGenerator(final GpConfig gpConfig, final GpContext userContext) {
        this(gpConfig, userContext, null);
    }
    public SimpleBatchGenerator(final GpConfig gpConfig, final GpContext userContext, final JobInputApi jobInputApiIn)
    {
       this.gpConfig=gpConfig;
        this.userContext=userContext;
        if (jobInputApiIn == null) {
            this.jobInputApi= JobInputApiFactory.createJobInputApi(gpConfig, userContext);
        }
        else {
            this.jobInputApi=jobInputApiIn;
        }
    }

    @Override
    public List<JobInput> prepareBatch(final JobInput batchInputTemplate) throws GpServerException
    {
        List<JobInput> batchJobs=new ArrayList<JobInput>();

        int numBatchJobs=batchInputTemplate.getNumBatchJobs();
        for(int idx=0; idx < numBatchJobs; ++idx)
        {
            JobInput jobInput=new JobInput();
            jobInput.setLsid(batchInputTemplate.getLsid());

            batchJobs.add(jobInput);

            for(final Param param : batchInputTemplate.getParams().values())
            {
                //if this is a batch param then only get the value at the current job index
                if (param.isBatchParam())
                {
                    ParamValue val=param.getValues().get(idx);
                    jobInput.addValue(param.getParamId(), val);
                }
                else
                {
                    //get the list of values for this parameter
                    for(final ParamValue val : param.getValues())
                    {
                        jobInput.addValue(param.getParamId(), val);
                    }
                }
            }
        }
        return batchJobs;
    }

    @Override
    /**
     * Submit your jobs to the GP server.
     * Use the list of JobInput from the prepareBatch() method as input to this method.
     *
     * @param batchInputs
     * @return
     * @throws org.genepattern.server.rest.GpServerException
     */
    public JobReceipt submitBatch(final List<JobInput> batchInputs) throws GpServerException {
        BatchSubmitter batchSubmitter = new BatchSubmitterImpl(gpConfig, userContext, jobInputApi);
        JobReceipt receipt= batchSubmitter.submitBatch(batchInputs);
        return receipt;
    }
}
