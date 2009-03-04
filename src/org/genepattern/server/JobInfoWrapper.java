package org.genepattern.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.genepattern.server.webapp.jsf.JobPermissionsBean;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;

public class JobInfoWrapper {
    private JobInfo jobInfo;
    private List<ParameterInfo> inputParameters = new ArrayList<ParameterInfo>();
    private List<ParameterInfo> outputParameters= new ArrayList<ParameterInfo>();
    private List<JobInfoWrapper> children = new ArrayList<JobInfoWrapper>();
    
    private boolean isPipeline = false;
    private boolean isVisualizer = false;
    private String visualizerAppletTag = "";

    private JobPermissionsBean jobPermissionsBean;

    public void setJobInfo(JobInfo j) {
        this.jobInfo = j;
        processParameterInfoArray();
        this.jobPermissionsBean = null;
    }

    //JobInfo wrapper methods
    public int getJobNumber() {
        return jobInfo.getJobNumber();
    }
    public String getUserId() {
        return jobInfo.getUserId();
    }
    public String getTaskName() {
        return jobInfo.getTaskName();
    }
    public String getStatus() {
        return jobInfo.getStatus();
    }
    public Date getDateSubmitted() {
        return jobInfo.getDateSubmitted();
    }
    public Date getDateCompleted() {
        return jobInfo.getDateCompleted();
    }
    public long getElapsedTimeMillis() {
        return jobInfo.getElapsedTimeMillis();
    }
    //--- end JobInfo wrapper methods

    public void setPipeline(boolean isPipeline) {
        this.isPipeline = isPipeline;
    }
    
    public boolean isPipeline() {
        return isPipeline;
    }

    public void setVisualizer(boolean isVisualizer) {
        this.isVisualizer = isVisualizer;
    }

    public boolean isVisualizer() {
        return isVisualizer;
    }
    
    public void setVisualizerAppletTag(String tag) {
        this.visualizerAppletTag = tag;
    }

    public String getVisualizerAppletTag() {
        return visualizerAppletTag;
    }

    public List<ParameterInfo> getInputParameters() {
        return inputParameters;
    }
    
    public List<ParameterInfo> getOutputFiles() {
        return outputParameters;
    }
    
    public List<JobInfoWrapper> getChildren() {
        return children;
    }
    
    public void addChildJobInfo(JobInfoWrapper j) {
        children.add(j);
    }
    
    public JobPermissionsBean getPermissions() {
        if (jobPermissionsBean == null) {
            initGroupPermissions();
        }
        return jobPermissionsBean;
    }
    
    /**
     * Read the ParameterInfo array from the jobInfo object 
     * and store the input and output parameters.
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
    }
    
    //Job Permissions methods
    private void initGroupPermissions() { 
        jobPermissionsBean = new JobPermissionsBean();
        jobPermissionsBean.setJobId(jobInfo.getJobNumber());
        //this.deleteAllowed = jobPermissionsBean.isDeleteAllowed();
    }

}