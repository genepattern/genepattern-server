/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2013) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.task.category.dao;

import java.io.Serializable;

import javax.persistence.Embeddable;

@Embeddable
public class TaskCategoryPk implements Serializable {
    private String task;
    private String category;
    
    public TaskCategoryPk() {
    }
    public String getTask() {
        return task;
    }
    public void setTask(String task) {
        this.task=task;
    }
    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category=category;
    }
    
    public int hashCode() {
        final String tmp=task+"_"+category;
        return tmp.hashCode();
    }
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj==null) {
            return false;
        }
        if (!(obj instanceof TaskCategoryPk)) {
            return false;
        }
        TaskCategoryPk arg=(TaskCategoryPk)obj;
        if (task==null) {
            if (arg.task != null) {
                return false;
            }
        }
        if (!task.equals(arg.task)) {
            return false;
        }
        if (category==null) {
            return arg.category==null;            
        }
        return category.equals(arg.category);
    }
}
