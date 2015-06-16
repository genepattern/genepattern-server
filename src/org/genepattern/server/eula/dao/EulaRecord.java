/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.eula.dao;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Hibernate mapping class for recording EULA to the GP database.
 * 
 * @author pcarr
 */
@Entity
@Table(name="eula_record", uniqueConstraints = {@UniqueConstraint(columnNames={"user_id", "lsid"})})
public class EulaRecord {
    @Id
    @GeneratedValue
    private long id;

    //the GP user id
    @Column(name = "user_id")
    private String userId;

    //the lsid of the module which the user has agreed to
    @Column(name = "lsid")
    private String lsid;
    
    //the Date that the user agreed
    // initialized in this class, which is good enough
    @Column(name = "date_recorded")
    private Date dateRecorded = new Date();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLsid() {
        return lsid;
    }

    public void setLsid(String lsid) {
        this.lsid = lsid;
    }

    public Date getDateRecorded() {
        return dateRecorded;
    }

}



