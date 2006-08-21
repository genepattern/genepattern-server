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

import org.genepattern.server.NoTaskFoundException;
import org.genepattern.server.webservice.server.dao.AnalysisJobService;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterFormatConverter;
import org.genepattern.webservice.TaskInfo;

//import edu.mit.wi.omnigene.omnidas.*;

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
	public List executeRequest() throws OmnigeneException,
			NoTaskFoundException {
        List tasksVector = null;
		try {

			//Get EJB reference
			AnalysisJobService ds = AnalysisJobService.getInstance();
            
			//Invoke EJB function
			tasksVector = ds.getTasks(userId);
			if (tasksVector != null) {
				TaskInfo taskInfo = null;
				for (int i = 0; i < tasksVector.size(); i++) {
					taskInfo = (TaskInfo) tasksVector.get(i);
					ParameterFormatConverter pfc = new ParameterFormatConverter();
					taskInfo
							.setParameterInfoArray(pfc
									.getParameterInfoArray(taskInfo
											.getParameterInfo()));
				}
			} else {
				throw new OmnigeneException(
						"GetAvailableTasksRequest:executeRequest  null value returned for TaskInfo");
			}

		} catch (NoTaskFoundException ex) {
			System.out
					.println("GetAvailableTasksRequest(executeRequest) NoTaskFoundException...");
			throw ex;
		} catch (Exception ex) {
			System.out
					.println("GetAvailableTasksRequest(executeRequest): Error "
							+ ex.getMessage());
			ex.printStackTrace();
			throw new OmnigeneException(ex.getMessage());
		}
		return tasksVector;
	}

	public static void main(String args[]) {
		GetAvailableTasksHandler arequest = new GetAvailableTasksHandler("");
		try {
			List taskVector = arequest.executeRequest();
			System.out.println("Size " + taskVector.size());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

