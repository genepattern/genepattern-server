package org.genepattern.drm.impl.lsf.core;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.Arrays;

import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.drm.JobRunner;
import org.genepattern.drm.Memory;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.junit.Before;
import org.junit.Test;

import edu.mit.broad.core.lsf.LsfJob;

/**
 * junit tests for the virtual queue feature of the Lsf Runner.
 * @author pcarr
 *
 */
public class TestVirtualQueue {
    GpConfig gpConfig;
    File jobDir;
    JobInfo topHatJobInfo;
    TaskInfo topHatTaskInfo;
    JobInfo cleJobInfo;
    TaskInfo cleTaskInfo;

    CmdLineLsfRunner lsfRunner;

    @Before
    public void setUp() {
        // use the example config file from resources directory
        File configFile=new File("resources/config_example_virtual_queue.yaml");
        gpConfig=new GpConfig.Builder()
            .configFile(configFile)
        .build();
        
        this.jobDir=new File("jobResults/1");

        this.topHatJobInfo=mock(JobInfo.class);
        when(topHatJobInfo.getTaskName()).thenReturn("TopHat");
        when(topHatJobInfo.getTaskLSID()).thenReturn("urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00230:8.6");

        this.topHatTaskInfo=mock(TaskInfo.class);
        when(topHatTaskInfo.getName()).thenReturn("TopHat");
        when(topHatTaskInfo.getLsid()).thenReturn("urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00230:8.6");
        when(topHatTaskInfo.giveTaskInfoAttributes()).thenReturn(new TaskInfoAttributes());
        
        this.cleJobInfo=mock(JobInfo.class);
        this.cleTaskInfo=mock(TaskInfo.class);
        
        lsfRunner = new CmdLineLsfRunner();
    }
    
//    @Test
//    public void jobMemory() {
//        DrmJobSubmission customMemJob=mock(DrmJobSubmission.class);
//        when(customMemJob.getGpConfig()).thenReturn(gpConfig);
//        when(customMemJob.getWorkingDir()).thenReturn(jobDir);
//        when(customMemJob.getCpuCount()).thenReturn(null);
//        when(customMemJob.getNodeCount()).thenReturn(null);
//        when(customMemJob.getMemory()).thenReturn(Memory.fromString("12Gb"));
//        LsfJob lsfJob = lsfRunner.initLsfJob(customMemJob);
//        assertEquals(
//            "job.memory:  12Gb",
//            Arrays.asList(new String[]{"-R", "rusage[mem=12]", "-M", "12"}),
//            lsfJob.getExtraBsubArgs());
//    }

    
    /**
     * Get LSF command line when the default 'job.virtualQueue'
     */
    @Test
    public void defaultVirtualQueue() {
        GpContext cleJobContext=mock(GpContext.class);
        when(cleJobContext.getJobInfo()).thenReturn(cleJobInfo);
        when(cleJobContext.getTaskInfo()).thenReturn(cleTaskInfo);
        DrmJobSubmission cleJob=new DrmJobSubmission.Builder(jobDir)
            .gpConfig(gpConfig)
            .jobContext(cleJobContext)
        .build();

        assertEquals("job.queue for cleJob", "genepattern", cleJob.getValue(JobRunner.PROP_QUEUE).getValue());
        assertEquals("job.virtualQueue for cleJob", "genepattern_short", cleJob.getValue(JobRunner.PROP_VIRTUAL_QUEUE).getValue());
        
        LsfJob lsfJob=lsfRunner.initLsfJob(cleJob);
        assertEquals("lsfJob.queue", "genepattern",  lsfJob.getQueue());
        assertEquals(
                "lsfJob.extraBsubArgs",
                Arrays.asList(new String[]{"-R", "rusage[mem=2]", "-M", "2"}),
                lsfJob.getExtraBsubArgs());
    }

    /**
     * Get LSF command line values when a custom 'job.virtualQueue'.
     */
    @Test
    public void customVirtualQueue() {
        //setUp the test
        GpContext topHatJobContext=mock(GpContext.class);
        when(topHatJobContext.getJobInfo()).thenReturn(topHatJobInfo);
        when(topHatJobContext.getTaskInfo()).thenReturn(topHatTaskInfo);

        DrmJobSubmission topHatJob=new DrmJobSubmission.Builder(jobDir)
            .gpConfig(gpConfig)
            .jobContext(topHatJobContext)
        .build();
        
        //verify the config file
        assertEquals("job.queue from gpConfig", "genepattern", gpConfig.getValue(topHatJobContext, JobRunner.PROP_QUEUE).getValue());
        assertEquals("executor.props from gpConfig", "genepattern_long", gpConfig.getValue(topHatJobContext, "executor.props").getValue());
        assertEquals("job.virtualQueue from gpConfig", "genepattern_long", gpConfig.getValue(topHatJobContext, JobRunner.PROP_VIRTUAL_QUEUE).getValue());
        
        assertEquals("job.queue for topHatJob", "genepattern", topHatJob.getValue(JobRunner.PROP_QUEUE).getValue());
        assertEquals("job.virtualQueue for topHatJob", "genepattern_long", topHatJob.getValue(JobRunner.PROP_VIRTUAL_QUEUE).getValue());
        
        
        LsfJob lsfJob=lsfRunner.initLsfJob(topHatJob);
        assertEquals("lsfJob.queue", "genepattern",  lsfJob.getQueue());
        assertEquals(
                "lsfJob.extraBsubArgs",
                Arrays.asList(new String[]{"-R", "rusage[mem=2]", "-M", "2", "-g", "/genepattern/gpprod/long",  "-m", "node1448 node1449 node1450 node1451 node1452 node1453 node1454 node1455"}),
                lsfJob.getExtraBsubArgs());
    }
    
    @Test
    public void customVirtualQueue_customMemory() {
        JobInfo myJobInfo=mock(JobInfo.class);
        when(myJobInfo.getTaskName()).thenReturn("MyGpLongCustomMem");
        when(myJobInfo.getTaskLSID()).thenReturn("urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:10999:1");

        TaskInfo myTaskInfo=mock(TaskInfo.class);
        when(topHatTaskInfo.getName()).thenReturn("MyGpLongCustomMem");
        when(topHatTaskInfo.getLsid()).thenReturn("urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:10999:1");
        when(topHatTaskInfo.giveTaskInfoAttributes()).thenReturn(new TaskInfoAttributes());

        
        GpContext myJobContext=mock(GpContext.class);
        when(myJobContext.getJobInfo()).thenReturn(myJobInfo);
        when(myJobContext.getTaskInfo()).thenReturn(myTaskInfo);

        DrmJobSubmission myJob=new DrmJobSubmission.Builder(jobDir)
            .gpConfig(gpConfig)
            .jobContext(myJobContext)
        .build();

        assertEquals("job.queue for myJob", "genepattern", myJob.getValue(JobRunner.PROP_QUEUE).getValue());
        assertEquals("job.virtualQueue for myJob", "genepattern_long", myJob.getValue(JobRunner.PROP_VIRTUAL_QUEUE).getValue());
        assertEquals("myJob.getMemory", Memory.fromString("12 Gb"), myJob.getMemory()); 
        
        LsfJob lsfJob=lsfRunner.initLsfJob(myJob);
        assertEquals("lsfJob.queue", "genepattern",  lsfJob.getQueue());
        assertEquals(
                "lsfJob.extraBsubArgs",
                Arrays.asList(new String[]{"-R", "rusage[mem=12]", "-M", "12", "-g", "/genepattern/gpprod/long",  "-m", "node1448 node1449 node1450 node1451 node1452 node1453 node1454 node1455"}),
                lsfJob.getExtraBsubArgs());
    }


}
