/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.eula;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.database.HibernateSessionManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * jUnit tests for the RecordEulaDefault class
 * 
 * @author pcarr
 */
public class TestRecordEulaDefault {
    final String userId="test_RecordEulaDefault_user";
    private HibernateSessionManager mgr;
    private GpConfig gpConfig;
    private EulaInfo eula;
    
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Before
    public void setUp() throws ExecutionException, IOException {
        mgr=DbUtil.getTestDbSession();
        final String userDir=temp.newFolder("users").getAbsolutePath();
        gpConfig=new GpConfig.Builder()
            .addProperty(GpConfig.PROP_USER_ROOT_DIR, userDir)
        .build();
        
        DbUtil.addUserToDb(gpConfig, mgr, userId);
        
        eula = new EulaInfo();
        eula.setLicense("gp_license.txt");
        eula.setModuleName("testLicenseAgreement");
        try {
            eula.setModuleLsid("urn:lsid:9090.gpdev.gpint01:genepatternmodules:812:3");
        }
        catch (InitException e) {
            fail(""+e.getLocalizedMessage());
        }
    }

    @Test
    public void testIntegration() {
        RecordEulaDefault recordEula = new RecordEulaDefault(mgr);
        try {
            recordEula.recordLicenseAgreement(userId, eula);
            
            //sleep so that we have a chance for the asynch call to commence and complete
            //Thread.sleep(10*1000);
        }
        catch (Throwable t) {
            Assert.fail("failed to record eula: "+t.getLocalizedMessage());
        }
    }

    @Test
    public void testIntegration_withRecordEulaStub() {
        RecordEula stub = new RecordEulaStub();
        RecordEulaDefault recordEula = new RecordEulaDefault(mgr, stub);
        
        try {
            recordEula.recordLicenseAgreement(userId, eula);
            
            //sleep so that we have a chance for the asynch call to commence and complete
            //Thread.sleep(10*1000);
        }
        catch (Throwable t) {
            Assert.fail("failed to record eula: "+t.getLocalizedMessage());
        }
    }

}
