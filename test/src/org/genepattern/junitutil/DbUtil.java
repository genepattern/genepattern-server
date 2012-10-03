package org.genepattern.junitutil;

import java.io.File;
import java.io.FilenameFilter;

import org.genepattern.server.database.HsqlDbUtil;

public class DbUtil {
    private static boolean isDbInitialized = false;
    
    public static void initDb() throws Exception { 
        final File hsqlDbDir=new File("junitdb");
        final String hsqlDbName="GenePatternDB";
        final String gpVersion="3.4.2";
        initDb(hsqlDbDir, hsqlDbName, gpVersion);
    }

    public static void initDb(final File hsqlDbDir, final String hsqlDbName, final String gpVersion) throws Exception {
        //some of the classes being tested require a Hibernate Session connected to a GP DB
        if (!isDbInitialized) { 
            final String hibernateConfigFile="hibernate.junit.cfg.xml";
            
            final boolean deleteDbFiles=true;
            if (deleteDbFiles) {
                if (hsqlDbDir.exists()) {
                    File[] todel = hsqlDbDir.listFiles(new FilenameFilter() {
                        public boolean accept(final File dir, final String name) {
                            return name != null && name.startsWith(hsqlDbName);
                        }
                    });
                    for(File old : todel) {
                        if (old.isFile()) {
                            if (!old.canWrite()) {
                                throw new Exception("Error initializing DB, not sufficient permissions to delete old HSQLDB file: "+old.getAbsolutePath());
                            }
                            else {
                                boolean deleted = old.delete();
                                if (!deleted) {
                                    throw new Exception("Error initializing DB, didn't delete old HSQLDB file: "+old.getAbsolutePath());
                                }
                            }
                        }
                    }
                }
            }
            
            //TODO: use DbUnit to improve Hibernate and DB configuration for the unit tests 
            System.setProperty("hibernate.configuration.file", hibernateConfigFile);
            
            //String args = System.getProperty("HSQL.args", " -port 9001  -database.0 file:../resources/GenePatternDB -dbname.0 xdb");
            //System.setProperty("HSQL.args", " -port 9001  -database.0 file:testdb/GenePatternDB -dbname.0 xdb");
            final String path=hsqlDbDir.getPath()+"/"+hsqlDbName;
            System.setProperty("HSQL.args", " -port 9001  -database.0 file:"+path+" -dbname.0 xdb");
            System.setProperty("hibernate.connection.url", "jdbc:hsqldb:hsql://127.0.0.1:9001/xdb");
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
