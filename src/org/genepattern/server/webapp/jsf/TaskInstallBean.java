/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webapp.jsf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.genepattern.server.process.InstallTask;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A session scoped bean to keep track of tasks installed during the user
 * session.
 * 
 * @author jrobinso
 * 
 */
public class TaskInstallBean {

    /**
     * A map of LSID -> intall status (true for installed, false for not).
     */
    Map<String, TaskInstallStatus> tasksMap;

    public TaskInstallBean() {

    }

    /**
     * Sets the list of lsids to the task installation map
     * 
     * @param lsids
     */
    public void setTasks(String[] lsids, Map<String, InstallTask> lsidToTaskMap) {
        tasksMap = new HashMap<String, TaskInstallStatus>();
        for (String lsid : lsids) {
            tasksMap.put(lsid, new TaskInstallStatus(lsid, lsidToTaskMap.get(lsid).getName()));
        }
    }

    /**
     * Sets the list of lsids to the task installation map
     * 
     * @param lsids
     */
    public void setTasks(String[] lsids, String[] taskNames) {
        tasksMap = new HashMap<String, TaskInstallStatus>();
        for (int i = 0; i < lsids.length; i++) {
            tasksMap.put(lsids[i], new TaskInstallStatus(lsids[i], taskNames[i]));
        }
    }

    public void setStatus(String lsid, String status) {
        setStatus(lsid, status, null);
    }

    public void appendPatchProgressMessage(String lsid, String message) {
        TaskInstallStatus bean = tasksMap.get(lsid);
        if (bean != null) {
            bean.patchMessage += message;
        }
    }

    public void setStatus(String lsid, String status, String message) {
        TaskInstallStatus bean = tasksMap.get(lsid);
        if (bean != null) {
            bean.setStatus(status);
            bean.setMessage(message);
        }
    }

    public List<TaskInstallStatus> getTasks() {
        if (tasksMap == null) {
            return Collections.EMPTY_LIST;
        }
        List<TaskInstallStatus> tasks = new ArrayList<TaskInstallStatus>(tasksMap.values());
        Collections.sort(tasks);
        return tasks;
    }

    /**
     * Return a JSON string representing the tasks that have been installed.
     * This method supports an ajax request, the returned string is the response
     * text.
     * 
     */
    public String getInstalledTaskString() {

        JSONArray jsonArray = new JSONArray();
        if (tasksMap != null) {
            for (TaskInstallStatus task : tasksMap.values()) {
                try {
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("lsid", task.getLsid());
                    jsonObj.put("status", task.getStatus());
                    jsonObj.put("message", task.getMessage());
                    String patch = task.getPatchMessage();

                    jsonObj.put("patch", patch);

                    jsonArray.put(jsonObj);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }

        return jsonArray.toString();
    }

    public static class TaskInstallStatus implements Comparable<TaskInstallStatus> {
        String patchMessage = "";

        String lsid;

        String name;

        String status;

        String message;

        public TaskInstallStatus(String lsid, String name) {
            this.lsid = lsid;
            this.name = name;
        }

        public String getPatchMessage() {
            return patchMessage;
        }

        public String getLsid() {
            return lsid;
        }

        public void setLsid(String lsid) {
            this.lsid = lsid;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int compareTo(TaskInstallStatus o) {
            return name.compareToIgnoreCase(o.getName());
        }

    }

}
