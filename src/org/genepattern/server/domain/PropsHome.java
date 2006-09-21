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
 * Home object for domain model class Props.
 * 
 * @see org.genepattern.server.domain.Props
 * @author Hibernate Tools
 */
public class PropsHome {

    private static final Log log = LogFactory.getLog(PropsHome.class);

    public void persist(Props transientInstance) {
        log.debug("persisting Props instance");
        try {
            HibernateUtil.getSession().persist(transientInstance);
            log.debug("persist successful");
        }
        catch (RuntimeException re) {
            log.error("persist failed", re);
            throw re;
        }
    }

    public void attachDirty(Props instance) {
        log.debug("attaching dirty Props instance");
        try {
            HibernateUtil.getSession().saveOrUpdate(instance);
            log.debug("attach successful");
        }
        catch (RuntimeException re) {
            log.error("attach failed", re);
            throw re;
        }
    }

    public void attachClean(Props instance) {
        log.debug("attaching clean Props instance");
        try {
            HibernateUtil.getSession().lock(instance, LockMode.NONE);
            log.debug("attach successful");
        }
        catch (RuntimeException re) {
            log.error("attach failed", re);
            throw re;
        }
    }

    public void delete(Props persistentInstance) {
        log.debug("deleting Props instance");
        try {
            HibernateUtil.getSession().delete(persistentInstance);
            log.debug("delete successful");
        }
        catch (RuntimeException re) {
            log.error("delete failed", re);
            throw re;
        }
    }

    public Props merge(Props detachedInstance) {
        log.debug("merging Props instance");
        try {
            Props result = (Props) HibernateUtil.getSession().merge(detachedInstance);
            log.debug("merge successful");
            return result;
        }
        catch (RuntimeException re) {
            log.error("merge failed", re);
            throw re;
        }
    }

    public Props findById(java.lang.String id) {
        log.debug("getting Props instance with id: " + id);
        try {
            Props instance = (Props) HibernateUtil.getSession().get("org.genepattern.server.domain.Props", id);
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

    public List findByExample(Props instance) {
        log.debug("finding Props instance by example");
        try {
            List results = HibernateUtil.getSession().createCriteria("org.genepattern.server.domain.Props").add(
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
