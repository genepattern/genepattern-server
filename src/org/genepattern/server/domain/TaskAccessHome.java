package org.genepattern.server.domain;

// Generated Sep 21, 2006 12:36:06 PM by Hibernate Tools 3.1.0.beta5

import java.util.List;
import javax.naming.InitialContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.genepattern.server.util.HibernateUtil;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Example;

/**
 * Home object for domain model class TaskAccess.
 * 
 * @see org.genepattern.server.domain.TaskAccess
 * @author Hibernate Tools
 */
public class TaskAccessHome {

    private static final Log log = LogFactory.getLog(TaskAccessHome.class);
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
    
    public void persist(TaskAccess transientInstance) {
        log.debug("persisting TaskAccess instance");
        try {
            HibernateUtil.getSession().persist(transientInstance);
            log.debug("persist successful");
        }
        catch (RuntimeException re) {
            log.error("persist failed", re);
            throw re;
        }
    }

    public void attachDirty(TaskAccess instance) {
        log.debug("attaching dirty TaskAccess instance");
        try {
            HibernateUtil.getSession().saveOrUpdate(instance);
            log.debug("attach successful");
        }
        catch (RuntimeException re) {
            log.error("attach failed", re);
            throw re;
        }
    }

    public void attachClean(TaskAccess instance) {
        log.debug("attaching clean TaskAccess instance");
        try {
            HibernateUtil.getSession().lock(instance, LockMode.NONE);
            log.debug("attach successful");
        }
        catch (RuntimeException re) {
            log.error("attach failed", re);
            throw re;
        }
    }

    public void delete(TaskAccess persistentInstance) {
        log.debug("deleting TaskAccess instance");
        try {
            HibernateUtil.getSession().delete(persistentInstance);
            log.debug("delete successful");
        }
        catch (RuntimeException re) {
            log.error("delete failed", re);
            throw re;
        }
    }

    public TaskAccess merge(TaskAccess detachedInstance) {
        log.debug("merging TaskAccess instance");
        try {
            TaskAccess result = (TaskAccess) HibernateUtil.getSession().merge(detachedInstance);
            log.debug("merge successful");
            return result;
        }
        catch (RuntimeException re) {
            log.error("merge failed", re);
            throw re;
        }
    }

    public TaskAccess findById(java.lang.Integer id) {
        log.debug("getting TaskAccess instance with id: " + id);
        try {
            TaskAccess instance = (TaskAccess) sessionFactory.getCurrentSession().get(
                    "org.genepattern.server.domain.TaskAccess", id);
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

    public List findByExample(TaskAccess instance) {
        log.debug("finding TaskAccess instance by example");
        try {
            List results = HibernateUtil.getSession().createCriteria("org.genepattern.server.domain.TaskAccess").add(
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
