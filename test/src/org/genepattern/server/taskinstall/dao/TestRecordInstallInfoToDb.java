package org.genepattern.server.taskinstall.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.junitutil.DbUtil.DbType;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.taskinstall.InstallInfo;
import org.genepattern.server.taskinstall.RecordInstallInfoToDb;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test cases for recording task install information to the DB via Hibernate calls.
 * 
 * @author pcarr
 *
 */
public class TestRecordInstallInfoToDb {
    private HibernateSessionManager mgr;
    final String cleLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2";
    final String lsid_2="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00003:1";
    
    final String userId="gp_user";
    final String repoUrl="http://www.broadinstitute.org/webservices/gpModuleRepository";
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        //some of the classes being tested require a Hibernate Session connected to a GP DB
        DbUtil.initDb(DbType.MYSQL);
        
        //add two users
        DbUtil.addUserToDb("admin");
        DbUtil.addUserToDb("gp_user");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        DbUtil.shutdownDb();
    }
    
    @Before
    public void setUp() {
        mgr=HibernateUtil.instance();
    }

    /**
     * A test which creates, then queries, then deletes a record.
     * @throws Exception
     */
    @Test
    public void testCrud() throws MalformedURLException, Exception {
        List<?> taskInstallCategoryTable = TaskInstall.getAllTaskInstallCategory(mgr);
        assertEquals("num task_install_category rows before", 0, taskInstallCategoryTable.size());
        assertEquals("allCategories.size", 0, Category.getAllCategories(mgr).size());

        RecordInstallInfoToDb recorder=new RecordInstallInfoToDb();
        InstallInfo installInfo=new InstallInfo(InstallInfo.Type.REPOSITORY);
        installInfo.setLsidFromString(cleLsid);
        installInfo.setUserId(userId);
        installInfo.setDateInstalled(new Date());
        installInfo.setRepositoryUrl(new URL(repoUrl));
        installInfo.addCategory("Preprocess & Utilities");
        installInfo.addCategory("Test");
        recorder.save(installInfo);
        taskInstallCategoryTable = TaskInstall.getAllTaskInstallCategory(mgr);
        assertEquals("num task_install_category rows after adding a task with 2 categories", 2, taskInstallCategoryTable.size());
        assertEquals("allCategories.size", 2, Category.getAllCategories(mgr).size());
        
        TaskInstall record=recorder.query(cleLsid);
        assertNotNull("Expecting a record for lsid="+cleLsid, record);
        assertEquals("record.lsid", cleLsid, record.getLsid());
        assertEquals("record.user_id", "gp_user", record.getUserId());
        assertEquals("record.source_type", InstallInfo.Type.REPOSITORY.name(), record.getSourceType());
        assertEquals("record.categories.size", 2, record.getCategories().size());
        
        // add another task with some overlapping categories
        installInfo=new InstallInfo(InstallInfo.Type.REPOSITORY);
        installInfo.setLsidFromString(lsid_2);
        installInfo.setUserId(userId);
        installInfo.setDateInstalled(new Date());
        installInfo.setRepositoryUrl(new URL(repoUrl));
        installInfo.addCategory("Preprocess & Utilities");
        recorder.save(installInfo);
        taskInstallCategoryTable = TaskInstall.getAllTaskInstallCategory(mgr);
        assertEquals("num task_install_category rows after adding a task with duplicate categories", 3, taskInstallCategoryTable.size());
        assertEquals("allCategories.size", 2, Category.getAllCategories(mgr).size());
        
        //now remove a category
        TaskInstall.setCategories(mgr, cleLsid, Arrays.asList("Preprocess & Utilities"));
        record=recorder.query(cleLsid);
        assertEquals("record.categories.size", 1, record.getCategories().size());
        assertEquals("num task_install_category rows after remove one category", 2, TaskInstall.getAllTaskInstallCategory(mgr).size());
        assertEquals("allCategories.size", 2, Category.getAllCategories(mgr).size());
        
        //now add another category
        TaskInstall.setCategories(mgr, cleLsid, Arrays.asList("Preprocess & Utilities", "Test", "My Test"));
        record=recorder.query(cleLsid);
        assertEquals("record.categories.size", 3, record.getCategories().size());
        assertEquals("num task_install_category rows after adding two categories", 4, TaskInstall.getAllTaskInstallCategory(mgr).size());
        assertEquals("allCategories.size", 3, Category.getAllCategories(mgr).size());

        //now delete the record
        int numDeleted=recorder.delete(cleLsid);
        Assert.assertEquals("numDeleted", 1, numDeleted);
        assertEquals("num task_install_category rows after delete", 1, TaskInstall.getAllTaskInstallCategory(mgr).size());
        assertEquals("allCategories.size", 3, Category.getAllCategories(mgr).size());
    }

}
