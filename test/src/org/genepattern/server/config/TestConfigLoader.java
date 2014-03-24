package org.genepattern.server.config;

import java.io.File;

import org.genepattern.drm.TestDrmJobSubmission;
import org.genepattern.junitutil.FileUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * junit tests for initializing the GpConfig instance.
 * @author pcarr
 *
 */
public class TestConfigLoader {
    
    private String printContext(final GpContext context) {
        if (context==null) {
            return "context=null";
        }
        StringBuffer buf=new StringBuffer();
        buf.append("userId="+context.getUserId());
        if (context.getTaskInfo() != null) {
            buf.append(",taskInfo.name="+context.getTaskInfo().getName());
        }
        if (context.getJobInfo() != null) {
            buf.append(",jobInfo.taskName="+context.getJobInfo().getTaskName());
        }
        return buf.toString();
    }

    private void doTest(final GpConfig gpConfig, final GpContext context, final String prop, final String expected) {
        String message="for "+printContext(context)+" expecting "+prop+"="+expected;
        Value value=gpConfig.getValue(context, prop);
        //Value value=ServerConfigurationFactory.instance().getValue(context, prop);
        Assert.assertEquals(message, expected, value.getValue());
    }

    @Test
    public void testFromSystemProps() {
        GpConfig config=GpConfigLoader.createFromSystemProps();
        Assert.assertNull("expecting null config.resourcesDir", config.getResourcesDir());
        Assert.assertEquals("num init errors", 2, config.getInitializationErrors().size());
    }
    
    @Test
    public void testFromResourcesDir() {
        File resourcesDir=FileUtil.getSourceDir(this.getClass());
        GpConfig config=GpConfigLoader.createFromResourcesDir(resourcesDir);
        Assert.assertEquals("resourcesDir", resourcesDir, config.getResourcesDir());
    }
    
    @Test
    public void testCustomConfigFile() {
        GpContext gpContext=new GpContextFactory.Builder().build();
        File configYaml=FileUtil.getSourceFile(this.getClass(), "test_module_properties.yaml");
        GpConfig gpConfig=GpConfigLoader.createFromConfigYaml(configYaml);
        Value value=gpConfig.getValue(gpContext, "test.prop");
        Assert.assertEquals(new Value("DEFAULT_VALUE"), value);
    }
    
    @Test
    public void testDrmConfig() {   
        File configYaml=FileUtil.getSourceFile(TestDrmJobSubmission.class, "drm_test.yaml");
        GpContext gpContext=new GpContextFactory.Builder()
            .userId("test_user")
            .build();
        GpConfig gpConfig=GpConfigLoader.createFromConfigYaml(configYaml);
        Assert.assertEquals("test_user.executor.props", "PbsBigMem", gpConfig.getGPProperty(gpContext, "executor.props"));
        Assert.assertEquals("test_user.executor", "DemoPbsJobRunner", gpConfig.getGPProperty(gpContext, "executor"));
        Assert.assertEquals("test_user.job.queue", "pbsBigMemQueue", gpConfig.getGPProperty(gpContext, "job.queue"));
    }

}
