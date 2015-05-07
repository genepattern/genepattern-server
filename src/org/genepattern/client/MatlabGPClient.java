/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

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
public class MatlabGPClient extends GPClient {

    /**
     * Creates a new MatlabGPClient instance.
     * 
     * @param server
     *                The server, for example http://127.0.0.1:8080
     * @param userName
     *                The user name.
     * @exception WebServiceException
     *                    If an error occurs while connecting to the server
     */
    public MatlabGPClient(String server, String userName) throws WebServiceException {
	super(server, userName);
    }

    public MatlabGPClient(String server, String userName, String password) throws WebServiceException {
	super(server, userName, password);
    }

    /**
     * Contacts this GenePattern server and retrieves a map of task ids to analysis services
     * 
     * @return the service map
     */
    public Map<String, AnalysisService> getLatestServices() throws WebServiceException {
	try {
	    Map<String, AnalysisService> services = new HashMap<String, AnalysisService>();
	    TaskInfo[] tasks = adminProxy.getLatestTasksByName();
	    for (int i = 0, length = tasks.length; i < length; i++) {
		String taskId = tasks[i].getTaskInfoAttributes().get(GPConstants.LSID);
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
     * Contacts this GenePattern server and retrieves a map of task ids to analysis services
     * 
     * @return the service map
     */
    public Map<String, AnalysisService> getServices() throws WebServiceException {
	try {
	    Map<String, AnalysisService> services = new HashMap<String, AnalysisService>();
	    TaskInfo[] tasks = adminProxy.getAllTasks();
	    for (int i = 0, length = tasks.length; i < length; i++) {
		String taskId = tasks[i].getTaskInfoAttributes().get(GPConstants.LSID);
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
