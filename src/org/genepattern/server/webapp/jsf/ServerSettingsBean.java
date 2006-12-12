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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.genepattern.server.util.PropertiesManager;

public class ServerSettingsBean {

    private Map<String, String[]> modes;
    private String[] clientModes = new String[] { "Local", "Any", "Specified" };
    private String currentClientMode = clientModes[0]; // default
    private String specifiedClientMode;
    
    private String currentMode; // Default
    

    private Properties settings;
    private List<KeyValuePair> customSettings;
    private Properties defaultSettings;
    private String proxyPassword;
    private String newCSKey = "";
    private String newCSValue = "";

    private Calendar cal = Calendar.getInstance();
    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    
    /**
     * 
     */
    public ServerSettingsBean() {
    	if (modes == null) {
    		modes=new TreeMap<String, String[]>();
    		modes.put("Access", new String[]{"gp.allowed.clients"});
    		modes.put("Command Line Prefix", new String[]{"gp.allowed.clients"});
    		modes.put("File Purge Settings", new String[]{"purgeJobsAfter", "purgeTime"});
    		modes.put("History", new String[]{"historySize"});
    		modes.put("Java Flag Settings", new String[]{"java_flags"});
    		modes.put("Gene Pattern Log", null);
    		modes.put("Web Server Log", null);
    		modes.put("Repositories", new String[]{"ModuleRepositoryURL", "ModuleRepositoryURLs", "SuiteRepositoryURL", "SuiteRepositoryURLs"});
    		modes.put("Proxy Settings", new String[]{"http.proxyHost", "http.proxyPort", "http.proxyUser"});
    		modes.put("Database Settings", new String[]{"database.vendor", "HSQL_port", "HSQL.class", "HSQL.args", "HSQL.schema", 
    				"hibernate.connection.driver_class", "hibernate.connection.shutdown", "hibernate.connection.url", "hibernate.connection.username", 
    				"hibernate.connection.password", "hibernate.dialect", "hibernate.default_schema", "hibernate.connection.SetBigStringTryClob"});
    		modes.put("LSID", new String[]{"lsid.authority", "lsid.show"});
    		modes.put("Programming Language", new String[]{"perl", "java", "R", "run_r_path"});
    		//modes.put("Documentation Attibutes", new String[]{"files.doc", "files.binary", "files.code"});
    		modes.put("Advanced", new String[]{"DefaultPatchRepositoryURL", "DefaultPatchURL", "patchQualifiers", "patches", "ant", "resources", "index",
    				"tasklib", "jobs", "tomcatCommonLib", "webappDir", "log4j.appender.R.File", "pipeline.cp", "pipeline.main", "pipeline.vmargs", 
    				"pipeline.decorator", "installedPatchLSIDs", "JavaGEInstallerURL", "AnalysisTaskQueuePollingFrequency", "num.threads", 
    				"org.apache.lucene.commitLockTimeout", "org.apache.lucene.writeLockTimeout", "org.apache.lucene.mergeFactor"});
    		modes.put("Create Custom Settings", null);
    		modes.put("Shut Down Server", null);
    	}
    	currentMode=(String)modes.keySet().toArray()[0];
        if (settings == null) {
            try {
                settings = PropertiesManager.getGenePatternProperties();
            }
            catch (IOException ioe) {
                ioe.getStackTrace();
            }
        }
        if (customSettings == null) {
            try {
                Properties tmp = PropertiesManager.getCustomProperties();
                customSettings = new ArrayList<KeyValuePair>();
                for (Map.Entry entry : tmp.entrySet()) {
                    customSettings.add(new KeyValuePair((String) entry.getKey(), (String) entry.getValue()));
                }

            }
            catch (IOException ioe) {
                ioe.getStackTrace();
            }
        }
        if (defaultSettings == null) {
        	try {
        		defaultSettings = PropertiesManager.getDefaultProperties();
            }
            catch (IOException ioe) {
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
    public Object[] getModes() {
        return modes.keySet().toArray();
    }

    /**
     * @param evt
     */
    public void modeChanged(ActionEvent evt) {
        setCurrentMode(getRequest().getParameter("mode"));
    }

    /**
     * Return the properties object with the server settings. This should be
     * lazy initialized, it might be called many times but we only need to read
     * the file once.
     * 
     */
    public Properties getSettings() {
        return settings;
    }

    /**
     * @return
     */
    public String getCurrentClientMode() {
        currentClientMode = (String) settings.get("gp.allowed.clients");

        if (!(clientModes[0].equals(currentClientMode) || clientModes[1].equals(currentClientMode))) {
            currentClientMode = clientModes[2];
        }
        return currentClientMode;
    }

    /**
     * @param mode
     */
    public void setCurrentClientMode(String mode) {
        currentClientMode = mode;
        if (!clientModes[2].equals(currentClientMode)) {
        	settings.put("gp.allowed.clients", mode);
        	specifiedClientMode=null;
        }
    }
    
    /**
     * @return
     */
    public String getSpecifiedClientMode() {
    	return specifiedClientMode;
    }

    /**
     * @param mode
     */
    public void setSpecifiedClientMode(String mode) {
        specifiedClientMode=mode;
    }
    
    /**
     * @return
     */
    public List getSpecifiedClientModes() {
    	return getSelectItems("gp.allowed.clients");
    }

    /**
     * @param mode
     */
    public void setSpecifiedClientModes(List clientModes) {
    	setSelectItems(clientModes, "gp.allowed.clients");
    }
    
    /**
     * @param event
     */
    public void addSpecifiedClientMode(ActionEvent event) {
    	if (clientModes[2].equals(currentClientMode)) {
	    	String allClientModes = (String) settings.get("gp.allowed.clients");
	        String[] result = allClientModes.split(",");
	        boolean exist = false;
	        for (int i = 0; i < result.length; i++) {
	            if (result[i] != null && result[i].equals(specifiedClientMode)) {
	                exist = true;
	                break;
	            }
	        }
	        if (!exist) {
	        	allClientModes = allClientModes.concat(",").concat(specifiedClientMode);
	        }
	        settings.put("gp.allowed.clients", allClientModes);
    	}
        saveSettings(event);
    }
    
    /**
     * @param event
     */
    public void removeSpecifiedClientMode(ActionEvent event) {
    	String allClientModes = (String) settings.get("gp.allowed.clients");
        String[] result = allClientModes.split(",");
        StringBuffer newClientModes = new StringBuffer();
        for (int i = 0; i < result.length; i++) {
            if (result[i] != null && !result[i].equals(specifiedClientMode)) {
            	newClientModes.append(result[i]).append(",");
            }
        }
        settings.put("gp.allowed.clients", newClientModes.substring(0, newClientModes.length() - 1).toString());
    }

    /**
     * @return
     */
    public String getSearchEngine() {
        String searchEngine = (String) settings.get("disable.gp.indexing");
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
        BufferedReader br = null;

        try {

            if (log != null && log.exists()) {
                br = new BufferedReader(new FileReader(log));
                String thisLine = "";

                while ((thisLine = br.readLine()) != null) { // while loop
                                                                // begins here
                    buf.append(thisLine).append("\n");
                } // end while
            }
        }
        catch (IOException exc) {
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
        File gpLog = getGpLogFile();
        return getLogHeader(gpLog, "Gene Pattern");
    }

    /**
     * @return
     */
    public String getWsLogHeader() {
        File wsLog = getWsLogFile();
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
        }
        else {
            buf.append(name + " log file from ");
            buf.append(UIBeanHelper.getRequest().getServerName() + " on ");
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
        	String defaultValue = (String)defaultSettings.get("log4j.configuration");
            log4jConfiguration = defaultValue;            
        }

        Properties props = new Properties();
        try {
            props.load(new FileInputStream(log4jConfiguration));
        }
        catch (IOException exc) {
            exc.printStackTrace();
            System.exit(1);
        }
        return new File(props.getProperty("log4j.appender.R.File"));
    }

    /**
     * @return
     */
    private File getWsLogFile() {
        File wsLog = null;
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
     * @param repositoryName
     * @return
     */
    private List getSelectItems(String commaSeparatedValue) {
        String selectItems = (String) settings.get(commaSeparatedValue);
        String[] result = selectItems.split(",");
        List<SelectItem> valuesLst = new ArrayList<SelectItem>();
        for (int i = 0; i < result.length; i++) {
        	valuesLst.add(new SelectItem(result[i]));
        }
        return valuesLst;
    }

    /**
     * @param mrURLs
     * @param repositoryName
     */
    private void setSelectItems(List values, String Name) {
        StringBuffer repositoryURLs = new StringBuffer();
        for (int i = 0; i < values.size(); i++) {
            repositoryURLs.append(values.get(i)).append(",");
        }
        settings.put(Name, repositoryURLs.substring(0, repositoryURLs.length() - 1).toString());
    }

    /**
     * @param currentRepositoryName
     * @param repositoryNames
     */
    private void addRepositoryURL(String currentRepositoryName, String repositoryNames) {
        String currentRepositoryURL = (String) settings.get(currentRepositoryName);
        String repositoryURLs = (String) settings.get(repositoryNames);
        String[] result = repositoryURLs.split(",");
        boolean exist = false;
        for (int i = 0; i < result.length; i++) {
            if (result[i] != null && result[i].equals(currentRepositoryURL)) {
                exist = true;
                break;
            }
        }
        if (!exist) {
            repositoryURLs = repositoryURLs.concat(",").concat(currentRepositoryURL);
        }
        settings.put(repositoryNames, repositoryURLs);
        saveSettings(null);
    }

    /**
     * @param currentRepositoryName
     * @param repositoryNames
     * @param defaultName
     */
    private void removeRepositoryURL(String currentRepositoryName, String repositoryNames, String defaultName) {
        String currentRepositoryURL = (String) settings.get(currentRepositoryName);
        String repositoryURLs = (String) settings.get(repositoryNames);
        String[] result = repositoryURLs.split(",");
        StringBuffer newRepositoryURLs = new StringBuffer(defaultName + ",");
        if (!defaultName.equals(currentRepositoryURL)) {
            for (int i = 0; i < result.length; i++) {
                if (!defaultName.equals(result[i]) && result[i] != null && !result[i].equals(currentRepositoryURL)) {
                    newRepositoryURLs.append(result[i]).append(",");
                }
            }
            settings.put(repositoryNames, newRepositoryURLs.substring(0, newRepositoryURLs.length() - 1).toString());
        }
    }

    /**
     * @return
     */
    public List getModuleRepositoryURLs() {
        return getSelectItems("ModuleRepositoryURLs");

    }

    /**
     * @param mrURLs
     */
    public void setModuleRepositoryURLs(List mrURLs) {
    	setSelectItems(mrURLs, "ModuleRepositoryURLs");
    }

    /**
     * @return
     */
    public void addModuleRepositoryURL(ActionEvent event) {
    	addRepositoryURL("ModuleRepositoryURL", "ModuleRepositoryURLs");
    }

    /**
     * @return
     */
    public void removeModuleRepositoryURL(ActionEvent event) {
    	String defaultModuleRepositoryURL = (String)defaultSettings.get("DefaultModuleRepositoryURL");
    	removeRepositoryURL("ModuleRepositoryURL", "ModuleRepositoryURLs", defaultModuleRepositoryURL);
    	settings.put("ModuleRepositoryURL", defaultModuleRepositoryURL);
    }
    
    /**
     * @return
     */
    public List getSuiteRepositoryURLs() {
        return getSelectItems("SuiteRepositoryURLs");

    }

    /**
     * @param mrURLs
     */
    public void setSuiteRepositoryURLs(List mrURLs) {
    	setSelectItems(mrURLs, "SuiteRepositoryURLs");
    }

    /**
     * @return
     */
    public void addSuiteRepositoryURL(ActionEvent event) {
        addRepositoryURL("SuiteRepositoryURL", "SuiteRepositoryURLs");
    }

    /**
     * @return
     */
    public void removeSuiteRepositoryURL(ActionEvent event) {
    	String defaultSuiteRepositoryURL = (String)defaultSettings.get("DefaultSuiteRepositoryURL");
        removeRepositoryURL("SuiteRepositoryURL", "SuiteRepositoryURLs", defaultSuiteRepositoryURL);
        settings.put("SuiteRepositoryURL", defaultSuiteRepositoryURL);
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
    public String getDb() {
        String db = (String) settings.get("database.vendor");
        return db;
    }

    /**
     * @param dbName
     */
    public void setDb(String dbName) {
        settings.put("database.vendor", dbName);
    }

    /**
     * @param event
     */
    public void changeDb(ValueChangeEvent event) {
        String db = (String) settings.get("database.vendor");
    }
    
    /**
     * @return
     */
    public String getClobRadio() {
    	String value=(String)settings.get("hibernate.connection.SetBigStringTryClob");
    	return (value==null)?"":value;
    }
    
    /**
     * @param mode
     */
    public void setClobRadio(String mode) {
    	if (mode!=null && (mode.equals("true") || mode.equals("false"))) {
    		settings.put("hibernate.connection.SetBigStringTryClob", mode);
    	}
    }
    
    /**
     * @return
     */
    public String getLsidShowRadio() {
    	String value=(String)settings.get("lsid.show");
    	return (value==null)?"":(value.equals("1")?"true":"false");
    }
    
    /**
     * @param mode
     */
    public void setLsidShowRadio(String mode) {
    	if (mode!=null) {
    		if (mode.equals("true")) {
    			settings.put("lsid.show", "1");
    		}else {
    			settings.put("lsid.show", "0");
    		}		
    	}
    }
    
    /**
     * @return
     */
    public String getShutDownRadio() {
    	String value=(String)settings.get("hibernate.connection.shutdown");
    	return (value==null)?"":value;
    }
    
    /**
     * @param mode
     */
    public void setShutDownRadio(String mode) {
    	if (mode!=null && (mode.equals("true") || mode.equals("false"))) {
    		settings.put("hibernate.connection.shutdown", mode);
    	}
    }
    

    /**
     * @return
     */
    public String shutDownServer() {
        System.exit(1);
        return null;
    }

    /**
     * @return
     */
    public String getNewCSKey() {
        return newCSKey;
    }

    /**
     * @return
     */
    public String getNewCSValue() {
        return newCSValue;
    }

    /**
     * @param key
     */
    public void setNewCSKey(String key) {
        newCSKey = key;
    }

    /**
     * @param value
     */
    public void setNewCSValue(String value) {
        newCSValue = value;
    }

    /**
     * @param event
     */
    public void addNewCustomSetting(ActionEvent event) {
        if (newCSKey != "" && newCSValue != "") {
            customSettings.add(new KeyValuePair(newCSKey, newCSValue));
        }
    }

    /**
     * @param event
     */
    public void deleteCustomSetting(ActionEvent event) {
        customSettings.remove(getKey());
    }

    /**
     * @return
     */
    public List<KeyValuePair> getCustomSettings() {
        return customSettings;
    }

    /**
     * @return
     */
    private String getKey() {
        Map params = UIBeanHelper.getExternalContext().getRequestParameterMap();
        String key = (String) params.get("key");
        return key;
    }
    
    /**
     * Save the settings back to the file. Trigger by the "submit" button on the
     * page.
     * 
     * @return
     */
    public void saveSettings(ActionEvent event) {
        PropertiesManager.storeChanges(settings);
        PropertiesManager.storeChangesToCustomProperties(customSettings);
    }
    
    /**
     * @param event
     */
    public void restore(ActionEvent event) {
		String[] propertyKeys = modes.get(currentMode);	
		String subtype = currentMode.equals("Repositories")?(String)event.getComponent().getAttributes().get("subtype"):"";
				
		if (propertyKeys!=null) {
			String defaultValue;
			for (String propertyKey:propertyKeys) {
				if (subtype.equals("Module") && !propertyKey.contains(subtype)) {
					continue;
				}else if (subtype.equals("Suite") && !propertyKey.contains(subtype)) {
					continue;
				}
				defaultValue = (String)defaultSettings.get(propertyKey);			
				if (defaultValue!=null) {
					settings.put(propertyKey, defaultValue);
				}
			}
			saveSettings(event);
		}
	}
    
}
