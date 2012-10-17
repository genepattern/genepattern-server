package org.genepattern.server.eula;

import java.io.File;
import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.webservice.TaskInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Special-case: if we need to disable the EulaManager, make sure to use a NoOp implementation of the IEulaManager interface.
 * 
 * @author pcarr
 */
public class TestEulaManagerNoOp {
    private static String configFilename="config_EulaManager.disabled.yaml";
    
    @Before
    public void setUp() {
        //load a 'config.yaml' file from the directory which contains this source file
        File configFile=FileUtil.getSourceFile(TestEulaManagerNoOp.class, configFilename);
        String configFilepath=configFile.getAbsolutePath();
        System.setProperty("config.file", configFilepath);
        ServerConfiguration.instance().reloadConfiguration(configFilepath);
    }
    
    @After
    public void tearDown() {
        //revert back to a 'default' config.file
        File configFile=FileUtil.getSourceFile(TestEulaManagerNoOp.class, "config.yaml");
        String configFilepath=configFile.getAbsolutePath();
        System.setProperty("config.file", configFilepath);
        ServerConfiguration.instance().reloadConfiguration(configFilepath);
    }
    
    @Test
    public void testDisableEulaManager() {
        Context userContext=ServerConfiguration.Context.getContextForUser("gp_user");
        boolean customSetting=ServerConfiguration.instance().getGPBooleanProperty(userContext, "org.genepattern.server.eula.EulaManager.enabled", false);
        Assert.assertEquals("eula.enabled", false, customSetting);
    }
    
    @Test
    public void testGetNoOpEulaManager() {
        ServerConfiguration.Context context=new ServerConfiguration.Context();
        IEulaManager eulaMgr=EulaManager.instance(context);
        
        if (eulaMgr instanceof EulaManagerNoOp) {
        }
        else {
            Assert.fail("Expecting instanceof EulaManagerNoOp");
        }
    }
    
    @Test
    public void testSetGetEulaFromTask() {
        final IEulaManager eulaMgr=new EulaManagerNoOp();
        GetEulaFromTask getEulaFromTask=null;
        eulaMgr.setGetEulaFromTask(getEulaFromTask);
    }
    
    @Test
    public void testSetGetTaskStrategy() {
        final IEulaManager eulaMgr=new EulaManagerNoOp();
        GetTaskStrategy getTaskStrategy=null;
        eulaMgr.setGetTaskStrategy(getTaskStrategy);
    }

    @Test
    public void testSetRecordEulaStrategy() {
        final IEulaManager eulaMgr=new EulaManagerNoOp();
        RecordEula recordEula=null;
        eulaMgr.setRecordEulaStrategy(recordEula);
    }
    
    @Test
    public void testRequiresEula() {
        final IEulaManager eulaMgr=new EulaManagerNoOp();
        final Context taskContext=null;
        boolean requiresEula=eulaMgr.requiresEula(taskContext);
        Assert.assertFalse("eulaMgr.requiresEula should always return false when eula manager is not enabled", requiresEula);
    }

    @Test
    public void testGetEula() {
        final IEulaManager eulaMgr=new EulaManagerNoOp();
        final TaskInfo taskInfo=null;
        final List<EulaInfo> eulas=eulaMgr.getEulas(taskInfo);
        Assert.assertEquals("eulaMgr.getEulas should always return an empty list when eula manager is not enabled", 0, eulas.size());
    }

    @Test
    public void testSetEula() {
        final IEulaManager eulaMgr=new EulaManagerNoOp();
        final TaskInfo taskInfo=null;
        final EulaInfo eula=null;
        eulaMgr.setEula(eula, taskInfo);
        //nothing to test
    }

    @Test
    public void testSetEulas() {
        final IEulaManager eulaMgr=new EulaManagerNoOp();
        final TaskInfo taskInfo=null;
        final List<EulaInfo> eulas=Collections.emptyList();
        eulaMgr.setEulas(eulas, taskInfo);
        //nothing to test
    }

    @Test
    public void testGetAllEulaForModule() {
        final IEulaManager eulaMgr=new EulaManagerNoOp();
        final Context taskContext=null;
        List<EulaInfo> eulas=eulaMgr.getAllEulaForModule(taskContext);
        Assert.assertEquals("eulaMgr.allEulaForModule should always return an empty list when eula manager is not enabled", 0, eulas.size());
    }

    @Test
    public void testGetPendingEulaForModule() {
        final IEulaManager eulaMgr=new EulaManagerNoOp();
        final Context taskContext=null;
        List<EulaInfo> eulas=eulaMgr.getPendingEulaForModule(taskContext);
        Assert.assertEquals("eulaMgr.pendingEulaForModule should always return an empty list when eula manager is not enabled", 0, eulas.size());
    }

    @Test
    public void testRecordEula() {
        final IEulaManager eulaMgr=new EulaManagerNoOp();
        final Context taskContext=null;
        eulaMgr.recordEula(taskContext);
    }

}
