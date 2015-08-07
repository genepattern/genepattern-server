/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.domain;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class BatchJob {
	public static final String BATCH_KEY = "Batch2478925018:";
	private Integer jobNo;    
    private Set<AnalysisJob> batchJobs = new HashSet<AnalysisJob>();
    private Date submittedDate;
    private boolean deleted;
    private String userId;
    
    //No arg constructor for Hibernate.
    public BatchJob(){
    	
    }
    public BatchJob(String userId){
    	this.userId = userId;
    	deleted = false;
    	setSubmittedDate(Calendar.getInstance().getTime());
    }
    
	private void setJobNo(Integer jobNo) {
		this.jobNo = jobNo;
	}
	public Integer getJobNo() {
		return jobNo;
	}
	
	public void setBatchJobs(Set<AnalysisJob> batchJobs) {
		this.batchJobs = batchJobs;
	}
	public Set<AnalysisJob> getBatchJobs() {
		return batchJobs;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUserId() {
		return userId;
	}
	public void setSubmittedDate(Date submittedDate) {
		this.submittedDate = submittedDate;
	}
	public Date getSubmittedDate() {
		return submittedDate;
	}

}
