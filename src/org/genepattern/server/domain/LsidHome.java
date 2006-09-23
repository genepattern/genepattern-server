package org.genepattern.server.domain;

// Generated Sep 21, 2006 12:36:06 PM by Hibernate Tools 3.1.0.beta5

import java.util.List;
import javax.naming.InitialContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.genepattern.server.database.AbstractHome;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Example;

/**
 * Home object for domain model class Lsid.
 * 
 * @see org.genepattern.server.domain.Lsid
 * @author Hibernate Tools
 */
public class LsidHome extends AbstractHome {

    public static final Log log = LogFactory.getLog(LsidHome.class);

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
