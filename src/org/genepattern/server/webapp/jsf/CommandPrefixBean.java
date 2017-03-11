/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webapp.jsf;

import static org.genepattern.util.GPConstants.COMMAND_PREFIX;
import static org.genepattern.util.GPConstants.TASK_PREFIX_MAPPING;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import javax.faces.FacesException;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.genepattern.server.util.PropertiesManager_3_2;
import org.genepattern.server.webservice.server.local.IAdminClient;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

public class CommandPrefixBean {

    private IAdminClient admin;
    private String defaultCommandPrefix;
    private String newPrefixName;
    private String newPrefixValue;
    private List<String> newMappingLSID;
    private String newMappingPrefix;

    PropertiesManager_3_2 pm = null;

    public CommandPrefixBean() {
        if (!AuthorizationHelper.adminServer()) {
            throw new FacesException("You don' have the required permissions to administer the server.");
        }
        admin = new LocalAdminClient(UIBeanHelper.getUserId());
        pm = PropertiesManager_3_2.getInstance();
        defaultCommandPrefix = pm.getCommandPrefixes().getProperty("default", "");
        setDefault();
    }

    public String getDefaultCommandPrefix() {
        return defaultCommandPrefix;
    }

    public void setDefaultCommandPrefix(String defaultCommandPrefix) {
        this.defaultCommandPrefix = defaultCommandPrefix;
        if (defaultCommandPrefix == null) {
            defaultCommandPrefix = "";
        }
    }

    public void saveDefaultCommandPrefix(ActionEvent event) {
        UIBeanHelper.setInfoMessage("Property successfully updated");
        setDefault();
        System.setProperty(COMMAND_PREFIX, defaultCommandPrefix);
    }

    private void setDefault() {
        Properties p = pm.getCommandPrefixes();
        p.setProperty("default", defaultCommandPrefix);
        pm.saveProperties(COMMAND_PREFIX, p);
    }

    private static final Comparator<Map.Entry<Object, Object>> PREFIX_NAME_COMPARATOR = new Comparator<Map.Entry<Object,Object>>() {
        public int compare(Entry<Object, Object> arg0, Entry<Object, Object> arg1) {
            Object k0 = arg0 == null ? null : arg0.getKey();
            Object k1 = arg1 == null ? null : arg1.getKey();
            String s0 = k0 == null ? "" : k0 instanceof String ? (String) k0 : "";
            String s1 = k1 == null ? "" : k1 instanceof String ? (String) k1 : "";
            return s0.compareToIgnoreCase(s1);
        }
    };
    
    private static final TaskPrefixMappingComparator TASK_PREFIX_MAPPING_COMPARATOR = new TaskPrefixMappingComparator();
    private static class TaskPrefixMappingComparator implements Comparator<KeyValuePair> {
        private boolean sortByKey = true;
        public void setSortByKey(boolean b) {
            this.sortByKey = b;
        }

        public int compare(KeyValuePair arg0, KeyValuePair arg1) {
            String k0 = arg0 == null ? "" : arg0.getKey();
            String k1 = arg1 == null ? "" : arg1.getKey();
            String v0 = arg0 == null ? "" : arg0.getValue();
            String v1 = arg1 == null ? "" : arg1.getValue();
            
            //sort by key then by value
            if (sortByKey) {
                int c = k0.compareToIgnoreCase(k1);
                if (c == 0) {
                    c = v0.compareToIgnoreCase(v1);
                }
                return c;
            }
            //sort by value then by key
            else {
                int c = v0.compareToIgnoreCase(v1);
                if (c == 0) {
                    c = k0.compareToIgnoreCase(k1);
                }
                return c;
            }
        }
    };

    public List<Map.Entry<Object, Object>> getCommandPrefixes() {
        //return new ArrayList(pm.getCommandPrefixes().entrySet());
        Properties cmdPrefixes = pm.getCommandPrefixes();
        Set<Map.Entry<Object,Object>> entrySet = cmdPrefixes.entrySet();
        List<Map.Entry<Object,Object>> prefixes = new ArrayList<Map.Entry<Object,Object>>(entrySet);
        Collections.sort(prefixes,PREFIX_NAME_COMPARATOR);
        return prefixes;
    }

    public List<SelectItem> getCommandPrefixesAsSelectItems() {
        List<SelectItem> out = new ArrayList<SelectItem>();
        
        List<Map.Entry<Object,Object>> cmdPrefixes = getCommandPrefixes();
        for (Map.Entry<Object,Object> entry : cmdPrefixes) {
            String key = (String) entry.getKey();
            out.add(new SelectItem(key, key));
        }
        return out;
    }

    /**
     * We store the mapping with LSIDs (unique) but display with names (not unique).
     *
     * @return
     */
    public List<KeyValuePair> getTaskPrefixMapping() throws WebServiceException {
        List<KeyValuePair> out = new ArrayList<KeyValuePair>();
        Properties taskPrefixMapping = pm.getTaskPrefixMapping();

        // return tastPrefixMapping; by name not LSID as we really keep it
        for (Object lsidStr : taskPrefixMapping.keySet()) {
            String name = nameFromLSID((String) lsidStr);
            out.add(new KeyValuePair(name, (String) lsidStr, taskPrefixMapping.getProperty((String) lsidStr)));
        }
        //TODO: allow this to be set from web page, 
        //TASK_PREFIX_MAPPING_COMPARATOR.setSortByKey(false);
        Collections.sort(out,TASK_PREFIX_MAPPING_COMPARATOR);
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

    public List<String> getNewMappingLSID() {
        return newMappingLSID;
    }

    public void setNewMappingLSID(List<String> newMappingLSID) {
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
        final Properties p = pm.getTaskPrefixMapping();
        for (final String anLsid : newMappingLSID) {
            final String lsid = lsidFromName(anLsid);
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
