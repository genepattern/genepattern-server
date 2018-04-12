package org.genepattern.server.database.oracle;

import static org.junit.Assert.assertNotNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Example code for testing Oracle JDBC connection.
 *   Tests are ignored, by default, but can be useful for debugging.
 *   
 * Template database_custom.properties
 * <pre>
   hibernate.connection.driver_class=oracle.jdbc.driver.OracleDriver
   # hibernate.connection.url=jdbc:oracle:thin:<username>/<password>@<hostname>[:<port>]:<sid>
   hibernate.connection.url=jdbc:oracle:thin:scott/tiger@example.com:1521:gptest
   hibernate.username=scott
   hibernate.password=tiger
   hibernate.dialect=org.genepattern.server.database.PlatformOracle9Dialect
   hibernate.default_schema=GP_TEST
 * </pre>
 * 
 * Warning: Make sure you know which DB you are connecting to before
 * modifying these tests.
 *   
 * Links:
 *     https://docs.oracle.com/cd/B19306_01/java.102/b14355/urls.htm#i1070726
 *     https://docs.oracle.com/cd/E14571_01/web.1111/e13737/jdbc_datasources.htm#JDBCA137
 */
public class TestOracleConfig {
    // hibernate.connection.url=jdbc:oracle:thin:<username>/<password>@<hostname>[:<port>]:<sid>
    private String hostname="example.com";
    private String port="1521";
    private String sid="gptest";
    private String username="scott";
    private String password="tiger";
    
    private String jdbc_url="jdbc:oracle:thin:"+username+"/"+password+"@"+hostname+":"+port+":"+sid;
    final String driver_class="oracle.jdbc.driver.OracleDriver"; 

    @Before
    public void setUp() { 
        // Note: set system properties to match your database connection
        jdbc_url=System.getProperty(      "GP_TEST_ORACLE_JDBC_URL", jdbc_url);
        username=System.getProperty( "GP_TEST_ORACLE_JDBC_USERNAME", username);
        password=System.getProperty( "GP_TEST_ORACLE_JDBC_PASSWORD", password);
    }
    
    @Ignore
    @Test
    public void testDbConnection() throws ClassNotFoundException, SQLException {
        Connection conn=null;
        try {
            final Class<?> driverClass = Class.forName(driver_class);
            assertNotNull(driverClass);

            // Option 1: pass user and password in method call
            //   conn = DriverManager.getConnection(jdbc_url, username, password);

            // Option 2: set user, password, (and possible other) as properties
            final Properties props = new Properties();
                props.setProperty("user", username);
                props.setProperty("password", password);
             // cancel after 3 seconds
             DriverManager.setLoginTimeout(3);
             // Note: Must use DriverManager.setLoginTimout because 'loginTimeout' 'connectTimeout' 
             // did not work when I tested with ojdbc14.jar
             //   props.setProperty("loginTimeout", "5"); // 5 seconds
             //   props.setProperty("connectTimeout", "5000"); // 5000 milliseconds
            conn = DriverManager.getConnection(jdbc_url, props);
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

}
