/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.dm.jobinput;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.log4j.Logger;

@Entity
@Table(name="job_input", uniqueConstraints = {@UniqueConstraint(columnNames={"job_id", "name"})})
public class JobInput {
    @SuppressWarnings("unused")
    private final static Logger log = Logger.getLogger(JobInput.class);
    
    @Id
    @GeneratedValue
    private long id;
    @Column(name = "job_id")
    private int job;
    private String name;
    @Column(name = "user_value")
    private String userValue;
    @Column(name = "cmd_value")
    private String commandValue;
    private String kind;
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public int getJob() {
        return job;
    }
    
    public void setJob(int job) {
        this.job = job;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getUserValue() {
        return userValue;
    }
    
    public void setUserValue(String userValue) {
        this.userValue = userValue;
    }
    
    public String getCommandValue() {
        return commandValue;
    }
    
    public void setCommandValue(String commandValue) {
        this.commandValue = commandValue;
    }
    
    public String getKind() {
        return kind;
    }
    
    public void setKind(String kind) {
        this.kind = kind;
    }
}
