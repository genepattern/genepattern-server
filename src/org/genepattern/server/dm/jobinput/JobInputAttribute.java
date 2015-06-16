/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.dm.jobinput;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="job_input_attribute")
public class JobInputAttribute {
    
    @Id
    @GeneratedValue
    private long id;
    @Column(name = "input_id")
    private long inputId;
    private String name;
    private String val;
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public long getInputId() {
        return inputId;
    }
    
    public void setInputId(long input_id) {
        this.inputId = input_id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getVal() {
        return val;
    }
    
    public void setVal(String value) {
        this.val = value;
    }
    
}
