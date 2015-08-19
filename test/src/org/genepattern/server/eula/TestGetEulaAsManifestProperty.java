/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.eula;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.eula.InitException;
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
        TaskInfo taskInfo = TaskUtil.getTaskInfoFromZip(this.getClass(), filename);
        assertNotNull("taskInfo==null", taskInfo);
        File licenseFile=FileUtil.getSourceFile(TestGetEulaAsManifestProperty.class, "gp_server_license.txt");

        GetEulaAsManifestProperty stub = new GetEulaAsManifestProperty();
        try {
            EulaInfo eulaIn=GetEulaFromTaskStub.initEulaInfo(taskInfo, licenseFile);
            stub.setEula(eulaIn, taskInfo);
        }
        catch (InitException e) {
            fail(""+e.getLocalizedMessage());
        }

        final String userId="gp_user";
        GpContext taskContext=GpContext.getContextForUser(userId);
        taskContext.setTaskInfo(taskInfo);
        
        final List<EulaInfo> eulas=stub.getEulasFromTask(taskInfo);
        assertNotNull("eulas==null", eulas);
        assertEquals("Expecting one EulaInfo", 1, eulas.size());
        final String expectedContent=EulaInfo.fileToString(licenseFile);
        TestEulaManagerImpl.assertEulaInfo(eulas, 0, "testLicenseAgreement", "urn:lsid:9090.gpdev.gpint01:genepatternmodules:812:3", "3", expectedContent);
    }

    /**
     * Allow for the possibility of multiple EULA declared in the same manifest file.
     */
    @Test
    public void testGetTwoEulaFromOneModule() {
        TaskInfo taskInfo=new TaskInfo();
        taskInfo.setName("testLicenseAgreement");
        taskInfo.giveTaskInfoAttributes().put("LSID", "urn:lsid:9090.gpdev.gpint01:genepatternmodules:812:3");
        
        File gpLicenseFile=FileUtil.getSourceFile(TestGetEulaAsManifestProperty.class, "gp_server_license.txt");
        File exampleLicenseFile=FileUtil.getSourceFile(TestGetEulaAsManifestProperty.class, "example_license.txt");

        GetEulaAsManifestProperty stub = new GetEulaAsManifestProperty();
        try {
            EulaInfo eulaIn0=GetEulaFromTaskStub.initEulaInfo(taskInfo, gpLicenseFile);
            EulaInfo eulaIn1=GetEulaFromTaskStub.initEulaInfo(taskInfo, exampleLicenseFile);
            List<EulaInfo> eulasIn=new ArrayList<EulaInfo>();
            eulasIn.add(eulaIn0);
            eulasIn.add(eulaIn1);
            stub.setEulas(eulasIn, taskInfo);
        }
        catch (InitException e) {
            fail(""+e.getLocalizedMessage());
        }

        final String userId="gp_user";
        GpContext taskContext=GpContext.getContextForUser(userId);
        taskContext.setTaskInfo(taskInfo);
        
        final List<EulaInfo> eulas=stub.getEulasFromTask(taskInfo);
        assertNotNull("eulas==null", eulas);
        assertEquals("Expecting two EulaInfo", 2, eulas.size());
        final String gpLicenseContent=EulaInfo.fileToString(gpLicenseFile);
        final String exampleLicenseContent=EulaInfo.fileToString(exampleLicenseFile);
        
        TestEulaManagerImpl.assertEulaInfo(eulas, 0, "testLicenseAgreement", "urn:lsid:9090.gpdev.gpint01:genepatternmodules:812:3", "3", gpLicenseContent);
        TestEulaManagerImpl.assertEulaInfo(eulas, 1, "testLicenseAgreement", "urn:lsid:9090.gpdev.gpint01:genepatternmodules:812:3", "3", exampleLicenseContent);
    }

}
