package org.genepattern.gpge.ui.tasks;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.AnalysisWebServiceProxy;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * Mantains an internal cache of analysis services.
 * 
 * @author Joshua Gould
 */
public class AnalysisServiceManager {
	private String server;

	private String username;

	/** maps lsids to the latest versions of all analysis services */
	private Map lsid2LatestAnalysisServices;

	private Map filteredTasks = new HashMap();

	/** maps lsids to an older version of an analysis service */
	private Map lsid2AnalysisServices;

	private static AnalysisServiceManager instance = new AnalysisServiceManager();

	/** Maps a versionless LSID to a list of versions for that LSID */
	private Map lsid2VersionsMap = new HashMap();

	private Map filteredLsid2VersionsMap = new HashMap();

	private Comparator versionComparator = new ReverseComparator(
			LSIDVersionComparator.INSTANCE);

	private List suites;

	private static class ReverseComparator implements Comparator {
		private Comparator c;

		public ReverseComparator(Comparator c) {
			this.c = c;
		}

		public int compare(Object obj1, Object obj2) {
			return c.compare(obj2, obj1);
		}
	}

	private AnalysisServiceManager() {
		this.lsid2LatestAnalysisServices = new HashMap();
		this.lsid2AnalysisServices = new HashMap();
	}

	public void setVisibleSuites(List suites) {
		this.suites = suites;
		if (suites != null && suites.size() == 0) {
			this.suites = null;
		}
		if (suites == null) {
			try {
				refresh();
			} catch (WebServiceException e) {
				e.printStackTrace();
			}
		} else {
			filteredTasks.clear();
			filteredLsid2VersionsMap.clear();
			for (int i = 0, size = suites.size(); i < size; i++) {
				SuiteInfo suite = (SuiteInfo) suites.get(i);
				try {
					String[] lsids = suite.getModuleLSIDs();
					for (int j = 0; j < lsids.length; j++) {

						try {
							LSID l = new LSID(lsids[i]);
							List versions = (List) filteredLsid2VersionsMap
									.get(l.toStringNoVersion());
							if (versions == null) {
								versions = new ArrayList();
								filteredLsid2VersionsMap.put(l
										.toStringNoVersion(), versions);
							}
							if (l.getVersion() != null
									&& !l.getVersion().equals("")) {
								versions.add(l.getVersion());
							}
						} catch (MalformedURLException e) {
							e.printStackTrace();
						}

						TaskInfo task = new org.genepattern.webservice.AdminProxy(
								server, username, false).getTask(lsids[j]);
						if (task != null) {
							filteredTasks.put(lsids[j], new AnalysisService(
									server, task));
						} else {
							System.out.println("Can't find " + lsids[j]);
						}
					}
				} catch (WebServiceException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public List getVisibleSuites() {
		return suites;
	}

	/**
	 * Notify this service manager that a task was installed
	 * 
	 * @param lsid
	 */
	public void taskInstalled(LSID lsid) {
		List versions = (List) lsid2VersionsMap.get(lsid.toStringNoVersion());
		if (versions == null) {
			versions = new ArrayList();
			lsid2VersionsMap.put(lsid.toStringNoVersion(), versions);
		}
		if (!versions.contains(lsid.getVersion())) {
			versions.add(lsid.getVersion());
			Collections.sort(versions, versionComparator);
		}

		if (isLatestVersion(lsid)) {
			String lsidNoVersionString = lsid.toStringNoVersion();
			for (Iterator it = lsid2LatestAnalysisServices.keySet().iterator(); it
					.hasNext();) {
				String taskLSID = (String) it.next();
				try {
					if (lsidNoVersionString.equals(new LSID(taskLSID)
							.toStringNoVersion())) {
						it.remove();
						break;
					}
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
			TaskInfo task = null;
			try {
				task = new org.genepattern.webservice.AdminProxy(server,
						username, false).getTask(lsid.toString());
			} catch (WebServiceException e) {
				e.printStackTrace();
			}
			if (task != null) {
				lsid2LatestAnalysisServices.put(lsid.toString(),
						new AnalysisService(server, task));
			} else {
				System.err.println("Installed task " + lsid + " not found.");
			}

		} else {
			getAnalysisService(lsid.toString()); // adds to
			// lsid2AnalysisServices
		}
		if (suites != null) {
			setVisibleSuites(suites);
		}
	}

	/**
	 * Returns <tt>true</tt> if the given <tt>lsid</tt> is the latest
	 * version of the task, <tt>false</tt> otherwise
	 * 
	 * @param lsid
	 *            an lsid
	 * @return whether the lsid is the latest version
	 */
	public boolean isLatestVersion(LSID lsid) {
		Map map = suites == null ? lsid2VersionsMap : filteredLsid2VersionsMap;
		String version = lsid.getVersion();
		List versions = (List) map.get(lsid.toStringNoVersion());
		if (versions == null || versions.size() == 0) {
			return true;
		}
		if (version.equals(versions.get(0))) {
			return true;
		}
		String latestInstalledVersion = (String) versions.get(0);
		return versionComparator.compare(lsid.getVersion(),
				latestInstalledVersion) > 0;

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
		suites = null;
	}

	/**
	 * Gets the internal cache of a map of the module LSIDs without the version
	 * to a <tt>List</tt> of versions. Invoke refresh to update the internal
	 * cache.
	 * 
	 * @return the lsid to versions map. The list of versions is sorted in
	 *         descending order (latest version first).
	 */
	public Map getLSIDToVersionsMap() {
		return suites == null ? lsid2VersionsMap : filteredLsid2VersionsMap;
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
	 * Refreshes the modules
	 * 
	 * @throws WebServiceException
	 */
	public void refresh() throws WebServiceException {
		if (suites == null) {
			updateLatestAnalysisServices();
		} else {
			setVisibleSuites(suites);
		}
	}

	/**
	 * Retrieves the latest versions of all analysis services from the server
	 * stores the analysis services internally
	 * 
	 * @exception WebServiceException
	 *                Description of the Exception
	 */
	private void updateLatestAnalysisServices() throws WebServiceException {
		this.lsid2LatestAnalysisServices.clear();
		this.lsid2AnalysisServices.clear();

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
		this.lsid2VersionsMap = new org.genepattern.webservice.AdminProxy(
				server, username).getLSIDToVersionsMap();

		for (Iterator it = lsid2VersionsMap.keySet().iterator(); it.hasNext();) {
			List versions = (List) lsid2VersionsMap.get(it.next());
			versions = new ArrayList(new HashSet(versions)); // ensure
			// versions are
			// unique
			Collections.sort(versions, versionComparator);
		}
	}

	/**
	 * Gets the internal cache of the latest analysis services. Invoke refresh
	 * to update the internal cache.
	 * 
	 * @return the collections of the latest analysis services
	 */
	public Collection getLatestAnalysisServices() {
		return getLatestAnalysisServices(false);
	}

	/**
	 * Gets the internal cache of the latest analysis services. Invoke refresh
	 * to update the internal cache.
	 * 
	 * @return the collections of the latest analysis services
	 */
	public Collection getLatestAnalysisServices(boolean ignoreFilter) {
		return suites == null || ignoreFilter ? lsid2LatestAnalysisServices
				.values() : filteredTasks.values();
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