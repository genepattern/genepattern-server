package org.genepattern.server.webservice.server;

/**
 * @author Joshua Gould
 * @created July 21, 2004
 */
public class JobResult implements java.io.Serializable {
	String jobNumber;

	long dateSubmitted;

	long dateCompleted;

	String userName;

	int status;

	String taskName;

	String[] outputFileNames;

	Boolean[] outputFileExists;

	// Vector subResults;

	/*
	 * public Vector getSubResults() { return subResults; }
	 * 
	 * public void addSubResult(JobResult js) { if(subResults==null) {
	 * subResults = new Vector(); } subResults.add(js); }
	 * 
	 * public void getSubResults(Vector subResults) { this.subResults =
	 * subResults; }
	 */

	public void setOutputFileNames(String[] outputFileNames) {
		this.outputFileNames = outputFileNames;
	}

	public String[] getOutputFileNames() {
		return outputFileNames;
	}

	public void setOutputFileExists(Boolean[] outputFileExists) {
		this.outputFileExists = outputFileExists;
	}

	public Boolean[] getOutputFileExists() {
		return outputFileExists;
	}

	public void setJobNumber(String i) {
		jobNumber = i;
	}

	public String getJobNumber() {
		return jobNumber;
	}

	public long getDateSubmitted() {
		return dateSubmitted;
	}

	public void setDateSubmitted(long l) {
		dateSubmitted = l;
	}

	public long getDateCompleted() {
		return dateCompleted;
	}

	public void setDateCompleted(long l) {
		dateCompleted = l;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String s) {
		userName = s;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int i) {
		status = i;
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String s) {
		taskName = s;
	}

}