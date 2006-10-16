package org.genepattern.server.webapp.jsf;

import javax.faces.event.ActionEvent;

import org.genepattern.webservice.TaskInfo;

public class ModuleCategory {
    private boolean expanded = true;
    private String name;
    private TaskInfo[] modules;

    public ModuleCategory(String name, TaskInfo[] modules) {
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
