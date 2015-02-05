package org.genepattern.drm.impl.lsf.broadcore;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;

import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.drm.JobRunner;
import org.genepattern.drm.Memory;
import org.genepattern.server.config.ConfigurationException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.Value;
import org.genepattern.server.executor.lsf.LsfProperties;
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
        when(gpConfig.getGPIntegerProperty(jobContext, JobRunner.PROP_CPU_COUNT))
            .thenReturn(null);
        when(gpConfig.getGPIntegerProperty(jobContext, JobRunner.PROP_NODE_COUNT))
            .thenReturn(null);
        when(gpConfig.getGPIntegerProperty(jobContext, "lsf.cpu.slots"))
            .thenReturn(null);
        initGpJob();
    }
    
    protected void initGpJob() {
        gpJob = new DrmJobSubmission.Builder(jobDir)
            .gpConfig(gpConfig)
            .jobContext(jobContext)
            .build();   
    }
    
    @Test
    public void jobMemory() {
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(Memory.fromString("12Gb"));
        gpJob = new DrmJobSubmission.Builder(jobDir)
            .gpConfig(gpConfig)
            .jobContext(jobContext)
            .build();   

        LsfJob lsfJob = lsfRunner.initLsfJob(gpJob);
        Assert.assertEquals(
            "job.memory:  12Gb",
            Arrays.asList(new String[]{"-R", "rusage[mem=12]", "-M", "12"}),
            lsfJob.getExtraBsubArgs());
    }
    
    @Test
    public void lsfMaxMemory() {
        when(gpJob.getProperty(LsfProperties.Key.MAX_MEMORY.getKey())).thenReturn("12");
        LsfJob lsfJob = lsfRunner.initLsfJob(gpJob);
        Assert.assertEquals(
            "job.memory:  12Gb",
            Arrays.asList(new String[]{"-R", "rusage[mem=12]", "-M", "12"}),
            lsfJob.getExtraBsubArgs());
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
        when(gpConfig.getGPIntegerProperty(jobContext, JobRunner.PROP_NODE_COUNT)).thenReturn(6);
        gpJob = new DrmJobSubmission.Builder(jobDir)
            .gpConfig(gpConfig)
            .jobContext(jobContext)
            .build();   
        LsfJob lsfJob = lsfRunner.initLsfJob(gpJob);
        Assert.assertEquals(
            "job.nodeCount: 6",
            Arrays.asList(new String[]{"-R", "rusage[mem=2]", "-M", "2", "-n", "6"}),
            lsfJob.getExtraBsubArgs());

    }

    @Test
    public void jobCpuCount() { 
        when(gpConfig.getGPIntegerProperty(jobContext, JobRunner.PROP_CPU_COUNT)).thenReturn(6);
        initGpJob();
        LsfJob lsfJob = lsfRunner.initLsfJob(gpJob);
        Assert.assertEquals(
            "job.cpuCount",
            Arrays.asList(new String[]{"-R", "rusage[mem=2]", "-M", "2", "-n", "6"}),
            lsfJob.getExtraBsubArgs());
    }
    
    @Test
    public void lsfCpuSlots() {        
        when(gpConfig.getGPIntegerProperty(jobContext, "lsf.cpu.slots"))
            .thenReturn(6);
        initGpJob();
        LsfJob lsfJob = lsfRunner.initLsfJob(gpJob);
        Assert.assertEquals(
            "lsf.cpu.slots",
            Arrays.asList(new String[]{"-R", "rusage[mem=2]", "-M", "2", "-n", "6"}),
            lsfJob.getExtraBsubArgs());
    }
    
    @Test
    public void jobCpuSlots() {        
        when(gpConfig.getGPIntegerProperty(jobContext, JobRunner.PROP_CPU_COUNT))
            .thenReturn(6);
        initGpJob();
        LsfJob lsfJob = lsfRunner.initLsfJob(gpJob);
        Assert.assertEquals(
            "job.cpuCount",
            Arrays.asList(new String[]{"-R", "rusage[mem=2]", "-M", "2", "-n", "6"}),
            lsfJob.getExtraBsubArgs());
    }
    
    @Test
    public void lsfProject() {
        when(gpConfig.getGPProperty(jobContext, "lsf.project"))
            .thenReturn("gpdev");
        LsfJob lsfJob = lsfRunner.initLsfJob(gpJob);
        Assert.assertEquals(
            "lsf.project",
            "gpdev",
            lsfJob.getProject());
    }
    
    @Test
    public void lsfPriority() {
        when(gpConfig.getGPProperty(jobContext, LsfProperties.Key.PRIORITY.getKey())).thenReturn("50");
        LsfJob lsfJob = lsfRunner.initLsfJob(gpJob);
        Assert.assertEquals(
            "lsf.priority",
            Arrays.asList(new String[]{"-R", "rusage[mem=2]", "-M", "2", "-sp", "50"}),
            lsfJob.getExtraBsubArgs());
    }
    
    @Test
    public void walltime_7days() {
        // 7 days limit
        when(gpConfig.getGPProperty(jobContext, JobRunner.PROP_WALLTIME)).thenReturn("7-00:00:00");
        when(gpConfig.getGPProperty(jobContext, JobRunner.PROP_WALLTIME, null)).thenReturn("7-00:00:00");
        initGpJob();

        LsfJob lsfJob = lsfRunner.initLsfJob(gpJob);
        Assert.assertEquals(
            "job.walltime",
            Arrays.asList(new String[]{"-R", "rusage[mem=2]", "-M", "2", "-W", "168:00" }),
            lsfJob.getExtraBsubArgs());
    }

    @Test
    public void walltime_2hours45min() {
        // 7 days limit
        when(gpConfig.getGPProperty(jobContext, JobRunner.PROP_WALLTIME)).thenReturn("02:45:00");
        when(gpConfig.getGPProperty(jobContext, JobRunner.PROP_WALLTIME, null)).thenReturn("02:45:00");
        initGpJob();

        LsfJob lsfJob = lsfRunner.initLsfJob(gpJob);
        Assert.assertEquals(
            "job.walltime",
            Arrays.asList(new String[]{"-R", "rusage[mem=2]", "-M", "2", "-W", "02:45" }),
            lsfJob.getExtraBsubArgs());
    }
    
    @Test
    public void walltime_45min() {
        // 7 days limit
        when(gpConfig.getGPProperty(jobContext, JobRunner.PROP_WALLTIME)).thenReturn("00:45:00");
        when(gpConfig.getGPProperty(jobContext, JobRunner.PROP_WALLTIME, null)).thenReturn("00:45:00");
        initGpJob();

        LsfJob lsfJob = lsfRunner.initLsfJob(gpJob);
        Assert.assertEquals(
            "job.walltime",
            Arrays.asList(new String[]{"-R", "rusage[mem=2]", "-M", "2", "-W", "00:45" }),
            lsfJob.getExtraBsubArgs());
    }
    
    @Test
    public void lsfExtraBsubArgs() throws ConfigurationException {
        when(gpConfig.getValue(jobContext, "lsf.extra.bsub.args")).thenReturn(
                new Value(Arrays.asList("-g", "/genepattern/gpprod/long", "-m", "node1448 node1449")));
        LsfJob lsfJob = lsfRunner.initLsfJob(gpJob);
        Assert.assertEquals(
            "job.extaArgs",
            Arrays.asList(new String[]{"-R", "rusage[mem=2]", "-M", "2", "-g", "/genepattern/gpprod/long", "-m", "node1448 node1449" }),
            lsfJob.getExtraBsubArgs());
    }

    @Test
    public void jobExtraArgs() throws ConfigurationException {
        when(gpConfig.getValue(jobContext, JobRunner.PROP_EXTRA_ARGS)).thenReturn(
                new Value(Arrays.asList("-g", "/genepattern/gpprod/long", "-m", "node1448 node1449")));
        LsfJob lsfJob = lsfRunner.initLsfJob(gpJob);
        Assert.assertEquals(
            "job.extaArgs",
            Arrays.asList(new String[]{"-R", "rusage[mem=2]", "-M", "2", "-g", "/genepattern/gpprod/long", "-m", "node1448 node1449" }),
            lsfJob.getExtraBsubArgs());
    }
    
}
