package org.genepattern.server.webapp.jsf;

import java.util.*;
import org.json.*;

import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.genepattern.TaskInstallationException;
import org.genepattern.server.process.InstallTask;
import org.genepattern.util.GPConstants;

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
	Map<String, Boolean> lsidStatusMap = new HashMap<String, Boolean>();

	private Map<String, InstallTask> lsidToTaskMap;

	public TaskInstallBean() {

	}

	/**
	 * Add the list of lsids to the task installation map
	 * 
	 * @param lsids
	 */
	public void setLsidsToInstall(String[] lsids) {
		for (String lsid : lsids) {
			if (!lsidStatusMap.containsKey(lsid)) {
				lsidStatusMap.put(lsid, false);
			}
		}
	}

	/**
	 * Mark a task as installed
	 * 
	 * @param lsid
	 */
	public void markTaskInstalled(String lsid) {
		lsidStatusMap.put(lsid, true);
	}

	/**
	 * Mark a list of tasks as "not installed". This method is to support a
	 * database rollback, where tasks might have previously been marked
	 * "installed".
	 * 
	 * @param lsids
	 */
	public void markInstallsRolledBack(String[] lsids) {
		setLsidsToInstall(lsids);
	}

	public List<String> getTaskLsids() {
		List<String> lsids = new ArrayList<String>(lsidStatusMap.keySet());
		Collections.sort(lsids);
		return lsids;
	}

	/**
	 * Return a JSON string representing the tasks that have been installed.  This method
	 * supports an ajax request,  the returned string is the response text.
	 * @return
	 * var myJSONObject = {"bindings": [
        {"ircEvent": "PRIVMSG", "method": "newURI", "regex": "^http://.*"},
        {"ircEvent": "PRIVMSG", "method": "deleteURI", "regex": "^delete.*"},
        {"ircEvent": "PRIVMSG", "method": "randomURI", "regex": "^random.*"}
    ]
};
	 */
	public String getInstalledTaskString() {
		Set<String> lsids = lsidStatusMap.keySet();
		
		JSONArray jsonArray = new JSONArray();	
		for (String lsid : lsids) {
			if (lsidStatusMap.get(lsid) == true) {
				try {
					JSONObject jsonObj = new JSONObject();
					jsonObj.put("lsid", lsid);
					jsonObj.put("status", true);
					jsonArray.put(jsonObj);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		String returnString =  jsonArray.toString();
		System.out.println(returnString);
		
		return returnString;
	}

	public void setLsidToTaskMap(Map<String, InstallTask> lsidToTaskMap) {
		this.lsidToTaskMap = lsidToTaskMap;
	}

	public Map<String, InstallTask> getLsidToTaskMap() {
		return lsidToTaskMap;
	}



}
