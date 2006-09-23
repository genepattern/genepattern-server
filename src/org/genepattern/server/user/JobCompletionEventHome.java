package org.genepattern.server.user;

// Generated Sep 21, 2006 12:36:06 PM by Hibernate Tools 3.1.0.beta5

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.genepattern.server.database.AbstractHome;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.criterion.Example;

 /**
 * Home object for domain model class JobCompletionEvent.
 * @org.genepattern.server.user.JobCompletionEvent
 * @author Hibernate Tools
 */
public class JobCompletionEventHome extends AbstractHome {

    private static final Logger log = Logger.getLogger(JobCompletionEventHome.class);

    public JobCompletionEvent merge(JobCompletionEvent detachedInstance) {
        log.debug("merging Props instance");
        try {
            return (JobCompletionEvent) HibernateUtil.getSession().merge(detachedInstance);
        }
        catch (RuntimeException re) {
            log.error("merge failed", re);
            throw re;
        }
    }

    public JobCompletionEvent findById(Integer id) {
        log.debug("getting Props instance with id: " + id);
        try {
            return (JobCompletionEvent) HibernateUtil.getSession().get("org.genepattern.server.user.JobCompletionEvent", id);
        }
        catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }
    }

    public List<JobCompletionEvent> findByExample(JobCompletionEvent instance) {
        log.debug("finding JobCompletionEvent instance by example");
        try {
            return HibernateUtil.getSession().createCriteria("org.genepattern.server.user.JobCompletionEvent").add(
                    Example.create(instance)).list();
        }
        catch (RuntimeException re) {
            log.error("find by example failed", re);
            throw re;
        }
    }
 }