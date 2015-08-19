/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.junitutil;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.genepattern.server.DbException;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.SchemaUpdater;
import org.hibernate.Query;
import org.junit.Ignore;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Ignore
public class DbUtil {
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
    

    /**
     * Add a new user to the database, or ignore if there is already a user with the given userId
     * @param gpConfig, non-null, makes a call to gpConfig.getUserDir(userContext);
     * @param mgr
     * @param userId
     * @return
     * @throws DbException
     */
    public static String addUserToDb(final GpConfig gpConfig, final HibernateSessionManager mgr, final String userId)
    throws DbException
    {
        final boolean isInTransaction = mgr.isInTransaction();
        boolean doClose=!isInTransaction;
        try {
            mgr.beginTransaction();
            final boolean userExists=UserAccountManager.userExists(mgr, userId);
            final String gp_email=null; //can be null
            final String gp_password=null; //can be null
            if (!userExists) {
                UserAccountManager.createUser(gpConfig, mgr, userId, gp_password, gp_email);
                if (!isInTransaction) {
                    mgr.commitTransaction();
                }
            } 
        }
        catch (AuthenticationException e) {
            doClose=true;
            fail("Failed to add user to db, gp_username="+userId+": "+e.getLocalizedMessage());
        }
        catch (Throwable t) {
            doClose=true;
            throw new DbException("Error adding user to db, gp_username="+userId, t);
        }
        finally {
            if (doClose) {
                mgr.closeCurrentSession();
            }
        }
        return userId;
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
