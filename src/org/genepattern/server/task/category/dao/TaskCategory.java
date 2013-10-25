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

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

/**
 * Hibernate mapping class for associating a module with zero or more categories.
 * 
 * @author pcarr
 */
@Entity
@Table(name="task_category")
@IdClass(TaskCategoryPk.class)
public class TaskCategory implements Serializable {
    private String task;
    private String category;
    
    public TaskCategory() {
    }
    public TaskCategory(final String task, final String category) {
        this.task=task;
        this.category=category;
    }

    @Id
    public String getTask() {
        return task;
    }
    public void setTask(final String task) {
        this.task=task;
    }
    
    @Id
    public String getCategory() {
        return category;
    }
    public void setCategory(final String category) {
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
        TaskCategory arg=(TaskCategory)obj;
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

