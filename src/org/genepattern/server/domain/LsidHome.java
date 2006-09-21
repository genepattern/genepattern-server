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
 * Home object for domain model class Lsid.
 * 
 * @see org.genepattern.server.domain.Lsid
 * @author Hibernate Tools
 */
public class LsidHome {

    private static final Log log = LogFactory.getLog(LsidHome.class);

    public void persist(Lsid transientInstance) {
        log.debug("persisting Lsid instance");
        try {
            HibernateUtil.getSession().persist(transientInstance);
            log.debug("persist successful");
        }
        catch (RuntimeException re) {
            log.error("persist failed", re);
            throw re;
        }
    }

    public void attachDirty(Lsid instance) {
        log.debug("attaching dirty Lsid instance");
        try {
            HibernateUtil.getSession().saveOrUpdate(instance);
            log.debug("attach successful");
        }
        catch (RuntimeException re) {
            log.error("attach failed", re);
            throw re;
        }
    }

    public void attachClean(Lsid instance) {
        log.debug("attaching clean Lsid instance");
        try {
            HibernateUtil.getSession().lock(instance, LockMode.NONE);
            log.debug("attach successful");
        }
        catch (RuntimeException re) {
            log.error("attach failed", re);
            throw re;
        }
    }

    public void delete(Lsid persistentInstance) {
        log.debug("deleting Lsid instance");
        try {
            HibernateUtil.getSession().delete(persistentInstance);
            log.debug("delete successful");
        }
        catch (RuntimeException re) {
            log.error("delete failed", re);
            throw re;
        }
    }

    public Lsid merge(Lsid detachedInstance) {
        log.debug("merging Lsid instance");
        try {
            Lsid result = (Lsid) HibernateUtil.getSession().merge(detachedInstance);
            log.debug("merge successful");
            return result;
        }
        catch (RuntimeException re) {
            log.error("merge failed", re);
            throw re;
        }
    }

    public Lsid findById(java.lang.String id) {
        log.debug("getting Lsid instance with id: " + id);
        try {
            Lsid instance = (Lsid) HibernateUtil.getSession().get("org.genepattern.server.domain.Lsid", id);
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

    public List findByExample(Lsid instance) {
        log.debug("finding Lsid instance by example");
        try {
            List results = HibernateUtil.getSession().createCriteria("org.genepattern.server.domain.Lsid").add(
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
