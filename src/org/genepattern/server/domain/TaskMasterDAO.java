package org.genepattern.server.domain;

// Generated Sep 21, 2006 12:36:06 PM by Hibernate Tools 3.1.0.beta5

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.Query;

/**
 * Home object for domain model class TaskMaster.
 * 
 * @see org.genepattern.server.domain.TaskMaster
 * @author Hibernate Tools
 */
public class TaskMasterDAO extends BaseDAO {

    private static final Log log = LogFactory.getLog(TaskMasterDAO.class);

    public TaskMaster findById(Integer id) {
        log.debug("getting TaskMaster instance with id: " + id);
        try {
            return (TaskMaster) HibernateUtil.getSession().get("org.genepattern.server.domain.TaskMaster", id);
        } catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }
    }

    public TaskMaster findByIdLsid(String lsid) {
        String hql = "from org.genepattern.server.domain.TaskMaster where lsid = :lsid";
        Query query = HibernateUtil.getSession().createQuery(hql);
        query.setString("lsid", lsid);
        return (TaskMaster) query.uniqueResult();
    }

    public List<TaskMaster> findAll() {
        try {
            return HibernateUtil.getSession().createQuery("from org.genepattern.server.domain.TaskMaster").list();

        } catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }
    }

}
