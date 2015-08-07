/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.sge;

import java.util.Date;

/**
 * Hibernate mapping class.
 * @author pcarr
 *
 */
public class JobSge {
    private int gpJobNo = -1;
    
    private String sgeJobId = null;
    private Date sgeSubmitTime = null;
    private Date sgeStartTime = null;
    private Date sgeEndTime = null;
    private int sgeReturnCode = 0;
    private String sgeJobCompletionStatus = null;
    
    public JobSge() {
    }

    public int getGpJobNo() {
        return gpJobNo;
    }
    public void setGpJobNo(int gpJobNo) {
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

    public int getSgeReturnCode() {
        return sgeReturnCode;
    }
    public void setSgeReturnCode(int sgeReturnCode) {
        this.sgeReturnCode = sgeReturnCode;
    }

    public String getSgeJobCompletionStatus() {
        return sgeJobCompletionStatus;
    }
    public void setSgeJobCompletionStatus(String sgeJobCompletionStatus) {
        this.sgeJobCompletionStatus = sgeJobCompletionStatus;
    }

}
