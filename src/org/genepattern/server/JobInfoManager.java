package org.genepattern.server;

import static org.genepattern.util.GPConstants.TASKLOG;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.JobInfoWrapper.ParameterInfoWrapper;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.genepattern.RunVisualizer;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.util.GPConstants;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Get job status information.
 * 
 * @author pcarr
 */
public class JobInfoManager {
    private static Logger log = Logger.getLogger(JobInfoManager.class);

    final static private String dateFormatPattern = "MMM dd hh:mm:ss aa";
    final static private DateFormat df = new SimpleDateFormat(dateFormatPattern);
    
    /**
     * Get the current job status information by doing a db query.
     * 
     * @param documentCookie
     * @param contextPath
     * @param currentUser
     * @param jobNo
     * 
     * @return
     */
    public JobInfoWrapper getJobInfo(String documentCookie, String contextPath, String currentUser, int jobNo) {
        try {
            HibernateUtil.beginTransaction();
            UserDAO userDao = new UserDAO();
            boolean showExecutionLogs = userDao.getPropertyShowExecutionLogs(currentUser);

            AnalysisDAO analysisDao = new AnalysisDAO();
            JobInfo jobInfo = analysisDao.getJobInfo(jobNo);
            
            AdminDAO adminDao = new AdminDAO();
            TaskInfo[] latestTasks = adminDao.getLatestTasks(currentUser);
            Map<String, Collection<TaskInfo>> kindToModules = SemanticUtil.getKindToModulesMap(latestTasks);

            JobInfoWrapper jobInfoWrapper = processChildren((JobInfoWrapper)null, showExecutionLogs, documentCookie, contextPath, analysisDao, adminDao, kindToModules, jobInfo);

            //this call initializes the helper methods
            jobInfoWrapper.getPathFromRoot();

            ///PermissionsHelper perm = new PermissionsHelper(currentUser, jobNo);
            
            return jobInfoWrapper;
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }
    
    private PipelineModel getPipelineModel(AdminDAO adminDao, JobInfo jobInfo) {
        PipelineModel model = null;

        int taskId = jobInfo.getTaskID();
        TaskInfo taskInfo = adminDao.getTask(taskId);

        if ((taskInfo != null) && (model == null)) {
            TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
            if (tia != null) {
                String serializedModel = (String) tia.get(GPConstants.SERIALIZED_MODEL);
                if (serializedModel != null && serializedModel.length() > 0) {
                    try {
                        model = PipelineModel.toPipelineModel(serializedModel);
                    } 
                    catch (Throwable x) {
                        log.error(x);
                    }
                }
            }
        }
        return model;
    }
    
    /**
     * Create a new JobInfoWrapper, recursively looking up and including all child jobs.
     * Link each new JobInfoWrapper to its parent, which is null for top level jobs.
     * 
     * @param parent
     * @param documentCookie
     * @param contextPath
     * @param analysisDao
     * @param jobInfo
     * @return a new JobInfoWrapper
     */
    private JobInfoWrapper processChildren(JobInfoWrapper parent, boolean showExecutionLogs, String documentCookie, String contextPath, AnalysisDAO analysisDao, AdminDAO adminDao, Map<String, Collection<TaskInfo>> kindToModules, JobInfo jobInfo) {
        JobInfoWrapper jobInfoWrapper = new JobInfoWrapper();
        jobInfoWrapper.setParent(parent);
        
        //get the visualizer flag
        int taskId = jobInfo.getTaskID();
        AdminDAO ad = new AdminDAO();
        TaskInfo taskInfo = ad.getTask(taskId);
        ParameterInfo[] formalParameters = taskInfo.getParameterInfoArray();

        jobInfoWrapper.setJobInfo(showExecutionLogs, contextPath, kindToModules, formalParameters, jobInfo);
        jobInfoWrapper.setPipeline(taskInfo.isPipeline());
        jobInfoWrapper.setVisualizer(taskInfo.isVisualizer());
        
        if (taskInfo.isVisualizer()) {
            String tag = createVisualizerAppletTag(documentCookie, contextPath, jobInfo, taskInfo);
            jobInfoWrapper.setVisualizerAppletTag(tag);
        }

        JobInfo[] children = analysisDao.getChildren(jobInfo.getJobNumber());
        for(JobInfo child : children) {
            JobInfoWrapper nextChild = processChildren(jobInfoWrapper, showExecutionLogs, documentCookie, contextPath, analysisDao, adminDao, kindToModules, child);
            jobInfoWrapper.addChildJobInfo(nextChild);
        }
        
        int numSteps = 1;
        if (jobInfoWrapper.isPipeline()) {
            PipelineModel pipelineModel = getPipelineModel(adminDao, jobInfo);
            numSteps = pipelineModel.getTasks().size();
        }
        
        jobInfoWrapper.setNumStepsInPipeline(numSteps);
        return jobInfoWrapper;
    }
    
    private String createVisualizerAppletTag(String documentCookie, String contextPath, JobInfo jobInfo, TaskInfo taskInfo) {
        RunVisualizer runVis = new RunVisualizer();
        runVis.setJobInfo(jobInfo);
        TaskInfoAttributes taskInfoAttributes = taskInfo.giveTaskInfoAttributes();
        runVis.setTaskInfoAttributes(taskInfoAttributes);
        runVis.setContextPath(contextPath);
        runVis.setDocumentCookie(documentCookie);
        StringWriter writer = new StringWriter();
        try {
            runVis.writeVisualizerAppletTag(writer);
            writer.close();
        }
        catch (Exception e) {
            writer.write("<p>Error in getVisualizerAppletTag: "+e.getLocalizedMessage()+"</p>");
        }
        return writer.toString();
    }
    
    public void writeJobInfo(Writer writer, JobInfoWrapper jobInfoWrapper) 
    throws IOException,JSONException
    {
        JSONObject jobInfoObj = convertToJSON(jobInfoWrapper);
        jobInfoObj.write(writer);
    }
    
    private JSONObject convertToJSON(JobInfoWrapper jobInfoWrapper) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("jobNumber", jobInfoWrapper.getJobNumber());
        obj.put("formattedSize", jobInfoWrapper.getFormattedSize());
        obj.put("userId", jobInfoWrapper.getUserId());
        obj.put("taskName", jobInfoWrapper.getTaskName());
        obj.put("dateSubmitted", formatDate( jobInfoWrapper.getDateSubmitted() ));
        obj.put("dateCompleted", formatDate( jobInfoWrapper.getDateCompleted() ));
        obj.put("elapsedTime",  jobInfoWrapper.getElapsedTimeMillis());
        obj.put("status", jobInfoWrapper.getStatus());
        obj.put("numAncestors", jobInfoWrapper.getNumAncestors().length);
        obj.put("stepPath", jobInfoWrapper.getStepPath());
        
        obj.put("isPipeline", jobInfoWrapper.isPipeline());
        obj.put("numStepsCompleted", jobInfoWrapper.getNumStepsCompleted());
        obj.put("numSteps", jobInfoWrapper.getNumStepsInPipeline().length);

        obj.put("isVisualizer", jobInfoWrapper.isVisualizer());
        if (jobInfoWrapper.isVisualizer()) {
            obj.put("visualizerAppletTag", jobInfoWrapper.getVisualizerAppletTag());
        }
        
        //add input parameters
        JSONArray inputParameters = new JSONArray();
        for(JobInfoWrapper.ParameterInfoWrapper inputParam : jobInfoWrapper.getInputParameters()) {
            JSONObject inp = new JSONObject();
            inp.put("name", inputParam.getName());
            inp.put("value", inputParam.getDisplayValue());
            inp.put("description", inputParam.getDescription());
            
            inputParameters.put(inp);
        }
        obj.put("inputParameters", inputParameters);
        
        //add input files
        JSONArray inputFiles = new JSONArray();
        for(JobInfoWrapper.InputFile inputFile : jobInfoWrapper.getInputFiles()) {
            JSONObject inp = new JSONObject();
            inp.put("name", inputFile.getName());
            inp.put("value", inputFile.getDisplayValue());
            inp.put("link", inputFile.getLink());
            inp.put("description", inputFile.getDescription());
            
            inputFiles.put(inp);
        }
        obj.put("inputFiles", inputFiles);
        
        //add output files
        JSONArray outputFiles = new JSONArray();
        for(JobInfoWrapper.OutputFile outputFile : jobInfoWrapper.getOutputFiles()) {
            JSONObject inp = new JSONObject();
            inp.put("name", outputFile.getName());
            inp.put("value", outputFile.getDisplayValue());
            inp.put("link", outputFile.getLink());
            inp.put("description", outputFile.getDescription());
            inp.put("formattedSize", outputFile.getFormattedSize());
            
            outputFiles.put(inp);
        }
        obj.put("outputFiles", outputFiles);
        
        
        for(JobInfoWrapper child : jobInfoWrapper.getChildren()) {
            JSONObject childObj = convertToJSON(child);
            obj.accumulate("children", childObj);
        }
        return obj;
    }
    
    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return df.format(date);
    }
    
    public static File writeExecutionLog(String outDirName, JobInfoWrapper jobInfoWrapper, Properties props) {
        File outDir = new File(outDirName);
        File gpExecutionLog = new File(outDir, TASKLOG);
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(gpExecutionLog));
            writeExecutionLog(writer, jobInfoWrapper, props);
            return gpExecutionLog;
        } 
        catch (IOException e) {
            log.error("Unable to create gp_execution_log: "+gpExecutionLog.getAbsolutePath(), e);
            return null;
        } 
        finally {
            if (writer != null) {
                try {
                    writer.close();
                } 
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public static void writeExecutionLog(Writer writer, JobInfoWrapper jobInfoWrapper, Properties props) 
    throws IOException
    {
        writer.write("# Created: " + new Date() + " by " + jobInfoWrapper.getUserId());
        writer.write("\n# Job: " + jobInfoWrapper.getJobNumber());

        String GP_URL = System.getProperty("GenePatternURL");
        if (GP_URL != null) {
            writer.write("    server:  ");
            writer.write(GP_URL);
        }
        writer.write("\n# Module: " + jobInfoWrapper.getTaskName() + " " + jobInfoWrapper.getTaskLSID());
        writer.write("\n# Parameters: ");

        //case 2: pattern match for uploaded input file
        final String matchFileUploadPrefix = jobInfoWrapper.getServletContextPath() + "/getFile.jsp?file=";
        
        for(ParameterInfoWrapper inputParam : jobInfoWrapper.getInputParameters()) {
            writer.write("\n#    " + inputParam.getName() + " = ");

            String link = inputParam.getLink();
            if(link == null) {
                String displayValue = inputParam.getDisplayValue();
                String value = inputParam.getValue();
                String substitutedValue = GenePatternAnalysisTask.substitute(value, props, null);
                // bug 899 perform command line substitutions
                if (substitutedValue != null && !(value.equals(substitutedValue))) {
                    displayValue = substitutedValue + " (" + value + ")";
                }
                writer.write(displayValue);
            }
            //special case for input files
            else {
                //case 1: an external URL
                if (link.equals(inputParam.getDisplayValue())) {
                    writer.write(link);
                }
                //case 2: an uploaded input file
                else if (link.startsWith( matchFileUploadPrefix )) {
                    link = link.substring(jobInfoWrapper.getServletContextPath().length(), link.length());
                    if (GP_URL.endsWith("/")) {
                        link = link.substring(1);
                    }
                    writer.write(inputParam.getDisplayValue() + "   " + GP_URL + link);
                }
                //case 3: an input which is part of the pipeline created from an output of a previous job with an uploaded file
                //case 4: an output from a previous step in a pipeline
                else {
                    writer.write(inputParam.getDisplayValue() + "   " + link);                    
                }
            }
        }
        writer.write("\n");
    }

}
