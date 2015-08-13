/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.junitutil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.genepattern.server.DbException;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HsqlDbUtil;
import org.genepattern.server.database.SchemaUpdater;
import org.hibernate.Query;
import org.junit.Assert;
import org.junit.Ignore;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Ignore
public class DbUtil {
    private static boolean isDbInitialized = false;
    public static final File schemaDir=new File("website/WEB-INF/schema");
    private static LoadingCache<String,HibernateSessionManager> testDbCache=initTestDbCache();
    private static LoadingCache<String,HibernateSessionManager> initTestDbCache() {
        return CacheBuilder.newBuilder().build(
                new CacheLoader<String,HibernateSessionManager>() {
                    public HibernateSessionManager load(String key) throws DbException {
                        return initTestDbSession(key);
                    }
                }
                );
    }
    
    public enum DbType {
        HSQLDB,
        MYSQL;
    }
    
    public static HibernateSessionManager getTestDbSession() throws ExecutionException {
        return getTestDbSession("testdb");
    }
    
    public static HibernateSessionManager getTestDbSession(final String dbName) throws ExecutionException {
        return testDbCache.get(dbName);
    }
    
    public static HibernateSessionManager initTestDbSession(final String dbName) throws DbException {
        HibernateSessionManager mgr=initSessionMgrHsqlInMemory(dbName);
        SchemaUpdater.updateSchema(mgr, schemaDir, "analysis_hypersonic-");
        return mgr;
    }
    
    /**
     * Create a new HSQL DB connection to a "Memory-Only Database". The db is not persistent and exists entirely in RAM. 
     * No need to start it up or shut it down.
     * 
     * @param dbName, a unique name for the database, to allow for multiple instances in the same JVM. Must be a lower-case single-word identifier.
     * @return
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static HibernateSessionManager initSessionMgrHsqlInMemory(final String dbName) {
        final String connectionUrl="jdbc:hsqldb:mem:"+dbName;
        return initSessionMgrHsql(connectionUrl);
    }
    
    /**
     * Create a new HSQL DB connection in "In-Process (Standalone) Mode". The DB is saved directly to the file system
     * without any network I/O. This is suitable for testing.
     * 
     * @param dbDir, a directory for saving HSQL DB database files.
     * @param dbName, a unique name for the database, default is "GenePatternDB"
     * @return
     */
    protected static HibernateSessionManager initSessionMgrHsqlInProcess(final File dbDir, final String dbName) throws IOException, FileNotFoundException {
        final File hsqlDbFile=new File(dbDir, dbName);
        final String connectionUrl="jdbc:hsqldb:file:"+hsqlDbFile;
        return initSessionMgrHsql(connectionUrl);
    }
    
    protected static HibernateSessionManager initSessionMgrHsql(final String connectionUrl) {
        // init common properties
        final Properties p=new Properties();
        p.setProperty("hibernate.current_session_context_class", "thread");
        p.setProperty("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
        p.setProperty("hibernate.username", "sa");
        p.setProperty("hibernate.password", "");
        p.setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
        p.setProperty("hiberate.default_schema", "PUBLIC");
        p.setProperty("database.vendor", "HSQL");
        // custom connectionUrl
        p.setProperty("hibernate.connection.url", connectionUrl);

        HibernateSessionManager mgr=new HibernateSessionManager(p);
        return mgr;
    }

    /** @deprecated, TODO: make private, remove from project */
    public static void initDb() throws Exception {
        initDb(DbType.HSQLDB);
    }

    /** @deprecated */
    public static void initDb(DbType dbType) throws Exception {
        initDb( org.genepattern.server.database.HibernateUtil.instance(), dbType);
    }

    /**
     * To help with debugging turn off batch mode by setting this property before you call initDb.
     * <pre>
       System.setProperty("hibernate.jdbc.factory_class", "org.hibernate.jdbc.NonBatchingBatcherFactory");
     * </pre>
     * @throws Exception
     */
    public static void initDb(HibernateSessionManager mgr) throws Exception {
        initDb( mgr, DbType.HSQLDB);
    }
    
    /**
     * Added for manually switching between HSQLDB and MySQL databases. By default, use HSQLDB.
     * @param dbType
     * @throws Exception
     */
    private static void initDb(final HibernateSessionManager mgr, DbType dbType) throws Exception {
        if (dbType==DbType.HSQLDB) {
            DbUtil.initDbDefault(mgr);
        }
        else if (dbType==DbType.MYSQL) {
            System.setProperty("hibernate.configuration.file", "hibernate.mysql.cfg.xml");
        }
    }
    
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
    
    protected static void initDbDefault(final HibernateSessionManager mgr) throws Exception { 
        final File hsqlDbDir=new File("junitdb");
        final String hsqlDbName="GenePatternDB";
        final String gpVersion="3.9.3";
        initDb(mgr, hsqlDbDir, hsqlDbName, gpVersion);
    }

//    protected static void initDb(final File hsqlDbDir, final String hsqlDbName, final String gpVersion) throws Exception {
//    }
    protected static void initDb(final HibernateSessionManager mgr, final File hsqlDbDir, final String hsqlDbName, final String gpVersion) throws Exception {
        //some of the classes being tested require a Hibernate Session connected to a GP DB
        if (!isDbInitialized) { 
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
            
            final String path=hsqlDbDir.getPath()+"/"+hsqlDbName;
            final String hsqlArgs=" -port 9001  -database.0 file:"+path+" -dbname.0 xdb";

            final String hibernateConfigFile="hibernate.junit.cfg.xml";
            final String hibernateConnectionUrl="jdbc:hsqldb:hsql://127.0.0.1:9001/xdb";
            System.setProperty("hibernate.configuration.file", hibernateConfigFile);
            System.setProperty("hibernate.connection.url", hibernateConnectionUrl);
            System.setProperty("GenePatternVersion", gpVersion);

            File resourceDir = new File("resources");
            String pathToResourceDir = resourceDir.getAbsolutePath();
            System.setProperty("genepattern.properties", pathToResourceDir);
            System.setProperty("resources", pathToResourceDir);

            try {
                isDbInitialized = true;
                HsqlDbUtil.startDatabase(hsqlArgs);
                SchemaUpdater.updateSchema(mgr, schemaDir, "analysis_hypersonic-", gpVersion);
            }
            catch (Throwable t) {
                //the unit tests can pass even if db initialization fails, so ...
                // ... try commenting this out if it gives you problems
                throw new Exception("Error initializing test database", t);
            }
        }
    }

//    public static void startDb(final File hsqlDbDir, final String hsqlDbName) throws Throwable {
//        if (isDbInitialized) {
//            return;
//        }
//        isDbInitialized = true;
//        final String path=hsqlDbDir.getPath()+"/"+hsqlDbName;
//        final String hsqlArgs=" -port 9001  -database.0 file:"+path+" -dbname.0 xdb";
//        HsqlDbUtil.startDatabase(hsqlArgs);
//    }
//    
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

    /** @deprecated */
    static public String addUserToDb(final String gp_username) {
        return addUserToDb(ServerConfigurationFactory.instance(), org.genepattern.server.database.HibernateUtil.instance(), gp_username);
    }
    
    static public String addUserToDb(final GpConfig gpConfig, final HibernateSessionManager mgr, final String gp_username) {
        final boolean isInTransaction = mgr.isInTransaction();
        final boolean userExists=UserAccountManager.userExists(mgr, gp_username);
        final String gp_email=null; //can be null
        final String gp_password=null; //can be null
        
        if (!userExists) {
            try {
                UserAccountManager.createUser(gpConfig, mgr, gp_username, gp_password, gp_email);
                if (!isInTransaction) {
                    mgr.commitTransaction();
                }
            }
            catch (AuthenticationException e) {
                Assert.fail("Failed to add user to db, gp_username="+gp_username+": "+e.getLocalizedMessage());
            }
            finally {
                if (!isInTransaction) {
                    mgr.closeCurrentSession();
                }
            }
        } 
        return gp_username;
    }
    
    /** @deprecated */
    public static int deleteAllRows(final Class<?> entityClass) throws Exception {
        return deleteAllRows(org.genepattern.server.database.HibernateUtil.instance(), entityClass);
    }
    
    /**
     * Remove all rows from the database, for the given hibernate mapped entity.
     * 
     * @param entityClass
     * @return
     * @throws Exception
     */
    public static int deleteAllRows(final HibernateSessionManager mgr, final Class<?> entityClass) throws Exception {
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            final String hql="delete from "+entityClass.getSimpleName();
            mgr.beginTransaction();
            final Query query=mgr.getSession().createQuery(hql);
            int numDeleted=query.executeUpdate();
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
            return numDeleted;
        }
        catch (Throwable t) {
            mgr.rollbackTransaction();
            throw new Exception("Error deleting items from entityClass="+entityClass.getSimpleName(), t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

}
