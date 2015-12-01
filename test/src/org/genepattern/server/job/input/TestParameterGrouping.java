/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input;

import java.io.File;
import java.io.InputStream;

import org.genepattern.junitutil.TaskLoader;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.webservice.TaskInfo;
import org.json.JSONArray;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

/**
 * Created by nazaire on 2/11/14.
 */
public class TestParameterGrouping
{
    final static private String adminUserId="admin";
    private static GpConfig gpConfig;
    private static GpContext userContext;
    private static TaskLoader taskLoader;
    final static private String tBAdvancedParamsLsid ="urn:lsid:8080.nazaire.69.173.118.131:genepatternmodules:6:4";
    final static private String moduleZipFile = "TestBasicAdvancedParameters_v4.zip";
    
    @Rule
    public TemporaryFolder temp= new TemporaryFolder();

    @BeforeClass
    static public void beforeClass()
    {
        gpConfig=Mockito.mock(GpConfig.class);
        userContext=new GpContext.Builder()
            .userId(adminUserId)
            .isAdmin(true)
            .build();

        taskLoader=new TaskLoader();
        taskLoader.addTask(TestParameterGrouping.class, moduleZipFile);
    }

    @Test
    public void testNumParamGroups() throws Exception
    {
        TaskInfo taskInfo = taskLoader.getTaskInfo(tBAdvancedParamsLsid);

        InputStream paramGroupsInputStream = TaskUtil.getSupportFileFromZip(
                TestParameterGrouping.class, moduleZipFile, "paramgroups.json");

        if(paramGroupsInputStream == null)
        {
           Assert.fail("Could not open file paramgroups.json");
        }

        File paramGroupsFile = temp.newFile("paramgroups.json");
        TaskUtil.writeSupportFileToFile(paramGroupsInputStream, paramGroupsFile);

        LoadModuleHelper loadModuleHelper = new LoadModuleHelper(gpConfig, userContext, taskLoader);
        JSONArray paramGroupsJson = loadModuleHelper.getParameterGroupsJson(taskInfo, paramGroupsFile);

        //check that there were three parameter groups defined
        Assert.assertEquals(paramGroupsJson.length(), 3);
    }
}
