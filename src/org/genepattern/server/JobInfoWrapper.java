package org.genepattern.server;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.webapp.jsf.JobHelper;
import org.genepattern.server.webapp.jsf.JobPermissionsBean;
import org.genepattern.server.webapp.jsf.KeyValuePair;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.util.GPConstants;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.jfree.util.Log;

/**
 * Wrapper class to access JobInfo from JSON and JSF formatted pages.
 * Including support for pipelines and nested pipelines et cetera.
 * 
 * @author pcarr
 */
public class JobInfoWrapper {
    private static Logger log = Logger.getLogger(JobInfoWrapper.class);

    public static class ParameterInfoWrapper {
        public static class KeyValueComparator implements Comparator<KeyValuePair> {
            public int compare(KeyValuePair o1, KeyValuePair o2) {
                return o1.getKey().compareToIgnoreCase(o2.getKey());
            }
        }

        protected static ParameterInfo getFormalParameter(ParameterInfo[] formalParameters, ParameterInfo parameterInfo) {
            //TODO: optimize
            String paramName = null;
            if (parameterInfo != null) {
                paramName = parameterInfo.getName();
            }
            if (paramName == null) {
                return null;
            }
            for(ParameterInfo formalParameter : formalParameters) {
                if (paramName.equals(formalParameter.getName())) {
                    return formalParameter;
                }
            }
            return null;
        }
        
        private ParameterInfo parameterInfo = null;
        private String displayName = null;
        private String displayValue = null;
        private String link = null; //optional link to GET input or output file
        private Date lastModified = null; //optional last modification date for files on the server
        private long size = 0L; //optional size for files on the server
        protected static final Comparator<KeyValuePair> KEY_VALUE_COMPARATOR = new KeyValueComparator();

        private List<KeyValuePair> moduleMenuItems = new ArrayList<KeyValuePair>();
        
        public ParameterInfoWrapper(ParameterInfo parameterInfo) {
            this.parameterInfo = parameterInfo;
        }
        
        /**
         * Provide access to the wrapped ParameterInfo in case a wrapper method is not available.
         */
        public ParameterInfo getParameterInfo() {
            return parameterInfo;
        } 

        //ParameterInfo wrapper methods        
        public String getName() {
            if (parameterInfo != null) {
                return parameterInfo.getName();
            }
            return "";
        }

        public String getDescription() {
            return parameterInfo.getDescription();
        } 
        
        public String getValue() {
            if (parameterInfo != null) {
                return parameterInfo.getValue();
            }
            return "";
        }
        //------ end ParameterInfo wrapper methods

        protected void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            if (displayName == null) {
                return getName();
            }
            return displayName;
        }
        
        public String getTruncatedDisplayValue() {
        	if (getDisplayValue() != null && getDisplayValue().length() > 70) {
        		return getDisplayValue().substring(0, 35)+"..." + getDisplayValue().substring(getDisplayValue().length()-32, getDisplayValue().length());
        	} else {
        		return getDisplayValue();
        	}
        }

        protected void setDisplayValue(String displayValue) {
            this.displayValue = displayValue;
        }
        
        public String getDisplayValue() {
            if (displayValue == null) {
                return parameterInfo.getValue();
            }
            return displayValue;
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
        
        protected void setSize(long size) {
            this.size = size;
        }
        
        public long getSize() {
            return this.size;
        }
        
        public String getFormattedSize() {
            return JobHelper.getFormattedSize(size);
        }

        protected void setLastModified(Date lastModified) {
            this.lastModified = lastModified;
        }

        public Date getLastModified() {
            return lastModified;
        }
        
        /**
         * @param link
         * @see #getLink()
         */
        protected void setLink(String link) {
            this.link = link;
        }
        
        /**
         * @return a link, relative to the server, for a web client to access this parameter;
         *     Should be null unless this is an input or output file.
         */
        public String getLink() {
            return link;
        }
        
        protected void setModuleMenuItemsForFile(Map<String, Collection<TaskInfo>>  kindToModules, File file) {
            List<KeyValuePair> moduleMenuItems = new ArrayList<KeyValuePair>();
            String kind = SemanticUtil.getKind(file);
            Collection<TaskInfo> taskInfos = kindToModules.get(kind);
            if (taskInfos != null) {
                for (TaskInfo taskInfo : taskInfos) {
                    KeyValuePair mi = new KeyValuePair(taskInfo.getShortName(), UIBeanHelper.encode(taskInfo.getLsid()));
                    moduleMenuItems.add(mi);
                }
                Collections.sort(moduleMenuItems, KEY_VALUE_COMPARATOR);
            }
            else {
                log.error("JobInfoWrapper.setModuleMenuItemsForFile: kindToModules.get('"+kind+"') returned null");
            }
            this.moduleMenuItems = moduleMenuItems;
        }
        
        public List<KeyValuePair> getModuleMenuItems() {
            return moduleMenuItems;
        }
    }
    
    /**
     * Wrapper class for a ParameterInfo which is an output file.
     */
    public static class OutputFile extends ParameterInfoWrapper {
        public static boolean isTaskLog(ParameterInfo parameterInfo) {
            String filename = parameterInfo == null ? "" : parameterInfo.getName();
            boolean isTaskLog = 
                filename != null &&
                ( filename.equals(GPConstants.TASKLOG) || 
                  filename.endsWith(GPConstants.PIPELINE_TASKLOG_ENDING)
                );
            return isTaskLog;
        }

        private boolean isTaskLog = false;

        OutputFile(Map<String, Collection<TaskInfo>>  kindToModules, File outputDir, String contextPath, JobInfo jobInfo, ParameterInfo parameterInfo) {
            super(parameterInfo);
            File outputFile = new File(outputDir, parameterInfo.getName());
            //Set the size and lastModified properties for each output file
            boolean exists = outputFile.exists();
            if (exists) {
                setSize(outputFile.length());
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(outputFile.lastModified());
                setLastModified(cal.getTime());
            }
            else {
                Log.error("Outputfile not found on server: "+outputFile.getAbsolutePath());
            }

            //map from ParameterInfo.name to URL for downloading the output file from the server
            String link = contextPath + "/jobResults/" + jobInfo.getJobNumber() + "/" + parameterInfo.getName();
            setLink(link);
            setDisplayValue(outputFile.getName());
            
            //check execution log
            this.isTaskLog = isTaskLog(parameterInfo);
            
            //set up module popup menu for the output file
            if (!this.isTaskLog && exists) {
                setModuleMenuItemsForFile(kindToModules, outputFile);
            }
        }
        
        public boolean isTaskLog() {
            return isTaskLog;
        } 
    }
    
    public static class InputFile extends ParameterInfoWrapper {
        /**
         *
<pre>
         <h:outputText rendered="#{p.url}">
           <a href="#{p.value}">#{p.displayValue}</a>
         </h:outputText>
         <h:outputText rendered="#{!p.url and p.exists}">
           <h:outputText rendered="#{!empty p.directory}">
             <a href="#{facesContext.externalContext.requestContextPath}/getFile.jsp?file=#{p.directory}/#{p.value}">#{p.displayValue}</a>
           </h:outputText>
           <h:outputText rendered="#{empty p.directory}">
             <a href="#{facesContext.externalContext.requestContextPath}/getFile.jsp?file=#{p.value}">#{p.displayValue}</a>
           </h:outputText>
         </h:outputText>
         <h:outputText rendered="#{!p.url and !p.exists}">
           #{p.displayValue}
         </h:outputText>
</pre>
         * @param parameterInfo
         */
        InputFile(String contextPath, String uiValue, ParameterInfo parameterInfo) {
            super(parameterInfo);
            initLinkValue(contextPath, uiValue);
        }
        
        private void initLinkValue( String contextPath, String value ) 
        {
            //String value = parameterInfo.getUIValue(formalParameter);
            // skip parameters that the user did not give a value for
            //if (value == null || value.equals("")) {
            //    return;
            //}
            String displayValue = value;
            boolean isUrl = false;
            boolean exists = false;

            String directory = null;
            String genePatternUrl = UIBeanHelper.getServer();
            try {
                // see if a URL was passed in
                URL url = new URL(value);
                // bug 2026 - file:// URLs should not be treated as a URL
                isUrl = true;
                if ("file".equals(url.getProtocol())){
                    isUrl = false;
                    value = value.substring(5);// strip off the file: part for the next step
                }
                if (displayValue.startsWith(genePatternUrl)) {          
                    int lastNameIdx = value.lastIndexOf("/");
                    displayValue = value.substring(lastNameIdx+1);      
                    isUrl = true;
                }
            } 
            catch (MalformedURLException e) {
                if (displayValue.startsWith("<GenePatternURL>")) {          
                    int lastNameIdx = value.lastIndexOf("/");
                    if (lastNameIdx == -1) {
                        lastNameIdx = value.lastIndexOf("file=");
                        if (lastNameIdx != -1) {
                            lastNameIdx += 5;
                        }
                    }
                    if (lastNameIdx != -1) { 
                        displayValue = value.substring(lastNameIdx);        
                    } 
                    else {
                        displayValue = value;
                    }
                    value = genePatternUrl + "/" + value.substring("<GenePatternURL>".length());
                    isUrl = true;
                } 
            }

            if (!isUrl) {
                File inputFile = new File(value);
                exists = inputFile.exists();
                value = inputFile.getName();
                displayValue = value;
                if (displayValue.startsWith("Axis")) {
                    displayValue = displayValue.substring(displayValue.indexOf('_') + 1);
                }
                if (exists) {
                    directory = inputFile.getParentFile().getName();
                    setSize(inputFile.length());
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(inputFile.lastModified());
                    setLastModified(cal.getTime());
                    
                    //TODO: don't add the menu items until the action links are implemented, requires some updates to the JobBean
                    //set up module popup menu for the input file
                    //setModuleMenuItemsForFile(kindToModules, inputFile);
                }
            }
            
            String link = null;
            if (isUrl) {
                link = value;
            }
            else if (exists) {
                String fileParam = "";
                if (directory != null) {
                    fileParam += directory + "/";
                }
                fileParam += value;
                //url encode fileParam
                try {
                    fileParam = URLEncoder.encode(fileParam, "UTF-8");
                }
                catch (UnsupportedEncodingException e) {
                    log.error("Error encoding inputFile param, '"+fileParam+"' "+e.getLocalizedMessage(), e);
                } 
                link = contextPath + "/getFile.jsp?file="+fileParam;
            }
            setLink(link);
            setDisplayValue(displayValue);
        }
    }
    
    private JobInfo jobInfo = null;
    private TaskInfo taskInfo = null;
    private Map<String, Collection<TaskInfo>> kindToModules;
    private Long size = null;
    private boolean includeInputFilesInSize = false;
    /**
     * Get the total size of all of the output files for this job, including all descendent jobs.
     * Note: the size of input files is ignored.
     * @return
     */
    public long getSize() {
        if (size == null) {
            long counter = 0L;
            for(JobInfoWrapper child : children) {
                counter += child.getSize();
            }
            if (includeInputFilesInSize) {
                for(InputFile inputFile : inputFiles) {
                    counter += inputFile.getSize();
                }
            }
            for(OutputFile outputFile : outputFiles) {
                counter += outputFile.getSize();
            }
            size = counter;
        }
        return size;
    }

    public String getFormattedSize() {
        return JobHelper.getFormattedSize(getSize());
    }

    private String servletContextPath = "/gp";
    private File outputDir;
    private boolean showExecutionLogs = false;
    private List<ParameterInfoWrapper> inputParameters = new ArrayList<ParameterInfoWrapper>();
    private List<InputFile> inputFiles = new ArrayList<InputFile>();
    private List<OutputFile> outputFiles = new ArrayList<OutputFile>();
    private List<OutputFile> outputFilesAndTaskLogs = new ArrayList<OutputFile>();
    
    private JobInfoWrapper parent = null;
    private List<JobInfoWrapper> children = new ArrayList<JobInfoWrapper>();
    private List<JobInfoWrapper> allSteps = null;
    
    private int numAncestors = 0;
    
    private String visualizerAppletTag = null;

    private JobPermissionsBean jobPermissionsBean;

    public void setTaskInfo(TaskInfo taskInfo) {
        this.taskInfo = taskInfo;
    }
    
    public void setJobInfo(boolean showExecutionLogs, String servletContextPath, Map<String, Collection<TaskInfo>> kindToModules, JobInfo jobInfo) {
        this.servletContextPath = servletContextPath;
        this.showExecutionLogs = showExecutionLogs;
        this.jobInfo = jobInfo;
        this.kindToModules = kindToModules;
        String jobDir = GenePatternAnalysisTask.getJobDir(""+jobInfo.getJobNumber());
        this.outputDir = new File(jobDir);
        processParameterInfoArray();
        this.jobPermissionsBean = null;        
    }

    //JobInfo wrapper methods
    public int getJobNumber() {
        if (jobInfo != null) {
            return jobInfo.getJobNumber();
        }
        log.error("jobInfo is null");
        return -1;
    }
    public String getUserId() {
        if (jobInfo != null) {
            return jobInfo.getUserId();
        }
        log.error("jobInfo is null");
        return "";
    }
    public String getTaskName() {
        if (jobInfo != null) {
            return jobInfo.getTaskName();
        }
        log.error("jobInfo is null");
        return "";
    }
    public String getTruncatedTaskName() {
    	if (getTaskName() != null && getTaskName().length() > 70) {
    		return getTaskName().substring(0, 67)+"...";
    	} else {
    		return getTaskName();
    	}
    }
    public String getTaskLSID() {
        if (jobInfo != null) {
            return jobInfo.getTaskLSID();
        }
        log.error("jobInfo is null");
        return "";
    }
    public String getStatus() {
        if (jobInfo != null) {
            return jobInfo.getStatus();
        }
        log.error("jobInfo is null");
        return "";
    }
    public Date getDateSubmitted() {
        if (jobInfo != null) {
            return jobInfo.getDateSubmitted();
        }
        log.error("jobInfo is null");
        return null;
    }
    public Date getDateCompleted() {
        if (jobInfo != null) {
            return jobInfo.getDateCompleted();
        }
        log.error("jobInfo is null");
        return null;
    }
    public long getElapsedTimeMillis() {
        if (jobInfo != null) {
            return jobInfo.getElapsedTimeMillis();
        }
        log.error("jobInfo is null");
        return 0L;
    }
    //--- end JobInfo wrapper methods
    
    public String getServletContextPath() {
        return this.servletContextPath;
    }

    //access in to input and output parameters
    public List<ParameterInfoWrapper> getInputParameters() {
        return inputParameters;
    }
    
    public List<InputFile> getInputFiles() {
        return inputFiles;
    }
    
    public List<OutputFile> getOutputFiles() {
        if (this.showExecutionLogs) {
            return this.outputFilesAndTaskLogs;
        }
        else {
            return outputFiles;
        }
    }

    private boolean purgeDateInitialized = false;
    private Date purgeDate = null;
    private String formattedPurgeDate = "";

    /**
     * @return the date when the job result files will be deleted from the server, 
     *             a null value indicates that files won't be purged.
     */
    private synchronized void initPurgeDate() {
        //this is also implemented in the JobPurger and Purger classes
        //TODO: add a static method to the JobPurger to get the purge date
        GregorianCalendar purgeTOD = new GregorianCalendar();
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
            GregorianCalendar gcPurge = new GregorianCalendar();
            gcPurge.setTime(dateFormat.parse(System.getProperty("purgeTime", "23:00")));
            purgeTOD.set(GregorianCalendar.HOUR_OF_DAY, gcPurge.get(GregorianCalendar.HOUR_OF_DAY));
            purgeTOD.set(GregorianCalendar.MINUTE, gcPurge.get(GregorianCalendar.MINUTE));
        } 
        catch (ParseException pe) {
            purgeTOD.set(GregorianCalendar.HOUR_OF_DAY, 23);
            purgeTOD.set(GregorianCalendar.MINUTE, 0);
        }
        purgeTOD.set(GregorianCalendar.SECOND, 0);
        purgeTOD.set(GregorianCalendar.MILLISECOND, 0);
        int purgeInterval;
        try {
            purgeInterval = Integer.parseInt(System.getProperty("purgeJobsAfter", "-1"));
        } 
        catch (NumberFormatException nfe) {
            log.error("Error getting file purge settings: "+nfe.getLocalizedMessage(), nfe);
            purgeInterval = 7;
        }
        purgeTOD.add(GregorianCalendar.DATE, purgeInterval);
        this.purgeDate = purgeTOD.getTime();
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        this.formattedPurgeDate =  df.format(purgeDate.getTime()).toLowerCase();
        this.purgeDateInitialized = true;
    }
    
    public Date getPurgeDate() {
        if (!purgeDateInitialized) {
            initPurgeDate();
        }
        return purgeDate;
    }

    public String getFormattedPurgeDate() {
        if (!purgeDateInitialized) {
            initPurgeDate();
        }
        return formattedPurgeDate;
    }

    /**
     * When a module or pipeline has been deleted the taskInfo is not available.
     * @return true if the TaskInfo can be loaded from the GP database.
     */
    public boolean isTaskInfoAvailable() {
        return taskInfo != null;
    }

    /**
     * Read the ParameterInfo array from the jobInfo object 
     * and store the input and output parameters.
     */
    private void processParameterInfoArray() {
        for(ParameterInfo param : jobInfo.getParameterInfoArray()) {
            if (param.isOutputFile()) {
                OutputFile outputFile = new OutputFile(kindToModules, outputDir, servletContextPath, jobInfo, param);
                outputFilesAndTaskLogs.add(outputFile);
                if (!outputFile.isTaskLog()) {
                    //don't add execution logs
                    outputFiles.add(outputFile);
                }
            }
            else {
                ParameterInfoWrapper inputParam = null;
                //ParameterInfo formalParam = ParameterInfoWrapper.getFormalParameter(formalParams, param);
                //String uiValue = param.getUIValue(formalParam);
                String value = param.getValue();
                if (value != null && !"".equals(value)) {
                    if (isInputFile(param)) {
                        InputFile inputFile = new InputFile(servletContextPath, value, param);
                        inputFiles.add(inputFile);
                        inputParam = inputFile;
                    } 
                    else {
                        inputParam = new ParameterInfoWrapper(param);
                    }
                }
                else {
                    // [optional] parameter that the user did not give a value for
                    inputParam = new ParameterInfoWrapper(param);
                }
                //set the display name
                String name = param.getName();
                //String name = (String) formalParam.getAttributes().get("altName");
                //if (name == null) {
                //    name = formalParam.getName();
                //}
                name = name.replaceAll("\\.", " ");
                inputParam.setDisplayName(name);

                inputParameters.add(inputParam);
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
    public boolean isVisualizer() {
        return this.visualizerAppletTag != null && !"".equals(this.visualizerAppletTag);
    }
    
    public void setVisualizerAppletTag(String tag) {
        this.visualizerAppletTag = tag;
    }

    public String getVisualizerAppletTag() {
        return visualizerAppletTag;
    }

    //support for pipelines ...
    public boolean isPipeline() {
        if (taskInfo != null) {
            return taskInfo.isPipeline();
        }
        //handle special case when the module for this job number is not available
        return children != null && children.size() > 0;
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
    public Integer[] getNumAncestors() {
        int num = getPathFromRoot().size() - 1;
        Integer[] returnedArray = new Integer[num];
        return returnedArray;
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
    public Integer[] getStepCount() {
        if (this.parent == null || this.parent.children == null || this.parent.children.size() == 0) {
            return new Integer[0];
        }
        Integer[] returnedArray = new Integer[this.parent.children.size()];
        for (int i = 0; i < this.parent.children.size(); i++) {
        	returnedArray[i] = i;
        }
        return returnedArray;
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
    
    public Integer[] getNumStepsInPipeline() {
        return new Integer[numStepsInPipeline];
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
        if (jobInfo != null) {
            jobPermissionsBean.setJobId(jobInfo.getJobNumber());
            //this.deleteAllowed = jobPermissionsBean.isDeleteAllowed();
        }
        else {
            log.error("jobInfo is null");
        }
    }

}