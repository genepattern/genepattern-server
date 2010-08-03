package org.genepattern.webservice;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.genepattern.server.JobInfoManager.TaskInfoNotFoundException;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.TaskMaster;
import org.genepattern.server.domain.TaskMasterDAO;

/**
 * Use this cache to fetch a TaskInfo for a given taskId.
 * @author pcarr
 */
public class TaskInfoCache {
    private static Logger log = Logger.getLogger(TaskInfoCache.class);

    public static TaskInfoCache instance() {
        return Singleton.taskInfoCache;
    }
    private static class Singleton {
        final static TaskInfoCache taskInfoCache = new TaskInfoCache();
        private Singleton() {
        }
    }
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

//    private static class ComputeTaskInfoFromLsid implements Computable<String,TaskInfo> {
//        public TaskInfo compute(String lsid) throws InterruptedException {
//            TaskInfo taskInfo = null;
//            TaskMaster taskMaster = null;
//            try {
//                //TODO: @see findByLsid(lsid,user) which accounts for duplicate lsids
//                TaskMasterDAO dao = new TaskMasterDAO();
//                taskMaster = dao.findByLsid(lsid);
//            }
//            finally {
//                HibernateUtil.closeCurrentSession();
//            }
//            if (taskMaster != null) {
//                taskInfo = taskInfoFromTaskMaster(taskMaster);
//            }
//
//            return taskInfo;
//        }
//    }

    private final ConcurrentMap<Integer, TaskInfo> taskInfoCache = new ConcurrentHashMap<Integer, TaskInfo>();

    //TODO: this method could be further optimized:
    //    1) in many cases size of the taskIds list will be identical to the size of the cache
    //    2) additional DB queries could be batched
    public TaskInfo[] getAllTasks() {
        try {
            TaskMasterDAO dao = new TaskMasterDAO();
            List<Integer> taskIds = dao.findAllTaskIds();
            return getAllTasks(taskIds);
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }

    public TaskInfo[] getAllTasksForUser(String username) {
        try {
            TaskMasterDAO dao = new TaskMasterDAO();
            List<Integer> taskIds = dao.findAllTaskIdsForUser(username);
            return getAllTasks(taskIds);
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }
    
    public TaskInfo[] getAllTasks(List<Integer> taskIds) {
        if (taskIds == null) {
            log.error("Unexpected null arg, returning empty list");
            return new TaskInfo[0];
        }
        if (taskIds.size() == 0) {
            return new TaskInfo[0];
        }
        
        List<TaskInfo> allTasks = new ArrayList<TaskInfo>();
        for(Integer taskId : taskIds) {
            TaskInfo taskInfo = getTaskInfo(taskId);
            if (taskInfo != null) {
                allTasks.add(taskInfo);
            }
        }
        return allTasks.toArray(new TaskInfo[0]);
    }

    public TaskInfo getTaskInfo(int taskId) {
        return getTaskInfo(taskId, true);
    }

    public TaskInfo getTaskInfo(int taskId, boolean closeConnection) {
        TaskInfo taskInfo = taskInfoCache.get(taskId);
        if (taskInfo != null) {
            return taskInfo;
        }
        taskInfo = getTaskInfoFromDb(taskId, closeConnection);
        return taskInfo;
    }
    
    /**
     * Get a TaskInfo directly from the DB, rather than from the cache.
     * This is because the TaskInfo instance stores job specific info in the parameter_info field.
     * This is not a very good design, but it is here for legacy reasons.
     * 
     * @param taskId
     * @return
     * @throws TaskInfoNotFoundException
     */
    public TaskInfo getTaskInfoFromDb(int taskId) throws TaskInfoNotFoundException {
        TaskMasterDAO dao = new TaskMasterDAO();
        TaskMaster tm = dao.findById(taskId);
        if (tm == null) {
            throw new TaskInfoNotFoundException(taskId);
        }
        TaskInfo taskInfo = taskInfoFromTaskMaster(tm);
        return taskInfo;
    }
    
    /**
     * Must call this as part of any transaction which updates or deletes a task_info in the db,
     * to keep the cache from getting stale.
     * 
     * @param taskId
     * @return
     */
    public TaskInfo remove(int taskId) {
        return taskInfoCache.remove(taskId);
    }

    private TaskInfo getTaskInfoFromDb(Integer taskId, boolean closeConnection) {
        try {
            TaskMasterDAO dao = new TaskMasterDAO();
            TaskMaster tm = dao.findById(taskId);
            if (tm == null) {
                return null;
            }
            TaskInfo taskInfo = taskInfoFromTaskMaster(tm);
            taskInfoCache.put(taskId, taskInfo);
            return taskInfo;
        }
        finally {
            if (closeConnection) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    public void loadAllTasks() throws InterruptedException {
        try {
            TaskMasterDAO dao = new TaskMasterDAO();
            List<TaskMaster> taskMasters = dao.findAll();
            for(TaskMaster tm : taskMasters) {
                TaskInfo taskInfo = taskInfoFromTaskMaster(tm);
                taskInfoCache.put(taskInfo.getID(), taskInfo);
            }
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }

}
