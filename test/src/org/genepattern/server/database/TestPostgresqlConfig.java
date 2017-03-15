/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.database;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import org.genepattern.junitutil.ConfigUtil;
import org.genepattern.server.domain.PropsTable;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.postgresql.util.PSQLException;

/**
 * Tests for the PostgreSQL database integration.
 * Requires a connection to a test database. 
 * 
 * To run the tests ...
 *     (1) install and/or start the test database instance
 *     (2) configure the DB to require passwords
 *     (3) create the user and database
 *     
 * I tested with the Postgress.app on a Mac OS X machine; running PostgreSQL (v. 9.5.4).
 *     See: http://postgresapp.com/
 *     
 * If necessary, edit the pg_hba.conf file to require password authentication. 
 * Hint: 
 *     SHOW hba_file;
 * e.g., ~/Library/Application Support/Postgres/var-9.5/pg_hba.conf
 * 
 * Example pg_hba.conf file:
<pre>
# "local" is for Unix domain socket connections only                                                                                                                                         
local   genepattern     genepattern                             password
local   all             all                                     trust
# IPv4 local connections:                                                                                                                                                                    
host    genepattern     genepattern     127.0.0.1/32            password
host    all             all             127.0.0.1/32            trust
</pre>
  *
  * To create the user and database, connect to the DB with the 'psql' command 
<pre>
create user genepattern;
alter user "genepattern" WITH PASSWORD 'test_password';
create database genepattern with owner = genepattern;
</pre>
  *
  * If necessary, start with a clean db before running the tests.
<pre>
drop database genepattern;
create database genepattern with owner = genepattern;
</pre>
 *
 * These tests are ignored because we do not automatically have access to a test DB.
 * @author pcarr
 *
 */
@Ignore
public class TestPostgresqlConfig {
    private final String hostname="127.0.0.1";
    private final String port="5432";
    private final String dbName="genepattern";
    private final String username="genepattern";
    private final String password="test_password";
    private final String dbSchema="public";
    private final String jdbcUrl="jdbc:postgresql://"+hostname+":"+port+"/"+dbName+"?user="+username+"&password="+password;
    
    private File resourcesDir;
    
    @Before
    public void setUp() {
        File workingDir=new File(System.getProperty("user.dir"));
        resourcesDir=new File(workingDir, "resources");
    }
    
    /**
     * test the database connection
     * 
     * @throws Throwable
     */
    @Test
    public void testPostgresqlConnection() throws Throwable {
        Connection conn=null;
        try {
            conn = DriverManager.getConnection(jdbcUrl);
        }
        catch (Throwable t) {
            throw t;
        }
        finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    @Rule
    public ExpectedException ex = ExpectedException.none();

    /** verify that the test DB requires any password. */
    @Test
    public void missingPassword() throws Throwable {
        final String jdbcUrl="jdbc:postgresql://"+hostname+":"+port+"/"+dbName+"?user="+username;
        
        Connection conn=null;
        try {
            ex.expect(PSQLException.class);
            ex.expectMessage("The server requested password-based authentication, but no password was provided.");
            conn = DriverManager.getConnection(jdbcUrl);
        }
        finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    /** verify that the test DB requires a valid password */
    @Test
    public void incorrectPassword() throws Throwable {
        final String jdbcUrl="jdbc:postgresql://"+hostname+":"+port+"/"+dbName+"?user="+username+"&password=incorrect_password";
        Connection conn=null;
        try {
            ex.expect(PSQLException.class);
            ex.expectMessage("FATAL: password authentication failed for user \""+username+"\"");
            conn = DriverManager.getConnection(jdbcUrl);
        }
        finally {
            if (conn != null) {
                conn.close();
            }
        }
    }
    
    protected HibernateSessionManager initSessionMgrPostgreSQL() throws FileNotFoundException, IOException {
        Properties p=new Properties();
        ConfigUtil.loadPropertiesInto(p, new File(resourcesDir, "database_default.properties"));

        p.setProperty("hibernate.current_session_context_class", "thread");
        p.setProperty("database.vendor", "postgresql");
        p.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
        p.setProperty("hibernate.connection.url", jdbcUrl);
        p.setProperty("hibernate.default_schema", dbSchema);
        p.setProperty("hibernate.dialect", CustomPostgreSQLDialact.class.getName());
        p.setProperty("hibernate.show_sql", "false");
        p.setProperty("hibernate.temp.use_jdbc_metadata_defaults", "false");

        HibernateSessionManager sessionMgr=new HibernateSessionManager(p);        
        return sessionMgr;
    }
    
    /**
     * PostgeSQL integration test. 
     * Initializes the genepattern schema by running through all of the DDL scripts.
     * 
     * To set this up, first delete the DB if necessary, then create a new one.
     * See top-level comments for details.
     *   
     * @throws Throwable
     */
    @Test
    public void initDbSchemaPostresql() throws Throwable {
        final String fromVersion="";
        
        HibernateSessionManager sessionMgr=initSessionMgrPostgreSQL();
        String dbSchemaVersion=SchemaUpdater.getDbSchemaVersion(sessionMgr);
        assertEquals("before update", fromVersion, dbSchemaVersion);
        assertEquals("before update, 'props' table exists", !"".equals(fromVersion), SchemaUpdater.tableExists(sessionMgr, "props"));
        assertEquals("before update, 'PROPS' table exists", false, SchemaUpdater.tableExists(sessionMgr, "PROPS"));

        final String dbVendor="postgresql";
        final File schemaDir=new File("website/WEB-INF/schema");
        SchemaUpdater.updateSchema(sessionMgr, schemaDir, "analysis_"+dbVendor.toLowerCase()+"-");
        
        // do a test query
        dbSchemaVersion=PropsTable.selectValue(sessionMgr, "schemaVersion");
        final String toVersion="3.9.9";
        assertEquals("after update", toVersion, dbSchemaVersion);
    }

}
