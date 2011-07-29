package org.genepattern.server.executor.sge;

import java.util.Date;

/**
 * Hibernate mapping class.
 * @author pcarr
 *
 */
public class JobSge {
    private long gpJobNo;
    
    private String sgeJobId;
    private Date sgeSubmitTime;
    private Date sgeStartTime;
    private Date sgeEndTime;
    private long sgeReturnCode;
    private String sgeJobCompletionStatus;

    
    
    public JobSge() {
    }

    public long getGpJobNo() {
        return gpJobNo;
    }
    public void setGpJobNo(long gpJobNo) {
        this.gpJobNo = gpJobNo;
    }

    public String getSgeJobId() {
        return sgeJobId;
    }
    public void setSgeJobId(String sgeJobId) {
        this.sgeJobId = sgeJobId;
    }

    public Date getSgeSubmitTime() {
        return sgeSubmitTime;
    }
    public void setSgeSubmitTime(Date sgeSubmitTime) {
        this.sgeSubmitTime = sgeSubmitTime;
    }

    public Date getSgeStartTime() {
        return sgeStartTime;
    }
    public void setSgeStartTime(Date sgeStartTime) {
        this.sgeStartTime = sgeStartTime;
    }

    public Date getSgeEndTime() {
        return sgeEndTime;
    }
    public void setSgeEndTime(Date sgeEndTime) {
        this.sgeEndTime = sgeEndTime;
    }

    public long getSgeReturnCode() {
        return sgeReturnCode;
    }
    public void setSgeReturnCode(long sgeReturnCode) {
        this.sgeReturnCode = sgeReturnCode;
    }

    public String getSgeJobCompletionStatus() {
        return sgeJobCompletionStatus;
    }
    public void setSgeJobCompletionStatus(String sgeJobCompletionStatus) {
        this.sgeJobCompletionStatus = sgeJobCompletionStatus;
    }

}
