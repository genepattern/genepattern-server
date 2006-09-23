package org.genepattern.server.domain;

// Generated Sep 21, 2006 12:36:06 PM by Hibernate Tools 3.1.0.beta5

import java.util.List;
import javax.naming.InitialContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.genepattern.server.database.AbstractHome;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Example;

/**
 * Home object for domain model class Suite.
 * 
 * @see org.genepattern.server.domain.Suite
 * @author Hibernate Tools
 */
public class SuiteHome extends AbstractHome {

    private static final Logger log = Logger.getLogger(SuiteHome.class);

    public Suite merge(Suite detachedInstance) {
        log.debug("merging Props instance");
        try {
            return (Suite) HibernateUtil.getSession().merge(detachedInstance);
        }
        catch (RuntimeException re) {
            log.error("merge failed", re);
            throw re;
        }
    }

    public Suite findById(String id) {
        log.debug("getting Props instance with id: " + id);
        try {
            return (Suite) HibernateUtil.getSession().get("org.genepattern.server.domain.Suite", id);
        }
        catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }
    }

    public List<Props> findByExample(Suite instance) {
        log.debug("finding Suite instance by example");
        try {
            return HibernateUtil.getSession().createCriteria("org.genepattern.server.domain.Suite").add(
                    Example.create(instance)).list();
        }
        catch (RuntimeException re) {
            log.error("find by example failed", re);
            throw re;
        }
    }
}
