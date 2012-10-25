package org.genepattern.server.eula.dao;

import java.util.Date;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.eula.EulaInfo;
import org.genepattern.server.eula.InitException;
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
        final boolean isInTransaction = HibernateUtil.isInTransaction();
        final boolean userExists=UserAccountManager.instance().userExists(gp_username);
        final String gp_email=null; //can be null
        final String gp_password=null; //can be null
        
        if (!userExists) {
            try {
                UserAccountManager.instance().createUser(gp_username, gp_password, gp_email);
                if (!isInTransaction) {
                    HibernateUtil.commitTransaction();
                }
            }
            catch (AuthenticationException e) {
                Assert.fail("Failed to add user to db, gp_username="+gp_username+": "+e.getLocalizedMessage());
            }
            finally {
                if (!isInTransaction) {
                    HibernateUtil.closeCurrentSession();
                }
            }
        } 
        return gp_username;
    }

    private static EulaInfo init(final String lsid) {
        EulaInfo eula = new EulaInfo();
        try {
            eula.setModuleLsid(lsid);
        }
        catch (InitException e) {
            Assert.fail(e.getLocalizedMessage());
        }
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
            recorder.recordLicenseAgreement(userId, eula);
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
            recorder.recordLicenseAgreement(userId, eula);
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

    @Test
    public void testGetDateRecorded_NullEula() {
        final EulaInfo eula = null;
        final String userId="gp_user";
        final RecordEula recorder = new RecordEulaToDb();
        
        //Date actualDate=null;
        try {
            //actualDate = 
                recorder.getUserAgreementDate(userId, eula);
            Assert.fail("Expecting IllegalArgumentException, when eula==null");
        }
        catch (IllegalArgumentException e) {
            //expected
        }
        catch (Exception e) {
            Assert.fail("Unexpected exception in getUserAgreementDate: "+e.getLocalizedMessage());
        }
    }
    
    @Test
    public void testGetDateRecorded_DefaultLsid() {
        final EulaInfo eula = new EulaInfo();
        eula.setLicense("license.txt");
        final String userId="gp_user";
        final RecordEula recorder = new RecordEulaToDb();
        //Date actualDate=null;
        try {
            //actualDate = 
                recorder.getUserAgreementDate(userId, eula);
            Assert.fail("Expecting IllegalArgumentException, when eula has default lsid");
        }
        catch (IllegalArgumentException e) {
            //expected
        }
        catch (Exception e) {
            Assert.fail("Unexpected exception in getUserAgreementDate: "+e.getLocalizedMessage());
        }
    }
    
    @Test
    public void testGetDateRecorded_NullLsid() {
        final EulaInfo eula = new EulaInfo();
        //TODO: eula.setModuleLsid(null);
        eula.setLicense("license.txt");
        final String userId="gp_user";
        final RecordEula recorder = new RecordEulaToDb();
        //Date actualDate=null;
        try {
            //actualDate = 
                recorder.getUserAgreementDate(userId, eula);
            Assert.fail("Expecting IllegalArgumentException, when eula.lsid==null");
        }
        catch (IllegalArgumentException e) {
            //expected
        }
        catch (Exception e) {
            Assert.fail("Unexpected exception in getUserAgreementDate: "+e.getLocalizedMessage());
        }
    }

    @Test
    public void testGetDateRecorded_LsidNotSet() {
        final EulaInfo eula = new EulaInfo();
        eula.setLicense("license.txt");
        final String userId="gp_user";
        final RecordEula recorder = new RecordEulaToDb();
        //Date actualDate=null;
        try {
            //actualDate = 
                recorder.getUserAgreementDate(userId, eula);
            Assert.fail("Expecting IllegalArgumentException, when eula.lsid has not been set");
        }
        catch (IllegalArgumentException e) {
            //expected
        }
        catch (Exception e) {
            Assert.fail("Unexpected exception in getUserAgreementDate: "+e.getLocalizedMessage());
        }
    }
    
    @Test
    public void testGetDateRecorded_InvalidLsid() {
        final EulaInfo eula = new EulaInfo();
        try {
            eula.setModuleLsid("testLicenseAgreement");  //it's not an LSID
            Assert.fail("eula.setModuleLsid(\"testLicenseAgreement\") should throw InitException. It's not a valid LSID");
        }
        catch (InitException e) {
            //expected
        }
    }
    
    @Test
    public void testGetDateRecorded_NullUserId() {
        final EulaInfo eula = init("urn:lsid:8080.gp-trunk-dev.120.0.0.1:genepatternmodules:303:5");
        final String userId=null;
        final RecordEula recorder = new RecordEulaToDb();
        //Date actualDate=null;
        try {
            //actualDate = 
                recorder.getUserAgreementDate(userId, eula);
            Assert.fail("Expecting IllegalArgumentException, when userId==null");
        }
        catch (IllegalArgumentException e) {
            //expected
        }
        catch (Exception e) {
            Assert.fail("Unexpected exception in getUserAgreementDate: "+e.getLocalizedMessage());
        }
    }

    @Test
    public void testGetDateRecorded_UserIdNotSet() {
        final EulaInfo eula = init("urn:lsid:8080.gp-trunk-dev.120.0.0.1:genepatternmodules:303:5");
        final String userId="";
        final RecordEula recorder = new RecordEulaToDb();
        //Date actualDate=null;
        try {
            //actualDate = 
                recorder.getUserAgreementDate(userId, eula);
            Assert.fail("Expecting IllegalArgumentException, when userId==\"\" (empty string)");
        }
        catch (IllegalArgumentException e) {
            //expected
        }
        catch (Exception e) {
            Assert.fail("Unexpected exception in getUserAgreementDate: "+e.getLocalizedMessage());
        }
    }
    
    @Test
    public void testGetDateRecorded_UserNotInDb() {
        final EulaInfo eula = init("urn:lsid:8080.gp-trunk-dev.120.0.0.1:genepatternmodules:303:5");
        final String userId="not_registered";
        final RecordEula recorder = new RecordEulaToDb();
        Date actualDate=null;
        try {
            actualDate = recorder.getUserAgreementDate(userId, eula);
        }
        catch (Throwable t) {
            //expected
            Assert.fail("Unexpected exception in getUserAgreementDate: "+t.getLocalizedMessage());
        }
        Assert.assertNull("userAgreementDate should be null, when the userId is not for a registered GP user account", actualDate);
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
            recorder.recordLicenseAgreement(userId, eula);
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
            recorder.recordLicenseAgreement(userId, eula);
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

    /**
     * Start the hibernate transaction before calling recordEula method.
     */
    @Test
    public void testRecordEulaInTransaction() {
        final String userId=addUser("test_03");
        final EulaInfo eula = init("urn:lsid:8080.gp-trunk-dev.120.0.0.1:genepatternmodules:303:5");

        RecordEula recorder = new RecordEulaToDb();
        HibernateUtil.beginTransaction();
        boolean agreed = false;
        try {
            recorder.hasUserAgreed(userId, eula);
        }
        catch (Exception e) {
            Assert.fail("Unexpected exception in first call to hasUserAgreed: "+e.getLocalizedMessage());
        }
        Assert.assertFalse("hasUserAgreed", agreed);
        HibernateUtil.closeCurrentSession();

        HibernateUtil.beginTransaction();
        try {
            recorder.recordLicenseAgreement(userId, eula);
        }
        catch (Exception e) {
            Assert.fail("Unexpected exception in first call to recordLicenseAgreement: "+e.getLocalizedMessage());
        }
        HibernateUtil.rollbackTransaction();

        agreed = false;
        try {
            recorder.hasUserAgreed(userId, eula);
        }
        catch (Exception e) {
            Assert.fail("Unexpected exception in second call to hasUserAgreed: "+e.getLocalizedMessage());
        }
        Assert.assertFalse("hasUserAgreed, after rollback", agreed);
        HibernateUtil.closeCurrentSession();
    }

    /**
     * Should fail when the user_is is not in the DB.
     */
    @Test
    public void testRecordEulaInvalidUser() {
        final String userId="bogus"; //not in db
        final EulaInfo eula = init("urn:lsid:8080.gp-trunk-dev.120.0.0.1:genepatternmodules:303:5");
        RecordEula recorder = new RecordEulaToDb();
        boolean agreed = false;
        try {
            recorder.hasUserAgreed(userId, eula);
        }
        catch (Exception e) {
            Assert.fail("Unexpected exception in first call to hasUserAgreed: "+e.getLocalizedMessage());
        }
        Assert.assertFalse("hasUserAgreed", agreed);
        
        try {
            recorder.recordLicenseAgreement(userId, eula);
            Assert.fail("recordLicenseAgreement should throw Exception when the userId is not in the DB");
        }
        catch (Exception e) {
            //expected
        }
    }
    
}
