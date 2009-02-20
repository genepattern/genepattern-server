package org.genepattern.server;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Get job status information.
 * 
 * @author pcarr
 */
public class JobInfoManager {
    public static class MyJobInfo {
        public static class InputParameter {
        }
        public static class OutputFile {            
        }

        private JobInfo jobInfo;
        private Set<InputParameter> inputParameters = new TreeSet<InputParameter>();
        private Set<OutputFile> outputFiles = new TreeSet<OutputFile>();
        private List<MyJobInfo> children = new ArrayList<MyJobInfo>();

        public JobInfo getJobInfo() {
            return jobInfo;
        }

        public void setJobInfo(JobInfo j) {
            this.jobInfo = j;
        }

        public Set<InputParameter> getInputParameters() {
            return inputParameters;
        }
        
        public Set<OutputFile> getOutputFiles() {
            return outputFiles;
        }
        
        public List<MyJobInfo> getChildren() {
            return children;
        }
        
        public void addChildJobInfo(MyJobInfo j) {
            children.add(j);
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
            MyJobInfo myJobInfo = step(ds, jobInfo);
            PermissionsHelper perm = new PermissionsHelper(currentUser, jobNo);
            return myJobInfo;
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }
    
    private MyJobInfo step(AnalysisDAO ds, JobInfo jobInfo) {
        MyJobInfo j = new MyJobInfo();
        j.setJobInfo(jobInfo);

        JobInfo[] children = ds.getChildren(jobInfo.getJobNumber());
        for(JobInfo child : children) {
            MyJobInfo nextChild = step(ds, child);
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
        
        for(MyJobInfo child : myJobInfo.getChildren()) {
            JSONObject childObj = convertToJSON(child);
            obj.accumulate("children", childObj);
        }
        return obj;
    }
 
}
