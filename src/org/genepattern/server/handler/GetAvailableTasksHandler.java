/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.handler;

import java.util.Arrays;
import java.util.List;

import org.genepattern.server.NoTaskFoundException;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterFormatConverter;
import org.genepattern.webservice.TaskInfo;

/**
 * Class used to get available tasks
 * 
 * @author rajesh kuttan
 * @version 1.0
 */
public class GetAvailableTasksHandler extends RequestHandler {
    private String userId = null;

	/** Creates new GetAvailableTasksHandler */
	public GetAvailableTasksHandler() {
		super();
	}

	public GetAvailableTasksHandler(String userId) {
		this.userId = userId;
	}

	/**
	 * Fetches information abouts tasks
	 * 
	 * @throws NoTaskFoundException
	 * @throws OmnigeneException
	 * @return Vector of <CODE>TaskInfo</CODE>
	 */
	public List executeRequest() throws OmnigeneException, NoTaskFoundException {
        List tasksVector = null;
		try {
			//Get EJB reference
			AdminDAO ds = new AdminDAO();            
			//Invoke EJB function
            TaskInfo[] taskArray = (userId == null ? ds.getAllTasks() : ds.getAllTasksForUser(userId));
            tasksVector = Arrays.asList(taskArray);
                
			if (tasksVector != null) {
				TaskInfo taskInfo = null;
				for (int i = 0; i < tasksVector.size(); i++) {
					taskInfo = (TaskInfo) tasksVector.get(i);
					taskInfo.setParameterInfoArray(ParameterFormatConverter.getParameterInfoArray(taskInfo.getParameterInfo()));
				}
			} 
			else {
				throw new OmnigeneException(
						"GetAvailableTasksRequest:executeRequest  null value returned for TaskInfo");
			}
		} 
		catch (NoTaskFoundException ex) {
			System.out
					.println("GetAvailableTasksRequest(executeRequest) NoTaskFoundException...");
			throw ex;
		} 
		catch (Exception ex) {
			System.out.println("GetAvailableTasksRequest(executeRequest): Error " + ex.getMessage());
			ex.printStackTrace();
			throw new OmnigeneException(ex.getMessage());
		}
		return tasksVector;
	}
}

