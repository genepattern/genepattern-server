package edu.mit.broad.gp.ws;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;

import org.genepattern.gpge.ui.analysis.AnalysisJob;
import org.genepattern.gpge.ui.analysis.AnalysisService;
import org.genepattern.gpge.ui.analysis.LocalTaskExecutor;
import org.genepattern.gpge.ui.analysis.RequestHandler;
import org.genepattern.gpge.ui.analysis.TaskExecutor;
import org.genepattern.server.analysis.JobInfo;
import org.genepattern.server.analysis.ParameterInfo;
import org.genepattern.server.analysis.TaskInfo;
import org.genepattern.server.analysis.webservice.client.AdminProxy;
import org.genepattern.util.GPConstants;
/**
 *  This class is used to communicate with a GenePattern server.
 *
 *@author     Joshua Gould
 */
public class MatlabGPClient extends GPServer {
	
	/**
	 *  Creates a new MatlabGPClient instance.
	 *
	 *@param  server                   The server, for example
	 *      http://127.0.0.1:8080
	 *@param  userName                 The user name.
	 *@exception  WebServiceException  If an error occurs while connecting to the server
	 */
	public MatlabGPClient (String server, String userName) throws WebServiceException {
		super(server, userName);
	}

	
	/** 
	* Contacts this GenePattern server and retrieves a map of task ids to analysis services
	*@return the service map
	*/
	public Map getLatestServices() throws WebServiceException{
		try {
			Map services = new HashMap();
			TaskInfo[] tasks = adminProxy.getLatestTasksByName();
			for(int i = 0, length = tasks.length; i < length; i++) {
				String taskId = (String) tasks[i].getTaskInfoAttributes().get(GPConstants.LSID);
				if(taskId==null) {
					taskId = tasks[i].getName();
				}

				services.put(taskId, new AnalysisService(server, axisServletURL, tasks[i]));
			}
			return services;
		} catch(Exception e) {
			throw new WebServiceException(e);	
		}
	}
		
	/** 
	* Contacts this GenePattern server and retrieves a map of task ids to analysis services
	*@return the service map
	*/
	public Map getServices() throws WebServiceException{
		try {
			Map services = new HashMap();
			TaskInfo[] tasks = adminProxy.getAllTasks();
			for(int i = 0, length = tasks.length; i < length; i++) {
				String taskId = (String) tasks[i].getTaskInfoAttributes().get(GPConstants.LSID);
				if(taskId==null) {
					taskId = tasks[i].getName();
				}
				services.put(taskId, new AnalysisService(server, axisServletURL, tasks[i]));
			}
			return services;
		} catch(Exception e) {
			throw new WebServiceException(e);	
		}
	}



}

