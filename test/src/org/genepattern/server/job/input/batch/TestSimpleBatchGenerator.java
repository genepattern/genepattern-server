package org.genepattern.server.job.input.batch;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamValue;
import org.genepattern.server.rest.GpServerException;
import org.junit.Test;

/**
 * Prototype code demonstrating the use of the JobInput API for supporting simple batch jobs.
 * @author pcarr
 *
 */
public class TestSimpleBatchGenerator {
    @Test
    public void testMultipleBatchParams() throws GpServerException {
        JobInput jobInputTemplate=new JobInput();
        jobInputTemplate.addValue("pA", "x", true);
        jobInputTemplate.addValue("pA", "y", true);
        jobInputTemplate.addValue("pA", "z", true);
        jobInputTemplate.addValue("pB", "1", true);
        jobInputTemplate.addValue("pB", "2", true);
        jobInputTemplate.addValue("pB", "3", true);
        jobInputTemplate.addValue("pC", "single", false);
        jobInputTemplate.addValue("pD", "another", false);
        
        assertTrue("jobInputTemplate.isBatchJob", jobInputTemplate.isBatchJob());
        assertEquals("jobInputTemplate.numBatchJobs", 3, jobInputTemplate.getNumBatchJobs());

        GpConfig gpConfig=new GpConfig.Builder().build();
        final GpContext userContext=GpContext.getContextForUser("test_user", false);

        final BatchGenerator simpleBatchGenerator=new SimpleBatchGenerator(gpConfig, userContext);
        
        List<JobInput> batchJobs=simpleBatchGenerator.prepareBatch(jobInputTemplate);
        assertEquals("batchJobs.size", 3, batchJobs.size());
        
        // job 0
        assertEquals("batchJobs[0].pA", "x",       batchJobs.get(0).getParam("pA").getValues().get(0).getValue());
        assertEquals("batchJobs[0].pB", "1",       batchJobs.get(0).getParam("pB").getValues().get(0).getValue());
        assertEquals("batchJobs[0].pC", "single",  batchJobs.get(0).getParam("pC").getValues().get(0).getValue());
        assertEquals("batchJobs[0].pD", "another", batchJobs.get(0).getParam("pD").getValues().get(0).getValue());
        // job 1
        assertEquals("batchJobs[1].pA", "y",       batchJobs.get(1).getParam("pA").getValues().get(0).getValue());
        assertEquals("batchJobs[1].pB", "2",       batchJobs.get(1).getParam("pB").getValues().get(0).getValue());
        assertEquals("batchJobs[1].pC", "single",  batchJobs.get(1).getParam("pC").getValues().get(0).getValue());
        assertEquals("batchJobs[1].pD", "another", batchJobs.get(1).getParam("pD").getValues().get(0).getValue());        
        // job 2
        assertEquals("batchJobs[2].pA", "z",       batchJobs.get(2).getParam("pA").getValues().get(0).getValue());
        assertEquals("batchJobs[2].pB", "3",       batchJobs.get(2).getParam("pB").getValues().get(0).getValue());
        assertEquals("batchJobs[2].pC", "single",  batchJobs.get(2).getParam("pC").getValues().get(0).getValue());
        assertEquals("batchJobs[2].pD", "another", batchJobs.get(2).getParam("pD").getValues().get(0).getValue());        
    }

    @Test
    public void testMismatchBatchParams()
    {
        JobInput jobInputTemplate=new JobInput();
        jobInputTemplate.addValue("pA", "x", true);
        jobInputTemplate.addValue("pA", "y", true);
        jobInputTemplate.addValue("pA", "z", true);
        jobInputTemplate.addValue("pB", "1", true);
        jobInputTemplate.addValue("pB", "2", true);
        jobInputTemplate.addValue("pC", "single", false);
        jobInputTemplate.addValue("pD", "another", false);


        try {
            GpConfig gpConfig = new GpConfig.Builder().build();
            final GpContext userContext = GpContext.getContextForUser("test_user", false);

            final BatchGenerator simpleBatchGenerator = new SimpleBatchGenerator(gpConfig, userContext);

            List<JobInput> batchJobs = simpleBatchGenerator.prepareBatch(jobInputTemplate);
            fail("Expecting GpServerException: Number of batch parameters doesn't match.");
        }
        catch(GpServerException e)
        {
            //expected
        }
    }
}
