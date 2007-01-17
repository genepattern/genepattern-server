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

package org.genepattern.server.webservice.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.apache.axis.MessageContext;
import org.genepattern.server.domain.Suite;
import org.genepattern.server.domain.SuiteDAO;
import org.genepattern.server.domain.TaskMaster;
import org.genepattern.server.domain.TaskMasterDAO;
import org.genepattern.server.util.AuthorizationManagerFactoryImpl;
import org.genepattern.server.util.IAuthorizationManager;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.dao.AdminDAOSysException;
import org.genepattern.util.GPConstants;
import org.genepattern.util.IGPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * AdminService Web Service. Do a Thread.yield at beginning of each method-
 * fixes BUG in which responses from AxisServlet are sometimes empty
 * 
 * @author Joshua Gould
 */

public class AdminService implements IAdminService {
    static Map<String, String> serviceInfoMap;

    AdminDAO dataService;

    IAuthorizationManager authManager = (new AuthorizationManagerFactoryImpl()).getAuthorizationManager();

    protected String localUserName = "";

    public AdminService() {
        dataService = new AdminDAO();
    }

    public AdminService(String localUser) {
        dataService = new AdminDAO();
        localUserName = localUser;
    }

    private void isAuthorized(String user, String method) throws WebServiceException {
        if (!authManager.isAllowed(method, user)) {
            throw new WebServiceException("You do not have permission for items owned by other users.");
        }
    }

    private boolean isTaskOwner(String user, String lsid) throws WebServiceException {

        TaskMaster tm = (new TaskMasterDAO()).findByIdLsid(lsid);
        if (tm == null)
            return false; // can't own what you can't see
        return user.equals(tm.getUserId());
    }

    private void isTaskOwnerOrAuthorized(String user, String lsid, String method) throws WebServiceException {

        if (!isTaskOwner(user, lsid)) {
            isAuthorized(user, method);
        }
    }

    private boolean isSuiteOwner(String user, String lsid) {

        Suite aSuite = (new SuiteDAO()).findById(lsid);
        String owner = aSuite.getOwner();
        return owner.equals(getUserName());

    }

    private void isSuiteOwnerOrAuthorized(String user, String lsid, String method) throws WebServiceException {
        if (!isSuiteOwner(user, lsid)) {
            isAuthorized(user, method);
        }
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
        isAuthorized(getUserName(), "AdminService.getServiceInfo");
        Thread.yield();
        return serviceInfoMap;
    }

    public DataHandler getServerLog() throws WebServiceException {
        isAuthorized(getUserName(), "AdminService.getServerLog");
        Thread.yield();
        return getLog(false);
    }

    public DataHandler getGenePatternLog() throws WebServiceException {
        isAuthorized(getUserName(), "AdminService.getGenePatternLog");
        Thread.yield();
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

    public TaskInfo[] getAllTasks() throws WebServiceException {
        isAuthorized(getUserName(), "AdminService.getAllTasks");

        Thread.yield();
        try {
            return dataService.getAllTasksForUser(getUserName());
        } catch (OmnigeneException e) {
            throw new WebServiceException(e);
        }
    }

    public TaskInfo getTask(String lsidOrTaskName) throws WebServiceException {
        isTaskOwnerOrAuthorized(getUserName(), lsidOrTaskName, "AdminService.getTask");
        Thread.yield();
        try {
            return dataService.getTask(lsidOrTaskName, getUserName());
        } catch (OmnigeneException e) {
            throw new WebServiceException(e);
        }
    }

    public TaskInfo[] getLatestTasks() throws WebServiceException {
        isAuthorized(getUserName(), "AdminService.getLatestTasks");
        Thread.yield();
        try {
            return dataService.getLatestTasks(getUserName());
        } catch (Exception e) {
            throw new WebServiceException(e);
        }
    }

    public TaskInfo[] getLatestTasksByName() throws WebServiceException {
        isAuthorized(getUserName(), "AdminService.getLatestTasksByName");

        Thread.yield();
        try {
            return dataService.getLatestTasksByName(getUserName());
        } catch (AdminDAOSysException e) {
            throw new WebServiceException(e);
        }
    }

    public Map<String, List<String>> getSuiteLsidToVersionsMap() throws WebServiceException {
        isAuthorized(getUserName(), "AdminService.getSuiteLsidToVersionsMap");
        Map<String, List<String>> suiteLsid2VersionsMap = new HashMap<String, List<String>>();
        SuiteInfo[] allSuites;
        try {
            allSuites = dataService.getAllSuites();
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
                }
            }
        }
        return suiteLsid2VersionsMap;
    }

    public Map<String, List<String>> getLSIDToVersionsMap() throws WebServiceException {
        isAuthorized(getUserName(), "AdminService.getLSIDToVersionsMap");
        Thread.yield();
        Map<String, List<String>> lsid2VersionsMap = new HashMap<String, List<String>>();
        TaskInfo[] tasks = null;
        try {
            tasks = dataService.getAllTasksForUser(getUserName());
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
        isSuiteOwnerOrAuthorized(getUserName(), lsid, "AdminService.getSuite");
        try {
            SuiteInfo suite = dataService.getSuite(lsid);

            String owner = suite.getOwner();
            if (suite.getAccessId() == IGPConstants.ACCESS_PRIVATE) {
                if (!owner.equals(getUserName())) {
                    throw new WebServiceException("You may not view a private suite you are not the owner of.");
                }
            }
            return suite;
        } catch (OmnigeneException e) {
            throw new WebServiceException(e);
        }
    }

    /**
     * Gets the latest versions of all suites
     * 
     * @return The latest suites
     * @exception WebServiceException
     *                If an error occurs
     */
    public SuiteInfo[] getLatestSuites() throws WebServiceException {
        isAuthorized(getUserName(), "AdminService.getLatestSuites");
        try {
            return dataService.getLatestSuites(getUserName());
        } catch (AdminDAOSysException e) {
            throw new WebServiceException(e);
        }

    }

    /**
     * Gets all versions of all suites
     * 
     * @return The suites
     * @exception WebServiceException
     *                If an error occurs
     */
    public SuiteInfo[] getAllSuites() throws WebServiceException {
        isAuthorized(getUserName(), "AdminService.getAllSuites");
        try {
            return dataService.getAllSuites(getUserName());
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
        isTaskOwnerOrAuthorized(getUserName(), taskLsid, "AdminService.getSuiteMembership");
        try {
            return dataService.getSuiteMembership(taskLsid);
        } catch (OmnigeneException e) {
            throw new WebServiceException(e);
        }
    }

    static {
        serviceInfoMap = new HashMap<String, String>();
        String gpPropsFilename = System.getProperty("genepattern.properties");
        File gpProps = new File(gpPropsFilename, "genepattern.properties");
        if (gpProps.exists()) {
            FileInputStream fis = null;
            Properties props = new Properties();
            try {
                fis = new FileInputStream(gpProps);
                props.load(fis);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (IOException ioe) {
                }
            }
            serviceInfoMap.put("genepattern.version", props.getProperty("GenePatternVersion"));
            serviceInfoMap.put("lsid.authority", props.getProperty("lsid.authority"));
            serviceInfoMap.put("require.password", props.getProperty("require.password"));
        }
    }

}