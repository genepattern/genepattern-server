/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

/**
 * 
 */
package org.genepattern.server.webapp.jsf;

import java.io.PrintStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;

import org.genepattern.server.genepattern.LSIDManager;
import org.genepattern.server.util.AuthorizationManagerFactory;
import org.genepattern.server.util.IAuthorizationManager;
import org.genepattern.server.webservice.server.local.IAdminClient;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.util.StringUtils;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.WebServiceException;

/**
 * @author jrobinso
 * 
 */
public class IndexPageBean {

    private Collection<TaskInfo> tmTasks;
    
    
    public void setSelectedTask(String value) {
        System.out.println("Task = " + value);
    }

    public List<SelectItem> getAllTaskItems() {
        
        List<SelectItem> allTaskItems = new ArrayList<SelectItem>();
        try {
            String userID = getUserId();

            IAdminClient adminClient = new LocalAdminClient(userID);
            Collection latestTmTasks = adminClient.getLatestTasks();
            HashMap latestTaskMap = new HashMap();
            for (Iterator itTasks = latestTmTasks.iterator(); itTasks.hasNext();) {
                TaskInfo taskInfo = (TaskInfo) itTasks.next();
                TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
                String versionlessLSID = (new LSID(tia.get(GPConstants.LSID))).toStringNoVersion();
                latestTaskMap.put(versionlessLSID, taskInfo);
            }

            allTaskItems =  getTaskCatalog(getTmTasks(), latestTaskMap, null, userID, true);
        }
        catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (WebServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return allTaskItems;

    }

    private Collection<TaskInfo> getTmTasks() {
        try {
            if (tmTasks == null) {
                HttpServletRequest request = UIBeanHelper.getRequest();

                String userID = getUserId();
                boolean userIDKnown = !(userID == null || userID.length() == 0);

                IAdminClient adminClient = new LocalAdminClient(userID);
                Collection tmTasks = null;
                Collection latestTmTasks = adminClient.getLatestTasks();
                ArrayList suiteFilterAttr = (ArrayList) UIBeanHelper.getSessionMap().get("suiteSelection");

                boolean allTasks = true;
                SuiteInfo[] suites = adminClient.getAllSuites();

                if (suiteFilterAttr != null) {
                    if (suiteFilterAttr.contains("all")) {
                        allTasks = true;
                        System.out.println("\tall=true");
                    }
                    else {
                        allTasks = false;
                    }
                }

                if (allTasks) {
                    tmTasks = adminClient.getTaskCatalog();
                }
                else {
                    tmTasks = new ArrayList();
                    for (int i = 0; i < suites.length; i++) {
                        SuiteInfo suite = suites[i];
                        if (suiteFilterAttr.contains(suite.getLSID())) {
                            String[] mods = suite.getModuleLSIDs();
                            for (int j = 0; j < mods.length; j++) {
                                TaskInfo ti = adminClient.getTask(mods[j]);
                                if (ti != null) tmTasks.add(ti);
                            }
                        }
                    }
                }

            }
        }
        catch (WebServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return tmTasks;
    }

    protected String getUserId() {
        return (String) UIBeanHelper.getRequestMap().get("userID");
    }

    public Map<String, String> getLsidVersionMap() {
        try {

            Map versionMap = new HashMap<String, String>();

            HttpServletRequest request = UIBeanHelper.getRequest();

            String userID = (String) request.getAttribute("userID");
            boolean userIDKnown = !(userID == null || userID.length() == 0);

            IAdminClient adminClient = new LocalAdminClient(userID);
            Collection tmTasks = null;
            Collection latestTmTasks = adminClient.getLatestTasks();
            ArrayList suiteFilterAttr = (ArrayList) request.getSession().getAttribute("suiteSelection");

            boolean allTasks = true;
            SuiteInfo[] suites = adminClient.getAllSuites();

            if (suiteFilterAttr != null) {
                if (suiteFilterAttr.contains("all")) {
                    allTasks = true;
                    System.out.println("\tall=true");
                }
                else {
                    allTasks = false;
                }
            }

            if (allTasks) {
                tmTasks = adminClient.getTaskCatalog();
            }
            else {
                tmTasks = new ArrayList();
                for (int i = 0; i < suites.length; i++) {
                    SuiteInfo suite = suites[i];
                    if (suiteFilterAttr.contains(suite.getLSID())) {
                        String[] mods = suite.getModuleLSIDs();
                        for (int j = 0; j < mods.length; j++) {
                            TaskInfo ti = adminClient.getTask(mods[j]);
                            if (ti != null) tmTasks.add(ti);
                        }
                    }
                }
            }

            HashMap latestTaskMap = new HashMap();
            for (Iterator itTasks = latestTmTasks.iterator(); itTasks.hasNext();) {
                TaskInfo taskInfo = (TaskInfo) itTasks.next();
                TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
                String versionlessLSID = (new LSID(tia.get(GPConstants.LSID))).toStringNoVersion();
                latestTaskMap.put(versionlessLSID, taskInfo);
            }

            TaskInfo taskInfo;
            TaskInfoAttributes tia;
            boolean isPublic;
            boolean isMine;
            String lsid;
            LSID l;
            TreeMap tmNoVersions = new TreeMap();
            Vector v;
            for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext();) {
                taskInfo = (TaskInfo) itTasks.next();
                tia = taskInfo.giveTaskInfoAttributes();
                isPublic = tia.get(GPConstants.PRIVACY).equals(GPConstants.PUBLIC);
                isMine = tia.get(GPConstants.USERID).equals(userID);
                if (!isPublic && !isMine) {
                    continue;
                }
                lsid = tia.get(GPConstants.LSID);

                try {
                    l = new LSID(lsid);
                    v = (Vector) tmNoVersions.get(l.toStringNoVersion());
                    if (v == null) {
                        v = new Vector();
                        v.add(l.getVersion());

                    }
                    else {
                        String highestVersion = (String) v.firstElement();
                        String curVersion = l.getVersion();

                        if ((curVersion.compareTo(highestVersion)) > 0) {
                            v.add(0, l.getVersion());

                        }
                        else {
                            v.add(l.getVersion());
                        }
                    }
                    tmNoVersions.put(l.toStringNoVersion(), v);
                }
                catch (MalformedURLException mue) {
                    // don't display tasks with bad LSIDs
                }
            }

            PrintStream out = null;

            for (Iterator itLSIDs = tmNoVersions.keySet().iterator(); itLSIDs.hasNext();) {
                lsid = (String) itLSIDs.next();
                v = (Vector) tmNoVersions.get(lsid);

                out.print("LSIDs['" + lsid + "'] = new Array(");
                for (Iterator itVersions = v.iterator(); itVersions.hasNext();) {
                    out.print("'" + (String) itVersions.next() + "'");
                    if (itVersions.hasNext()) {
                        out.print(", ");
                    }
                }
                out.println(");");
            }

            return versionMap;
        }
        catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    public List<SelectItem> getTaskCatalog(Collection<TaskInfo> tmTasks, HashMap latestTaskMap, String type,
            String userID, boolean bIncludePipelines) {

        List<SelectItem> items = new ArrayList<SelectItem>();

        TaskInfo taskInfo;
        TaskInfoAttributes tia;

        // used to avoid displaying multiple versions of same basic task
        HashMap hmLSIDsWithoutVersions = new HashMap();
        String versionlessLSID = null;

        LSID l = null;

        // put public and my tasks into list first
        for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext();) {
            taskInfo = (TaskInfo) itTasks.next();
            tia = taskInfo.giveTaskInfoAttributes();
            String lsid = tia.get(GPConstants.LSID);
            try {
                l = new LSID(lsid);
                versionlessLSID = l.toStringNoVersion();

                TaskInfo latestTaskInfo = (TaskInfo) latestTaskMap.get(versionlessLSID);
                TaskInfoAttributes latestTia = latestTaskInfo.giveTaskInfoAttributes();
                lsid = latestTia.get(GPConstants.LSID);

                String taskType = latestTia.get(GPConstants.TASK_TYPE);
                if (type != null && !taskType.equals(type)) continue;
                if (!bIncludePipelines && taskType.equals(GPConstants.TASK_TYPE_PIPELINE)) continue;
                String display = latestTaskInfo.getName();
                if (taskType.equals(GPConstants.TASK_TYPE_PIPELINE)) {
                    String dotPipeline = "." + GPConstants.TASK_TYPE_PIPELINE;
                    if (display.endsWith(dotPipeline)) {
                        display = display.substring(0, display.length() - dotPipeline.length());
                    }
                }

                int halfLength = Integer.parseInt(System.getProperty("gp.name.halflength", "17"));
                String shortenedName = display;
                if (display.length() > ((2 * halfLength) + 3)) {
                    int len = display.length();
                    int idx = display.length() - halfLength;
                    shortenedName = display.substring(0, halfLength) + "..." + display.substring(idx, len);
                    display = shortenedName;
                }

                String description = latestTaskInfo.getDescription();
                boolean isPublic = latestTia.get(GPConstants.PRIVACY).equals(GPConstants.PUBLIC);
                boolean isMine = latestTia.get(GPConstants.USERID).equals(userID);
                String name = latestTaskInfo.getName();

                String key = versionlessLSID;
                if (hmLSIDsWithoutVersions.containsKey(key)) {
                    continue;
                }
                hmLSIDsWithoutVersions.put(key, latestTaskInfo);
                String authorityType = LSIDManager.getInstance().getAuthorityType(l);

                if (isPublic || isMine) {
                    // get the name of the last version of this LSID
                    String value = (lsid != null ? l.toStringNoVersion() : taskInfo.getName());
                    // + "\" class=\"tasks-" + authorityType + "\"" + "
                    String title = StringUtils.htmlEncode(description) + ", " + l.getAuthority();
                    SelectItem si = new SelectItem();
                    si.setLabel(title);
                    si.setValue(value);
                    items.add(si);
                }
            }
            catch (MalformedURLException mue) {
                System.out.println("index.jsp: skipping " + mue.getMessage() + " in " + lsid);
                continue;
            }
        }

        return items;
    }
}
