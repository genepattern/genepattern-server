/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.data.pipeline;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Vector;

import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

public class JobSubmission implements Serializable {

	protected String taskName = null;

	protected String taskDescription = null;

	protected String lsid = null;

	protected Vector pia = new Vector();

	protected boolean runTimePrompt[] = null;

	protected boolean isVisualizer = false;

	protected TaskInfo taskInfo = null;

	public JobSubmission(String taskName, String taskDescription, String lsid,
			ParameterInfo[] parameterInfoArray, boolean[] runTimePrompt,
			boolean isVisualizer, TaskInfo taskInfo) {
		setName(taskName);
		setDescription(taskDescription);
		setLSID(lsid);
		setParameters(parameterInfoArray);
		setRuntimePrompt(runTimePrompt);
		setVisualizer(isVisualizer);
		setTaskInfo(taskInfo);
	}

	public JobSubmission() {
	}

	public void setName(String name) {
		this.taskName = name;
	}

	public String getName() {
		return this.taskName;
	}

	public void setDescription(String description) {
		this.taskDescription = description;
	}

	public String getDescription() {
		return this.taskDescription;
	}

	public void setLSID(String lsid) {
		this.lsid = lsid;
	}

	public String getLSID() {
		return this.lsid;
	}

	// convert a Map of parameter name/value pairs into a ParameterInfo[]
	public void addParameter(String name, String value) {
		pia.add(new ParameterInfo(name, value, ""));
	}

	public void addParameter(ParameterInfo pi) {
		pia.add(pi);
	}

	public void setParameters(ParameterInfo[] parameterInfoArray) {
		if (parameterInfoArray != null) {
			pia.addAll(Arrays.asList(parameterInfoArray));
		}
	}

	// return Vector of Parameters
	public Vector getParameters() {
		return pia;
	}

	public ParameterInfo[] giveParameterInfoArray() {
		return (ParameterInfo[]) pia.toArray(new ParameterInfo[0]);
	}

	public void setRuntimePrompt(boolean[] runTimePrompt) {
		this.runTimePrompt = runTimePrompt;
	}

	public boolean[] getRuntimePrompt() {
		return runTimePrompt;
	}

	public void setVisualizer(boolean bVisualizer) {
		this.isVisualizer = bVisualizer;
	}

	public boolean isVisualizer() {
		return isVisualizer;
	}

	public void setTaskInfo(TaskInfo taskInfo) {
		this.taskInfo = taskInfo;
	}

	public TaskInfo getTaskInfo() {
		return taskInfo;
	}
}
