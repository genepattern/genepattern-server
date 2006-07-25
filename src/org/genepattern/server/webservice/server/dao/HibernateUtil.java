package org.genepattern.server.webservice.server.dao;

import java.sql.*;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.*;
import org.hibernate.cfg.*;
import java.util.*;

public class HibernateUtil {

    private static Logger log = Logger.getLogger(HibernateUtil.class);

    private static final SessionFactory sessionFactory;

    static {
        try {
            // Create the SessionFactory from hibernate.cfg.xml
            sessionFactory = new Configuration().configure().buildSessionFactory();
        }
        catch (Throwable ex) {
            // Make sure you log the exception, as it might be swallowed
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static Session getSession() {
        return getSessionFactory().getCurrentSession();
    }

    public static StatelessSession getStatelessSession() {
        return getSessionFactory().openStatelessSession();
    }

}