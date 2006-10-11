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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

public class ModuleChooserBean {
    Logger log = Logger.getLogger(ModuleChooserBean.class);
    List<Category> categories = null;

    private String mode = "all"; // @todo - externalize or make enum
    private String selectedModule = "";

    public List<Category> getAllTasks() {
        tasksByCatgory();
        if (categories == null) {
            categories = new ArrayList<Category>();
            categories.add(getRecentlyUsed());
            categories.add(getAll());
        }
        return categories;
    }
    
    public List<Category> getTaskByCategory() {
        
        if (categories == null) {
            categories = new ArrayList<Category>();
            categories.add(getRecentlyUsed());
            categories.add(getAll());
        }
        return categories;
    }

    public List<Category> getTaskBySuite() {
        
        if (categories == null) {
            categories = new ArrayList<Category>();
            categories.add(getRecentlyUsed());
            categories.add(getAll());
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

    public void modeChanged(ActionEvent event) {
        // @todo -- reload page contents for new mode
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
    
    private void tasksByCatgory() {
        
        TaskInfo[] alltasks = (new AdminDAO()).getAllTasksForUser(getUserId());
        Map<String, List<TaskInfo>> taskMap = new HashMap<String, List<TaskInfo>>();
        
        for(int i=0; i<alltasks.length; i++) {
            TaskInfo ti = alltasks[i];
            String taskType = ti.getTaskInfoAttributes().get("taskType");
            List<TaskInfo> tasks = taskMap.get(taskType);
            if(tasks == null) {
                tasks = new ArrayList<TaskInfo>();
                taskMap.put(taskType, tasks);
            }
            tasks.add(ti);
        }
        
        List<String> categories = new ArrayList(taskMap.keySet());
        
        
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
