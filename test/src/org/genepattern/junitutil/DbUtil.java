package org.genepattern.junitutil;

import java.io.File;

import org.genepattern.server.database.HsqlDbUtil;

public class DbUtil {
    private static boolean isDbInitialized = false;
    private static String gpVersion="3.4.2";
    
    public static void initDb() throws Exception { 
        //some of the classes being tested require a Hibernate Session connected to a GP DB
        if (!isDbInitialized) {
            //TODO: use DbUnit to improve Hibernate and DB configuration for the unit tests 
            System.setProperty("hibernate.configuration.file", "hibernate.junit.cfg.xml");
            
            //String args = System.getProperty("HSQL.args", " -port 9001  -database.0 file:../resources/GenePatternDB -dbname.0 xdb");
            System.setProperty("HSQL.args", " -port 9001  -database.0 file:testdb/GenePatternDB -dbname.0 xdb");
            System.setProperty("hibernate.connection.url", "jdbc:hsqldb:hsql://127.0.0.1:9001/xdb");
            //System.setProperty("GenePatternVersion", "3.3.3");
            System.setProperty("GenePatternVersion", gpVersion);

            File resourceDir = new File("resources");
            String pathToResourceDir = resourceDir.getAbsolutePath();
            System.setProperty("genepattern.properties", pathToResourceDir);
            System.setProperty("resources", pathToResourceDir);

            try {
                isDbInitialized = true;
                HsqlDbUtil.startDatabase();
            }
            catch (Throwable t) {
                //the unit tests can pass even if db initialization fails, so ...
                // ... try commenting this out if it gives you problems
                throw new Exception("Error initializing test database", t);
            }
        }
    }

}
