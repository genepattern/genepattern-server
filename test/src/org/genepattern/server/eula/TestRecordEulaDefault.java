package org.genepattern.server.eula;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.webservice.TaskInfo;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * jUnit tests for the RecordEulaDefault class
 * 
 * @author pcarr
 */
public class TestRecordEulaDefault {
    final String userId="test_RecordEulaDefault_user";
    //final String lsid="";
    TaskInfo taskInfo;
    EulaInfo eula;
    
    @Before
    public void setUp() {
        try {
            DbUtil.initDb();
        }
        catch (Throwable t) {
            Assert.fail("failed to initialize DB for test: "+t.getLocalizedMessage());
        }
        
        DbUtil.addUserToDb(userId);
        
        taskInfo = TaskUtil.getTaskInfoFromZip(this.getClass(), "testLicenseAgreement_v3.zip");         
        //prove that we loaded the expected module
        Assert.assertNotNull("taskInfo==null", taskInfo);
        Assert.assertEquals("taskName", "testLicenseAgreement", taskInfo.getName());
        Assert.assertEquals("taskLsid", "urn:lsid:9090.gpdev.gpint01:genepatternmodules:812:3", taskInfo.getLsid());
        
        eula = new EulaInfo();
        eula.setLicense("gp_license.txt");
        eula.setModuleName("testLicenseAgreement");
        try {
            eula.setModuleLsid("urn:lsid:9090.gpdev.gpint01:genepatternmodules:812:3");
        }
        catch (InitException e) {
            Assert.fail(""+e.getLocalizedMessage());
        }
    }
    
    @After
    public void tearDown() {
        // DbUtil.shutdownDb();
    }

    @Test
    public void testIntegration() {
        //RecordEula stub = RecordEulaStub.instance();
        RecordEulaDefault recordEula = new RecordEulaDefault();
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
        RecordEula stub = RecordEulaStub.instance();
        RecordEulaDefault recordEula = new RecordEulaDefault(stub);
        
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
