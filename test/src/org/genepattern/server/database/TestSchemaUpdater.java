/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
    private final int numSchemaFiles_3_9_3=24;  // re-wrote history in 3.9.8 release, deleted *-3.3.2.sql

    @Test
    public void initDbSchemaHsqlDb_toLatest() throws ExecutionException {
        HibernateSessionManager mgr=DbUtil.getTestDbSession();
        
        // sanity check
        assertEquals("sanity check for 'props' table", 
                true,
                SchemaUpdater.tableExistsIgnoreCase(mgr, "props"));
        
        assertEquals("tableExists('job_input'), dropped in v3.9.6",
                false,
                SchemaUpdater.tableExistsIgnoreCase(mgr, "job_input"));

        assertEquals("tableExists('job_input_attribute'), dropped in v3.9.6",
                false,
                SchemaUpdater.tableExistsIgnoreCase(mgr, "job_input"));
        
        assertEquals("tableExists('upload_file'), dropped in v3.9.8",
                false,
                SchemaUpdater.tableExistsIgnoreCase(mgr, "upload_file"));
    }
    
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
        assertEquals("num schema files, new install of 3.9.3", numSchemaFiles_3_9_3, schemaFiles.size());
    }

    @Test
    public void listSchemaFiles_emptyDbSchemaVersion() {
        final String schemaPrefix="analysis_hypersonic-";
        final String dbSchemaVersion="";
        List<File> schemaFiles = SchemaUpdater.listSchemaFiles(schemaDir, schemaPrefix, "3.9.3", dbSchemaVersion);
        assertEquals("num schema files, new install of 3.9.3", numSchemaFiles_3_9_3, schemaFiles.size());
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
        assertEquals("num schema files, updating from 3.9.1 to latest", 6, schemaFiles.size());
    }

    @Test
    public void listSchemaFiles_default() {
        final String schemaPrefix="analysis_hypersonic-";
        List<File> schemaFiles = SchemaUpdater.listSchemaFiles(schemaDir, schemaPrefix, null, null);
        final int expectedNumSchemaFiles=28;
        assertEquals("num schema files, latest version", expectedNumSchemaFiles, schemaFiles.size());
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
    public void dbSchemaFilter_compare_v3_to_v10() {
        final DbSchemaFilter dbSchemaFilter=new DbSchemaFilter.Builder().build();
        assertTrue("'3.9.3' < '3.9.10'",  
                dbSchemaFilter.compare(new File("analysis_hypersonic-3.9.3.sql"), new File("analysis_hypersonic-3.9.10.sql"))
                < 0);
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
        final String dbSchemaVersion="3.9.11";
        List<File> schemaFiles = SchemaUpdater.listSchemaFiles(schemaDir, schemaPrefix, expectedSchemaVersion, dbSchemaVersion);
        
        boolean upToDate=SchemaUpdater.isUpToDate(schemaFiles);
        assertEquals("isUpToDate, dbSchemaVersion="+dbSchemaVersion, true, upToDate);
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
    
    @Test
    public void escapeSemicolon_create_trigger_example() throws Exception {
        final String triggerStmtEsc=
                "CREATE TRIGGER testref BEFORE INSERT ON test1\n"+
                        "  FOR EACH ROW\n"+
                        "  BEGIN\n"+
                        "    INSERT INTO test2 SET a2 = NEW.a1\\;\n"+
                        "    DELETE FROM test3 WHERE a3 = NEW.a1\\;\n"+
                        "    UPDATE test4 SET b4 = b4 + 1 WHERE a4 = NEW.a1\\;\n"+
                        "  END;";
        final String triggerStmt=
                "CREATE TRIGGER testref BEFORE INSERT ON test1\n"+
                        "  FOR EACH ROW\n"+
                        "  BEGIN\n"+
                        "    INSERT INTO test2 SET a2 = NEW.a1;\n"+
                        "    DELETE FROM test3 WHERE a3 = NEW.a1;\n"+
                        "    UPDATE test4 SET b4 = b4 + 1 WHERE a4 = NEW.a1;\n"+
                        "  END";
                
        final String all=triggerStmtEsc;
        
        List<String >sqlStatements=SchemaUpdater.extractSqlStatements(all);
        assertEquals("sqlStatements.size", 1, sqlStatements.size());
        assertEquals("sqlStatements[0]", triggerStmt, sqlStatements.get(0));
    }

    @Test
    public void escapeSemicolon_mysql_3_9_11() throws DbException {
        final File schemaFile=new File("website/WEB-INF/schema/analysis_mysql-3.9.11.sql");
        assertTrue("schemaFile.exists: "+schemaFile, schemaFile.exists());
        List<String> sqlStatements=SchemaUpdater.extractSqlStatements(schemaFile);
        assertEquals("sqlStatements.size", 6, sqlStatements.size());
    }

}
