package org.genepattern.webservice;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.genepattern.server.TaskIDNotFoundException;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.TaskMaster;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.webservice.server.DirectoryManager;
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
    private final ConcurrentMap<Integer, List<String>> taskDocFilenameCache = new ConcurrentHashMap<Integer, List<String>>();
    
    public void initializeCache() {
        boolean closeDbSession = true;
        List<TaskMaster> allTaskMasters = findAll(closeDbSession);
        for(TaskMaster taskMaster : allTaskMasters) {
            Integer taskId = taskMaster.getTaskId();
            taskMasterCache.put(taskId, taskMaster);
            TaskInfoAttributes taskInfoAttributes = TaskInfoAttributes.decode(taskMaster.getTaskinfoattributes());
            taskInfoAttributesCache.put(taskId, taskInfoAttributes);
            List<String> docFilenames = listDocFilenames(taskMaster.getLsid());
            taskDocFilenameCache.putIfAbsent(taskId, docFilenames);
        }
    }
    
    private static class IsDocFilenameFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return GenePatternAnalysisTask.isDocFile(name) && !name.equals("version.txt");
        }
    }
    private static FilenameFilter isDocFilenameFilter = new IsDocFilenameFilter();
    private static class DocFilenameComparator implements Comparator { 
        public int compare(Object o1, Object o2) {
            if (((File) o1).getName().equals("version.txt")) {
                return 1;
            }
            return ((File) o1).getName().compareToIgnoreCase(((File) o2).getName());
        }
    }
    private static Comparator docFilenameComparator = new DocFilenameComparator();
    private List<String> listDocFilenames(String lsid) {
        List<String> docFilenames = new ArrayList<String>();
        File taskLibDir = null;
        try {
            String libDir = DirectoryManager.getLibDir(lsid);
            taskLibDir = new File(libDir);
        } 
        catch (Exception e) {
            log.error(e);
            return docFilenames;
        }
        File[] docFiles = taskLibDir.listFiles(isDocFilenameFilter);
        boolean hasDoc = docFiles != null && docFiles.length > 0;
        if (hasDoc) {
            // put version.txt last, all others alphabetically
            Arrays.sort(docFiles, docFilenameComparator);
        }
        if (docFiles == null) {
            return docFilenames;
        }
        for(File docFile : docFiles) {
            docFilenames.add(docFile.getName());
        }
        return docFilenames;
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
    
    public TaskInfo getTask(Integer taskId) throws TaskIDNotFoundException {
        TaskMaster taskMaster = taskMasterCache.get(taskId);
        if (taskMaster == null) {
            //fetch from DB, then add to cache
            taskMaster = findById(taskId);
            if (taskMaster == null) {
                throw new TaskIDNotFoundException(taskId);
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
        return getTasks(allTaskIds);
    }
    
    public TaskInfo[] getTasks(List<Integer> taskIds) {
        List<TaskInfo> allTaskInfos = new ArrayList<TaskInfo>();
        for(Integer taskId : taskIds) {
            try {
                TaskInfo taskInfo = getTask(taskId);
                allTaskInfos.add(taskInfo);
            }
            catch (TaskIDNotFoundException e) {
                log.error("Missing task info: ", e);
            }
        }
        TaskInfo[] taskInfoArray = allTaskInfos.toArray(new TaskInfo[allTaskInfos.size()]);
        return taskInfoArray;        
    }
    
    public List<String> getDocFilenames(Integer taskId, String lsid) {
        List<String> docFilenames = taskDocFilenameCache.get(taskId);
        if (docFilenames == null) {
            docFilenames = this.listDocFilenames(lsid);
            taskDocFilenameCache.putIfAbsent(taskId, docFilenames);
        }
        return docFilenames;
    }

    public void deleteTask(Integer taskId) {
        taskMasterCache.remove(taskId);
        taskInfoAttributesCache.remove(taskId);
        taskDocFilenameCache.remove(taskId);
    }
}
