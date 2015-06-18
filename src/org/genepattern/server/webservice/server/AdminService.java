/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webservice.server;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.apache.axis.MessageContext;
import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.util.AuthorizationManagerFactory;
import org.genepattern.server.util.IAuthorizationManager;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.dao.AdminDAOSysException;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * AdminService Web Service.
 *
 * @author Joshua Gould
 */

public class AdminService implements IAdminService {
    private static Logger log = Logger.getLogger(AdminService.class);

    private static Map<String, String> serviceInfoMap;
    static {
        serviceInfoMap = new HashMap<String, String>();
        GpContext serverContext = GpContext.getServerContext();
        
        serviceInfoMap.put("genepattern.version", 
                ServerConfigurationFactory.instance().getGenePatternVersion());
        serviceInfoMap.put("lsid.authority", 
                ServerConfigurationFactory.instance().getGPProperty(serverContext, "lsid.authority"));
        serviceInfoMap.put("require.password", 
                ServerConfigurationFactory.instance().getGPProperty(serverContext, "require.password"));
    }


    private AdminDAO adminDAO;
    private IAuthorizationManager authManager = AuthorizationManagerFactory.getAuthorizationManager();
    private String localUserName = "";

    public AdminService() {
        adminDAO = new AdminDAO();
    }

    public AdminService(String localUser) {
        adminDAO = new AdminDAO();
        localUserName = localUser;
    }

    protected String getUserName() {
        MessageContext context = MessageContext.getCurrentContext();
        if (context == null)
            return this.localUserName;

        String username = context.getUsername();
        if (username == null) {
            username = this.localUserName;
        }
        return username;
    }

    public Map getServiceInfo() throws WebServiceException {

        return serviceInfoMap;
    }

    public DataHandler getServerLog() throws WebServiceException {
        if (!AuthorizationHelper.adminServer(getUserName())) {
            throw new WebServiceException("You are not authorized to perform this action.");
        }
        return getLog(false);
    }

    public DataHandler getGenePatternLog() throws WebServiceException {
        if (!AuthorizationHelper.adminServer(getUserName())) {
            throw new WebServiceException("You are not authorized to perform this action.");
        }
        return getLog(true);
    }

    private DataHandler getLog(boolean doGP) throws WebServiceException {
        Calendar cal = Calendar.getInstance();
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd");

        String what = doGP ? "GenePattern" : "Tomcat";
        String filename;
        File f = null;

        // if the date has rolled over but there is not yet an entry in today's
        // log, look backward in time
        for (int i = 0; i < 10; i++) {
            filename = (doGP ? "genepattern.log" : ("localhost_log." + df.format(cal.getTime()) + ".txt"));
            f = new File("logs", filename);
            if (doGP) {
                break;
            }
            if (f.exists()) {
                break;
            }
            cal.add(Calendar.DATE, -1); // backup up one day
        }

        if (!f.exists()) {
            throw new WebServiceException("Log not found.");
        }
        return new DataHandler(new FileDataSource(f));
    }

    /**
     * Returns all tasks that are owned by the user, or are public.
     */
    public TaskInfo[] getAllTasks() throws WebServiceException {

        try {
            return adminDAO.getAllTasksForUser(getUserName());
        } catch (OmnigeneException e) {
            throw new WebServiceException(e);
        }
    }

    public TaskInfo getTask(String lsidOrTaskName) throws WebServiceException {

        try {
            TaskInfo taskInfo = adminDAO.getTask(lsidOrTaskName, getUserName());
           
            if (taskInfo != null) {
                if (!(taskInfo.getAccessId() == GPConstants.ACCESS_PUBLIC || taskInfo.getUserId().equals(getUserName()))) {
                    if (!AuthorizationHelper.adminModules(getUserName())) {
                        throw new WebServiceException(
                                "You do not have the required permissions to get the requested module.");
                    }
                }
            } else if (AuthorizationHelper.adminModules(getUserName())){
            	taskInfo = adminDAO.getTask(lsidOrTaskName, null);
            }
            return taskInfo;
        } catch (OmnigeneException e) {
            throw new WebServiceException(e);
        }
    }

    /**
     * Return the latest version of each task accessible to the user sorted by
     * task name. Tasks are groupd by LSID minus the version, so tasks with the
     * same nambe but different authority are treated as distinct by this
     * method. To group tasks by name use getLatestTasksByName().
     */
    public TaskInfo[] getLatestTasks() throws WebServiceException {

        try {
            return adminDAO.getLatestTasks(getUserName());
        } catch (Exception e) {
            throw new WebServiceException(e);
        }
    }

    /**
     * Returns an array of the latest tasks for the current user. The returned
     * array will not contain tasks with the same name. If more than one task
     * with the same name exists on the server, the returned array will contain
     * the one task with the name that is closest to the server LSID authority.
     * The closest authority is the first match in the sequence: local server
     * authority, Broad authority, other authority.
     *
     * @param username
     *            The username to get the tasks for.
     * @return The array of tasks.
     * @throws AdminDAOSysException
     *             If an error occurs.
     */

    public TaskInfo[] getLatestTasksByName() throws WebServiceException {

        try {
            return adminDAO.getLatestTasksByName(getUserName());
        } catch (AdminDAOSysException e) {
            throw new WebServiceException(e);
        }
    }

    public Map<String, List<String>> getSuiteLsidToVersionsMap() throws WebServiceException {
        Map<String, List<String>> suiteLsid2VersionsMap = new HashMap<String, List<String>>();
        SuiteInfo[] allSuites;
        try {
            allSuites = adminDAO.getAllSuites();
        } catch (AdminDAOSysException e1) {
            throw new WebServiceException(e1);
        }

        for (int i = 0, length = allSuites.length; i < length; i++) {
            SuiteInfo suite = allSuites[i];
            String suiteLsid = suite.getLsid();
            if (suiteLsid != null) {
                try {
                    LSID lsid = new LSID(suiteLsid);
                    String lsidNoVersion = lsid.toStringNoVersion();
                    List<String> versions = suiteLsid2VersionsMap.get(lsidNoVersion);
                    if (versions == null) {
                        versions = new Vector<String>();
                        suiteLsid2VersionsMap.put(lsidNoVersion, versions);
                    }
                    versions.add(lsid.getVersion());
                } catch (java.net.MalformedURLException e) {
                    log.error(e);
                }
            }
        }
        return suiteLsid2VersionsMap;
    }

    public Map<String, List<String>> getLSIDToVersionsMap() throws WebServiceException {

        Map<String, List<String>> lsid2VersionsMap = new HashMap<String, List<String>>();
        TaskInfo[] tasks = null;
        try {
            tasks = adminDAO.getAllTasksForUser(getUserName());
        } catch (OmnigeneException e) {
            throw new WebServiceException(e);
        }
        for (int i = 0, length = tasks.length; i < length; i++) {
            TaskInfo task = tasks[i];
            String _taskLSID = (String) task.getTaskInfoAttributes().get(GPConstants.LSID);
            if (_taskLSID != null) {
                try {
                    LSID lsid = new LSID(_taskLSID);
                    String lsidNoVersion = lsid.toStringNoVersion();
                    List<String> versions = lsid2VersionsMap.get(lsidNoVersion);
                    if (versions == null) {
                        versions = new Vector<String>();
                        lsid2VersionsMap.put(lsidNoVersion, versions);
                    }
                    versions.add(lsid.getVersion());
                } catch (java.net.MalformedURLException e) {
                }
            }
        }
        return lsid2VersionsMap;
    }

    public SuiteInfo getSuite(String lsid) throws WebServiceException {
        try {
            SuiteInfo suite = adminDAO.getSuite(lsid);
            isSuiteOwnerOrAuthorized(suite);
            return suite;
        } catch (OmnigeneException e) {
            throw new WebServiceException(e);
        }
    }

    private void isSuiteOwnerOrAuthorized(SuiteInfo suite) throws WebServiceException {
        if (!(suite.getAccessId() == GPConstants.ACCESS_PUBLIC || suite.getOwner().equals(getUserName()))) {
            if (!AuthorizationHelper.adminSuites(getUserName())) {
                throw new WebServiceException("You do not have the required permissions to get the requested suite.");
            }
        }
    }

    /**
     * Gets the latest versions of all suites that are either public, or owned
     * by the current user.
     *
     * @return The latest suites
     * @exception WebServiceException
     *                If an error occurs
     */
    public SuiteInfo[] getLatestSuites() throws WebServiceException {
        try {
            return adminDAO.getLatestSuitesForUser(getUserName());
        } catch (AdminDAOSysException e) {
            throw new WebServiceException(e);
        }

    }

    /**
     * Gets all versions of all suites that are visible to the current user.
     *
     * @return The suites
     * @exception WebServiceException
     *                if an error occurs
     */
    public SuiteInfo[] getAllSuites() throws WebServiceException {
        try {
            if (AuthorizationHelper.adminSuites(getUserName())) {
                return adminDAO.getAllSuites();
            } else {
                return adminDAO.getAllSuites();

            }
        } catch (AdminDAOSysException e) {
            throw new WebServiceException(e);
        }

    }

    /**
     * Gets all suites this task is a part of
     *
     * @return The suites
     * @exception WebServiceException
     *                If an error occurs
     */
    public SuiteInfo[] getSuiteMembership(String taskLsid) throws WebServiceException {
        try {
            return adminDAO.getSuiteMembership(taskLsid);
        } catch (OmnigeneException e) {
            throw new WebServiceException(e);
        }
    }


}
