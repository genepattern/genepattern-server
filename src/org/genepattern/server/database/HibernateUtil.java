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
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
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

    public static int getNextSequenceValue(final HibernateSessionManager mgr, final GpConfig gpConfig, final String sequenceName) 
    throws DbException
    {
        final String dbVendor=gpConfig.getDbVendor();
        return getNextSequenceValue(mgr, dbVendor, sequenceName);
    }
    
    public static int getNextSequenceValue(final HibernateSessionManager mgr, final String dbVendor, final String sequenceName) 
    throws DbException
    {
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            if (dbVendor.equalsIgnoreCase("ORACLE")) {
                return getNextSequenceValueOracle(mgr, sequenceName);
            } 
            else if (dbVendor.equalsIgnoreCase("HSQL")) {
                return getNextSequenceValueHsql(mgr, sequenceName);
            } 
            else {
                return getNextSequenceValueGeneric(mgr, sequenceName);
            }
        }
        catch (Throwable t) {
            log.error(t);
            mgr.rollbackTransaction();
            throw new DbException(t);
        }
        finally {
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
        }
    }
    
    protected static int getNextSequenceValueOracle(final HibernateSessionManager mgr, final String sequenceName) {
        return ((BigDecimal) mgr.getSession().createSQLQuery("SELECT " + sequenceName + ".NEXTVAL FROM dual")
                .uniqueResult()).intValue();
    }
    
    protected static int getNextSequenceValueHsql(final HibernateSessionManager mgr, final String sequenceName) {
        return (Integer) mgr.getSession().createSQLQuery("SELECT NEXT VALUE FOR " + sequenceName + " FROM dual").uniqueResult();
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

    /**
     * Experimental code; removed StatelessSession, delegate DB session management to the calling class.
     * 
     * Get the next integer value from the named sequence, using the 'SEQUENCE_TABLE' to simulate a sequence.
     * 
     * @param mgr
     * @param sequenceName
     * @return
     */
    protected static synchronized int getNextSequenceValueGeneric_txn(final HibernateSessionManager mgr, final String sequenceName) {
        final Query query = mgr.getSession().createQuery("from org.genepattern.server.domain.Sequence where name = :name");
        query.setString("name", sequenceName);
        final Sequence seq = (Sequence) query.uniqueResult();
        if (seq != null) {
            int nextValue = seq.getNextValue();
            seq.setNextValue(nextValue + 1);
            mgr.getSession().update(seq);
            return nextValue;
        } 
        else {
            String errorMsg = "Sequence table does not have an entry for: " + sequenceName;
            log.error(errorMsg);
            throw new OmnigeneException(errorMsg);
        }
    }

    /**
     * Add a new entry to the SEQUENCE_TABLE if and only if there is not an existing entry with the given name.
     * Example insert statement:
       <pre>
       insert into SEQUENCE_TABLE (NAME, NEXT_VALUE) values('lsid_identifier_seq', 1);
       </pre>
     * 
     *  Workaround for this exception:
     *      ids for this class must be manually assigned before calling save(): org.genepattern.server.domain.Sequence
     *  This code won't work,
     *  <pre>
         Sequence seq=new Sequence();
         seq.setName(seqName);
         seq.setNextValue(1);
         mgr.getSession().saveOrUpdate(seq);
     *  </pre>
     *  Calling 'seq.setId(... next sequence id ...)' fixes the problem, but it requires knowing the next id. Cannot compute.
     * @param seqName the name of a new entry in the SEQUENCE_TABLE, e.g. 'lsid_identifier_seq'
     * @return The number of rows added
     * 
     * @throws DbException for general DB connection errors
     */
    protected static int createSequence(final HibernateSessionManager mgr, final String seqName) throws DbException {
        final boolean inTxn=mgr.isInTransaction();
        try {
            mgr.beginTransaction();

            // double check if the sequence already exists
            final boolean exists=hasSequence(mgr, seqName);
            if (exists) {
                log.warn("sequence already exists: "+seqName);
                return 0;
            }
            
            final String sql = "insert into SEQUENCE_TABLE (NAME, NEXT_VALUE) values(:seqName, :seqNextValue)";
            final SQLQuery query = mgr.getSession().createSQLQuery(sql);
            query.setString("seqName", seqName);
            query.setInteger("seqNextValue", 1);
            int rval=query.executeUpdate();
            if (!inTxn) {
                mgr.commitTransaction();
            }
            return rval;
        }
        catch (HibernateException e) {
            if (e.getCause() != null) {
                throw new DbException(e.getCause().getLocalizedMessage(), e.getCause());
            }
            throw new DbException(e);
        }
        catch (Throwable t) {
            throw new DbException("Unexpected error: "+t.getLocalizedMessage(), t);
        }
        finally {
            if (!inTxn) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    /**
     * Check if there is already a sequence in the database.
     * 
     * @param mgr
     * @param sequenceName
     * @return a count of the number of rows in the sequence_table with the given sequenceName
     *     0, there is not a sequence
     *     1, there is already a sequence
     */
    protected static boolean hasSequence(final HibernateSessionManager mgr, final String sequenceName) {
        final Query query = mgr.getSession().createQuery("from org.genepattern.server.domain.Sequence where name = :name");
        query.setString("name", sequenceName);
        final Sequence seq = (Sequence) query.uniqueResult();
        if (seq != null) {
            return true;
        }
        return false;
    }

}
