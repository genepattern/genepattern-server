/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.dm.userupload.dao;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.genepattern.drm.Memory;
import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.database.HibernateSessionManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * jUnit tests for the UserUploadDao class.
 * 
 * @author pcarr
 */
public class TestUserUploadDao {
    final static long HOUR_IN_MS = 1000 * 60 * 60;
    final static long DAY_IN_MS = 1000 * 60 * 60 * 24;
    
    final static Date now=new Date();
    final static Date oneDayAgo=new Date(now.getTime() - DAY_IN_MS);
    final static Date eightDaysAgo=new Date(now.getTime() - 8 * DAY_IN_MS);

    static String adminUser;
    static String testUser;
    
    private HibernateSessionManager mgr;
    private GpConfig gpConfig;
     
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();
    
    @Before
    public void setUp() throws Exception {
        mgr=DbUtil.getTestDbSession();
        final String userDir=temp.newFolder("users").getAbsolutePath();
        gpConfig=new GpConfig.Builder()
            .addProperty(GpConfig.PROP_USER_ROOT_DIR, userDir)
        .build();
        adminUser=DbUtil.addUserToDb(gpConfig, mgr, "admin");
        testUser=DbUtil.addUserToDb(gpConfig, mgr, "test");
        
        //initialize by adding a bunch of records
        try {
            mgr.beginTransaction();
            
            //-------------------------
            // admin acount
            //-------------------------            
            createUserUploadRecord(adminUser, new File("tmp/"), 10 * DAY_IN_MS);
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
            
            
            //-------------------------
            // test account
            //-------------------------            
            createUserUploadRecord(testUser, new File("tmp/"), 10 * DAY_IN_MS);
            //exact date isn't purged
            createUserUploadRecord(testUser, new File("tmp/exact_date.txt"), new Date(oneDayAgo.getTime()));
            //exact date + 1 ms isn't purged
            createUserUploadRecord(testUser, new File("tmp/plus_one_milli.txt"), new Date(1L+oneDayAgo.getTime()));

            //everything else should be purged
            createUserUploadRecord(testUser, new File("tmp/minus_one_milli.txt"), new Date(-1L+oneDayAgo.getTime()));
            createUserUploadRecord(testUser, new File("tmp/minus_ten_milli.txt"), new Date(-10L+oneDayAgo.getTime()));
            createUserUploadRecord(testUser, new File("tmp/minus_one_sec.txt"), new Date(-1000L+oneDayAgo.getTime()));
            createUserUploadRecord(testUser, new File("tmp/minus_ten_sec.txt"), new Date(-10000L+oneDayAgo.getTime()));
            createUserUploadRecord(testUser, new File("tmp/minus_one_hour.txt"), new Date(-HOUR_IN_MS+oneDayAgo.getTime()));
            createUserUploadRecord(testUser, new File("tmp/minus_one_day.txt"), new Date(-DAY_IN_MS+oneDayAgo.getTime()));
            
            //add some regular (non-temp) files for the test account
            createUserUploadRecord(testUser, new File("all_aml_test.gct"), new Date());
            createUserUploadRecord(testUser, new File("all_aml_test.cls"), new Date());

            mgr.commitTransaction();
        }
        finally {
            mgr.closeCurrentSession();
        }
    }
    
    @Test
    public void testSelectTmpUserUploadsToPurge() throws Exception {

        //query for tmp files
        try {
            UserUploadDao dao = new UserUploadDao(mgr);
            List<UserUpload> tmpFiles = dao.selectTmpUserUploadsToPurge(adminUser, oneDayAgo);
            Assert.assertEquals("num tmpFiles", 7, tmpFiles.size());
        }
        finally {
            mgr.closeCurrentSession();
        }
    }
    
    /**
     * Test cases for comparing UserUpload records by timestamp, make sure we can handle millisecond resolution.
     */
    @Test
    public void testSelectTmpUserUploadsToPurgeTimestamp() {
        //query for tmp files
        try {
            UserUploadDao dao = new UserUploadDao(mgr);
            List<UserUpload> tmpFiles = dao.selectTmpUserUploadsToPurge(testUser, oneDayAgo);
            Assert.assertEquals("num tmpFiles", 6, tmpFiles.size());
        }
        finally {
            mgr.closeCurrentSession();
        }
    }

    /**
     * test case for geting the filtered list of files from the user uploads tab,
     * filter out the tmp files.
     */
    @Test
    public void testSelectAllUserUploadExceptTmpFiles() {
        //set up
        try {
            UserUploadDao dao = new UserUploadDao(mgr);
            final boolean includeTempFiles=false;
            List<UserUpload> userUploads=dao.selectAllUserUpload(adminUser, includeTempFiles);
            Assert.assertEquals("num files not including tmp", 7, userUploads.size());
        }
        finally {
            mgr.closeCurrentSession();
        }
    }

    /**
     * test case for geting the list of files from the user uploads tab,
     * including the tmp files.
     */
    @Test
    public void testSelectAllUserUploadIncludeTmpFiles() {
        //set up
        try {
            UserUploadDao dao = new UserUploadDao(mgr);
            final boolean includeTempFiles=true;
            List<UserUpload> userUploads=dao.selectAllUserUpload(adminUser, includeTempFiles);
            Assert.assertEquals("num files including tmp", 18, userUploads.size());
        }
        finally {
            mgr.closeCurrentSession();
        }
    }

    /**
     * test case for getting the length of files from the user uploads tab,
     * excluding the tmp files.
     */
    @Test
    public void testGetSizeOfUserUploadExceptTmpFiles() {
        //set up
        try {
            UserUploadDao dao = new UserUploadDao(mgr);
            final boolean includeTempFiles=false;
            Memory size = dao.sizeOfAllUserUploads(adminUser, includeTempFiles);
            Assert.assertEquals("size of files excluding tmp", 60, size.getNumBytes());
        }
        finally {
            mgr.closeCurrentSession();
        }
    }

    /**
     * test case for getting the length of files from the user uploads tab,
     * including the tmp files.
     */
    @Test
    public void testGetSizeOfUserUploadIncludeTmpFiles() {
        //set up
        try {
            UserUploadDao dao = new UserUploadDao(mgr);
            final boolean includeTempFiles=true;
            Memory size = dao.sizeOfAllUserUploads(adminUser, includeTempFiles);
            Assert.assertEquals("size of files including tmp", 150, size.getNumBytes());
        }
        finally {
            mgr.closeCurrentSession();
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

        uu.setFileLength(10);

        final boolean isInTransaction=mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            new UserUploadDao(mgr).saveOrUpdate(uu);
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
        }
        catch (Throwable t) {
            mgr.rollbackTransaction();
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
}
