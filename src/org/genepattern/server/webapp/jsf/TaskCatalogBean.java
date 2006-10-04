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

package org.genepattern.server.webapp.jsf;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;
import org.genepattern.server.genepattern.TaskInstallationException;
import org.genepattern.server.process.InstallTask;
import org.genepattern.server.process.InstallTasksCollectionUtils;
import org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;

public class TaskCatalogBean {
    private InstallTask[] tasks;
    private SelectItem[] operatingSystems;
    private SelectItem[] states;
    private HashSet missingLsids;
    private static Logger log = Logger.getLogger(TaskCatalogBean.class);
    private boolean error;
    // Map of filters. key=filter name, value=List of settings
    private Map<String, List> filterKeyToValuesMap;
    private InstallTasksCollectionUtils collection;
    private Map<String, InstallTask> lsidToTaskMap;
    private static final Comparator DESC_COMPARATOR = new DescendingVersionComparator();

    public TaskCatalogBean() {
        boolean initialInstall = (UIBeanHelper.getRequest().getParameter("initialInstall") != null);
        try {
            collection = new InstallTasksCollectionUtils(UIBeanHelper.getUserId(), initialInstall);
            tasks = collection.getAvailableModules();
            tasks = collection.sortTasks(GPConstants.NAME, true);
            lsidToTaskMap = new HashMap<String, InstallTask>();
            for (InstallTask t : tasks) {
                lsidToTaskMap.put(t.getLsid(), t);
            }
        }
        catch (Exception e) {
            this.error = true;
            log.error(e);
            return;
        }

        updateFilters();
    }

    public InstallTask[] getTasks() {
        return tasks;
    }

    public List<String> getOperatingSystemsSelectedValues() {
        return filterKeyToValuesMap.get("os");
    }

    public SelectItem[] getOperatingSystems() {
        return this.operatingSystems;
    }

    public SelectItem[] getStates() {
        return this.states;
    }

    public List<String> getStatesSelectedValues() {
        return filterKeyToValuesMap.get("state");
    }

    public int getNumberOfTasks() {
        return tasks.length;
    }

    public boolean isError() {
        return error;
    }

    public HashSet getMissingLsids() {
        return missingLsids;
    }

    public void installTasks() {
        String[] lsids = UIBeanHelper.getRequest().getParameterValues("lsid_version");
        if (lsids != null) {
            String username = UIBeanHelper.getUserId();
            LocalTaskIntegratorClient taskIntegrator = new LocalTaskIntegratorClient(username);
            for (String lsid : lsids) {

                try {
                    InstallTask t = lsidToTaskMap.get(lsid);
                    if (t == null) {
                        UIBeanHelper.setInfoMessage("Task " + lsid + " not found.");
                    }
                    else {
                        t.install(username, GPConstants.ACCESS_PUBLIC, taskIntegrator);
                    }
                }
                catch (TaskInstallationException e) {
                    log.error(e);
                }
            }
        }
    }

    public void updateFilters() {
        filterKeyToValuesMap = new HashMap<String, List>();

        String[] requestedLsidsArray = UIBeanHelper.getRequest().getParameterValues(GPConstants.LSID);

        // if a specific list of LSIDs is requested, display just those
        if (requestedLsidsArray != null && requestedLsidsArray.length > 0) {
            filterKeyToValuesMap.clear();
            filterKeyToValuesMap.put(GPConstants.LSID, Arrays.asList(requestedLsidsArray));
            this.missingLsids = new HashSet();
            missingLsids.addAll(Arrays.asList(requestedLsidsArray));
            for (InstallTask task : tasks) {
                String lsidStr = task.getLsid();
                missingLsids.remove(lsidStr); // to look for LSIDs
                // requested
                // but
                // absent
            }
        }
        else {
            String[] filters = { "os", "state" };

            for (String filter : filters) {
                String[] filterValues = UIBeanHelper.getRequest().getParameterValues("filterForm:" + filter);
                if (filterValues != null && filterValues.length > 0) {
                    filterKeyToValuesMap.put(filter, Arrays.asList(filterValues));
                }
            }

            List<String> osFilter = filterKeyToValuesMap.get("os");

            if (osFilter == null) { // user did not select a field for OS, set
                // the
                // default
                boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
                boolean isMac = System.getProperty("mrj.version") != null;
                boolean isLinux = System.getProperty("os.name").toLowerCase().startsWith("linux");

                osFilter = new ArrayList<String>();
                filterKeyToValuesMap.put("os", osFilter);
                if (isWindows) { // remove all tasks that are not windows or
                    // any
                    osFilter.add("Windows");
                }
                else if (isMac) {
                    osFilter.add("Mac OS X");
                }
                else if (isLinux) {
                    osFilter.add("Linux");
                }

                osFilter.add("any");

            }

            String[] operatingSystemsArray = collection.getUniqueValues("os");
            operatingSystems = new SelectItem[operatingSystemsArray.length];
            for (int i = 0; i < operatingSystemsArray.length; i++) {
                String os = operatingSystemsArray[i];
                operatingSystems[i] = new SelectItem(os);
            }

            List<String> stateFilterValues = filterKeyToValuesMap.get("state");
            states = new SelectItem[] { new SelectItem("new", "new"), new SelectItem("updated", "updated"),
                    new SelectItem("up to date", "up to date") };

            if (stateFilterValues == null || stateFilterValues.size() == 0) {
                stateFilterValues = new ArrayList<String>(2);
                stateFilterValues.add(InstallTask.NEW);
                stateFilterValues.add(InstallTask.UPDATED);
                filterKeyToValuesMap.put("state", stateFilterValues);
            }

        }
        this.tasks = collection.filterTasks(filterKeyToValuesMap);

        if (!filterKeyToValuesMap.get("state").contains("up to date")) {
            // remove all earlier versions of modules
            // build HashMap of associations between LSIDs and InstallTask
            // objects
            Map<String, List> baseLsidToTasksMap = new LinkedHashMap<String, List>();
            for (InstallTask t : tasks) {
                try {
                    String baseLsid = new LSID(t.getLsid()).toStringNoVersion();
                    List<InstallTask> taskList = baseLsidToTasksMap.get(baseLsid);
                    if (taskList == null) {
                        taskList = new ArrayList<InstallTask>();
                        baseLsidToTasksMap.put(baseLsid, taskList);
                    }
                    taskList.add(t);
                }
                catch (MalformedURLException e) {
                    log.error(e);
                }
            }
            this.tasks = new InstallTask[baseLsidToTasksMap.size()];
            int i = 0;
            System.out.println(baseLsidToTasksMap);
            for (String baseLsid : baseLsidToTasksMap.keySet()) {
                List<InstallTask> taskList = baseLsidToTasksMap.get(baseLsid);
                Collections.sort(taskList, DESC_COMPARATOR);
                this.tasks[i++] = taskList.get(0);
            }
        }
        else {
            // TODO sort in descending lsid order
        }

    }

    private static class DescendingVersionComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            InstallTask t1 = (InstallTask) o1;
            InstallTask t2 = (InstallTask) o2;
            return new Integer(Integer.parseInt(t2.getLsidVersion())).compareTo(Integer.parseInt(t1.getLsidVersion()));
        }

    }

}
