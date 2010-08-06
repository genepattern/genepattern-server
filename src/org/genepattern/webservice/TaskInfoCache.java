package org.genepattern.webservice;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.TaskMaster;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;

/**
 * Use this cache to fetch a TaskInfo for a given lsid.
 * @author pcarr
 */
public class TaskInfoCache {
    static Logger log = Logger.getLogger(TaskInfoCache.class);
    public static TaskInfoCache instance() {
        return Singleton.taskInfoCache;
    }
    private static class Singleton {
        final static TaskInfoCache taskInfoCache = new TaskInfoCache();
        private Singleton() {
        }
    }
    private static TaskInfo taskInfoFromTaskMaster(TaskMaster tm, TaskInfoAttributes taskInfoAttributes) {
        return new TaskInfo(
                tm.getTaskId(), 
                tm.getTaskName(), 
                tm.getDescription(), 
                tm.getParameterInfo(),
                taskInfoAttributes, 
                tm.getUserId(), 
                tm.getAccessId());
    }

    private final ConcurrentMap<Integer, TaskMaster> taskMasterCache = new ConcurrentHashMap<Integer, TaskMaster>();
    private final ConcurrentMap<Integer, TaskInfoAttributes> taskInfoAttributesCache = new ConcurrentHashMap<Integer, TaskInfoAttributes>();
    
    public void initializeCache() {
        boolean closeDbSession = true;
        List<TaskMaster> allTaskMasters = findAll(closeDbSession);
        for(TaskMaster taskMaster : allTaskMasters) {
            Integer taskId = taskMaster.getTaskId();
            taskMasterCache.put(taskId, taskMaster);
            TaskInfoAttributes taskInfoAttributes = TaskInfoAttributes.decode(taskMaster.getTaskinfoattributes());
            taskInfoAttributesCache.put(taskId, taskInfoAttributes);
        }
    }
    
    //helper DAO methods
    private List<TaskMaster> findAll(boolean closeTransaction) throws ExceptionInInitializerError {
        StatelessSession session = null;
        try {
            SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
            if (sessionFactory == null) {
                throw new ExceptionInInitializerError("Hibernate session factory is not initialized");
            }
            session = sessionFactory.openStatelessSession();
            session.beginTransaction();
            
            String hql = "from org.genepattern.server.domain.TaskMaster";
            Query query = session.createQuery(hql);
            List<TaskMaster> taskMasters = query.list();
            return taskMasters;
        }
        finally {
            if (closeTransaction) {
                if (session != null) {
                    session.close();
                }
            }
        }
    }

    private List<Integer> findAllTaskIds() {
        String hql = "select taskId from org.genepattern.server.domain.TaskMaster";
        Session session = HibernateUtil.getSession();
        Query query = session.createQuery(hql);
        List<Integer> results = query.list();
        return results;
    }

    public TaskMaster findById(Integer taskId) {
        String hql = "from org.genepattern.server.domain.TaskMaster where taskId = :taskId";
        HibernateUtil.beginTransaction();
        Session session = HibernateUtil.getSession();
        Query query = session.createQuery(hql);
        query.setInteger("taskId", taskId);
        TaskMaster taskMaster = (TaskMaster) query.uniqueResult();
        return taskMaster;
    }
    
    public TaskInfo get(Integer taskId) {
        TaskMaster taskMaster = taskMasterCache.get(taskId);
        if (taskMaster == null) {
            //fetch from DB, then add to cache
            taskMaster = findById(taskId);
            if (taskMaster == null) {
                //TODO: error
                return null;
            }
            taskMasterCache.put(taskId, taskMaster);
        }
        TaskInfoAttributes taskInfoAttributes = taskInfoAttributesCache.get(taskId);
        if (taskInfoAttributes == null) {
            taskInfoAttributes = TaskInfoAttributes.decode(taskMaster.getTaskinfoattributes());
            taskInfoAttributesCache.put(taskId, taskInfoAttributes);
        }
        
        TaskInfo taskInfo = taskInfoFromTaskMaster(taskMaster, taskInfoAttributes);
        return taskInfo;
    }
    
    public TaskInfo[] getAllTasks() {
        List<Integer> allTaskIds = findAllTaskIds();
        List<TaskInfo> allTaskInfos = new ArrayList<TaskInfo>();
        for(Integer taskId : allTaskIds) {
            TaskInfo taskInfo = get(taskId);
            allTaskInfos.add(taskInfo);
        }
        TaskInfo[] taskInfoArray = allTaskInfos.toArray(new TaskInfo[allTaskInfos.size()]);
        return taskInfoArray;
    }

}
