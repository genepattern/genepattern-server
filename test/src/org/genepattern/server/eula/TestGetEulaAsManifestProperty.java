package org.genepattern.server.eula;

import java.io.File;
import java.util.List;

import junit.framework.Assert;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.eula.EulaInfo.EulaInitException;
import org.genepattern.webservice.TaskInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * jUnit tests for the GetEulaAsManifestProperty class.
 * 
 * @author pcarr
 */
public class TestGetEulaAsManifestProperty {
    @Before
    public void setUp() {
        File thisDir=FileUtil.getSourceDir(this.getClass());
        EulaInfo.setLibdirStrategy( new LibdirStub(thisDir) );
    }
    
    @After
    public void tearDown() {
        EulaInfo.setLibdirStrategy(null);
    }
    
    /**
     * Test case: make sure we can get the list of EulaInfo from a single module,
     * which requires an EULA.
     */
    @Test
    public void testGetEulaFromModule() {
        final String filename="testLicenseAgreement_v3.zip";
        TaskInfo taskInfo = TestEulaManager.initTaskInfoFromZip(filename);
        Assert.assertNotNull("taskInfo==null", taskInfo);
        File licenseFile=FileUtil.getSourceFile(TestGetEulaAsManifestProperty.class, "gp_server_license.txt");

        GetEulaAsManifestProperty stub = new GetEulaAsManifestProperty();
        try {
            EulaInfo eulaIn=GetEulaFromTaskStub.initEulaInfo(taskInfo, licenseFile);
            stub.setEula(eulaIn, taskInfo);
        }
        catch (EulaInitException e) {
            Assert.fail(""+e.getLocalizedMessage());
        }


        final String userId="gp_user";
        Context taskContext=Context.getContextForUser(userId);
        taskContext.setTaskInfo(taskInfo);
        
        List<EulaInfo> eulas=stub.getEulasFromTask(taskInfo);
        Assert.assertNotNull("eulas==null", eulas);
        Assert.assertEquals("Expecting one EulaInfo", 1, eulas.size());
        EulaInfo eula = eulas.get(0);
        Assert.assertEquals("eula.moduleName", "testLicenseAgreement", eula.getModuleName());
        Assert.assertEquals("eula.moduleLsid", "urn:lsid:9090.gpdev.gpint01:genepatternmodules:812:3", eula.getModuleLsid());
        Assert.assertEquals("eula.moduleLsidVersion", "3", eula.getModuleLsidVersion());

        final String expectedContent=EulaInfo.fileToString(licenseFile);
        try {
            Assert.assertEquals("eula.content", expectedContent, eula.getContent());
        }
        catch (EulaInfo.EulaInitException e) {
            Assert.fail("EulaInfo.EulaInitException thrown in eula.getContent(): "+e.getLocalizedMessage());
        }
    }

}
