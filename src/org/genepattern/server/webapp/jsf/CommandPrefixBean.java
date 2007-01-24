package org.genepattern.server.webapp.jsf;

import static org.genepattern.util.GPConstants.COMMAND_PREFIX;
import static org.genepattern.util.GPConstants.TASK_PREFIX_MAPPING;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.faces.FacesException;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;
import org.genepattern.server.util.AuthorizationManagerFactory;
import org.genepattern.server.util.IAuthorizationManager;
import org.genepattern.server.util.PropertiesManager;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

public class CommandPrefixBean {

    private static Logger log = Logger.getLogger(CommandPrefixBean.class);

    private LocalAdminClient admin;

    private String defaultCommandPrefix;

    private String newPrefixName;

    private String newPrefixValue;

    private List newMappingLSID;

    private String newMappingPrefix;

    PropertiesManager pm = null;

    public CommandPrefixBean() {
        IAuthorizationManager authManager = AuthorizationManagerFactory.getAuthorizationManager();
        if (!authManager.checkPermission("administrateServer", UIBeanHelper.getUserId())) {
            throw new FacesException("You don' have the required permissions to administer the server.");
        }
        admin = new LocalAdminClient(UIBeanHelper.getUserId());
        pm = PropertiesManager.getInstance();
        defaultCommandPrefix = System.getProperty(COMMAND_PREFIX, "");
        setDefault();
    }

    public String getDefaultCommandPrefix() {
        return defaultCommandPrefix;
    }

    public void setDefaultCommandPrefix(String defaultCommandPrefix) {
        this.defaultCommandPrefix = defaultCommandPrefix;
        if(defaultCommandPrefix==null) {
            defaultCommandPrefix = "";
        }
    }
    
    public void saveDefaultCommandPrefix(ActionEvent event) {
    	setDefault();
        System.setProperty(COMMAND_PREFIX, defaultCommandPrefix);
    }
    
    private void setDefault() {
    	Properties p = pm.getCommandPrefixes();
    	p.setProperty("default", defaultCommandPrefix);
    	pm.saveProperties(COMMAND_PREFIX, p);
    }

    public List getCommandPrefixes() {
        return new ArrayList(pm.getCommandPrefixes().entrySet());
    }

    public List<SelectItem> getCommandPrefixesAsSelectItems() {
        ArrayList out = new ArrayList<SelectItem>();
        for (Iterator iter = pm.getCommandPrefixes().keySet().iterator(); iter.hasNext();) {
            String key = (String) iter.next();
            out.add(new SelectItem(key, key));
        }
        return out;
    }

    /**
     * We store the mapping with LSIDs (unique) but display with names (not
     * unique)
     * 
     * @return
     */
    public List getTaskPrefixMapping() throws WebServiceException {
        ArrayList out = new ArrayList();
        Properties taskPrefixMapping = pm.getTaskPrefixMapping();

        // return tastPrefixMapping; by name not LSID as we really keep it
        for (Object lsidStr : taskPrefixMapping.keySet()) {
            String name = nameFromLSID((String) lsidStr);
            out.add(new KeyValuePair(name, (String) lsidStr, taskPrefixMapping.getProperty((String) lsidStr)));
        }
        return out;
    }

    public String getNewPrefixName() {
        return newPrefixName;
    }

    public void setNewPrefixName(String newPrefix) {
        this.newPrefixName = newPrefix;
    }

    public String getNewPrefixValue() {
        return newPrefixValue;
    }

    public void setNewPrefixValue(String newPrefix) {
        this.newPrefixValue = newPrefix;
    }

    public List getNewMappingLSID() {
        return newMappingLSID;
    }

    public void setNewMappingLSID(List newMappingLSID) {
        this.newMappingLSID = newMappingLSID;
    }

    public String getNewMappingPrefix() {
        return newMappingPrefix;
    }

    public void setNewMappingPrefix(String newMappingPrefix) {
        this.newMappingPrefix = newMappingPrefix;
    }

    /**
     * Save a new set of prefixes for use in mappings
     * 
     * @param event --
     *            ignored
     */
    public void saveCommandPrefixes(ActionEvent event) {
        Properties p = pm.getCommandPrefixes();
        p.setProperty(newPrefixName, newPrefixValue);
        pm.saveProperties(COMMAND_PREFIX, p);
    }

    public void deletePrefix(ActionEvent event) {
        String prefixToDelete = getKey("aPrefixKey");
        Properties cp = pm.getCommandPrefixes();
        cp.remove(prefixToDelete);
        pm.saveProperties(COMMAND_PREFIX, cp);

        Properties tpm = pm.getTaskPrefixMapping();
        boolean tpmChanged = false;
        for (Object oKey : tpm.keySet()) {
            String key = (String) oKey;
            String prefixInUse = tpm.getProperty(key);
            if (prefixInUse.equals(prefixToDelete)) {
                tpm.remove(key);
                tpmChanged = true;
            }
        }
        if (tpmChanged)
            pm.saveProperties(TASK_PREFIX_MAPPING, tpm);
    }

    private String getKey(String keyName) {
        Map params = UIBeanHelper.getExternalContext().getRequestParameterMap();
        String key = (String) params.get(keyName);
        return key;
    }

    /**
     * Save the mappings between a task and a prefix
     * 
     * @param event --
     *            ignored
     */
    public void savePrefixTaskMapping(ActionEvent event) throws MalformedURLException, WebServiceException {
        Properties p = pm.getTaskPrefixMapping();

        for (String anLsid : (List<String>) newMappingLSID) {
            String lsid = lsidFromName(anLsid);

            p.setProperty(lsid, newMappingPrefix);
        }
        pm.saveProperties(TASK_PREFIX_MAPPING, p);
    }

    public void deleteTaskPrefixMapping(ActionEvent event) throws MalformedURLException, WebServiceException {
        String k = lsidFromName(getKey("aPrefixMapKey"));
        Properties p = pm.getTaskPrefixMapping();

        p.remove(k);
        pm.saveProperties(TASK_PREFIX_MAPPING, p);

    }

    protected String nameFromLSID(String lsid) throws WebServiceException {

        TaskInfo task = admin.getTask(lsid);
        if (task != null)
            return task.getName();
        else
            return lsid;

    }

    protected String lsidFromName(String name) throws MalformedURLException, WebServiceException {

        TaskInfo task = admin.getTask(name);
        if (task != null) {
            LSID lsid = new LSID((String) task.getTaskInfoAttributes().get("LSID"));
            return lsid.toStringNoVersion();
        } else {
            return name;
        }
    }

}
