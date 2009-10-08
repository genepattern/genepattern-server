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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * Copied from RunPipelineSoap, for executing each pipeline in its own thread on the server JVM, 
 * instead of exec'ing in a new JVM.
 * 
 * Additionally, removed the SOAP client calls.
 * 
 * @author pcarr
 */
public class RunPipelineInThread {
    private static final String logFile = "pipelineErrors.log"; // one log file per pipeline
    private static final Logger log = setupLog4jConfig(logFile);

    private String userID;
    private String server;
    
    /** job id for the pipeline */
    private int jobId;
    // was: System.getProperty(GPConstants.LSID)
    private String pipelineTaskLsid;
    private PipelineModel model;
    private Map additionalArgs;
    // was: System.getProperty(GPConstants.PIPELINE_ARG_STOP_AFTER_TASK_NUM);
    private String stopAfterTaskStr = null;
    
    private LocalAnalysisClient analysisClient;
    
    public RunPipelineInThread() {
    }
    
    public void setUserId(String userId) {
        this.userID = userId;
    }
    
    public void setServer(String server) {
        this.server = server;
    }
    
    public void setJobId(int jobId) {
        this.jobId = jobId;
    }
    
    public void setPipelineTaskLsid(String lsid) {
        this.pipelineTaskLsid = lsid;
    }

    public void setPipelineModel(PipelineModel pm) {
        this.model = pm;
    }
    
    public void setAdditionalArgs(Map args) {
        this.additionalArgs = args;
    }

    public void setStopAfterTaskNum(String stopAfterTaskStr) {
        this.stopAfterTaskStr = stopAfterTaskStr;
    }

    public RunPipelineInThread(String server, String userID, String cmdLinePassword, int jobId, PipelineModel model) {
        this.userID = userID;
        this.jobId = jobId;
        this.model = model;
    }

    public void runPipeline() throws WebServiceException {
        this.analysisClient = new LocalAnalysisClient(userID);
        //this.adminClient = new AdminService();

        int stopAfterTask = Integer.MAX_VALUE;
        if (stopAfterTaskStr != null && stopAfterTaskStr.trim().length() > 0) {
            try {
                stopAfterTask = Integer.parseInt(stopAfterTaskStr);
            } 
            catch (NumberFormatException nfe) {
                log.error("Ignoring invalid number format for: stopAfterTaskStr=" + stopAfterTaskStr, nfe);
            }
        }

//        boolean okayToRun = true;
//        StringBuffer errorMessages = null;
//        for(JobSubmission jobSubmission : model.getTasks()) {
//            TaskInfo taskInfo = adminClient.getTask(jobSubmission.getLSID());
//            if (taskInfo == null) {
//                okayToRun = false;
//                String errorMessage = "No such module " + jobSubmission.getName() + " (" + jobSubmission.getLSID() + ")";
//                log.error(errorMessage);
//                if (errorMessages == null) {
//                    errorMessages = new StringBuffer(errorMessage);
//                }
//                else {
//                    errorMessages.append(", "+errorMessage);
//                }
//            }
//        }
//        if (!okayToRun) {
//            throw new IllegalArgumentException(errorMessages.toString());
//        }

        JobInfo results[] = new JobInfo[model.getTasks().size()];
        int stepNum = 0;
        for(JobSubmission jobSubmission : model.getTasks()) { 
            if (stepNum >= stopAfterTask) {
                break; // stop and execute no further
            }
            ParameterInfo[] parameterInfo = jobSubmission.giveParameterInfoArray();
            try {
                setInheritedJobParameters(parameterInfo, results);
            }
            catch (FileNotFoundException e) {
                String errorMessage = "Execution for " + jobSubmission.getName() + " module failed: "+e.getMessage();
                throw new WebServiceException(errorMessage, e);
            }

            substituteLsidInInputFiles(pipelineTaskLsid, parameterInfo);
            ParameterInfo[] params = parameterInfo;
            params = setJobParametersFromArgs(jobSubmission.getName(), stepNum + 1, params, results, additionalArgs);
            params = removeEmptyOptionalParams(parameterInfo);
                
            JobInfo taskResult = executeTask(jobSubmission, params, results);
            taskResult = collectChildJobResults(taskResult);
            results[stepNum] = taskResult;
                    
            if (JobStatus.ERROR.equals(taskResult.getStatus())) {
                //halt pipeline execution if one of the steps fails
                throw new WebServiceException("Error in pipeline step " + (stepNum + 1) + ": "+ taskResult.getTaskName()+" [id: "+taskResult.getJobNumber()+"]");
            }
            ++stepNum;
        }
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
                return taskResult;
            }
            JobInfo[] children = analysisClient.getChildren(taskResult.getJobNumber());
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
        } 
        catch (Exception wse) {
            wse.printStackTrace();
        }
        return taskResult;
    }

    /**
     * submit the job and wait for it to complete
     * 
     * @throws WebServiceException
     */
    protected JobInfo executeTask(JobSubmission jobSubmission, ParameterInfo[] params, JobInfo[] results)
    throws WebServiceException {
        log.debug("Begin executeTask");
        if (jobSubmission == null) {
            log.error("ignoring executeTask, jobSubmissions is null");
            return null;
        }
        TaskInfo taskInfo = jobSubmission.getTaskInfo();
        if (taskInfo == null) {
            log.error("ignoring executeTask, jobSubmission.tasKInfo is null");
            return null;
        }
        int taskNum = taskInfo.getID();
        String lsidOrTaskName = jobSubmission.getLSID();
        if (lsidOrTaskName == null || lsidOrTaskName.equals("")) {
            lsidOrTaskName = jobSubmission.getName();
        }
        
        JobInfo jobInfo = analysisClient.submitJob(taskNum, params, jobId);
        if (jobInfo == null || "ERROR".equalsIgnoreCase(jobInfo.getStatus())) {
            log.error("Unexpected error in execute task: taskNum="+taskNum+", lsidOrTaskName="+lsidOrTaskName);
            return jobInfo;
        }
        jobInfo = waitForErrorOrCompletion(jobInfo.getJobNumber());
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
     * Note: this substitution used to depend on a call to System.setProperty(LSID)
     *    This won't work now that pipelines are no longer executing in their own JVM 
     *    This property seems to have been set in GenePatternAnalysisTask 
     *    For some reason, pipelineModel.getLsid() is null
     *
     * Note: must be called after {@link #setInheritedJobParameters(ParameterInfo[], JobInfo[])}
     * 
     * @param parameterInfos
     */
    protected void substituteLsidInInputFiles(String lsidValue, ParameterInfo[] parameterInfos) {
        final String lsidTag = "<LSID>";
        final String gpUrlTag = "<GenePatternURL>";
        for (ParameterInfo param : parameterInfos) {
            String value = param.getValue();
            if (value != null && value.startsWith(gpUrlTag)) {
                // substitute <LSID> flags for pipeline files
                value = value.replace(lsidTag, lsidValue);
                param.setValue(value);
            }
        }        
    }

    /**
     * Look for parameters that are passed in on the command line and put them into the ParameterInfo array
     */
    protected ParameterInfo[] setJobParametersFromArgs(String name, int taskNum, ParameterInfo[] parameterInfo, JobInfo[] results, Map args) {
        for (int i = 0; i < parameterInfo.length; i++) {
            ParameterInfo aParam = parameterInfo[i];
            HashMap attributes = aParam.getAttributes();
            if (attributes != null) {
                if (attributes.get(PipelineModel.RUNTIME_PARAM) != null) {
                    attributes.remove(PipelineModel.RUNTIME_PARAM);
                    String key = name + taskNum + "." + aParam.getName();
                    String val = (String) args.get(key);                    
                    if ((val != null)) {
                        
                        //We don't want to double prefix the arguments.  If this RunPipelineSoap was
                        //run from the GenePattern webpage, the arguments will have been prefixed as
                        //the "run pipeline" job was run to get us here.
                        if (attributes.containsKey(GPConstants.PARAM_INFO_PREFIX[0])){
                            String prefix = (String) attributes.get(GPConstants.PARAM_INFO_PREFIX[0]);
                            if (val.startsWith(prefix)){
                                val = val.substring(prefix.length());
                            }
                        }
                        
                        aParam.setValue(val);
                    }
                }
            }
        }
        return parameterInfo;
    }

    /**
     * 
     * @throws WebServiceException
     */
    private JobInfo waitForErrorOrCompletion(int jobNumber) throws WebServiceException {
        int maxtries = 100; // used only to increment sleep, not a hard limit
        int sleep = 1000;
        return waitForErrorOrCompletion(jobNumber, maxtries, sleep);
    }

    private JobInfo waitForErrorOrCompletion(int jobNumber, int maxTries, int initialSleep) throws WebServiceException {
        String status = "";
        JobInfo jobInfo = null;
        int count = 0;
        int sleep = initialSleep;
        do {
            count++;
            try {
                Thread.sleep(sleep);
            } 
            catch (InterruptedException e) {
            }
            jobInfo = analysisClient.checkStatus(jobNumber);
            status = jobInfo.getStatus();
            sleep = incrementSleep(initialSleep, maxTries, count);
        } 
        while (!(status.equalsIgnoreCase("ERROR") || (status.equalsIgnoreCase("Finished"))));
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
    public static String getOutputFileName(JobInfo job, String fileStr) throws FileNotFoundException {
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
                if (!aFile.exists()) {
                    aFile = new File(jobDir + "/", fn);
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
            } 
            catch (NumberFormatException nfe) {
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
            throw new FileNotFoundException("Unable to find output file from job " + job.getJobNumber() + " that matches " + fileStr + ".");
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
        Properties log4jconfig = new Properties();
        log4jconfig.setProperty("log4j.debug", "false"); // set this to true to debug Log4j configuration
        log4jconfig.setProperty("log4j.rootLogger", "error, R");
        log4jconfig.setProperty("log4j.logger.org.genepattern", "error");

        log4jconfig.setProperty("log4j.appender.R", "org.apache.log4j.RollingFileAppender");
        log4jconfig.setProperty("log4j.appender.R.File", logFile);
        log4jconfig.setProperty("log4j.appender.R.MaxFileSize", "256KB");
        log4jconfig.setProperty("log4j.appender.R.MaxBackupIndex", "2");
        log4jconfig.setProperty("log4j.appender.R.layout", "org.apache.log4j.PatternLayout");
        log4jconfig.setProperty("log4j.appender.R.layout.ConversionPattern", "%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [%t] (%F:%L) - %m%n");

        //System.setProperty("log4j.defaultInitOverride", "true"); // required to prevent stack trace to System.err
        PropertyConfigurator.configure(log4jconfig);
        return Logger.getLogger(RunPipelineInThread.class);
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


}
