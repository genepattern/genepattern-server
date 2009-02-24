package org.genepattern.server;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.genepattern.RunVisualizer;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
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
    final static private String dateFormatPattern = "MMM dd hh:mm:ss aa";
    final static private DateFormat df = new SimpleDateFormat(dateFormatPattern);
    
    public static class MyJobInfo {
        private JobInfo jobInfo;
        private List<ParameterInfo> inputParameters = new ArrayList<ParameterInfo>();
        private List<ParameterInfo> outputParameters= new ArrayList<ParameterInfo>();
        private List<MyJobInfo> children = new ArrayList<MyJobInfo>();
        
        private boolean isPipeline = false;
        private boolean isVisualizer = false;
        private String visualizerAppletTag = "";

        public JobInfo getJobInfo() {
            return jobInfo;
        }

        public void setJobInfo(JobInfo j) {
            this.jobInfo = j;
            processParameterInfoArray();
        }

        public boolean isPipeline() {
            return isPipeline;
        }

        public boolean isVisualizer() {
            return isVisualizer;
        }
        
        public String getVisualizerAppletTag() {
            return visualizerAppletTag;
        }

        public List<ParameterInfo> getInputParameters() {
            return inputParameters;
        }
        
        public List<ParameterInfo> getOutputParameters() {
            return outputParameters;
        }
        
        public List<MyJobInfo> getChildren() {
            return children;
        }
        
        public void addChildJobInfo(MyJobInfo j) {
            children.add(j);
        }
        
        /**
         * Parse the ParameterInfo array from the XML file stored in the DB
         * and convert into input and output objects for display in the UI.
         */
        private void processParameterInfoArray() {
            for(ParameterInfo param : jobInfo.getParameterInfoArray()) {
                if (param.isOutputFile()) {
                    outputParameters.add(param);
                }
                else {
                    inputParameters.add(param);
                }
            }
            this.jobInfo.getParameterInfoArray();
        }
    }

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
    public MyJobInfo getJobInfo(String documentCookie, String contextPath, String currentUser, int jobNo) {
        try {
            HibernateUtil.beginTransaction();
            AnalysisDAO ds = new AnalysisDAO();
            JobInfo jobInfo = ds.getJobInfo(jobNo);
            
            MyJobInfo myJobInfo = processChildren(documentCookie, contextPath, ds, jobInfo);
            PermissionsHelper perm = new PermissionsHelper(currentUser, jobNo);
            return myJobInfo;
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }
    
    /**
     * Create a new MyJobInfo, recursively looking up and including all child jobs.
     * @param documentCookie
     * @param contextPath
     * @param ds
     * @param jobInfo
     * @return
     */
    private MyJobInfo processChildren(String documentCookie, String contextPath, AnalysisDAO ds, JobInfo jobInfo) {
        MyJobInfo j = new MyJobInfo();
        j.setJobInfo(jobInfo);
        
        //get the visualizer flag
        int taskId = jobInfo.getTaskID();
        AdminDAO ad = new AdminDAO();
        TaskInfo taskInfo = ad.getTask(taskId);
        j.isPipeline = taskInfo.isPipeline();
        j.isVisualizer = taskInfo.isVisualizer();
        
        if (taskInfo.isVisualizer()) {
            j.visualizerAppletTag = getVisualizerAppletTag(documentCookie, contextPath, jobInfo, taskInfo);
        }

        JobInfo[] children = ds.getChildren(jobInfo.getJobNumber());
        for(JobInfo child : children) {
            MyJobInfo nextChild = processChildren(documentCookie, contextPath, ds, child);
            j.addChildJobInfo(nextChild);
        }
        
        return j;
    }
    
    private String getVisualizerAppletTag(String documentCookie, String contextPath, JobInfo jobInfo, TaskInfo taskInfo) {
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
    
    public void writeJobInfo(Writer writer, MyJobInfo myJobInfo) 
    throws IOException,JSONException
    {
        JSONObject jobInfoObj = convertToJSON(myJobInfo);
        jobInfoObj.write(writer);
    }
    
    private JSONObject convertToJSON(MyJobInfo myJobInfo) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("jobNumber", myJobInfo.getJobInfo().getJobNumber());
        obj.put("userId", myJobInfo.getJobInfo().getUserId());
        obj.put("taskName", myJobInfo.getJobInfo().getTaskName());
        obj.put("dateSubmitted", formatDate( myJobInfo.getJobInfo().getDateSubmitted() ));
        obj.put("dateCompleted", formatDate( myJobInfo.getJobInfo().getDateCompleted() ));
        obj.put("elapsedTime", (myJobInfo.getJobInfo().getDateCompleted() == null ? new Date().getTime() : myJobInfo.getJobInfo().getDateCompleted().getTime()) - myJobInfo.getJobInfo().getDateSubmitted().getTime());
        obj.put("status", myJobInfo.getJobInfo().getStatus());
        obj.put("isPipeline", myJobInfo.isPipeline());
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
        
        //add output parameters
        for(ParameterInfo outputParam : myJobInfo.getOutputParameters()) {
            JSONObject inp = new JSONObject();
            inp.put("name", outputParam.getName());
            inp.put("value", outputParam.getValue());
            inp.put("description", outputParam.getDescription());
            
            obj.accumulate("outputParams", inp);
        }
        
        for(MyJobInfo child : myJobInfo.getChildren()) {
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
