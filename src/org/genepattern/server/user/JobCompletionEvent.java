package org.genepattern.server.user;

import java.util.Date;

import org.genepattern.webservice.JobInfo;

public class JobCompletionEvent {

    Integer id;
    String userID;
    String type;
    int jobNumber;
    int parentJobNumber;
    String taskLsid;
    String taskName;
    Date completionDate;
    long elapsedTime;

    public JobCompletionEvent() {

    }

    public JobCompletionEvent(JobInfo jobInfo, JobInfo parentJobInfo, Date completionDate, long elapsedTime) {
        userID = jobInfo.getUserId();
        // type

        jobNumber = jobInfo.getJobNumber();
        if (parentJobInfo != null) {
            parentJobNumber = parentJobInfo.getJobNumber();
        }
        taskLsid = jobInfo.getTaskLSID();
        taskName = jobInfo.getTaskName();
        this.completionDate = completionDate;
        this.elapsedTime = elapsedTime;

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

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

}
