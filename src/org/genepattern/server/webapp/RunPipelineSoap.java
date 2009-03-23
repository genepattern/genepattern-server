/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2009) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.EncryptionUtil;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.AdminProxy;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.AnalysisWebServiceProxy;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;
import org.xml.sax.InputSource;

/**
 * Note that RunPipeline may only be run on the server side in the context of a GenePattern installation as it needs to
 * connect natively to the DB (via LocalAnalysisClient and LocalAdminClient). This is done to prevent the need to pass a
 * users Password into this code in order to execute the pipeline tasks as that user.
 * 
 * If needed, we could easily make a remote version (using remote interfaces) but this does not seem to be necessary.
 * 
 * @author liefeld
 * 
 */

public class RunPipelineSoap {
    private static final String logFile = "pipelineErrors.log"; // one log file per pipeline

    private static final Logger log = setupLog4jConfig(logFile);

    private RunPipelineOutputDecoratorIF decorator;

    private PipelineModel model;
    /** server to run the pipeline on */
    private String server;

    /** job id for the pipeline */
    private int jobId;

    private AnalysisWebServiceProxy analysisClient;

    private AdminProxy adminClient;

    public RunPipelineSoap(String server, String userID, String cmdLinePassword, int jobId, PipelineModel model,
	    RunPipelineOutputDecoratorIF decorator) throws WebServiceException {
	this.analysisClient = new AnalysisWebServiceProxy(server, userID, cmdLinePassword);
	this.adminClient = new AdminProxy(server, userID, cmdLinePassword);
	this.server = server;
	System.setProperty("userID", userID);
	this.jobId = jobId;
	this.model = model;
	this.decorator = decorator == null ? new RunPipelineBasicDecorator() : decorator;
    }

    public void runPipeline(Map args) throws WebServiceException {
        log.debug("runPipeline");
        String stopAfterTaskStr = System.getProperty(GPConstants.PIPELINE_ARG_STOP_AFTER_TASK_NUM);
        int stopAfterTask = Integer.MAX_VALUE;
        if (stopAfterTaskStr != null) {
            if (stopAfterTaskStr.trim().length() > 0) {
                try {
                    stopAfterTask = Integer.parseInt(stopAfterTaskStr);
                } 
                catch (NumberFormatException nfe) {
                    log.error("Ignoring invalid number format for: " + 
                            GPConstants.PIPELINE_ARG_STOP_AFTER_TASK_NUM + "=" + stopAfterTask, nfe);
                }
            }
        }
        //Vector vTasks = model.getTasks();
        //JobSubmission jobSubmission = null;
        TaskInfo taskInfo = null;
        ParameterInfo[] parameterInfo = null;
        int taskNum = 0;
        boolean okayToRun = true;
        StringBuffer errorMessages = null;
        for(JobSubmission jobSubmission : model.getTasks()) {
            taskInfo = adminClient.getTask(jobSubmission.getLSID());
            if (taskInfo == null) {
                okayToRun = false;
                String errorMessage = "No such module " + jobSubmission.getName() + " (" + jobSubmission.getLSID() + ")";
                log.error(errorMessage);
                decorator.error(model, errorMessage);
                if (errorMessages == null) {
                    errorMessages = new StringBuffer(errorMessage);
                }
                else {
                    errorMessages.append(", "+errorMessage);
                }
            }
        }
        if (!okayToRun) {
            throw new IllegalArgumentException(errorMessages.toString());
        }

        taskNum = 0;
        JobInfo results[] = new JobInfo[model.getTasks().size()];
        decorator.beforePipelineRuns(model);
        try {
            for(JobSubmission jobSubmission : model.getTasks()) { 
                if (taskNum >= stopAfterTask) {
                    break; // stop and execute no further
                }
                try {
                    parameterInfo = jobSubmission.giveParameterInfoArray();
                    setInheritedJobParameters(parameterInfo, results);
                    substituteLsidInInputFiles(parameterInfo);
                    ParameterInfo[] params = parameterInfo;
                    params = setJobParametersFromArgs(jobSubmission.getName(), taskNum + 1, params, results, args);
                    params = removeEmptyOptionalParams(parameterInfo);
                    decorator.recordTaskExecution(jobSubmission, taskNum + 1, model.getTasks().size());
                    JobInfo taskResult = executeTask(jobSubmission, params, taskNum, results);

                    // handle the special case where a task is a pipeline by adding
                    // all output files of the pipeline's children (recursively) to its
                    // taskResult so that they can be used downstream
                    taskResult = collectChildJobResults(taskResult);
                    decorator.recordTaskCompletion(taskResult, jobSubmission.getName() + (taskNum + 1));
                    results[taskNum] = taskResult;
                } 
                catch (Exception e) {
                    log.error("Execution for " + jobSubmission.getName() + " module failed.");
                    if (e.getMessage() != null) {
                        log.error(e.getMessage());
                    }
                    break;
                }
            }
        } 
        finally {
            decorator.afterPipelineRan(model);
        }
        setStatus(JobStatus.FINISHED);
    }

    /**
     * handle the special case where a task is a pipeline by adding all output files of the pipeline's children
     * (recursively) to its taskResult so that they can be used downstream
     * 
     * recurse through the children and add all output params to the parent
     */
    protected JobInfo collectChildJobResults(JobInfo taskResult) {
	if (taskResult == null) {
	    log.debug("Invalid null arg to collectChildJobResults");
	    return taskResult;
	}
	log.debug("collectChildJobResults for: " + taskResult.getJobNumber());
	try {
	    List<ParameterInfo> outs = new ArrayList<ParameterInfo>();

	    if (taskResult == null) {
		log.error("taskResult == null");
	    }
	    int[] children = analysisClient.getChildren(taskResult.getJobNumber());
	    if (children.length == 0) {
		return taskResult;
	    }
	    for (int i = 0; i < children.length; i++) {
		getChildJobOutputs(children[i], outs);
	    }
	    // now add them to the parent
	    if (outs.size() == 0) {
		return taskResult;
	    }
	    for (ParameterInfo p : outs) {
		taskResult.addParameterInfo(p);
	    }

	} catch (Exception wse) {
	    wse.printStackTrace();
	}
	return taskResult;
    }

    /**
     * submit the job and wait for it to complete
     * 
     * @throws WebServiceException
     */
    protected JobInfo executeTask(JobSubmission jobSubmission, ParameterInfo[] params, int taskNum, JobInfo[] results)
	    throws WebServiceException {
	log.debug("Begin executeTask");

	String lsidOrTaskName = jobSubmission.getLSID();
	if (lsidOrTaskName == null || lsidOrTaskName.equals("")) {
	    lsidOrTaskName = jobSubmission.getName();
	}

	log.debug("lsid: " + lsidOrTaskName);

	TaskInfo task = adminClient.getTask(lsidOrTaskName);

	if (task == null) {
	    log.error("Module " + lsidOrTaskName + " not found.");
	    return new JobInfo();
	}
	log.debug("taskInfo: " + task.getName() + ", " + task.getLsid());

	AnalysisService svc = new AnalysisService(server, task);
	AnalysisJob job = submitJob(svc, params);
	JobInfo jobInfo = waitForErrorOrCompletion(job);
	return jobInfo;
    }

    protected void getChildJobOutputs(int childJobID, List<ParameterInfo> outs) {
	try {
	    JobInfo childJobInfo = analysisClient.checkStatus(childJobID);
	    getChildJobOutputs(childJobInfo, outs);
	} catch (WebServiceException e) {
	    log.error(e);
	}

    }

    protected void getChildJobOutputs(JobInfo child, List<ParameterInfo> outs) {
	ParameterInfo[] childParams = child.getParameterInfoArray();
	for (int i = 0; i < childParams.length; i++) {
	    if (childParams[i].isOutputFile()) {
		File f = new File(childParams[i].getValue());
		if (!f.getName().equals(GPConstants.TASKLOG)) {
		    outs.add(childParams[i]);
		    // System.out.println("Adding child output: "+ childParams[i].getValue());
		}
	    }
	}
    }

    protected String getInheritedFilename(Map attributes, JobInfo[] results) throws FileNotFoundException {
        // these params must be removed so that the soap lib doesn't try to send the file as an attachment
        String taskStr = (String) attributes.get(PipelineModel.INHERIT_TASKNAME);
        String fileStr = (String) attributes.get(PipelineModel.INHERIT_FILENAME);
        attributes.remove("TYPE");
        attributes.put(ParameterInfo.MODE, ParameterInfo.URL_INPUT_MODE);

        int task = Integer.parseInt(taskStr);
        JobInfo job = results[task];
        String fileName = getOutputFileName(job, fileStr);

        String context = System.getProperty("GP_Path", "/gp");
        String url = server + context + "/jobResults/" + fileName;
        return url;
    }

    protected ParameterInfo[] removeEmptyOptionalParams(ParameterInfo[] parameterInfo) {
	ArrayList<ParameterInfo> params = new ArrayList<ParameterInfo>();

	for (int i = 0; i < parameterInfo.length; i++) {
	    ParameterInfo aParam = parameterInfo[i];

	    if (aParam.getAttributes() != null) {

		String value = aParam.getValue();

		if (value != null) {
		    if ((value.trim().length() == 0) && aParam.isOptional()) {
			log
				.debug("Removing Param " + aParam.getName() + " has null value. Opt= "
					+ aParam.isOptional());

		    } else {
			params.add(aParam);
		    }
		}

	    }
	}
	return params.toArray(new ParameterInfo[params.size()]);
    }

    protected void setInheritedJobParameters(ParameterInfo[] parameterInfos, JobInfo[] results) throws FileNotFoundException {
        for (ParameterInfo param : parameterInfos) {
            boolean isInheritTaskName = false;
            HashMap attributes = param.getAttributes();
            if (attributes != null) {
                isInheritTaskName = attributes.get(PipelineModel.INHERIT_TASKNAME) != null;
            }
            if (isInheritTaskName) {
                String url = getInheritedFilename(param.getAttributes(), results);
                param.setValue(url);
            }
        }
    }

    /**
     * Substitute '<LSID>' in input files. 
     * This is a special case for steps in a pipeline which use input files from the pipeline or from a previous step in the pipeline.
     *
     * Note: must be called after {@link #setInheritedJobParameters(ParameterInfo[], JobInfo[])}
     * 
     * @param parameterInfos
     */
    protected void substituteLsidInInputFiles(ParameterInfo[] parameterInfos) {
        for (ParameterInfo param : parameterInfos) {
            String value = param.getValue();
            if (value != null && value.startsWith("<GenePatternURL>")) {
                // substitute <LSID> flags for pipeline files
                String lsidTag = "<LSID>";
                String lsidValue = System.getProperty("LSID");
                value = value.replace(lsidTag, lsidValue);
                param.setValue(value);
            }
        }        
    }

    /**
     * Look for parameters that are passed in on the command line and put them into the ParameterInfo array
     */
    protected ParameterInfo[] setJobParametersFromArgs(String name, int taskNum, ParameterInfo[] parameterInfo,
	    JobInfo[] results, Map args) {
	for (int i = 0; i < parameterInfo.length; i++) {
	    ParameterInfo aParam = parameterInfo[i];
	    if (aParam.getAttributes() != null) {
		if (aParam.getAttributes().get(PipelineModel.RUNTIME_PARAM) != null) {
		    aParam.getAttributes().remove(PipelineModel.RUNTIME_PARAM);
		    String key = name + taskNum + "." + aParam.getName();
		    String val = (String) args.get(key);
		    if ((val != null))
			aParam.setValue(val);
		}
	    }
	}
	return parameterInfo;

    }

    /**
     * Notify the server of the pipeline's status (Process, Finished, etc)
     * 
     * @throws WebServiceException
     */
    protected void setStatus(String status) throws WebServiceException {
	if (log.isDebugEnabled()) {
	    log.debug(("Setting job# " + jobId + " status to " + status));
	}
	analysisClient.setJobStatus(jobId, status);
    }

    /**
     * submit a job based on a service and its parameters
     * 
     * @throws WebServiceException
     */
    protected AnalysisJob submitJob(AnalysisService svc, ParameterInfo[] parmInfos) throws WebServiceException {
	if (parmInfos != null) {
	    for (int i = 0; i < parmInfos.length; i++) {
		if (parmInfos[i].isInputFile()) {
		    String file = parmInfos[i].getValue(); // bug 724
		    if (file != null && file.trim().length() != 0) {
			String val = file;
			try {
			    new URL(file);
			} catch (MalformedURLException e) {
			    val = new File(file).toURI().toString();
			}
			parmInfos[i].setValue(val);
			parmInfos[i].getAttributes().remove("TYPE");
			parmInfos[i].getAttributes().remove("MODE");
		    }
		}
	    }
	}

	TaskInfo tinfo = svc.getTaskInfo();
	JobInfo job = analysisClient.submitJob(tinfo.getID(), parmInfos, jobId);
	return new AnalysisJob(svc.getServer(), job);
    }

    /**
     * 
     * @throws WebServiceException
     */
    private JobInfo waitForErrorOrCompletion(AnalysisJob job) throws WebServiceException {
	int maxtries = 100; // used only to increment sleep, not a hard limit
	int sleep = 1000;
	return waitForErrorOrCompletion(job, maxtries, sleep);
    }

    private JobInfo waitForErrorOrCompletion(AnalysisJob job, int maxTries, int initialSleep)
	    throws WebServiceException {

	if (log.isDebugEnabled()) {
	    log.debug("WaitForErrorOrCompletion jobId= " + job.getJobInfo().getJobNumber() + " taskName= "
		    + job.getTaskName());
	}
	String status = "";
	JobInfo jobInfo = null;
	int count = 0;
	int sleep = initialSleep;

	do {
	    count++;
	    try {
		Thread.sleep(sleep);
	    } catch (InterruptedException e) {

	    }
	    int jobID = job.getJobInfo().getJobNumber();
	    log.debug("checking status for job " + jobID + " ...");
	    jobInfo = analysisClient.checkStatus(jobID);
	    status = jobInfo.getStatus();
	    log.debug("   status=" + status);
	    sleep = incrementSleep(initialSleep, maxTries, count);
	} while (!(status.equalsIgnoreCase("ERROR") || (status.equalsIgnoreCase("Finished"))));

	return jobInfo;
    }

    public static String getFileType(File file) {
	// ODF
	if (file.getName().toLowerCase().endsWith("." + GPConstants.ODF.toLowerCase())) {
	    return ODFModelType(file);
	}
	String filename = file.getName();
	return filename.substring(filename.lastIndexOf(".") + 1);

    }

    /**
     * return the file name for the previously run job by index or name
     */
    public static String getOutputFileName(org.genepattern.webservice.JobInfo job, String fileStr)
	    throws FileNotFoundException {
	String fileName = null;
	String fn = null;
	int j;
	ParameterInfo[] jobParams = job.getParameterInfoArray();
	String jobDir = System.getProperty("jobs");
	// try semantic match on output files first

	// For now, just match on filename extension
	semantic_search_loop: for (j = 0; j < jobParams.length; j++) {
	    if (jobParams[j].isOutputFile()) {
		fn = jobParams[j].getValue(); // get the filename
		File aFile = new File(fn);
		if (!aFile.exists()) {
		    aFile = new File("../", fn);
		}
		if (!aFile.exists()) {
		    aFile = new File("../" + jobDir + "/", fn);
		}

		if (isFileType(aFile, fileStr)) {
		    fileName = fn;
		    break semantic_search_loop;
		}
	    }
	}

	if (fileName == null) {
	    // no match on extension, try assuming that it is an integer number
	    // (1..5)
	    try {
		int fileIdx = Integer.parseInt(fileStr);
		// success, find the nth output file
		int jobFileIdx = 1;
		for (j = 0; j < jobParams.length; j++) {
		    if (jobParams[j].isOutputFile()) {
			if (jobFileIdx == fileIdx) {
			    fileName = jobParams[j].getValue();
			    break;
			}
			jobFileIdx++;
		    }
		}
	    } catch (NumberFormatException nfe) {
		// not an extension, not a number, look for stdout or stderr

		// fileStr is stderr or stdout instead of an index
		if (fileStr.equals(GPConstants.STDOUT) || fileStr.equals(GPConstants.STDERR)) {
		    fileName = fileStr;
		}
	    }
	}

	if (fileName != null) {
	    int lastIdx = fileName.lastIndexOf(File.separator);
	    lastIdx = fileName.lastIndexOf(File.separator, lastIdx - 1); // get the job # too

	    if (lastIdx != -1) {
		fileName = fileName.substring(lastIdx + 1);
	    }
	}
	if (fileName == null) {
	    throw new FileNotFoundException("Unable to find output file from job " + job.getJobNumber()
		    + " that matches " + fileStr + ".");
	}
	return fileName;
    }

    public static boolean isFileType(File file, String fileFormat) {
	if (file.getName().toLowerCase().endsWith(".odf")) {
	    return ODFModelType(file).equalsIgnoreCase(fileFormat);
	}
	// when fileFormat does not contain the '.' character, assume that
	// fileFormat
	// refers to a file extension and prepend '.'. For example if value
	// of fileFormat is 'gct',
	// fileFormat becomes '.gct' when testing if file.getName() ends
	// with fileFormat
	// when the file format does contain the '.' character, assume
	// fileFormat can refer to a complete
	// filename (e.g. all_aml_train.gct).
	if (fileFormat.indexOf('.') == -1) {
	    fileFormat = "." + fileFormat;
	}
	return file.getName().toLowerCase().endsWith(fileFormat.toLowerCase());
    }

    /**
     * expects minimum of two args. pipeline name, username, args to pipeline Additionally the system properties jobID,
     * LSID, genepattern.properties are required, while the decorator system property is optional
     * 
     * @throws Exception
     */
    public static void main(String args[]) throws Exception {
	log.debug("working dir: " + new File("test").getAbsolutePath());

	String userKey = "";
	try {
	    Properties additionalArguments = new Properties();
	    String genePatternPropertiesFile = System.getProperty("genepattern.properties") + java.io.File.separator
		    + "genepattern.properties";
	    FileInputStream fis = null;
	    Properties genepatternProps = new Properties();
	    try {
		fis = new FileInputStream(genePatternPropertiesFile);
		genepatternProps.load(fis);
	    } catch (IOException x) {
		log.error("Unable to open properties file.");
	    } finally {
		if (fis != null) {
		    try {
			fis.close();
		    } catch (IOException e) {

		    }
		}
	    }
	    for (Iterator iter = genepatternProps.keySet().iterator(); iter.hasNext();) {
		String key = (String) iter.next();
		String val = genepatternProps.getProperty(key);
		System.setProperty(key, val);
	    }

	    String trustStore = genepatternProps.getProperty("javax.net.ssl.trustStore");
	    if (trustStore != null)
		System.setProperty("javax.net.ssl.trustStore", trustStore);

	    String GP_Path = genepatternProps.getProperty("GP_Path");
	    if (GP_Path != null)
		System.setProperty("GP_Path", GP_Path);
	    Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());

	    //
	    // Probably best to put this code in a function somewhere...
	    //
	    HostnameVerifier hv = new HostnameVerifier() {
		public boolean verify(String urlHostName, SSLSession session) {
		    if (!(urlHostName.equals(session.getPeerHost())) && (System.getProperty("DEBUG") != null))
			System.out.println("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost());
		    return true;
		}
	    };

	    HttpsURLConnection.setDefaultHostnameVerifier(hv);

	    if (args.length < 2) {
		System.out.println("usage: RunPipeline pipelineFile username args");
		System.out.println(java.util.Arrays.asList(args));
		System.exit(1);
	    }
	    if (args.length > 2) {
		for (int i = 2; i < args.length; i++) {
		    // assume args are in the form name=value
		    String arg = args[i];

		    StringTokenizer strtok = new StringTokenizer(arg, "=");
		    String key = strtok.nextToken();
		    StringBuffer valbuff = new StringBuffer("");
		    int count = 0;
		    while (strtok.hasMoreTokens()) {
			valbuff.append(strtok.nextToken());
			if ((strtok.hasMoreTokens()))
			    valbuff.append("=");
			count++;
		    }
		    additionalArguments.put(key, valbuff.toString());
		}
	    }
	    String pipelineLSID = System.getProperty(GPConstants.LSID);
	    // set the tasklib. In the genepattern.properties it may be relative
	    // to tomcat but we are
	    // in a pipeline 2 dirs deeper. If it does not start wit a ".." use
	    // it as it. if it does
	    // start with ".." then add two more directory jumps up
	    String taskLib = genepatternProps.getProperty("tasklib");
	    if (taskLib == null) {
		taskLib = "../../../../../tasklib";
	    } else if (taskLib.startsWith("..")) {
		taskLib = "../../../../" + taskLib;
	    }
	    System.setProperty("tasklib", taskLib);

	    String pipelineFileName = args[0];
	    String userId = args[1];
	    System.setProperty("userId", userId);
	    userKey = System.getProperty(EncryptionUtil.PROP_PIPELINE_USER_KEY, null);
	    if (userKey == null) {
		userKey = System.getenv().get(EncryptionUtil.PROP_PIPELINE_USER_KEY);
	    }
	    log.debug("EncryptionUtil.PROP_PIPELINE_USER_KEY: " + userKey);

	    int jobId = -1;
	    if (System.getProperty("jobID") == null) {
		// null when run using java client
		File dir = new File(System.getProperty("user.dir"));
		jobId = Integer.parseInt(dir.getName());
		System.setProperty("jobID", "" + jobId);
	    } else {
		jobId = Integer.parseInt(System.getProperty("jobID"));
	    }
	    RunPipelineOutputDecoratorIF decorator = null;
	    if (System.getProperty("decorator") != null) {
		String decoratorClass = System.getProperty("decorator");
		decorator = (RunPipelineOutputDecoratorIF) (Class.forName(decoratorClass)).newInstance();
	    }

	    // see StartupServlet.java
	    String gpUrl = System.getProperty("GenePatternURL", "");
	    if (gpUrl == null || gpUrl.trim().length() == 0) {
		// if it is not a system property, check genepattern.properties
		gpUrl = genepatternProps.getProperty("GenePatternURL", "");
		// if it is not set in genepattern.properties, then construct the URL using InetAddress
		if (gpUrl == null || gpUrl.trim().length() == 0) {
		    try {
			InetAddress addr = InetAddress.getLocalHost();
			String host_address = addr.getCanonicalHostName();
			String port = System.getProperty("GENEPATTERN_PORT", "");
			if (port == null) {
			    port = "";
			} else {
			    port = port.trim();
			}
			if (port.length() > 0) {
			    port = ":" + port;
			}
			gpUrl = "http://" + host_address + port + "/gp/";
		    } catch (UnknownHostException e) {
			throw new Exception("Error constructing GenePatternURL from localhost: "
				+ e.getLocalizedMessage(), e);
		    }
		}
		System.setProperty("GenePatternURL", gpUrl);
	    }

	    URL serverFromFile = null;
	    try {
		serverFromFile = new URL(gpUrl);
	    } catch (MalformedURLException e) {
		throw new Exception("Invalid GenePatternURL: " + gpUrl, e);
	    }

	    String host = serverFromFile.getHost();
	    String port = "";
	    int portNum = serverFromFile.getPort();
	    if (portNum >= 0) {
		port = ":" + portNum;
	    }
	    String server = serverFromFile.getProtocol() + "://" + host + port;
	    PipelineModel pipelineModel = getPipelineModel(pipelineFileName, pipelineLSID, server, userId, userKey);
	    if (pipelineModel == null) {
		log.error("Unable to construct pipeline model.");
		System.exit(1);
	    }
	    RunPipelineSoap rp = new RunPipelineSoap(server, userId, userKey, jobId, pipelineModel, decorator);
	    rp.runPipeline(additionalArguments);
	} finally {
	    log.debug("DEBUG=" + System.getProperty("DEBUG"));
	    if (System.getProperty("DEBUG") == null) {
		File logFileInstance = new File(logFile);
		if (logFileInstance.exists()) {
		    log.debug("deleting: " + logFileInstance.getAbsolutePath());
		    logFileInstance.delete();
		}
	    }
	}
    }

    public static String ODFModelType(File file) {
	String model = "";
	BufferedReader inputB = null;
	try {
	    if (!file.exists()) {
		log.error("Can't find " + file.getCanonicalPath());
	    }
	    // System.out.println(file.getCanonicalPath());
	    inputB = new BufferedReader(new FileReader(file));
	    String modelLine = inputB.readLine();
	    while (modelLine != null && !modelLine.startsWith("Model")) {
		modelLine = inputB.readLine();
	    }

	    if (modelLine != null) {
		model = modelLine.substring(modelLine.indexOf("=") + 1).trim();
	    }
	} catch (IOException e) {
	    log.error("Error reading " + file);
	} finally {
	    if (inputB != null) {
		try {
		    inputB.close();
		} catch (IOException x) {
		}
	    }
	}
	return model;
    }

    public static Logger setupLog4jConfig(String logFile) {
	// System.setProperty("DEBUG", "true"); //don't delete logfile

	Properties log4jconfig = new Properties();
	log4jconfig.setProperty("log4j.debug", "false"); // set this to true to debug Log4j configuration
	log4jconfig.setProperty("log4j.rootLogger", "error, R");
	log4jconfig.setProperty("log4j.logger.org.genepattern", "error");

	log4jconfig.setProperty("log4j.appender.R", "org.apache.log4j.RollingFileAppender");
	log4jconfig.setProperty("log4j.appender.R.File", logFile);
	log4jconfig.setProperty("log4j.appender.R.MaxFileSize", "256KB");
	log4jconfig.setProperty("log4j.appender.R.MaxBackupIndex", "2");
	log4jconfig.setProperty("log4j.appender.R.layout", "org.apache.log4j.PatternLayout");
	log4jconfig.setProperty("log4j.appender.R.layout.ConversionPattern",
		"%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [%t] (%F:%L) - %m%n");

	System.setProperty("log4j.defaultInitOverride", "true"); // required to prevent stack trace to System.err
	PropertyConfigurator.configure(log4jconfig);
	return Logger.getLogger(RunPipelineSoap.class);
    }

    protected static void dumpParameters(ParameterInfo[] params, String where) {
	System.out.println("");
	System.out.println(where);
	for (int i = 0; params != null && i < params.length; i++) {
	    System.out.println("RunPipeline.executeTask: " + params[i].getName() + "=" + params[i].toString());
	}
    }

    /**
     * make the sleep time go up as it takes longer to exec. eg for 100 tries of 1000ms (1 sec) first 20 are 1 sec each
     * next 20 are 2 sec each next 20 are 4 sec each next 20 are 8 sec each any beyond this are 16 sec each
     */
    protected static int incrementSleep(int init, int maxTries, int count) {
	if (count < (maxTries * 0.2))
	    return init;
	if (count < (maxTries * 0.4))
	    return init * 2;
	if (count < (maxTries * 0.6))
	    return init * 4;
	if (count < (maxTries * 0.8))
	    return init * 8;
	return init * 16;
    }

    /**
     * the pipelineFileName may be either a local file or a URL. Figure out which it is and get it either way
     * 
     * @throws WebServiceException
     */
    private static PipelineModel getPipelineModel(String pipelineFileName, String lsid, String server, String userId,
	    String password) throws WebServiceException {
	File file = new File(pipelineFileName);
	BufferedReader reader = null;
	PipelineModel model = null;

	try {
	    AdminProxy adminClient = new AdminProxy(server, userId, password);
	    if (!file.exists()) {
		TaskInfo ti = adminClient.getTask(lsid);
		String serializedModel = (String) ti.getTaskInfoAttributes().get(GPConstants.SERIALIZED_MODEL);
		reader = new BufferedReader(new StringReader(serializedModel));
	    } else {
		reader = new BufferedReader(new FileReader(pipelineFileName));
		// file.deleteOnExit();
	    }

	    try {
		model = PipelineModel.toPipelineModel(new InputSource(reader), false, adminClient);
	    } catch (Exception e) {
		return null;
	    }
	    model.setLsid(lsid);
	    return model;
	} catch (IOException e) {
	    log.error("Error", e);
	} finally {
	    if (reader != null) {
		try {
		    reader.close();
		} catch (IOException e) {

		}
	    }
	}
	return model;
    }

}
