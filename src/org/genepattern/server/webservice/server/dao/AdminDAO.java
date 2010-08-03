/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2009) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webservice.server.dao;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
    static Logger log = Logger.getLogger(AdminDAO.class);

    private static int PUBLIC_ACCESS_ID = 1;

    /**
     * Returns the versions of the tasks with the same versionless LSID as the given LSID. The returned list is in
     * ascending order.
     * 
     * @param lsid
     *                the LSID.
     * @param username
     *                the username.
     * @return The versions.
     */
    public List<String> getVersions(LSID lsid, String username) {
        String hql = "from org.genepattern.server.domain.TaskMaster where lsid like :lsid"
            + " and (userId = :userId or accessId = :accessId)";
        Query query = getSession().createQuery(hql);
        query.setString("lsid", lsid.toStringNoVersion() + "%");
        query.setString("userId", username);
        query.setInteger("accessId", GPConstants.ACCESS_PUBLIC);
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

    public TaskInfo getTask(String lsidOrTaskName, String username) throws OmnigeneException {
        if (lsidOrTaskName == null || lsidOrTaskName.trim().equals("")) {
            return null;
        }

        try {
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
                    String hql = "select taskId from org.genepattern.server.domain.TaskMaster where lsid = :lsid";
                    query = getSession().createQuery(hql);
                    query.setString("lsid", lsidOrTaskName);
                }
            } 
            else { // lsid with no version
                if (username != null) {
                    String hql = "select taskId from org.genepattern.server.domain.TaskMaster where lsid like :lsid"
                        + " and (userId = :userId or accessId = :accessId)";
                    query = getSession().createQuery(hql);
                    query.setString("lsid", lsidOrTaskName + "%");
                    query.setString("userId", username);
                    query.setInteger("accessId", GPConstants.ACCESS_PUBLIC);
                } 
                else {
                    String hql = "select taskId from org.genepattern.server.domain.TaskMaster where lsid like :lsid";
                    query = getSession().createQuery(hql);
                    query.setString("lsid", lsidOrTaskName + "%");
                }
            }
            List<Integer> taskIds = query.list();
            TaskInfo latestTask = null;
            LSID latestLSID = null;
            
            TaskInfo[] taskInfos = TaskInfoCache.instance().getAllTasks(taskIds);
            if (taskInfos != null && taskInfos.length > 0) {
                latestTask = taskInfos[0];
                latestLSID = new LSID(taskInfos[0].getLsid());
            }
            boolean first = true;
            for(TaskInfo taskInfo : taskInfos) {
                if (first) {
                    first = false;
                    latestTask = taskInfo;
                    latestLSID = new LSID(taskInfo.getLsid());
                }
                else {
                    LSID l = new LSID(taskInfo.getLsid());
                    if (l.compareTo(latestLSID) < 0) {
                        latestTask = taskInfo;
                        latestLSID = l;
                    }
                }
            }
            return latestTask;
        } 
        catch (java.net.MalformedURLException e) {
            // no lsid specified, find the 'best' match
            Query query = null;
            if (username != null) {
                String hql = "select taskId from org.genepattern.server.domain.TaskMaster where taskName = :taskName "
                    + " and (userId = :userId or accessId = :accessId)";
                query = getSession().createQuery(hql);
                query.setString("taskName", lsidOrTaskName);
                query.setString("userId", username);
                query.setInteger("accessId", GPConstants.ACCESS_PUBLIC);
            } 
            else {
                String hql = "select taskId from org.genepattern.server.domain.TaskMaster where lsid = :taskName";
                query = getSession().createQuery(hql);
                query.setString("taskName", lsidOrTaskName);
            }

            List<Integer> taskIds = query.list();
            TaskInfo[] taskInfos = TaskInfoCache.instance().getAllTasks(taskIds);
            Collection latestTasks = null;
            try {
                latestTasks = getLatestTasks(taskInfos).values();
            } 
            catch (MalformedURLException e1) {
                throw new OmnigeneException(e1);
            }

            TaskInfo latestTask = null;
            LSID closestLSID = null;
            for (Iterator it = latestTasks.iterator(); it.hasNext();) {
                TaskInfo t = (TaskInfo) it.next();
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

            return latestTask;
        }
        catch (RuntimeException e) {
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
        List<Integer> taskIds = query.list();
        return TaskInfoCache.instance().getAllTasks(taskIds);
    }

    public TaskInfo[] getAllTasksForUser(String username) {
        return TaskInfoCache.instance().getAllTasksForUser(username);
    }
    
    /**
     * Get the list of lsids for tasks owned by the user.
     * @param username
     * @return a List<Object[]>, where arg0=(int)taskId and arg1=(String)lsid
     */
    private List<Object[]> getAllLsidsForUser(String username) {
        String hql = "select taskId, lsid from org.genepattern.server.domain.TaskMaster where userId = :userId or accessId = :accessId order by TASK_NAME asc";
        Query query = getSession().createQuery(hql);
        query.setString("userId", username);
        query.setInteger("accessId", PUBLIC_ACCESS_ID);
        List<Object[]> results = query.list();
        return results;
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
            List<Integer> results = query.list();
            for (Integer taskId : results) {
                TaskInfo taskInfo = TaskInfoCache.instance().getTaskInfo(taskId);
                queriedTasks.put(lsid, taskInfo);
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
        List<String> recentLsids = query.list();

        if (recentLsids.isEmpty()) {
            return new TaskInfo[0];
        }

        StringBuffer hql = new StringBuffer();
        hql.append("select taskId from org.genepattern.server.domain.TaskMaster tm where lsid in (");
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
        List<Integer> taskIds = query.list();
        return TaskInfoCache.instance().getAllTasks(taskIds);
    }

    /**
     * Returns a map containing the latest version of each task in the input array. The keys for the map are the
     * no-version LSIDs of the tasks.
     * 
     * @param tasks
     * @return
     * @throws MalformedURLException
     */
    private static Map getLatestTasks(TaskInfo[] tasks) throws MalformedURLException {
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
        List<Integer> taskIds = getLatestTaskIdsForUser(username);
        if (taskIds == null || taskIds.size() == 0) {
            return new TaskInfo[0];
        }
        TaskInfo[] taskInfos = new TaskInfo[taskIds.size()];
        int i=0;
        for(Integer taskId : taskIds) {
            taskInfos[i++] = TaskInfoCache.instance().getTaskInfo(taskId);
        }
        return taskInfos;
    }
    
    private List<Integer> getLatestTaskIdsForUser(String username) {
        int numTotal = 0;
        int numLatest = 0;
        Map<String, Object[]> latestTasks = new HashMap<String, Object[]>();
        List<Object[]> allLsids = getAllLsidsForUser(username);
        
        //for debugging
        numTotal = allLsids.size();
        for(Object[] row : allLsids) {
            //for each row, taskId=(int)row[0], lsid=(String)row[1]
            String lsidStr = (String) row[1];
            try {
                LSID lsid = new LSID(lsidStr);
                LSID altTi = null;
                Object[] altTiWrapper = latestTasks.get(lsid.toStringNoVersion());
                if (altTiWrapper != null) {
                    altTi = new LSID((String)altTiWrapper[1]);
                }
                if (altTi == null) {
                    latestTasks.put(lsid.toStringNoVersion(), row);
                } 
                else {
                    if (altTi.compareTo(lsid) > 0) {
                        latestTasks.put(lsid.toStringNoVersion(), row);
                    }
                }
            }
            catch (MalformedURLException e) {
                log.error("error creating LSID from '"+lsidStr+"', Ignoring this lsid");
                log.debug("exception was: "+e.getMessage(), e);
            }
        }
        //for debugging
        numLatest = latestTasks.size();
        log.debug("Pruned "+(numTotal-numLatest)+" tasks");
        List<Integer> taskIds = new ArrayList<Integer>();
        for(Object[] row : latestTasks.values()) {
            taskIds.add((Integer)row[0]);
        }
        return taskIds;
    }

    public TaskInfo getTask(int taskId) throws OmnigeneException {
        return TaskInfoCache.instance().getTaskInfo(taskId);
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
                throw new TaskIDNotFoundException("AnalysisHypersonicDAO:deleteTask TaskID " + taskID + " not a valid TaskID ");
            }
            
            TaskInfo taskInfoRemoved = TaskInfoCache.instance().remove(taskID);
            log.debug("removed taskInfo from cache: "+ (taskInfoRemoved != null ? taskInfoRemoved.getID() : "null"));
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
        ArrayList latestSuites = _getLatestSuites();
        SuiteInfo[] latest = new SuiteInfo[latestSuites.size()];
        int i = 0;
        for (Iterator iter = latestSuites.iterator(); iter.hasNext(); i++) {
            latest[i] = (SuiteInfo) iter.next();
        }
        return latest;
    }

    private ArrayList _getLatestSuites() throws AdminDAOSysException {
        try {
            SuiteInfo[] allSuites = getAllSuites();
            TreeMap latestSuites = new TreeMap();
            // loop through them placing them into a tree set based on their LSIDs
            for (int i = 0; i < allSuites.length; i++) {
                SuiteInfo si = allSuites[i];
                LSID siLsid = new LSID(si.getLSID());

                SuiteInfo altSi = (SuiteInfo) latestSuites.get(siLsid.toStringNoVersion());

                if (altSi == null) {
                    latestSuites.put(siLsid.toStringNoVersion(), si);
                } 
                else {
                    LSID altLsid = new LSID(altSi.getLSID());
                    if (altLsid.compareTo(siLsid) > 0) {
                        latestSuites.put(siLsid.toStringNoVersion(), si); // it is newer
                    } 
                    // else it is older so leave it out
                }
            }

            ArrayList latest = new ArrayList();
            int i = 0;
            for (Iterator iter = latestSuites.keySet().iterator(); iter.hasNext(); i++) {
                latest.add(latestSuites.get(iter.next()));
            }
            return latest;
        } 
        catch (Exception mfe) {
            throw new AdminDAOSysException("A database error occurred", mfe);
        }
    }

    public SuiteInfo[] getLatestSuitesForUser(String userName) throws AdminDAOSysException {
        ArrayList latestSuites = _getLatestSuites();
        ArrayList<SuiteInfo> allowedSuites = new ArrayList<SuiteInfo>();
        for (Iterator iter = latestSuites.iterator(); iter.hasNext();) {
            SuiteInfo si = (SuiteInfo) iter.next();
            if (si.getAccessId() == GPConstants.ACCESS_PRIVATE) {
                if (!si.getOwner().equals(userName))
                    continue;
            }
            allowedSuites.add(si);
        }

        SuiteInfo[] latest = new SuiteInfo[allowedSuites.size()];
        int i = 0;
        for (Iterator iter = allowedSuites.iterator(); iter.hasNext(); i++) {
            latest[i] = (SuiteInfo) iter.next();
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
        String hql = "from org.genepattern.server.domain.Suite ";
        Query query = getSession().createQuery(hql);
        List<Suite> results = query.list();
        SuiteInfo[] suites = new SuiteInfo[results.size()];
        for (int i = 0; i < suites.length; i++) {
            suites[i] = suiteInfoFromSuite(results.get(i));
        }
        return suites;
    }

    public SuiteInfo[] getAllSuites(String userName) throws AdminDAOSysException {
        String hql = "from org.genepattern.server.domain.Suite "
            + " where accessId = :publicAccessId  or  (accessId = :privateAccessId and owner = :userId)";
        Query query = getSession().createQuery(hql);
        query.setInteger("publicAccessId", GPConstants.ACCESS_PUBLIC);
        query.setInteger("privateAccessId", GPConstants.ACCESS_PRIVATE);
        query.setString("userId", userName);

        List<Suite> results = query.list();
        SuiteInfo[] allowedSuites = new SuiteInfo[results.size()];
        for (int i = 0; i < allowedSuites.length; i++) {
            allowedSuites[i] = suiteInfoFromSuite(results.get(i));
        }
        return allowedSuites;
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

    static class TaskNameComparator implements Comparator<TaskInfo> {
        public int compare(TaskInfo t1, TaskInfo t2) {
            return t1.getName().compareToIgnoreCase(t2.getName());
        }
    }
}
