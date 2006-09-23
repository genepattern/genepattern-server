package org.genepattern.server.domain;

// Generated Sep 21, 2006 12:36:06 PM by Hibernate Tools 3.1.0.beta5

import java.util.List;
import javax.naming.InitialContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Example;

/**
 * Home object for domain model class JobStatus.
 * 
 * @see org.genepattern.server.domain.JobStatus
 * @author Hibernate Tools
 */
public class JobStatusHome {

    private static final Logger log = Logger.getLogger(JobStatusHome.class);

    public JobStatus merge(JobStatus detachedInstance) {
        log.debug("merging JobStatus instance");
        try {
            JobStatus result = (JobStatus) HibernateUtil.getSession().merge(detachedInstance);
            log.debug("merge successful");
            return result;
        }
        catch (RuntimeException re) {
            log.error("merge failed", re);
            throw re;
        }
    }

    public JobStatus findById(java.lang.Integer id) {
        log.debug("getting JobStatus instance with id: " + id);
        try {
            JobStatus instance = (JobStatus) HibernateUtil.getSession().get("org.genepattern.server.domain.JobStatus",
                    id);
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

    public List findByExample(JobStatus instance) {
        log.debug("finding JobStatus instance by example");
        try {
            List results = HibernateUtil.getSession().createCriteria("org.genepattern.server.domain.JobStatus").add(
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
