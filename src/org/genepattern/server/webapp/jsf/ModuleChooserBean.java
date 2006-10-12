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
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

public class ModuleChooserBean {
    Logger log = Logger.getLogger(ModuleChooserBean.class);
    List<Category> categories = null;

    private String mode = "category"; // @todo - externalize or make enum
    private String selectedModule = "";

    public List<Category> getAllTasks() {
        if (categories == null) {
            categories = new ArrayList<Category>();
            categories.add(getRecentlyUsed());
            if (mode.equals("all")) {
                categories.add(getAll());
            }
            else if (mode.equals("category")) {
                for (Category cat : getTasksByCategory()) {
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

    private Category getRecentlyUsed() {
        AdminDAO dao = new AdminDAO();
        return new Category("Recently Used", dao.getLatestTasks(getUserId()));
    }

    private Category getAll() {
        AdminDAO dao = new AdminDAO();
        return new Category("All", dao.getAllTasksForUser(getUserId()));
    }

    private List<Category> getTasksByCategory() {

        List<Category> categories = new ArrayList<Category>();
        TaskInfo[] alltasks = (new AdminDAO()).getAllTasksForUser(getUserId());
        Map<String, List<TaskInfo>> taskMap = new HashMap<String, List<TaskInfo>>();

        for (int i = 0; i < alltasks.length; i++) {
            TaskInfo ti = alltasks[i];
            String taskType = ti.getTaskInfoAttributes().get("taskType");
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
            categories.add(new Category(categoryName, modules));
        }
        return categories;

    }

    public class Category {
        private boolean expanded = true;
        private String name;
        private TaskInfo[] modules;

        public Category(String name, TaskInfo[] modules) {
            this.name = name;
            this.modules = modules;
        }

        public String getName() {
            return name;
        }

        public TaskInfo[] getModules() {
            return modules;
        }

        public boolean isExpanded() {
            return expanded;
        }

        public void setExpanded(boolean exp) {
            expanded = exp;
        }

        public void toggleExpanded(ActionEvent event) {
            expanded = !expanded;
            System.out.println("toggle");
        }

    }

}
