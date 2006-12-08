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

    private Map<String, String[]> modes;/* = new String[] { "Access", "Command Line Prefix", "File Purge Settings", "History",
            "Java Flag Settings", "Gene Pattern Log", "Web Server Log", "Repositories", "Proxy Settings",
            "Search Engine", "Database Settings", "LSID", "Programming Language", "Documentation Attibutes",
            "Advanced", "Create Custom Settings", "Shut Down Server" };*/
    private String[] clientModes = new String[] { "Local", "Any", "Specified" };

    private String currentMode;// = modes[0]; // Default
    private String currentClientMode = clientModes[0]; // default

    private Properties settings;
    private List<KeyValuePair> customSettings;
    private Properties defaultSettings;
    private String proxyPassword;
    private String newCSKey = "";
    private String newCSValue = "";

    private ArrayList<KeyValuePair> out = new ArrayList<KeyValuePair>();
    private Calendar cal = Calendar.getInstance();
    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    private static final String defaultModuleRepositoryURL = "http://www.broad.mit.edu/webservices/genepatternmodulerepository";
    private static final String defaultSuiteRepositoryURL = "http://www.broad.mit.edu/webservices/genepatternmodulerepository/suite";
    private static final String defaultLog4jPath = "./webapps/gp/WEB-INF/classes/log4j.properties";
    private static final String log4jAppenderR = "log4j.appender.R.File";

    private static final String hsqlConnectionDriverclass = "org.hsqldb.jdbcDriver";
    private static final String hsqlConnectionUrl = "jdbc:hsqldb:hsql://localhost/xdb";
    private static final String hsqlConnectionUsername = "sa";
    private static final String hsqlConnectionPassword = "";
    private static final String hsqlDialect = "org.hibernate.dialect.HSQLDialect";

    private static final String oracleConnectionDriverclass = "oracle.jdbc.OracleDriver";
    private static final String oracleConnectionUrl = "jdbc:oracle:thin:@magnesium.broad.mit.edu:1521:meddev10";
    private static final String oracleConnectionUsername = "gpportal";
    private static final String oracleConnectionPassword = "gpportal";
    private static final String oracleDialect = "org.genepattern.server.database.PlatformOracle9Dialect";
    private static final String oracleDefaultSchema = "gpportal";

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
    		modes.put("Search Engine", new String[]{"disable.gp.indexing"});
    		modes.put("Database Settings", new String[]{"database.vendor", "HSQL_port", "HSQL.class", "HSQL.args", "HSQL.schema", 
    				"hibernate.connection.driver_class", "hibernate.connection.shutdown", "hibernate.connection.url", "hibernate.connection.username", 
    				"hibernate.connection.password", "hibernate.dialect", "hibernate.default_schema", "hibernate.connection.SetBigStringTryClob"});
    		modes.put("LSID", new String[]{"lsid.authority", "lsid.show"});
    		modes.put("Programming Language", new String[]{"perl", "java", "R", "run_r_path"});
    		modes.put("Documentation Attibutes", new String[]{"files.doc", "files.binary", "files.code"});
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
    public String getClientMode() {
        currentClientMode = (String) settings.get("gp.allowed.clients");

        if (!(clientModes[0].equals(currentClientMode) || clientModes[1].equals(currentClientMode))) {
            currentClientMode = clientModes[2];
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
        currentClientMode = mode;
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
            log4jConfiguration = defaultLog4jPath;
        }

        Properties props = new Properties();
        try {
            props.load(new FileInputStream(log4jConfiguration));
        }
        catch (IOException exc) {
            exc.printStackTrace();
            System.exit(1);
        }
        return new File(props.getProperty(log4jAppenderR));
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
    private Collection getRepositoryURLs(String repositoryName) {
        String repositoryURLs = (String) settings.get(repositoryName);
        String[] result = repositoryURLs.split(",");
        Collection<SelectItem> repositoryURLsLst = new ArrayList<SelectItem>();
        for (int i = 0; i < result.length; i++) {
            repositoryURLsLst.add(new SelectItem(result[i]));
        }
        return repositoryURLsLst;
    }

    /**
     * @param mrURLs
     * @param repositoryName
     */
    private void setRepositoryURLs(ArrayList mrURLs, String repositoryName) {
        StringBuffer repositoryURLs = new StringBuffer();
        for (int i = 0; i < mrURLs.size(); i++) {
            repositoryURLs.append(mrURLs.get(i)).append(",");
        }
        settings.put(repositoryName, repositoryURLs.substring(0, repositoryURLs.length() - 1).toString());
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
        resetModuleRepositoryURL();
    }

    /**
     * @return
     */
    public Collection getModuleRepositoryURLs() {
        return getRepositoryURLs("ModuleRepositoryURLs");

    }

    /**
     * @param mrURLs
     */
    public void setModuleRepositoryURLs(ArrayList mrURLs) {
        setRepositoryURLs(mrURLs, "ModuleRepositoryURLs");
    }

    /**
     * @return
     */
    public String addModuleRepositoryURL() {
        addRepositoryURL("ModuleRepositoryURL", "ModuleRepositoryURLs");
        return null;
    }

    /**
     * @return
     */
    public String resetModuleRepositoryURL() {
        settings.put("ModuleRepositoryURL", defaultModuleRepositoryURL);
        return null;
    }

    /**
     * @return
     */
    public String removeModuleRepositoryURL() {
        removeRepositoryURL("ModuleRepositoryURL", "ModuleRepositoryURLs", defaultModuleRepositoryURL);
        return null;
    }

    /**
     * @return
     */
    public Collection getSuiteRepositoryURLs() {
        return getRepositoryURLs("SuiteRepositoryURLs");

    }

    /**
     * @param mrURLs
     */
    public void setSuiteRepositoryURLs(ArrayList mrURLs) {
        setRepositoryURLs(mrURLs, "SuiteRepositoryURLs");
    }

    /**
     * @return
     */
    public String addSuiteRepositoryURL() {
        addRepositoryURL("SuiteRepositoryURL", "SuiteRepositoryURLs");
        return null;
    }

    /**
     * @return
     */
    public String resetSuiteRepositoryURL() {
        settings.put("SuiteRepositoryURL", defaultSuiteRepositoryURL);
        return null;
    }

    /**
     * @return
     */
    public String removeSuiteRepositoryURL() {
        removeRepositoryURL("SuiteRepositoryURL", "SuiteRepositoryURLs", defaultSuiteRepositoryURL);
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
        settings.remove("http.proxyHost");
        settings.remove("http.proxyPort");
        settings.remove("http.proxyUser");

        settings.remove("ftp.proxyHost");
        settings.remove("ftp.proxyPort");
        settings.remove("ftp.proxyUser");
        proxyPassword = "";

        return null;

    }

    public String getDb() {
        String db = (String) settings.get("database.vendor");
        //resetDbParam(db);
        return db;
    }

    public void setDb(String dbName) {
        settings.put("database.vendor", dbName);
    }

    public void changeDb(ValueChangeEvent event) {
        String db = (String) settings.get("database.vendor");
        //resetDbParam(db);

    }

    private void resetDbParam(String dbName) {
        if (dbName.equals("HSQL")) {
            settings.remove("hibernate.connection.SetbigStringTryClob");
            settings.remove("hibernate.default_schema");

            settings.put("hibernate.connection.driver_class", hsqlConnectionDriverclass);
            settings.put("hibernate.connection.url", hsqlConnectionUrl);
            settings.put("hibernate.connection.username", hsqlConnectionUsername);
            settings.put("hibernate.connection.password", hsqlConnectionPassword);
            settings.put("hibernate.dialect", hsqlDialect);

            settings.put("HSQL_port", "9001");
            settings.put("HSQL.class", "org.hsqldb.Server");
            settings.put("HSQL.args", " - port 9001 -database.0 file:../resources/GenePatternDB -dbname.0 xdb");
            settings.put("HSQL.schema", "analysis_hypersonic-");
            settings.put("hibernate.connection.shutdown", "true");
        }
        else if (dbName.equals("ORACLE")) {
            settings.remove("HSQL_port");
            settings.remove("HSQL.class");
            settings.remove("HSQL.args");
            settings.remove("HSQL.schema");
            settings.remove("hibernate.connection.shutdown");
            settings.remove("HSQL_port");
            settings.remove("HSQL.class");
            settings.remove("HSQL.args");
            settings.remove("HSQL.schema");

            settings.put("hibernate.connection.driver_class", oracleConnectionDriverclass);
            settings.put("hibernate.connection.url", oracleConnectionUrl);
            settings.put("hibernate.connection.username", oracleConnectionUsername);
            settings.put("hibernate.connection.password", oracleConnectionPassword);
            settings.put("hibernate.dialect", oracleDialect);

            settings.put("hibernate.default_schema", oracleDefaultSchema);
            settings.put("hibernate.connection.SetBigStringTryClob", "true");
        }
    }
    
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

    public String getNewCSKey() {
        return newCSKey;
    }

    public String getNewCSValue() {
        return newCSValue;
    }

    public void setNewCSKey(String key) {
        newCSKey = key;
    }

    public void setNewCSValue(String value) {
        newCSValue = value;
    }

    public void addNewCustomSetting(ActionEvent event) {
        if (newCSKey != "" && newCSValue != "") {
            customSettings.add(new KeyValuePair(newCSKey, newCSValue));
        }
    }

    public void deleteCustomSetting(ActionEvent event) {
        System.out.println(getKey());
        customSettings.remove(getKey());
    }

    public List<KeyValuePair> getCustomSettings() {
        return customSettings;
    }

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
    
    public void restore(ActionEvent event) {
		String[] propertyKeys = modes.get(currentMode);
		if (propertyKeys!=null) {
			String defaultValue;
			for (String propertyKey:propertyKeys) {
				defaultValue = (String)defaultSettings.get(propertyKey);
				if (defaultValue!=null) {
					settings.put(propertyKey, defaultValue);
				}
			}
			saveSettings(event);
		}
	}
    
}
