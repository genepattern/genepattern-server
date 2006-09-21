package org.genepattern.server.domain;

// Generated Sep 21, 2006 12:36:06 PM by Hibernate Tools 3.1.0.beta5

import java.util.List;
import javax.naming.InitialContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Example;

/**
 * Home object for domain model class SuiteModules.
 * 
 * @see org.genepattern.server.domain.SuiteModules
 * @author Hibernate Tools
 */
public class SuiteModulesHome {

    private static final Log log = LogFactory.getLog(SuiteModulesHome.class);

    private final SessionFactory sessionFactory = getSessionFactory();

    protected SessionFactory getSessionFactory() {
        try {
            return (SessionFactory) new InitialContext().lookup("SessionFactory");
        }
        catch (Exception e) {
            log.error("Could not locate SessionFactory in JNDI", e);
            throw new IllegalStateException("Could not locate SessionFactory in JNDI");
        }
    }

    public void persist(SuiteModules transientInstance) {
        log.debug("persisting SuiteModules instance");
        try {
            HibernateUtil.getSession().persist(transientInstance);
            log.debug("persist successful");
        }
        catch (RuntimeException re) {
            log.error("persist failed", re);
            throw re;
        }
    }

    public void attachDirty(SuiteModules instance) {
        log.debug("attaching dirty SuiteModules instance");
        try {
            HibernateUtil.getSession().saveOrUpdate(instance);
            log.debug("attach successful");
        }
        catch (RuntimeException re) {
            log.error("attach failed", re);
            throw re;
        }
    }

    public void attachClean(SuiteModules instance) {
        log.debug("attaching clean SuiteModules instance");
        try {
            HibernateUtil.getSession().lock(instance, LockMode.NONE);
            log.debug("attach successful");
        }
        catch (RuntimeException re) {
            log.error("attach failed", re);
            throw re;
        }
    }

    public void delete(SuiteModules persistentInstance) {
        log.debug("deleting SuiteModules instance");
        try {
            HibernateUtil.getSession().delete(persistentInstance);
            log.debug("delete successful");
        }
        catch (RuntimeException re) {
            log.error("delete failed", re);
            throw re;
        }
    }

    public SuiteModules merge(SuiteModules detachedInstance) {
        log.debug("merging SuiteModules instance");
        try {
            SuiteModules result = (SuiteModules) sessionFactory.getCurrentSession().merge(detachedInstance);
            log.debug("merge successful");
            return result;
        }
        catch (RuntimeException re) {
            log.error("merge failed", re);
            throw re;
        }
    }

    public SuiteModules findById(java.lang.Integer id) {
        log.debug("getting SuiteModules instance with id: " + id);
        try {
            SuiteModules instance = (SuiteModules) sessionFactory.getCurrentSession().get(
                    "org.genepattern.server.domain.SuiteModules", id);
            if (instance == null) {
                log.debug("get successful, no instance found");
            }
            else {
                log.debug("get successful, instance found");
            }
            return instance;
        }
        catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }
    }

    public List findByExample(SuiteModules instance) {
        log.debug("finding SuiteModules instance by example");
        try {
            List results = HibernateUtil.getSession().createCriteria("org.genepattern.server.domain.SuiteModules").add(
                    Example.create(instance)).list();
            log.debug("find by example successful, result size: " + results.size());
            return results;
        }
        catch (RuntimeException re) {
            log.error("find by example failed", re);
            throw re;
        }
    }
}
