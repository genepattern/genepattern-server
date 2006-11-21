/**
 * 
 */
package org.genepattern.server.webapp.jsf;

import static org.genepattern.server.webapp.jsf.UIBeanHelper.getRequest;

import javax.faces.event.ActionEvent;

/**
 * @author jrobinso
 * 
 */
public class ServerSettingsBean {

	private static String[] modes = new String[] { "Access",
			"Command Line Prefix", "File Purge Settings", "History",
			"Java Flag Settings", "Logs", "Module Repository",
			"Proxy Settings", "Search Engine", "Shut Down Server" };

	private String currentMode = modes[0];  // Default

	public String getCurrentMode() {
		return currentMode;
	}

	public void setCurrentMode(String currentMode) {
		this.currentMode = currentMode;
	}

	public String[] getModes() {
		return modes;
	}
	
	public void modeChanged(ActionEvent evt) {
		setCurrentMode(getRequest().getParameter("mode"));		
	}
	
	
}
