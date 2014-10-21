/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.database;

import java.math.BigDecimal;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.Sequence;
import org.genepattern.webservice.OmnigeneException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
    private static Logger log = Logger.getLogger(HibernateUtil.class);
    private static ConcurrentMap<String,SessionFactory> sessionFactoryMap = new ConcurrentHashMap<String,SessionFactory>();
    
    static {
        final String hibernateConfigurationFile = System.getProperty("hibernate.configuration.file", "hibernate.cfg.xml");
        final String connectionUrl=null;
        init(hibernateConfigurationFile, connectionUrl);
    }

    public static void init(final String hibernateConfigurationFile, final String connectionUrl) {
        //create the default session factory
        try {
            SessionFactory sessionFactory = createSessionFactory(hibernateConfigurationFile, connectionUrl);
            sessionFactoryMap.put("default", sessionFactory);
        }
        catch (Throwable ex) {
            log.error("Error initializing SessionFactory from '"+hibernateConfigurationFile+"': "+ex);
            log.debug("", ex);
        }
    }

    public static SessionFactory createSessionFactory(String configResource, final String connectionUrl) {
        AnnotationConfiguration config = new AnnotationConfiguration();
        config.configure(configResource);
        mergeSystemProperties(config);
        if (connectionUrl != null) {
            config.setProperty("hibernate.connection.url", connectionUrl);
        }
        return config.buildSessionFactory();
    }

    private static void mergeSystemProperties(Configuration config) {
        Properties props = System.getProperties();
        for (Object key : props.keySet()) {
            String name = (String) key;
            if (name.startsWith("hibernate.")) {
                config.setProperty(name, (String) props.get(name));
            }
        }
    }

    public static void setSessionFactory(final String key, SessionFactory sessionFactory) {
        sessionFactoryMap.put(key, sessionFactory);
    }
    
    public static SessionFactory getSessionFactory() {
        return sessionFactoryMap.get("default");
    }
 
    public static Session getSession() {
        SessionFactory sessionFactory = getSessionFactory();
        if (sessionFactory != null) {
            return sessionFactory.getCurrentSession();
        }
        throw new ExceptionInInitializerError("Hibernate session factory is not initialized");
    }
    
    /**
     * Close the current session, if open.
     * 
     */
    public static void closeCurrentSession() {
        Session session = getSession();
        if (session != null && session.isOpen()) {
            session.close();
        }
    }

    /**
     * If the current session has an open transaction commit it and close the current session, otherwise do nothing.
     */
    public static void commitTransaction() {
        Session session = getSession();
        Transaction tx = session.getTransaction();
        if (tx.isActive()) {
            tx.commit();
            closeCurrentSession();
        }
    }

    /**
     * If the current session has an open transaction roll it back and close the current session, otherwise do nothing.
     * 
     */
    public static void rollbackTransaction() {
        Session session = getSession();
        Transaction tx = session.getTransaction();
        if (tx.isActive()) {
            tx.rollback();
            closeCurrentSession();
        }
    }

    /**
     * Begin a new transaction. If a transaction is in progress do nothing.
     * 
     * @return
     */
    public static void beginTransaction() {
        Session session = getSession();
        Transaction tx = session.getTransaction();
        if (!tx.isActive()) {
            session.beginTransaction();
        }
    }

    public static boolean isInTransaction() {
        Session session = getSession();
        Transaction tx = session.getTransaction();
        if (!tx.isActive()) {
            return false;
        }
        return true;
    }

    public static int getNextSequenceValue(String sequenceName) {
        String dbVendor = System.getProperty("database.vendor", "UNKNOWN");
        if (dbVendor.equals("ORACLE")) {
            return ((BigDecimal) getSession().createSQLQuery("SELECT " + sequenceName + ".NEXTVAL FROM dual")
                    .uniqueResult()).intValue();
        } 
        else if (dbVendor.equals("HSQL")) {
            return (Integer) getSession().createSQLQuery("SELECT NEXT VALUE FOR " + sequenceName + " FROM dual").uniqueResult();
        } 
        else {
            return getNextSequenceValueGeneric(sequenceName);
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
    private static synchronized int getNextSequenceValueGeneric(String sequenceName) {
        StatelessSession session = null;
        try {
            // Open a new session and transaction. 
            // It's necessary that the sequence update be committed prior to exiting this method.
            SessionFactory sessionFactory = getSessionFactory();
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
                session.getTransaction().rollback();
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
