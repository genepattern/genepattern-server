package org.genepattern.server.domain;

// Generated Sep 21, 2006 12:36:06 PM by Hibernate Tools 3.1.0.beta5

import java.util.List;
import javax.naming.InitialContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.genepattern.server.database.AbstractHome;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Example;

/**
 * Home object for domain model class Props.
 * 
 * @see org.genepattern.server.domain.Props
 * @author Hibernate Tools
 */
public class PropsHome extends AbstractHome {

    private static final Log log = LogFactory.getLog(PropsHome.class);

    public Props merge(Props detachedInstance) {
        log.debug("merging Props instance");
        try {
            return (Props) HibernateUtil.getSession().merge(detachedInstance);
        }
        catch (RuntimeException re) {
            log.error("merge failed", re);
            throw re;
        }
    }

    public Props findById(java.lang.String id) {
        log.debug("getting Props instance with id: " + id);
        try {
            return (Props) HibernateUtil.getSession().get("org.genepattern.server.domain.Props", id);
        }
        catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }
    }

    public List<Props> findByExample(Props instance) {
        log.debug("finding Props instance by example");
        try {
            return HibernateUtil.getSession().createCriteria("org.genepattern.server.domain.Props").add(
                    Example.create(instance)).list();
        }
        catch (RuntimeException re) {
            log.error("find by example failed", re);
            throw re;
        }
    }
}
