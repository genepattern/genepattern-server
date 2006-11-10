/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp.jsf;

import static org.genepattern.server.webapp.jsf.UIBeanHelper.getRequest;
import static org.genepattern.server.webapp.jsf.UIBeanHelper.getUserId;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;
import java.util.*;

public class ModuleChooserBean implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -9026970426503039995L;
	private static Logger log = Logger.getLogger(ModuleChooserBean.class);

	private List<ModuleCategoryGroup> categoryGroups = null;
	private Set<String> closedCategories = new HashSet<String>();
	private String[] modes = { "category", "suite", "all" };
	private String selectedMode;
	private String selectedModule = "";

	public List<ModuleCategoryGroup> getTasks() {
		if (categoryGroups == null) {
			categoryGroups = new ArrayList<ModuleCategoryGroup>();

			ModuleHelper helper = new ModuleHelper();
			for (String mode : modes) {
				List<ModuleCategory> tmp = new ArrayList<ModuleCategory>();
				tmp.add(helper.getRecentlyUsed());
				if (mode.equals("all")) {
					ModuleCategory cat = helper.getAllTasks();
					if( closedCategories.contains(cat.getName())) {
						cat.setExpanded(false);
					}
					tmp.add(cat);
				} else if (mode.equals("suite")) {
					for (ModuleCategory cat : helper.getTasksBySuite()) {
						if( closedCategories.contains(cat.getName())) {
							cat.setExpanded(false);
						}
						tmp.add(cat);
					}
				} else if (mode.equals("category")) {
					for (ModuleCategory cat : helper.getTasksByType()) {
						if( closedCategories.contains(cat.getName())) {
							cat.setExpanded(false);
						}
						tmp.add(cat);
					}
				}
				categoryGroups.add(new ModuleCategoryGroup(mode, tmp));
			}
		}
		return categoryGroups;
	}

	public String getSelectedModule() {
		return selectedModule;
	}

	public void setSelectedModule(String selectedModule) {
		this.selectedModule = selectedModule;
		RunTaskBean runTaskBean = (RunTaskBean) UIBeanHelper
				.getManagedBean("#{runTaskBean}");
		if (runTaskBean != null) {
			runTaskBean.setTask(selectedModule);
		}
	}

	public void modeChanged(ValueChangeEvent event) {
		selectedMode = (String) event.getNewValue();
	}

	public void moduleClicked(ActionEvent event) {
		String expStatePrefix = "expansion_state_";
		Map parameterMap = getRequest().getParameterMap();
		for (Object paramKey : parameterMap.keySet()) {
			String paramName = (String) paramKey;
			if (paramName.startsWith(expStatePrefix)) {
				String categoryName = paramName.substring(expStatePrefix.length());
				if(parameterMap.get(paramKey).equals("false")) {
					closedCategories.add(categoryName);
				}
			}
		}
		setSelectedModule(getRequest().getParameter("task"));
	}

	public String getUserId() {
		return UIBeanHelper.getUserId();
	}

	private String getVersion(TaskInfo ti) {

		try {
			LSID lsid = new LSID(ti.getLsid());
			return lsid.getVersion();
		} catch (MalformedURLException e) {
			log.error("Bad LSID", e);
			throw new RuntimeException(e);
		}

	}

	public String[] getModes() {
		return modes;
	}

	public void setModes(String[] modes) {
		this.modes = modes;
	}

	public class ModuleCategoryGroup implements java.io.Serializable {
		String mode;

		List<ModuleCategory> categories;

		public ModuleCategoryGroup(String mode, List<ModuleCategory> categories) {
			this.mode = mode;
			this.categories = categories;
		}

		public List<ModuleCategory> getCategories() {
			return categories;
		}

		public String getMode() {
			return mode;
		}

	}

	public String getSelectedMode() {
		if (selectedMode == null) {
			selectedMode = "category";
		}
		return selectedMode;
	}

	public void setSelectedMode(String selectedMode) {

		this.selectedMode = selectedMode;
	}

}
