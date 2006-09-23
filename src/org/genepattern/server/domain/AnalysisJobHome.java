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
 * Home object for domain model class AnalysisJob.
 * 
 * @see org.genepattern.server.domain.AnalysisJob
 * @author Hibernate Tools
 */
public class AnalysisJobHome extends AbstractHome {

    private static final Logger log = Logger.getLogger(AnalysisJobHome.class);

    public AnalysisJob merge(AnalysisJob detachedInstance) {
        log.debug("merging AnalysisJob instance");
        try {
            AnalysisJob result = (AnalysisJob) HibernateUtil.getSession().merge(detachedInstance);
            log.debug("merge successful");
            return result;
        }
        catch (RuntimeException re) {
            log.error("merge failed", re);
            throw re;
        }
    }

    public AnalysisJob findById(java.lang.Integer id) {
        log.debug("getting AnalysisJob instance with id: " + id);
        try {
            AnalysisJob instance = (AnalysisJob) HibernateUtil.getSession().get(
                    "org.genepattern.server.domain.AnalysisJob", id);
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

    public List findByExample(AnalysisJob instance) {
        log.debug("finding AnalysisJob instance by example");
        try {
            List results = HibernateUtil.getSession().createCriteria("org.genepattern.server.domain.AnalysisJob").add(
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
