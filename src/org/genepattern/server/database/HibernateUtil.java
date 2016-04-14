/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.database;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.domain.Sequence;
import org.genepattern.webservice.OmnigeneException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;


public class HibernateUtil {
    private static final Logger log = Logger.getLogger(HibernateUtil.class);
    
    public static HibernateSessionManager instance() {
        return SessionMgr.INSTANCE;
    }
    
    private static class SessionMgr {
        private static final HibernateSessionManager INSTANCE=init();
        
        private static final HibernateSessionManager init() {
            log.debug("initializing hibernate session ...");
            GpContext serverContext=GpContext.getServerContext();
            GpConfig gpConfig=ServerConfigurationFactory.instance();
            return initFromConfig(gpConfig, serverContext);
        }
        
        private SessionMgr() {
        }
    }
    
    protected static HibernateSessionManager initFromConfig(final GpConfig gpConfig, final GpContext gpContext) {
        Properties hibProps=gpConfig.getDbProperties();
        
        if (hibProps==null) {
            final String legacyConfigFile = gpConfig.getGPProperty(gpContext, "hibernate.configuration.file");
            
            if (legacyConfigFile==null) {
                log.warn("Using hard-coded database properties");
                // use hard-coded DB properties
                hibProps=gpConfig.getDbPropertiesDefault(gpContext);
            }
            
            if (legacyConfigFile != null) {
                // fallback to pre 3.9.0 implementation
                log.warn("Using deprecated (pre-3.9.0) database configuration, hibernate.configuration.file="+legacyConfigFile);
                final String jdbcUrl=null;
                return new HibernateSessionManager(legacyConfigFile, jdbcUrl);
            }

        }

        return new HibernateSessionManager(hibProps);
    }

    public static final Session getSession() {
        return instance().getSession();
    }
    
    public static final SessionFactory getSessionFactory() {
        return instance().getSessionFactory();
    }
    
    
    /**
     * Close the current session, if open.
     * 
     */
    public static void closeCurrentSession() {
        instance().closeCurrentSession();
    }

    /**
     * If the current session has an open transaction commit it and close the current session, otherwise do nothing.
     */
    public static void commitTransaction() {
        instance().commitTransaction();
    }

    /**
     * If the current session has an open transaction roll it back and close the current session, otherwise do nothing.
     * 
     */
    public static void rollbackTransaction() {
        instance().rollbackTransaction();
    }

    /**
     * Begin a new transaction. If a transaction is in progress do nothing.
     * 
     * @return
     */
    public static void beginTransaction() {
        instance().beginTransaction();
    }

    public static boolean isInTransaction() {
        return instance().isInTransaction();
    }

    /**
     * Consolidate SuppressWarnings into one method call.
     * @param query
     * @return
     */
    public static <T> List<T> listUnchecked(final Query query) {
        @SuppressWarnings("unchecked")
        List<T> list = query.list();
        return list;
    }

    public static void executeSQL(final HibernateSessionManager mgr, final String sql) throws DbException {
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            if (!isInTransaction) {
                mgr.beginTransaction();
            } 
            Statement updateStatement = null;
            updateStatement = mgr.getSession().connection().createStatement();
            @SuppressWarnings("unused")
            int rval=updateStatement.executeUpdate(sql);
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
        }
        catch (SQLException e) {
            throw new DbException("Unexpected SQLException executing sql='"+sql+"': "+e.getLocalizedMessage(), e);
        }
        catch (Throwable t) {
            throw new DbException("Unexpected error executing sql='"+sql+"': "+t.getLocalizedMessage(), t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

    public static int getNextSequenceValue(final HibernateSessionManager mgr, final GpConfig gpConfig, final String sequenceName) {
        final String dbVendor=gpConfig.getDbVendor();
        return getNextSequenceValue(mgr, dbVendor, sequenceName);
    }
    
    public static int getNextSequenceValue(final HibernateSessionManager mgr, final String dbVendor, final String sequenceName) {
        if (dbVendor.equalsIgnoreCase("ORACLE")) {
            return ((BigDecimal) mgr.getSession().createSQLQuery("SELECT " + sequenceName + ".NEXTVAL FROM dual")
                    .uniqueResult()).intValue();
        } 
        else if (dbVendor.equalsIgnoreCase("HSQL")) {
            return (Integer) mgr.getSession().createSQLQuery("SELECT NEXT VALUE FOR " + sequenceName + " FROM dual").uniqueResult();
        } 
        else {
            return getNextSequenceValueGeneric(mgr, sequenceName);
        }
    }

    /**
     * get the next available sequence. Sequences are not part of the sql 92 standard and are not portable. The syntax
     * for hsql and oracle differ, and MySql doesn't expose sequeneces at all. Thus we use a table based scheme to
     * simulate a sequence.
     * 
     * The method is synchronized to prevent the same sequence number to be handed out to multiple callers (from
     * different threads. For the same reason a new session and transaction is created and closed prior to exit.
     */
    protected static synchronized int getNextSequenceValueGeneric(final HibernateSessionManager mgr, final String sequenceName) {
        StatelessSession session = null;
        try {
            // Open a new session and transaction. 
            // It's necessary that the sequence update be committed prior to exiting this method.
            SessionFactory sessionFactory = mgr.getSessionFactory();
            if (sessionFactory == null) {
                throw new ExceptionInInitializerError("Hibernate session factory is not initialized");
            }
            session = sessionFactory.openStatelessSession();
            session.beginTransaction();

            Query query = session.createQuery("from org.genepattern.server.domain.Sequence where name = :name");
            query.setString("name", sequenceName);
            Sequence seq = (Sequence) query.uniqueResult();
            if (seq != null) {
                int nextValue = seq.getNextValue();
                seq.setNextValue(nextValue + 1);
                session.update(seq);
                session.getTransaction().commit();
                return nextValue;
            } 
            else {
                session.getTransaction().rollback();
                String errorMsg = "Sequence table does not have an entry for: " + sequenceName;
                log.error(errorMsg);
                throw new OmnigeneException(errorMsg);
            }
        } 
        catch (Exception e) {
            if (session != null) {
                Transaction tx = session.getTransaction();
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                    session.close();
                }
            }
            log.error(e);
            throw new OmnigeneException(e);
        } 
        finally {
            if (session != null) {
                session.close();
            }
        }
    }
}
