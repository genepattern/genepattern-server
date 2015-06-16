/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webapp.jsf;

import static org.genepattern.server.webapp.jsf.UIBeanHelper.getRequest;

import java.util.ArrayList;
import java.util.List;

import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

public class ModuleChooserBean implements java.io.Serializable {
    private static final long serialVersionUID = -9026970426503039995L;
    private List<ModuleCategoryGroup> categoryGroups = null;
    private List<SelectItem> modes;
    private String selectedModule = "";
    private CollapsiblePanelState moduleChooserState;

    public ModuleChooserBean() {
        moduleChooserState = (CollapsiblePanelState) UIBeanHelper.getManagedBean("#{moduleChooserState}");
        boolean showCategoryView = Boolean.valueOf(System.getProperty("module.chooser.show.category.view", "true"));
        modes = new ArrayList<SelectItem>(3);
        if (showCategoryView) {
            modes.add(new SelectItem("category"));
        }
        boolean showSuiteView = Boolean.valueOf(System.getProperty("module.chooser.show.suite.view", "true"));
        if (showSuiteView) {
            modes.add(new SelectItem("suite"));
        }
        boolean showAllView = Boolean.valueOf(System.getProperty("module.chooser.show.all.view", "true"));
        if (showAllView) {
            modes.add(new SelectItem("all"));
        }
        if (moduleChooserState.getSelectedMode() == null) {
            moduleChooserState.setSelectedMode(modes.get(0).getLabel());
        }
    }

    public boolean isNoModulesInstalled() {
        return new ModuleHelper(true).getTasks().getModuleCount() == 0;
    }
    
    public List<ModuleCategoryGroup> getAllTasks() {
        List<ModuleCategoryGroup> returnList = new ArrayList();
        ModuleHelper helper = new ModuleHelper();
        List<ModuleCategory> tmp = new ArrayList<ModuleCategory>();
        ModuleCategory cat = helper.getTasks();
        cat.setExpanded(!moduleChooserState.isClosed(cat.getIdentifier()));
        tmp.add(cat);
        returnList.add(new ModuleCategoryGroup("all", tmp));
        return returnList;
        
    }

    public List<ModuleCategoryGroup> getTasks() {
        if (categoryGroups == null) {
            categoryGroups = new ArrayList<ModuleCategoryGroup>();
            ModuleHelper helper = new ModuleHelper();
            for (SelectItem item : modes) {
                String mode = item.getLabel();
                /* Create the "recent" pseudo category */
                List<ModuleCategory> tmp = new ArrayList<ModuleCategory>();
                ModuleCategory recent = helper.getRecentlyUsed();
                recent.setIdPrefix(mode);
                recent.setExpanded(!moduleChooserState.isClosed(recent.getIdentifier()));
                tmp.add(recent);

                if (mode.equals("all")) {
                    ModuleCategory cat = helper.getTasks();
                    cat.setExpanded(!moduleChooserState.isClosed(cat.getIdentifier()));
                    tmp.add(cat);
                } 
                else if (mode.equals("suite")) {
                    for (ModuleCategory cat : helper.getTasksBySuite()) {
                        cat.setExpanded(!moduleChooserState.isClosed(cat.getIdentifier()));
                        tmp.add(cat);
                    }
                } 
                else if (mode.equals("category")) {
                    for (ModuleCategory cat : helper.getTasksByType()) {
                        cat.setExpanded(!moduleChooserState.isClosed(cat.getIdentifier()));
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
        RunTaskBean runTaskBean = (RunTaskBean) UIBeanHelper.getManagedBean("#{runTaskBean}");
        if (runTaskBean != null) {
            runTaskBean.setTask(selectedModule);
            setVersionPrompt(runTaskBean);
        }
        EulaTaskBean eulaTaskBean = (EulaTaskBean) UIBeanHelper.getManagedBean("#{eulaTaskBean}");
        if (eulaTaskBean != null && runTaskBean != null) {
            eulaTaskBean.setCurrentLsid(selectedModule);
        }
    }
    
    public void setVersionPrompt(RunTaskBean runTaskBean) {
        String prompt = getRequest().getParameter("promptForLatestVersion");
        if ("true".equals(prompt)) {
            runTaskBean.setVersionPrompt(true);
        }
    }

    public void moduleClicked(ActionEvent event) {
        setSelectedModule(getRequest().getParameter("task"));
    }

    public String getUserId() {
        return UIBeanHelper.getUserId();
    }

    public List<SelectItem> getModes() {
        return modes;
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
        String mode = moduleChooserState.getSelectedMode();
        return (mode == null ? "category" : mode);
    }

    public void setSelectedMode(String selectedMode) {
        moduleChooserState.setSelectedMode(selectedMode);
    }

}
