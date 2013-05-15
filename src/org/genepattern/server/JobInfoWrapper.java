package org.genepattern.server;

import java.io.File;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.process.JobPurgerUtil;
import org.genepattern.server.util.AuthorizationManagerFactory;
import org.genepattern.server.util.IAuthorizationManager;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.server.webapp.jsf.JobHelper;
import org.genepattern.server.webapp.jsf.JobPermissionsBean;
import org.genepattern.server.webapp.jsf.KeyValuePair;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.util.GPConstants;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;

/**
 * Wrapper class to access JobInfo from JSF formatted pages.
 * Including support for pipelines and nested pipelines et cetera.
 * 
 * @author pcarr
 */
public class JobInfoWrapper implements Serializable {
    private static Logger log = Logger.getLogger(JobInfoWrapper.class);

    public static class ParameterInfoWrapper implements Serializable {
        public static class KeyValueComparator implements Comparator<KeyValuePair> {
            public int compare(KeyValuePair o1, KeyValuePair o2) {
                return o1.getKey().compareToIgnoreCase(o2.getKey());
            }
        }
        
        private ParameterInfo parameterInfo = null;
        private String displayName = null;
        private String displayValue = null;
        private String link = null; //optional link to GET input or output file
        private Date lastModified = null; //optional last modification date for files on the server
        private long size = 0L; //optional size for files on the server
        protected static final Comparator<KeyValuePair> KEY_VALUE_COMPARATOR = new KeyValueComparator();

        private List<KeyValuePair> moduleMenuItems = new ArrayList<KeyValuePair>();
        
        protected boolean displayFileInfo = true;
        
        public ParameterInfoWrapper(ParameterInfo parameterInfo) {
            this.parameterInfo = parameterInfo;
            
            String currentUserId = UIBeanHelper.getUserId();
            ServerConfiguration.Context userContext = ServerConfiguration.Context.getContextForUser(currentUserId);
            displayFileInfo = ServerConfiguration.instance().getGPBooleanProperty(userContext, "display.file.info");
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
        
        public String getKind() {
            String name = getValue();
            String extension = null;
            int idx = name.lastIndexOf(".");
            if (idx > 0 && idx < (name.length() - 1)) {
                extension = name.substring(idx + 1);
            }
            return extension;
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
            valueId = valueId.replace(" ", "%20");
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

        /**
         * @return the full url for a web client to access this file; 
         *     Should be null unless this is an input or output file.
         */
        public URL getUrl() {
            if (link == null || link.length() == 0) {
                return null;
            }
            URL url;
            try {
                url = new URL(link);
                return url;
            }
            catch (MalformedURLException e) {
                //expected for relative links, e.g. /gp/jobResults/1/out.txt
            }
            
            //assume a relative link, and append the server path
            // e.g. replace /gp/jobResults/1/out.txt with http://127.0.0.1:8080/gp/jobResults/out.txt
            //Note: be careful with assumptions about the servlet context path
            //TODO: remove hard-coded context path, '/gp'
            if (link.startsWith("/gp")) {
                String sub_link = link.substring("/gp".length());
                String genePatternUrl = UIBeanHelper.getServer();
                try {
                    url = new URL(genePatternUrl + sub_link);
                    return url;
                }
                catch (MalformedURLException e) {
                }
            }
            else {
                log.error("Unexpected value for link in getUrl, link="+link);
            }
            return null;
        }

        /**
         * When the URL is to be sent to the job input form, it should be
         * passed as part of the queryString for an HTTP request. 
         * This method returns the correct form of the url for this scenario.
         * 
         * @return 
         */
        public String getEncodedUrl() {
            URL url=getUrl();
            if (url==null) {
                return null;
            }
            final String urlStr=url.toExternalForm();
            final String encodedStr=UrlUtil.encodeURIcomponent(urlStr);
            return encodedStr;
        }
        
        public void addModuleMenuItem(TaskInfo sendTo) {
            if (moduleMenuItems == null) {
                moduleMenuItems = new ArrayList<KeyValuePair>();
            }
            KeyValuePair mi = new KeyValuePair(sendTo.getShortName(), UIBeanHelper.encode(sendTo.getLsid()));
            moduleMenuItems.add(mi);
        }
        
        public List<KeyValuePair> getModuleMenuItems() {
            //TODO: optimize, shouldn't sort this every time
            if (moduleMenuItems != null) {
                Collections.sort(moduleMenuItems, KEY_VALUE_COMPARATOR);
            }
            return moduleMenuItems;
        }
    }
    
    /**
     * Wrapper class for a ParameterInfo which is an output file.
     */
    public static class OutputFile extends ParameterInfoWrapper {
        private File outputFile = null;
        /**
         * @return the path to the output file on the server.
         */
        public File getOutputFile() {
            return outputFile;
        }
        
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
        private boolean isChildJobResult = false;
        private boolean isJnlp = false;

        /**
         * 
         * @return true iff this output file is an output of the given job.
         */
        public boolean isChildJobResult() {
            return isChildJobResult;
        }

        OutputFile(File outputDir, String contextPath, JobInfo jobInfo, ParameterInfo parameterInfo) {
            super(parameterInfo);

            boolean exists = false;

            
            if (outputDir != null) {
                this.outputFile = new File(outputDir.getParent(), parameterInfo.getValue());
                exists = outputFile.exists();
                if (exists) {
                    File relativePath = GenePatternAnalysisTask.getRelativePath(outputDir, outputFile);
                    if (relativePath != null) {
                        this.isChildJobResult = true;
                    }
                }
            }
            if (!exists) {
                this.outputFile = new File(outputDir, parameterInfo.getName());
                exists = outputFile.exists();
            }

            //Set the size and lastModified properties for each output file
            //for debugging, disable file system queries
            if (displayFileInfo && exists) {
                setSize(outputFile.length());
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(outputFile.lastModified());
                setLastModified(cal.getTime());
            }
            else {
                log.error("Outputfile not found on server: "+outputFile.getAbsolutePath());
            }

            //map from ParameterInfo.name to URL for downloading the output file from the server
            String linkIn = contextPath + "/jobResults/" + jobInfo.getJobNumber() + "/" + parameterInfo.getName();
            File file=new File(linkIn);
            String link=UrlUtil.encodeFilePath(file);
            setLink(link);
            setDisplayValue(parameterInfo.getName());
            
            //check execution log
            this.isTaskLog = isTaskLog(parameterInfo);
            
            this.isJnlp = this.outputFile != null && this.outputFile.getName().toLowerCase().endsWith(".jnlp");
        }
        
        public boolean isTaskLog() {
            return isTaskLog;
        } 
        
        public boolean isJnlp() {
            return isJnlp;
        }
    }
    
    public static class InputFile extends ParameterInfoWrapper {
        private Boolean isUrl = null;
        private Boolean isInternalLink = null;
        private Boolean isExternalLink = null;
        private Boolean isServerFilePath = null;
        
        public Boolean isUrl() {
            return isUrl;
        }
        
        public Boolean isInternalLink() {
            return isInternalLink;
        }
        
        public Boolean isExternalLink() {
            return isExternalLink;
        }
        
        public Boolean isServerFilePath() {
            return isServerFilePath;
        }
        
        /**
         * Needed for JSF calls
         * @return
         */
        public Boolean getInternalLink() {
            return isInternalLink;
        }
        
        /**
         * Needed for JSF calls
         * @return
         */
        public Boolean getExternalLink() {
            return isExternalLink;
        }
        
        /**
         * Needed for JSF calls
         * @return
         */
        public Boolean getServerFilePath() {
            return isServerFilePath;
        }
        
        public File getInputFile() {
            // TODO: Get a File reference to the input file itself
            return null;
        }

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
        InputFile(TaskInfo taskInfo, JobInfo jobInfo, String contextPath, String paramValue, ParameterInfo parameterInfo) {
            super(parameterInfo);
            initLinkValue(taskInfo, jobInfo.getJobNumber(), contextPath, paramValue);
        }

        /**
         * Map parameter value to displayValue and link.
         * @param jobNumber
         * @param contextPath
         * @param value
         */
        private void initLinkValue( TaskInfo taskInfo, int jobNumber, String contextPath, String value ) {  
            //A. External link, e.g. ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.gct

            //B. Internal links
            //   1. to file uploaded from web client in previous job, then reloaded for new job
            //      http://127.0.0.1:8080/gp/getFile.jsp?task=&job=1383&file=test_run30303.tmp/all_aml_test.gct
            //   2. to file uploaded from SOAP client in previous job, then reloaded for new job
            //      http://127.0.0.1:8080/gp/getFile.jsp?task=&job=1387&file=test/Axis30305.att_all_aml_test.gct
            //   3. to output from previous job
            //      http://127.0.0.1:8080/gp/jobResults/3182/all_aml_test.preprocessed.gct
            //   4. to file uploaded when creating a pipeline, e.g.
            //      <GenePatternURL>getFile.jsp?task=urn%3Alsid%3A8080.pcarr.gm971-3d7.broadinstitute.org%3Agenepatternmodules%3A127%3A9&file=all_aml_test.gct
            //      http://127.0.0.1:8080/gp/getFile.jsp?task=urn%3Alsid%3A8080.pcarr.gm971-3d7.broadinstitute.org%3Agenepatternmodules%3A127%3A9&file=all_aml_test.gct
            //   5. to file uploaded when creating a module, e.g.
            //      <libdir>stdin.txt
            
            //C. Server file path
            //   1. uploaded from web client,
            //      /xchip/genepattern/node256/gp-trunk/Tomcat/temp/test_run30301.tmp/all_aml_test.gct
            //   2. upload from SOAP client,
            //      /xchip/genepattern/node256/gp-trunk/temp/attachments/test/Axis30302.att_all_aml_test.gct
            //   3. file in <libdir>,
            //      /xchip/genepattern/node256/gp-trunk/taskLib/testCat.1.3361/stdin.txt
            //   4. other server file path, 
            //      a) allow.input.file.paths=true
            //      b) allow.input.file.paths=false, handle error 
            //      c) allow.input.file.paths=true, but path is to a restricted area
            
            String origValue = value;
            String genePatternUrl = UIBeanHelper.getServer();
            //substitute <GenePatternURL>
            if (value.startsWith("<GenePatternURL>")) {
                value = genePatternUrl + "/" + value.substring("<GenePatternURL>".length());
            }
            if (value.startsWith("file:")) {
                value = value.substring(5);
            }
            //substitute <libdir>
            String taskLibDir=null;
            if (value.startsWith("<libdir>")) {
                if (taskInfo != null) {
                    taskLibDir = DirectoryManager.getTaskLibDir(taskInfo);
                    value = taskLibDir + "/" + value.substring("<libdir>".length());
                }
                else {
                    log.error("Can't get taskLibDir for job #"+jobNumber+", taskInfo is null");
                }
            }
            isUrl = false;
            URL url = null;
            try {
                url = new URL(value);
                isUrl = true;
            }
            catch (MalformedURLException e) {
                isUrl = false;
            }
            
            isInternalLink = value.startsWith(genePatternUrl);
            isExternalLink = isUrl && !isInternalLink;
            isServerFilePath = !isInternalLink && !isExternalLink;
            
            //case A
            if (isExternalLink) {
                this.setDisplayValue(value);
                this.setLink(value);
                return;
            }
            
            //case B
            if (isInternalLink) {
                this.setLink(value);
                
                String displayValue = value;
                //drop the server url, including first '/' from the display value
                if (displayValue.startsWith(genePatternUrl)) {
                    displayValue = displayValue.substring(genePatternUrl.length() + 1);
                }
                
                int lastNameIdx = displayValue.lastIndexOf("/");
                if (lastNameIdx >= 0) {
                    ++lastNameIdx;
                }
                else {
                    lastNameIdx = displayValue.lastIndexOf("file=");
                    if (lastNameIdx != -1) {
                        lastNameIdx += 5;
                    }
                }
                if (lastNameIdx != -1) { 
                    displayValue = displayValue.substring(lastNameIdx);        
                } 
                
                //special case for Axis
                if (displayValue.startsWith("Axis")) {
                    int idx = displayValue.indexOf('_') + 1;
                    displayValue = displayValue.substring(idx);
                }
                this.setDisplayValue(displayValue);
                return;
            }
            
            //case C: server file path
            File inputFile = new File(value);
            if (displayFileInfo && inputFile.exists()) {
                //directory = inputFile.getParentFile().getName();
                setSize(inputFile.length());
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(inputFile.lastModified());
                setLastModified(cal.getTime());
            }

            String displayValue = inputFile.getName();
            
            File inputFileParent = inputFile.getParentFile();
            boolean isWebUpload = FileUtil.isWebUpload(inputFile);
            boolean isSoapUpload = false;
            boolean isInLibdir = taskLibDir != null;
            if (!isWebUpload) {
                isSoapUpload = FileUtil.isSoapUpload(inputFile);
            }
            
            if (isWebUpload) {
                log.debug("isWebUpload");
                String fileParam = "";
                if (inputFileParent != null) {
                    fileParam += inputFileParent.getName() + "/";
                }
                fileParam += inputFile.getName();
                //url encode fileParam
                try {
                    fileParam = URLEncoder.encode(fileParam, "UTF-8");
                }
                catch (UnsupportedEncodingException e) {
                    log.error("Error encoding inputFile param, '"+fileParam+"' "+e.getLocalizedMessage(), e);
                } 
                setLink(contextPath + "/getFile.jsp?job="+jobNumber+"&file="+fileParam);
            }
            else if (isSoapUpload) {
                log.debug("isSoapUpload");
                //http://127.0.0.1:8080/gp/getFile.jsp?task=&job=1387&file=test/Axis30305.att_all_aml_test.gct
                //TODO don't really know the job #, should be the original job number for when this file was uploaded
                setLink(contextPath + "/getFile.jsp?task=&file="+inputFileParent.getName()+"/"+inputFile.getName());
                //special case for axis
                if (displayValue.startsWith("Axis")) {
                    displayValue = displayValue.substring(displayValue.indexOf('_') + 1);
                }
            }
            else if (isInLibdir) {
                String taskLsid = taskInfo.getLsid();
                setLink(contextPath + "/getFile.jsp?task="+taskLsid+"&file="+inputFile.getName());
            }
            else {
                log.debug("isServerFilePath");
                displayValue = origValue;
                if (inputFile.canRead()) {
                    //TODO: calculate proper permissions for links to files on the server file path
                    
                    //TODO: set a link for downloading (via the web client) a server file path
                    //try {
                        //URI inputFileURI = inputFile.toURI();
                        //URL inputFileURL = inputFileURI.toURL();
                        //String inputFilePath = inputFileURL.getPath();
                        //setLink(contextPath + "/serverFilePath/" + inputFilePath);
                       
                        // set a link for downloading a file on the server file path
                        final String link = contextPath + "/data/" + inputFile.getPath();
                        setLink(link);
                    //}
                    //catch (MalformedURLException e) {
                    //    log.error(e);
                    //}
                }
            }
            this.setDisplayValue(displayValue);
        }
    }
    
    private JobInfo jobInfo = null;
    private TaskInfo taskInfo = null;
    private PipelineModel pipelineModel = null;
    private Long size = null;
    private boolean includeInputFilesInSize = false;

    /**
     * Get the total size of all of the output files for this job, including all descendant jobs.
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
    private List<ParameterInfoWrapper> directoryInputs = new ArrayList<ParameterInfoWrapper>();
    public List<ParameterInfoWrapper> getDirectoryInputs() {
        return directoryInputs;
    }

    public void setDirectoryInputs(List<ParameterInfoWrapper> directoryInputs) {
        this.directoryInputs = directoryInputs;
    }

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
    
    //public void setJobInfo(boolean showExecutionLogs, String servletContextPath, Map<String, Collection<TaskInfo>> kindToModules, JobInfo jobInfo) {
    public void setJobInfo(boolean showExecutionLogs, String servletContextPath, JobInfo jobInfo) {
        this.servletContextPath = servletContextPath;
        this.showExecutionLogs = showExecutionLogs;
        this.jobInfo = jobInfo;
        //this.kindToModules = kindToModules;
        String jobDir = GenePatternAnalysisTask.getJobDir(""+jobInfo.getJobNumber());
        this.outputDir = new File(jobDir);
        processParameterInfoArray();
        this.jobPermissionsBean = null;
        
        initPurgeDate();
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
    public String getTaskDescription() {
        if (taskInfo != null) {
            return taskInfo.getDescription();
        }
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
    //--- end JobInfo wrapper methods
    public long getElapsedTimeMillis() {
        if (jobInfo != null) {
            if (jobInfo.getDateSubmitted() == null) {
                return 0;
            }
            if (jobInfo.getDateCompleted() == null) {
                return System.currentTimeMillis() - jobInfo.getDateSubmitted().getTime();
            }
            return jobInfo.getDateCompleted().getTime() - jobInfo.getDateSubmitted().getTime();
        }
        log.error("jobInfo is null");
        return 0L;
    }

    /**
     * helper method which indicates if the job has completed processing.
     */
    public boolean isFinished() {
        if ( JobStatus.FINISHED.equals(getStatus()) ||
                JobStatus.ERROR.equals(getStatus()) ) {
            return true;
        }
        return false;        
    }
    
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

    private static final DateFormat purgeDateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    private Date purgeDate = null;
    private String formattedPurgeDate = "";

    private void initPurgeDate() {
        if (jobInfo != null) {
            purgeDate = JobPurgerUtil.getJobPurgeDate(jobInfo.getDateCompleted());
        }
        if (purgeDate != null) {
            formattedPurgeDate = purgeDateFormat.format(purgeDate);
        }
    }
    
    /**
     * @return the date that this job will be purged from the server.
     */
    public Date getPurgeDate() {
        return purgeDate;
    }

    /**
     * @return the date, formatted for the web page, that this job will be purged from the server.
     */
    public String getFormattedPurgeDate() {
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
            if (param._isDirectory()) {
                ParameterInfoWrapper directory = new ParameterInfoWrapper(param);
                directoryInputs.add(directory);
            }
            
            if (param.isOutputFile()) {
                //OutputFile outputFile = new OutputFile(kindToModules, outputDir, servletContextPath, jobInfo, param);
                OutputFile outputFile = new OutputFile(outputDir, servletContextPath, jobInfo, param);
                if (outputFile.isChildJobResult) {
                    outputFilesAndTaskLogs.add(outputFile);
                    if (!outputFile.isTaskLog()) {
                        outputFiles.add(outputFile);
                    }
                }
            }
            else {
                ParameterInfoWrapper inputParam = null;
                //ParameterInfo formalParam = ParameterInfoWrapper.getFormalParameter(formalParams, param);
                //String uiValue = param.getUIValue(formalParam);
                String value = param.getValue();
                if (value != null && !"".equals(value)) {
                    if (isInputFile(param)) {
                       InputFile inputFile = new InputFile(taskInfo, jobInfo, servletContextPath, value, param);
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
        
        postProcessOutputFiles();
        postProcessInputFiles();
        
        //for debugging only, 
        //change this flag to include the raw xml from the analysis_job table in the input parameters
        //which are displayed on the job status page
        //boolean debug = false;
        //debug = true;
        boolean displayParameterInfoDetails = false;
        //displayParameterInfoDetails = ServerConfiguration.instance().getGPBooleanProperty(userContext, "display.parameter.info.details", false);
        if (displayParameterInfoDetails) {
            //list each item in the parameter info array (including nested parameter attributes)
            int i=0;
            for(ParameterInfo p : jobInfo.getParameterInfoArray()) {
                inputParameters.add(new ParameterInfoWrapper(new ParameterInfo("p["+i+"]: "+p.getName(), p.getValue(), "")));
                ++i;
                int j=0;
                for(Object attrName : p.getAttributes().keySet()) {
                    Object attrVal = p.getAttributes().get(attrName);
                    inputParameters.add(new ParameterInfoWrapper(new ParameterInfo("     attr["+j+"]: "+attrName, ""+attrVal, "")));
                    ++j;
                }
            }
            
            //list each output file
            i=0;
            for(OutputFile o : this.getOutputFiles()) {
                ++i;
                ParameterInfo oPi = new ParameterInfo("o["+i+"]: "+o.getName(), o.getValue(), "");
                inputParameters.add(new ParameterInfoWrapper(oPi));
            }

            //also show the parameter_info CLOB
            String name = "analysis_job.parameter_info";
            String value = jobInfo.getParameterInfo();
            String description = "The raw entry in the PARAMETER_INFO column of the ANALYSIS_JOB table";
            ParameterInfo param = new ParameterInfo(name, value, description);
            ParameterInfoWrapper paramInfoWrapper = new ParameterInfoWrapper(param);
            inputParameters.add(paramInfoWrapper);
        }
    }
    
    /**
     * After the first pass through the list of parameters, postProcess all of the output files to set up the pop-up menu.
     */
    private void postProcessOutputFiles() {
        Map<String,Set<TaskInfo>> taskInfoMap = getFileTypeToTaskInfoMap();        
        for(OutputFile outputFile : getOutputFiles()) {
            if (outputFile.isTaskLog()) {
                //ignore task log files
            }
            else {
                //add an item to the popup menu
                String type = SemanticUtil.getKind(outputFile.getOutputFile());
                Set<TaskInfo> sendToTasks = taskInfoMap.get(type);
                if (sendToTasks != null) {
                    for(TaskInfo taskInfo : sendToTasks) {
                        outputFile.addModuleMenuItem(taskInfo);
                    }
                }
            }
        }
    }
    
    private void postProcessInputFiles() {
        Map<String,Set<TaskInfo>> taskInfoMap = getFileTypeToTaskInfoMap();        
        for(InputFile inputFile : getInputFiles()) {
            //add an item to the popup menu
            File file = inputFile.getInputFile();
            String type = null;
            if (file != null) {
                type = SemanticUtil.getKind(file);
            }
            else {
                type = extractFileExtension(inputFile.getDisplayValue());
            }
            Set<TaskInfo> sendToTasks = taskInfoMap.get(type);
            if (sendToTasks != null) {
                for(TaskInfo taskInfo : sendToTasks) {
                    inputFile.addModuleMenuItem(taskInfo);
                }
            }
        }
    }
    
    private String extractFileExtension(String name) {
        String[] parts = name.split("\\.");
        if (parts.length <= 1) {
            return "";
        }
        return parts[parts.length-1];
    }
    
    /**
     * @return a Map which maps each file type to a set of TaskInfo which accept the file type.
     */
    private Map<String,Set<TaskInfo>> getFileTypeToTaskInfoMap() {
        String currentUserId = UIBeanHelper.getUserId();
        IAuthorizationManager authManager = AuthorizationManagerFactory.getAuthorizationManager();
        boolean isAdmin = (authManager.checkPermission("adminServer", currentUserId) || authManager.checkPermission("adminModules", currentUserId));
        List<TaskInfo> allTaskInfos = getAllTaskInfosForUser(isAdmin, currentUserId);
        return SemanticUtil.getKindToModulesMap(allTaskInfos);
    }
    
    private List<TaskInfo> getAllTaskInfosForUser(boolean isAdmin, String currentUserId) {
        AdminDAO adminDao = new AdminDAO();
        TaskInfo[] latestTaskArray = adminDao.getLatestTasks(currentUserId);
        return Arrays.asList(latestTaskArray);
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
        //special case: URL input via SOAP interface
        String mode = (String) param.getAttributes().get("MODE");
        boolean dirFlag = true;
        if (type != null && type.equals("DIRECTORY")) {
        	dirFlag = false;
        }
        if (mode != null && mode.equals("URL_IN") && dirFlag) {
            return true;
        }
        return false;
    }
    
    //support for visualizers
    public boolean isVisualizer() {
        return this.visualizerAppletTag != null && !"".equals(this.visualizerAppletTag);
    }

    /**
     * The value of the 'id' attribute to the applet tag for this visualizer, so that you can access the visualizer with JavaScript.
     * E.g. Document.getElementById().
     * 
     * Note: This value is set even for jobs which aren't visualizers.
     * @return
     */
    public String getVisualizerAppletId() {
        if (jobInfo == null) {
            return "";
        }
        return "vis_" + jobInfo.getJobNumber();
    }

    /**
     * The value of the 'name' attribute to the applet tag for this visualizer, so that you can access the visualizer with JavaScript,
     * E.g. document.#{jobInfo.visualizerAppletName}.
     * 
     * Note: This value is set even for jobs which aren't visualizers.
     * @return
     */
    public String getVisualizerAppletName() {
        if (jobInfo == null) {
            return "";
        }
        return jobInfo.getTaskName() + "_" + jobInfo.getJobNumber();
    }
    
    public boolean getHasVisualizer() {
    	if (isVisualizer()) {
    	    return true;
    	}
    	else if (isPipeline()) {
    		for (JobInfoWrapper child : children) {
    			if (child.isVisualizer()) {
    			    return true;
    			}
    		}
    	}
    	return false;
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
    
    private PipelineModel getPipelineModel() {
        if (pipelineModel == null && isPipeline()) {
            pipelineModel = getPipelineModel(taskInfo);
        }
        return pipelineModel;
    }

    private PipelineModel getPipelineModel(TaskInfo taskInfo) {
        PipelineModel model = null;
        if (taskInfo != null) {
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
    
    public Integer[] getNumStepsInPipeline() {
        int numStepsInPipeline = 0;
        PipelineModel pm = this.getPipelineModel();
        if (pm != null) {
            numStepsInPipeline = pm.getTasks().size();
        }
        return new Integer[numStepsInPipeline];
    }

    public int getNumStepsInPipelineRecursive() {
        PipelineModel pm = this.getPipelineModel();
        return getNumStepsInPipelineRecursive(pm);
    }

    public int getNumVisualizers() {
        if (isVisualizer()) {
            return 1;
        }
        if (isPipeline()) {
            PipelineModel pm = this.getPipelineModel();
            return getNumVisualizersInPipelineRecursive(pm);
        }
        return 0;        
    }

    private int getNumVisualizersInPipelineRecursive(PipelineModel pm) {
        if (pm == null) {
            return 0;
        }
        int count = 0;
        for(JobSubmission jobSubmission : pm.getTasks()) {
            TaskInfo taskInfo = jobSubmission.getTaskInfo();
            if (taskInfo != null && TaskInfo.isVisualizer(taskInfo.getTaskInfoAttributes())) {
                count += 1;
            }
            else if (taskInfo != null && taskInfo.isPipeline()) {
                PipelineModel sub = this.getPipelineModel(taskInfo);
                count += getNumVisualizersInPipelineRecursive(sub);
            }
        }
        return count;
    }

    /**
     * Get a count of all jobs (excluding pipelines), including the count of jobs in nested pipelines.
     * @param pm
     * @return
     */
    private int getNumStepsInPipelineRecursive(PipelineModel pm) {
        if (pm == null) {
            return 0;
        }
        int level0count = pm.getTasks().size();
        int total = level0count;
        for(JobSubmission jobSubmission : pm.getTasks()) {
            TaskInfo ti = jobSubmission.getTaskInfo();
            if (ti.isPipeline()) {
                total -= 1;
                PipelineModel sub = this.getPipelineModel(ti);
                total += getNumStepsInPipelineRecursive(sub);
            }
        }
        return total;
    }

    /**
     * For debugging, or as a future improvement,
     * get the number of steps in the pipeline, based on the number of child jobs,
     * rather than based on the PipelineModel.
     */
    private Integer[] getNumStepsInPipelineFromJob() {
        int numStepsInPipeline = 0;
        if (children != null) {
            numStepsInPipeline = children.size();
        }
        return new Integer[numStepsInPipeline];
    }

    /**
     * For debugging, or as a future improvement,
     * get the number of visualizers in the pipeline,
     * based on the number of visualizers in child jobs,
     * rather than based on the PipelineModel.
     * @return
     */
    private int getNumVisualizersFromJob() {
        if (isVisualizer()) {
            return 1;
        }
        
        int count = 0;
        if (isPipeline()) {
            for(JobInfoWrapper step : getAllSteps()) {
                if (step.isVisualizer()) {
                    ++count;
                }
            }
        }
        return count;
    }
    

    private int currentStepInPipeline = 0;
    public int getCurrentStepInPipeline() {
        return currentStepInPipeline;
    }
    
    //Job Permissions methods
    public JobPermissionsBean getPermissions() {
        if (jobPermissionsBean == null) {
            initGroupPermissions();
        }
        return jobPermissionsBean;
    }
    
    private void initGroupPermissions() { 
        if (jobInfo == null) {
            log.error("jobInfo is null");
            return;
        }

        String currentUserId = UIBeanHelper.getUserId();
        final boolean isAdmin = AuthorizationHelper.adminJobs(currentUserId);
        JobInfoWrapper root = this.getRoot();
        String rootJobOwner = root.getUserId();
        int rootJobNumber = root.getJobNumber();

        PermissionsHelper ph = new  PermissionsHelper(isAdmin, currentUserId, jobInfo.getJobNumber(), rootJobOwner, rootJobNumber);
        jobPermissionsBean = new JobPermissionsBean(ph);
    }
    
    //for debugging
    public String getRandom() {
        return "r_" + System.currentTimeMillis() + ": " + Math.rint( 10.0 * Math.random() );
    }

}