/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.database;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.genepattern.server.DbException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.collect.Range;

/**
 * Unit tests for the SchemaUpdater class.
 * @author pcarr
 *
 */
public class TestSchemaUpdater {
    private final File schemaDir=new File("website/WEB-INF/schema");
    private final String schemaPrefix="analysis_hypersonic-";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    /**
     * Create a new HSQL DB connection in "In-Process (Standalone) Mode". The DB is saved directly to the file system
     * without any network I/O. This is suitable for testing.
     * 
     * @param dbDir, a directory for saving HSQL DB database files.
     * @param dbName, a unique name for the database, default is "GenePatternDB"
     * @return
     */
    protected HibernateSessionManager initSessionMgr(final File dbDir, final String dbName) throws IOException, FileNotFoundException {
        final File hsqlDbFile=new File(dbDir, dbName);

        //manually set properties
        final Properties p=new Properties();
        p.setProperty("hibernate.current_session_context_class", "thread");
        p.setProperty("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
        p.setProperty("hibernate.username", "sa");
        p.setProperty("hibernate.password", "");
        p.setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
        p.setProperty("hiberate.default_schema", "PUBLIC");
        p.setProperty("database.vendor", "HSQL");
        p.setProperty("hibernate.connection.url", "jdbc:hsqldb:file:"+hsqlDbFile);

        HibernateSessionManager mgr=new HibernateSessionManager(p);
        return mgr;
    }
    
    @Test
    public void initDbSchemaHsqlDb() throws DbException, Throwable {
        final File workingDir=new File(System.getProperty("user.dir"));
        final File resourcesDir=new File(workingDir, "resources");

        // path to load the schema files (e.g. ./resources/analysis_hypersonic-1.3.1.sql)
        File schemaDir=resourcesDir;
        // path to load the hsql db files (e.g. ./resources/GenePatternDB.script and ./resources/GenePatternDB.properties)
        final File hsqlDbDir=tmp.newFolder("junitdb");
        final String hsqlDbName="GenePatternDB";

        HibernateSessionManager mgr=initSessionMgr(hsqlDbDir, hsqlDbName);

        try {
            String dbSchemaVersion=SchemaUpdater.getDbSchemaVersion(mgr);
            assertEquals("before update", "", dbSchemaVersion);
            assertEquals("before update, 'props' table exists", !"".equals(""), SchemaUpdater.tableExists(mgr, "props"));
            assertEquals("before update, 'PROPS' table exists", false, SchemaUpdater.tableExists(mgr, "PROPS"));
            
            schemaDir=new File("website/WEB-INF/schema");
            SchemaUpdater.updateSchema(mgr, schemaDir, "analysis_hypersonic-", "3.9.2");
            assertEquals("after update", "3.9.2", SchemaUpdater.getDbSchemaVersion(mgr));
        }
        finally {
            // We create a new in-process DB for each test and then delete its directory it after the test completes
            // there is no need to properly close the DB
            // If this ever changes ...
            //if (mgr != null) {
            //    HsqlDbUtil.shutdownDatabase(mgr);
            //}
        } 
        
    }

    @Test
    public void listSchemaFiles_nullDbSchemaVersion() {
        final String schemaPrefix="analysis_hypersonic-";
        final String dbSchemaVersion=null;
        List<File> schemaFiles = SchemaUpdater.listSchemaFiles(schemaDir, schemaPrefix, "3.9.3", dbSchemaVersion);
        assertEquals("num schema files, new install of 3.9.3", 40, schemaFiles.size());
    }

    @Test
    public void listSchemaFiles_emptyDbSchemaVersion() {
        final String schemaPrefix="analysis_hypersonic-";
        final String dbSchemaVersion="";
        List<File> schemaFiles = SchemaUpdater.listSchemaFiles(schemaDir, schemaPrefix, "3.9.3", dbSchemaVersion);
        assertEquals("num schema files, new install of 3.9.3", 40, schemaFiles.size());
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
        assertEquals("num schema files, latest version", 40, schemaFiles.size());
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
