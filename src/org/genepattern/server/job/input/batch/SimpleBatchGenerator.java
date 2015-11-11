package org.genepattern.server.job.input.batch;

import java.util.ArrayList;
import java.util.List;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamValue;
import org.genepattern.server.rest.GpServerException;
import org.genepattern.server.rest.JobReceipt;

/**
 * Created by nazaire on 8/25/15.
 */
public class SimpleBatchGenerator implements BatchGenerator
{
    private final HibernateSessionManager mgr;
    protected final GpConfig gpConfig;
    private final GpContext userContext;

    public SimpleBatchGenerator(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext userContext) {
        this.mgr=mgr;
        this.gpConfig=gpConfig;
        this.userContext=userContext;
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
        BatchSubmitter batchSubmitter = new BatchSubmitterImpl(mgr, gpConfig, userContext);
        JobReceipt receipt= batchSubmitter.submitBatch(batchInputs);
        return receipt;
    }
}
