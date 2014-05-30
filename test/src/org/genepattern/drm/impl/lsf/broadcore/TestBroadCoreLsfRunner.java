package org.genepattern.drm.impl.lsf.broadcore;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.Arrays;

import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.drm.JobRunner;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.Value;
import org.genepattern.webservice.JobInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import edu.mit.broad.core.lsf.LsfJob;

/**
 * junit tests for the Broad Core LSF library.
 * @author pcarr
 *
 */
public class TestBroadCoreLsfRunner {
    private BroadCoreLsfRunner lsfRunner;
    
    private DrmJobSubmission gpJob;
    private File jobDir;
    private GpConfig gpConfig;
    private GpContext jobContext;
    private JobInfo jobInfo;
    
    @Before
    public void setUp () {
        jobDir = new File("jobResults/1");
        lsfRunner = new BroadCoreLsfRunner();
        jobInfo = mock(JobInfo.class);
        jobContext = GpContext.getContextForJob(jobInfo);
        gpConfig = mock(GpConfig.class);
        Mockito.when(gpConfig.getGPIntegerProperty(jobContext, JobRunner.PROP_CPU_COUNT))
            .thenReturn(null);
        Mockito.when(gpConfig.getGPIntegerProperty(jobContext, JobRunner.PROP_NODE_COUNT))
            .thenReturn(null);
        Mockito.when(gpConfig.getGPIntegerProperty(jobContext, "lsf.cpu.slots"))
            .thenReturn(null);
        gpJob = new DrmJobSubmission.Builder(jobDir)
            .gpConfig(gpConfig)
            .jobContext(jobContext)
            .build();   
    }
    
    @Test
    public void customHostname() {
        //test custom hostname
        Mockito.when(gpConfig.getValue(jobContext, "lsf.hostname")).thenReturn(new Value("node1448 node1449"));
        LsfJob lsfJob = lsfRunner.initLsfJob(gpJob);
        
        Assert.assertEquals(
            "lsf.hostname:  node1448 node1449 (as string)",
            Arrays.asList(new String[]{"-R", "rusage[mem=2]", "-M", "2", "-m", "node1448 node1449"}),
            lsfJob.getExtraBsubArgs());
    }

    @Test
    public void customHostnameAsList() {
        //test custom hostname
        Mockito.when(gpConfig.getValue(jobContext, "lsf.hostname")).thenReturn(
                new Value(Arrays.asList("node1448", "node1449")));
        LsfJob lsfJob = lsfRunner.initLsfJob(gpJob);
        
        Assert.assertEquals(
            "lsf.hostname: [node1448, node1449] (as list)",
            Arrays.asList(new String[]{"-R", "rusage[mem=2]", "-M", "2", "-m", "node1448 node1449"}),
            lsfJob.getExtraBsubArgs());
    }
    
    @Test
    public void customGroup() {
        Mockito.when(gpConfig.getValue(jobContext, "lsf.group")).thenReturn(new Value("/genepattern/gpprod/long"));
        LsfJob lsfJob = lsfRunner.initLsfJob(gpJob);
        Assert.assertEquals(
            "custom -m and -g args",
            Arrays.asList(new String[]{"-R", "rusage[mem=2]", "-M", "2", "-g", "/genepattern/gpprod/long"}),
            lsfJob.getExtraBsubArgs());
    }
    
    @Test
    public void jobNodeCount() { 
        Mockito.when(gpConfig.getGPIntegerProperty(jobContext, JobRunner.PROP_NODE_COUNT)).thenReturn(6);
        LsfJob lsfJob = lsfRunner.initLsfJob(gpJob);
        Assert.assertEquals(
            "job.nodeCount: 6",
            Arrays.asList(new String[]{"-R", "rusage[mem=2]", "-M", "2", "-n", "6"}),
            lsfJob.getExtraBsubArgs());

    }

    @Test
    public void jobCpuCount() { 
        Mockito.when(gpConfig.getGPIntegerProperty(jobContext, JobRunner.PROP_CPU_COUNT)).thenReturn(6);
        LsfJob lsfJob = lsfRunner.initLsfJob(gpJob);
        Assert.assertEquals(
            "job.cpuCount",
            Arrays.asList(new String[]{"-R", "rusage[mem=2]", "-M", "2", "-n", "6"}),
            lsfJob.getExtraBsubArgs());
    }
    
    @Test
    public void lsfCpuSlots() {        
        Mockito.when(gpConfig.getGPIntegerProperty(jobContext, "lsf.cpu.slots"))
            .thenReturn(6);
        LsfJob lsfJob = lsfRunner.initLsfJob(gpJob);
        Assert.assertEquals(
            "lsf.cpu.slots",
            Arrays.asList(new String[]{"-R", "rusage[mem=2]", "-M", "2", "-n", "6"}),
            lsfJob.getExtraBsubArgs());

    }
    
}
