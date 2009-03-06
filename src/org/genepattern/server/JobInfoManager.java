package org.genepattern.server;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.genepattern.RunVisualizer;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
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
            AnalysisDAO ds = new AnalysisDAO();
            JobInfo jobInfo = ds.getJobInfo(jobNo);
            
            JobInfoWrapper jobInfoWrapper = processChildren((JobInfoWrapper)null, documentCookie, contextPath, ds, jobInfo);

            //this call initializes the helper methods
            jobInfoWrapper.getPathFromRoot();

            ///PermissionsHelper perm = new PermissionsHelper(currentUser, jobNo);
            
            return jobInfoWrapper;
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }
    
    private PipelineModel getPipelineModel(JobInfo jobInfo) {
        PipelineModel model = null;

        int taskId = jobInfo.getTaskID();
        AdminDAO ad = new AdminDAO();
        TaskInfo taskInfo = ad.getTask(taskId);

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
    private JobInfoWrapper processChildren(JobInfoWrapper parent, String documentCookie, String contextPath, AnalysisDAO analysisDao, JobInfo jobInfo) {
        JobInfoWrapper jobInfoWrapper = new JobInfoWrapper();
        jobInfoWrapper.setParent(parent);
        
        //get the visualizer flag
        int taskId = jobInfo.getTaskID();
        AdminDAO ad = new AdminDAO();
        TaskInfo taskInfo = ad.getTask(taskId);
        ParameterInfo[] formalParameters = taskInfo.getParameterInfoArray();

        jobInfoWrapper.setJobInfo(contextPath, formalParameters, jobInfo);
        jobInfoWrapper.setPipeline(taskInfo.isPipeline());
        jobInfoWrapper.setVisualizer(taskInfo.isVisualizer());
        
        if (taskInfo.isVisualizer()) {
            String tag = createVisualizerAppletTag(documentCookie, contextPath, jobInfo, taskInfo);
            jobInfoWrapper.setVisualizerAppletTag(tag);
        }

        JobInfo[] children = analysisDao.getChildren(jobInfo.getJobNumber());
        int idx = 0;
        for(JobInfo child : children) {
            JobInfoWrapper nextChild = processChildren(jobInfoWrapper, documentCookie, contextPath, analysisDao, child);
            jobInfoWrapper.addChildJobInfo(nextChild);
        }
        
        int numSteps = 1;
        if (jobInfoWrapper.isPipeline()) {
            PipelineModel pipelineModel = getPipelineModel(jobInfo);
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
        obj.put("userId", jobInfoWrapper.getUserId());
        obj.put("taskName", jobInfoWrapper.getTaskName());
        obj.put("dateSubmitted", formatDate( jobInfoWrapper.getDateSubmitted() ));
        obj.put("dateCompleted", formatDate( jobInfoWrapper.getDateCompleted() ));
        obj.put("elapsedTime",  jobInfoWrapper.getElapsedTimeMillis());
        obj.put("status", jobInfoWrapper.getStatus());

        obj.put("isPipeline", jobInfoWrapper.isPipeline());
        obj.put("numStepsCompleted", jobInfoWrapper.getNumStepsCompleted());
        obj.put("numSteps", jobInfoWrapper.getNumStepsInPipeline());

        obj.put("isVisualizer", jobInfoWrapper.isVisualizer());
        if (jobInfoWrapper.isVisualizer()) {
            obj.put("visualizerAppletTag", jobInfoWrapper.getVisualizerAppletTag());
        }
        
        //add input parameters
        for(JobInfoWrapper.ParameterInfoWrapper inputParam : jobInfoWrapper.getInputParameters()) {
            JSONObject inp = new JSONObject();
            inp.put("name", inputParam.getName());
            inp.put("value", inputParam.getDisplayValue());
            inp.put("description", inputParam.getDescription());
            
            obj.accumulate("inputParams", inp);
        }
        
        //add input files
        for(JobInfoWrapper.InputFile inputFile : jobInfoWrapper.getInputFiles()) {
            JSONObject inp = new JSONObject();
            inp.put("name", inputFile.getName());
            inp.put("value", inputFile.getDisplayValue());
            inp.put("description", inputFile.getDescription());
            
            obj.accumulate("inputFiles", inp);
        }
        
        //add output files
        for(JobInfoWrapper.OutputFile outputFile : jobInfoWrapper.getOutputFiles()) {
            JSONObject inp = new JSONObject();
            inp.put("name", outputFile.getName());
            inp.put("value", outputFile.getDisplayValue());
            inp.put("link", outputFile.getLink());
            inp.put("description", outputFile.getDescription());
            
            obj.accumulate("outputFiles", inp);
        }

        
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
 
}
