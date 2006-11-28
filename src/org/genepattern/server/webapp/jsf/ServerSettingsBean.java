/**
 * 
 */
package org.genepattern.server.webapp.jsf;

import static org.genepattern.server.webapp.jsf.UIBeanHelper.getRequest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.genepattern.server.util.PropertiesManager;


/**
 * @author jrobinso
 * 
 */
public class ServerSettingsBean {

	private static String[] modes = new String[] { "Access",
			"Command Line Prefix", "File Purge Settings", "History",
			"Java Flag Settings", "Gene Pattern Log", "Web Server Log", "Module Repository",
			"Proxy Settings", "Search Engine", "Shut Down Server" };

	private String currentMode = modes[0];  // Default
	
	private Properties settings;
	private String proxyPassword;
	
	public static final String LOCAL = "Local";
	public static final String ANY= "Any";
	public static final String SPECIFIED = "Specified";
	private Calendar cal = Calendar.getInstance();
	private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	
	public ServerSettingsBean(){
		
		if(settings == null) {
			// TODO -- read this from a file.  
			/*settings = new Properties();
			settings.put("java_flags", "-Xmx512M");
			settings.put("historySize", "3");
			settings.put("purgeTime", "23:00");
			settings.put("purgeJobsAfter", "7");
			settings.put("ModuleRepositoryURL", "http://www.broad.mit.edu/webservices/genepatternmodulerepository");*/
			try {
				settings=PropertiesManager.getGenePatternProperties();

			} catch (IOException ioe){
				System.out.println(ioe.getStackTrace());
			}
			
			// etc.
		}
	}
	
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
		PropertiesManager.storeChanges(settings);
		return null;   // This returns us to the same page
	}
	
	public String getClientMode() {
		String clientMode= (String)this.settings.get("gp.allowed.clients");
		
		if (!(LOCAL.equals(clientMode) || ANY.equals(clientMode))) {
			
			clientMode=SPECIFIED;
		}
		return clientMode;
	}
	
	public String getSpecifiedClientMode() {
		String clientMode= (String)this.settings.get("gp.allowed.clients");
		
		if (LOCAL.equals(clientMode) || ANY.equals(clientMode)) {		
			return "";
		}
		return clientMode;
	}
	
	public void setClientMode(String mode) {	
		settings.put("gp.allowed.clients", mode);		
	}
	
	public void setSpecifiedClientMode(String mode) {	
		settings.put("gp.allowed.clients", mode);		
	}
	
	public String getProxyPassword() {
		return proxyPassword;
	}

	public void setProxyPassword(String password) {
		proxyPassword = password;
	}
	
	public String getSearchEngine() {
		String searchEngine= (String)this.settings.get("disable.gp.indexing");
		
		return searchEngine;
	}
	
	public void setSearchEngine(String searchEngine) {
		settings.put("disable.gp.indexing", searchEngine);
	}
	
	public String getLog(File log) throws IOException {
		StringBuffer buf = new StringBuffer();
		BufferedReader br=null;
		
		try {
			
			if (log != null && log.exists()) {
				br = new BufferedReader(new FileReader(log));
				String thisLine="";
						
				while ((thisLine = br.readLine()) != null) { // while loop begins here
					buf.append(thisLine).append("\n");
				} // end while
			}
		} catch (IOException exc) {
		      System.out.println(exc);
		      System.exit(1);
		}
		return buf.toString();
		
	}
	
	public String getWsLog() throws IOException {
		return this.getLog(getWsLogFile());
		
	}
	
	public String getGpLog() throws IOException {
		return this.getLog(getGpLogFile());
	}
	
	public String getLogHeader() {
		StringBuffer buf = new StringBuffer();
		File gpLog=getGpLogFile();
		if (gpLog == null || !gpLog.exists()) {
			buf.append("No logs exist.");
		}else {
			buf.append("GenePattern log file from ");
			buf.append(UIBeanHelper.getRequest().getServerName()+" on ");
			buf.append(cal.getTime());
			
		}
		return buf.toString();
	}
	
	
	
	private File getGpLogFile() {
		String log4jConfiguration = System.getProperty("log4j.configuration"); 
		if (log4jConfiguration == null) { 
			log4jConfiguration = "./webapps/gp/WEB-INF/classes/log4j.properties"; 
		}

		Properties props = new Properties();
		try {
			props.load(new FileInputStream(log4jConfiguration));
		}catch (IOException exc) {
		      System.out.println(exc);
		      System.exit(1);
		}
		return new File(props.getProperty("log4j.appender.R.File"));
	}
	
	private File getWsLogFile() {
		File wsLog=null;
		if (System.getProperty("serverInfo").indexOf("Apache Tomcat") != -1) {
			for (int i = 0; i < 10; i++) {
				String filename = "localhost." + df.format(cal.getTime()) + ".log";
				wsLog = new File("logs", filename);
				if (wsLog.exists()) break;
				wsLog = null;
				cal.add(Calendar.DATE, -1); // backup up one day
			}
		}
		return wsLog;
	}
	
}
