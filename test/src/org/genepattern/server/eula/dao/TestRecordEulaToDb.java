package org.genepattern.server.eula.dao;

import java.util.Date;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.eula.EulaInfo;
import org.genepattern.server.eula.RecordEula;
import org.genepattern.server.eula.dao.RecordEulaToDb;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Test cases for recording EULA to DB via Hibernate calls.
 * 
 * @author pcarr
 */
public class TestRecordEulaToDb {

    @BeforeClass
    static public void beforeClass() throws Exception {
        //some of the classes being tested require a Hibernate Session connected to a GP DB
        DbUtil.initDb();
        
        //add two users
        addUser("admin");
        addUser("gp_user");
    }

    //add the user to the DB
    private static String addUser(final String gp_username) {
        final String gp_email=null; //can be null
        final String gp_password=null; //can be null
        try {
            if (!UserAccountManager.instance().userExists(gp_username)) {
                UserAccountManager.instance().createUser(gp_username, gp_password, gp_email);
            } 
            return gp_username;
        }
        catch (AuthenticationException e) {
            Assert.fail("Failed to add user to db, gp_username="+gp_username+": "+e.getLocalizedMessage());
        }
        catch (Throwable t) {
            Assert.fail("Failed to add user to db, gp_username="+gp_username+": "+t.getLocalizedMessage());
        }
        //shouldn't be here, because the test should fail
        return null;
    }

    private static EulaInfo init(final String lsid) {
        EulaInfo eula = new EulaInfo();
        eula.setModuleLsid(lsid);
        eula.setLicense("license.txt");
        return eula;
    }
    
    /**
     * jUnit date comparison assertion, with built in tolerance of plus or minus one second.
     * 
     * @param message
     * @param expected
     * @param actual
     */
    private static void assertDateEquals(final String message, final Date expected, final Date actual) {
        //by default, within one second
        assertDateEquals(message, expected, actual, 1000);
    }
    /**
     * jUnit date comparison assertion, with custom tolerance of plus or mins delta milliseconds.
     * 
     * @param message
     * @param expected
     * @param actual
     * @param expectedDelta
     */
    private static void assertDateEquals(String message, Date expected, Date actual, long expectedDelta) {
        //make sure actual is not null
        Assert.assertNotNull(message, actual);
        Assert.assertNotNull(message, expected);
        
        long diff = Math.abs(expected.getTime()-actual.getTime());
        boolean acceptable = diff <= expectedDelta;
        Assert.assertTrue(message, acceptable);
    }

    /**
     * Integration test which covers the basic usage scenario. An initial check
     * for record of eula (should be false), following by record of euls (should update db),
     * followed by another check (should be true).
     */
    @Test
    public void testIntegration() {
        final EulaInfo eula = init("urn:lsid:8080.gp-trunk-dev.120.0.0.1:genepatternmodules:303:5");
        final String userId=addUser("test_01");

        RecordEula recorder = new RecordEulaToDb();
        try {
            boolean agreed = recorder.hasUserAgreed(userId, eula);
            Assert.assertFalse("User has not yet agreed", agreed);
        }
        catch (Exception e) {
            Assert.fail(""+e.getLocalizedMessage());
        }

        try {
            recorder.recordLicenseAgreement(userId, eula.getModuleLsid());
        }
        catch (Exception e) {
            Assert.fail(""+e.getLocalizedMessage());
        }
        catch (Throwable t) {
            Assert.fail(""+t.getLocalizedMessage());
        }
        
        try {
            boolean agreed = recorder.hasUserAgreed(userId, eula);
            Assert.assertTrue("User has agreed", agreed);
        }
        catch (Exception e) {
            Assert.fail(""+e.getLocalizedMessage());
        } 
    }

    @Test
    public void testDateRecorded() {
        final EulaInfo eula = init("urn:lsid:8080.gp-trunk-dev.120.0.0.1:genepatternmodules:303:5");
        final String userId=addUser("test_02");
        final RecordEula recorder = new RecordEulaToDb();
        
        Date actualDate=null;
        try {
            actualDate = recorder.getUserAgreementDate(userId, eula);
        }
        catch (Throwable t) {
            Assert.fail("failed 1st try to getUserAgreementDate: "+t.getLocalizedMessage());
        }
        Assert.assertNull("Expecting null date before accepting EULA", actualDate);
        
        Date expectedDate=new Date(); //plus or minus a few ticks
        try {
            recorder.recordLicenseAgreement(userId, eula.getModuleLsid());
        }
        catch (Throwable t) {
            Assert.fail("failed to record EULA: "+t.getLocalizedMessage());
        }
        
        //for debugging, to force the matter, let's wait a bit
        //try {
        //    Thread.sleep(5000);
        //}
        //catch (InterruptedException e) {
        //    Assert.fail("test thread interrupted");
        //    Thread.currentThread().interrupt();
        //}

        try {
            actualDate=recorder.getUserAgreementDate(userId, eula);
        }
        catch (Throwable t) {
            Assert.fail("failed 2nd try to getUserAgreementDate: "+t.getLocalizedMessage());
        }
        
        assertDateEquals("expected userAgreementDate ("+expectedDate+") doesn't match actual ("+actualDate+")", expectedDate, actualDate);
    }

    /**
     * Saving the same record twice should have no effect.
     */
    @Test
    public void testIdempotency() {
        final EulaInfo eula = init("urn:lsid:8080.gp-trunk-dev.120.0.0.1:genepatternmodules:303:5");
        //warning: can't use the same user id as another test, because we are running against the same DB for all tests
        final String userId=addUser("test_03");
        RecordEula recorder = new RecordEulaToDb();
        try {
            boolean agreed = recorder.hasUserAgreed(userId, eula);
            Assert.assertFalse("Expecting a user which has not already agreed", agreed);
        }
        catch (Throwable t) {
            Assert.fail("Failed in 1st call to hasUserAgreed: "+t.getLocalizedMessage());
        }
        try {
            recorder.recordLicenseAgreement(userId, eula.getModuleLsid());
        }
        catch (Throwable t) {
            Assert.fail("Failed in 1st call to recordLicenseAgreement: "+t.getLocalizedMessage());
        }
        try {
            boolean agreed = recorder.hasUserAgreed(userId, eula);
            Assert.assertTrue("hasUserAgreed", agreed);
        }
        catch (Throwable t) {
            Assert.fail("Failed in 2nd call to hasUserAgreed: "+t.getLocalizedMessage());
        }
        try {
            recorder.recordLicenseAgreement(userId, eula.getModuleLsid());
        }
        catch (Throwable t) {
            Assert.fail("Failed in 2nd (unnecessary) call to recordLicenseAgreement: "+t.getLocalizedMessage());
        }        
        try {
            boolean agreed = recorder.hasUserAgreed(userId, eula);
            Assert.assertTrue("hasUserAgreed", agreed);
        }
        catch (Throwable t) {
            Assert.fail("Failed in 2nd call to hasUserAgreed: "+t.getLocalizedMessage());
        }
    }
    
}
