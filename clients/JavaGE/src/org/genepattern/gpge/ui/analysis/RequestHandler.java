package org.genepattern.gpge.ui.analysis;


import java.io.File;
import javax.activation.DataHandler;
import org.apache.log4j.Category;
import org.genepattern.server.analysis.JobInfo;
import org.genepattern.server.analysis.ParameterInfo;
import org.genepattern.server.analysis.TaskInfo;
import org.genepattern.server.analysis.webservice.client.AnalysisWebServiceProxy;
import org.genepattern.server.webservice.WebServiceException;

/**
 * RequestHandler.java
 * <p>Description: handles the requests sent to web services.</p>
 * @author Hui Gong
 * @version $Revision$
 */

public class RequestHandler {
    private AnalysisWebServiceProxy _proxy;
    private String _url;
    private String _siteName;
    private static Category cat = Category.getInstance(RequestHandler.class.getName());

	
    private void debug(String str){
	if ("true".equals(System.getProperty("DEBUG")))
		cat.debug(str);
    }

    public RequestHandler(String name, String url) {
	this(name, url, null, null);
    }

    public RequestHandler(String name, String url, String username, String password) {
        this._siteName = name;
        this._url = url;
        _proxy = new AnalysisWebServiceProxy(url, username, password);
    }

    public synchronized String getURL(){
        return this._url;
    }

    public synchronized AnalysisService[] getAnalysisServices() throws WebServiceException{
        TaskInfo [] tasks = _proxy.getTasks();
        int i, size;
        size = tasks.length;
        AnalysisService[] services = new AnalysisService[size];
        for(i=0; i< size; i++){
            services[i] = new AnalysisService(_siteName, _url, tasks[i]);
        }
        return services;
    }

    public synchronized TaskInfo[] getTasks() throws WebServiceException{
        debug("Sending request for getTasks()");
        return _proxy.getTasks();
    }

    public synchronized JobInfo submitJob(int id, ParameterInfo[] parmInfos, File file) throws WebServiceException{
        debug("Sending request to submitJob");
        return _proxy.submitJob(id, parmInfos, file);
    }


    public synchronized JobInfo submitJob(int id, ParameterInfo[] parmInfos) throws WebServiceException{
        debug("Sending request to submitJob");
        return _proxy.submitJob(id, parmInfos);
    }

    public synchronized DataHandler getResults(int jobID) throws WebServiceException{
        debug("Sending request for getResults()");
        return _proxy.getResults(jobID);
    }

    public synchronized JobInfo checkStatus(int jobID) throws WebServiceException{
        debug("check job status " + jobID);
        return _proxy.checkStatus(jobID);
    }

    public synchronized String[] getResultFiles(int jobID) throws WebServiceException{
        debug("Getting result files");
        return _proxy.getResultFiles(jobID);
    }

}
