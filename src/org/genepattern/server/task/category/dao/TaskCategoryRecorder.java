/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.task.category.dao;

import java.util.List;

import org.genepattern.server.database.HibernateSessionManager;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 * Helper class for saving and querying TaskCategory records.
 * @author pcarr
 *
 */
public class TaskCategoryRecorder {
    private final HibernateSessionManager mgr;

    public TaskCategoryRecorder(final HibernateSessionManager mgr) {
        this.mgr=mgr;
    }
    
    public void save(final String baseLsid, final String category) {
        final boolean inTransaction=mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            TaskCategory tk=new TaskCategory();
            tk.setTask(baseLsid);
            tk.setCategory("MIT_701X");
            mgr.getSession().saveOrUpdate(tk);
            if (!inTransaction) {
                mgr.commitTransaction();
            }
        }
        finally {
            if (!inTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    public List<TaskCategory> query(final String baseLsid) {
        boolean inTransaction=mgr.isInTransaction();
        try {
            String hql = "from "+TaskCategory.class.getName()+" tc where tc.task = :baseLsid";
            mgr.beginTransaction();
            Session session = mgr.getSession();
            Query query = session.createQuery(hql);
            query.setString("baseLsid", baseLsid);
            @SuppressWarnings("unchecked")
            List<TaskCategory> records = query.list();
            return records;
        }
        finally {
            if (!inTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    public List<TaskCategory> getAllCustomCategories() {
        boolean inTransaction=mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            Session session = mgr.getSession();
            @SuppressWarnings("unchecked")
            final List<TaskCategory> records = session.createCriteria(TaskCategory.class).list();
            return records;
        }
        finally {
            if (!inTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

}
