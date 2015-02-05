package org.genepattern.server.config;

import static junit.framework.Assert.assertEquals;

import java.io.File;

import org.genepattern.drm.JobRunner;
import org.genepattern.drm.Memory;
import org.genepattern.server.job.input.JobInput;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test cases for getting the 'job.memory' flag for a job.
 * @author pcarr
 *
 */
public class TestGetValueFromJobInput {
    private static GpConfig gpConfig;
    
    @BeforeClass
    public static void setUp() throws ConfigurationException {
        final File configFile=new File("resources/config_example_job_memory.yaml");
        gpConfig= new GpConfig.Builder()
            .configFile(configFile)
        .build();
    }

    @Test
    public void defaultMemoryFlag() {
        GpContext jobContext=new GpContext();
        Memory actual=gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY);
        assertEquals("loading 'job.memory' from config", Memory.fromString("2 Gb"), actual);
    }

    @Test
    public void memoryFlagFromJobInput() {
        JobInput jobInput=new JobInput();
        jobInput.addValue(JobRunner.PROP_MEMORY, "8 gb");
        GpContext jobContext=new GpContext();
        jobContext.setJobInput(jobInput);
        Memory actual=gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY);
        assertEquals("setting 'job.memory' as job input parameter", Memory.fromString("8 Gb"), actual);
    }

}
