package org.genepattern.server.webservice.server.local;


import java.util.*;

import org.genepattern.server.webservice.server.AdminService;
import org.genepattern.server.webservice.server.IAdminService;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.WebServiceException;

public class LocalAdminClient {
	IAdminService service;

	String userName;

	public LocalAdminClient(final String userName) {
		this.userName = userName;
		service = new AdminService() {
			protected String getUserName() {
				return userName;
			}
		};
	}

	public TreeMap getTaskCatalogByLSID(Collection tasks)
			throws WebServiceException {
		TreeMap tmCatalog = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		for (Iterator it = tasks.iterator(); it.hasNext();) {
			TaskInfo ti = (TaskInfo) it.next();
			String lsid = ti.giveTaskInfoAttributes().get(GPConstants.LSID);
			if (lsid != null && lsid.length() > 0) {
				tmCatalog.put(lsid, ti);
			}
			tmCatalog.put(ti.getName(), ti);
		}
		return tmCatalog;
	}

	public Map getLSIDToVersionsMap() throws WebServiceException {
		return service.getLSIDToVersionsMap();
	}

	public TreeMap getTaskCatalogByLSID() throws WebServiceException {
		return getTaskCatalogByLSID(Arrays.asList(service.getAllTasks()));
	}

	public Collection getTaskCatalog() throws WebServiceException {
		return Arrays.asList(service.getAllTasks());
	}
	public Collection getLatestTasks() throws WebServiceException {
		return Arrays.asList(service.getLatestTasks());
	}
	

	/**
	 * return a map keyed by taskType with values being
	 * alphabetically sorted list of the tasks that are
	 * part of that type
	 **/
	public Map getLatestTasksByType() throws WebServiceException{
		Collection latest = getLatestTasks();
		TreeMap typeToTaskMap = new TreeMap(new Comparator() {
			public int compare(Object o1, Object o2) {
				String s1 = (String) o1;
				String s2 = (String) o2;
				return ((s1.toLowerCase()).compareTo(s2.toLowerCase()));
			}
			}
		);
		
		for (Iterator iter = latest.iterator(); iter.hasNext(); ){
			TaskInfo task = (TaskInfo)iter.next();
			String type = (String)task.getTaskInfoAttributes().get(GPConstants.TASK_TYPE);
			
			if (type == null) type = "Unclassified";
			TreeSet typeList = (TreeSet)typeToTaskMap.get(type);
			if (typeList == null) {
				typeList = new TreeSet( new Comparator() {
					public int compare(Object o1, Object o2) {
						TaskInfo t1 = (TaskInfo) o1;
						TaskInfo t2 = (TaskInfo) o2;
						return (((String)t1.getName().toLowerCase()).compareTo(t2.getName().toLowerCase()));
					}
				}
				);
				typeToTaskMap.put(type, typeList);
			}
			typeList.add(task);
		}
		return typeToTaskMap;
	}


	public TaskInfo getTask(String lsid) throws WebServiceException {
		return service.getTask(lsid);
	}

	public SuiteInfo getSuite(String lsid) throws WebServiceException{
		return service.getSuite(lsid);
	}

	
	public SuiteInfo[] getLatestSuites() throws WebServiceException{
		return service.getLatestSuites();
	}
	
	public SuiteInfo[] getAllSuites() throws WebServiceException{
		return service.getAllSuites();
	}

	public SuiteInfo[] getSuiteMembership(String lsid) throws WebServiceException{
		return service.getSuiteMembership(lsid);
	}




}