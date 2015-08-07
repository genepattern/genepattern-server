/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.server.process;

import java.net.MalformedURLException;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.util.LSID;

public class MissingTaskError extends Exception {
	public static final String[] errorTypes={"missing task", "wrong version", "wrong task"};
	private String parentTaskName;
	private String taskName;
	private LSID taskLSID;
	private String errorType = errorTypes[0];
	
	private String availableVersion;

	public MissingTaskError(String parentTaskName, String taskName, String taskLsid) {
		this.parentTaskName = parentTaskName;
		this.taskName = taskName;
		try {
			this.taskLSID = new LSID(taskLsid);
		}catch (MalformedURLException e) {
	        System.out.println(e.getStackTrace());
	    }
	}

	public void setErrorType(String type) {
		this.errorType=type;
	}
	
	public String getErrorType() {
		return errorType;
	}
	
	public String getParentTaskName() {
		return parentTaskName;
	}
	
	public String getTaskName() {
		return taskName;
	}
	
	public LSID getTaskLSID() {
		return taskLSID;
	}
	
	public void setAvailableVersion(String availableVersion) {
		this.availableVersion = availableVersion;
	}
	
	public String getAvailableVersion() {
		if (errorTypes[1].equals(errorType)) {
			return availableVersion;
		}
		return null;
	}

}
