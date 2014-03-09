package org.genepattern.server.config;

import java.io.File;

import org.genepattern.drm.TestDrmJobSubmission;
import org.genepattern.junitutil.ConfigUtil;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.executor.CommandExecutorMapper;
import org.genepattern.webservice.TaskInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * junit tests for custom values for the 'executor' in the config yaml file.
 * @author pcarr
 *
 */
public class TestGetExecutorId {
    private GpConfig gpConfig;
    
    @Before
    public void before() throws Throwable {
        File resourcesDir=FileUtil.getSourceDir(this.getClass());
        File configYaml=FileUtil.getSourceFile(TestDrmJobSubmission.class, "drm_test.yaml");
        this.gpConfig=new GpConfig.Builder()
            .resourcesDir(resourcesDir)
            .configFile(configYaml)
            .build();
        if (this.gpConfig.hasInitErrors()) {
            throw this.gpConfig.getInitializationErrors().get(0);
        }
        ConfigUtil.setUserGroups(this.getClass(), "userGroups.xml");
    }
    
    @Test
    public void testDefaultExecutor() {   
        GpContext gpContext=new GpContextFactory.Builder()
            .userId("test")
            .build();
        
        Assert.assertEquals("executor for 'test'", "RuntimeExec", gpConfig.getGPProperty(gpContext, "executor"));
    }
    
    @Test
    public void testPipelineExecutor() {
        final File zipFile=FileUtil.getDataFile("modules/testPipelineGolubNoViewers.zip");
        final TaskInfo taskInfo=TaskUtil.getTaskInfoFromZip(zipFile);
        GpContext gpContext=new GpContextFactory.Builder()
            .userId("test")
            .taskInfo(taskInfo)
            .build();
        
        Assert.assertEquals("pipeline.executor", CommandExecutorMapper.PIPELINE_EXEC_ID, gpConfig.getExecutorId(gpContext)); 
    }
    
    @Test
    public void testExecutorForGroup() {
        GpContext gpContext=new GpContextFactory.Builder()
            .userId("adminuser")
            .build();
        Assert.assertEquals("executor for 'adminuser' in group 'admingroup'", "AdminGroupJobRunner", gpConfig.getGPProperty(gpContext, "executor"));
    }
    
    @Test
    public void testExecutorForUser() {
        GpContext gpContext=new GpContextFactory.Builder()
            .userId("userA")
            .build();
        Assert.assertEquals("executor for 'userA'", "UserAJobRunner", gpConfig.getGPProperty(gpContext, "executor"));
    }
    
    @Test
    public void testExecutorViaExecutorPropertiesByUser() {
        GpContext gpContext=new GpContextFactory.Builder()
            .userId("userB")
            .build();
        Assert.assertEquals("executor for 'userB'", "DemoPbsJobRunner", gpConfig.getGPProperty(gpContext, "executor"));
        
    }
    
    @Test
    public void testExecutorViaExecutorPropertiesByGroup() {
        GpContext gpContext=new GpContextFactory.Builder()
            .userId("Broadie C")
            .build();
        Assert.assertEquals("executor for 'Broadie C' in group 'broadgroup'", "BroadGroupJobRunner", gpConfig.getGPProperty(gpContext, "executor"));
    }

}
