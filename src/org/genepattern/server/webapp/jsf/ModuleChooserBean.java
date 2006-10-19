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

import org.apache.log4j.Logger;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

public class ModuleChooserBean {
    Logger log = Logger.getLogger(ModuleChooserBean.class);
    List<ModuleCategory> categories = null;

    private String mode = "category"; // @todo - externalize or make enum
    private String selectedModule = "";

    public List<ModuleCategory> getAllTasks() {
        if (categories == null) {
            categories = new ArrayList<ModuleCategory>();
            categories.add(getRecentlyUsed());
            if (mode.equals("all")) {
                categories.add(getAll());
            }
            else if (mode.equals("category")) {
                for (ModuleCategory cat : getTasksByType()) {
                    categories.add(cat);
                }
            }
        }
        return categories;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getSelectedModule() {
        return selectedModule;
    }

    public void setSelectedModule(String selectedModule) {
        this.selectedModule = selectedModule;
    }

    public void modeChanged(ValueChangeEvent  event) {
        categories = null;
    }

    public void moduleClicked(ActionEvent event) {
        setSelectedModule(getRequest().getParameter("task"));
    }

    public String getUserId() {
        return UIBeanHelper.getUserId();
    }

    private ModuleCategory getRecentlyUsed() {
        AdminDAO dao = new AdminDAO();
        return new ModuleCategory("Recently Used", dao.getRecentlyRunTasksForUser(getUserId()));
    }

    private ModuleCategory getAll() {
        AdminDAO dao = new AdminDAO();
        return new ModuleCategory("All", dao.getAllTasksForUser(getUserId()));
    }

    private List<ModuleCategory> getTasksByType() {

        List<ModuleCategory> categories = new ArrayList<ModuleCategory>();
        TaskInfo[] alltasks = (new AdminDAO()).getAllTasksForUser(getUserId());
        Map<String, List<TaskInfo>> taskMap = new HashMap<String, List<TaskInfo>>();

        for (int i = 0; i < alltasks.length; i++) {
            TaskInfo ti = alltasks[i];
            String taskType = ti.getTaskInfoAttributes().get("taskType");
            if(taskType == null || taskType.length() == 0) {
                taskType = "Uncategorized";
            }
            List<TaskInfo> tasks = taskMap.get(taskType);
            if (tasks == null) {
                tasks = new ArrayList<TaskInfo>();
                taskMap.put(taskType, tasks);
            }
            tasks.add(ti);
        }

        List<String> categoryNames = new ArrayList(taskMap.keySet());
        Collections.sort(categoryNames);
        for (String categoryName : categoryNames) {
            TaskInfo[] modules = new TaskInfo[taskMap.get(categoryName).size()];
            modules = taskMap.get(categoryName).toArray(modules);
            categories.add(new ModuleCategory(categoryName, modules));
        }
        return categories;

    }
    
    private String getVersion(TaskInfo ti) {
        
        try {
            LSID lsid = new LSID(ti.getLsid());
            return lsid.getVersion();
        }
        catch (MalformedURLException e) {
            log.error("Bad LSID", e);
            throw new RuntimeException(e);
        }
        
    }



}
