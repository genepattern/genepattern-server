/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webservice.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.dm.GetFileDotJspUtil;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.webapp.PipelineCreationHelper;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.WebServiceException;

public class ProvenanceFinder {
    private static Logger log = Logger.getLogger(ProvenanceFinder.class);

    private final String serverURL;
    private String userID = null;
    //the max size (in bytes) for copying a file into the taskLib
    // pipeline.max.file.size
    private long maxFileSize = 0L;
    private ArrayList filesToCopy = new ArrayList();
    private ProvenancePipelineResult result = new ProvenancePipelineResult();

    private static String getServerURLtoUpper() {
        String serverURL = ServerConfigurationFactory.instance().getGpUrl();
        if (serverURL == null || serverURL.trim().length() == 0) {
            try {
                String portStr = System.getProperty("GENEPATTERN_PORT", "");
                portStr = portStr.trim();
                if (portStr.length()>0) {
                    portStr = ":"+portStr;
                }
                serverURL = "http://" + InetAddress.getLocalHost().getCanonicalHostName() + portStr;
                serverURL = serverURL.toUpperCase();
            } 
            catch (Exception e) {
                log.error(e);
            }
        }
        serverURL = serverURL.toUpperCase();
        return serverURL;
    }

    public ProvenanceFinder(final String user) {
        this.userID = user;
        this.serverURL=getServerURLtoUpper();

        //init system configured setting for max file size
        GpContext userContext = GpContext.getContextForUser(userID);
        final Long defaultMaxFileSize = 250L * 1000L * 1024L; //250 mB
        maxFileSize = ServerConfigurationFactory.instance().getGPLongProperty(userContext, "pipeline.max.file.size", defaultMaxFileSize);
    }

    public ProvenancePipelineResult createProvenancePipeline(Set<JobInfo> jobs, String pipelineName) {
        String lsid = null;

        try {
            PipelineModel model = this.createPipelineModel(jobs, pipelineName);
            PipelineCreationHelper helper = new PipelineCreationHelper(model);
            lsid = helper.generateTask();
            model.setLsid(lsid);
            copyFilesToPipelineDir(lsid, pipelineName);
            result.setLsid(lsid);
        }
        catch (Exception e) {
            log.error(e);
        }
        return result;
    }

    public ProvenancePipelineResult createProvenancePipeline(String filename, String pipelineName) {
        String lsid = null;
        Set<JobInfo> jobs = this.findJobsThatCreatedFile(filename);
        return createProvenancePipeline(jobs, pipelineName);
    }

    public Set<JobInfo> findJobsThatCreatedFile(String fileURL) {
        log.debug("A. Looking for creators of " + fileURL);
    	
        ArrayList<String> files = new ArrayList<String>();
        Set<JobInfo> jobs = new TreeSet<JobInfo>(new Comparator<JobInfo>() {
            public int compare(JobInfo j1, JobInfo j2) {
                if (j1.getJobNumber() > j2.getJobNumber()) {
                    return 1;
                }
                else if (j1.getJobNumber() < j2.getJobNumber()) {
                    return -1;
                }
                else {
                    return 0;
                }
            }
        });

        files.add(fileURL);

        while (!files.isEmpty()) {
            String aFile = files.get(0);
            if (aFile == null) {
                continue;
            }

            JobInfo job = findJobThatCreatedFile(aFile);
            if (job != null){
                if (!jobs.contains(job)) {
                    files.addAll(getLocalInputFiles(job));
                    jobs.add(job);
                }
            }
	 	  
            files.remove(0);
        }
        return jobs;
    }

    /**
     * Given a file URL find the Job that created it or return null. Must be a job output file
     */
    public JobInfo findJobThatCreatedFile(String fileURL) {
        String jobNoStr = getJobNoFromURL(fileURL);
        if (jobNoStr == null) {
            try {
                // maybe just a job # passed in
                Integer.parseInt(fileURL);
                jobNoStr = fileURL;
            } 
            catch (NumberFormatException nfe) {
            }
        }
        int jobid = -1;
        try {
            jobid = Integer.parseInt(jobNoStr);
            return new AnalysisDAO().getJobInfo(jobid);
        } 
        catch (Throwable t) {
            log.error("Error getting JobInfo for jobid="+jobNoStr, t);
            return null;
        }
    }

    /**
     * Given an reverse ordered set of jobs (ordered by decreasing Job #) create a pipeline model that represents it
     * with the appropriate file inheritence representing the original jobs
     */
    protected PipelineModel createPipelineModel(Set<JobInfo> jobs, String pipelineName) 
    throws OmnigeneException, WebServiceException 
    {
        filesToCopy = new ArrayList();
        // create an array list with the taskinfos at their taskid location for easier retrieval later
        Collection<TaskInfo> taskCatalog = new LocalAdminClient(userID).getTaskCatalog();
        HashMap<String, TaskInfo> taskList = new HashMap<String, TaskInfo>();
        HashMap<Integer, Integer> jobOrder = new HashMap<Integer, Integer>();

        for (Iterator<TaskInfo> iter = taskCatalog.iterator(); iter.hasNext();) {
            TaskInfo ti = iter.next();
            taskList.put(ti.getLsid(), ti);
        }

        String taskLSID = "";

        PipelineModel model = new PipelineModel();
        model.setName(pipelineName); // XXX
        model.setDescription("describe it here");// XXX
        model.setAuthor(userID);
        model.setUserID(userID);
        model.setLsid(taskLSID); // temp pipeline
        model.setVersion("0");
        model.setPrivacy(GPConstants.PRIVATE);
        int i = 0;
        for (Iterator<JobInfo> iter = jobs.iterator(); iter.hasNext(); i++) {
            JobInfo job = iter.next();
            if (!job.getUserId().equals(userID) && !AuthorizationHelper.adminJobs(userID)) {
                throw new WebServiceException("You do not have the required permission access job number "
                        + job.getJobNumber() + ".");
            }
            jobOrder.put(new Integer(job.getJobNumber()), new Integer(i));
            // map old job number to order in pipeline

            TaskInfo mTaskInfo = taskList.get(job.getTaskLSID());
            if (mTaskInfo == null) {
                throw new WebServiceException("Could not find job number: " + job.getJobNumber() + ", module: "
                        + job.getTaskName() + " in module list.");
            }
            TaskInfoAttributes mTia = mTaskInfo.giveTaskInfoAttributes();
            boolean isVisualizer = TaskInfo.isVisualizer(mTaskInfo.getTaskInfoAttributes());
            
            final List<ParameterInfo> adjustedParamList = createPipelineParams(job.getParameterInfoArray(), mTaskInfo.getParameterInfoArray(), jobOrder);
            ParameterInfo[] adjustedParams = new ParameterInfo[ adjustedParamList.size() ];
            boolean[] runTimePrompt = new boolean[ adjustedParamList.size() ];
            int j=0;
            for(ParameterInfo paramInfo : adjustedParamList) {
                adjustedParams[j] = paramInfo;
                boolean isPromptWhenRun = "1".equals(paramInfo.getAttributes().get("runTimePrompt"));                    
                runTimePrompt[j] = isPromptWhenRun; 
                if (isPromptWhenRun) {
                    //... add prompt when run parameter to the pipeline,
                    //    reverse-engineered from makePipeline.jsp
                    // i is the taskNum
                    int taskNum = i;
                    String taskName = job.getTaskName();
                    String paramName = paramInfo.getName();
                    model.addInputParameter(taskName + (taskNum + 1) + "." + paramName, paramInfo);
                }
                ++j;
            }
            JobSubmission jobSubmission = new JobSubmission(mTaskInfo.getName(), mTaskInfo.getDescription(), 
                    mTia.get(GPConstants.LSID), adjustedParams, runTimePrompt, isVisualizer, mTaskInfo);

            model.addTask(jobSubmission);
        }
        return model;
    }
    
    protected void copyFilesToPipelineDir(String pipelineLSID, String pipelineName) {
        String attachmentDir = null;
        try {
            attachmentDir = DirectoryManager.getTaskLibDir(pipelineName, pipelineLSID, userID);
        }
        catch (Exception e) {
            log.error("Could not copy files for pipeline: " + pipelineLSID, e);
            return;
        }

        File dir = new File(attachmentDir);
        dir.mkdir();
        byte[] buf = new byte[100000];
        int j;

        for (Iterator iter = filesToCopy.iterator(); iter.hasNext();) {
            File aFile = (File) iter.next();
            FileInputStream is = null;
            FileOutputStream os = null;
            try {
                is = new FileInputStream(aFile);
                os = new FileOutputStream(new File(dir, aFile.getName()));
                while ((j = is.read(buf, 0, buf.length)) > 0) {
                    os.write(buf, 0, j);
                }
            }
            catch (IOException e) {
                log.error("Could not copy file " + aFile.getAbsolutePath() + " todir " + dir.getAbsolutePath(), e);
            }
            finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                }
                catch (IOException e) {
                }
                try {
                    if (os != null) {
                        os.close();
                    }
                }
                catch (IOException e) {
                }
            }
        }
        filesToCopy.clear();
    }

    protected String getJobNoFromURL(String fileURL) {
        String j = getParamFromURL(fileURL, "job");
        if (j == null) {
            // maybe a "job#/filename"
            try {
                int idx = fileURL.indexOf(File.separator);
                String jobNumMaybe = fileURL.substring(0, idx);
                Integer.parseInt(jobNumMaybe);
                j = jobNumMaybe;
            }
            catch (Exception e) {
            }
        }

        if (fileURL != null) {
            log.debug("GJFU " + fileURL + "  " + j);
        }

        return j;
    }

    protected String getParamFromURL(String fileURL, String key) {
        // if it is null or not a local file we can do nothing
        String paramString = "";

        if (fileURL == null) {
            return null;
        }
	
        if (!(fileURL.toUpperCase().startsWith(serverURL)) 
                && !fileURL.startsWith("http://127.0.0.1")
                && !fileURL.startsWith("http://localhost")) {
            log.debug("HERE "+ fileURL);
            return null;
        }

        // if it is not a result file do nothing
        boolean isResultFile = fileURL.indexOf("jobResults") >= 0;
        if (! isResultFile){
            int idx1 = fileURL.indexOf("/");
            String jobNoMaybe = fileURL.substring(0,idx1);
            try {
                Integer i = new Integer(jobNoMaybe);
                isResultFile = true;
            } 
            catch (NumberFormatException nfe){
            }
        }
	
        if (!((fileURL.indexOf("retrieveResults.jsp") >= 1) || (isResultFile))) {
            return null;
        }

        if (isResultFile && "job".equals(key)) {
            int idx = fileURL.indexOf("jobResults");
            idx += 11;
            int endidx = fileURL.indexOf('/', idx);
            if (endidx == -1) {
                endidx = fileURL.indexOf("%2F", idx);
            }
            log.debug("GPFU "+ key + "  " + fileURL + " " + idx + " " + endidx);
		
            paramString = fileURL.substring(idx, endidx);
        } 
        else if (isResultFile && "filename".equals(key)) {
            int idx = fileURL.indexOf("jobResults");
            idx += 11;
            int endidx = fileURL.indexOf('/', idx);
            paramString = fileURL.substring(endidx + 1);
        } 
        else {
            // now we think we have a local result file url so grab the job #
            int idx = fileURL.indexOf(key + "=");
            if (idx < 0) {
                return null; // can't find a job #
            }
            int endIdx = fileURL.indexOf("&", idx);
            if (endIdx == -1) {
                endIdx = fileURL.length();
            }
            paramString = fileURL.substring(idx + 1 + key.length(), endIdx);
        }
        return paramString;
    }

    protected ArrayList<String> getLocalInputFiles(JobInfo job) {
        log.debug(" GLIF inputs for " + job.getJobNumber());
        ArrayList<String> inputFiles = new ArrayList<String>();
        if (job == null) {
            return inputFiles;
        }
        ParameterInfo[] params = job.getParameterInfoArray();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                String val = getURLFromParam(params[i]);
                if (val != null) {
                    inputFiles.add(val);
                }
            }
        }
        return inputFiles;
    }

    public String getURLFromParam(ParameterInfo pinfo) {
        HashMap attributes = pinfo.getAttributes();
        String pvalue = pinfo.getValue();
        
        //handle '<GenePatternURL>'
        if (pvalue.startsWith("<GenePatternURL>")) {
            pvalue=serverURL + pvalue.substring("<GenePatternURL>".length());
        }

        if (pvalue.toUpperCase().startsWith(serverURL.toUpperCase()) || pvalue.toUpperCase().startsWith(serverURL) || pvalue.toUpperCase().startsWith("HTTP://LOCALHOST") || pvalue.toUpperCase().startsWith("HTTP://127.0.0.1")) {
            log.debug("\t\t" + pinfo.getName() + "=" + pvalue);

            return pvalue;
        }
        else if ("FILE".equals(attributes.get("TYPE"))) {

            if ("CACHED_IN".equals(attributes.get("MODE"))) {
                int idx = pvalue.indexOf("/");
                String jobstr = pvalue.substring(0, idx);
                String filename = pvalue.substring(idx + 1);

                return serverURL + "/gp/jobResults/" + jobstr + "/" + filename;
            }
            else {

                return pvalue;

            }
        }

        return null;
    }

    /**
     * realigning input files from the old job params to the new pipeline. Look for input files matching the request
     * pattern like we did to find these jobs and replace with gpUseResult() calls. Create a new ParameterInfo array to
     * return.
     */
    protected List<ParameterInfo> createPipelineParams(ParameterInfo[] oldJobParams, ParameterInfo[] taskParams, HashMap jobOrder) 
    throws WebServiceException 
    {
        List<ParameterInfo> newParams = new ArrayList<ParameterInfo>();
        
        for (int i = 0; i < taskParams.length; i++) {
            ParameterInfo taskParam = taskParams[i];
            ParameterInfo oldJobParam = null;
            for (int j = 0; j < oldJobParams.length; j++) {
                oldJobParam = oldJobParams[j];
                if (oldJobParam.getName().equalsIgnoreCase(taskParam.getName())) {
                    break;
                }
            }
            boolean promptWhenRun = false;
            HashMap attrs = oldJobParam.getAttributes();
            String value = getURLFromParam(oldJobParam);
            String jobNoStr = getJobNoFromURL(value);
            if (jobNoStr == null) {
                // for files that are on the server, replace with generic URL with LSID to be substituted at runtime
                // for anything else leave it unmodified
                value = oldJobParam.getValue();
                File inFile = new File(value);
                
                if (!inFile.exists()) {
                    if (taskParam.isInputFile()) {
                        //this could be a user upload file
                        try {
                            GpFilePath inputFilePath = GpFileObjFactory.getRequestedGpFileObj(value);
                            inFile = inputFilePath.getServerFile();
                        }
                        catch (Throwable t) {
                            //it is expected that exceptions are thrown,
                            //such as when the initial value is an inherited default value
                            log.debug("Error c");
                        }
                    }
                }
                
                if (!inFile.exists()) {
                    if (taskParam.isInputFile()) {
                        //this could be a web upload file for a reloaded job
                        try {
                            GpFilePath inputFilePath = GetFileDotJspUtil.getRequestedGpFileObjFromGetFileDotJsp(value);
                            if (inputFilePath != null) {
                                inFile = inputFilePath.getServerFile();
                            }
                        }
                        catch (Throwable t) {
                            //because this is a hack, it is expected that exceptions are thrown, such as when the 
                            //initial value is an inherited default value
                            log.debug("Error creating pipeline parameter from previous job, "+taskParam.getName()+"="+value+": "+t.getLocalizedMessage(), t);
                        }
                    }
                }
                
                if (inFile.exists()) { 
                    if (inFile.length() > maxFileSize) {
                        //special-case, replace large files with 'prompt when run'
                        promptWhenRun = true;
                        result.getReplacedParams().add(oldJobParam);
                        log.error("Ignoring input file because it exceeds pipeline.max.file.size for user, \n\t"+
                                "inFile="+value+" size ="+inFile.length()+" bytes");
                        //TODO: communicate this back to end user
                    }
                    else { 
                        filesToCopy.add(inFile);
                        value = "<GenePatternURL>getFile.jsp?task=" + GPConstants.LEFT_DELIMITER + GPConstants.LSID + GPConstants.RIGHT_DELIMITER + "&file=" + URLEncoder.encode(inFile.getName());
                    }
                }
            }
            else {
                // figure out the jobs order in the new pipeline and use gpUseResult
                Integer jobNo = new Integer(jobNoStr);
                Integer pipeNo = (Integer) jobOrder.get(jobNo);
                attrs.put(PipelineModel.INHERIT_TASKNAME, "" + pipeNo);
                JobInfo priorJob = new AnalysisDAO().getJobInfo(jobNo);
                String name = getParamFromURL(value, "filename");
                
                // Special case code for handling special characters
                try {
                    name = URLDecoder.decode(name, "UTF-8");
                }
                catch (UnsupportedEncodingException e) {
                    log.error("Error decoding filename: " + name);
                }
                
                //
                // XXX use file index for now, 
                // Change to file type when I understand how
                // to get the right information
                //
                ParameterInfo[] pjp = priorJob.getParameterInfoArray();
                int fileIdx = 0;
                for (int j = 0; j < pjp.length; j++) {
                    if (pjp[j].isOutputFile()) {
                        fileIdx++;
                        log.debug("CPP " + pjp[j].getValue() + " " + name);
                        if (name != null) {
                            if (pjp[j].getValue().endsWith(name)) {
                                attrs.put(PipelineModel.INHERIT_FILENAME, "" + fileIdx);
                            }
                        }
                    }
                }

                // now we figure out which file to use from the job
                value = "";
            }
            // HACK++: special-case for when the value still has a literal
            String genePatternUrl = ServerConfigurationFactory.instance().getGpUrl();
            if (value != null && genePatternUrl != null && value.startsWith(genePatternUrl)) {
                value = value.replace(genePatternUrl, "<GenePatternURL>");
            }
            ParameterInfo paramInfo = new ParameterInfo(taskParam.getName(), value, taskParam.getDescription());
            paramInfo.setAttributes(attrs);
            //special-case for 'prompt when run' parameters
            //reverse-engineered from makePipeline.jsp
            if (promptWhenRun) {
                paramInfo.getAttributes().put("runTimePrompt", "1");
                paramInfo.setDescription("");
                paramInfo.setValue("");
            }
            newParams.add(paramInfo);
        }
        return newParams;
    }
    
    public class ProvenancePipelineResult {
        private String lsid;
        private List<ParameterInfo> replacedParams = new ArrayList<ParameterInfo>();
        
        public String getLsid() {
            return lsid;
        }
        
        public void setLsid(String lsid) {
            this.lsid = lsid;
        }
        
        public List<ParameterInfo> getReplacedParams() {
            return replacedParams;
        }
        
        public void setReplacedParams(List<ParameterInfo> replacedParams) {
            this.replacedParams = replacedParams;
        }
        
    }
}
