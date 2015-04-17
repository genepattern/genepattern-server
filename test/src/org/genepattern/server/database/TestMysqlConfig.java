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
import org.junit.Test;

/**
 * Example junit test for validating a MySQL connection.
 * @author pcarr
 *
 */
public class TestMysqlConfig {
    private final String username="gpdev";
    private final String password="gpdev";
    private final String dbSchema="gpdev";
    private final String jdbcUrl="jdbc:mysql://127.0.0.1:3306/gpdev";
    
    private File resourcesDir;
    
    @Before
    public void setUp() {
        File workingDir=new File(System.getProperty("user.dir"));
        resourcesDir=new File(workingDir, "resources");
    }
    
    /**
     * Install the Postgres.app Mac application.
     * From the 'psql' command line:
     * 
     * 1) create a user
     * create user genepattern;
     * 
     * 2) create a database
     * create database genepattern with owner = genepattern;
     * 
     * @throws Throwable
     */
    @Ignore @Test
    public void testMysqlConnection() throws Throwable {
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
    
    protected HibernateSessionManager initSessionMgr() throws FileNotFoundException, IOException {
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
        p.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        p.setProperty("hibernate.show_sql", "true");
        
        HibernateSessionManager sessionMgr=new HibernateSessionManager(p);        
        return sessionMgr;
    }
    
    /**
     * Manual SchemaUpdater test with MySQL. 
     *   
     * @throws Throwable
     */
    @Ignore @Test
    public void initDbSchemaMysql() throws Throwable {
        final String fromVersion="";
        final String toVersion="3.9.3";
        
        HibernateSessionManager sessionMgr=initSessionMgr();
        String dbSchemaVersion=SchemaUpdater.getDbSchemaVersion(sessionMgr);
        assertEquals("before update", fromVersion, dbSchemaVersion);
        assertEquals("before update, 'props' table exists", !"".equals(fromVersion), SchemaUpdater.tableExists(sessionMgr, "props"));
        assertEquals("before update, 'PROPS' table exists", false, SchemaUpdater.tableExists(sessionMgr, "PROPS"));

        final String dbVendor="postgresql";
        final File schemaDir=new File("website/WEB-INF/schema");
        SchemaUpdater.updateSchema(sessionMgr, schemaDir, "analysis_"+dbVendor.toLowerCase()+"-", toVersion);
        
        // do a test query
        dbSchemaVersion=PropsTable.selectValue(sessionMgr, "schemaVersion");
        assertEquals("after update", toVersion, dbSchemaVersion);
    }

}
