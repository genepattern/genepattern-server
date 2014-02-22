package org.genepattern.server.eula;

import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.genepattern.junitutil.ConfigUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
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
    
    @Before
    public void setUp() {
        ConfigUtil.loadConfigFile(this.getClass(), "config_EulaManager.disabled.yaml");
    }
    
    @After
    public void tearDown() {
        //revert back to a 'default' config.file
        ConfigUtil.loadConfigFile(null);
    }
    
    @Test
    public void testDisableEulaManager() {
        GpContext userContext=GpContext.getContextForUser("gp_user");
        boolean customSetting=ServerConfigurationFactory.instance().getGPBooleanProperty(userContext, "org.genepattern.server.eula.EulaManager.enabled", false);
        Assert.assertEquals("eula.enabled", false, customSetting);
    }
    
    @Test
    public void testGetNoOpEulaManager() {
        GpContext context=new GpContext();
        IEulaManager eulaMgr=EulaManager.instance(context);
        
        if (eulaMgr instanceof EulaManagerNoOp) {
        }
        else {
            Assert.fail("Expecting instanceof EulaManagerNoOp");
        }
    }
    
    @Test
    public void testRequiresEula() {
        final IEulaManager eulaMgr=new EulaManagerNoOp();
        final GpContext taskContext=null;
        boolean requiresEula=eulaMgr.requiresEula(taskContext);
        Assert.assertFalse("eulaMgr.requiresEula should always return false when eula manager is not enabled", requiresEula);
    }

    @Test
    public void testGetEula() {
        final IEulaManager eulaMgr=new EulaManagerNoOp();
        TaskInfo taskInfo = TaskUtil.getTaskInfoFromZip(this.getClass(), "testLicenseAgreement_v3.zip");
        final List<EulaInfo> eulas=eulaMgr.getEulas(taskInfo);
        Assert.assertEquals("eulaMgr.getEulas should return one associated license", 1, eulas.size());
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
        final GpContext taskContext=null;
        List<EulaInfo> eulas=eulaMgr.getAllEulaForModule(taskContext);
        Assert.assertEquals("eulaMgr.allEulaForModule should always return an empty list when eula manager is not enabled", 0, eulas.size());
    }

    @Test
    public void testGetPendingEulaForModule() {
        final IEulaManager eulaMgr=new EulaManagerNoOp();
        final GpContext taskContext=null;
        List<EulaInfo> eulas=eulaMgr.getPendingEulaForModule(taskContext);
        Assert.assertEquals("eulaMgr.pendingEulaForModule should always return an empty list when eula manager is not enabled", 0, eulas.size());
    }

    @Test
    public void testRecordEula() {
        final IEulaManager eulaMgr=new EulaManagerNoOp();
        final GpContext taskContext=null;
        eulaMgr.recordEula(taskContext);
    }

}
