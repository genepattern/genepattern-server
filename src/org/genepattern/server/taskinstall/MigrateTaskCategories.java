/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.taskinstall;

import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.DbException;
import org.genepattern.server.cm.CategoryUtil;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.PropsTable;
import org.genepattern.server.domain.TaskMaster;
import org.genepattern.server.taskinstall.dao.TaskInstall;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.hibernate.Query;

/**
 * Copy module categories from the TASK_MASTER.TASKINFOATTRIBUTES CLOB into the new 
 * TASK_INSTALL_CATEGORY and CATEGORY tables.
 * This is done as a one-time conversion when updating from GP <= 3.9.2 to GP >= 3.9.2.
 *  
 * @author pcarr
 *
 */
public class MigrateTaskCategories {
    private static Logger log = Logger.getLogger(MigrateTaskCategories.class);

    /**
     * This 'sync.task_install_category.complete' key in the PROPS table database indicates that the 
     * task_install_category table has already been initialized from previously installed modules. 
     */
    public static final String PROP_DB_CHECK="sync.task_install_category.complete";

    /**
     * Check the db to see if we need to run this migration.
     * @return true if the migration is complete
     */
    public boolean isComplete() {
        String val="";
        try {
            val=PropsTable.selectValue(PROP_DB_CHECK);
        }
        catch (DbException e) {
            val="";
        }
        Boolean isComplete=Boolean.valueOf(val);
        if (isComplete) {
            return true;
        }
        return false;
    }

    /**
     * Update the db to indicate that this migration is complete.
     * @return
     */
    protected boolean updateDb() {
        boolean success=PropsTable.saveProp(PROP_DB_CHECK, "true");
        return success;
    }

    public void copyCategoriesFromClobs() throws DbException  {
        if (isComplete()) {
            log.debug("task_install_category table already initialized");
            return;
        }
        walkTaskMasterTable();
        updateDb();
        log.debug("completed copy of categories from CLOB into task_install_category table");
    }
    
    protected void walkTaskMasterTable() throws DbException {
        HibernateSessionManager mgr=HibernateUtil.instance();
        boolean isInTransaction=mgr.isInTransaction();
        int i=0;
        int total=-1; // <--- not yet known
        try {
            if (!isInTransaction) {
                mgr.beginTransaction();
            }
            final List<String[]> records = (List<String[]>) getAllTaskMasterRecords(mgr);            
            if (records==null) {
                throw new DbException("Unexpected null value from allTaskMasterRecords");
            }
            total = records.size();
            for(final Object[] record : records) {
                String lsid = (String)record[0];
                String tia = (String)record[1];
                handleTask(mgr, lsid, tia);
                ++i;
                if (i % 20 == 0) {
                    mgr.getSession().flush();
                }
            }
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        catch (DbException e) {
            log.error("handled "+i+" of "+total+" records");
            throw e;
        }
        catch (Throwable t) {
            throw new DbException("Unexpected error converting module categories from CLOB", t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
        log.info("handled "+total+" records");
    }
    
    protected void handleTaskInfo(HibernateSessionManager mgr, final TaskInfo taskInfo) throws DbException {
        log.debug("processing tasklInfo ... "+taskInfo);
        List<String> categoryNames=CategoryUtil.getCategoriesFromManifest(taskInfo);
        TaskInstall.setCategories(mgr, taskInfo.getLsid(), categoryNames);
    }
    
    protected void handleTask(HibernateSessionManager mgr, final String lsid, final String tia) throws DbException {
        log.debug("processing lsid="+lsid);
        TaskInfoAttributes taskInfoAttributes=TaskInfoAttributes.decode(tia);
        List<String> categoryNames=CategoryUtil.getCategoriesFromManifest(taskInfoAttributes);
        TaskInstall.setCategories(mgr, lsid, categoryNames);
    }

    protected List<String[]> getAllTaskMasterRecords(HibernateSessionManager mgr) {
        Query query=mgr.getSession().createQuery("select lsid, taskinfoattributes from "+TaskMaster.class.getName());
        @SuppressWarnings("unchecked")
        List<String[]> results = (List<String[]>) query.list();
        return results;
    }

//Original implementation can consume more RAM
//import org.genepattern.webservice.TaskInfoCache;
//    /**
//     * Get all TaskInfo from the DB, this has the effect of bringing everything into RAM. 
//     * On GPPROD this is about 1600 entries, which is manageable.
//     * 
//     * @param mgr
//     * @return
//     */
//    protected List<TaskInfo> getAllTaskInfos(HibernateSessionManager mgr) {
//        final String hql = "select taskId from org.genepattern.server.domain.TaskMaster";
//        Query query = mgr.getSession().createQuery(hql);
//        List<Integer> taskIds = query.list();
//        return TaskInfoCache.instance().getTasksAsList(taskIds);
//    }

}
