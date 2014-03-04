package org.genepattern.drm;

import java.io.File;


import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.GpContextFactory;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.configparam.JobConfigParams;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.TaskInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPbsDemoConfig {
    private static TaskInfo cleTaskInfo=null;
    private static TaskInfo exampleTaskInfo=null;
    
    @BeforeClass
    public static final void beforeClass() {
        final File cleZip=FileUtil.getDataFile("modules/ConvertLineEndings_v2.zip");
        cleTaskInfo=TaskUtil.getTaskInfoFromZip(cleZip);
        
        exampleTaskInfo=new TaskInfo();
        exampleTaskInfo.setName("ExampleModule01");
        exampleTaskInfo.giveTaskInfoAttributes();
        exampleTaskInfo.getTaskInfoAttributes().put(GPConstants.LSID, "");
    }
    
    private GpConfig gpConfig;

    
    @Before
    public void beforeTest() throws Exception {
        final File resourcesDir=FileUtil.getSourceDir(this.getClass());
        if (!resourcesDir.canRead()) {
            throw new Exception("Can't read resourcesDir="+resourcesDir);
        }
        final File configYaml=FileUtil.getSourceFile(this.getClass(), "demo_pbs_config.yaml");
        if (!configYaml.canRead()) {
            throw new Exception("Can't read configYaml="+configYaml);
        }
        this.gpConfig = new GpConfig.Builder()
            .resourcesDir(resourcesDir)
            .configFile(configYaml)
            .build();
    }
    
    @Test
    public void testExtraInputParams() {
        GpContext gpContext=GpContextFactory.createContextForUser("test");
        final String executor=gpConfig.getExecutorId(gpContext);
        Assert.assertEquals("DemoPbsJobRunner", executor);
        Assert.assertEquals(
                "for a default user",
                "pbs_extra_input_params.yaml", 
                gpConfig.getGPProperty(gpContext, "executor.inputParams"));
        
        JobConfigParams jobConfigParams=JobConfigParams.initJobConfigParams(gpConfig, gpContext);
        Assert.assertNotNull("Expecting non-null executor.inputParams for default user", jobConfigParams);
        
        Assert.assertEquals("executor.inputParams.size", 4, jobConfigParams.getParams().size());
        Assert.assertEquals("inputParams[0].name", "drm.cpuCount", 
                jobConfigParams.getParams().get(0).getName());
        Assert.assertEquals("inputParams[1].name", "pbs.vmem", 
                jobConfigParams.getParams().get(1).getName());
        Assert.assertEquals("inputParams[2].name", "drm.walltime", 
                jobConfigParams.getParams().get(2).getName());
        Assert.assertEquals("inputParams[3].name", "example.dropdown", 
                jobConfigParams.getParams().get(3).getName());
    }
    
    @Test
    public void testChangeDefaultByModule() {
        //final String cleLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2";
        //for a default user, about to run ConvertLineEndings
        GpContext gpContext=new GpContextFactory.Builder()
            .userId("test")
            .taskInfo(cleTaskInfo)
            .build();

        JobConfigParams jobConfigParams=JobConfigParams.initJobConfigParams(gpConfig, gpContext);
        Assert.assertNull("RuntimeExec does not have exectuor.inputParams", jobConfigParams);
    }
    
    @Test
    public void testExecutorDefaults() {
        GpContext gpContext=new GpContextFactory.Builder()
            .userId("test")
            .build();
        
        Assert.assertEquals("executor.inputParams", "pbs_extra_input_params.yaml",  
                gpConfig.getGPProperty(gpContext, "executor.inputParams"));
        Assert.assertEquals("drm.queue", "batch",  
                gpConfig.getGPProperty(gpContext, "drm.queue"));
        Assert.assertEquals("drm.walltime", "02:00:00",  
                gpConfig.getGPProperty(gpContext, "drm.walltime"));
        Assert.assertEquals("drm.cpuCount", "1",  
                gpConfig.getGPProperty(gpContext, "drm.cpuCount"));
        Assert.assertEquals("pbs.host", "default.pbshost.genepattern.org",  
                gpConfig.getGPProperty(gpContext, "pbs.host"));
        Assert.assertEquals("pbs.mem", "",  
                gpConfig.getGPProperty(gpContext, "pbs.mem"));
        Assert.assertEquals("pbs.ppn", "8",  
                gpConfig.getGPProperty(gpContext, "pbs.ppn"));
        Assert.assertEquals("pbs.cput", "",  
                gpConfig.getGPProperty(gpContext, "pbs.cput"));
        Assert.assertEquals("pbs.vmem", "64gb",  
                gpConfig.getGPProperty(gpContext, "pbs.vmem"));

    }
    
    @Test
    public void testCustomExecutorProperties_himem() {
        GpContext gpContext=new GpContextFactory.Builder()
            .userId("test")
            .taskInfo(exampleTaskInfo)
            .build();

        Assert.assertEquals("custom 'drm.cpuCount'", (Integer) 4, 
                gpConfig.getGPIntegerProperty(gpContext, "drm.cpuCount"));
        Assert.assertEquals("custom 'pbs.vmem'", "320gb", 
                gpConfig.getGPProperty(gpContext, "pbs.vmem"));
        
        Memory pbsVmem=Memory.fromString(gpConfig.getGPProperty(gpContext, "pbs.vmem"));
        Assert.assertEquals("validate pbs.vmem", 320, pbsVmem.numGb(), 0.01);

    }
    
    @Test
    public void testExecutorPropertiesFromJobInput() {
        JobInput jobInput=new JobInput();
        jobInput.addValue("pbs.vmem", "120gb");
        
        GpContext gpContext=new GpContextFactory.Builder()
            .userId("test")
            .taskInfo(exampleTaskInfo)
            .jobInput(jobInput)
            .build();
        
        Assert.assertEquals("'pbs.vmem' from job input", "120gb", 
                gpConfig.getGPProperty(gpContext, "pbs.vmem"));
    }

}
