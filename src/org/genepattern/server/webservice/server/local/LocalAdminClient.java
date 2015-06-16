/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webservice.server.local;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.genepattern.server.user.UserDAO;
import org.genepattern.server.user.UserProp;
import org.genepattern.server.webservice.server.AdminService;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

public class LocalAdminClient implements IAdminClient {
    AdminService service;

    String userName;

    public LocalAdminClient(final String userName) {
	this.userName = userName;
	service = new AdminService(userName) {
	    protected String getUserName() {
		return userName;
	    }
	};
    }

    public List<String> getVersions(LSID lsid) {
	return new AdminDAO().getVersions(lsid, userName);
    }

    /**
     * Gets all tasks in the database regardless of privacy or owner. Should only be called for a user with moduleAdmin
     * priveleges.
     * 
     * @return All tasks in the database.
     */
    public TaskInfo[] getAllTasksForModuleAdmin() {
	return new AdminDAO().getAllTasks();
    }

    /**
     * Gets all tasks in the database owned by the username passed to the constructor.
     * 
     * @return The tasks.
     */
    public TaskInfo[] getTasksOwnedBy() {
	return new AdminDAO().getTasksOwnedBy(userName);
    }

    public TreeMap<String, TaskInfo> getTaskCatalogByLSID(Collection<TaskInfo> tasks) {
	TreeMap<String, TaskInfo> tmCatalog = new TreeMap<String, TaskInfo>(String.CASE_INSENSITIVE_ORDER);
	for (Iterator<TaskInfo> it = tasks.iterator(); it.hasNext();) {
	    TaskInfo ti = it.next();
	    String lsid = ti.giveTaskInfoAttributes().get(GPConstants.LSID);
	    if (lsid != null && lsid.length() > 0) {
		tmCatalog.put(lsid, ti);
	    }
	    tmCatalog.put(ti.getName(), ti);
	}
	return tmCatalog;
    }

    public TreeMap<String, TaskInfo> getTaskCatalogByLSID() throws WebServiceException {
	return getTaskCatalogByLSID(Arrays.asList(service.getAllTasks()));
    }

    public Collection<TaskInfo> getTaskCatalog() throws WebServiceException {
	return Arrays.asList(service.getAllTasks());
    }

    public Collection<TaskInfo> getLatestTasks() throws WebServiceException {
	return Arrays.asList(service.getLatestTasks());
    }

    /**
     * return a map keyed by taskType with values being alphabetically sorted list of the tasks that are part of that
     * type
     */
    public Map<String, Collection<TaskInfo>> getLatestTasksByType() throws WebServiceException {
	Collection<TaskInfo> latest = getLatestTasks();
	TreeMap<String, Collection<TaskInfo>> typeToTaskMap = new TreeMap<String, Collection<TaskInfo>>(
		new Comparator<String>() {
		    public int compare(String s1, String s2) {
			return ((s1.toLowerCase()).compareTo(s2.toLowerCase()));
		    }
		});

	for (Iterator<TaskInfo> iter = latest.iterator(); iter.hasNext();) {
	    TaskInfo task = iter.next();
	    String type = (String) task.getTaskInfoAttributes().get(GPConstants.TASK_TYPE);

	    if (type == null) {
		type = "Unclassified";
	    }
	    Collection<TaskInfo> typeList = typeToTaskMap.get(type);
	    if (typeList == null) {
		typeList = new TreeSet<TaskInfo>(new Comparator<TaskInfo>() {
		    public int compare(TaskInfo t1, TaskInfo t2) {
			return (((String) t1.getName().toLowerCase()).compareTo(t2.getName().toLowerCase()));
		    }
		});
		typeToTaskMap.put(type, typeList);
	    }
	    typeList.add(task);
	}
	return typeToTaskMap;
    }

    public TaskInfo getTask(String lsid) throws WebServiceException {
	return service.getTask(lsid);
    }

    public SuiteInfo getSuite(String lsid) throws WebServiceException {
	return service.getSuite(lsid);
    }

    public SuiteInfo[] getAllSuites() throws WebServiceException {
	return service.getAllSuites();
    }

    public String getUserProperty(String key) {
	if (key == null) {
	    return null;
	}
	UserProp prop = new UserDAO().getProperty(userName, key);
	return prop != null ? prop.getValue() : null;

    }

}
