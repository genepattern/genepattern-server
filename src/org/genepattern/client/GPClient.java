/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.client;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.AdminProxy;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.AnalysisWebServiceProxy;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.JobResult;
import org.genepattern.webservice.LocalTaskExecutor;
import org.genepattern.webservice.Parameter;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskExecutor;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * This class is used to run modules on a GenePattern server.
 * 
 * @author Joshua Gould
 */
public class GPClient {
    private static Logger log = Logger.getLogger(GPClient.class);

    /**
     * number of modules to cache.
     */
    protected final static int MAX_ENTRIES = 50;

    protected final String server; // e.g. http://localhost:8080

    protected final String username;

    protected String password;

    /**
     * LRU cache of tasks.
     */
    protected Map<String, TaskInfo> cachedTasks;

    protected AdminProxy adminProxy;

    /**
     * Creates a new instance.
     * 
     * @param server
     *                The server, for example http://127.0.0.1:8080
     * @param username
     *                The user name.
     * @throws WebServiceException
     *                 If an error occurs while connecting to the server
     */
    public GPClient(String server, String username) throws WebServiceException {
        this(server, username, null);
    }

    /**
     * Creates a new instance.
     * As initially implemented (circa GP 3.1 through 3.2.0) the server arg was expected to be of this form,
     *     <protocol>://<host>[:<port>]
     *     e.g. http://genepattern.broadinstitute.org or http://127.0.0.1:8080
     * the following form for the server arg is also accepted (circa GP 3.2.1+),
     *     <protocol>://<host>[:<port>][<path>]
     *     e.g. http://genepattern.broadinstitute.org/gp/ or http://127.0.0.1:8080/gp
     * 
     * @param server
     *                The server, for example http://127.0.0.1:8080
     * @param username
     *                The user name.
     * @param password
     *                The password.
     * @throws WebServiceException
     *                 If an error occurs while connecting to the server
     */
    public GPClient(String server, String username, String password) throws WebServiceException {
        URL serverUrl = null;
        try {
            serverUrl = new URL(server);
            if (serverUrl.getPort() == -1) {
                server = serverUrl.getProtocol() + "://" + serverUrl.getHost();
            }
            else {
                server = serverUrl.getProtocol() + "://" + serverUrl.getHost() + ":" + serverUrl.getPort();
            }
        }
        catch (MalformedURLException e) {
            throw new WebServiceException(e);
        }

        String serverPath = serverUrl.getPath();
        if (serverPath != null && !serverPath.equals("") && !serverPath.equals("/gp") && !serverPath.equals("/gp/")) {
            log.error("Ignoring server path: "+serverPath);
        }
        
        this.server = server;
        this.username = username;
        this.password = password;
        this.cachedTasks = new LinkedHashMap<String, TaskInfo>(MAX_ENTRIES + 1, .75F, true) {
            @Override
            public boolean removeEldestEntry(Map.Entry<String, TaskInfo> eldest) {
                return size() > MAX_ENTRIES;
            }
        };
        try {
            adminProxy = new AdminProxy(this.server, this.username, this.password);
        }
        catch (Exception e) {
            throw new WebServiceException(e);
        }
    }

    /**
     * Terminate the job.
     * @param jobId
     * @throws WebServiceException
     */
    public void terminateJob(int jobId) throws WebServiceException {
    	AnalysisWebServiceProxy analysisProxy = new AnalysisWebServiceProxy(server, username, password, false);
    	analysisProxy.terminateJob(jobId);
    }

    /**
     * Creates a new <tt>JobResult</tt> instance for the given job number. Invoke this method after the job is
     * complete.
     * 
     * @param jobNumber
     *                the job number
     * @return <tt>JobResult</tt> instance or <tt>null</tt> if the job is not complete
     * @throws WebServiceException
     *                 If an error occurs
     * @see #isComplete
     */
    public JobResult createJobResult(int jobNumber) throws WebServiceException {
        AnalysisWebServiceProxy analysisProxy = new AnalysisWebServiceProxy(server, username, password, false);
        analysisProxy.setTimeout(Integer.MAX_VALUE);
        JobInfo info = analysisProxy.checkStatus(jobNumber);
        if (info == null) {
            return null;
        }
        return createJobResult(info);
    }

    /**
     * Returns the url to retrieve the given file as part of the given module.
     * 
     * @param moduleNameOrLsid
     *                The module name or LSID of the module that contains the file. When an LSID is provided that does
     *                not include a version, the latest available version of the module identified by the LSID will be
     *                used. If a module name is supplied, the latest version of the module with the nearest authority is
     *                selected. The nearest authority is the first match in the sequence: local authority, Broad
     *                authority, other authority.
     * @param fileName
     *                The file name.
     * @return The url.
     */
    public URL getModuleFileUrl(String moduleNameOrLsid, String fileName) {
	try {
	    return new URL(server + "/gp/getFile.jsp?task=" + moduleNameOrLsid + "&file="
		    + URLEncoder.encode(fileName, "UTF-8"));
	} catch (java.net.MalformedURLException x) {
	    throw new Error(x);
	} catch (UnsupportedEncodingException x) {
	    throw new Error(x);
	}
    }

    /**
     * Checks if the given module is cached from the server.
     * 
     * @param moduleNameOrLsid
     *                The module name or LSID. When an LSID is provided that does not include a version, the latest
     *                available version of the task identified by the LSID will be used. If a module name is supplied,
     *                the latest version of the module with the nearest authority is selected. The nearest authority is
     *                the first match in the sequence: local authority, Broad authority, other authority.
     * @return <tt>true</tt> if the module is cached<tt>false</tt> otherwise.
     */
    public boolean isCached(String moduleNameOrLsid) {
	return cachedTasks.containsKey(moduleNameOrLsid);
    }

    /**
     * Returns the TaskInfo object for the specified module name or lsid.
     * 
     * @param moduleNameOrLsid
     *                The module name or LSID. When an LSID is provided that does not include a version, the latest
     *                available version of the task identified by the LSID will be used. If a module name is supplied,
     *                the latest version of the module with the nearest authority is selected. The nearest authority is
     *                the first match in the sequence: local authority, Broad authority, other authority.
     * @return The <tt>TaskInfo</tt> object.
     * @throws WebServiceException
     *                 If an error occurs while getting the parameters.
     */
    public TaskInfo getModule(String moduleNameOrLsid) throws WebServiceException {
	try {
	    return getTask(moduleNameOrLsid);
	} catch (org.genepattern.webservice.WebServiceException wse) {
	    throw new WebServiceException(wse.getMessage(), wse.getRootCause());
	}
    }

    /**
     * Returns the array of parameters for the specified module name or lsid.
     * 
     * @param moduleNameOrLsid
     *                The module name or LSID. When an LSID is provided that does not include a version, the latest
     *                available version of the task identified by the LSID will be used. If a module name is supplied,
     *                the latest version of the module with the nearest authority is selected. The nearest authority is
     *                the first match in the sequence: local authority, Broad authority, other authority.
     * @return The array of parameters for the specified module name or lsid.
     * @throws WebServiceException
     *                 If an error occurs while getting the parameters.
     */
    public ParameterInfo[] getParameters(String moduleNameOrLsid) throws WebServiceException {
	try {
	    TaskInfo taskInfo = getTask(moduleNameOrLsid);
	    return taskInfo.getParameterInfoArray();
	} catch (org.genepattern.webservice.WebServiceException wse) {
	    throw new WebServiceException(wse.getMessage(), wse.getRootCause());
	}
    }

    /**
     * Gets the server.
     * 
     * @return The server
     */
    public String getServer() {
	return server;
    }

    /**
     * Gets the username.
     * 
     * @return The username.
     */
    public String getUsername() {
	return username;
    }

    /**
     * Checks if the given job is complete.
     * 
     * @param jobNumber
     *                the job number
     * @return <tt>true</tt> if the job with the given job number is complete, <tt>false</tt> otherwise
     * @throws WebServiceException
     *                 If an error occurs
     */
    public boolean isComplete(int jobNumber) throws WebServiceException {
	try {
	    AnalysisWebServiceProxy analysisProxy = new AnalysisWebServiceProxy(server, username, password, false);
	    analysisProxy.setTimeout(Integer.MAX_VALUE);
	    JobInfo ji = analysisProxy.checkStatus(jobNumber);
	    if (ji == null) {
		throw new WebServiceException("The job number " + jobNumber + " was not found.");
	    }
	    return ji.getStatus().equalsIgnoreCase("finished") || ji.getStatus().equalsIgnoreCase("error");
	} catch (Exception x) {
	    throw new WebServiceException(x);
	}
    }

    /**
     * Submits the given module with the given parameters and waits for the job to complete.
     * 
     * @param moduleNameOrLsid
     *                The module name or LSID. When an LSID is provided that does not include a version, the latest
     *                available version of the task identified by the LSID will be used. If a module name is supplied,
     *                the latest version of the module with the nearest authority is selected. The nearest authority is
     *                the first match in the sequence: local authority, Broad authority, other authority.
     * @param parameters
     *                The parameters to run the module with.
     * @return The job result.
     * @throws WebServiceException
     *                 If an error occurs during the job submission or job result retrieval process.
     */
    public JobResult runAnalysis(String moduleNameOrLsid, Parameter[] parameters) throws WebServiceException {
	try {
	    TaskInfo taskInfo = getTask(moduleNameOrLsid);
	    ParameterInfo[] actualParameters = GPClient.createParameterInfoArray(taskInfo, parameters);
	    return runAnalysis(taskInfo, actualParameters);
	} catch (org.genepattern.webservice.WebServiceException wse) {
	    throw new WebServiceException(wse.getMessage(), wse.getRootCause());
	}
    }

    /**
     * Submits the given module with the given parameters and waits for the job to complete.
     * 
     * @param taskInfo
     *                The <tt>TaskInfo</tt> object
     * @param parameters
     *                The parameters to run the module with.
     * @return The job result.
     * @throws WebServiceException
     *                 If an error occurs during the job submission or job result retrieval process.
     */
    public JobResult runAnalysis(TaskInfo taskInfo, ParameterInfo[] parameters) throws WebServiceException {
	AnalysisWebServiceProxy analysisProxy = null;
	try {
	    analysisProxy = new AnalysisWebServiceProxy(server, username, password);
	    analysisProxy.setTimeout(Integer.MAX_VALUE);
	} catch (Exception x) {
	    throw new WebServiceException(x);
	}
	AnalysisJob job = submitJob(analysisProxy, taskInfo, parameters);
	waitForErrorOrCompletion(analysisProxy, job);
	return createJobResult(job.getJobInfo());
    }

    /**
     * Submits the given module with the given parameters and waits for the job to complete.
     * 
     * @param moduleNameOrLsid
     *                The module name or LSID. When an LSID is provided that does not include a version, the latest
     *                available version of the task identified by the LSID will be used. If a module name is supplied,
     *                the latest version of the module with the nearest authority is selected. The nearest authority is
     *                the first match in the sequence: local authority, Broad authority, other authority.
     * @param parameters
     *                The parameters to run the module with as a String. Format is
     *                name1=value1;name2=value2;name3=value3;...
     * @return The job result.
     * @throws WebServiceException
     *                 If an error occurs during the job submission or job result retrieval process.
     */
    public JobResult runAnalysis(String moduleNameOrLsid, String parameters) throws WebServiceException {
	return runAnalysis(moduleNameOrLsid, parseParameterString(parameters));
    }

    /**
     * Submits the given module with the given parameters and does not wait for the job to complete.
     * 
     * @param moduleNameOrLsid
     *                The module name or LSID. When an LSID is provided that does not include a version, the latest
     *                available version of the task identified by the LSID will be used. If a module name is supplied,
     *                the latest version of the module with the nearest authority is selected. The nearest authority is
     *                the first match in the sequence: local authority, Broad authority, other authority.
     * @param parameters
     *                The parameters to run the module with.
     * @return The job number.
     * @throws WebServiceException
     *                 If an error occurs during the job submission process.
     * @see #isComplete
     * @see #createJobResult
     */
    public int runAnalysisNoWait(String moduleNameOrLsid, Parameter[] parameters) throws WebServiceException {
	    return runAnalysisNoWait(moduleNameOrLsid, parameters, Integer.MAX_VALUE);
    }

    /**
     * Submits the given module with the given parameters and does not wait for the job to complete.
     * 
     * @param moduleNameOrLsid
     *                The module name or LSID. When an LSID is provided that does not include a version, the latest
     *                available version of the task identified by the LSID will be used. If a module name is supplied,
     *                the latest version of the module with the nearest authority is selected. The nearest authority is
     *                the first match in the sequence: local authority, Broad authority, other authority.
     * @param parameters
     *                The parameters to run the module with.
     * @return The job number.
     * @throws WebServiceException
     *                 If an error occurs during the job submission process.
     * @see #isComplete
     * @see #createJobResult
     */
    public int runAnalysisNoWait(String moduleNameOrLsid, Parameter[] parameters, int analysisProxyTimeout) throws WebServiceException {
    try {
        TaskInfo taskInfo = getTask(moduleNameOrLsid);
        ParameterInfo[] actualParameters = GPClient.createParameterInfoArray(taskInfo, parameters);
        AnalysisWebServiceProxy analysisProxy = null;
        try {
        analysisProxy = new AnalysisWebServiceProxy(server, username, password);
        analysisProxy.setTimeout(analysisProxyTimeout);
        } catch (Exception x) {
        throw new WebServiceException(x);
        }
        AnalysisJob job = submitJob(analysisProxy, taskInfo, actualParameters);
        return job.getJobInfo().getJobNumber();
    } catch (org.genepattern.webservice.WebServiceException wse) {
        throw new WebServiceException(wse.getMessage(), wse.getRootCause());
    }

    }

    /**
     * Submits the given module with the given parameters and does not wait for the job to complete.
     * 
     * @param moduleNameOrLsid
     *                The module name or LSID. When an LSID is provided that does not include a version, the latest
     *                available version of the task identified by the LSID will be used. If a module name is supplied,
     *                the latest version of the module with the nearest authority is selected. The nearest authority is
     *                the first match in the sequence: local authority, Broad authority, other authority.
     * @param parameters
     *                The parameters to run the module with as a String. Format is
     *                name1=value1;name2=value2;name3=value3;....
     * @return The job number.
     * @throws WebServiceException
     *                 If an error occurs during the job submission process.
     * @see #isComplete
     * @see #createJobResult
     */
    public int runAnalysisNoWait(String moduleNameOrLsid, String parameters) throws WebServiceException {
	return runAnalysisNoWait(moduleNameOrLsid, parseParameterString(parameters));
    }

    /**
     * Downloads the support files for the given module from the server and executes the given module locally.
     * 
     * @param moduleNameOrLsid
     *                The module name or LSID. When an LSID is provided that does not include a version, the latest
     *                available version of the task identified by the LSID will be used. If a module name is supplied,
     *                the latest version of the module with the nearest authority is selected. The nearest authority is
     *                the first match in the sequence: local authority, Broad authority, other authority.
     * @param parameters
     *                The parameters to run the module with.
     * @throws WebServiceException
     *                 If an error occurs while launching the visualizer.
     */
    public void runVisualizer(String moduleNameOrLsid, Parameter[] parameters) throws WebServiceException {
	TaskInfo taskInfo = getTask(moduleNameOrLsid);
	ParameterInfo[] actualParameters = GPClient.createParameterInfoArray(taskInfo, parameters);
	Map<String, String> paramName2ValueMap = new HashMap<String, String>();
	if (actualParameters != null) {
	    for (int i = 0; i < actualParameters.length; i++) {
		paramName2ValueMap.put(actualParameters[i].getName(), actualParameters[i].getValue());
	    }
	}
	try {
	    final TaskExecutor executor = new LocalTaskExecutor(taskInfo, paramName2ValueMap, username, password,
		    server);
	    new Thread() {
		@Override
		public void run() {
		    try {
			executor.exec();
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		}
	    }.start();
	} catch (Exception x) {
	    throw new WebServiceException(x);
	}
    }

    /**
     * Downloads the support files for the given module from the server and executes the given module locally.
     * 
     * @param moduleNameOrLsid
     *                The module name or LSID. When an LSID is provided that does not include a version, the latest
     *                available version of the task identified by the LSID will be used. If a module name is supplied,
     *                the latest version of the module with the nearest authority is selected. The nearest authority is
     *                the first match in the sequence: local authority, Broad authority, other authority.
     * @param parameters
     *                The parameters to run the module with as a String. Format is
     *                name1=value1;name2=value2;name3=value3;...
     * @throws WebServiceException
     *                 If an error occurs while launching the visualizer.
     */
    public void runVisualizer(String moduleNameOrLsid, String parameters) throws WebServiceException {
	runVisualizer(moduleNameOrLsid, parseParameterString(parameters));
    }

    private JobResult createJobResult(JobInfo info) throws WebServiceException {
        String status = info.getStatus();
        if (!(status.equalsIgnoreCase("ERROR") || (status.equalsIgnoreCase("Finished")))) {
            return null;
        }
        TaskInfo taskInfo = getTask(info.getTaskLSID());
        ArrayList<String> resultFiles = new ArrayList<String>();
        ParameterInfo[] jobParameterInfo = info.getParameterInfoArray();
        boolean stderr = false;
        boolean stdout = false;
        ArrayList<Parameter> jobParameters = new ArrayList<Parameter>();
        for(ParameterInfo paramInfo : jobParameterInfo) {
            if (paramInfo.isOutputFile()) {
                int fileJobNumber = info.getJobNumber();
                String fileName = paramInfo.getValue();
                int index1 = fileName.lastIndexOf('/');
                int index2 = fileName.lastIndexOf('\\');
                int index = (index1 > index2 ? index1 : index2);
                if (index != -1) {
                    try {
                        fileJobNumber = Integer.parseInt( fileName.substring(0, index) );
                    }
                    catch (NumberFormatException e) {
                        log.error("Error getting job number from resultFile: "+fileName, e);
                    }
                    fileName = fileName.substring(index + 1, fileName.length());
                }
                if (fileJobNumber != info.getJobNumber()) {
                    // ignore
                }
                else if (fileName.equals(GPConstants.STDOUT)) {
                    stdout = true;
                } 
                else if (fileName.equals(GPConstants.STDERR)) {
                    stderr = true;
                } 
                else if (fileName.equals(GPConstants.TASKLOG)) {
                    // ignore
                } 
                else if (fileName.endsWith(GPConstants.PIPELINE_TASKLOG_ENDING)) {
                    // ignore
                }
                else {
                    resultFiles.add(fileName);
                }
            } 
            else {
                jobParameters.add(new Parameter(paramInfo.getName(), paramInfo.getValue()));
            }
        }
        try {
            final URL serverUrl = new URL(server);
            final int jobNumber = info.getJobNumber();
            final String[] resultFilenames = resultFiles.toArray(new String[0]);
            final Parameter[] jobParametersArray = jobParameters.toArray(new Parameter[0]);
            final String taskLsid = taskInfo.getTaskInfoAttributes().get(GPConstants.LSID);
            return new JobResult(
                    serverUrl, 
                    jobNumber, 
                    resultFilenames, 
                    stdout, 
                    stderr, 
                    jobParametersArray, 
                    taskLsid, 
                    username, 
                    password);
        } 
        catch (java.net.MalformedURLException mfe) {
            throw new Error(mfe);
        }
    }

    private TaskInfo getTask(String lsid) throws WebServiceException {
	TaskInfo taskInfo = cachedTasks.get(lsid);
	if (taskInfo == null) {
	    try {
		taskInfo = adminProxy.getTask(lsid);
	    } catch (Exception e) {
		throw new WebServiceException(e);
	    }
	    if (taskInfo == null) {
		throw new WebServiceException(lsid + " not found on server.");
	    }
	    cachedTasks.put(lsid, taskInfo);
	}
	return taskInfo;
    }

    /**
     * submit a job based on a service and its parameters
     * 
     * @param parmInfos
     *                Description of the Parameter
     * @param handler
     *                Description of the Parameter
     * @param tinfo
     *                Description of the Parameter
     * @return Description of the Return Value
     * @throws org.genepattern.webservice.WebServiceException
     *                 Description of the Exception
     */
    private AnalysisJob submitJob(AnalysisWebServiceProxy handler, TaskInfo tinfo, ParameterInfo[] parmInfos)
	    throws org.genepattern.webservice.WebServiceException {
	final JobInfo job = handler.submitJob(tinfo.getID(), parmInfos);
	final AnalysisJob aJob = new AnalysisJob(server, job);
	return aJob;
    }

    private static ParameterInfo[] createParameterInfoArray(TaskInfo taskInfo, Parameter[] parameters)
	    throws WebServiceException {

	ParameterInfo[] formalParameters = taskInfo.getParameterInfoArray();
	List<ParameterInfo> actualParameters = new ArrayList<ParameterInfo>();

	Map<String, ParameterInfo> paramName2FormalParam = new HashMap<String, ParameterInfo>();
	if (formalParameters != null) {
	    for (int i = 0, length = formalParameters.length; i < length; i++) {
		paramName2FormalParam.put(formalParameters[i].getName(), formalParameters[i]);
	    }
	}

	if (parameters != null) {
	    for (int i = 0, length = parameters.length; i < length; i++) {
		ParameterInfo formalParam = paramName2FormalParam.remove(parameters[i].getName());
		if (formalParam == null) {
		    if (parameters[i].getName().equals(GPConstants.PIPELINE_ARG_STOP_AFTER_TASK_NUM)) {
			formalParam = new ParameterInfo(parameters[i].getName(), parameters[i].getValue(), "");
		    } else {
			throw new WebServiceException("Unknown parameter: " + parameters[i].getName());
		    }
		}
		Map<?, ?> formalAttributes = formalParam.getAttributes();
		if (formalAttributes == null) {
		    formalAttributes = new HashMap<Object, Object>();
		}
		String value = parameters[i].getValue();

		if (value == null) {
		    value = (String) formalAttributes.get(GPConstants.PARAM_INFO_DEFAULT_VALUE[0]);
		}
		if (value == null && !isOptional(formalParam)) {
		    throw new WebServiceException("Missing value for required parameter "
			    + formalParameters[i].getName());

		}
		if (value != null) {
		    value = sub(formalParam, value);
		    ParameterInfo p = new ParameterInfo(formalParam.getName(), value, "");
		    setAttributes(formalParam, p);
		    actualParameters.add(p);
		}
	    }
	}

	// go through parameters that were not provided by user
	for (Iterator<String> it = paramName2FormalParam.keySet().iterator(); it.hasNext();) {
	    String name = it.next();
	    ParameterInfo formalParam = paramName2FormalParam.get(name);
	    String value = (String) formalParam.getAttributes().get(TaskExecutor.PARAM_INFO_DEFAULT_VALUE[0]);
	    if (value == null && !isOptional(formalParam)) {
		throw new WebServiceException("Missing value for required parameter " + formalParam.getName());

	    }
	    if (value != null && !value.equals("")) {
		value = sub(formalParam, value);
		ParameterInfo actual = new ParameterInfo(formalParam.getName(), value, "");
		setAttributes(formalParam, actual);
		actualParameters.add(actual);
	    }

	}

	return actualParameters.toArray(new ParameterInfo[0]);
    }

    /**
     * make the sleep time go up as it takes longer to exec. eg for 100 tries of 1000ms (1 sec) first 20 are 1 sec each
     * next 20 are 2 sec each next 20 are 4 sec each next 20 are 8 sec each any beyond this are 16 sec each
     * 
     * @param init
     *                Description of the Parameter
     * @param maxTries
     *                Description of the Parameter
     * @param count
     *                Description of the Parameter
     * @return Description of the Return Value
     */
    private static int incrementSleep(int init, int maxTries, int count) {
	if (count < (maxTries * 0.2)) {
	    return init;
	}
	if (count < (maxTries * 0.4)) {
	    return init * 2;
	}
	if (count < (maxTries * 0.6)) {
	    return init * 4;
	}
	if (count < (maxTries * 0.8)) {
	    return init * 8;
	}
	return init * 16;
    }

    private static boolean isOptional(ParameterInfo formalParameter) {
	String sOptional = (String) formalParameter.getAttributes().get(GPConstants.PARAM_INFO_OPTIONAL[0]);
	return (sOptional != null && sOptional.length() > 0);

    }

    private static Parameter[] parseParameterString(String parameters) throws WebServiceException {
	String[] tokens = parameters.split(";");
	Parameter[] params = new Parameter[tokens.length];

	for (int i = 0, length = tokens.length; i < length; i++) {
	    String[] nameValue = tokens[i].split("=");
	    if (nameValue.length != 2) {
		throw new WebServiceException("Error parsing parameters");
	    }
	    params[i] = new Parameter(nameValue[0], nameValue[1]);
	}
	return params;
    }

    private static void setAttributes(ParameterInfo formalParam, ParameterInfo actualParam) {
	if (formalParam.isInputFile()) {
	    HashMap<String, String> actualAttributes = new HashMap<String, String>();
	    actualParam.setAttributes(actualAttributes);
	    String value = actualParam.getValue();
	    actualAttributes.put(GPConstants.PARAM_INFO_CLIENT_FILENAME[0], value);
	    if (value != null && new File(value).exists() && !new File(value).isDirectory()) {
		actualParam.setAsInputFile();
	    } else if (value != null) {
		actualAttributes.remove("TYPE");
		actualAttributes.put(ParameterInfo.MODE, ParameterInfo.URL_INPUT_MODE);
	    }
	}
    }

    /**
     * See if parameter belongs to a set of choices, e.g. 1=T-Test.
     * If so substitute 1 for T-Test, also check to see if value is valid.
     * 
     * @param formalParam
     * @param value
     * @return
     * @throws WebServiceException
     */
    private static String sub(ParameterInfo formalParam, String value) throws WebServiceException {
        String choicesString = formalParam.getValue();
        if (value != null && choicesString != null && !choicesString.equals("")) {
            String[] choices = choicesString.split(";");
            boolean validValue = false;
            for (int j = 0; j < choices.length && !validValue; j++) {
                String[] choiceValueAndChoiceUIValue = choices[j].split("=");
                if (value.equals(choiceValueAndChoiceUIValue[0])) {
                    validValue = true;
                } else if (choiceValueAndChoiceUIValue.length == 2 && value.equals(choiceValueAndChoiceUIValue[1])) {
                    value = choiceValueAndChoiceUIValue[0];
                    validValue = true;
                }
            }
            if (!validValue) {
                throw new WebServiceException("Illegal value for parameter " + formalParam.getName() + ". Value: "
                        + value);
            }
        }
        return value;
    }

    /**
     * Wait for a job to end or error.
     * 
     * @param job
     *                Description of the Parameter
     * @param handler
     *                Description of the Parameter
     * @throws org.genepattern.webservice.WebServiceException
     *                 Description of the Exception
     */
    private static void waitForErrorOrCompletion(AnalysisWebServiceProxy handler, AnalysisJob job)
	    throws org.genepattern.webservice.WebServiceException {
	int maxtries = 20;
	int sleep = 1000;
	waitForErrorOrCompletion(handler, job, maxtries, sleep);
    }

    private static void waitForErrorOrCompletion(AnalysisWebServiceProxy handler, AnalysisJob job, int maxTries,
	    int initialSleep) throws org.genepattern.webservice.WebServiceException {
	String status = "";
	JobInfo info = null;
	int count = 0;
	int sleep = initialSleep;
	while (!(status.equalsIgnoreCase("Error") || (status.equalsIgnoreCase("Finished")))) {
	    count++;
	    try {
		Thread.sleep(sleep);
	    } catch (InterruptedException ie) {
	    }
	    info = handler.checkStatus(job.getJobInfo().getJobNumber());
	    job.setJobInfo(info);
	    status = info.getStatus();
	    sleep = incrementSleep(initialSleep, maxTries, count);
	}
    }

}
