package org.genepattern.server.domain;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.server.auth.JobGroup;

public class AnalysisJobArchive {

    private static Logger log = Logger.getLogger(AnalysisJobArchive.class);

    private Integer jobNo;
    private JobStatus jobStatus;
    private int taskId;
    private Date submittedDate;
    private Date completedDate;
    private String userId;
    private Integer accessId;
    private String jobName;
    private String taskLsid;
    private String taskName;
    private Integer parent;
    private boolean deleted;
    private String parameterInfo;
    
    private Set<JobGroup> permissions = new HashSet<JobGroup>();

    /**
     * auto generated
     * 
     * @es_generated
     */
    public AnalysisJobArchive() {
        super();
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public AnalysisJobArchive(Integer jobNo, Boolean deleted) {
        super();
        this.jobNo = jobNo;
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
    public JobStatus getStatus() {
        return this.jobStatus;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public void setStatus(JobStatus value) {
        //log.debug("\tSetting status on " + this.getJobNo() +" to " + value.getStatusName());
        this.jobStatus = value;
    }

    

    public JobStatus getJobStatus() {
        return getStatus();
    }

    public void setJobStatus(JobStatus jobStatus) {
        setStatus(jobStatus);
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
    
    public Set<JobGroup> getPermissions() {
        return permissions;
    }
    
    public void setPermissions(Set<JobGroup> permissions) {
        this.permissions = permissions;
    }

}

