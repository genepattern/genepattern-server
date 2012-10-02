package org.genepattern.server.eula.dao;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.eula.EulaInfo;
import org.genepattern.server.eula.RecordEula;
import org.genepattern.server.eula.dao.RecordEulaToDb;
import org.junit.Assert;
import org.junit.Test;

import junit.framework.TestCase;

/**
 * Test cases for recording EULA to DB via Hibernate calls.
 * 
 * @author pcarr
 */
public class TestRecordEulaToDb extends TestCase {
    public void setUp() throws Exception {
        super.setUp();

        //some of the classes being tested require a Hibernate Session connected to a GP DB
        DbUtil.initDb();
    }

    /**
     * Integration test which covers the basic usage scenario. An initial check
     * for record of eula (should be false), following by record of euls (should update db),
     * followed by another check (should be true).
     */
    @Test
    public void testIntegration() {
        String userId="test";
        String lsid="urn:lsid:8080.gp-trunk-dev.120.0.0.1:genepatternmodules:303:5";
        EulaInfo eula = new EulaInfo();
        eula.setModuleLsid(lsid);
        RecordEula recorder = new RecordEulaToDb();
        try {
            boolean agreed = recorder.hasUserAgreed(userId, eula);
            Assert.assertFalse("User has not yet agreed", agreed);
        }
        catch (Exception e) {
            fail(""+e.getLocalizedMessage());
        }

        try {
            recorder.recordLicenseAgreement(userId, lsid);
        }
        catch (Throwable t) {
            fail(""+t.getLocalizedMessage());
        }
        
        try {
            boolean agreed = recorder.hasUserAgreed(userId, eula);
            Assert.assertTrue("User has agreed", agreed);
        }
        catch (Exception e) {
            fail(""+e.getLocalizedMessage());
        }
    }

}
