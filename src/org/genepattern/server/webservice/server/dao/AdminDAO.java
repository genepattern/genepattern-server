/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webservice.server.dao;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.genepattern.server.TaskIDNotFoundException;
import org.genepattern.server.TaskLSIDNotFoundException;
import org.genepattern.server.cm.CategoryUtil;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.domain.Suite;
import org.genepattern.server.domain.TaskMaster;
import org.genepattern.server.genepattern.LSIDManager;
import org.genepattern.server.util.AuthorizationManagerFactory;
import org.genepattern.server.util.IAuthorizationManager;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoCache;
import org.genepattern.webservice.WebServiceException;
import org.hibernate.Query;

/**
 * @author Joshua Gould
 * @modified "Hibernated" by Jim Robinson
 */
public class AdminDAO extends BaseDAO {
    private static final Logger log = Logger.getLogger(AdminDAO.class);

    private static int PUBLIC_ACCESS_ID = 1;

    /** @deprecated */
    public AdminDAO() {
    }
    
    public AdminDAO(final HibernateSessionManager mgr) {
        super(mgr);
    }

    /**
     * Returns the versions of the tasks with the same versionless LSID as the given LSID. 
     * The returned list is in ascending order.
     * 
     * @param lsid, the LSID.
     * @param username, the username.
     * 
     * @return The versions.
     */
    public List<String> getVersions(LSID lsid, String username) {
        String hql = "from org.genepattern.server.domain.TaskMaster where lsid like :lsid" + " and (userId = :userId or accessId = :accessId)";
        Query query = getSession().createQuery(hql);
        query.setString("lsid", lsid.toStringNoVersion() + "%");
        query.setString("userId", username);
        query.setInteger("accessId", GPConstants.ACCESS_PUBLIC);
        @SuppressWarnings("unchecked")
        List<TaskMaster> results = query.list();
        List<String> versions = new ArrayList<String>();
        for (TaskMaster tm : results) {
            try {
                versions.add(new LSID(tm.getLsid()).getVersion());
            } 
            catch (MalformedURLException e) {
                log.error(e);
            }
        }
        Collections.sort(versions, new Comparator<String>() {
            public int compare(String l1, String l2) {
                try {
                    Integer version1 = Integer.parseInt(l1);
                    Integer version2 = Integer.parseInt(l2);
                    return version1.compareTo(version2);
                } 
                catch (NumberFormatException nfe) {
                    return 0;
                }
            }
        });
        return versions;
    }

    // FIXME see doc for AdminDAO.getTaskId
    public TaskInfo getTask(String lsidOrTaskName, String username) throws OmnigeneException {
        if (lsidOrTaskName == null || lsidOrTaskName.trim().equals("")) {
            return null;
        }
        log.debug("getTask ... \n\tlsidOrTaskName="+lsidOrTaskName+"\n\tusername="+username);

        boolean startTransaction = false;
        try {
            if (!mgr.getSession().getTransaction().isActive()) {
                mgr.beginTransaction();
            }

            LSID lsid = new LSID(lsidOrTaskName);
            String version = lsid.getVersion();

            Query query = null;
            if (version != null && !version.equals("")) {
                if (username != null) {
                    IAuthorizationManager authManager = AuthorizationManagerFactory.getAuthorizationManager();
                    boolean isAdmin = (authManager.checkPermission("adminServer", username) || authManager.checkPermission("adminModules", username));
                    String hql = "select taskId from org.genepattern.server.domain.TaskMaster where lsid = :lsid";
                    if (!isAdmin) {
                        hql += " and (userId = :userId or accessId = :accessId)";
                    }
                    query = getSession().createQuery(hql);
                    query.setString("lsid", lsidOrTaskName);
                    if (!isAdmin){
                        query.setString("userId", username);
                        query.setInteger("accessId", GPConstants.ACCESS_PUBLIC);
                    }
                } 
                else {
                    // sql = "SELECT * FROM task_Master WHERE lsid='" +
                    // lsidOrTaskName + "'";
                    String hql = "select taskId from org.genepattern.server.domain.TaskMaster where lsid = :lsid";
                    query = getSession().createQuery(hql);
                    query.setString("lsid", lsidOrTaskName);
                }
            } 
            else { // lsid with no version
                if (username != null) {
                    // sql = "SELECT * FROM task_master WHERE LSID LIKE '" +
                    // lsidOrTaskName + "%' AND (user_id='"
                    // + username + "' OR access_id=" +
                    // GPConstants.ACCESS_PUBLIC + ")";
                    String hql = "select taskId from org.genepattern.server.domain.TaskMaster where lsid like :lsid"
                        + " and (userId = :userId or accessId = :accessId)";
                    query = getSession().createQuery(hql);
                    query.setString("lsid", lsidOrTaskName + "%");
                    query.setString("userId", username);
                    query.setInteger("accessId", GPConstants.ACCESS_PUBLIC);
                } 
                else {
                    // sql = "SELECT * FROM task_master WHERE LSID LIKE '" +
                    // lsidOrTaskName + "%'";
                    String hql = "select taskId from org.genepattern.server.domain.TaskMaster where lsid like :lsid";
                    query = getSession().createQuery(hql);
                    query.setString("lsid", lsidOrTaskName + "%");
                }
            }
            @SuppressWarnings("unchecked")
            List<Integer> taskIds = query.list();
            if (log.isDebugEnabled()) {
                for(int taskId : taskIds) {
                    log.debug("\tfetched taskId="+taskId);
                }
            }
            TaskInfo[] results = TaskInfoCache.instance().getTasks(taskIds);
            TaskInfo latestTask = null;
            LSID latestLSID = null;
            
            if (results != null && results.length > 0) {
                latestTask = results[0];
                latestLSID = new LSID((String) latestTask.getTaskInfoAttributes().get(GPConstants.LSID));
            }
            for(int i=1; i<results.length; ++i) {
                TaskInfo ti = results[i];
                LSID l = new LSID(ti.getLsid());
                if (l.compareTo(latestLSID) < 0) {
                    latestTask = ti;
                    latestLSID = l;
                }
            }
            if (latestTask == null) {
                log.debug("latestTask == null");
            }
            else {
                log.debug("latestTask.taskId="+latestTask.getID());
            }
            return latestTask;
        } 
        catch (java.net.MalformedURLException e) {
            // no lsid specified, find the 'best' match
            Query query = null;
            if (username != null) {
                // sql = "SELECT * FROM task_master WHERE task_name='" +
                // lsidOrTaskName + "' AND (user_id='" + username
                // + "' OR access_id=" + GPConstants.ACCESS_PUBLIC + ")";
                String hql = "select taskId from org.genepattern.server.domain.TaskMaster where taskName = :taskName "
                    + " and (userId = :userId or accessId = :accessId)";
                query = getSession().createQuery(hql);
                query.setString("taskName", lsidOrTaskName);
                query.setString("userId", username);
                query.setInteger("accessId", GPConstants.ACCESS_PUBLIC);
            } 
            else {
                // sql = "SELECT * FROM task_master WHERE task_name='" +
                // lsidOrTaskName + "'";
                String hql = "select taskId from org.genepattern.server.domain.TaskMaster where lsid = :taskName";
                query = getSession().createQuery(hql);
                query.setString("taskName", lsidOrTaskName);
            }

            @SuppressWarnings("unchecked")
            List<Integer> taskIds = query.list();
            if (log.isDebugEnabled()) {
                for(int taskId : taskIds) {
                    log.debug("fetched taskId="+taskId);
                }
            }
            List<TaskInfo> tasksWithGivenName = new ArrayList<TaskInfo>();
            for (Integer taskId : taskIds) {
                try {
                    TaskInfo taskInfo = TaskInfoCache.instance().getTask(taskId);
                    tasksWithGivenName.add(taskInfo);
                }
                catch (TaskIDNotFoundException ex) {
                    log.error("", ex);
                }
            }
            Collection<TaskInfo> latestTasks = null;
            try {
                latestTasks = getLatestTasks((TaskInfo[]) tasksWithGivenName.toArray(new TaskInfo[0])).values();
            } 
            catch (MalformedURLException e1) {
                throw new OmnigeneException(e1);
            }

            TaskInfo latestTask = null;
            LSID closestLSID = null;
            for (Iterator<TaskInfo> it = latestTasks.iterator(); it.hasNext();) {
                TaskInfo t =  it.next();
                try {
                    LSID lsid = new LSID((String) t.getTaskInfoAttributes().get(GPConstants.LSID));
                    if (closestLSID == null) {
                        closestLSID = lsid;
                    } 
                    else {
                        closestLSID = LSIDManager.getInstance().getNearerLSID(closestLSID, lsid);
                    }
                    if (closestLSID == lsid) {
                        latestTask = t;
                    }
                } 
                catch (java.net.MalformedURLException mfe) {
                    // shouldn't happen
                }
            }

            if (startTransaction) {
                mgr.commitTransaction();
            }
            if (latestTask == null) {
                log.debug("latestTask == null");
            }
            else {
                log.debug("latestTask.taskId="+latestTask.getID());
            }
            return latestTask;
        }
        catch (RuntimeException e) {
            if (startTransaction) {
                mgr.getSession().getTransaction().rollback();
            }
            throw e;
        }
    }

    public TaskInfo[] getAllTasks() {
        return TaskInfoCache.instance().getAllTasks();
    }

    public TaskInfo[] getTasksOwnedBy(String username) {
        String hql = "select taskId from org.genepattern.server.domain.TaskMaster where userId = :userId";
        Query query = getSession().createQuery(hql);
        query.setString("userId", username);
        @SuppressWarnings("unchecked")
        List<Integer> taskIds = query.list();
        return TaskInfoCache.instance().getTasks(taskIds);
    }

    public TaskInfo[] getAllTasksForUser(String username) {
        String hql = "select taskId from org.genepattern.server.domain.TaskMaster where userId = :userId or accessId = :accessId order by TASK_NAME asc";
        Query query = getSession().createQuery(hql);
        query.setString("userId", username);
        query.setInteger("accessId", PUBLIC_ACCESS_ID);
        @SuppressWarnings("unchecked")
        List<Integer> taskIds = query.list();
        return TaskInfoCache.instance().getTasks(taskIds);
    }

    /**
     * @param username
     * @param lsids
     * @return
     */
    public Map<String, TaskInfo> getAllTasksForUserWithLsids(String username, List<String> lsids) {
        String hql = "select taskId from org.genepattern.server.domain.TaskMaster where (userId = :userId or accessId = :accessId) and lsid like :lsids";
        Query query = getSession().createQuery(hql);
        query.setString("userId", username);
        query.setInteger("accessId", PUBLIC_ACCESS_ID);

        Map<String, TaskInfo> queriedTasks = new HashMap<String, TaskInfo>();
        for (String lsid : lsids) {
            query.setString("lsids", lsid + "%");
            @SuppressWarnings("unchecked")
            List<Integer> taskIds = query.list();
            for(Integer taskId : taskIds) {
                try {
                    queriedTasks.put(lsid, TaskInfoCache.instance().getTask(taskId));
                }
                catch (TaskIDNotFoundException e) {
                    log.error("Unexpected error getting TaskInfo from cache", e);
                }
            }
        }
        return queriedTasks;
    }

    /**
     * 
     * @param username
     * @param maxResults
     * @return
     */
    public TaskInfo[] getRecentlyRunTasksForUser(String username, int maxResults) {
        String jobHQL = "select aJob.taskLsid from org.genepattern.server.domain.AnalysisJob aJob "
            + " where userId = :userId and ((parent = null) OR (parent = -1)) order by aJob.jobNo desc";
        Query query = getSession().createQuery(jobHQL);
        query.setMaxResults(20);
        query.setString("userId", username);
        @SuppressWarnings("unchecked")
        List<String> recentLsids = query.list();

        if (recentLsids.isEmpty()) {
            return new TaskInfo[0];
        }
        StringBuffer hql = new StringBuffer();

        hql.append("select taskId from org.genepattern.server.domain.TaskMaster where lsid in (");
        Set<String> versionlessLsids = new HashSet<String>();

        for (int i = 0, size = recentLsids.size(); i < size; i++) {
            String lsid = recentLsids.get(i);
            String versionlessLsid;
            try {
                versionlessLsid = new LSID(lsid).toStringNoVersion();
                if (!versionlessLsids.contains(versionlessLsid)) {
                    versionlessLsids.add(versionlessLsid);
                    if (i > 0) {
                        hql.append(", ");
                    }
                    hql.append("'" + lsid + "'");
                }
            } 
            catch (MalformedURLException e) {
                log.error("Error", e);
            }
            if (versionlessLsids.size() == maxResults) {
                break;
            }
        }
        hql.append(")");

        query = getSession().createQuery(hql.toString());
        @SuppressWarnings("unchecked")
        List<Integer> taskIds = query.list();
        return TaskInfoCache.instance().getTasks(taskIds);
    }

    /**
     * Returns a map containing the latest version of each task in the input array. The keys for the map are the
     * no-version LSIDs of the tasks.
     * 
     * @param tasks
     * @return
     * @throws MalformedURLException
     */
    public static Map<String, TaskInfo> getLatestTasks(TaskInfo[] tasks) throws MalformedURLException {
        Map<String, TaskInfo> latestTasks = new HashMap<String, TaskInfo>();
        for (int i = 0; i < tasks.length; i++) {
            TaskInfo ti = tasks[i];
            LSID tiLSID = new LSID((String) ti.getTaskInfoAttributes().get(GPConstants.LSID));
            TaskInfo altTi = (TaskInfo) latestTasks.get(tiLSID.toStringNoVersion());
            if (altTi == null) {
                latestTasks.put(tiLSID.toStringNoVersion(), ti);
            } 
            else {
                LSID altLSID = new LSID((String) altTi.getTaskInfoAttributes().get(GPConstants.LSID));
                if (altLSID.compareTo(tiLSID) > 0) {
                    latestTasks.put(tiLSID.toStringNoVersion(), ti); // it
                }
            }
        }
        return latestTasks;
    }

    /**
     * Returns a map containing the latest version of each task in the input array which does not have the specified category. The keys for the map are the
     * no-version LSIDs of the tasks.
     *
     * @param tasks
     * @return
     * @throws MalformedURLException
     */
    public static Map<String, TaskInfo> getLatestTasks(TaskInfo[] tasks, List<String> excludedQualityLevels) throws MalformedURLException {
        Map<String, TaskInfo> latestTasks = new HashMap<String, TaskInfo>();
        for (int i = 0; i < tasks.length; i++) {
            TaskInfo ti = tasks[i];
            LSID tiLSID = new LSID((String) ti.getTaskInfoAttributes().get(GPConstants.LSID));
            TaskInfo altTi = (TaskInfo) latestTasks.get(tiLSID.toStringNoVersion());

            String taskQuality = ti.getTaskInfoAttributes().get(GPConstants.QUALITY);
            boolean skip = false;
            if (altTi == null) {
                latestTasks.put(tiLSID.toStringNoVersion(), ti);
            }
            else {
                if(excludedQualityLevels != null && excludedQualityLevels.size() > 0)
                {
                    for(String excludedQualityLevel: excludedQualityLevels)
                    {
                        //include this task if all versions include the excluded quality level
                        if (excludedQualityLevel.equalsIgnoreCase(taskQuality)
                                && !altTi.getTaskInfoAttributes().get(GPConstants.QUALITY).equalsIgnoreCase(taskQuality))
                        {
                            skip = true;
                        }
                    }
                }
                if(!skip)
                {
                    LSID altLSID = new LSID((String) altTi.getTaskInfoAttributes().get(GPConstants.LSID));
                    if (altLSID.compareTo(tiLSID) > 0) {
                        latestTasks.put(tiLSID.toStringNoVersion(), ti); // it
                    }
                }
            }
        }
        return latestTasks;
    }

    /**
     * Returns an array of the latest tasks for the specified user. The returned array will not contain tasks with the
     * same name. If more than one task with the same name exists on the server, the returned array will contain the one
     * task with the name that is closest to the server LSID authority. The closest authority is the first match in the
     * sequence: local server authority, Broad authority, other authority.
     * 
     * @param username
     *                The username to get the tasks for.
     * @return The array of tasks.
     * @throws AdminDAOSysException
     */
    public TaskInfo[] getLatestTasksByName(String username) throws AdminDAOSysException {
        TaskInfo[] tasks = getLatestTasks(username);
        Map<String, TaskInfo> map = new LinkedHashMap<String, TaskInfo>();
        for (int i = 0; i < tasks.length; i++) {
            TaskInfo t = (TaskInfo) map.get(tasks[i].getName());
            if (t != null) {
                try {
                    LSID existingLsid = new LSID((String) t.getTaskInfoAttributes().get(GPConstants.LSID));
                    LSID currentLSID = new LSID((String) tasks[i].getTaskInfoAttributes().get(GPConstants.LSID));
                    LSID closer = LSIDManager.getInstance().getNearerLSID(existingLsid, currentLSID);
                    if (closer == currentLSID) {
                        map.put(tasks[i].getName(), tasks[i]);
                    }
                } 
                catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            } 
            else {
                map.put(tasks[i].getName(), tasks[i]);
            }
        }
        return (TaskInfo[]) map.values().toArray(new TaskInfo[0]);
    }

    public TaskInfo[] getLatestTasks(String username) {
        TaskInfo[] tasks = getAllTasksForUser(username);
        if (tasks == null) {
            return new TaskInfo[0];
        }
        try {
            final Map<String, TaskInfo> lsidToTask = getLatestTasks(tasks);
            TaskInfo[] tasksArray = (TaskInfo[]) lsidToTask.values().toArray(new TaskInfo[0]);
            Arrays.sort(tasksArray, new TaskNameComparator());
            return tasksArray;
        } 
        catch (MalformedURLException mfe) {
            log.error(mfe);
            throw new OmnigeneException("Error fetching task:  Malformed URL: " + mfe.getMessage());
        }
    }
    /**
     * Get the taskInfo for the given taskId.
     * @param taskId
     * @return
     * @throws TaskIDNotFoundException
     */
    public TaskInfo getTask(int taskId) throws TaskIDNotFoundException {
        return TaskInfoCache.instance().getTask(taskId);
    }

    /**
     * Get the taskInfo with the given lsid.
     * @param lsid
     * @return
     * @throws TaskLSIDNotFoundException
     */
    public TaskInfo getTask(String lsid) throws TaskLSIDNotFoundException {
        return TaskInfoCache.instance().getTask(lsid);
    }

    /**
     * To remove registered task based on task ID
     * 
     * @param taskID
     * @throws OmnigeneException
     * @throws RemoteException
     * @return No. of updated records
     */
    public int deleteTask(int taskID) throws OmnigeneException {
        try {
            String hql = "delete from org.genepattern.server.domain.TaskMaster  where taskId = :taskId ";
            Query query = getSession().createQuery(hql);
            query.setInteger("taskId", taskID);

            getSession().flush();
            getSession().clear();
            int updatedRecord = query.executeUpdate();

            // If no record updated
            if (updatedRecord == 0) {
                log.error("deleteTask Could not delete task, taskID not found");
                throw new TaskIDNotFoundException(taskID);
            }
            
            //remove the entry from the cache
            TaskInfoCache.instance().deleteTask(taskID);
            return updatedRecord;
        } 
        catch (Exception e) {
            log.error("deleteTask failed " + e);
            throw new OmnigeneException(e.getMessage());
        }
    }

    public SuiteInfo getSuite(String lsid) throws OmnigeneException {
        String hql = "from org.genepattern.server.domain.Suite where lsid = :lsid ";
        Query query = getSession().createQuery(hql);
        query.setString("lsid", lsid);
        Suite result = (Suite) query.uniqueResult();
        if (result != null) {
            return this.suiteInfoFromSuite(result);
        } 
        else {
            log.error("suite id " + lsid + " not found");
            throw new OmnigeneException("suite id " + lsid + " not found");
        }
    }

    /**
     * Gets the latest versions of all suites
     * 
     * @return The latest suites
     * @exception WebServiceException
     *                    If an error occurs
     */
    public SuiteInfo[] getLatestSuites() throws AdminDAOSysException {
        ArrayList<SuiteInfo> latestSuites = _getLatestSuites();
        SuiteInfo[] latest = new SuiteInfo[latestSuites.size()];
        latest=latestSuites.toArray(latest);
        return latest;
    }

    public SuiteInfo[] getLatestSuitesForUser(final String userName) throws AdminDAOSysException {
        final SuiteInfo[] all=getAllSuites(userName);
        ArrayList<SuiteInfo> suites = _getLatestSuitesFromList(all);
        SuiteInfo[] arr=new SuiteInfo[ suites.size() ];
        return suites.toArray(arr);
    }

    private LSID getLsid(final SuiteInfo si) {
        if (si==null) {
            log.error("Unexpected null arg");
            return null;
        }
        try {
            final LSID siLsid = new LSID(si.getLsid());
            return siLsid;
        }
        catch (MalformedURLException e) {
            // it is possible for the lsid for an installed suite to be invalid
            log.debug(e);
        }
        catch (Throwable t) {
            log.error("Unexpected error for si.getLSID="+si.getLSID(), t);
        }
        return null;
    }
    
    private ArrayList<SuiteInfo> _getLatestSuites() throws AdminDAOSysException {
        try {
            final SuiteInfo[] allSuites = getAllSuites();
            return _getLatestSuitesFromList(allSuites);
        } 
        catch (Exception mfe) {
            throw new AdminDAOSysException("A database error occurred", mfe);
        }
    }
    
    private ArrayList<SuiteInfo> _getLatestSuitesFromList(final SuiteInfo[] allSuites) {
        final TreeMap<String,SuiteInfo> latestSuites = new TreeMap<String, SuiteInfo>();
        // loop through them placing them into a tree set based on their LSIDs
        for (int i = 0; i < allSuites.length; i++) {
            final SuiteInfo si = allSuites[i];
            final LSID siLsid=getLsid(si);
            if (siLsid != null) {
                final String baseLsid=siLsid.toStringNoVersion();
                final SuiteInfo altSi = (SuiteInfo) latestSuites.get(baseLsid);
                if (altSi == null) {
                    latestSuites.put(baseLsid, si);
                } 
                else {
                    final LSID altLsid = getLsid(altSi);
                    if (altLsid != null) {
                        if (altLsid.compareTo(siLsid) > 0) {
                            latestSuites.put(siLsid.toStringNoVersion(), si); // it is newer
                        }
                    }
                    // else it is older so leave it out
                }
            }
            else {
                //unexpected, but we can tolerate this error
                // if we are here is means that si.getLsid is not a properly formatted LSID
                log.debug("SuiteInfo.lsid is not a propery formatted LSID, lsid="+si.getLsid());
                latestSuites.put(si.getLsid(), si);
            }
        }

        final ArrayList<SuiteInfo> latest = new ArrayList<SuiteInfo>();
        for(final SuiteInfo suiteInfo : latestSuites.values()) {
            latest.add(suiteInfo);
        }
        return latest;
    }

    /**
     * Gets all versions of all suites
     * 
     * @return The suites
     * @exception WebServiceException
     *                    If an error occurs
     */
    public SuiteInfo[] getAllSuites() throws AdminDAOSysException {
        List<Suite> results = queryAllSuites();
        return suiteInfoFromSuites(results);
    }
    
    private SuiteInfo[] suiteInfoFromSuites(final List<Suite> suites) {
        SuiteInfo[] suiteInfos = new SuiteInfo[suites.size()];
        for (int i = 0; i < suiteInfos.length; i++) {
            suiteInfos[i] = suiteInfoFromSuite(suites.get(i));
        }
        return suiteInfos;
    }

    private List<Suite> queryAllSuites() {
        String hql = "from org.genepattern.server.domain.Suite ";
        Query query = getSession().createQuery(hql);
        @SuppressWarnings("unchecked")
        List<Suite> results = query.list();
        return results;
    }
    
    private List<Suite> queryAllSuites(final String userId) {
        String hql = "from org.genepattern.server.domain.Suite "
            + " where accessId = :publicAccessId  or  userId = :userId";
        Query query = getSession().createQuery(hql);
        query.setInteger("publicAccessId", GPConstants.ACCESS_PUBLIC);
        query.setString("userId", userId);

        @SuppressWarnings("unchecked")
        List<Suite> results = query.list();
        return results;
    }

    public SuiteInfo[] getAllSuites(final String userId) throws AdminDAOSysException {
        final List<Suite> results = queryAllSuites(userId);
        return suiteInfoFromSuites(results);
    }

    /**
     * Gets all suites this task is a part of.
     * 
     * @return The suites
     * @exception WebServiceException
     */
    public SuiteInfo[] getSuiteMembership(String taskLsid) throws OmnigeneException {
        PreparedStatement st = null;
        ResultSet rs = null;
        ArrayList<SuiteInfo> suites = new ArrayList<SuiteInfo>();
        try {
            st = getSession().connection().prepareStatement("SELECT lsid FROM suite_modules where module_lsid = ?");
            st.setString(1, taskLsid);
            rs = st.executeQuery();
            while (rs.next()) {
                String suiteId = rs.getString("lsid");
                suites.add(getSuite(suiteId));
            }
        } 
        catch (SQLException e) {
            throw new OmnigeneException(e);
        } 
        finally {
            cleanupJDBC(rs, st);
        }
        return suites.toArray(new SuiteInfo[suites.size()]);
    }

    public static class TaskNameComparator implements Comparator<TaskInfo> {
        public int compare(TaskInfo t1, TaskInfo t2) {
            return t1.getName().compareToIgnoreCase(t2.getName());
        }
    }
}
