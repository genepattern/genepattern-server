package org.genepattern.webservice;

/**
 * Used to hold information about particular Task
 * 
 * @author Rajesh Kuttan
 * @version 1.0
 */

import java.io.*;
import java.util.Map;

public class TaskInfo implements Serializable {

	private int taskID = 0;

	private String taskName = "", description = "",
			parameter_info = "";

	private int accessId = 0;

	private String userId = null;

	// JL
	private Map taskInfoAttributes = null;

	private ParameterInfo[] parameterInfoArray = null;

	/** Creates new TaskInfo */
	public TaskInfo() {
	}

	public TaskInfo(int taskID, String taskName, String description,
			String parameter_info) {
		this(taskID, taskName, description, parameter_info, null);
	}

	public TaskInfo(int taskID, String taskName, String description,
			String parameter_info, TaskInfoAttributes taskInfoAttributes) {
		this(taskID, taskName, description, parameter_info,
				taskInfoAttributes, null, 0);
	}

	public TaskInfo(int taskID, String taskName, String description,
			String parameter_info,
			TaskInfoAttributes taskInfoAttributes, String userId, int accessId) {
		this.taskID = taskID;
		this.taskName = taskName;
		this.description = description;
		this.parameter_info = parameter_info;
		this.taskInfoAttributes = taskInfoAttributes;
		this.userId = userId;
		this.accessId = accessId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUserId() {
		return this.userId;
	}

	public void setAccessId(int accessId) {
		this.accessId = accessId;
	}

	public int getAccessId() {
		return this.accessId;
	}

	public Map getTaskInfoAttributes() {
		// JL
		return (Map) taskInfoAttributes;
	}

	public TaskInfoAttributes giveTaskInfoAttributes() {
		// JL
		if (taskInfoAttributes == null)
			taskInfoAttributes = new TaskInfoAttributes();
		return (TaskInfoAttributes) taskInfoAttributes;
	}

	public void setTaskInfoAttributes(Map taskInfoAttributes) {
		try {
			TaskInfoAttributes tia = (TaskInfoAttributes) taskInfoAttributes;
			this.taskInfoAttributes = tia;
		} catch (ClassCastException cce) {
			this.taskInfoAttributes = new TaskInfoAttributes(taskInfoAttributes);
		}

	}

	public String getName() {
		return taskName;
	}

	public void setName(java.lang.String taskName) {
		this.taskName = taskName;
	}

	public int getID() {
		return taskID;
	}

	public void setID(int taskID) {
		this.taskID = taskID;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(java.lang.String description) {
		this.description = description;
	}

	/**
	 * get parameter info jaxb string
	 * 
	 * @return parameter_info
	 */
	public String getParameterInfo() {
		return parameter_info;
	}

	/**
	 * get <CODE>ParameterInfo</CODE> array
	 *  
	 */
	public void setParameterInfo(java.lang.String parameter_info) {
		this.parameter_info = parameter_info;
	}

	/**
	 * get <CODE>ParameterInfo</CODE> array
	 * 
	 * @return parameterInfoArray
	 */
	public ParameterInfo[] getParameterInfoArray() {
		//add checking in here --Hui
		if (this.parameterInfoArray == null && this.parameter_info != null) {
			ParameterFormatConverter converter = new ParameterFormatConverter();
			try {
				this.parameterInfoArray = converter
						.getParameterInfoArray(this.parameter_info);
			} catch (OmnigeneException e) {
			}
		}
		return this.parameterInfoArray;
	}

	/**
	 * set <CODE>ParameterInfo</CODE> array
	 * 
	 * @param parameterInfoArray
	 */
	public void setParameterInfoArray(ParameterInfo[] parameterInfoArray) {
		this.parameterInfoArray = parameterInfoArray;
	}

	

	/**
	 * Checks to see if the TaslInfo contains a input file parameter field
	 * 
	 * @return true if it contains a
	 *         <code>ParamterInfo<code> object with TYPE as FILE and MODE as INPUT
	 */
	public boolean containsInputFileParam() {
		//add convinent checking method --Hui
		if (this.parameterInfoArray != null) {
			for (int i = 0; i < this.parameterInfoArray.length; i++) {
				if (this.parameterInfoArray[i].isInputFile()) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean equals(Object otherThing) {
		if (!(otherThing instanceof TaskInfo) || otherThing == null)
			return false;
		TaskInfo other = (TaskInfo) otherThing;
		return getUserId().equals(other.getUserId())
				&& getAccessId() == other.getAccessId()
				&& ((getTaskInfoAttributes() == null && other
						.getTaskInfoAttributes() == null) || (getTaskInfoAttributes() != null
						&& other.getTaskInfoAttributes() != null && getTaskInfoAttributes()
						.equals(other.getTaskInfoAttributes())))
				&& getName().equals(other.getName())
				&& getID() == other.getID()
				&& getDescription().equals(other.getDescription())
				&& ((getParameterInfo() == null && other.getParameterInfo() == null) || (getParameterInfo() != null
						&& other.getParameterInfo() != null && getParameterInfo()
						.equals(other.getParameterInfo())));
	}
}