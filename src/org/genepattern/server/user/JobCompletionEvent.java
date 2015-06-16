/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.user;

import java.util.Date;

import org.genepattern.server.domain.JobStatus;
import org.genepattern.webservice.JobInfo;

public class JobCompletionEvent {

    Integer id;
    String userId;
    
    /**
     * Recognized values are TASK and PIPELINE
     */
    String type;
    
    int jobNumber;
    int parentJobNumber;
    String taskLsid;
    String taskName;
    Date completionDate;
    String completionStatus;
    long elapsedTime;

    public JobCompletionEvent() {

    }

    public String getCompletionStatus() {
        return completionStatus;
    }

    public void setCompletionStatus(String completionStatus) {
        this.completionStatus = completionStatus;
    }

    public Date getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(Date completionDate) {
        this.completionDate = completionDate;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(long elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getJobNumber() {
        return jobNumber;
    }

    public void setJobNumber(int jobNumber) {
        this.jobNumber = jobNumber;
    }

    public int getParentJobNumber() {
        return parentJobNumber;
    }

    public void setParentJobNumber(int parentJobNumber) {
        this.parentJobNumber = parentJobNumber;
    }

    public String getTaskLsid() {
        return taskLsid;
    }

    public void setTaskLsid(String taskLSID) {
        this.taskLsid = taskLSID;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userID) {
        this.userId = userID;
    }

}
