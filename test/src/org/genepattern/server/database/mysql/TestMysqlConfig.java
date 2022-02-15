/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.database.mysql;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.genepattern.junitutil.ConfigUtil;
import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.SchemaUpdater;
import org.genepattern.server.domain.PropsTable;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.webapp.rest.api.v1.DateUtil;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Example junit test for validating a MySQL connection. These tests are Ignore'd by default.
 * On my MacOS X dev machine I installed MySQL Server and launch it from the Preference Pane.
 *     $ mysql --version
 *     mysql  Ver 14.14 Distrib 5.7.12, for osx10.11 (x86_64) using  EditLine wrapper
 *     
 * @author pcarr
 *
 */
@Ignore 
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestMysqlConfig {

    private static final String dbHost="127.0.0.1";
    
    /**
     * The name of the test database aka schema; 
     * WARNING!
     * don't use a real database this db gets dropped before running the tests
     */
    private static final String dbName="gp_test_db";

    // MySQL config
    // To change the root password from MySQL circa v5.7
    //     mysql> use mysql;
    //     mysql> update user set authentication_string = PASSWORD('<root_password>') where User = '<root_user>';
    private static final String root_jdbcUrl="jdbc:mysql://"+dbHost+":3306/";
    private static final String root_user="root";
    private static final String root_password="1111";
    
    private static GpConfig gpConfig;
    private static HibernateSessionManager mgr;
    
    @BeforeClass
    public static void beforeClass() throws FileNotFoundException, IOException, SQLException {
        createTestDb(dbName);
        testDbConnection(dbHost, dbName);
        
        File workingDir=new File(System.getProperty("user.dir"));
        File webappDir=new File(workingDir, "website");
        
        gpConfig = new GpConfig.Builder()
            .webappDir(webappDir)
            .addProperty(GpConfig.PROP_DATABASE_VENDOR, "MySQL")
        .build();
        mgr=initSessionMgr(dbName); 
    }
    
    /** 
     * Initialize the test database, drops dbName if it already exists, then creates a new dbName 
     * if it doesn't already exist.
     */
    protected static void createTestDb(final String dbName) throws SQLException {
        final Connection conn=DriverManager.getConnection(root_jdbcUrl, root_user, root_password);
        createTestDb(conn, dbName);
    }

    protected static void createTestDb(final Connection conn, final String dbName) throws SQLException {
        try {
            final Statement stmt=conn.createStatement();
            int rval=stmt.executeUpdate("DROP DATABASE IF EXISTS " + dbName);
            final String createStmt="CREATE DATABASE IF NOT EXISTS " + dbName;
            rval=stmt.executeUpdate(createStmt);
            assertEquals("executeUpdate('"+createStmt+"'", 1, rval);
            //rval=stmt.executeUpdate("GRANT all on "+dbName+".* to '"+dbName+"'@'localhost' identified by '"+dbName+"'");
            rval=stmt.executeUpdate("GRANT all on "+dbName+".* to '"+dbName+"' identified by '"+dbName+"'");
            rval=stmt.executeUpdate("flush privileges");
        }
        catch (SQLException e) {
            throw e;
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

    public static Connection getTestDbConnection() throws SQLException {
        final String jdbcUrl="jdbc:mysql://"+dbHost+":3306/"+dbName;
        return DriverManager.getConnection(jdbcUrl, dbName, dbName);
    }

    protected static void testDbConnection(final String dbHost, final String dbName) throws SQLException {
        Connection conn=null;
        try {
            conn = getTestDbConnection();
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

    /**
     * Create an new MySQL session based on the dbName.
     * 
     *     See http://www.javabeat.net/configure-mysql-database-with-hibernate-mappings
     *     See http://stackoverflow.com/questions/4668063/hibernate-use-backticks-for-mysql-but-not-for-hsql
     * 
     * @param dbName the name of the database, e.g. 'gp-test-db'
     */
    protected static HibernateSessionManager initSessionMgr(final String dbName) throws FileNotFoundException, IOException {
        File resourcesDir=new File("resources");
        Properties p=new Properties();
        ConfigUtil.loadPropertiesInto(p, new File(resourcesDir, "database_default.properties"));

        //# MySQL DB specific settings
        p.setProperty("database.vendor", "MySQL");
        p.setProperty("hibernate.connection.driver_class", "com.mysql.jdbc.Driver");
        p.setProperty("hibernate.connection.url", root_jdbcUrl+dbName);
        p.setProperty("hibernate.connection.username", dbName);
        p.setProperty("hibernate.connection.password", dbName);
        p.setProperty("hibernate.default_schema", dbName);
        p.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL5Dialect"); 
        p.setProperty("hibernate.show_sql", "false");

        HibernateSessionManager sessionMgr=new HibernateSessionManager(p);        
        return sessionMgr;
    }
    
    /**
     * Manual SchemaUpdater test with MySQL. 
     * @throws DbException 
     * @throws IOException 
     * @throws FileNotFoundException 
     *   
     * @throws Throwable
     */
    @Test
    public void _02_initDbSchemaMysql() throws DbException {
        DbUtil.assertDbUpdateSchema(gpConfig, mgr);
    }
    
    @Test
    public void _03_insertIntoPropsTable() throws DbException {
        final String key="test_key_"+Math.random();
        String value="TEST_INSERT";
        assertEquals("before insert", null, PropsTable.selectRow(mgr, key));
        assertEquals("before insert", "", PropsTable.selectValue(mgr, key));
        PropsTable.saveProp(mgr, key, value);
        assertEquals("after insert", value, PropsTable.selectValue(mgr, key));
        PropsTable.saveProp(mgr, key, "TEST_UPDATE");
        assertEquals("after update", "TEST_UPDATE", PropsTable.selectValue(mgr, key));
    }
    
    @Test
    public void _04_selectDbSchemaVersion() throws DbException {
        // do a test query
        String dbSchemaVersion=PropsTable.selectValue(mgr, "schemaVersion");
        assertEquals("after update", "3.9.11", dbSchemaVersion); 
    }
    
    @Test
    public void _05_updateDbSchemaVersion() throws FileNotFoundException, IOException, DbException {
        final String origVersion=SchemaUpdater.getDbSchemaVersion(mgr);
        final String schemaPrefix="analysis_mysql-";
        final String updatedVersion="3.9.3-"+Math.random();
        File mockSchemaFile=new File(schemaPrefix+updatedVersion+".sql");
        
        SchemaUpdater.updateDbSchemaVersion(mgr, schemaPrefix, mockSchemaFile);
        assertEquals("after update", updatedVersion, SchemaUpdater.getDbSchemaVersion(mgr));
        SchemaUpdater.updateDbSchemaVersion(mgr, origVersion);
    }
    
    @Test
    public void _06_check_task_docker_image_view() throws SQLException {
        Connection conn=null;
        try {
            conn = getTestDbConnection();
            String selectTableSQL = "SELECT * from TASK_DOCKER_IMAGE";
            final Statement statement = conn.createStatement();
            final ResultSet rs = statement.executeQuery(selectTableSQL);
            int columnCount=rs.getMetaData().getColumnCount();
            assertEquals("columnCount", 4, columnCount);
            assertEquals("columnName[1]", "TASK_NAME", rs.getMetaData().getColumnName(1));
            assertEquals("columnName[2]", "LSID_VERSION", rs.getMetaData().getColumnName(2));
            assertEquals("columnName[3]", "DOCKER_IMAGE", rs.getMetaData().getColumnName(3));
            assertEquals("columnName[4]", "LSID", rs.getMetaData().getColumnName(4));
        }
        catch (SQLException e) {
            throw e;
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
    
    @Test
    public void _07_getNewUsers() {
        UserDAO userDao = new UserDAO(mgr);
        final Date startDate=DateUtil.timeAgoUtc.parseDateTime("2018-01-01 00:00:00").toDate();
        final Date endDate=new Date();
        List<User> newUsers = userDao.getNewUsers(startDate, endDate);
        assertNotNull(newUsers);
        assertEquals("newUsers", 0, newUsers.size());
    }
    
    @Test
    public void _08_check_analysis_job_total_view() throws DbException, SQLException {
        DbUtil.assertAnalysisJobTotalView(mgr);
    }

}
