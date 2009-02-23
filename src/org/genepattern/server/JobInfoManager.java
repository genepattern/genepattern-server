package org.genepattern.server;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Get job status information.
 * 
 * @author pcarr
 */
public class JobInfoManager {
    public static class MyJobInfo {
        private JobInfo jobInfo;
        private List<ParameterInfo> inputParameters = new ArrayList<ParameterInfo>();
        private List<ParameterInfo> outputParameters= new ArrayList<ParameterInfo>();
        private List<MyJobInfo> children = new ArrayList<MyJobInfo>();
        
        private boolean isVisualizer = false;

        public JobInfo getJobInfo() {
            return jobInfo;
        }

        public void setJobInfo(JobInfo j) {
            this.jobInfo = j;
            processParameterInfoArray();
        }

        public boolean isVisualizer() {
            return isVisualizer;
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
     * @param jobNo
     * @return
     */
    public MyJobInfo getJobInfo(String currentUser, int jobNo) {
        try {
            HibernateUtil.beginTransaction();
            AnalysisDAO ds = new AnalysisDAO();
            JobInfo jobInfo = ds.getJobInfo(jobNo);
            
            MyJobInfo myJobInfo = processChildren(ds, jobInfo);
            PermissionsHelper perm = new PermissionsHelper(currentUser, jobNo);
            return myJobInfo;
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }
    
    /**
     * Create a new MyJobInfo, recursively looking up and including all child jobs.
     * @param ds
     * @param jobInfo
     * @return
     */
    private MyJobInfo processChildren(AnalysisDAO ds, JobInfo jobInfo) {
        MyJobInfo j = new MyJobInfo();
        j.setJobInfo(jobInfo);
        
        //get the visualizer flag
        int taskId = jobInfo.getTaskID();
        AdminDAO ad = new AdminDAO();
        TaskInfo taskInfo = ad.getTask(taskId);
        j.isVisualizer = taskInfo.isVisualizer();

        JobInfo[] children = ds.getChildren(jobInfo.getJobNumber());
        for(JobInfo child : children) {
            MyJobInfo nextChild = processChildren(ds, child);
            j.addChildJobInfo(nextChild);
        }
        
        return j;
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
        obj.put("dateSubmitted", myJobInfo.getJobInfo().getDateSubmitted().getTime());
        obj.put("dateCompleted", myJobInfo.getJobInfo().getDateCompleted().getTime());
        obj.put("status", myJobInfo.getJobInfo().getStatus());
        obj.put("isVisualizer", Boolean.toString( myJobInfo.isVisualizer() ));
        
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
 
}
