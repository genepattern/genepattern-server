package org.genepattern.server.database;

import java.sql.*;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.Sequence;
import org.genepattern.webservice.OmnigeneException;
import org.hibernate.*;
import org.hibernate.cfg.*;
import java.util.*;

public class HibernateUtil {

    private static Logger log = Logger.getLogger(HibernateUtil.class);

    private static  SessionFactory sessionFactory = null;;

    public static synchronized SessionFactory getSessionFactory() {
        if(sessionFactory == null) {
            createSessionFactory();
        }
        return sessionFactory;
    }


    static void createSessionFactory() {
        try {
             // Create the SessionFactory from hibernate.cfg.xml
            Configuration config = new Configuration();
            // config.configure("oracle.cfg.xml");
            config.configure("hibernate.cfg.xml");
            sessionFactory = config.buildSessionFactory();
         }
        catch (Throwable ex) {
            // Make sure you log the exception, as it might be swallowed
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static Session getSession() {

        return getSessionFactory().getCurrentSession();
    }
    
    public static void closeCurrentSession() {
        if(getSession().isOpen()) {
            getSession().close();
        }
    }

    /**
     * get the next available sequence. Sequences are not part of the sql 92
     * standard and are not portable. The syntax for hsql and oracle differ, and
     * MySql doesn't expose sequeneces at all. Thus we use a table based scheme
     * to simulate a sequence.
     * 
     * The method is synchronized to prevent the same sequence number to be
     * handed out to multiple callers (from different threads. For the same
     * reason a new session and transaction is created and closed prior to exit.
     */
    public static synchronized int getNextSequenceValue(String sequenceName) {

        StatelessSession session = null;

        try {
            // Open a new session and transaction. Its neccessary that the
            // sequence update be
            // committed prior to exiting this method.
            session = getSessionFactory().openStatelessSession();
            session.beginTransaction();

            Query query = session
                    .createQuery("from org.genepattern.server.webservice.server.dao.Sequence where name = :name");
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
            session.getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e);
        }
        finally {
            session.close();
        }
    }

}