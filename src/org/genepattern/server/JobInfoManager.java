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
            
            JobInfoWrapper jobInfoWrapper = processChildren(documentCookie, contextPath, ds, jobInfo);
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
     * Create a new MyJobInfo, recursively looking up and including all child jobs.
     * @param documentCookie
     * @param contextPath
     * @param ds
     * @param jobInfo
     * @return
     */
    private JobInfoWrapper processChildren(String documentCookie, String contextPath, AnalysisDAO ds, JobInfo jobInfo) {
        JobInfoWrapper jobInfoWrapper = new JobInfoWrapper();
        
        //get the visualizer flag
        int taskId = jobInfo.getTaskID();
        AdminDAO ad = new AdminDAO();
        TaskInfo taskInfo = ad.getTask(taskId);

        jobInfoWrapper.setJobInfo(jobInfo);
        jobInfoWrapper.setPipeline(taskInfo.isPipeline());
        jobInfoWrapper.setVisualizer(taskInfo.isVisualizer());
        
        if (taskInfo.isVisualizer()) {
            String tag = createVisualizerAppletTag(documentCookie, contextPath, jobInfo, taskInfo);
            jobInfoWrapper.setVisualizerAppletTag(tag);
        }

        JobInfo[] children = ds.getChildren(jobInfo.getJobNumber());
        for(JobInfo child : children) {
            JobInfoWrapper nextChild = processChildren(documentCookie, contextPath, ds, child);
            jobInfoWrapper.addChildJobInfo(nextChild);
        }
        
        int numSteps = 1;
        if (jobInfoWrapper.isPipeline()) {
            PipelineModel pipelineModel = getPipelineModel(jobInfo);
            numSteps = pipelineModel.getTasks().size();
        }
        jobInfoWrapper.setNumSteps(numSteps);
        
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
    
    public void writeJobInfo(Writer writer, JobInfoWrapper myJobInfo) 
    throws IOException,JSONException
    {
        JSONObject jobInfoObj = convertToJSON(myJobInfo);
        jobInfoObj.write(writer);
    }
    
    private JSONObject convertToJSON(JobInfoWrapper myJobInfo) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("jobNumber", myJobInfo.getJobNumber());
        obj.put("userId", myJobInfo.getUserId());
        obj.put("taskName", myJobInfo.getTaskName());
        obj.put("dateSubmitted", formatDate( myJobInfo.getDateSubmitted() ));
        obj.put("dateCompleted", formatDate( myJobInfo.getDateCompleted() ));
        obj.put("elapsedTime",  myJobInfo.getElapsedTimeMillis());
        obj.put("status", myJobInfo.getStatus());

        obj.put("isPipeline", myJobInfo.isPipeline());
        obj.put("numStepsCompleted", myJobInfo.getNumStepsCompleted());
        obj.put("numSteps", myJobInfo.getNumSteps());

        obj.put("isVisualizer", myJobInfo.isVisualizer());
        if (myJobInfo.isVisualizer()) {
            obj.put("visualizerAppletTag", myJobInfo.getVisualizerAppletTag());
        }
        
        //add input parameters
        for(ParameterInfo inputParam : myJobInfo.getInputParameters()) {
            JSONObject inp = new JSONObject();
            inp.put("name", inputParam.getName());
            inp.put("value", inputParam.getValue());
            inp.put("description", inputParam.getDescription());
            
            obj.accumulate("inputParams", inp);
        }
        
        //add input files
        for(ParameterInfo inputFile : myJobInfo.getInputFiles()) {
            JSONObject inp = new JSONObject();
            inp.put("name", inputFile.getName());
            inp.put("value", inputFile.getValue());
            inp.put("description", inputFile.getDescription());
            
            obj.accumulate("inputFiles", inp);
        }
        
        //add output parameters
        for(ParameterInfo outputParam : myJobInfo.getOutputFiles()) {
            JSONObject inp = new JSONObject();
            inp.put("name", outputParam.getName());
            inp.put("value", outputParam.getValue());
            inp.put("description", outputParam.getDescription());
            
            obj.accumulate("outputParams", inp);
        }
        
        for(JobInfoWrapper child : myJobInfo.getChildren()) {
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
