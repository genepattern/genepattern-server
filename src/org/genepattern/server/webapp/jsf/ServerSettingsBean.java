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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

import javax.faces.event.ActionEvent;

import org.genepattern.server.util.PropertiesManager;


public class ServerSettingsBean {

	private static String[] modes = new String[] { "Access",
			"Command Line Prefix", "File Purge Settings", "History",
			"Java Flag Settings", "Gene Pattern Log", "Web Server Log", "Module Repository",
			"Proxy Settings", "Search Engine", "Shut Down Server" };
	private String[] clientModes= new String[] {"Local", "Any", "Specified"};
	
	private String currentMode = modes[0];  // Default
	private String currentClientMode = clientModes[0];	//default
	
	private Properties settings;	
	private String proxyPassword;

	private Calendar cal = Calendar.getInstance();
	private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	
	
	private static final String defaultModuleReposityURL="http://www.broad.mit.edu/webservices/genepatternmodulerepository";
	private static final String defaultLog4jPath = "./webapps/gp/WEB-INF/classes/log4j.properties";
	private static final String log4jAppenderR = "log4j.appender.R.File";
	
	/**
	 * 
	 */
	public ServerSettingsBean(){		
		if(settings == null) {
			try {
				settings=PropertiesManager.getGenePatternProperties();
			} catch (IOException ioe){
				ioe.getStackTrace();
			}
		}
	}
	
	/**
	 * @return
	 */
	public String getCurrentMode() {
		return currentMode;
	}

	/**
	 * @param currentMode
	 */
	public void setCurrentMode(String currentMode) {
		this.currentMode = currentMode;
	}

	/**
	 * @return
	 */
	public String[] getModes() {
		return modes;
	}
	
	/**
	 * @param evt
	 */
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
		PropertiesManager.storeChanges(settings);
		return null;   // This returns us to the same page
	}
	
	/**
	 * @return
	 */
	public String getClientMode() {
		currentClientMode= (String)settings.get("gp.allowed.clients");
		
		if (!(clientModes[0].equals(currentClientMode) || clientModes[1].equals(currentClientMode))) {
			currentClientMode=clientModes[2];
		}
		return currentClientMode;
	}
	
	/**
	 * @return
	 */
	public String getSpecifiedClientMode() {
		if (!(clientModes[0].equals(currentClientMode) || clientModes[1].equals(currentClientMode))) {
			return currentClientMode;
		}
		return "";
	}
	
	/**
	 * @param mode
	 */
	public void setClientMode(String mode) {
		currentClientMode=mode;
		settings.put("gp.allowed.clients", mode);		
	}
	
	/**
	 * @param mode
	 */
	public void setSpecifiedClientMode(String mode) {
		if (clientModes[2].equals(currentClientMode)) {		
			settings.put("gp.allowed.clients", mode);
		}		
	}
	
	/**
	 * @return
	 */
	public String getSearchEngine() {
		String searchEngine= (String)settings.get("disable.gp.indexing");		
		return searchEngine;
	}
	
	/**
	 * @param searchEngine
	 */
	public void setSearchEngine(String searchEngine) {
		settings.put("disable.gp.indexing", searchEngine);
	}
	
	/**
	 * @param log
	 * @return
	 * @throws IOException
	 */
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
	
	/**
	 * @return
	 * @throws IOException
	 */
	public String getWsLog() throws IOException {
		return getLog(getWsLogFile());
		
	}
	
	/**
	 * @return
	 * @throws IOException
	 */
	public String getGpLog() throws IOException {
		return getLog(getGpLogFile());
	}
	
	/**
	 * @return
	 */
	public String getGpLogHeader() {	
		File gpLog=getGpLogFile();
		return getLogHeader(gpLog, "Gene Pattern");	
	}
	
	/**
	 * @return
	 */
	public String getWsLogHeader() {	
		File wsLog=getWsLogFile();
		return getLogHeader(wsLog, "Web Server");	
	}
	
	/**
	 * @param log
	 * @param name
	 * @return
	 */
	private String getLogHeader(File log, String name) {
		StringBuffer buf = new StringBuffer();
		if ((log == null || !log.exists())) {
			buf.append("No logs exist.");
		}else {
			buf.append(name+" log file from ");
			buf.append(UIBeanHelper.getRequest().getServerName()+" on ");
			buf.append(cal.getTime());
			
		}
		return buf.toString();
	}
	
	/**
	 * @return
	 */
	private File getGpLogFile() {
		String log4jConfiguration = System.getProperty("log4j.configuration"); 
		if (log4jConfiguration == null) { 
			log4jConfiguration = defaultLog4jPath; 
		}

		Properties props = new Properties();
		try {
			props.load(new FileInputStream(log4jConfiguration));
		}catch (IOException exc) {
		      exc.printStackTrace();
		      System.exit(1);
		}
		return new File(props.getProperty(log4jAppenderR));
	}
	
	/**
	 * @return
	 */
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
	
	/**
	 * @return
	 */
	public String resetModuleRepositoryURL() {
		settings.put("ModuleRepositoryURL", defaultModuleReposityURL);
		return null;
	}
	
	/**
	 * @return
	 */
	public String getProxyPassword() {
		return proxyPassword;
	}

	/**
	 * @param password
	 */
	public void setProxyPassword(String password) {
		proxyPassword = password;
	}
	
	/**
	 * @return
	 */
	public String removeProxySettings() {
		settings.put("http.proxyHost", "");
		settings.put("http.proxyPort", "");
		settings.put("http.proxyUser", "");
		
		settings.put("ftp.proxyHost", "");
		settings.put("ftp.proxyPort", "");
		settings.put("ftp.proxyUser", "");
		proxyPassword="";
		
		return null;
		
	}
	
	/**
	 * @return
	 */
	public String shutDownServer() {
		System.exit(1);
		return null;
	}
}
