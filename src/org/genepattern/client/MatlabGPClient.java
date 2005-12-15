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


package org.genepattern.client;

import java.util.HashMap;
import java.util.Map;

import org.genepattern.util.GPConstants;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * This class is used to communicate with a GenePattern server.
 * 
 * @author Joshua Gould
 */
public class MatlabGPClient extends GPServer {

	/**
	 * Creates a new MatlabGPClient instance.
	 * 
	 * @param server
	 *            The server, for example http://127.0.0.1:8080
	 * @param userName
	 *            The user name.
	 * @exception WebServiceException
	 *                If an error occurs while connecting to the server
	 */
	public MatlabGPClient(String server, String userName)
			throws WebServiceException {
		super(server, userName);
	}

	/**
	 * Contacts this GenePattern server and retrieves a map of task ids to
	 * analysis services
	 * 
	 * @return the service map
	 */
	public Map getLatestServices() throws WebServiceException {
		try {
			Map services = new HashMap();
			TaskInfo[] tasks = adminProxy.getLatestTasksByName();
			for (int i = 0, length = tasks.length; i < length; i++) {
				String taskId = (String) tasks[i].getTaskInfoAttributes().get(
						GPConstants.LSID);
				if (taskId == null) {
					taskId = tasks[i].getName();
				}

				services.put(taskId, new AnalysisService(server, tasks[i]));
			}
			return services;
		} catch (Exception e) {
			throw new WebServiceException(e);
		}
	}

	/**
	 * Contacts this GenePattern server and retrieves a map of task ids to
	 * analysis services
	 * 
	 * @return the service map
	 */
	public Map getServices() throws WebServiceException {
		try {
			Map services = new HashMap();
			TaskInfo[] tasks = adminProxy.getAllTasks();
			for (int i = 0, length = tasks.length; i < length; i++) {
				String taskId = (String) tasks[i].getTaskInfoAttributes().get(
						GPConstants.LSID);
				if (taskId == null) {
					taskId = tasks[i].getName();
				}
				services.put(taskId, new AnalysisService(server, tasks[i]));
			}
			return services;
		} catch (Exception e) {
			throw new WebServiceException(e);
		}
	}

}

