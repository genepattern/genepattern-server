/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webapp.jsf;

import javax.faces.event.ActionEvent;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;

public class ModuleCategory implements java.io.Serializable {
    static Logger log = Logger.getLogger(ModuleCategory.class);

    private boolean expanded = true;

    /**
     * Optional prefix used to qualify the identifier. The need for this arose to create a unique identifier for the
     * "recently used" category since it appears three times on the page.
     */
    private String idPrefix = "";

    private List<Module> modules;

    private String name;

    private String id;

    public ModuleCategory(String name, TaskInfo[] taskInfos) {
	this(name, taskInfos, true);
    }

    public ModuleCategory(String name, TaskInfo[] taskInfos, boolean selectLatestVersion) {
	this(name, taskInfos, selectLatestVersion, name);
    }

    public ModuleCategory(String name, TaskInfo[] taskInfos, boolean selectLatestVersion, String id) {
	this.name = name;
	this.id = id.replace(' ', '_').replace('&', '-').trim();
	HashMap<String, Module> tmp = new HashMap<String, Module>();
	for (TaskInfo ti : taskInfos) {
	    try {
		LSID lsid = new LSID(ti.getLsid());
		Module module = tmp.get(ti.getName());
		if (module == null) {
		    module = new Module(ti, lsid);
		    module.setSelectedVersion(selectLatestVersion ? "" : lsid.getVersion());
		    tmp.put(ti.getName(), module);
		} else {
		    module.addVersion(lsid);
		}
	    } catch (MalformedURLException e) {
		log.error("Malformed lsid: " + ti.getLsid(), e);
	    }
	}
	modules = new ArrayList<Module>(tmp.values());
	Collections.sort(modules, new Comparator<Module>() {
	    public int compare(Module o1, Module o2) {
		String n1 = o1.getName();
		String n2 = o2.getName();
		return n1.compareToIgnoreCase(n2);
	    }

	});
    }

    public String getIdentifier() {
	return idPrefix + id;
    }

    public int getModuleCount() {
	return (modules == null ? 0 : modules.size());
    }

    public List<Module> getModules() {
	return modules;
    }

    public String getName() {
	return name;
    }

    public boolean isExpanded() {
	return expanded;
    }

    public void setExpanded(boolean exp) {
	expanded = exp;
    }

    public void setIdPrefix(String idPrefix) {
	this.idPrefix = idPrefix;
    }

    public void setSelected(List<String> selectedLsids) {
	for (String selectedLsid : selectedLsids) {
	    LSID lsidObj = null;
	    try {
		lsidObj = new LSID(selectedLsid);
	    } catch (MalformedURLException e) {
		log.error("Error parsing lsid: " + selectedLsid, e);
		continue;
	    }

	    for (Module module : modules) {
		if (selectedLsid.contains(module.getLSID().toStringNoVersion())) {
		    module.setSelected(true);
		    module.setSelectedVersion(lsidObj.getVersion());
		    break;
		}
	    }
	}
    }

    public void setSelectedVersionOfModules(Map<String, TaskInfo> lsidToTaskInfoMap) {
	LSID lsidObj = null;
	for (Module module : modules) {
	    for (Map.Entry<String, TaskInfo> entry : lsidToTaskInfoMap.entrySet()) {
		if (entry.getKey().contains(module.getLSID().toStringNoVersion())) {
		    try {
			lsidObj = new LSID(entry.getKey());
		    } catch (MalformedURLException e) {
			log.error("Error parsing lsid: " + entry.getKey(), e);
		    }
		    String version = (lsidObj != null) ? lsidObj.getVersion() : "";
		    if (!version.equals("")) {
			module.setSelectedVersion(version);
		    }
		    break;
		}
	    }
	}
    }

    public void toggleExpanded(ActionEvent event) {
	expanded = !expanded;
    }

}
