package org.genepattern.webservice;

import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.TaskMaster;
import org.genepattern.server.domain.TaskMasterDAO;
import org.genepattern.server.util.Computable;
import org.genepattern.server.util.Memoizer;
import org.genepattern.server.webservice.server.dao.AdminDAO;

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

//    //helper class for precomputing the TaskInfo on startup
//    private static class ArgWrapper {
//        String lsid=null;
//        TaskInfo taskInfo=null;
//
//        //use this constructor when you want to fetch the TaskInfo from the DB
//        public ArgWrapper(String lsid) {
//            this.lsid = lsid;
//        }
//        //use this contructor when you already have a TaskInfo
//        public ArgWrapper(TaskInfo taskInfo) {
//            this.lsid = taskInfo.getLsid();
//            this.taskInfo = taskInfo;
//        }
//        
//        public boolean equals(Object o) {
//            if (lsid==null) {
//                return o == null;
//            }
//            return lsid.equals(o);
//        }
//        public int hashCode() {
//            if (lsid==null) {
//                return "".hashCode();
//            }
//            return lsid.hashCode();
//        }
//    }

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

    private final Memoizer<String,TaskInfo> cache = new Memoizer<String,TaskInfo>(new ComputeTaskInfoFromLsid());
    
    public void loadAllTasks() throws InterruptedException {
            AdminDAO dao = new AdminDAO();
            TaskInfo[] taskInfos = dao.getAllTasks();
            for(TaskInfo taskInfo : taskInfos) {
                cache.put(taskInfo.getLsid(), taskInfo);
                //cache.compute(new ArgWrapper(taskInfo));
            }
    }

    public TaskInfo get(String lsid) {
        try {
            return cache.compute(lsid);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            //TODO: handle exception
            return null;
        }
    }
}
