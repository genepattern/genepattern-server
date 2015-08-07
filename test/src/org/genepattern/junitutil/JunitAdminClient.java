/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.junitutil;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.genepattern.server.webservice.server.local.IAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;
import org.junit.Ignore;

@Ignore
public class JunitAdminClient implements IAdminClient {
    private static Map<String, TaskInfo> catalog=new ConcurrentHashMap<String,TaskInfo>();
    private static Map<String, SortedSet<TaskInfo>> nameCatalog=new ConcurrentHashMap<String, SortedSet<TaskInfo>>();
    private static Map<String, SortedSet<TaskInfo>> baseLsidCatalog=new ConcurrentHashMap<String, SortedSet<TaskInfo>>();
    
    private static Comparator<TaskInfo> taskLsidComparator = new Comparator<TaskInfo>() {
        //@Override
        public int compare(TaskInfo arg0, TaskInfo arg1) {
            try {
                LSID l0 = new LSID(arg0.getLsid());
                LSID l1= new LSID(arg1.getLsid());
                return l0.compareTo(l1);
            }
            catch (Throwable t) {
                throw new IllegalArgumentException(t.getLocalizedMessage());
            }
        }
    };
    
    private String userId;
    private boolean isAdmin;
    
    public JunitAdminClient(final String userId, final boolean isAdmin) {
        this.userId=userId;
        this.isAdmin=isAdmin;
    }

    public static void addTask(final TaskInfo taskInfo) {
        if (taskInfo==null) {
            throw new IllegalArgumentException("taskInfo==null");
        }
        final String lsidStr=taskInfo.getLsid();
        if (lsidStr==null || lsidStr.length()==0) {
            throw new IllegalArgumentException("taskInfo.lsid not set");
        }
        if (catalog.containsKey(lsidStr)) {
            throw new IllegalArgumentException("duplicate task with lsid="+taskInfo.getLsid());
        }

        LSID lsid=null;
        try {
            lsid=new LSID(lsidStr);
        }
        catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
        catalog.put(lsidStr, taskInfo);

        //add to the base lsid lookup table
        final String baseLsid=lsid.toStringNoVersion();
        SortedSet<TaskInfo> byBaseLsid=baseLsidCatalog.get(baseLsid);
        if (byBaseLsid==null) {
            byBaseLsid=new ConcurrentSkipListSet<TaskInfo>(taskLsidComparator);
            baseLsidCatalog.put(baseLsid, byBaseLsid);
        }
        byBaseLsid.add(taskInfo);

        //add to the name lookup table
        SortedSet<TaskInfo> items=nameCatalog.get(taskInfo.getName());
        if (items==null) {
            items=new ConcurrentSkipListSet<TaskInfo>(taskLsidComparator);
            nameCatalog.put(taskInfo.getName(), items);
        }
        items.add(taskInfo);
    }

    //@Override
    public List<String> getVersions(LSID lsid) {
        // TODO Auto-generated method stub
        throw new IllegalArgumentException("Not implemented!");
    }

    //@Override
    public TaskInfo[] getAllTasksForModuleAdmin() {
        // TODO Auto-generated method stub
        throw new IllegalArgumentException("Not implemented!");
    }

    //@Override
    public TaskInfo[] getTasksOwnedBy() {
        // TODO Auto-generated method stub
        throw new IllegalArgumentException("Not implemented!");
    }

    //@Override
    public TreeMap<String, TaskInfo> getTaskCatalogByLSID(Collection<TaskInfo> tasks) {
        // TODO Auto-generated method stub
        throw new IllegalArgumentException("Not implemented!");
    }

    //@Override
    public TreeMap<String, TaskInfo> getTaskCatalogByLSID() throws WebServiceException {
        // TODO Auto-generated method stub
        throw new IllegalArgumentException("Not implemented!");
    }

    //@Override
    public Collection<TaskInfo> getTaskCatalog() throws WebServiceException {
        // TODO Auto-generated method stub
        throw new IllegalArgumentException("Not implemented!");
    }

    //@Override
    public Collection<TaskInfo> getLatestTasks() throws WebServiceException {
        // TODO Auto-generated method stub
        throw new IllegalArgumentException("Not implemented!");
    }

    //@Override
    public Map<String, Collection<TaskInfo>> getLatestTasksByType() throws WebServiceException {
        // TODO Auto-generated method stub
        throw new IllegalArgumentException("Not implemented!");
    }
    
    private boolean canRead(TaskInfo taskInfo) {
        if (isAdmin) {
            //admin users can read all modules
            return true;
        }
        if (taskInfo.getAccessId() == GPConstants.ACCESS_PUBLIC) {
            //all users can read public modules
            return true;
        }
        if (taskInfo.getUserId().equals(userId)) {
            //otherwise, you must be the owner of the module
            return true;
        }
        return false;
    }

    //@Override
    public TaskInfo getTask(final String lsidOrTaskName) throws WebServiceException {
        //1) check for full lsid
        TaskInfo task=catalog.get(lsidOrTaskName);
        if (task != null) {
            if (canRead(task)) {
                return task;
            }
            return null;
        }
        
        //2) check by module name
        SortedSet<TaskInfo> matchingTasks=nameCatalog.get(lsidOrTaskName);
        if (matchingTasks != null) {
            //return the first match for which the current user has permission
            for(TaskInfo matchingTask : matchingTasks) {
                if (canRead(matchingTask)) {
                    return matchingTask;
                }
            }
            return null;
        }
        
        //3) check by lsid no version
        LSID lsid = null;
        try {
            lsid=new LSID(lsidOrTaskName);
        }
        catch (MalformedURLException e) {
            //it's not an LSID
            //should be an error because we already checked for matching names
            throw new WebServiceException("Can't find TaskInfo for lsidOrTaskName="+lsidOrTaskName);
        }
        
        if (lsid != null) {
            //it is an LSID, 
            String version = lsid.getVersion();
            if (version==null || version.length()==0) {
                //lsid no version
                matchingTasks=baseLsidCatalog.get(lsidOrTaskName);
                if (matchingTasks != null) {
                    for(TaskInfo matchingTask : matchingTasks) {
                        if (canRead(matchingTask)) {
                            return matchingTask;
                        }
                    }
                    return null;
                }
                return null;
            }
            else {
                //unexpected
                throw new WebServiceException("Can't find TaskInfo for lsidOrTaskName="+lsidOrTaskName);
            }
        }
        
        //unexpected
        throw new WebServiceException("Can't find TaskInfo for lsidOrTaskName="+lsidOrTaskName);
    }

    //@Override
    public SuiteInfo getSuite(String lsid) throws WebServiceException {
        // TODO Auto-generated method stub
        throw new IllegalArgumentException("Not implemented!");
    }

    //@Override
    public SuiteInfo[] getAllSuites() throws WebServiceException {
        // TODO Auto-generated method stub
        throw new IllegalArgumentException("Not implemented!");
    }

    //@Override
    public String getUserProperty(String key) {
        // TODO Auto-generated method stub
        throw new IllegalArgumentException("Not implemented!");
    }

}
