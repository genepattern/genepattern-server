/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2008) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.database;

import java.math.BigDecimal;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.Sequence;
import org.genepattern.webservice.OmnigeneException;
import org.hibernate.*;
import org.hibernate.cfg.*;
import java.util.*;

public class HibernateUtil {

    private static Logger log = Logger.getLogger(HibernateUtil.class);

    private static SessionFactory sessionFactory = null;;

    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            createSessionFactory();
        }
        return sessionFactory;
    }

    static void createSessionFactory() {
        try {
            // Create the SessionFactory from hibernate.cfg.xml
            Configuration config = new Configuration();
            config.configure("hibernate.cfg.xml");
            mergeSystemProperties(config);
            sessionFactory = config.buildSessionFactory();

        } catch (Throwable ex) {
            // Make sure you log the exception, as it might be swallowed
        	if (log != null) {
        	     log.error("Initial SessionFactory creation failed.", ex);
        	}
        	else {
        	    System.err.println("Initial SessionFactory creation failed: "+ex.getLocalizedMessage());
        	    ex.printStackTrace(System.err);
        	}
            throw new ExceptionInInitializerError(ex);
        }
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

    public static Session getSession() {
        return getSessionFactory().getCurrentSession();
    }

    /**
     * Close the current session, if open.
     * 
     */
    public static void closeCurrentSession() {
        log.debug("closeCurrentSession...");
        
        Session session = getSession();
        if (session.isOpen()) {
            log.debug("   session.close...");
            session.close();
        }
        
        log.debug("...closeCurrentSession");
    }

    /**
     * If the current session has an open transaction commit it and close the current session, otherwise do nothing.
     * 
     */
    public static void commitTransaction() {
        log.debug("commitTransaction...");

        Session session = getSession();
        Transaction tx = session.getTransaction();
        if (tx.isActive()) {
            log.debug("   tx.commit...");
            tx.commit();
            closeCurrentSession();
        }
        
        log.debug("...commitTransaction");
    }

    /**
     * If the current session has an open transaction roll it back and close the current session, otherwise do nothing.
     * 
     */
    public static void rollbackTransaction() {
        log.debug("rollbackTransaction...");

        Session session = getSession();
        Transaction tx = session.getTransaction();
        if (tx.isActive()) {
            log.debug("   tx.rollback...");
            tx.rollback();
            closeCurrentSession();
        }
        
        log.debug("...rollbackTransaction");
    }

    /**
     * Begin a new transaction. If a transaction is in progress do nothing.
     * 
     * @return
     */
    public static void beginTransaction() {
        log.debug("beginTransaction...");
        
        Session session = getSession();
        Transaction tx = session.getTransaction();
        if (!tx.isActive()) {
            log.debug("   session.beginTransaction...");
            session.beginTransaction();
        }
        
        log.debug("...beginTransaction");
    }

    public static boolean isInTransaction() {
       
        Session session = getSession();
        Transaction tx = session.getTransaction();
        
        log.debug("   session.isInTransaction " + tx.isActive());
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
        } else if (dbVendor.equals("HSQL")) {
            return (Integer) getSession().createSQLQuery("SELECT NEXT VALUE FOR " + sequenceName + " FROM dual")
                    .uniqueResult();
        } else {
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
            // Open a new session and transaction. Its neccessary that the
            // sequence update be
            // committed prior to exiting this method.
            session = getSessionFactory().openStatelessSession();
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
            } else {
                session.getTransaction().rollback();
                String errorMsg = "Sequence table does not have an entry for: " + sequenceName;
                log.error(errorMsg);
                throw new OmnigeneException(errorMsg);
            }
        } catch (Exception e) {
            session.getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e);
        } finally {
            session.close();
        }
    }

}
