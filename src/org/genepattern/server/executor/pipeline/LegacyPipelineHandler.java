package org.genepattern.server.executor.pipeline;

import static org.genepattern.util.GPConstants.STDERR;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.JobManager;
import org.genepattern.server.JobManager.JobSubmissionException;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.executor.pipeline.RunPipelineAsynchronously.MissingTasksException;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.webservice.server.AdminService;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * Refactor code from GP 3.2.3 PipelineExecutor to implement the PipelineHandler interface.
 * 
 * @author pcarr
 */
public class LegacyPipelineHandler implements PipelineHandler {
    private static Logger log = Logger.getLogger(LegacyPipelineHandler.class);

    public void startPipeline(String[] commandLine, JobInfo pipelineJobInfo, int stopAfterTask) throws Exception {
        try {
            String userId = pipelineJobInfo.getUserId();
            int pipelineJobId = pipelineJobInfo.getJobNumber();
            int pipelineTaskId = pipelineJobInfo.getTaskID();
            PipelineModel pipelineModel = PipelineUtil.getPipelineModel(pipelineTaskId);
            Map additionalArgs = getAdditionalArgs(commandLine);
            runPipeline(pipelineJobId, userId, pipelineModel, stopAfterTask, additionalArgs);
            HibernateUtil.commitTransaction();
        }
        catch (Throwable t) {
            HibernateUtil.rollbackTransaction();
            throw new Exception("Server error starting pipeline, for job #"+pipelineJobInfo.getJobNumber(), t);
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }

    public void handleJobCompletion(int jobId) {
        // TODO Auto-generated method stub
        
    }

    public void completePipeline(int pipelineJobId, String stdoutFilename, String stderrFilename, int exitCode, StringBuffer stderrBuffer) {
        recordPipelineJobCompletion(pipelineJobId, stdoutFilename, stderrFilename, exitCode, stderrBuffer);
    }
    
    private Map getAdditionalArgs(String[] commandTokens) {
        Properties additionalArguments = new Properties();
        //HACK: remove all args up to org.genepattern.server.webapp.RunPipelineSoap
        List<String> modifiedCommandTokens = new ArrayList<String>();
        int startIdx = 0;
        for(int i=0; i<commandTokens.length; ++i) {
            if ("org.genepattern.server.webapp.RunPipelineSoap".equals(commandTokens[i])) {
                startIdx = i+1;
                break;
            }
        }
        for(int i=startIdx; i<commandTokens.length; ++i) {
            modifiedCommandTokens.add(commandTokens[i]);
        }
        String[] args = new String[modifiedCommandTokens.size()];
        args = modifiedCommandTokens.toArray(args);
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
        return additionalArguments;
    }

    /**
     * Hack: This method was originally called in a loop 
     * Call this before running the next step in the 
     *
     * @param jobInfo
     */
    public static JobInfo prepareNextStep(int parentJobId, JobInfo jobInfo) {
        log.error("method not implemented");
        return jobInfo;
    }
    
    
    private void runPipeline(int pipelineJobId, String userID, PipelineModel model, int stopAfterTask, Map additionalArgs) 
    throws MissingTasksException, WebServiceException, JobSubmissionException
    {  
        checkForMissingTasks(userID, model);

        int stepNum = 0;
        Vector<JobSubmission> tasks = model.getTasks();
        JobInfo[] results = new JobInfo[tasks.size()];
        for(JobSubmission jobSubmission : tasks) { 
            if (stepNum >= stopAfterTask) {
                break; // stop and execute no further
            }
            
            //TODO: need to run this after each completed step, so we do need to make this part of the generic job submission process
            //    e.g. from GPAT, just before getting the command executor, we need to set inherited job parameters
            ParameterInfo[] parameterInfo = jobSubmission.giveParameterInfoArray();
            try {
                setInheritedJobParameters(parameterInfo, results);
            }
            catch (FileNotFoundException e) {
                String errorMessage = "Execution for " + jobSubmission.getName() + " module failed: "+e.getMessage();
                throw new WebServiceException(errorMessage, e);
            }

            //TODO: verify that model.getLsid() is the correct replacement for substituteLsidInInputFiles(pipelineTaskLsid, parameterInfo);
            substituteLsidInInputFiles(model.getLsid(), parameterInfo);
            ParameterInfo[] params = parameterInfo;
            params = setJobParametersFromArgs(jobSubmission.getName(), stepNum + 1, params, additionalArgs);
            params = removeEmptyOptionalParams(parameterInfo);

            //TODO: add the task to the queue, with status == 'WAIT', 
            //TODO: insert into the JOB_DEPENDENCY table
            //JobInfo taskResult = executeTask(jobSubmission, params);
            JobInfo submittedJob = addJobToPipeline(pipelineJobId, userID, jobSubmission, params);
            results[stepNum] = submittedJob;
            
            //TODO: call collectChildJobResults in the callback upon completion of each job
            //taskResult = collectChildJobResults(taskResult);
            //results[stepNum] = taskResult;
            
//            if (this.isPipelineTerminated) {
//                this.exitCode = -1;
//                this.jobStatus = JobStatus.JOB_ERROR;
//                return;
//            }
//            
//            if (JobStatus.ERROR.equals(taskResult.getStatus())) {
//                //halt pipeline execution if one of the steps fails
//                String errorMessage = "Error in pipeline step " + (stepNum + 1) + ": ";
//                if (taskResult != null) {
//                    errorMessage += (taskResult.getTaskName()+" [id: "+taskResult.getJobNumber()+"]");
//                }
//                else {
//                    errorMessage += " taskResult==null, "+jobSubmission.getName() + " (" + jobSubmission.getLSID() + ")";
//                }
//                throw new WebServiceException(errorMessage);
//            }
            ++stepNum;
        }
//        this.exitCode = 0;
//        this.jobStatus = JobStatus.JOB_FINISHED;
        //}
        //catch (InterruptedException e) {
        //    log.debug("handle thread interruption while the pipeline is still running");
        //    this.exitCode = -1;
        //    this.jobStatus = JobStatus.JOB_ERROR;
        //}
    }
    
    //private void addJobToPipeline(JobSubmission jobSubmission, ParameterInfo params, JobStatus initialStatus, List<Integer> waitingForJobs ) {
    //    
    //}

    
    /**
     * submit the job and wait for it to complete
     * 
     * @throws WebServiceException
     */
    private JobInfo addJobToPipeline(int parentJobId, String userID, JobSubmission jobSubmission, ParameterInfo[] params)
    throws WebServiceException, JobSubmissionException 
    {
        log.debug("Begin executeTask");
        if (jobSubmission == null) {
            log.error("ignoring executeTask, jobSubmission is null");
            return null;
        }

        String lsidOrTaskName = jobSubmission.getLSID();
        if (lsidOrTaskName == null || lsidOrTaskName.equals("")) {
            lsidOrTaskName = jobSubmission.getName();
        }

        int taskId = 0;
        if (jobSubmission.getTaskInfo() != null) {
            taskId = jobSubmission.getTaskInfo().getID();
        }
        if (taskId <= 0) {
            log.error("jobSubmission.taskInfo.ID not set ... calling adminService.getTask("+lsidOrTaskName+")");
            //try {
                AdminService adminService = new AdminService(userID);
                TaskInfo task = adminService.getTask(lsidOrTaskName);
                if (task == null) {
                    log.error("Module " + lsidOrTaskName + " not found.");
                    return new JobInfo();
                }
                taskId = task.getID();
                log.debug("taskInfo: " + task.getName() + ", " + task.getLsid());
            //}
            //finally {
            //    HibernateUtil.closeCurrentSession();
            //}
        }

        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (params[i].isInputFile()) {
                    String file = params[i].getValue(); // bug 724
                    if (file != null && file.trim().length() != 0) {
                        String val = file;
                        try {
                            new URL(file);
                        } 
                        catch (MalformedURLException e) {
                            val = new File(file).toURI().toString();
                        }
                        params[i].setValue(val);
                        params[i].getAttributes().remove("TYPE");
                        params[i].getAttributes().remove("MODE");
                    }
                }
            }
        }
        
        JobInfo jobInfo = JobManager.addJobToQueue(taskId, userID, params, parentJobId);
        //LocalAnalysisClient analysisClient = new LocalAnalysisClient(userID);
        //try { 
        //    //TODO: update anlysisClient so that WAITING job status can be set
        //    jobInfo = analysisClient.submitJob(taskId, params, parentJobId);
       // }
        //finally {
        //    HibernateUtil.closeCurrentSession();
        //}
        //if (jobInfo == null || "ERROR".equalsIgnoreCase(jobInfo.getStatus())) {
        //    log.error("Unexpected error in execute task: taskNum="+taskId+", lsidOrTaskName="+lsidOrTaskName);
        //    return jobInfo;
        //}
        return jobInfo;
    }

    
    private static void checkForMissingTasks(String userID, PipelineModel model) throws MissingTasksException {
        MissingTasksException ex = null;
        //try { 
            //adminService constructor starts a DB transaction
            AdminService adminService = new AdminService(userID);
            for(JobSubmission jobSubmission : model.getTasks()) {
                String lsid = jobSubmission.getLSID();
                TaskInfo taskInfo = null;
                try {
                    taskInfo = adminService.getTask(lsid);
                }
                catch (Exception e) {
                }
                if (taskInfo == null) {
                    if (ex == null) {
                        ex = new MissingTasksException();
                    }
                    ex.addError(jobSubmission);
                }
            }
            if (ex != null) {
                throw ex;
            }
        //}
        //finally {
        //    HibernateUtil.closeCurrentSession();
        //}
    }
    
    private void setInheritedJobParameters(ParameterInfo[] parameterInfos, JobInfo[] results) throws FileNotFoundException {
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
    
    private String server = null;
    private String getServer() {
        if (server != null) {
            return server;
        }
        //1) set server
        String gpUrl = System.getProperty("GenePatternURL");
        URL serverFromFile = null;
        try {
            serverFromFile = new URL(gpUrl);
        } 
        catch (MalformedURLException e) {
            log.error("Invalid GenePatternURL: " + gpUrl, e);
        }
        String host = serverFromFile.getHost();
        String port = "";
        int portNum = serverFromFile.getPort();
        if (portNum >= 0) {
            port = ":" + portNum;
        }
        server = serverFromFile.getProtocol() + "://" + host + port;
        return server;
    }

    private String getInheritedFilename(Map attributes, JobInfo[] results) throws FileNotFoundException {
        // these params must be removed so that the soap lib doesn't try to send the file as an attachment
        String taskStr = (String) attributes.get(PipelineModel.INHERIT_TASKNAME);
        String fileStr = (String) attributes.get(PipelineModel.INHERIT_FILENAME);
        attributes.remove("TYPE");
        attributes.put(ParameterInfo.MODE, ParameterInfo.URL_INPUT_MODE);

        int task = Integer.parseInt(taskStr);
        JobInfo job = results[task];
        String fileName = getOutputFileName(job, fileStr);

        String context = System.getProperty("GP_Path", "/gp");
        String url = getServer() + context + "/jobResults/" + fileName;
        return url;
    }

    /**
     * return the file name for the previously run job by index or name
     */
    private static String getOutputFileName(JobInfo job, String fileStr) throws FileNotFoundException {
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

    private static boolean isFileType(File file, String fileFormat) {
        if (file.getName().toLowerCase().endsWith(".odf")) {
            return ODFModelType(file).equalsIgnoreCase(fileFormat);
        }
        // when fileFormat does not contain the '.' character, 
        // assume that fileFormat refers to a file extension and prepend '.'. 
        // For example if value of fileFormat is 'gct', fileFormat becomes '.gct' when testing if file.getName() ends with fileFormat
        // when the file format does contain the '.' character, 
        // assume fileFormat can refer to a complete filename (e.g. all_aml_train.gct).
        if (fileFormat.indexOf('.') == -1) {
            fileFormat = "." + fileFormat;
        }
        return file.getName().toLowerCase().endsWith(fileFormat.toLowerCase());
    }

    private static String ODFModelType(File file) {
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
        } 
        catch (IOException e) {
            log.error("Error reading " + file);
        } 
        finally {
            if (inputB != null) {
                try {
                    inputB.close();
                } 
                catch (IOException x) {
                }
            }
        }
        return model;
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
    private void substituteLsidInInputFiles(String lsidValue, ParameterInfo[] parameterInfos) {
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
    
    private ParameterInfo[] removeEmptyOptionalParams(ParameterInfo[] parameterInfo) {
        ArrayList<ParameterInfo> params = new ArrayList<ParameterInfo>();
        for (int i = 0; i < parameterInfo.length; i++) {
            ParameterInfo aParam = parameterInfo[i];
            if (aParam.getAttributes() != null) {
                String value = aParam.getValue();
                if (value != null) {
                    if ((value.trim().length() == 0) && aParam.isOptional()) {
                        log.debug("Removing Param " + aParam.getName() + " has null value. Opt= " + aParam.isOptional());
                    } 
                    else {
                        params.add(aParam);
                    }
                }
            }
        }
        return params.toArray(new ParameterInfo[params.size()]);
    }
    
    /**
     * Look for parameters that are passed in on the command line and put them into the ParameterInfo array
     */
    private ParameterInfo[] setJobParametersFromArgs(
            String name, 
            int taskNum, //this is the step number
            ParameterInfo[] parameterInfo, 
            Map args) 
    {
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
     * called to notify the gp server that a pipeline job has completed.
     */
    private void recordPipelineJobCompletion(int rootJobId, String stdoutFilename, String stderrFilename, int exitCode, StringBuffer stderrBuffer) {
        //output stderrBuffer to STDERR file
        int jobStatus = JobStatus.JOB_FINISHED;
        if (stderrBuffer != null && stderrBuffer.length() > 0) {
            jobStatus = JobStatus.JOB_ERROR;
            if (exitCode == 0) {
                exitCode = -1;
            }
            String outDirName = GenePatternAnalysisTask.getJobDir(Integer.toString(rootJobId));
            GenePatternAnalysisTask.writeStringToFile(outDirName, STDERR, stderrBuffer.toString());
        }

        try {
            GenePatternAnalysisTask.handleJobCompletion(rootJobId, stdoutFilename, stderrFilename, exitCode, jobStatus, GenePatternAnalysisTask.JOB_TYPE.PIPELINE);
        }
        catch (Exception e) {
            log.error("Error handling job completion for pipeline: "+rootJobId, e);
        }
    }

}