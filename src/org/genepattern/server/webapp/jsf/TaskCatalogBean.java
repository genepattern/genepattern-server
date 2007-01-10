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
import java.util.Set;

import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.genepattern.TaskInstallationException;
import org.genepattern.server.process.InstallTask;
import org.genepattern.server.process.InstallTasksCollectionUtils;
import org.genepattern.server.util.AuthorizationManagerFactoryImpl;
import org.genepattern.server.util.IAuthorizationManager;
import org.genepattern.server.webservice.server.ITaskIntegrator;
import org.genepattern.server.webservice.server.Status;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;

public class TaskCatalogBean {
    private InstallTask[] tasks;

    private List<MyTask> filteredTasks;

    private SelectItem[] operatingSystems = new SelectItem[] { new SelectItem("any") };

    private SelectItem[] states = new SelectItem[] { new SelectItem("new", "new"),
            new SelectItem("updated", "updated"), new SelectItem("up to date", "up to date") };;

    private HashSet missingLsids;

    private static Logger log = Logger.getLogger(TaskCatalogBean.class);

    private boolean error;

    private InstallTasksCollectionUtils collection;

    private Map<String, InstallTask> lsidToTaskMap;

    private List<String> selectedStates;

    private List<String> selectedOperatingSystems;

    private Map<String, List> baseLsidToTasksMap;

    private static final Comparator DESC_COMPARATOR = new DescendingVersionComparator();

    public TaskCatalogBean() {
        selectedStates = new ArrayList<String>();
        selectedStates.addAll(getDefaultStatesSelection());
        selectedOperatingSystems = new ArrayList<String>();
        selectedOperatingSystems.addAll(getDefaultOperatingSystemSelection());
        collection = new InstallTasksCollectionUtils(UIBeanHelper.getUserId(), false);
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
                    List<InstallTask> taskList = baseLsidToTasksMap.get(baseLsid);
                    if (taskList == null) {
                        taskList = new ArrayList<InstallTask>();
                        baseLsidToTasksMap.put(baseLsid, taskList);
                    }
                    taskList.add(t);
                } catch (MalformedURLException e) {
                    log.error(e);
                }
            }

        } catch (Exception e) {
            this.error = true;
            log.error(e);
        }
        lsidToTaskMap = new HashMap<String, InstallTask>();
        if (tasks != null) {
            for (InstallTask t : tasks) {
                lsidToTaskMap.put(t.getLsid(), t);
            }
        }

        String[] operatingSystemsArray = collection.getUniqueValues("os");
        operatingSystems = new SelectItem[operatingSystemsArray.length];
        for (int i = 0; i < operatingSystemsArray.length; i++) {
            String os = operatingSystemsArray[i];
            operatingSystems[i] = new SelectItem(os);
        }
        if (UIBeanHelper.getRequest().getParameter("taskCatalogForm:taskCatalogSubmit") == null) {
            filter();
        }
    }

    public List<MyTask> getTasks() {
        return filteredTasks;
    }

    public List<String> getSelectedOperatingSystems() {
        return selectedOperatingSystems;
    }

    public void setSelectedOperatingSystems(List<String> l) {
        selectedOperatingSystems = l;
    }

    public List<String> getSelectedStates() {
        return selectedStates;
    }

    public void setSelectedStates(List<String> l) {
        selectedStates = l;
    }

    public SelectItem[] getOperatingSystems() {
        return this.operatingSystems;
    }

    public SelectItem[] getStates() {
        return this.states;
    }

    public void setOperatingSystems(SelectItem[] si) {
        this.operatingSystems = si;
    }

    public void getStates(SelectItem[] l) {
        this.states = l;
    }

    public int getNumberOfTasks() {
        return filteredTasks != null ? filteredTasks.size() : 0;
    }

    public boolean isError() {
        return error;
    }

    public HashSet getMissingLsids() {
        return missingLsids;
    }

    public String install() {
        filter();
        IAuthorizationManager authManager = new AuthorizationManagerFactoryImpl().getAuthorizationManager();
        final boolean taskInstallAllowed = authManager.checkPermission("createTask", UIBeanHelper.getUserId());
        if (!taskInstallAllowed) {
            UIBeanHelper.setInfoMessage("You don't have the required permissions to install tasks.");
            return "failure";
        }
        final String[] lsids = UIBeanHelper.getRequest().getParameterValues("installLsid");
        if (lsids != null) {
            final String username = UIBeanHelper.getUserId();
            final TaskInstallBean installBean = (TaskInstallBean) UIBeanHelper.getManagedBean("#{taskInstallBean}");
            installBean.setTasks(lsids, lsidToTaskMap);

            new Thread() {
                public void run() {

                    for (final String lsid : lsids) {
                        try {
                            HibernateUtil.beginTransaction();
                            InstallTask t = lsidToTaskMap.get(lsid);

                            if (t == null) {
                                installBean.setStatus(lsid, "error", lsid + " not found.");

                            } else {

                                t.install(username, GPConstants.ACCESS_PUBLIC, new Status() {

                                    public void beginProgress(String string) {
                                    }

                                    public void continueProgress(int percent) {
                                    }

                                    public void endProgress() {
                                    }

                                    public void statusMessage(String message) {
                                        if (message != null) {
                                            installBean.appendPatchProgressMessage(lsid, message + "<br />");
                                        }
                                    }

                                });
                                installBean.setStatus(lsid, "success");
                            }
                            HibernateUtil.commitTransaction();
                        } catch (TaskInstallationException e) {
                            installBean.setStatus(lsid, "error", e.getMessage());
                            HibernateUtil.rollbackTransaction();
                            log.error(e);
                        }
                    }
                }
            }.start();
        }
        return "install";

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

    private List<String> getDefaultOperatingSystemSelection() {
        List<String> l = new ArrayList<String>();
        l.add("any");
        String os = getOS();
        if (os != null) {
            l.add(os);
        }
        return l;
    }

    private List<String> getDefaultStatesSelection() {
        if (UIBeanHelper.getRequest().getParameter("taskCatalogForm:taskCatalogSubmit") == null) {
            List<String> l = new ArrayList<String>();
            l.add(InstallTask.NEW);
            l.add(InstallTask.UPDATED);
            return l;
        }
        List<String> l = new ArrayList<String>();
        l.add(InstallTask.NEW);
        l.add(InstallTask.UPDATED);
        l.add(InstallTask.UPTODATE);
        return l;

    }

    private String getOS() {
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        boolean isMac = System.getProperty("mrj.version") != null;
        boolean isLinux = System.getProperty("os.name").toLowerCase().startsWith("linux");

        if (isWindows) { // remove all tasks that are not windows or
            // any
            return "Windows";
        } else if (isMac) {
            return "Mac OS X";
        } else if (isLinux) {
            return "Linux";
        }
        return null;
    }

    public void filter() {
    	String[] lsids = UIBeanHelper.getRequest().getParameterValues("lsid");
    	filteredTasks = new ArrayList<MyTask>();
        
    	if (lsids==null) {
	        Map<String, List> filterKeyToValuesMap = new HashMap<String, List>();
	        if (selectedOperatingSystems.size() == 0) {
	            // set default
	            selectedOperatingSystems.addAll(getDefaultOperatingSystemSelection());
	        }
	        filterKeyToValuesMap.put("os", selectedOperatingSystems);
	
	        if (selectedStates.size() == 0) {// set default
	            selectedStates.addAll(getDefaultStatesSelection());
	        }
	        filterKeyToValuesMap.put("state", selectedStates);
	
	        List<InstallTask> allFilteredTasks = new ArrayList<InstallTask>();
	        if (tasks != null) {
	            for (int i = 0; i < tasks.length; i++) {
	                if (tasks[i].matchesAttributes(filterKeyToValuesMap)) {
	                    allFilteredTasks.add(tasks[i]);
	                }
	            }
	        }
	
	        for (int i = 0; i < allFilteredTasks.size(); i++) { // find latest
	            // version of
	            // tasks
	            InstallTask t = allFilteredTasks.get(i);
	            try {
	                List<InstallTask> taskList = baseLsidToTasksMap.remove(new LSID(t.getLsid()).toStringNoVersion());
	                if (taskList != null) {
	                    Collections.sort(taskList, DESC_COMPARATOR);
	                    MyTask latest = new MyTask(taskList.remove(0));
	                    if (taskList.size() > 0) {
	                        MyTask[] laterVersions = new MyTask[taskList.size()];
	                        for (int j = 0; j < taskList.size(); j++) {
	                            laterVersions[j] = new MyTask(taskList.get(j));
	                        }
	                        latest.setLaterVersions(laterVersions);
	                    }
	                    filteredTasks.add(latest);
	                }
	
	            } catch (MalformedURLException e) {
	                log.error(e);
	            }
	        }
    	}else if (tasks != null) {
            MyTask missingTask;
            for (int i = 0; i < tasks.length; i++) {
            	for (String lsid:lsids) {
            		if (lsid.equals(tasks[i].getLsid())) {
                        missingTask = new MyTask(tasks[i]);
                        filteredTasks.add(missingTask);
                    }
            	}      
            }
    		
    	}
        Collections.sort(filteredTasks, new TaskNameComparator());

    }

    public void refilter(Set<String> lsids) {
        filteredTasks = new ArrayList<MyTask>();
        if (tasks != null) {
            MyTask missingTask;
            for (int i = 0; i < tasks.length; i++) {
                if (lsids.contains(tasks[i].getLsid())) {
                    missingTask = new MyTask(tasks[i]);
                    filteredTasks.add(missingTask);
                }
            }
        }
        Collections.sort(filteredTasks, new TaskNameComparator());
    }

    private static class TaskNameComparator implements Comparator<MyTask> {

        public int compare(MyTask t1, MyTask t2) {
            return t1.getName().compareToIgnoreCase(t2.getName());
        }

    }

    private static class DescendingVersionComparator implements Comparator<InstallTask> {

        public int compare(InstallTask t1, InstallTask t2) {
            return new Integer(Integer.parseInt(t2.getLsidVersion())).compareTo(Integer.parseInt(t1.getLsidVersion()));
        }

    }

    public static class MyTask {
        private InstallTask task;

        private MyTask[] laterVersions;

        public void setLaterVersions(MyTask[] laterVersions) {
            this.laterVersions = laterVersions;
        }

        public MyTask[] getLaterVersions() {
            return laterVersions;
        }

        public MyTask(InstallTask task) {
            this.task = task;
        }

        public boolean equals(Object obj) {
            return task.equals(obj);
        }

        public Map getAttributes() {
            return task.getAttributes();
        }

        public String getAuthor() {
            return task.getAuthor();
        }

        public String getDescription() {
            return task.getDescription();
        }

        public String getDocumentationUrl() {
            return task.getDocumentationUrl();
        }

        public String[] getDocUrls() {
            return task.getDocUrls();
        }

        public long getDownloadSize() {
            return task.getDownloadSize();
        }

        public String getExternalSiteName() {
            return task.getExternalSiteName();
        }

        public Map getInstalledTaskInfoAttributes() {
            return task.getInstalledTaskInfoAttributes();
        }

        public String getLanguage() {
            return task.getLanguage();
        }

        public String getLanguageLevel() {
            return task.getLanguageLevel();
        }

        public String getLsid() {
            return task.getLsid();
        }

        public String getLsidVersion() {
            return task.getLsidVersion();
        }

        public long getModificationTimestamp() {
            return task.getModificationTimestamp();
        }

        public String getName() {
            return task.getName();
        }

        public String getOperatingSystem() {
            return task.getOperatingSystem();
        }

        public String getQuality() {
            return task.getQuality();
        }

        public String getRequirements() {
            return task.getRequirements();
        }

        public String getTaskType() {
            return task.getTaskType();
        }

        public String getUrl() {
            return task.getUrl();
        }

        public String getVersionComment() {
            String comment = task.getVersionComment();
            if (comment.length() > 100) {
                comment = comment.substring(0, 99) + "...";
            }
            return comment;
        }

        public int hashCode() {
            return task.hashCode();
        }

        public boolean install(String username, int access_id, ITaskIntegrator taskIntegrator)
                throws TaskInstallationException {
            return task.install(username, access_id, taskIntegrator);
        }

        public boolean isAlreadyInstalled() {
            return task.isAlreadyInstalled();
        }

        public boolean isDeprecated() {
            return task.isDeprecated();
        }

        public boolean isNewer() {
            return task.isNewer();
        }

        public boolean matchesAttributes(Map attributes) {
            return task.matchesAttributes(attributes);
        }

        public void setInitialInstall(boolean initialInstall) {
            task.setInitialInstall(initialInstall);
        }

        public String toLongString() {
            return task.toLongString();
        }

        public String toString() {
            return task.toString();
        }
    }
}
