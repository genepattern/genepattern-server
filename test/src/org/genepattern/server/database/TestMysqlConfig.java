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
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.domain.PropsTable;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Example junit test for validating a MySQL connection. These tests are Ignore'd by default.
 * 
 * On my MacOS X dev machine I installed and started MySQL via GUI.
 * I created the DB from the command line:
 * <pre>
mysql -u admin
create database gpdev;
drop database gpdev;
 * </pre>
 * 
 * @author pcarr
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Ignore
public class TestMysqlConfig {
    private final static String username="gpdev";
    private final static String password="gpdev";
    private final static String dbSchema="gpdev";
    private static final String jdbcUrl="jdbc:mysql://127.0.0.1:3306/gpdev";
    
    private GpConfig gpConfig;
    private static HibernateSessionManager mgr;
    
    @Before
    public void setUp() throws FileNotFoundException, IOException {
        File workingDir=new File(System.getProperty("user.dir"));
        File webappDir=new File(workingDir, "website");
        
        gpConfig = new GpConfig.Builder()
            .webappDir(webappDir)
            .addProperty(GpConfig.PROP_DATABASE_VENDOR, "MySQL")
        .build();
        
        mgr=initSessionMgr();
    }
    
    // Note: http://stackoverflow.com/questions/4668063/hibernate-use-backticks-for-mysql-but-not-for-hsql
    protected static HibernateSessionManager initSessionMgr() throws FileNotFoundException, IOException {
        File resourcesDir=new File("resources");
        Properties p=new Properties();
        ConfigUtil.loadPropertiesInto(p, new File(resourcesDir, "database_default.properties"));

        //# MySQL DB specific settings
        //# See more at: http://www.javabeat.net/configure-mysql-database-with-hibernate-mappings
        p.setProperty("database.vendor", "MySQL");
        p.setProperty("hibernate.connection.driver_class", "com.mysql.jdbc.Driver");
        p.setProperty("hibernate.connection.url", jdbcUrl);
        p.setProperty("hibernate.connection.username", username);
        p.setProperty("hibernate.connection.password", password);
        p.setProperty("hibernate.default_schema", dbSchema);
        p.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL5Dialect"); 
        p.setProperty("hibernate.show_sql", "false");
        
        HibernateSessionManager sessionMgr=new HibernateSessionManager(p);        
        return sessionMgr;
    }
    
    @Test
    public void _01_driverManagerGetConnection() throws Throwable {
        Connection conn=null;
        try {
            conn = DriverManager.getConnection(jdbcUrl, username, password);
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
     * Manual SchemaUpdater test with MySQL. 
     * @throws DbException 
     * @throws IOException 
     * @throws FileNotFoundException 
     *   
     * @throws Throwable
     */
    @Test
    public void _02_initDbSchemaMysql() throws DbException, FileNotFoundException, IOException {
        // from empty string to null means run all DDL scripts
        final String fromVersion=""; 
        final String toVersion=null;
        
        String dbSchemaVersion=SchemaUpdater.getDbSchemaVersion(mgr);
        assertEquals("before update", fromVersion, dbSchemaVersion);
        assertEquals("before update, 'props' table exists", !"".equals(fromVersion), SchemaUpdater.tableExists(mgr, "props"));
        assertEquals("before update, 'PROPS' table exists", false, SchemaUpdater.tableExists(mgr, "PROPS"));

        SchemaUpdater.updateSchema(gpConfig, mgr, toVersion);
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
        assertEquals("after update", "3.9.3", dbSchemaVersion); 
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

}
