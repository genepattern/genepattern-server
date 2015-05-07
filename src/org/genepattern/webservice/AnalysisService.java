/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.webservice;

import org.genepattern.util.GPConstants;

/**
 * Holds a <tt>TaskInfo</tt> object and the server on which the task is
 * located.
 * 
 * @author Hui Gong
 * @version $Revision 1.2 $
 */

public class AnalysisService {
	private String server;

	private TaskInfo task;

	/**
	 * Creates a new <tt>AnalysisService</tt> instance
	 * 
	 * @param server
	 *            the server, for example http://127.0.0.1:8080
	 * @param task
	 *            the task info object for the service
	 */
	public AnalysisService(String server, TaskInfo task) {
		this.server = server;
		this.task = task;
	}

	/**
	 * Returns a string representation of this <tt>AnalysisService</tt>
	 * instance
	 * 
	 * @return the string representation
	 */
	public String toString() {
		return this.task.getName() + " (" + this.server + ")";
	}

	/**
	 * Gets the server on which this <tt>AnalysisService</tt> instance is
	 * located.
	 * 
	 * @return The server
	 */
	public String getServer() {
		return this.server;
	}

	/**
	 * Gets the LSID for this <tt>AnalysisService</tt> instance
	 * 
	 * @return the lsid
	 */
	public String getLsid() {
		return (String) task.getTaskInfoAttributes().get(GPConstants.LSID);
	}

	/**
	 * Gets the name of this <tt>AnalysisService</tt> instance
	 * 
	 * @return the task name
	 */
	public String getName() {
		return task.getName();
	}

	/**
	 * Gets the <tt>TaskInfo</tt> object for this <tt>AnalysisService</tt>
	 * instance
	 * 
	 * @return The task info
	 */
	public TaskInfo getTaskInfo() {
		return this.task;
	}
}
