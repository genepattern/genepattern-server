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

    private SelectItem[] operatingSystems = new SelectItem[] { new SelectItem(
            "any") };

    private SelectItem[] states = new SelectItem[] {
            new SelectItem("new", "new"), new SelectItem("updated", "updated"),
            new SelectItem("up to date", "up to date") };;

    private HashSet missingLsids;

    private static Logger log = Logger.getLogger(TaskCatalogBean.class);

    private boolean error;

    private InstallTasksCollectionUtils collection;

    private Map<String, InstallTask> lsidToTaskMap;

    private List<String> selectedStates;

    private List<String> selectedOperatingSystems;

    private Map<String, List> baseLsidToTasksMap;

    private boolean filter;

    private static final Comparator DESC_COMPARATOR = new DescendingVersionComparator();

    public TaskCatalogBean() {
        filter = false;
        selectedStates = new ArrayList<String>();
        selectedOperatingSystems = new ArrayList<String>();
        collection = new InstallTasksCollectionUtils(UIBeanHelper.getUserId(),
                false);
        try {
            this.tasks = collection.getAvailableModules();
        } catch (Exception e) {
            log.error(e);
            error = true;
        }
        try {
            this.baseLsidToTasksMap = new LinkedHashMap<String, List>();
            for (InstallTask t : tasks) {
                try {
                    String baseLsid = new LSID(t.getLsid()).toStringNoVersion();
                    List<InstallTask> taskList = baseLsidToTasksMap
                            .get(baseLsid);
                    if (taskList == null) {
                        taskList = new ArrayList<InstallTask>();
                        baseLsidToTasksMap.put(baseLsid, taskList);
                    }
                    taskList.add(t);
                } catch (MalformedURLException e) {
                    log.error(e);
                }
            }
            System.out.println("tasks before filtering " + tasks.length);

        } catch (Exception e) {
            this.error = true;
            log.error(e);
        }
        lsidToTaskMap = new HashMap<String, InstallTask>();
        for (InstallTask t : tasks) {
            lsidToTaskMap.put(t.getLsid(), t);
        }

        String[] operatingSystemsArray = collection.getUniqueValues("os");
        operatingSystems = new SelectItem[operatingSystemsArray.length];
        for (int i = 0; i < operatingSystemsArray.length; i++) {
            String os = operatingSystemsArray[i];
            operatingSystems[i] = new SelectItem(os);
        }
    }

    public InstallTask[] getTasks() {
        if (!filter) {
            filterTasks();
        }
        return tasks;
    }

    public List<String> getSelectedOperatingSystems() {
        return selectedOperatingSystems;
    }

    public void setSelectedOperatingSystems(List<String> l) {
        selectedOperatingSystems = l;
    }

    public SelectItem[] getOperatingSystems() {
        return this.operatingSystems;
    }

    public SelectItem[] getStates() {
        return this.states;
    }

    public List<String> getSelectedStates() {
        return selectedStates;
    }

    public void getSelectedStates(List<String> l) {
        selectedStates = l;
    }

    public int getNumberOfTasks() {
        if (!filter) {
            filterTasks();
        }
        return tasks.length;
    }

    public boolean isError() {
        return error;
    }

    public HashSet getMissingLsids() {
        return missingLsids;
    }

    public void installTasks() {
        String[] lsids = UIBeanHelper.getRequest().getParameterValues(
                "lsid_version");
        if (lsids != null) {
            String username = UIBeanHelper.getUserId();
            LocalTaskIntegratorClient taskIntegrator = new LocalTaskIntegratorClient(
                    username);
            for (String lsid : lsids) {

                try {
                    InstallTask t = lsidToTaskMap.get(lsid);
                    if (t == null) {
                        UIBeanHelper.setInfoMessage("Task " + lsid
                                + " not found.");
                    } else {
                        t.install(username, GPConstants.ACCESS_PUBLIC,
                                taskIntegrator);
                    }
                } catch (TaskInstallationException e) {
                    log.error(e);
                }
            }
        }
    }

    private void findMissingTasks(String[] requestedLsidsArray) {
        // if a specific list of LSIDs is requested, display just those
        if (requestedLsidsArray != null && requestedLsidsArray.length > 0) {
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
    }

    public void filterTasks() {
        filter = true;
        Map<String, List> filterKeyToValuesMap = new HashMap<String, List>();
        if (selectedOperatingSystems.size() > 0) {
            filterKeyToValuesMap.put("os", selectedOperatingSystems);
        } else {
            // set default
            boolean isWindows = System.getProperty("os.name").toLowerCase()
                    .startsWith("windows");
            boolean isMac = System.getProperty("mrj.version") != null;
            boolean isLinux = System.getProperty("os.name").toLowerCase()
                    .startsWith("linux");
            filterKeyToValuesMap.put("os", selectedOperatingSystems);
            if (isWindows) { // remove all tasks that are not windows or
                // any
                selectedOperatingSystems.add("Windows");
            } else if (isMac) {
                selectedOperatingSystems.add("Mac OS X");
            } else if (isLinux) {
                selectedOperatingSystems.add("Linux");
            }
            selectedOperatingSystems.add("any");
        }
        if (selectedStates.size() > 0) {
            filterKeyToValuesMap.put("state", selectedStates);
        } else { // set default
            selectedStates.add(InstallTask.NEW);
            selectedStates.add(InstallTask.UPDATED);
            filterKeyToValuesMap.put("state", selectedStates);
        }
        List<InstallTask> filteredTasks = new ArrayList<InstallTask>();
        for (int i = 0; i < tasks.length; i++) {
            if (tasks[i].matchesAttributes(filterKeyToValuesMap)) {
                filteredTasks.add(tasks[i]);
            }
        }
        Collections.sort(filteredTasks, new TaskNameComparator());

        for (int i = 0; i < filteredTasks.size(); i++) { // find latest
                                                            // version of
            // tasks
            InstallTask t = filteredTasks.get(i);
            String lsid = t.getLsid();

            try {
                List<InstallTask> taskList = baseLsidToTasksMap.get(new LSID(
                        lsid).toStringNoVersion());

            } catch (MalformedURLException e) {
                log.error(e);
            }
        }
        this.tasks = (InstallTask[]) filteredTasks.toArray(new InstallTask[0]);

    }

    private static class TaskNameComparator implements Comparator<InstallTask> {

        public int compare(InstallTask t1, InstallTask t2) {
            return t1.getName().compareToIgnoreCase(t2.getName());
        }

    }

    private static class DescendingVersionComparator implements
            Comparator<InstallTask> {

        public int compare(InstallTask t1, InstallTask t2) {
            return new Integer(Integer.parseInt(t2.getLsidVersion()))
                    .compareTo(Integer.parseInt(t1.getLsidVersion()));
        }

    }

}
