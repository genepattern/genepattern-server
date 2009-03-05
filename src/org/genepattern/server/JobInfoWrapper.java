package org.genepattern.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.webapp.jsf.JobPermissionsBean;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;

/**
 * Wrapper class to access JobInfo from JSON and JSF formatted pages.
 * 
 * @author pcarr
 */
public class JobInfoWrapper {
    /**
     * Wrapper class for a ParameterInfo which is an output file.
     */
    public static class OutputFile {
        private ParameterInfo parameterInfo = null;
        private String link = null;

        OutputFile(String contextPath, JobInfo jobInfo, ParameterInfo param) {
            this.parameterInfo = param;
            //map from ParameterInfo.name to URL for downloading the output file from the server
            this.link = contextPath + "/jobResults/" + jobInfo.getJobNumber() + "/" + parameterInfo.getName();
        }

        //ParameterInfo wrappper methods
        public String getName() {
            return parameterInfo.getName();
        }
        
        public String getValue() {
            return parameterInfo.getValue();
        }
        
        public String getValueId() {
            return parameterInfo.getValueId();
        }
        
        public String getDescription() {
            return parameterInfo.getDescription();
        }
        //----- end ParameterInfo wrapper methods

        /**
         * In case a ParameterInfo method is not wrapper, access it directly.
         */
        public ParameterInfo getParameterInfo() {
            return parameterInfo;
        } 

        /**
         * @return a link, relative to the server, for a web client to access the output file.
         */
        public String getLink() {
            return link;
        }
    }
    
    private JobInfo jobInfo;
    private List<ParameterInfo> inputParameters = new ArrayList<ParameterInfo>();
    private List<ParameterInfo> inputFiles = new ArrayList<ParameterInfo>();
    private List<OutputFile> outputFiles = new ArrayList<OutputFile>();
    private List<JobInfoWrapper> children = new ArrayList<JobInfoWrapper>();
    
    private boolean isPipeline = false;
    private int numSteps = 1;
    
    private boolean isVisualizer = false;
    private String visualizerAppletTag = "";

    private JobPermissionsBean jobPermissionsBean;

    public void setJobInfo(String contextPath, JobInfo jobInfo) {
        this.jobInfo = jobInfo;
        processParameterInfoArray(contextPath);
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
    
    public int getNumStepsCompleted() {
        //for pipelines
        if (isPipeline()) {
            if (children == null || children.size() == 0) {
                return 0;
            }
            int lastIdx = children.size() - 1;
            JobInfoWrapper last = children.get(lastIdx);
            if (last.isFinished()) {
                return lastIdx + 1;
            }
            else {
                return lastIdx;
            }
        }
        //for non-pipelines
        if (isFinished()) {
            return 1;
        }
        return 0;
    }
    
    public void setNumSteps(int n) {
        this.numSteps = n;
    }
    
    public int getNumSteps() {
        return numSteps;
    }
    
    private boolean isFinished() {
        if ( JobStatus.FINISHED.equals(getStatus()) ||
                JobStatus.ERROR.equals(getStatus()) ) {
            return true;
        }
        return false;        
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
    
    public List<ParameterInfo> getInputFiles() {
        return inputFiles;
    }
    
    public List<OutputFile> getOutputFiles() {
        return outputFiles;
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
    private void processParameterInfoArray(String contextPath) {
        for(ParameterInfo param : jobInfo.getParameterInfoArray()) {
            if (param.isOutputFile()) {
                OutputFile outputFile = new OutputFile(contextPath, jobInfo, param);
                outputFiles.add(outputFile);
            }
            else {
                inputParameters.add(param);
            }
            if (isInputFile(param)) {
                inputFiles.add(param);
            }
        }
    }
    
    private boolean isInputFile(ParameterInfo param) {
        //Note: formalParameters is one way to check if a given ParameterInfo is an input file
        //ParameterInfo[] formalParameters = taskInfo.getParameterInfoArray();

        if (param.isInputFile()) {
            return true;
        }
        //not to be confused with 'TYPE'
        String type = (String) param.getAttributes().get("type");
        if (type != null && type.equals("java.io.File")) {
            return true;
        }
        return false;
    }
    
    //Job Permissions methods
    private void initGroupPermissions() { 
        jobPermissionsBean = new JobPermissionsBean();
        jobPermissionsBean.setJobId(jobInfo.getJobNumber());
        //this.deleteAllowed = jobPermissionsBean.isDeleteAllowed();
    }

}