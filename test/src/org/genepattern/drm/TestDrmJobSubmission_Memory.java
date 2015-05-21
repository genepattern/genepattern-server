package org.genepattern.drm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.TaskInfo;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for getMemory() on DrmJobSubmission.
 * 
 * @author pcarr
 *
 */
public class TestDrmJobSubmission_Memory {
    GpConfig gpConfig;
    GpContext jobContext;

    // mock commandLine= in manifest file
    final String javaCmdLine="<java> <java_flags> -jar <libdir>DemoJava.jar";
    // mock runtime command array, after subsitutions, before applying -Xmx customization
    final String[] javaCmdArgs={"java", "-Xmx512m", "-jar", "/mock/libdir/DemoJava.jar"};
    
    /**
     * Set up basic use-case, the server is configured with job.memory, job.javaXmxMin and job.javaXmxPad.
     * initialize queue memory for a java module.
     */
    @Before
    public void setUp() {
        gpConfig=mock(GpConfig.class);
        jobContext=mockJobContext("DemoModule", javaCmdLine);

        // default mock configuration ... 
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(Memory.fromString("4 Gb"));
        //when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX)).thenReturn("256 Mb");
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX_MIN)).thenReturn(Memory.fromString("1 Gb"));
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX_PAD)).thenReturn(Memory.fromString("3 Gb"));
    }
    
    protected static GpContext mockJobContext(String taskName, String taskCommandLine) {
        GpContext jobContext=mock(GpContext.class);
        JobInfo jobInfo = mock(JobInfo.class);
        TaskInfo taskInfo = createTask(taskName, taskCommandLine);
        when(jobContext.getTaskInfo()).thenReturn(taskInfo);
        when(jobContext.getJobInfo()).thenReturn(jobInfo);
        return jobContext;
    }

    protected static TaskInfo createTask(final String name, final String cmdLine) {
        TaskInfo mockTask=new TaskInfo();
        mockTask.setName(name);
        mockTask.giveTaskInfoAttributes();
        mockTask.getTaskInfoAttributes().put(GPConstants.LSID, "");
        mockTask.getTaskInfoAttributes().put(GPConstants.TASK_TYPE, "Test");
        mockTask.getTaskInfoAttributes().put(GPConstants.COMMAND_LINE, cmdLine);
        return mockTask;
    }
    
    @Test
    public void initMemoryInDrmJobSubmission() {
        File workingDir=new File("jobResults/0");
        DrmJobSubmission job=new DrmJobSubmission.Builder(workingDir)
            .gpConfig(gpConfig)
            .jobContext(jobContext)
            .commandLine(javaCmdArgs)
        .build();
        assertEquals("java module with 'job.javaXmxPad'", Memory.fromString("7 Gb"), job.getMemory());
        assertEquals("numBytes", Memory.fromString("7 Gb").getNumBytes(), job.getMemory().getNumBytes());
    }
    
    //special-cases when job.javaXmxMin is involved
    @Test
    public void initMemory_xmxMin() {
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(Memory.fromString("512m"));
        assertEquals("job.memory < job.javaXmxMin", Memory.fromString("4 Gb"), 
                DrmJobSubmission.initQueueMemory(gpConfig, jobContext));
    }

    @Test
    public void initMemory_xmxMin_noPad() {
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(Memory.fromString("512m"));
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX_PAD)).thenReturn(null);
        assertEquals("job.memory < job.javaXmxMin; no padding", Memory.fromString("1 Gb"), 
                DrmJobSubmission.initQueueMemory(gpConfig, jobContext));
    }
    
    //special-cases when job.javaXmx is set 
    // ... with no padding ...
    @Test
    public void initMemory_xmx_greater_than() { 
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(Memory.fromString("1 Gb"));
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX)).thenReturn(Memory.fromString("3 Gb"));
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX_MIN)).thenReturn(null);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX_PAD)).thenReturn(null);
        assertEquals("javaXmx > memory", Memory.fromString("3 Gb"), 
                DrmJobSubmission.initQueueMemory(gpConfig, jobContext));
    }

    @Test
    public void initMemory_xmx_less_than() { 
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(Memory.fromString("3 Gb"));
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX)).thenReturn(Memory.fromString("1 Gb"));
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX_MIN)).thenReturn(null);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX_PAD)).thenReturn(null);
        assertEquals("xmx < memory", Memory.fromString("3 Gb"), 
                DrmJobSubmission.initQueueMemory(gpConfig, jobContext));
    }

    // ... with padding ...
    @Test
    public void initMemory_xmxPadded_greater_than() { 
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX)).thenReturn(Memory.fromString("2 Gb"));
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX_MIN)).thenReturn(null);
        assertEquals("xmx+pad > memory", Memory.fromString("5 Gb"), 
                DrmJobSubmission.initQueueMemory(gpConfig, jobContext));
    }

    @Test
    public void initMemory_xmxPadded_less_than() { 
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(Memory.fromString("8 Gb"));
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX)).thenReturn(Memory.fromString("2 Gb"));
        assertEquals("xmx+pad < memory", Memory.fromString("8 Gb"), 
                DrmJobSubmission.initQueueMemory(gpConfig, jobContext));
    }

    @Test
    public void initQueueMem_no_mem_config() {
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(null);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX)).thenReturn(null);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX_MIN)).thenReturn(null);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX_PAD)).thenReturn(null);
        assertEquals((Memory)null, DrmJobSubmission.initQueueMemory(gpConfig, jobContext));
    }

    @Test
    public void initQueueMem_no_mem_config_except_padding() {
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(null);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX)).thenReturn(null);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX_MIN)).thenReturn(null);
        assertEquals(Memory.fromString("3 Gb"), DrmJobSubmission.initQueueMemory(gpConfig, jobContext));
    }

    @Test
    public void initQueueMem_no_mem_config_except_xmxMin() {
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(null);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX)).thenReturn(null);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX_PAD)).thenReturn(null);
        assertEquals(Memory.fromString("1 Gb"), DrmJobSubmission.initQueueMemory(gpConfig, jobContext));
    }

    @Test
    public void initQueueMem_nonJavaModule() {
        GpContext jobContext=mockJobContext("DemoModule", "echo <arg>");
        // using new jobContext, need to int the mock settings ...
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(Memory.fromString("4g"));
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX)).thenReturn(Memory.fromString("8g"));
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX_PAD)).thenReturn(Memory.fromString("3 Gb"));

        assertEquals("not a java module", Memory.fromString("4g"), DrmJobSubmission.initQueueMemory(gpConfig, jobContext));
    }

}
