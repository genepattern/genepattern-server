package org.genepattern.server.database;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.Lsid;
import org.hibernate.LockMode;

public abstract class AbstractHome {
    
    Logger log = Logger.getLogger(AbstractHome.class);
    
    public void persist(Object transientInstance) {
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

    public void attachDirty(Object instance) {
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

    public void attachClean(Object instance) {
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

    public void delete(Object persistentInstance) {
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

}
