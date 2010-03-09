package edu.mit.broad.core;

import java.sql.Connection;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import edu.mit.broad.core.lsf.LsfWrapper;

/**
 * Override Main BroadCore class to use GenePattern HibernateUtil instead of a JNDI DataSource to connect to the database.
 * 
 * @author Peter Carr
 */
public final class Main {
    private static Logger log = Logger.getLogger(Main.class);
    
    private static Main instance = null;
    private String environment;
    private String dataSourceName;
    private int lsfCheckFrequency = 30;
    private boolean lsfStarted = false;

    private Main() {}

    /**
     *
     * @return
     */
    public static synchronized Main getInstance() {
        if (instance == null) {
            instance = new Main();
        }

        return instance;
    }

    public void setEnvironment(String environment) { 
        log.error("ignoring setEnvironment");
    }
    public String getEnvironment() { 
        return environment; 
    }

    public void setDataSourceName(String dataSourceName) { 
        log.error("ignoring setDataSourceName: "+dataSourceName);
    }
    public String getDataSourceName() { return dataSourceName; }

    public void setHibernateOptions(Properties hibernateOptions) {
        log.error("ignoring setHibernateOptions...");
    }

    public void setLsfCheckFrequency(int lsfCheckFrequency) { 
        this.lsfCheckFrequency = lsfCheckFrequency; 
    }

    /**
     *
     */
    public synchronized void start() {
        if (this.lsfCheckFrequency != 0) {
            this.lsfStarted = new LsfWrapper().start(this.lsfCheckFrequency);
        }
    }

    public synchronized void stop() {
        if (this.lsfStarted) {
            new LsfWrapper().stop();
        }
    }


    /**
     *
     * @return
     */
    public Connection getConnection() {
        log.error("Ignoring getConnection...");
        throw new BroadCoreException("This method not supported!");
    }

    public SessionFactory getHibernateSessionFactory() {
        return HibernateUtil.getSession().getSessionFactory();
    }

    public Session getHibernateSession() {
        return HibernateUtil.getSession();
    }
}