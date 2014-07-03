package org.genepattern.junitutil;

import java.io.File;
import java.io.FilenameFilter;

import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.database.HsqlDbUtil;
import org.junit.Assert;
import org.junit.Ignore;

@Ignore
public class DbUtil {
    private static boolean isDbInitialized = false;
    
    @Ignore
    private static class Fnf implements FilenameFilter {
        private final String hsqlDbName;
        public Fnf(final String hsqlDbName) {
            this.hsqlDbName=hsqlDbName;
        }
        public boolean accept(final File dir, final String name) {
            return name != null && name.startsWith(hsqlDbName);
        }
    }
    
    public static void initDb() throws Exception { 
        final File hsqlDbDir=new File("junitdb");
        final String hsqlDbName="GenePatternDB";
        final String gpVersion="3.8.3";
        initDb(hsqlDbDir, hsqlDbName, gpVersion);
    }

    public static void initDb(final File hsqlDbDir, final String hsqlDbName, final String gpVersion) throws Exception {
        //some of the classes being tested require a Hibernate Session connected to a GP DB
        if (!isDbInitialized) { 
            final String hibernateConfigFile="hibernate.junit.cfg.xml";
            
            final boolean deleteDbFiles=true;
            if (deleteDbFiles) {
                if (hsqlDbDir.exists()) {
                    File[] todel = hsqlDbDir.listFiles(new Fnf(hsqlDbName));
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

    public static void shutdownDb() {
        if (!isDbInitialized) {
            return;
        }
        try {
            //log.info("stopping HSQLDB ...");
            HsqlDbUtil.shutdownDatabase();
            isDbInitialized=false;
            //log.info("done!");
        }
        catch (Throwable t) {
            //log.error("Error stopoping HSQLDB: "+t.getLocalizedMessage(), t);
        }
    }
    
    static public String addUserToDb(final String gp_username) {
        final boolean isInTransaction = HibernateUtil.isInTransaction();
        final boolean userExists=UserAccountManager.instance().userExists(gp_username);
        final String gp_email=null; //can be null
        final String gp_password=null; //can be null
        
        if (!userExists) {
            try {
                UserAccountManager.instance().createUser(gp_username, gp_password, gp_email);
                if (!isInTransaction) {
                    HibernateUtil.commitTransaction();
                }
            }
            catch (AuthenticationException e) {
                Assert.fail("Failed to add user to db, gp_username="+gp_username+": "+e.getLocalizedMessage());
            }
            finally {
                if (!isInTransaction) {
                    HibernateUtil.closeCurrentSession();
                }
            }
        } 
        return gp_username;
    }

}
