/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/

/**
 *
 */
package org.genepattern.server.webapp.jsf;

import static org.genepattern.server.webapp.jsf.UIBeanHelper.getRequest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Vector;

import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.dm.userupload.dao.UserUploadDao;
import org.genepattern.server.purger.PurgerFactory;
import org.genepattern.server.quota.DiskInfo;
import org.genepattern.server.util.FastReverseLineInputStream;
import org.genepattern.server.util.PropertiesManager_3_2;

import com.google.common.base.Strings;



public class ServerSettingsBean implements Serializable {
    private static Logger log = Logger.getLogger("ServerSettingsBean.class");

    private Map<String, String[]> modes;
    private String[] clientModes = new String[] { "Local", "Any", "Specified" };
    private String currentClientMode = clientModes[0]; // default
    private String specifiedClientMode;
    private String currentMode; // Default
    private Properties settings;
    private CustomProperties customProperties;
    private Properties defaultSettings;
    private String genepatternURL = "";
    private String newCSKey = "";
    private String newCSValue = "";
    private Calendar cal = Calendar.getInstance();
    private Integer logFileDisplaySize = 10;
    
    /**
     * 
     */
    public ServerSettingsBean() {
        if (!AuthorizationHelper.adminServer()) {
            throw new SecurityException("You don't have the required permissions to administer the server.");
        }

        if (modes == null) {
            modes = new TreeMap<String, String[]>();
            modes.put("Access", new String[] { "gp.allowed.clients", "gp.blacklisted.clients" });
            modes.put("Command Line Prefix", null);
            modes.put("Disk Quota", new String[] { "gp.allowed.clients" });
            modes.put("File Purge", new String[] { "purgeJobsAfter", "purgeTime" });
            modes.put("Gene Pattern Log", null);
            modes.put("Web Server Log", null);
            modes.put("Repositories", new String[] { 
                    "ModuleRepositoryURL", "ModuleRepositoryURLs", "SuiteRepositoryURL", "SuiteRepositoryURLs" });
            modes.put("Proxy", new String[] { 
                    "http.proxyHost", "http.proxyPort", "http.proxyUser","http.proxyPassword", "ftp.proxyHost", "ftp.proxyPort", "ftp.proxyUser", "ftp.proxyPassword" });
            modes.put("Programming Languages", new String[] { "java", "R2.5", "R" });
            //modes.put("Advanced", new String[] { 
            //        "num.threads" 
            // });
            modes.put("Custom", null);
            modes.put("Shut Down Server", null);
            modes.put("System Message", null);
            modes.put("Users and Groups", null);
            modes.put("Uploaded Files", null);
            modes.put("Job Configuration", null);
            modes.put("Task Info", null);
        }
        currentMode = (String) modes.keySet().toArray()[0];
        if (settings == null) {
            try {
                settings = PropertiesManager_3_2.getGenePatternProperties();
            } 
            catch (IOException ioe) {
                log.error(ioe);
                settings = new Properties();
            }
        }
        if (customProperties == null) {
            customProperties = new CustomProperties();
        }
        if (defaultSettings == null) {
            try {
                defaultSettings = PropertiesManager_3_2.getDefaultProperties();
            } 
            catch (IOException ioe) {
                log.debug(ioe);
                defaultSettings=new Properties();
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
     * Method to support live debugging of bean
     */
    public String getModesAsString() {
        String msg = "Server modes: ";
        for (Object m : getModes()) {
            msg += m.toString() + " ";
        }
        return msg;
    }

    /**
     * @param evt
     */
    public void modeChanged(ActionEvent evt) {
        setCurrentMode(getRequest().getParameter("mode"));
    }

    /**
     * Return the properties object with the server settings. This should be lazy initialized, it might be called many
     * times but we only need to read the file once.
     * 
     */
    public Properties getSettings() {
        return settings;
    }

    /**
     * @return
     */
    public String getCurrentClientMode() {
        currentClientMode = settings.getProperty("gp.allowed.clients");

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
            specifiedClientMode = null;
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
        specifiedClientMode = mode;
    }

    /**
     * @return
     */
    public List<SelectItem> getSpecifiedClientModes() {
        return getSelectItems("gp.allowed.clients");
    }

    /**
     * @param mode
     */
    public void setSpecifiedClientModes(List<SelectItem> clientModes) {
        setSelectItems(clientModes, "gp.allowed.clients");
    }

    /**
     * @param event
     */
    public void addSpecifiedClientMode(ActionEvent event) {
        if (clientModes[2].equals(currentClientMode)) {
            removeDomain(clientModes[0]);
            removeDomain(clientModes[1]);
            String allClientModes = settings.getProperty("gp.allowed.clients");
            String[] result = allClientModes.split(",");
            // avoid adding duplicated domains.
            boolean exist = false;
            for (int i = 0; i < result.length; i++) {
                if (result[i] != null && result[i].equals(specifiedClientMode)) {
                    exist = true;
                    break;
                }
            }
            if (!exist && allClientModes.length() > 0) {
                allClientModes = allClientModes.concat(",").concat(specifiedClientMode);
            } 
            else if (allClientModes.length() == 0) {
                allClientModes = specifiedClientMode;
            }
            settings.put("gp.allowed.clients", allClientModes);
        }
        saveSettings(event);
    }

    /**
     * @param event
     */
    public void removeSpecifiedClientMode(ActionEvent event) {
        event.getComponent();
        removeDomain(specifiedClientMode);
        saveSettings(event);
    }

    private void removeDomain(String mode) {
        String allClientModes = settings.getProperty("gp.allowed.clients");
        String[] result = allClientModes.split(",");
        StringBuffer newClientModes = new StringBuffer();
        for (int i = 0; i < result.length; i++) {
            if (result[i] != null && !result[i].equals(mode)) {
                newClientModes.append(result[i]).append(",");
            }
        }
        String newClientModesStr = (newClientModes.length() > 0) ? newClientModes.substring(0,
                newClientModes.length() - 1) : "";
        settings.put("gp.allowed.clients", newClientModesStr);
    }

    /**
     * @param logFile
     * @return
     * @throws IOException
     */
    public static String getLog(File logFile, Integer len) {

        StringBuffer buf = new StringBuffer();
        try {

            if (logFile != null && logFile.exists()) {
                Path logAsPath = Paths.get(logFile.getAbsolutePath());
                tailFile(logAsPath,len, buf);
            }
        } catch (IOException exc) {
            log.error(exc);
        }
        
        return buf.toString();
        
    }
    static public String getEntireLog(File logFile) {
        StringBuffer buf = new StringBuffer();
        BufferedReader br = null;

        try {

            if (logFile != null && logFile.exists()) {
                br = new BufferedReader(new FileReader(logFile));
                String thisLine = "";

                while ((thisLine = br.readLine()) != null) { // while loop
                    // begins here
                    buf.append(thisLine).append("\n");
                } // end while
            }
        } catch (IOException exc) {
            log.error(exc);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } 
                catch (IOException e) {
                    log.error("Error", e);
                }
            }
        }
        return buf.toString();
    }
    
    
    /**
     * Adapted from https://roytuts.com/read-last-n-lines-from-a-file/
     * @param source
     * @param noOfLines
     * @param buf
     * @throws IOException
     */
    public static final void tailFile(final Path source, final int noOfLines, StringBuffer buf) throws IOException {
        ArrayList<String> reversedLines = new ArrayList<String>(noOfLines);
        FastReverseLineInputStream stream = new FastReverseLineInputStream(source.toFile(), noOfLines);
        BufferedReader in = new BufferedReader (new InputStreamReader (stream));
        int count = 0;
        while(count <= noOfLines) {
            count++;
            String line = in.readLine();
            if (line != null) reversedLines.add(line);
            else if (line == null) {
                break;
            }
            
        }
        for (int i = reversedLines.size()-1; i >= 0; i--){
            buf.append(reversedLines.get(i));
            buf.append("\n");
        }
    }

   
    
    /**
     * @return
     * @throws IOException
     */
    public  String getWsLog() {
        File wsLogFile = getWsLogFile();
        String out = "";
        try {
            out = getLog(wsLogFile, logFileDisplaySize);
        }
        catch (Exception e) {
            out = e.getLocalizedMessage();
        }
        return out;
    }

    /**
     * @return
     * @throws IOException
     */
    public String getGpLog() {
        return getLog(getGpLogFile(), this.logFileDisplaySize);
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
     * @param logFile
     * @param name
     * @return
     */
    private String getLogHeader(File logFile, String name) {
        StringBuffer buf = new StringBuffer();
        if ((logFile == null || !logFile.exists())) {
            buf.append("Log, ");
            buf.append(logFile.getAbsolutePath());
            buf.append(" not found.");
        } 
        else {
            buf.append(name + " log file, ");
            buf.append(logFile.getAbsolutePath());
            buf.append(" from ");
            buf.append(UIBeanHelper.getServer() + " on ");
            buf.append(cal.getTime());

        }
        return buf.toString();
    }

    /**
     * @return
     */
    private File getGpLogFile()
    {
        return ServerConfigurationFactory.instance().getGPLogFile(GpContext.getServerContext());
    }

    /**
     * @return
     */
    private static File getWsLogFile()
    {
        return ServerConfigurationFactory.instance().getWsLogFile(GpContext.getServerContext());
    }

    /**
     * @param repositoryName
     * @return
     */
    private List<SelectItem> getSelectItems(String commaSeparatedValue) {
        String selectItems = settings.getProperty(commaSeparatedValue);
        if (selectItems == null) {
            return Collections.emptyList();
        }
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
    private void setSelectItems(List<SelectItem> values, String Name) {
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
        String currentRepositoryURL = settings.getProperty(currentRepositoryName);
        String repositoryURLs = settings.getProperty(repositoryNames);
        if (repositoryURLs == null) {
            if (currentRepositoryURL != null)
                settings.put(repositoryNames, currentRepositoryURL);
            return;
        }
        
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

    }

    /**
     * @param currentRepositoryName
     * @param repositoryNames
     * @param defaultName
     */
    private void removeRepositoryURL(String currentRepositoryName, String repositoryNames, String defaultName) {
        String currentRepositoryURL = settings.getProperty(currentRepositoryName);
        String repositoryURLs = settings.getProperty(repositoryNames);
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
    public List<SelectItem> getModuleRepositoryURLs() {
        addRepositoryURL("ModuleRepositoryURL", "ModuleRepositoryURLs");
        return getSelectItems("ModuleRepositoryURLs");
    }

    /**
     * @param mrURLs
     */
    public void setModuleRepositoryURLs(List<SelectItem> mrURLs) {
        setSelectItems(mrURLs, "ModuleRepositoryURLs");
    }

    /**
     * @return
     */
    public void addModuleRepositoryURL(ActionEvent event) {
        addRepositoryURL("ModuleRepositoryURL", "ModuleRepositoryURLs");
        saveSettings(null);
    }

    /**
     * @return
     */
    public void removeModuleRepositoryURL(ActionEvent event) {
        String defaultModuleRepositoryURL = (String) defaultSettings.get("DefaultModuleRepositoryURL");
        removeRepositoryURL("ModuleRepositoryURL", "ModuleRepositoryURLs", defaultModuleRepositoryURL);
        settings.put("ModuleRepositoryURL", defaultModuleRepositoryURL);
    }

    /**
     * @return
     */
    public List<SelectItem> getSuiteRepositoryURLs() {
        addRepositoryURL("SuiteRepositoryURL", "SuiteRepositoryURLs");
        return getSelectItems("SuiteRepositoryURLs");
    }

    /**
     * @param mrURLs
     */
    public void setSuiteRepositoryURLs(List<SelectItem> mrURLs) {
        setSelectItems(mrURLs, "SuiteRepositoryURLs");
    }

    /**
     * @return
     */
    public void addSuiteRepositoryURL(ActionEvent event) {
        addRepositoryURL("SuiteRepositoryURL", "SuiteRepositoryURLs");
        saveSettings(null);
    }

    /**
     * @return
     */
    public void removeSuiteRepositoryURL(ActionEvent event) {
        String defaultSuiteRepositoryURL = (String) defaultSettings.get("DefaultSuiteRepositoryURL");
        removeRepositoryURL("SuiteRepositoryURL", "SuiteRepositoryURLs", defaultSuiteRepositoryURL);
        settings.put("SuiteRepositoryURL", defaultSuiteRepositoryURL);
    }

    
    public void setBlacklist(String blacklist) {
        settings.put("gp.blacklisted.clients", blacklist);
    }

    public String getBlacklist() {
        return settings.getProperty("gp.blacklisted.clients");
    }

    
    /**
     * @return
     */
    public void setProxyHost(String host) {
        settings.put("http.proxyHost", host);
        settings.put("ftp.proxyHost", host);
    }

    public String getProxyHost() {
        return settings.getProperty("http.proxyHost");
    }

    public void setProxyPort(String port) {
        settings.put("http.proxyPort", port);
        settings.put("ftp.proxyPort", port);
    }

    public String getProxyPort() {
        return settings.getProperty("http.proxyPort");
    }

    public void setProxyUser(String user) {
        settings.put("http.proxyUser", user);
        settings.put("ftp.proxyUser", user);
    }

    public String getProxyUser() {
        return settings.getProperty("http.proxyUser");
    }

    /**
     * @return
     */
    public String getProxyPassword() {
        return System.getProperty("http.proxyPassword");
    }

    /**
     * @param password
     */
    public void setProxyPassword(String password) {
        System.setProperty("http.proxyPassword", password);
        System.setProperty("ftp.proxyPassword", password);
    }

    /**
     * @return
     */
    public String getDb() {
        return settings.getProperty("database.vendor");
    }

    /**
     * @param dbName
     */
    public void setDb(String dbName) {
        settings.put("database.vendor", dbName);
    }

    /**
     * @return
     */
    public String getClobRadio() {
        String value = settings.getProperty("hibernate.connection.SetBigStringTryClob");
        return (value == null) ? "" : value;
    }

    /**
     * @param mode
     */
    public void setClobRadio(String mode) {
        if (mode != null && (mode.equals("true") || mode.equals("false"))) {
            settings.put("hibernate.connection.SetBigStringTryClob", mode);
        }
    }

    public String getDefaultSchema() {
        String value = settings.getProperty("hibernate.default_schema");
        return (value == null) ? "" : value;
    }

    public void setDefaultSchema(String defaultSchema) {
        if (defaultSchema != null && !defaultSchema.equals("")) {
            settings.put("hibernate.default_schema", defaultSchema);
        }
    }

    public String getHibernatePassword() {
        String value = settings.getProperty("hibernate.connection.password");
        return (value == null) ? "" : value;
    }

    public void setHibernatePassword(String hibernatePassword) {
        if (hibernatePassword != null && !hibernatePassword.equals("")) {
            settings.put("hibernate.connection.password", hibernatePassword);
        }
    }

    /**
     * @return
     */
    public String getShutDownRadio() {
        String value = settings.getProperty("hibernate.connection.shutdown");
        return (value == null) ? "" : value;
    }

    /**
     * @param mode
     */
    public void setShutDownRadio(String mode) {
        if (mode != null && (mode.equals("true") || mode.equals("false"))) {
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
    public void setGenepatternURL(String genepatternURL) {
        this.genepatternURL = genepatternURL;
    }

    /**
     * @return
     */
    public String getGenepatternURL() {
        return genepatternURL;
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
    public void saveNewCustomSetting(ActionEvent event) {
        if (newCSKey != "" && newCSValue != null) {
            //if this is a GP_URL set using add custom new property
            if(newCSKey.equals(CustomProperties.GP_URL) && !Strings.isNullOrEmpty(newCSValue)) {
                genepatternURL = newCSValue;
                saveGenePatternURL(event);
            }
            else {
                customProperties.addCustomSetting(newCSKey, newCSValue);
            }
            newCSKey = "";
            newCSValue = "";
        }
    }

    /**
     * @param event
     */
    public void deleteCustomSetting(ActionEvent event) {
        String keyToRemove = getKey();
        customProperties.deleteCustomSetting(keyToRemove);
    }

    /**
     * @param event
     */
    public void saveGenePatternURL(ActionEvent event) {
        if (!genepatternURL.equals("")) {
            customProperties.saveGenePatternURL(genepatternURL);
            genepatternURL = "";
        }
    }

    /**
     * @return
     */
    public List<KeyValuePair> getCustomSettings() {
        return customProperties.getCustomSettings();
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
     * Save the settings back to the file. Trigger by the "submit" button on the page.
     * 
     * @return
     */
    public void saveSettings(ActionEvent event) {
        UIBeanHelper.setInfoMessage("Property successfully updated");
        PropertiesManager_3_2.storeChanges(settings);
        //force reload of genepattern.properties and custom.properties files
        final boolean reloadConfiguration=true;
        customProperties.storeChangesToCustomProperties(reloadConfiguration);
    }

    /**
     * @param event
     */
    public void restore(ActionEvent event) {
        final String[] propertyKeys = modes.get(currentMode);
        final String subtype = currentMode.equals("Repositories") ? 
                (String) event.getComponent().getAttributes().get("subtype") : "";

        if (propertyKeys != null) {
            String defaultValue;
            Vector<String> keysToRemove = new Vector<String>();
            for (String propertyKey : propertyKeys) {
                if (subtype.equals("Module") && !propertyKey.contains(subtype)) {
                    continue;
                } 
                else if (subtype.equals("Suite") && !propertyKey.contains(subtype)) {
                    continue;
                }
                defaultValue = (String) defaultSettings.get(propertyKey);
                if (defaultValue != null) {
                    settings.put(propertyKey, defaultValue);
                } 
                else {
                    settings.remove(propertyKey);
                    keysToRemove.add(propertyKey);
                }
            }
            if (keysToRemove.size() > 0) {
                PropertiesManager_3_2.removeProperties(keysToRemove);
            }
            this.saveSettings(event);
        }
    }

    public Integer getLogFileDisplaySize() {
        return this.logFileDisplaySize;
    }

    public void setLogFileDisplaySize(Integer size) {
        logFileDisplaySize = size;
    }
    
    
    public String getPurgeJobsAfter() {
        return settings.getProperty("purgeJobsAfter");
    }

    public void setPurgeJobsAfter(String purgeJobsAfter) {
        settings.setProperty("purgeJobsAfter", purgeJobsAfter);
    }

    public String getPurgeTime() {
        return settings.getProperty("purgeTime");
    }

    public void setPurgeTime(String purgeTime) {
        settings.setProperty("purgeTime", purgeTime);
    }

    public void savePurgeSettings(ActionEvent event) {
        saveSettings(event);
        PurgerFactory.instance().restart();
    }

    public List<DiskInfoBean> getDiskInfos() {
        List<DiskInfoBean> diskInfoBeans=new ArrayList<DiskInfoBean>();
        for(final DiskInfo diskInfo :  new UserUploadDao().allDiskInfo()) {
            if (diskInfo != null) {
                diskInfoBeans.add(new DiskInfoBean(diskInfo));
            }
        }
        return diskInfoBeans;
    }
}
