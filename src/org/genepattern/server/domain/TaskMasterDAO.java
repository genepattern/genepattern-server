/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.domain;

// Generated Sep 21, 2006 12:36:06 PM by Hibernate Tools 3.1.0.beta5

import java.util.List;

import org.apache.log4j.Logger;
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
    private static Logger log = Logger.getLogger(TaskMasterDAO.class);

    public TaskMaster findById(Integer id) {
        log.debug("getting TaskMaster instance with id: " + id);
        try {
            return (TaskMaster) HibernateUtil.getSession().get("org.genepattern.server.domain.TaskMaster", id);
        } catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }
    }
    
    public TaskMaster findByLsid(String lsid) {
        String hql = "from org.genepattern.server.domain.TaskMaster where lsid = :lsid";
        Query query = HibernateUtil.getSession().createQuery(hql);
        query.setString("lsid", lsid);
        List<TaskMaster> rval = query.list();
        if (rval == null) {
            log.error("Unexpected null returned from hibernate query");
            return null;
        }
        if (rval.size()==0) {
            log.warn("No match to lsid: "+lsid);
            return null;
        }
        if (rval.size()==1) {
            return rval.get(0);
        }
        
        log.error("Found "+rval.size()+" entries in the TASK_MASTER table with this lsid: "+lsid);
        return rval.get(0);
    }

    public TaskMaster findByIdLsid(String lsid, String user) {
        String hql = "from org.genepattern.server.domain.TaskMaster where lsid = :lsid";
        Query query = HibernateUtil.getSession().createQuery(hql);
        query.setString("lsid", lsid);
        List<TaskMaster> matches = query.list();
        
        if (matches.size() ==1) return matches.get(0);
        
        for (TaskMaster aTask: matches){
        	if (aTask.getUserId().equals(user)) return aTask;
        	
        }
        
        return (TaskMaster) null;
    }
    
    public boolean isTaskOwner(String user, String lsid) {
        String hql = "select userId, lsid from org.genepattern.server.domain.TaskMaster where lsid = :lsid and userId = :userId";
        Query query = HibernateUtil.getSession().createQuery(hql);
        query.setString("lsid", lsid);
        query.setString("userId", user);
        List<Object[]> results = query.list();
        return results != null && results.size() > 0;
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
