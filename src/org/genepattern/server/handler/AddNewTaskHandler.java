/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.handler;

import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterFormatConverter;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * AddNewTaskHandler to submit a job request and get back <CODE>TaskInfo
 * </CODE>
 * 
 * @author rajesh kuttan
 * @version 1.0
 */

public class AddNewTaskHandler extends RequestHandler {
    private static Logger log = Logger.getLogger(AddNewTaskHandler.class);

    private String taskName = "";
	private String description = "";
	private String parameter_info = "";
	private String taskInfoAttributes = null;
	private ParameterInfo[] parameterInfoArray = null;
	private String userId;
	private int accessId;

	/** Creates new GetAvailableTaskHandler */
	public AddNewTaskHandler() {
		super();
	}

	/**
	 * Constructor with Task parameters
	 * 
	 * @param taskName
	 * @param description
	 * @param parameterInfoArray
	 * @param className
	 */
	public AddNewTaskHandler(String userId, int accessId, String taskName,
			String description, ParameterInfo[] parameterInfoArray, String taskInfoAttributes) {
		this.userId = userId;
		this.accessId = accessId;
		this.taskName = taskName;
		this.description = description;
		this.parameterInfoArray = parameterInfoArray;
		this.taskInfoAttributes = taskInfoAttributes;
	}

	/**
	 * Adds new Task and returns <CODE>TaskInfo</CODE>
	 * 
	 * @throws OmnigeneException
	 * @return taskID
	 */
	public int executeRequest() throws OmnigeneException {
		int taskID = 0;
		try {
            AdminDAO ds = new AdminDAO();

			GetAvailableTasksHandler th = new GetAvailableTasksHandler();
			List vTasks = th.executeRequest();
			if (vTasks != null) {
				TaskInfo taskInfo = null;
				for (int i = 0; i < vTasks.size(); i++) {
					taskInfo = (TaskInfo) vTasks.get(i);
					if (taskInfo.getName().equalsIgnoreCase(this.taskName)) {
						taskID = taskInfo.getID();
						log.info("updating existing task ID: " + taskID);
						ds.deleteTask(taskID);
						break;
					}
				}
			}

			parameter_info = ParameterFormatConverter.getJaxbString(parameterInfoArray);
			taskID = (new AnalysisDAO()).addNewTask(taskName, userId, accessId, description, parameter_info, taskInfoAttributes);
		} 
		catch (Exception ex) {
			log.error("AddNewTaskRequest(execute): Error " + ex.getMessage(), ex);
			throw new OmnigeneException(ex.getMessage());
		}
		return taskID;
	}
}
