/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.database;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.genepattern.server.DbException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Unit tests for the SchemaUpdater class.
 * @author pcarr
 *
 */
public class TestSchemaUpdater {
    private File schemaDir=new File("website/WEB-INF/schema");

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
    public void listSchemaFiles_default() {
        final String schemaPrefix="analysis_hypersonic-";
        List<File> schemaFiles = SchemaUpdater.listSchemaFiles(schemaDir, schemaPrefix, null, null);
        assertEquals("num schema files, latest version", 40, schemaFiles.size());
    }

    @Test
    public void dbSchemaFilter_compare() {
        HsqlDbUtil.DbSchemaFilter dbSchemaFilter=new HsqlDbUtil.DbSchemaFilter("analysis_hypersonic-");
        int c=dbSchemaFilter.compare(new File("analysis_hypersonic-3.9.3"), new File("analysis_hypersonic-3.9.3-a"));
        assertEquals("'3.9.3'.compare('3.9.3-a')", true, c<0);
    }
    
    @Test
    public void getLatestSchemaVersionFromSchemaDir() {
        HsqlDbUtil.DbSchemaFilter f=new HsqlDbUtil.DbSchemaFilter("analysis_hypersonic-");
        int c=f.compare(new File("analysis_hypersonic-3.9.3"), new File("analysis_hypersonic-3.9.3-a"));
        assertEquals("'3.9.3'.compare('3.9.3-a')", true, c<0);
        //Strings.
        //assertEquals(
        
        //HsqlDbUtil.getLatestSchemaVersionFromSchemaDir(schemaDir, "");
    }
    
    
}
