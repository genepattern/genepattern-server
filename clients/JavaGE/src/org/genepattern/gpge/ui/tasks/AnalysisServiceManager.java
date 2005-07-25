package org.genepattern.gpge.ui.tasks;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.net.InetAddress;
import java.net.URL;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.AnalysisWebServiceProxy;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * Mantains an internal cache of the latest analysis services. Retrieves older
 * versions of services when requested.
 * 
 * @author Joshua Gould
 */
public class AnalysisServiceManager {
	private String server;

	private String username;

	/** maps lsids to the latest versions of all analysis services */
	private Map lsid2LatestAnalysisServices;

	/** maps lsids to an older version of an analysis service */
	private Map lsid2AnalysisServices;

	private static AnalysisServiceManager instance = new AnalysisServiceManager();

	/** Maps a versionless LSID to a list of versions for that LSID */
	private Map lsid2VersionsMap = new HashMap();

	private AnalysisServiceManager() {
		this.lsid2LatestAnalysisServices = new HashMap();
		final int maxEntries = 50;
		this.lsid2AnalysisServices = new HashMap();
	}

	/**
	 * Gets the <tt>AnalysisServiceManager</tt> instance
	 * 
	 * @return the instance
	 */
	public static AnalysisServiceManager getInstance() {
		return instance;
	}

	/**
	 * Sets the server that this <tt>AnalysisServiceManager</tt> connects to.
	 * Clears the internal cache of analysis services.
	 * 
	 * @param server
	 *            A server URL, for example http://127.0.0.1:8080
	 * @param username
	 *            The username
	 */
	public void changeServer(String server, String username) {
		this.server = server;
		this.username = username;
		lsid2LatestAnalysisServices.clear();
		lsid2VersionsMap.clear();
		lsid2AnalysisServices.clear();
	}

	/**
	 * Gets the internal cache of an unmodifiable map of the module LSIDs
	 * without the version to a <tt>List</tt> of versions. Invoke refresh to
	 * update the internal cache.
	 * 
	 * @return the lsid to versions map
	 */
	public Map getLSIDToVersionsMap() {
		return lsid2VersionsMap;
	}

	/**
	 * Returns <tt>true</tt> if the server is localhost, <tt>false</tt>
	 * otherwise
	 * 
	 * @return whether the server is localhost
	 */
	public boolean isLocalHost() {
		try {
			URL url = new URL(server);
			String host = url.getHost();
			if ("127.0.0.1".equals(host) || "localhost".equals(host)) {
				return true;
			}
			InetAddress localHost = InetAddress.getLocalHost();
			return localHost.getCanonicalHostName().equals(
					InetAddress.getByName(host).getCanonicalHostName());
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Retrieves the latest versions of all analysis services from the server
	 * stores the analysis services internally
	 * 
	 * @exception WebServiceException
	 *                Description of the Exception
	 */
	public void refresh() throws WebServiceException {
		lsid2LatestAnalysisServices.clear();

		TaskInfo[] tasks = new AnalysisWebServiceProxy(server, username)
				.getTasks();
		for (int i = 0; i < tasks.length; i++) {
			TaskInfo task = tasks[i];
			String lsid = (String) task.getTaskInfoAttributes().get(
					GPConstants.LSID);
			String lsidOrTaskName = lsid != null ? lsid : task.getName();
			lsid2LatestAnalysisServices.put(lsidOrTaskName,
					new AnalysisService(server, task));
		}
		this.lsid2VersionsMap = java.util.Collections
				.unmodifiableMap(new org.genepattern.webservice.AdminProxy(
						server, username).getLSIDToVersionsMap());
		this.lsid2AnalysisServices.clear();

	}

	/**
	 * Gets the internal cache of the latest analysis services. Invoke refresh
	 * to update the internal cache.
	 * 
	 * @return the collections of the latest analysis services
	 */
	public Collection getLatestAnalysisServices() {
		return lsid2LatestAnalysisServices.values();
	}

	/**
	 * Gets the server
	 * 
	 * @return the server
	 */
	public String getServer() {
		return server;
	}

	/**
	 * Gets the username
	 * 
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Gets the analysis service with the given task name or lsid or
	 * <code>null</code> if no such service exists.
	 * 
	 * @param lsid
	 *            an LSID
	 * @return the analysis service
	 */
	public AnalysisService getAnalysisService(String lsid) {
		if (lsid == null) {
			return null;
		}
		AnalysisService service = (AnalysisService) lsid2LatestAnalysisServices
				.get(lsid);
		if (service == null) {
			try {
				TaskInfo task = new org.genepattern.webservice.AdminProxy(
						server, username, false).getTask(lsid);// old
				// servers
				// don't
				// have
				// this
				// method
				if (task == null) {
					return null;
				}
				service = new AnalysisService(server, task);
				lsid2AnalysisServices.put(lsid, service);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		return service;
	}
}