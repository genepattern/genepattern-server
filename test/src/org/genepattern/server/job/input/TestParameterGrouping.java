package org.genepattern.server.job.input;

import org.genepattern.junitutil.TaskLoader;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.dm.tasklib.TasklibPath;
import org.genepattern.server.eula.LibdirLegacy;
import org.genepattern.server.eula.LibdirStrategy;
import org.genepattern.server.job.input.LoadModuleHelper;
import org.genepattern.webservice.TaskInfo;
import org.json.JSONArray;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;

/**
 * Created by nazaire on 2/11/14.
 */
public class TestParameterGrouping
{
    final static private String userId="test";
    final static private String adminUserId="admin";
    private static ServerConfiguration.Context userContext;
    private static TaskLoader taskLoader;
    final static private String tBAdvancedParamsLsid ="urn:lsid:8080.nazaire.69.173.118.131:genepatternmodules:6:4";
    final static private String moduleZipFile = "TestBasicAdvancedParameters_v4.zip";

    @BeforeClass
    static public void beforeClass()
    {
        userContext = ServerConfiguration.Context.getContextForUser(adminUserId);
        userContext.setIsAdmin(true);

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

        File paramGroupsFile = new File("paramgroups.json");
        TaskUtil.writeSupportFileToFile(paramGroupsInputStream, paramGroupsFile);

        LoadModuleHelper loadModuleHelper = new LoadModuleHelper(userContext);
        JSONArray paramGroupsJson = loadModuleHelper.getParameterGroupsJson(taskInfo, paramGroupsFile);

        //check that there were three parameter groups defined
        Assert.assertEquals(paramGroupsJson.length(), 3);
    }
}
