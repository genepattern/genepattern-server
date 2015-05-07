/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.jsf;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoCache;

/**
 * For the admin page, list all TaskInfo and allow clearing the cache. 
 * This is being developed for debugging changes introduced with the TaskInfoCache.
 * 
 * @author pcarr
 */
public class TaskInfoBean {
    public static class TaskInfoWrapper {
        private TaskInfo taskInfo;
        private LSID lsid;

        public TaskInfoWrapper(final TaskInfo taskInfo) {
            this.taskInfo = taskInfo;
            try {
                lsid = new LSID(taskInfo.getLsid());
            }
            catch (MalformedURLException e) {
                lsid = null;
            }
        }
        
        public TaskInfo getTaskInfo() {
            return taskInfo;
        }
        
        public LSID getLsid() {
            return lsid;
        }

        /**
         * Get the taskId. Wrapped the call to taskInfo#getID because by default
         * (our implementation of) JSF does not deal well 'getID()'.
         * @return
         */
        public int getTaskId() {
            return taskInfo.getID();
        }
        
        public String getLsidNoVersion() {
            if (lsid != null) {
                return lsid.toStringNoVersion();
            }
            //TODO: log error
            return "";
        }
        
        public String getVersion() {
            if (lsid != null) {
                return lsid.getVersion();
            }
            //TODO: log error
            return "";
        }
        
        public boolean isPublic() {
            return taskInfo != null && taskInfo.getAccessId() == 1;
        }
    }

    public void clearCache() {
        TaskInfoCache.instance().clearCache();
    }
    
    public List<TaskInfoWrapper> getTaskInfos() {
        TaskInfo[] taskInfoArray = TaskInfoCache.instance().getAllTasks();
        List<TaskInfoWrapper> taskInfos = new ArrayList<TaskInfoWrapper>();
        for(TaskInfo taskInfo : taskInfoArray) {
            taskInfos.add(new TaskInfoWrapper(taskInfo));
        }
        
        //group by lsidNoVersion
        SortedMap<String,SortedMap<LSID,TaskInfoWrapper>> groupByLsid = new TreeMap<String,SortedMap<LSID,TaskInfoWrapper>>();
        for(TaskInfoWrapper ti : taskInfos) {
            SortedMap<LSID,TaskInfoWrapper> entry = groupByLsid.get(ti.getLsidNoVersion());
            if (entry == null) {
                entry = new TreeMap<LSID, TaskInfoWrapper>();
                groupByLsid.put(ti.getLsidNoVersion(), entry);
            }
            //add taskinfo to entry
            entry.put(ti.getLsid(), ti);
        }
        
        //sort groups by latest task name, ignoring case
        SortedMap<String,List<TaskInfoWrapper>> sortByTaskName = new TreeMap<String,List<TaskInfoWrapper>>();
        for(Entry<String,SortedMap<LSID,TaskInfoWrapper>> groupEntry : groupByLsid.entrySet()) {
            SortedMap<LSID,TaskInfoWrapper> groupValue = groupEntry.getValue();
            TaskInfoWrapper latest = groupValue.get( groupValue.firstKey() );
            List<TaskInfoWrapper> taskInfoWrappers = new ArrayList<TaskInfoWrapper>(groupValue.values());
            sortByTaskName.put(latest.getTaskInfo().getName().toLowerCase(), taskInfoWrappers);
        }

        //return the flattened list
        taskInfos.clear();
        for(List<TaskInfoWrapper> taskInfoWrappers : sortByTaskName.values()) {
            for(TaskInfoWrapper taskInfoWrapper : taskInfoWrappers) {
                taskInfos.add(taskInfoWrapper);
            }
        }
        
        return taskInfos;
    }
    
}
