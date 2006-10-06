/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webservice.server.dao;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.sql.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.genepattern.server.TaskIDNotFoundException;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.Suite;
import org.genepattern.server.domain.TaskMaster;
import org.genepattern.server.genepattern.LSIDManager;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.*;
import org.hibernate.HibernateException;
import org.hibernate.Query;

/**
 * @author Joshua Gould
 * @modified "Hibernated" by Jim Robinson
 */
public class AdminDAO extends BaseDAO {

    private static int PUBLIC_ACCESS_ID = 1;

    static Logger log = Logger.getLogger(AdminDAO.class);

    public List<String> getVersions(LSID lsid, String username) {
        String hql = "from org.genepattern.server.domain.TaskMaster where lsid like :lsid"
                + " and (userId = :userId or accessId = :accessId)";
        Query query = getSession().createQuery(hql);
        query.setString("lsid", lsid.toStringNoVersion() + "%");
        query.setString("userId", username);
        query.setInteger("accessId", GPConstants.ACCESS_PUBLIC);
        List<TaskMaster> results = query.list();
        List<String> versions = new ArrayList<String>();
        for(TaskMaster tm:results) {
            try {
                versions.add(new LSID(tm.getLsid()).getVersion());
            }
            catch (MalformedURLException e) {
                log.error(e);
            }
        }
        return versions;
    }

    // FIXME see doc for AdminDAO.getTaskId
    public TaskInfo getTask(String lsidOrTaskName, String username) throws OmnigeneException {
        if (lsidOrTaskName == null || lsidOrTaskName.trim().equals("")) {
            return null;
        }

        boolean startTransaction = false;
        try {

            if (!HibernateUtil.getSession().getTransaction().isActive()) {
                HibernateUtil.getSession().beginTransaction();
            }

            LSID lsid = new LSID(lsidOrTaskName);
            String version = lsid.getVersion();

            Query query = null;
            if (version != null && !version.equals("")) {
                if (username != null) {
                    String hql = "from org.genepattern.server.domain.TaskMaster where lsid = :lsid"
                            + " and (userId = :userId or accessId = :accessId)";
                    query = getSession().createQuery(hql);
                    query.setString("lsid", lsidOrTaskName);
                    query.setString("userId", username);
                    query.setInteger("accessId", GPConstants.ACCESS_PUBLIC);
                }
                else {
                    // sql = "SELECT * FROM task_Master WHERE lsid='" +
                    // lsidOrTaskName + "'";
                    String hql = "from org.genepattern.server.domain.TaskMaster where lsid = :lsid";
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
                    String hql = "from org.genepattern.server.domain.TaskMaster where lsid like :lsid"
                            + " and (userId = :userId or accessId = :accessId)";
                    query = getSession().createQuery(hql);
                    query.setString("lsid", lsidOrTaskName + "%");
                    query.setString("userId", username);
                    query.setInteger("accessId", GPConstants.ACCESS_PUBLIC);
                }
                else {
                    // sql = "SELECT * FROM task_master WHERE LSID LIKE '" +
                    // lsidOrTaskName + "%'";
                    String hql = "from org.genepattern.server.domain.TaskMaster where lsid like :lsid";
                    query = getSession().createQuery(hql);
                    query.setString("lsid", lsidOrTaskName + "%");
                }
            }
            List<TaskMaster> results = query.list();
            TaskInfo latestTask = null;
            LSID latestLSID = null;

            Iterator<TaskMaster> iter = results.iterator();
            if (iter.hasNext()) {
                latestTask = taskInfoFromTaskMaster(iter.next());
                latestLSID = new LSID((String) latestTask.getTaskInfoAttributes().get(GPConstants.LSID));
            }
            while (iter.hasNext()) {
                TaskMaster tm = iter.next();
                LSID l = new LSID(tm.getLsid());
                if (l.compareTo(latestLSID) < 0) {
                    latestTask = taskInfoFromTaskMaster(tm);
                    latestLSID = l;
                }
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
                String hql = "from org.genepattern.server.domain.TaskMaster where taskName = :taskName "
                        + " and (userId = :userId or accessId = :accessId)";
                query = getSession().createQuery(hql);
                query.setString("taskName", lsidOrTaskName);
                query.setString("userId", username);
                query.setInteger("accessId", GPConstants.ACCESS_PUBLIC);

            }
            else {
                // sql = "SELECT * FROM task_master WHERE task_name='" +
                // lsidOrTaskName + "'";
                String hql = "from org.genepattern.server.domain.TaskMaster where lsid = :taskName";
                query = getSession().createQuery(hql);
                query.setString("taskName", lsidOrTaskName);

            }

            List<TaskMaster> results = query.list();
            List tasksWithGivenName = new ArrayList();
            for (TaskMaster tm : results) {
                tasksWithGivenName.add(taskInfoFromTaskMaster(tm));
            }
            Collection latestTasks = null;
            try {
                latestTasks = getLatestTasks((TaskInfo[]) tasksWithGivenName.toArray(new TaskInfo[0])).values();
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
                }// shouldn't happen

            }

            if (startTransaction) {
                HibernateUtil.getSession().getTransaction().commit();
            }

            return latestTask;
        }

        catch (RuntimeException e) {
            if (startTransaction) {
                HibernateUtil.getSession().getTransaction().rollback();
            }
            throw e;
        }

    }

    public TaskInfo[] getAllTasks() {

        String hql = "from org.genepattern.server.domain.TaskMaster";
        Query query = getSession().createQuery(hql);
        List<TaskMaster> results = query.list();

        TaskInfo[] allTasks = new TaskInfo[results.size()];
        for (int i = 0; i < allTasks.length; i++) {
            allTasks[i] = taskInfoFromTaskMaster(results.get(i));
        }
        return allTasks;
    }

    public TaskInfo[] getAllTasksForUser(String username) throws OmnigeneException {

        try {
            String hql = "from org.genepattern.server.domain.TaskMaster where userId = :userId or accessId = :accessId";
            Query query = getSession().createQuery(hql);
            query.setString("userId", username);
            query.setInteger("accessId", PUBLIC_ACCESS_ID);
            List<TaskMaster> results = query.list();

            TaskInfo[] allTasks = new TaskInfo[results.size()];
            for (int i = 0; i < allTasks.length; i++) {
                allTasks[i] = taskInfoFromTaskMaster(results.get(i));
            }
            return allTasks;
        }
        catch (HibernateException e) {
            log.error(e);
            throw new OmnigeneException(e);
        }
    }

    private static Map getLatestTasks(TaskInfo[] tasks) throws MalformedURLException {
        Map latestTasks = new HashMap();
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
                    // is
                    // newer
                } // else it is older so leave it out

            }
        }
        return latestTasks;
    }

    public TaskInfo[] getLatestTasksByName(String username) throws AdminDAOSysException {
        TaskInfo[] tasks = getLatestTasks(username);
        Map map = new LinkedHashMap();
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

        TaskInfo[] tasks = this.getAllTasksForUser(username);
        if (tasks == null) {
            return new TaskInfo[0];
        }
        try {
            Map lsidToTask = getLatestTasks(tasks);
            TaskInfo[] tasksArray = (TaskInfo[]) lsidToTask.values().toArray(new TaskInfo[0]);
            Arrays.sort(tasksArray, new TaskNameComparator());
            return tasksArray;
        }
        catch (MalformedURLException mfe) {
            log.error(mfe);
            throw new OmnigeneException("Error fetching task:  Malformed URL: " + mfe.getMessage());
        }
    }

    public TaskInfo getTask(int taskId) throws OmnigeneException {

        String hql = "from org.genepattern.server.domain.TaskMaster where  taskId = :taskId";
        Query query = getSession().createQuery(hql);
        query.setInteger("taskId", taskId);
        TaskMaster tm = (TaskMaster) query.uniqueResult();
        if (tm != null) {
            return taskInfoFromTaskMaster(tm);
        }
        throw new OmnigeneException("task id " + taskId + " not found");
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
                throw new TaskIDNotFoundException("AnalysisHypersonicDAO:deleteTask TaskID " + taskID
                        + " not a valid TaskID ");
            }

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
     *                If an error occurs
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
            // loop through them placing them into a tree set based on their
            // LSIDs
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
                        latestSuites.put(siLsid.toStringNoVersion(), si); // it
                        // is
                        // newer
                    } // else it is older so leave it out

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

    public SuiteInfo[] getLatestSuites(String userName) throws AdminDAOSysException {
        ArrayList latestSuites = _getLatestSuites();
        ArrayList allowedSuites = new ArrayList();
        for (Iterator iter = latestSuites.iterator(); iter.hasNext();) {
            SuiteInfo si = (SuiteInfo) iter.next();
            if (si.getAccessId() == GPConstants.ACCESS_PRIVATE) {
                if (!si.getOwner().equals(userName)) continue;
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
     *                If an error occurs
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
     * Gets all suites this task is a part of
     * 
     * @return The suites
     * @exception WebServiceException
     *                If an error occurs
     */
    public SuiteInfo[] getSuiteMembership(String taskLsid) throws OmnigeneException {
        PreparedStatement st = null;
        ResultSet rs = null;
        ArrayList suites = new ArrayList();
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
        return (SuiteInfo[]) suites.toArray(new SuiteInfo[suites.size()]);
    }

    static class TaskNameComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            TaskInfo t1 = (TaskInfo) o1;
            TaskInfo t2 = (TaskInfo) o2;
            return t1.getName().compareToIgnoreCase(t2.getName());
        }

    }

}