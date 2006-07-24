/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/


package org.genepattern.server.handler;

import java.util.List;
import java.util.Vector;

import org.genepattern.server.AnalysisManager;
import org.genepattern.server.webservice.server.AnalysisJobDataSource;
import org.genepattern.server.webservice.server.dao.AdminDataService;
import org.genepattern.server.webservice.server.dao.AnalysisDataService;
import org.genepattern.server.util.BeanReference;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterFormatConverter;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

//import edu.mit.wi.omnigene.omnidas.*;

/**
 * AddNewTaskHandler to submit a job request and get back <CODE>TaskInfo
 * </CODE>
 * 
 * @author rajesh kuttan
 * @version 1.0
 */

public class AddNewTaskHandler extends RequestHandler {

	private String taskName = "", description = "", parameter_info = "", taskInfoAttributes = null;

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

			//Get EJB reference
			AnalysisManager analysisManager = AnalysisManager.getInstance();

			GetAvailableTasksHandler th = new GetAvailableTasksHandler();
			List vTasks = th.executeRequest();
			if (vTasks != null) {
				TaskInfo taskInfo = null;
				for (int i = 0; i < vTasks.size(); i++) {
					taskInfo = (TaskInfo) vTasks.get(i);
					if (taskInfo.getName().equalsIgnoreCase(this.taskName)) {
						taskID = taskInfo.getID();
						System.out.println("updating existing task ID: "
								+ taskID);
						analysisManager.stop(taskInfo.getName());
                        AdminDataService.getInstance().deleteTask(taskID);
						break;
					}
				}
			}

			ParameterFormatConverter pfc = new ParameterFormatConverter();
			parameter_info = pfc.getJaxbString(parameterInfoArray);

			//Invoke EJB function

			taskID = AdminDataService.getInstance().addNewTask(taskName, userId, accessId, description,
					parameter_info, taskInfoAttributes);
			analysisManager.startNewAnalysisTask(taskID);

		} catch (Exception ex) {
			System.out.println("AddNewTaskRequest(execute): Error "
					+ ex.getMessage());
			ex.printStackTrace();
			throw new OmnigeneException(ex.getMessage());
		}
		return taskID;
	}

	public static void main(String args[]) {
		AddNewTaskHandler arequest = new AddNewTaskHandler();
		try {
			System.out.println("Execute Result " + arequest.executeRequest());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}