package org.genepattern.server.webservice.server.local;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;

import org.genepattern.server.webservice.server.AdminService;
import org.genepattern.server.webservice.server.IAdminService;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.TaskInfo;
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

	public TreeMap getTaskCatalogByLSID() throws WebServiceException {
		return getTaskCatalogByLSID(Arrays.asList(service.getLatestTasks()));
	}

	public Collection getTaskCatalog() throws WebServiceException {
		return Arrays.asList(service.getAllTasks());
	}

	public TaskInfo getTask(String lsid) throws WebServiceException {
		return service.getTask(lsid);
	}
}