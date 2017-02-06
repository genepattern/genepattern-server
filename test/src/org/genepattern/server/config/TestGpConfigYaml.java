package org.genepattern.server.config;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;

import org.genepattern.drm.JobRunner;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.webservice.TaskInfo;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test site customization in config_yaml file.
 * @author pcarr
 *
 */
public class TestGpConfigYaml {
    private static GpConfig configExample;
    private static GpContext jobContext_default;
    private static TaskInfo cleTask;
    private static TaskInfo testStep;
    
    /** utility method: create new GpConfig instance from given config_yaml file */
    protected static final GpConfig initGpConfig(final String filename) throws Throwable {
        final URL url=TestGpConfigYaml.class.getResource(filename);
        if (url==null) {
            throw new Exception("failed to getResource("+filename+")");
        }
        final File configFile=new File(url.getFile());
        final GpConfig gpConfig=new GpConfig.Builder()
            .configFile(configFile)
        .build();
        if (gpConfig.hasInitErrors()) {
            throw gpConfig.getInitializationErrors().get(0);
        }
        return gpConfig;
    }

    @BeforeClass
    public static void beforeClass() throws Throwable {
        cleTask=new TaskInfo();
        cleTask.setName("ConvertLineEndings");
        testStep=new TaskInfo();
        testStep.setName("TestStep");
        configExample=initGpConfig("config_example_test.yaml");
    }
    
    @Before
    public void setUp() throws Throwable {
        jobContext_default=new GpContext.Builder()
            .userId("test_user")
            .taskInfo(cleTask)
        .build();
    }
    
    @Test
    public void jobPriority_default() {
        assertEquals("job.priority", 
                null, 
                configExample.getGPBigDecimalProperty(jobContext_default, JobRunner.PROP_PRIORITY));        
    }

    @Test 
    public void jobPriority_perModule() throws Throwable {
        final GpContext jobContext_custom=new GpContext.Builder()
            .userId("test_user")
            .taskInfo(testStep)
        .build();
        assertEquals("getGPBooleanProperty(job.priority)", 
                new BigDecimal("0.5"), 
                configExample.getGPBigDecimalProperty(jobContext_custom, JobRunner.PROP_PRIORITY));
    }

    @Test
    public void jobGeClear_default() throws Throwable { 
        assertEquals("job.ge.clear", 
                false, 
                configExample.getGPBooleanProperty(jobContext_default, "job.ge.clear"));
    }
    
    @Test 
    public void jobGeClear_perModule() throws Throwable {
        final GpContext jobContext_custom=new GpContext.Builder()
            .userId("test_user")
            .taskInfo(testStep)
        .build();
        assertEquals("getGPBooleanProperty(job.ge.clear)", 
                true, 
                configExample.getGPBooleanProperty(jobContext_custom, "job.ge.clear"));
    }
    
    @Test 
    public void jobGeClear_perUser() throws Throwable {
        final GpContext jobContext_custom=new GpContext.Builder()
            .userId("custom_user")
        .build();
        assertEquals("getGPBooleanProperty(job.ge.clear)", 
                true, 
                configExample.getGPBooleanProperty(jobContext_custom, "job.ge.clear"));
    }
    

}
