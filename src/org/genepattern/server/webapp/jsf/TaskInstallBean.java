package org.genepattern.server.webapp.jsf;

import java.util.*;

/**
 * A session scoped bean to keep track of tasks installed during the user session.
 * 
 * @author jrobinso
 *
 */
public class TaskInstallBean {
	
	/**
	 * A map of LSID -> intall status (true for installed, false for not).
	 */
    Map<String, Boolean> tasks = new HashMap<String, Boolean>();	
    
    
    /**
     * Add the list of lsids to the task installation map
     * @param lsids
     */
    public void addTasks(Collection<String> lsids) {
    	for(String lsid: lsids) {
    		if(!tasks.containsKey(lsid)) {
    			tasks.put(lsid, false);
    		}
    	}
    }
    
    /**
     * Mark a task as installed
     * @param lsid
     */
    public void taskInstalled(String lsid) {
    	tasks.put(lsid, true);
    }
    
    /**
     * Mark a list of tasks as "not installed".  This method is to support a database
     * rollback, where tasks might have previously been marked "installed".
     * @param lsids
     */
    public void rollbackInstalls(Collection<String> lsids) {
    	addTasks(lsids);
    }
    
    /**
     * Return a string representing the tasks that have been installed.
     * @return
     */
    public String getInstalledTaskString() {
    	Set<String> lsids = tasks.keySet();
    	StringBuffer sb = new StringBuffer();
    	for(String lsid : lsids) {
    		if(tasks.get(lsid) == true) {
    			if(sb.length() > 0) {
    				sb.append(", ");
    			}
    			sb.append(lsid);
     		}
    	}
    	return sb.toString();
    }

    
}
