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
 * Home object for domain model class TaskMaster.
 * 
 * @see org.genepattern.server.domain.TaskMaster
 * @author Hibernate Tools
 */
public class TaskMasterHome extends AbstractHome {

    private static final Log log = LogFactory.getLog(TaskMasterHome.class);

    public TaskMaster merge(TaskMaster detachedInstance) {
        log.debug("merging Props instance");
        try {
            return (TaskMaster) HibernateUtil.getSession().merge(detachedInstance);
        }
        catch (RuntimeException re) {
            log.error("merge failed", re);
            throw re;
        }
    }

    public TaskMaster findById(Integer id) {
        log.debug("getting Props instance with id: " + id);
        try {
            return (TaskMaster) HibernateUtil.getSession().get("org.genepattern.server.domain.TaskMaster", id);
        }
        catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }
    }

    public List<TaskMaster> findByExample(TaskMaster instance) {
        log.debug("finding TaskMaster instance by example");
        try {
            return HibernateUtil.getSession().createCriteria("org.genepattern.server.domain.TaskMaster").add(
                    Example.create(instance)).list();
        }
        catch (RuntimeException re) {
            log.error("find by example failed", re);
            throw re;
        }
    }
    
    public List<TaskMaster> findAll() {
        try {
            return HibernateUtil.getSession().createQuery("from org.genepattern.server.domain.TaskMaster").list();
            
        }
        catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }
    }

}
