/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.jobqueue;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;


@Entity
@Table(name="job_queue", uniqueConstraints = {@UniqueConstraint(columnNames={"job_no"})})
public class JobQueue {

    public enum Status {
        WAITING,
        PENDING,
        DISPATCHING
    }

    @Id
    @Column(name = "job_no")
    private Integer jobNo;
    
    @Column(name = "parent_job_no")
    private Integer parentJobNo = -1;
    
    @Column(name = "status")
    private String status;

    @Column(name = "date_submitted")
    private Date dateSubmitted;

    public int getJobNo() {
        return jobNo;
    }

    public void setJobNo(int jobNo) {
        this.jobNo = jobNo;
    }
    
    public int getParentJobNo() {
        return parentJobNo;
    }
    public void setParentJobNo(int i) {
        this.parentJobNo = i;
    }
    
    public String getStatus() {
        return this.status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public Date getDatSubmitted() {
        return dateSubmitted;
    }

    public void setDateSubmitted(Date date) {
        this.dateSubmitted = date;
    }
}
