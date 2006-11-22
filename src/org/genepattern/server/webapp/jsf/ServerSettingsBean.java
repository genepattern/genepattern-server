/**
 * 
 */
package org.genepattern.server.webapp.jsf;

import static org.genepattern.server.webapp.jsf.UIBeanHelper.getRequest;

import java.util.Properties;

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
	
	private Properties settings;

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
	
	/**
	 * Return the properties object with the server settings.  This should be
	 * lazy initialized,  it might be called many times but we only need to read the
	 * file once.
	 * 
	 */
	public Properties getSettings() {
		
		if(settings == null) {
			// TODO -- read this from a file.  
			settings = new Properties();
			settings.put("java_flags", "-Xmx512M");
			// etc.
		}
		return settings;
	}
	
	/**
	 * Save the settings back to the file.  Trigger by the "submit" button on the page.
	 * 
	 * @return
	 */
	public String saveSettings() {
		
		// TODO -- save the settings back to the file
		System.out.println(settings.get("java_flags"));
		
		return null;   // This returns us to the same page
	}
	
	
}
