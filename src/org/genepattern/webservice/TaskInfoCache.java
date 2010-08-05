package org.genepattern.webservice;

import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.TaskMaster;
import org.genepattern.server.domain.TaskMasterDAO;
import org.genepattern.server.util.Computable;
import org.genepattern.server.util.Memoizer;

/**
 * Use this cache to fetch a TaskInfo for a given lsid.
 * @author pcarr
 */
public class TaskInfoCache {
    public static TaskInfoCache instance() {
        return Singleton.taskInfoCache;
    }
    private static class Singleton {
        final static TaskInfoCache taskInfoCache = new TaskInfoCache();
        private Singleton() {
        }
    }
    private static class ComputeTaskInfoFromLsid implements Computable<String,TaskInfo> {
        private static TaskInfo taskInfoFromTaskMaster(TaskMaster tm) {
            TaskInfoAttributes taskInfoAttributes = TaskInfoAttributes.decode(tm.getTaskinfoattributes());
            return new TaskInfo(
                    tm.getTaskId(), 
                    tm.getTaskName(), 
                    tm.getDescription(), 
                    tm.getParameterInfo(),
                    taskInfoAttributes, 
                    tm.getUserId(), 
                    tm.getAccessId());
        }
        
        public TaskInfo compute(String lsid) throws InterruptedException {
            TaskInfo taskInfo = null;
            TaskMaster taskMaster = null;
            try {
                //TODO: @see findByLsid(lsid,user) which accounts for duplicate lsids
                TaskMasterDAO dao = new TaskMasterDAO();
                taskMaster = dao.findByLsid(lsid);
            }
            finally {
                HibernateUtil.closeCurrentSession();
            }
            if (taskMaster != null) {
                taskInfo = taskInfoFromTaskMaster(taskMaster);
            }

            return taskInfo;
        }
    }

    private final Computable<String,TaskInfo> cache = new Memoizer<String,TaskInfo>(new ComputeTaskInfoFromLsid());

    public TaskInfo get(String lsid) {
        try {
            return cache.compute(lsid);
        }
        catch (InterruptedException e) {
            //TODO: handle exception
            return null;
        }
    }
}
