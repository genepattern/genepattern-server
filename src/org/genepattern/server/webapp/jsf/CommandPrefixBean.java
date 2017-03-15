/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webapp.jsf;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.faces.FacesException;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.genepattern.server.config.CommandPrefixConfig;
import org.genepattern.server.webservice.server.local.IAdminClient;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

/**
 * Backing bean for the Server Settings | Command Line Prefix page
 * 
 * @author pcarr
 */
public class CommandPrefixBean { 
    private IAdminClient _admin;
    private IAdminClient getAdmin() {
        if (_admin == null) {
            _admin = new LocalAdminClient(UIBeanHelper.getUserId());
        }
        return _admin;
    }

    private CommandPrefixConfig _cfg;
    private CommandPrefixConfig cfg() {
        if (_cfg==null) {
            _cfg = new CommandPrefixConfig();
        }
        return _cfg;
    }
    private void resetCfg() {
        this._cfg=null;
    }

    // for the 'save default' link
    private String _defaultCommandPrefix;
    
    // for the 'add prefix' link
    private String newPrefixName;
    private String newPrefixValue;
    
    // for the 'For Modules ...' GUI
    private List<String> selectedForModuleNames;
    // for the '... Use Prefix ...' GUI
    private String selectedCommandPrefixName;

    public CommandPrefixBean() {
        final String userId=UIBeanHelper.getUserId();
        final boolean isAdmin = AuthorizationHelper.adminServer(userId);
        if (!isAdmin) {
            throw new FacesException("You don't have the required permissions to administer the server."); 
        }
        this._defaultCommandPrefix=cfg().getDefaultCommandPrefix();
    }

    protected String getKey(String keyName) {
        final Map<String, String> params = UIBeanHelper.getExternalContext().getRequestParameterMap();
        return params.get(keyName);
    }

    protected String nameFromLSID(final IAdminClient admin, final String lsid) throws WebServiceException {
        final TaskInfo task = admin.getTask(lsid);
        if (task != null) {
            return task.getName();
        }
        else {
            return lsid;
        }
    }

    protected String lsidFromName(final IAdminClient admin, final String name) throws MalformedURLException, WebServiceException {
        final TaskInfo task = admin.getTask(name);
        if (task != null) {
            LSID lsid = new LSID((String) task.getTaskInfoAttributes().get("LSID"));
            return lsid.toStringNoVersion();
        } 
        else {
            return name;
        }
    }

    public String getDefaultCommandPrefix() {
        return cfg().getDefaultCommandPrefix();
    }

    public void setDefaultCommandPrefix(final String defaultCommandPrefix) {
        this._defaultCommandPrefix = Strings.nullToEmpty(defaultCommandPrefix);
    }

    public ImmutableList<KeyValuePair> getCommandPrefixes() {
        final Properties cmdPrefixes = cfg().getCommandPrefixProperties();
        final ImmutableList<KeyValuePair> prefixes = KeyValuePair.listFromProperties(cmdPrefixes);
        return prefixes;
    }

    public List<SelectItem> getCommandPrefixesAsSelectItems() {
        List<SelectItem> out = new ArrayList<SelectItem>();
        List<KeyValuePair> cmdPrefixes = getCommandPrefixes();
        for (KeyValuePair entry : cmdPrefixes) {
            String key = entry.getKey();
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
        final IAdminClient admin=getAdmin();
        final Properties taskPrefixMapping = cfg().getTaskPrefixMappingProperties();

        // return taskPrefixMapping; by name not LSID as we really keep it
        final SortedSet<KeyValuePair> s = new TreeSet<KeyValuePair>(KeyValuePair.sortByKeyValueIgnoreCase);
        
        for(final String lsidStr : taskPrefixMapping.stringPropertyNames()) {
            final String name = nameFromLSID(admin, lsidStr);
            final String value = taskPrefixMapping.getProperty(lsidStr);
            s.add(new KeyValuePair(name, lsidStr, value));
        }
        return ImmutableList.copyOf( s );
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
        return selectedForModuleNames;
    }

    public void setNewMappingLSID(List<String> newMappingLSID) {
        this.selectedForModuleNames = newMappingLSID;
    }

    public String getNewMappingPrefix() {
        return selectedCommandPrefixName;
    }

    public void setNewMappingPrefix(String newMappingPrefix) {
        this.selectedCommandPrefixName = newMappingPrefix;
    }
    
    /*
     * ====================
     * Actions handlers 
     * ====================
     */

    /** handle 'save default' link */
    public void saveDefaultCommandPrefix(final ActionEvent event) {
        UIBeanHelper.setInfoMessage("Property successfully updated");
        cfg().saveDefaultCommandPrefix(this._defaultCommandPrefix);
        resetCfg();
    }

    /**
     * handle 'add prefix' link.
     * 
     * Save {newPrefixName}={newPrefixValue} into the commandPrefix.properties file.
     */
    public void addPrefix(final ActionEvent event) {
        cfg().addCommandPrefix(newPrefixName, newPrefixValue);
        resetCfg();
    }
    
    /** 
     * handle 'delete' prefix link 
     * 
     * Remove {aPrefixKey} from the commandPrefix.properties file.
     * Make sure to clean up taskPrefixMapping.properties file as needed.
     */
    public void deletePrefix(final ActionEvent event) {
        final String prefixToDelete = getKey("aPrefixKey");
        cfg().deleteCommandPrefix(prefixToDelete);
        resetCfg();
    }

    /**
     * handle 'add mapping' link.
     * 
     * Save selection to taskPrefixMapping.properties file, of the form
     *     baseLsid={selectedCommandPrefixName}
     * 
     * The {selectForModuleNames} property is a list of selected module names.
     */
    public void addTaskPrefixMapping(final ActionEvent event) throws MalformedURLException, WebServiceException {
        final IAdminClient admin = getAdmin();
        List<String> baseLsids=new ArrayList<String>();
        for(final String fromName : selectedForModuleNames) {
            final String lsid = lsidFromName(admin, fromName);
            baseLsids.add(lsid);
        }
        cfg().addTaskPrefixMapping(baseLsids, selectedCommandPrefixName);
        resetCfg();
    }

    /** 
     * handle 'delete' mapping link. 
     * 
     * Delete {aPrefixMapKey} from the taskPrefixMapping.properties file.
     */
    public void deleteTaskPrefixMapping(ActionEvent event) throws MalformedURLException, WebServiceException {
        final IAdminClient admin = getAdmin();
        final String baseLsid = lsidFromName(admin, getKey("aPrefixMapKey"));
        cfg().deleteTaskPrefixMapping(baseLsid);
        resetCfg();
    }

}
