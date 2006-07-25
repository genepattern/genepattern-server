/* Auto generated file */

package org.genepattern.server.webservice.server.dao;

import java.util.Date;

import org.genepattern.webservice.ParameterInfo;

public class AnalysisJob {


    private Integer jobNo;
    private JobStatus jobStatus;
    private int taskId;
    private Date submittedDate;
    private Date completedDate;
    private String parameterInfo;
    private String userId;
    private Integer accessId;
    private String jobName;
    private String lsid;
    private String taskLsid;
    private String taskName;
    private Integer parent;
    private boolean deleted;

    /**
     * auto generated
     * 
     * @es_generated
     */
    public AnalysisJob() {
        super();
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public AnalysisJob(Integer jobNo, Boolean deleted) {
        super();
        this.jobNo = jobNo;
        this.deleted = deleted;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public AnalysisJob(Integer jobNo, JobStatus statusId, Integer taskId, Date dateSubmitted, Date dateCompleted,
            String parameterInfo, String userId, Integer accessId, String jobName, String lsid,
            String taskLsid, String taskName, Integer parent, Boolean deleted) {
        super();
        this.jobNo = jobNo;
        this.jobStatus = statusId;
        this.taskId = taskId;
        this.submittedDate = dateSubmitted;
        this.completedDate = dateCompleted;
        this.parameterInfo = parameterInfo;
        this.userId = userId;
        this.accessId = accessId;
        this.jobName = jobName;
        this.lsid = lsid;
        this.taskLsid = taskLsid;
        this.taskName = taskName;
        this.parent = parent;
        this.deleted = deleted;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public Integer getJobNo() {
        return this.jobNo;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public void setJobNo(Integer value) {
        this.jobNo = value;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public JobStatus getStatusId() {
        return this.jobStatus;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public void setStatusId(JobStatus value) {
        this.jobStatus = value;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public int getTaskId() {
        return this.taskId;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public void setTaskId(int value) {
        this.taskId = value;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public Date getSubmittedDate() {
        return this.submittedDate;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public void setSubmittedDate(Date value) {
        this.submittedDate = value;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public Date getCompletedDate() {
        return this.completedDate;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public void setCompletedDate(Date value) {
        this.completedDate = value;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public String getParameterInfo() {
        return this.parameterInfo;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public void setParameterInfo(String value) {
        this.parameterInfo = value;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public String getUserId() {
        return this.userId;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public void setUserId(String value) {
        this.userId = value;
    }



    /**
     * auto generated
     * 
     * @es_generated
     */
    public Integer getAccessId() {
        return this.accessId;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public void setAccessId(Integer value) {
        this.accessId = value;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public String getJobName() {
        return this.jobName;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public void setJobName(String value) {
        this.jobName = value;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public String getLsid() {
        return this.lsid;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public void setLsid(String value) {
        this.lsid = value;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public String getTaskLsid() {
        return this.taskLsid;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public void setTaskLsid(String value) {
        this.taskLsid = value;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public String getTaskName() {
        return this.taskName;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public void setTaskName(String value) {
        this.taskName = value;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public Integer getParent() {
        return this.parent;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public void setParent(Integer value) {
        this.parent = value;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public Boolean getDeleted() {
        return this.deleted;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public void setDeleted(Boolean value) {
        this.deleted = value;
    }

    
    public JobStatus getJobStatus() {
        return jobStatus;
    }

    
    public void setJobStatus(JobStatus jobStatus) {
        this.jobStatus = jobStatus;
    }
}
