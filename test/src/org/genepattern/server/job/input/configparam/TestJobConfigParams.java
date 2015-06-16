/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.configparam;

import java.io.File;

import org.genepattern.drm.JobRunner;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.job.input.choice.ChoiceInfoHelper;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * junit tests for the JobConfigParams helper class.
 * @author pcarr
 *
 */
public class TestJobConfigParams {
    private final String userId="test_user";
    private File resourcesDir=null;
    
    @Before
    public void before() {
        resourcesDir=FileUtil.getSourceDir(this.getClass());
    }

    @Test
    public void testNullGpConfig() {
        final GpConfig gpConfig=null;
        final GpContext gpContext=new GpContext.Builder().build();
        JobConfigParams jobConfigParams=JobConfigParams.initJobConfigParams(gpConfig, gpContext);
        Assert.assertNull("When gpConfig is null, return null", jobConfigParams);
    }
    
    @Test
    public void testDefault() {
        final File zipFile=FileUtil.getDataFile("modules/ComparativeMarkerSelection_v9.zip");
        final TaskInfo taskInfo=TaskUtil.getTaskInfoFromZip(zipFile);
        final GpContext taskContext=new GpContext.Builder()
            .userId(userId)
            .taskInfo(taskInfo)
            .build();

        final GpConfig gpConfig=new GpConfig.Builder().build();
        JobConfigParams jobConfigParams=JobConfigParams.initJobConfigParams(gpConfig, taskContext);
        Assert.assertNull("by default, the jobConfigParams should be null", jobConfigParams);
    }
    
    /**
     * Test for GP-5001, special case when the module manifest sets a config param, 'job.memory=6gb',
     * which doesn't match an entry in the custom 'job.memory' drop-down. 
     */
    @Test
    public void testCustomMemoryDropdown() {
        // 'job.memory' drop-down set via 'job.inputParams' in the config file.
        final GpConfig gpConfig=new GpConfig.Builder()
            .configFile(new File(resourcesDir, "config_custom.yaml"))
            .build();
        
        final TaskInfo mockTask=new TaskInfo();
        mockTask.setName("JavaEcho");
        mockTask.giveTaskInfoAttributes();
        mockTask.getTaskInfoAttributes().put(GPConstants.LSID, "");
        mockTask.getTaskInfoAttributes().put(GPConstants.TASK_TYPE, "Test");
        mockTask.getTaskInfoAttributes().put(GPConstants.COMMAND_LINE, "<java> -cp <libdir>Echo.jar Echo <arg1>");
        // module declares 'job.memory=6gb'
        mockTask.getTaskInfoAttributes().put(JobRunner.PROP_MEMORY, "6gb");

        final GpContext taskContext=new GpContext.Builder()
            .userId(userId)
            .taskInfo(mockTask)
            .build();

        JobConfigParams jobConfigParams=JobConfigParams.initJobConfigParams(gpConfig, taskContext);
        ParameterInfo memInfo=jobConfigParams.getParam(JobRunner.PROP_MEMORY);
        Assert.assertEquals("default memory", "6gb", memInfo.getDefaultValue());
        Assert.assertEquals("numChoices", 8, memInfo.getChoices().size());
        Assert.assertEquals("1st", "6gb", ChoiceInfoHelper.initChoiceInfo(memInfo).getChoices().get(0).getValue());
    }

}
