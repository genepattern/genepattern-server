package org.genepattern.server.dm.userupload.dao;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.database.HibernateUtil;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * jUnit tests for the UserUploadDao class.
 * 
 * @author pcarr
 */
public class TestUserUploadDao {
    final static long HOUR_IN_MS = 1000 * 60 * 60;
    final static long DAY_IN_MS = 1000 * 60 * 60 * 24;
    
    @BeforeClass
    static public void beforeClass() throws Exception {
        DbUtil.initDb();
    }
    
    @AfterClass
    static public void afterClass() throws Exception {
        DbUtil.shutdownDb();
    }
    
    @Test
    public void testSelectTmpUserUploadsToPurge() throws Exception {
        final String adminUser=DbUtil.addUserToDb("admin");
        
        final Date olderThanDate = new Date(System.currentTimeMillis() - DAY_IN_MS);
        
        //initialize by adding a bunch of records
        try {
            HibernateUtil.beginTransaction();
            createUserUploadRecord(adminUser, new File("tmp/a.txt"), 8 * DAY_IN_MS);
            createUserUploadRecord(adminUser, new File("tmp/b.txt"), 8 * DAY_IN_MS);
            createUserUploadRecord(adminUser, new File("tmp/c.txt"), 8 * DAY_IN_MS);
            //in a subdirectory
            createUserUploadRecord(adminUser, new File("tmp/sub/"), 8 * DAY_IN_MS);
            createUserUploadRecord(adminUser, new File("tmp/sub/d.txt"), 8 * DAY_IN_MS);
            createUserUploadRecord(adminUser, new File("tmp/sub/e.txt"), 8 * DAY_IN_MS);
            createUserUploadRecord(adminUser, new File("tmp/sub/f.txt"), 8 * DAY_IN_MS);
            //not tmp files
            createUserUploadRecord(adminUser, new File("all_aml_test.cls"), 8 * DAY_IN_MS);
            createUserUploadRecord(adminUser, new File("all_aml_test.gct"), 8 * DAY_IN_MS);
            createUserUploadRecord(adminUser, new File("tutorial/"), 8 * DAY_IN_MS);
            createUserUploadRecord(adminUser, new File("tutorial/all_aml_test.cls"), 8 * DAY_IN_MS);
            createUserUploadRecord(adminUser, new File("tutorial/all_aml_train.cls"), 8 * DAY_IN_MS);
            createUserUploadRecord(adminUser, new File("tutorial/all_aml_test.gct"), 8 * DAY_IN_MS);
            createUserUploadRecord(adminUser, new File("tutorial/all_aml_train.gct"), 8 * DAY_IN_MS);
            
            //tmp files newer than 24 hours ago
            createUserUploadRecord(adminUser, new File("tmp/d.txt"));
            createUserUploadRecord(adminUser, new File("tmp/e.txt"));
            createUserUploadRecord(adminUser, new File("tmp/f.txt"));

            HibernateUtil.commitTransaction();
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }

        //query for tmp files
        try {
            UserUploadDao dao = new UserUploadDao();
            List<UserUpload> tmpFiles = dao.selectTmpUserUploadsToPurge(adminUser, olderThanDate);
            Assert.assertEquals("num tmpFiles", 7, tmpFiles.size());
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }
    
    /**
     * Test cases for comparing UserUpload records by timestamp, make sure we can handle millisecond resolution.
     */
    @Test
    public void testSelectTmpUserUploadsToPurgeTimestamp() {
        final Date olderThanDate=new Date(System.currentTimeMillis() - DAY_IN_MS);
        final String testUser=DbUtil.addUserToDb("test");
        
        try {
            HibernateUtil.beginTransaction();
            //exact date isn't purged
            createUserUploadRecord(testUser, new File("tmp/exact_date.txt"), new Date(olderThanDate.getTime()));
            //exact date + 1 ms isn't purged
            createUserUploadRecord(testUser, new File("tmp/plus_one_milli.txt"), new Date(1L+olderThanDate.getTime()));

            
            //everything else should be purged
            createUserUploadRecord(testUser, new File("tmp/minus_one_milli.txt"), new Date(-1L+olderThanDate.getTime()));
            createUserUploadRecord(testUser, new File("tmp/minus_ten_milli.txt"), new Date(-10L+olderThanDate.getTime()));
            createUserUploadRecord(testUser, new File("tmp/minus_one_sec.txt"), new Date(-1000L+olderThanDate.getTime()));
            createUserUploadRecord(testUser, new File("tmp/minus_ten_sec.txt"), new Date(-10000L+olderThanDate.getTime()));
            createUserUploadRecord(testUser, new File("tmp/minus_one_hour.txt"), new Date(-HOUR_IN_MS+olderThanDate.getTime()));
            createUserUploadRecord(testUser, new File("tmp/minus_one_day.txt"), new Date(-DAY_IN_MS+olderThanDate.getTime()));

            HibernateUtil.commitTransaction();
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
        
        //query for tmp files
        try {
            UserUploadDao dao = new UserUploadDao();
            List<UserUpload> tmpFiles = dao.selectTmpUserUploadsToPurge(testUser, olderThanDate);
            Assert.assertEquals("num tmpFiles", 6, tmpFiles.size());
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }

    }

    private void createUserUploadRecord(final String userId, final File relativeFile) {
        final Date date;
        if (relativeFile.exists()) {
            date=new Date(relativeFile.lastModified());
        }
        else {
            date=new Date();
        }
        createUserUploadRecord(userId, relativeFile, date);
    }
    
    private void createUserUploadRecord(final String userId, final File relativeFile, final long timeOffset) {
        createUserUploadRecord(userId, relativeFile, new Date(System.currentTimeMillis() - timeOffset));
    }

    private void createUserUploadRecord(final String userId, final File relativeFile, final Date lastModified) {
        UserUpload uu = new UserUpload();
        uu.setUserId(userId);
        uu.setPath(relativeFile.getPath());
        final String name=relativeFile.getName();
        uu.setName(name);
        if (relativeFile.isDirectory()) {
            uu.setKind("directory");
        }
        else {
            int idx=name.lastIndexOf(".");
            if (idx>0 && idx<name.length()) {
                String extension=name.substring(idx);
                uu.setExtension(extension);
                uu.setKind(extension);
            }
        }
        uu.setLastModified(lastModified);
        uu.setNumParts(1);
        uu.setNumPartsRecd(1);
        
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            new UserUploadDao().saveOrUpdate(uu);
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        catch (Throwable t) {
            HibernateUtil.rollbackTransaction();
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
}
