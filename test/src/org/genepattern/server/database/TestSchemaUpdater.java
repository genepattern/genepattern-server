/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.database;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.DbException;
import org.junit.Test;

import com.google.common.collect.Range;

/**
 * Unit tests for the SchemaUpdater class.
 * @author pcarr
 *
 */
public class TestSchemaUpdater {
    private final File schemaDir=new File("website/WEB-INF/schema");
    private final String schemaPrefix="analysis_hypersonic-";
    private final int numSchemaFiles=25;

    /**
     * Collect all DB integration tests into one unit test
     * @throws DbException
     * @throws Throwable
     */
    @Test
    public void initDbSchemaHsqlDb() throws DbException, Throwable {
        // path to load the schema files (e.g. ./resources/analysis_hypersonic-1.3.1.sql)
        final File schemaDir=new File("website/WEB-INF/schema");
        HibernateSessionManager db1=null;
        HibernateSessionManager db2=null;

        db1=DbUtil.initSessionMgrHsqlInMemory("db1");
        db2=DbUtil.initSessionMgrHsqlInMemory("db2");

        String dbSchemaVersion=SchemaUpdater.getDbSchemaVersion(db1);
        assertEquals("before update db1, dbSchemaVersion", "", dbSchemaVersion);
        assertEquals("before update db1, 'props' table exists", !"".equals(""), SchemaUpdater.tableExists(db1, "props"));
        assertEquals("before update db1, 'PROPS' table exists", false, SchemaUpdater.tableExists(db1, "PROPS"));

        // test auto-generate
        SchemaUpdater.updateSchema(db1, schemaDir, "analysis_hypersonic-", "1.3.0");
        assertEquals("after update, auto-save dbSchemaVersion (1.3.0)", "1.3.0", SchemaUpdater.getDbSchemaVersion(db1));

        SchemaUpdater.updateSchema(db1, schemaDir, "analysis_hypersonic-", "3.9.2");
        assertEquals("after update", "3.9.2", SchemaUpdater.getDbSchemaVersion(db1));

        dbSchemaVersion=SchemaUpdater.getDbSchemaVersion(db2);
        assertEquals("before update db2, dbSchemaVersion", "", dbSchemaVersion);
        assertEquals("before update db2, 'props' table exists", !"".equals(""), SchemaUpdater.tableExists(db2, "props"));
        assertEquals("before update db2, 'PROPS' table exists", false, SchemaUpdater.tableExists(db2, "PROPS"));

        // test auto-generate
        SchemaUpdater.updateSchema(db2, schemaDir, "analysis_hypersonic-", "1.3.0");
        assertEquals("after update db2, auto-save dbSchemaVersion (1.3.0)", "1.3.0", SchemaUpdater.getDbSchemaVersion(db2));

        SchemaUpdater.updateSchema(db2, schemaDir, "analysis_hypersonic-", "3.9.2");
        assertEquals("after update db2", "3.9.2", SchemaUpdater.getDbSchemaVersion(db2));
    }

    @Test
    public void listSchemaFiles_nullDbSchemaVersion() {
        final String schemaPrefix="analysis_hypersonic-";
        final String dbSchemaVersion=null;
        List<File> schemaFiles = SchemaUpdater.listSchemaFiles(schemaDir, schemaPrefix, "3.9.3", dbSchemaVersion);
        assertEquals("num schema files, new install of 3.9.3", numSchemaFiles, schemaFiles.size());
    }

    @Test
    public void listSchemaFiles_emptyDbSchemaVersion() {
        final String schemaPrefix="analysis_hypersonic-";
        final String dbSchemaVersion="";
        List<File> schemaFiles = SchemaUpdater.listSchemaFiles(schemaDir, schemaPrefix, "3.9.3", dbSchemaVersion);
        assertEquals("num schema files, new install of 3.9.3", numSchemaFiles, schemaFiles.size());
    }
    
    @Test
    public void listSchemaFiles_update() {
        final String schemaPrefix="analysis_hypersonic-";
        List<File> schemaFiles = SchemaUpdater.listSchemaFiles(schemaDir, schemaPrefix, "3.9.2", "3.9.1");
        assertEquals("num schema files, updated install of 3.9.2", 1, schemaFiles.size());
    }

    @Test
    public void listSchemaFiles_update_expectedSchemaVersionNotSet() {
        final String schemaPrefix="analysis_hypersonic-";
        List<File> schemaFiles = SchemaUpdater.listSchemaFiles(schemaDir, schemaPrefix, null, "3.9.1");
        assertEquals("num schema files, updated install of 3.9.2", 2, schemaFiles.size());
    }

    @Test
    public void listSchemaFiles_default() {
        final String schemaPrefix="analysis_hypersonic-";
        List<File> schemaFiles = SchemaUpdater.listSchemaFiles(schemaDir, schemaPrefix, null, null);
        assertEquals("num schema files, latest version", numSchemaFiles, schemaFiles.size());
    }
    
    @Test
    public void listSchemaFiles_invalidRange() {
        
        final String expectedSchemaVersion="3.7.6";
        final String dbSchemaVersion="3.8.2";
        final Range<String> range=DbSchemaFilter.initRange(dbSchemaVersion, expectedSchemaVersion);
        assertEquals("", false, range.contains(""));

        // expectedSchemaVersion < dbVersion
        final String schemaPrefix="analysis_hypersonic-";
        List<File> schemaFiles = SchemaUpdater.listSchemaFiles(schemaDir, schemaPrefix, expectedSchemaVersion, dbSchemaVersion);
        assertEquals("num schema files, expectedSchemaVersion < dbVersion", 0, schemaFiles.size());
        
    }

    @Test
    public void dbSchemaFilter_compare() {
        DbSchemaFilter dbSchemaFilter=new DbSchemaFilter.Builder().build();
        int c=dbSchemaFilter.compare(new File("analysis_hypersonic-3.9.3.sql"), new File("analysis_hypersonic-3.9.3-a.sql"));
        assertEquals("'3.9.3'.compare('3.9.3-a')", true, c<0);
    }
    
    @Test
    public void acceptSchemaVersion_newInstall() {
        DbSchemaFilter filter=new DbSchemaFilter.Builder().build();
        assertEquals("new install", true, 
                filter.acceptSchemaVersion("3.9.1"));
    }

    @Test
    public void acceptSchemaVersion_lessThan() {
        DbSchemaFilter filter=new DbSchemaFilter.Builder()
            .dbSchemaVersion("3.9.1")
        .build();
        assertEquals("schemaVersion < dbSchemaVersion", false, 
                filter.acceptSchemaVersion("3.9.0"));
    }

    @Test
    public void acceptSchemaVersion_equals() {
        DbSchemaFilter filter=new DbSchemaFilter.Builder()
            .dbSchemaVersion("3.9.1")
        .build();
        assertEquals("schemaVersion == dbSchemaVersion", false, 
                filter.acceptSchemaVersion("3.9.1"));
    }

    @Test
    public void acceptSchemaVersion_greaterThan() {
        DbSchemaFilter filter=new DbSchemaFilter.Builder()
            .dbSchemaVersion("3.9.0")
        .build();
        assertEquals("schemaVersion > dbSchemaVersion", true, 
                filter.acceptSchemaVersion("3.9.1"));
    }
    
    @Test
    public void isUpToDate_emptyList() {
        List<File> schemaFiles=Collections.emptyList();
        assertEquals("empty list", true, SchemaUpdater.isUpToDate(schemaFiles));
    }

    @Test
    public void isUpToDate_nullSchemaFiles() {
        List<File> schemaFiles=null;
        assertEquals(true, SchemaUpdater.isUpToDate(schemaFiles));
    }
    
    @Test
    public void isUpToDate() {
        final String expectedSchemaVersion=null;
        final String dbSchemaVersion="3.9.3";
        List<File> schemaFiles = SchemaUpdater.listSchemaFiles(schemaDir, schemaPrefix, expectedSchemaVersion, dbSchemaVersion);
        
        boolean upToDate=SchemaUpdater.isUpToDate(schemaFiles);
        assertEquals("schemaFiles.size>0, expectedSchemaVersion==null", true, upToDate);
    }

    @Test
    public void isUpToDate_not() {
        final String expectedSchemaVersion=null;
        final String dbSchemaVersion="3.9.1";
        List<File> schemaFiles = SchemaUpdater.listSchemaFiles(schemaDir, schemaPrefix, expectedSchemaVersion, dbSchemaVersion);
        
        boolean upToDate=SchemaUpdater.isUpToDate(schemaFiles);
        assertEquals("schemaFiles.size>0, maxSchemaVersion==null", false, upToDate);
    }
    
    @Test
    public void isUpToDate_dbVersion_lt_maxVersion() {
        final String expectedSchemaVersion="3.8.2";
        final String dbSchemaVersion="3.7.6";
        List<File> schemaFiles = SchemaUpdater.listSchemaFiles(schemaDir, schemaPrefix, expectedSchemaVersion, dbSchemaVersion);
        
        boolean upToDate=SchemaUpdater.isUpToDate(schemaFiles);
        assertEquals("schemaFiles.size>0, dbSchemaVersion < maxSchemaVersion", false, upToDate);
    }

    @Test
    public void isUpToDate_dbVersion_eq_MaxVersion() {
        final String expectedSchemaVersion="3.7.6";
        final String dbSchemaVersion="3.7.6";
        List<File> schemaFiles = SchemaUpdater.listSchemaFiles(schemaDir, schemaPrefix, expectedSchemaVersion, dbSchemaVersion);
        
        boolean upToDate=SchemaUpdater.isUpToDate(schemaFiles);
        assertEquals("schemaFiles.size==0, dbSchemaVersion == maxSchemaVersion", true, upToDate);
    }
    
    @Test
    public void isUpToDate_dbVersion_gt_maxVersion() {
        final String expectedSchemaVersion="3.7.6";
        final String dbSchemaVersion="3.8.2";
        List<File> schemaFiles = SchemaUpdater.listSchemaFiles(schemaDir, schemaPrefix, expectedSchemaVersion, dbSchemaVersion);
        
        boolean upToDate=SchemaUpdater.isUpToDate(schemaFiles);
        assertEquals("schemaFiles.size>0, dbSchemaVersion > maxSchemaVersion", true, upToDate);
    }
}
