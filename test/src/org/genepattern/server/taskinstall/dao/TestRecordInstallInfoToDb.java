package org.genepattern.server.taskinstall.dao;

import java.net.URL;
import java.util.Date;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.taskinstall.InstallInfo;
import org.genepattern.server.taskinstall.RecordInstallInfoToDb;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test cases for recording task install information to the DB via Hibernate calls.
 * 
 * @author pcarr
 *
 */
public class TestRecordInstallInfoToDb {
    @BeforeClass
    static public void beforeClass() throws Exception {
        //some of the classes being tested require a Hibernate Session connected to a GP DB
        DbUtil.initDb();
        
        //add two users
        DbUtil.addUserToDb("admin");
        DbUtil.addUserToDb("gp_user");
    }

    @AfterClass
    static public void afterClass() throws Exception {
        DbUtil.shutdownDb();
    }
    
    /**
     * A test which creates, then queries, then deletes a record.
     * @throws Exception
     */
    @Test
    public void testCrud() throws Exception {
        final String cleLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2";
        final String userId="gp_user";
        //final String installDir="<tasklib>/ConvertLineEndings.2.1398";
        final String repoUrl="http://www.broadinstitute.org/webservices/gpModuleRepository";
        
        RecordInstallInfoToDb recorder=new RecordInstallInfoToDb();
        InstallInfo installInfo=new InstallInfo(InstallInfo.Type.REPOSITORY);
        installInfo.setLsidFromString(cleLsid);
        installInfo.setUserId(userId);
        installInfo.setDateInstalled(new Date());
        installInfo.setRepositoryUrl(new URL(repoUrl));
        recorder.save(installInfo);
        
        TaskInstall record=recorder.query(cleLsid);
        Assert.assertNotNull("Expecting a record for lsid="+cleLsid, record);
        Assert.assertEquals("record.lsid", cleLsid, record.getLsid());
        Assert.assertEquals("record.user_id", "gp_user", record.getUserId());
        Assert.assertEquals("record.source_type", InstallInfo.Type.REPOSITORY.name(), record.getSourceType());
        
        //now delete the record
        int numDeleted=recorder.delete(cleLsid);
        Assert.assertEquals("numDeleted", 1, numDeleted);
    }

}
