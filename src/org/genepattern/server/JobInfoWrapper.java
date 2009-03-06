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
 * Including support for pipelines and nested pipelines et cetera.
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
        
        /**
         * Helper method for accessing the value from web client JavaScript code.
         * @return the value, replacing all '/' with '_'.
         */
        public String getValueId() {
            String value = parameterInfo.getValue();
            if (value == null) {
                return null;
            }
            String valueId = value.replace('/', '_');
            return valueId;
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
    
    private JobInfoWrapper parent = null;
    private List<JobInfoWrapper> children = new ArrayList<JobInfoWrapper>();
    private List<JobInfoWrapper> allSteps = null;
    
    private boolean isPipeline = false;
    private int numAncestors = 0;
    
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

    //access in to input and output parameters
    public List<ParameterInfo> getInputParameters() {
        return inputParameters;
    }
    
    public List<ParameterInfo> getInputFiles() {
        return inputFiles;
    }
    
    public List<OutputFile> getOutputFiles() {
        return outputFiles;
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
    
    //support for visualizers
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

    //support for pipelines ...
    public void setPipeline(boolean isPipeline) {
        this.isPipeline = isPipeline;
    }
    
    public boolean isPipeline() {
        return isPipeline;
    }

    //support for pipelines ... including traversing the list of all steps (including nested steps (and nested nested steps (and ...)))
    public void setParent(JobInfoWrapper parent) {
        this.parent = parent;
        if (parent == null) {
            numAncestors = 0;
        }
        else {
            numAncestors = parent.numAncestors + 1;
        }
    }
    
    /**
     * @return true iff  this a top level job
     */
    public boolean isRoot() {
        return this.parent == null;
    }
    
    public JobInfoWrapper getRoot() {
        if (this.parent == null) {
            return this;
        }
        return this.parent.getRoot();
    }

    public List<JobInfoWrapper> getChildren() {
        return children;
    }

    public synchronized void addChildJobInfo(JobInfoWrapper jobInfoWrapper) {
        children.add(jobInfoWrapper);
    }
    
    /**
     * If this is a pipeline, get all of the steps in the pipeline.
     * Does not include the root job.
     * @return
     */
    public List<JobInfoWrapper> getAllSteps() {
        if (allSteps == null) {
            allSteps = getAllSteps(this);
        }
        return allSteps;
    }
    
    private List<JobInfoWrapper> getAllSteps(JobInfoWrapper parent) {
        List<JobInfoWrapper> all = new ArrayList<JobInfoWrapper>();
        //don't include the root job
        if (!parent.isRoot()) {
            all.add(parent);
        }
        for (JobInfoWrapper child : parent.children) {
            List<JobInfoWrapper> allChildren = getAllSteps(child);
            all.addAll(allChildren);
        }
        return all;
    }
    
    private List<JobInfoWrapper> pathFromRoot = null;
    public List<JobInfoWrapper> getPathFromRoot() {
        if (pathFromRoot == null) {
            pathFromRoot = constructPathFromRoot();
        }
        return pathFromRoot;
    }
    
    private List<JobInfoWrapper> constructPathFromRoot() {
        if (parent == null) {
            List<JobInfoWrapper> p = new ArrayList<JobInfoWrapper>();
            p.add(this);
            return p;            
        }
        else {
            List<JobInfoWrapper> p = parent.constructPathFromRoot();
            p.add(this);
            return p;
        }
    }

    /**
     * @return the number of ancestor jobs, 0 if the parent is null, more than 0 for jobs that are in pipelines.
     */
    public int getNumAncestors() {
        return getPathFromRoot().size() - 1;
    }

    /**
     * Get the position of this job amongst the list of its siblings,
     * indexing based on 1 ... number of siblings.
     * @return
     */
    public int getStepNumber() {
        if (this.parent == null || this.parent.children == null || this.parent.children.size() == 0) {
            return 0;
        }
        return 1 + this.parent.children.indexOf(this);
    }
    
    /**
     * @return The number of steps (sibling jobs) in this jobs parent pipeline.
     */
    public int getStepCount() {
        if (this.parent == null || this.parent.children == null || this.parent.children.size() == 0) {
            return 0;
        }
        return this.parent.children.size();
    }

    /**
     * @return A string denoting the path from the root job to this job, delimited by the dot '.' character, labeling each node with the stepNumber of that node relative to its siblings.
     * 
     *     For example, '5.2.4' indicates that this job is the 4th step in its parent pipeline, 
     *     which is the 2nd step in its parent pipeline, 
     *     which is the 5th step in its parent pipeline, which is the root.
     */
    public String getStepPath() {
        String stepPath = "";
        boolean first = true;
        boolean second = false;
        for (JobInfoWrapper step : getPathFromRoot()) {
            if (first) {
                first = false;
                second = true;
            }
            else {
                if (second) {
                    second = false;
                }
                else {
                    stepPath += ".";
                }
                stepPath += step.getStepNumber();
            }
        }
        return stepPath;
    }

    //special cases when nested pipelines are involved
    /**
     * @return the total number of steps in the root job, including a count of steps in all nested pipelines.
     */
    public int getTotalStepCount() {
        JobInfoWrapper root = this.getRoot();
        List<JobInfoWrapper> allSteps = root.getAllSteps();
        return -1 + allSteps.size();
    }

    /**
     * @return the index plus one of the current job into the list of all jobs including nested pipelines.
     */
    public int getTotalStepNumber() {
        JobInfoWrapper root = this.getRoot();
        if (this == root) {
            return 0;
        }
        List<JobInfoWrapper> allSteps = root.getAllSteps();
        int idx = allSteps.indexOf(this);
        return idx;
    }
    
    //helper methods for indicating how many steps in the pipeline are completed
    private int numStepsInPipeline = 0;
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
    
    public void setNumStepsInPipeline(int n) {
        this.numStepsInPipeline = n;
    }
    
    public int getNumStepsInPipeline() {
        return numStepsInPipeline;
    }

    private int currentStepInPipeline = 0;
    public int getCurrentStepInPipeline() {
        return currentStepInPipeline;
    }
    
    private boolean isFinished() {
        if ( JobStatus.FINISHED.equals(getStatus()) ||
                JobStatus.ERROR.equals(getStatus()) ) {
            return true;
        }
        return false;        
    }

    public JobPermissionsBean getPermissions() {
        if (jobPermissionsBean == null) {
            initGroupPermissions();
        }
        return jobPermissionsBean;
    }
    
    //Job Permissions methods
    private void initGroupPermissions() { 
        jobPermissionsBean = new JobPermissionsBean();
        jobPermissionsBean.setJobId(jobInfo.getJobNumber());
        //this.deleteAllowed = jobPermissionsBean.isDeleteAllowed();
    }

}