/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2013) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.task.category.dao;

import java.util.List;

import org.genepattern.server.database.HibernateUtil;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 * Helper class for saving and querying TaskCategory records.
 * @author pcarr
 *
 */
public class TaskCategoryRecorder {
    public void save(final String baseLsid, final String category) {
        final boolean inTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            TaskCategory tk=new TaskCategory();
            tk.setTask(baseLsid);
            tk.setCategory("MIT_701X");
            HibernateUtil.getSession().saveOrUpdate(tk);
            if (!inTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        finally {
            if (!inTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    public List<TaskCategory> query(final String baseLsid) {
        boolean inTransaction=HibernateUtil.isInTransaction();
        try {
            String hql = "from "+TaskCategory.class.getName()+" tc where tc.task = :baseLsid";
            HibernateUtil.beginTransaction();
            Session session = HibernateUtil.getSession();
            Query query = session.createQuery(hql);
            query.setString("baseLsid", baseLsid);
            List<TaskCategory> records = query.list();
            return records;
        }
        finally {
            if (!inTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

}
