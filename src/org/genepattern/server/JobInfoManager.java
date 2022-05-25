/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server;

import static org.genepattern.util.GPConstants.TASKLOG;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.server.JobInfoWrapper.OutputFile;
import org.genepattern.server.JobInfoWrapper.ParameterInfoWrapper;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.ExternalFileManager;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.executor.drm.dao.JobRunnerJob;
import org.genepattern.server.executor.drm.dao.JobRunnerJobDao;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.genepattern.JavascriptHandler;
import org.genepattern.server.job.status.JobStatusLoaderFromDb;
import org.genepattern.server.job.status.Status;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.user.UserProp;
import org.genepattern.server.user.UserPropKey;
import org.genepattern.server.webapp.WritePipelineExecutionLog;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.util.GPConstants;
import org.genepattern.visualizer.RunVisualizerConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;

/**
 * Get job status information.
 *
 * @author pcarr
 */
public class JobInfoManager {
    private static Logger log = Logger.getLogger(JobInfoManager.class);
    private static Logger elapsedTimeLog = Logger.getLogger("org.genepattern.server.JobInfoManager.JobElapsedTimeLog");
  
    
    //cache pipeline status so we don't need to make so many DB queries
    private static Map<Integer, Boolean> isPipelineCache = new ConcurrentHashMap<Integer, Boolean>();

    public static boolean isPipeline(JobInfo jobInfo) {
        boolean closeDbSession = true;
        return isPipeline(jobInfo, closeDbSession);
    }

    public static boolean isPipeline(JobInfo jobInfo, boolean closeDbSession) {
        if (jobInfo == null) {
            return false;
        }

        //check the cache
        int taskId = jobInfo.getTaskID();
        if (taskId >= 0) {
            Boolean status = isPipelineCache.get(taskId);
            if (status != null) {
                return status;
            }
        }

        boolean isPipeline = false;
        try {
            TaskInfo taskInfo = getTaskInfo(jobInfo, closeDbSession);
            isPipeline = taskInfo.isPipeline();
            isPipelineCache.put(taskInfo.getID(), isPipeline);
        } catch (Throwable t) {
            String msg = "Error in isPipeline";
            msg += ", jobInfo.taskName=" + jobInfo.getTaskName();
            msg += ", jobInfo.taskLSID=" + jobInfo.getTaskLSID();
            msg += ", jobInfo.taskID=" + jobInfo.getTaskID();
            log.error(msg, t);
        }
        return isPipeline;
    }

    public static boolean isVisualizer(JobInfo jobInfo) {
        boolean isVisualizer = false;
        if (jobInfo == null) {
            return false;
        }
        try {
            TaskInfo taskInfo = getTaskInfo(jobInfo);
            isVisualizer = TaskInfo.isVisualizer(taskInfo.getTaskInfoAttributes());
        } catch (Exception e) {
            log.error(e);
        }
        return isVisualizer;
    }

    public static TaskInfo getTaskInfo(JobInfo jobInfo) throws TaskIDNotFoundException {
        boolean closeDbSession = true;
        return getTaskInfo(jobInfo, closeDbSession);
    }

    public static TaskInfo getTaskInfo(JobInfo jobInfo, boolean closeDbSession) throws TaskIDNotFoundException {
        return getTaskInfo(jobInfo.getTaskID(), closeDbSession);
    }

    public static TaskInfo getTaskInfo(int taskId) throws TaskIDNotFoundException {
        boolean closeDbSession = true;
        return getTaskInfo(taskId, closeDbSession);
    }

    public static TaskInfo getTaskInfo(int taskId, boolean closeDbSession) throws TaskIDNotFoundException {
        final HibernateSessionManager mgr = org.genepattern.server.database.HibernateUtil.instance();

        TaskInfo taskInfo = null;
        try {
            //calls HibernateUtil.beginTransaction...
            AdminDAO ds = new AdminDAO(mgr);
            taskInfo = ds.getTask(taskId);
            return taskInfo;
        } finally {
            //...must close the session here, or in an enclosing method
            if (closeDbSession) {
                mgr.closeCurrentSession();
            }
        }
    }

    /**
     * Get the current job status information by doing a db query.
     *
     * @param documentCookie
     * @param contextPath
     * @param currentUser
     * @param jobNo
     * @return null if the job is deleted.
     */
    public JobInfoWrapper getJobInfo(String documentCookie, String contextPath, String currentUser, int jobNo) {
        final HibernateSessionManager mgr = org.genepattern.server.database.HibernateUtil.instance();
        AnalysisDAO analysisDao = new AnalysisDAO(mgr);
        JobInfo jobInfo = analysisDao.getJobInfo(jobNo);
        if (jobInfo == null) {
            return null;
        }

        return getJobInfo(documentCookie, contextPath, currentUser, jobInfo);
    }

    public JobInfoWrapper getJobInfo(String documentCookie, String contextPath, String currentUser, JobInfo jobInfo) {
        return getJobInfo(documentCookie, contextPath, currentUser, jobInfo, false, null);
    }
    
    public JobInfoWrapper getJobInfo(String documentCookie, String contextPath, String currentUser, JobInfo jobInfo, boolean includeStatus, String gpUrl) {
        final HibernateSessionManager mgr = org.genepattern.server.database.HibernateUtil.instance();

        AnalysisDAO analysisDao = new AnalysisDAO(mgr);
        UserDAO userDao = new UserDAO(mgr);
        boolean showExecutionLogs = userDao.getPropertyShowExecutionLogs(currentUser);
        String visualizerJavaFlags = null;
        try {
            visualizerJavaFlags = getVisualizerJavaFlags(userDao, currentUser);
        } catch (Exception e){
            e.printStackTrace();
            // XXX JTL 08/03/2020  swallow as we don't care sometimes
        }

        AdminDAO adminDao = new AdminDAO(mgr);

        JobInfoWrapper jobInfoWrapper = processChildren(
                (JobInfoWrapper) null,
                showExecutionLogs,
                documentCookie,
                contextPath,
                analysisDao,
                adminDao,
                jobInfo,
                visualizerJavaFlags,
                includeStatus,
                gpUrl);

        //this call initializes the helper methods
        jobInfoWrapper.getPathFromRoot();
        return jobInfoWrapper;
    }

    /**
     * Create a new JobInfoWrapper, recursively looking up and including all child jobs.
     * Link each new JobInfoWrapper to its parent, which is null for top level jobs.
     *
     * @param parent
     * @param documentCookie
     * @param contextPath
     * @param analysisDao
     * @param jobInfo
     * @param visualizerJavaFlags
     * @param includeJobStatus
     * 
     * @return a new JobInfoWrapper
     */
    private JobInfoWrapper processChildren(
            JobInfoWrapper parent,
            boolean showExecutionLogs,
            String documentCookie,
            String contextPath,
            AnalysisDAO analysisDao,
            AdminDAO adminDao,
            JobInfo jobInfo,
            String visualizerJavaFlags,
            boolean includeJobStatus,
            String gpUrl) {
        TaskInfo taskInfo = null;
        try {
            //an exception is thrown if the module has been deleted
            String lsid = jobInfo.getTaskLSID();
            taskInfo = adminDao.getTask(lsid);
        } catch (Exception e) {
            //TODO: provide feedback in UI that the module for this job has been deleted 
            log.info("Error loading taskInfo for job '" + jobInfo.getJobNumber() + "', taskId=" + jobInfo.getTaskID() + "  : " + e.getLocalizedMessage(), e);
        }

        JobInfoWrapper jobInfoWrapper = new JobInfoWrapper();
        jobInfoWrapper.setParent(parent);
        //Note: must call setTaskInfo before setJobInfo
        jobInfoWrapper.setTaskInfo(taskInfo);
        jobInfoWrapper.setJobInfo(showExecutionLogs, contextPath, jobInfo);

        //special case for visualizers
        if (taskInfo != null && TaskInfo.isVisualizer(taskInfo.getTaskInfoAttributes())) {
            String tag = createVisualizerAppletTag(documentCookie, jobInfoWrapper, taskInfo, visualizerJavaFlags);
            jobInfoWrapper.setVisualizerAppletTag(tag);
        }
        
        if (includeJobStatus) {
            GpContext jobContext=new GpContext.Builder()
                .jobInfo(jobInfo)
            .build();
            Status jobStatus = new JobStatusLoaderFromDb(org.genepattern.server.database.HibernateUtil.instance(), gpUrl).loadJobStatus(jobContext);
            jobInfoWrapper.setJobStatus(jobStatus);
        }

        JobInfo[] children = analysisDao.getChildren(jobInfo.getJobNumber());
        for (JobInfo child : children) {
            JobInfoWrapper nextChild = processChildren(
                    jobInfoWrapper,
                    showExecutionLogs,
                    documentCookie,
                    contextPath,
                    analysisDao,
                    adminDao,
                    child,
                    visualizerJavaFlags,
                    includeJobStatus,
                    gpUrl);
            jobInfoWrapper.addChildJobInfo(nextChild);
        }

        return jobInfoWrapper;
    }

    private static String getVisualizerJavaFlags(UserDAO userDao, String userId) {
        UserProp userProp = userDao.getProperty(userId, UserPropKey.VISUALIZER_JAVA_FLAGS);
        String javaFlags = userProp.getValue();
        if (javaFlags == null) {
            @SuppressWarnings("deprecation")
            GpContext userContext = GpContext.getContextForUser(userId);
            javaFlags = ServerConfigurationFactory.instance().getGPProperty(userContext, RunVisualizerConstants.JAVA_FLAGS_VALUE);
        }
        return javaFlags;
    }

    private static String createVisualizerAppletTag(String documentCookie, JobInfoWrapper jobInfoWrapper, TaskInfo taskInfo, String visualizerJavaFlags) {
        try {
            final String GP_URL = UIBeanHelper.getServer();
            String name = jobInfoWrapper.getTaskName();

            TaskInfoAttributes taskInfoAttributes = taskInfo.giveTaskInfoAttributes();

            String os = taskInfoAttributes.get(GPConstants.OS);
            String cpuType = taskInfoAttributes.get(GPConstants.CPU_TYPE);

            String contextPath = jobInfoWrapper.getServletContextPath();
            String commandLine = taskInfoAttributes.get(GPConstants.COMMAND_LINE);

            Map<String, ParameterInfo> formalParameters = new HashMap<String, ParameterInfo>();
            Map<String, ParameterInfo> optionalParameters = new HashMap<String, ParameterInfo>();
            for (ParameterInfo formalParam : taskInfo.getParameterInfoArray()) {
                formalParameters.put(formalParam.getName(), formalParam);
                if (formalParam.isOptional()) {
                    optionalParameters.put(formalParam.getName(), formalParam);
                }
            }

            StringWriter appletTag = new StringWriter();
            //appletTag.append("<applet ");
            appletTag.append(" name=\"" + jobInfoWrapper.getVisualizerAppletName() + "\" id=\"" + jobInfoWrapper.getVisualizerAppletId() + "\" code=\""
                    + org.genepattern.visualizer.RunVisualizerApplet.class.getName()
                    + "\" archive=\"runVisualizer.jar,commons-httpclient.jar,commons-codec-1.6.jar\" codebase=\"/gp/downloads\" width=\"1\" height=\"1\" alt=\"Your browser can not run applets\">");

            appletTag.append("<param name=\"" + RunVisualizerConstants.NAME + "\" value=\"" + URLEncoder.encode(name, "UTF-8") + "\" >");
            appletTag.append("<param name=\"" + RunVisualizerConstants.OS + "\" value=\"" + URLEncoder.encode(os, "UTF-8") + "\">");
            appletTag.append("<param name=\"" + RunVisualizerConstants.CPU_TYPE + "\" value=\"" + URLEncoder.encode(cpuType, "UTF-8") + "\">");
            appletTag.append("<param name=\"" + RunVisualizerConstants.JAVA_FLAGS_VALUE + "\" value=\"" + URLEncoder.encode(visualizerJavaFlags, "UTF-8") + "\">");
            appletTag.append("<param name=\"" + RunVisualizerConstants.CONTEXT_PATH + "\" value=\"" + URLEncoder.encode(contextPath, "UTF-8") + "\">");

            StringBuffer paramNameList = new StringBuffer();
            StringBuffer paramNameValueList = new StringBuffer();
            StringBuffer downloadFiles = new StringBuffer();

            for (ParameterInfoWrapper inputParam : jobInfoWrapper.getInputParameters()) {
                String paramName = inputParam.getName();
                String paramValue = inputParam.getValue();
                if (paramValue != null) {
                    paramValue = paramValue.replace("\\", "\\\\");
                } else {
                    paramValue = "";
                }

                //process input file
                boolean isInputFile = false;
                ParameterInfo formalParam = formalParameters.get(paramName);
                boolean isOptional = false;
                if (formalParam != null) {
                    ParameterInfo optionalParam = optionalParameters.remove(paramName);
                    if (optionalParam != null) {
                        log.debug("optional parameter: " + paramName + "=" + paramValue);
                        isOptional = true;
                    }
                    isInputFile = formalParam.isInputFile();
                } else {
                    isInputFile = inputParam.getLink() != null;
                }
                if (isInputFile) {
                    String link = inputParam.getLink();
                    if (link != null) {
                        if (link.startsWith(contextPath)) {
                            //append server url
                            link = link.substring(jobInfoWrapper.getServletContextPath().length(), link.length());
                            if (GP_URL.endsWith("/")) {
                                link = link.substring(1);
                            }
                            link = GP_URL + link;
                        }
                        paramValue = link;
                    } else {
                        if (!isOptional) {
                            if (JobStatus.FINISHED.equals(jobInfoWrapper.getStatus())) {
                                final String currentStatus = jobInfoWrapper.getStatus();
                                log.warn("link not set for job=" + jobInfoWrapper.getJobNumber() + ": " + inputParam.getName() + "=" + inputParam.getValue() + ", currentStatus=" + currentStatus);
                            }
                        }
                    }
                }

                if (paramNameList.length() > 0) {
                    paramNameList.append(",");
                }
                paramNameList.append(paramName);
                paramNameValueList.append("<param name=\"" + paramName + "\" value=\"" + paramValue + "\">");
                if (isInputFile) {
                    if (downloadFiles.length() > 0) {
                        downloadFiles.append(",");
                    }
                    downloadFiles.append(inputParam.getName());
                }
            }

            //Bug GP-2605: add unset optional parameters to paramName and paramNameValueList
            for (ParameterInfo optionalParam : optionalParameters.values()) {
                String paramName = optionalParam.getName();
                String paramValue = "";
                if (paramNameList.length() > 0) {
                    paramNameList.append(",");
                }
                paramNameList.append(paramName);
                paramNameValueList.append("<param name=\"" + paramName + "\" value=\"" + paramValue + "\">");
            }

            appletTag.append("<param name=\"" + RunVisualizerConstants.PARAM_NAMES + "\" value=\"" + paramNameList.toString() + "\" >");
            appletTag.append(paramNameValueList.toString());
            appletTag.append("<param name=\"" + RunVisualizerConstants.DOWNLOAD_FILES + "\" value=\"" + URLEncoder.encode(downloadFiles.toString(), "UTF-8") + "\">");
            appletTag.append("<param name=\"" + RunVisualizerConstants.COMMAND_LINE + "\" value=\"" + URLEncoder.encode(commandLine, "UTF-8") + "\">");
            appletTag.append("<param name=\"" + RunVisualizerConstants.DEBUG + "\" value=\"1\">");

            StringBuffer fileNamesBuf = new StringBuffer();
            StringBuffer fileDatesBuf = new StringBuffer();
            String lsid = jobInfoWrapper.getTaskLSID();
            String libdir = DirectoryManager.getTaskLibDir(null, lsid, null);
            File[] supportFiles = new File(libdir).listFiles();
            for (int i = 0; i < supportFiles.length; i++) {
                if (i > 0) {
                    fileNamesBuf.append(",");
                    fileDatesBuf.append(",");
                }
                fileNamesBuf.append(supportFiles[i].getName());
                fileDatesBuf.append(supportFiles[i].lastModified());
            }
            appletTag.append("<param name=\"" + RunVisualizerConstants.SUPPORT_FILE_NAMES + "\" value=\"" + URLEncoder.encode(fileNamesBuf.toString(), "UTF-8") + "\" >");
            appletTag.append("<param name=\"" + RunVisualizerConstants.SUPPORT_FILE_DATES + "\" value=\"" + URLEncoder.encode(fileDatesBuf.toString(), "UTF-8") + "\" >");
            appletTag.append("<param name=\"" + RunVisualizerConstants.LSID + "\" value=\"" + URLEncoder.encode(lsid, "UTF-8") + "\" >");
            if (documentCookie != null && documentCookie.trim() != "") {
                appletTag.append("<param name=\"browserCookie\" value=\"" + documentCookie + "\">");
            }
            //appletTag.append("</applet>");
            return appletTag.toString();

        } catch (UnsupportedEncodingException e) {
            return "<p>Error in createVisualizerAppletTag: " + e.getLocalizedMessage() + "</p>";

        } catch (MalformedURLException e) {
            return "<p>Error in createVisualizerAppletTag: " + e.getLocalizedMessage() + "</p>";

        }
    }

    public static File writeExecutionLog(File outDir, JobInfoWrapper jobInfoWrapper) {
        File gpExecutionLog = new File(outDir, TASKLOG);
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(gpExecutionLog));
            writeExecutionLog(writer, jobInfoWrapper);
            return gpExecutionLog;
        } catch (IOException e) {
            log.error("Unable to create gp_execution_log: " + gpExecutionLog.getAbsolutePath(), e);
            return null;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    
    public static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyy-mm-dd hh:mm:ss.S"); 
    
    public static void writeExecutionLog(Writer writer, JobInfoWrapper jobInfoWrapper)
            throws IOException {
        writer.write("\n# Job: " + jobInfoWrapper.getJobNumber() );
        writer.write("\n# User: " + jobInfoWrapper.getUserId());
        writer.write("\n# Submitted: " + jobInfoWrapper.getDateSubmitted());
        writer.write("\n# Completed: " + dateFormatter.format(new Date()) );
        
        writer.write("\n# ET(ms): "  +jobInfoWrapper.getElapsedTimeMillis());
        
        String GP_URL = ServerConfigurationFactory.instance().getGpUrl();
        if (GP_URL != null) {
            writer.write("    server:  ");
            writer.write(GP_URL);
        }
        writer.write("\n# Module: " + jobInfoWrapper.getTaskName() + " " + jobInfoWrapper.getTaskLSID());

        // [optionally output the command line], 
        // this is turned off for improved security
        // TODO: make this a configurable parameter
        //boolean debug = false;
        //if (debug && processBuilder != null) {
        //    writer.write("\n# Command: ");
        //    String working_dir = "";
        //    if ( processBuilder.directory() != null ) {
        //        working_dir = processBuilder.directory().getCanonicalPath();
        //    }
        //    writer.write("\n#\tworking directory: "+working_dir);
        //    writer.write("\n#\tcommand line: ");
        //    for(String n : processBuilder.command()) {
        //        writer.write(n+" ");
        //    }
        //}

        writer.write("\n# Parameters: ");

        //case 2: pattern match for uploaded input file
        final String matchFileUploadPrefix = jobInfoWrapper.getServletContextPath() + "/getFile.jsp?file=";

        for (ParameterInfoWrapper inputParam : jobInfoWrapper.getInputParameters()) {
            writer.write("\n#    " + inputParam.getName() + " = ");

            String link = inputParam.getLink();
            if (link == null) {
                String displayValue = inputParam.getDisplayValue();
                //String value = inputParam.getValue();
                //String substitutedValue = GenePatternAnalysisTask.substitute(value, props, null);
                // bug 899 perform command line substitutions
                //if (substitutedValue != null && !(value.equals(substitutedValue))) {
                //    displayValue = substitutedValue + " (" + value + ")";
                //}
                writer.write(displayValue);
            
            }
            //special case for input files
            else {
                //case 1: an external URL
                if (link.equals(inputParam.getDisplayValue())) {
                    writer.write(link);
                }
                //case 2: an uploaded input file
                else if (link.startsWith(matchFileUploadPrefix)) {
                    link = link.substring(jobInfoWrapper.getServletContextPath().length(), link.length());
                    if (GP_URL.endsWith("/")) {
                        link = link.substring(1);
                    }
                    writer.write(inputParam.getDisplayValue() + "   " + GP_URL + link);
                }
                 //case 3: an input which is part of the pipeline created from an output of a previous job with an uploaded file
                //case 4: an output from a previous step in a pipeline
                else {
                    
                    writer.write(inputParam.getDisplayValue() + "   " + link);
                }
             }
            // GP-8093 
            // get the file (if local drive) or external file URL (if S3) to get the file size
            try {
                if (inputParam.getParameterInfo().getAttributes().get("type").equals("java.io.File")){
                    
                    String fileSize = getFileSize(inputParam, matchFileUploadPrefix);
                   
                    JobRunnerJob jrj=new JobRunnerJobDao().selectJobRunnerJob(HibernateUtil.instance(), jobInfoWrapper.getJobNumber());
                    long queued =  (jrj.getStartTime().getTime() - jrj.getSubmitTime().getTime());
                    
                    long elapsed =  (jrj.getEndTime().getTime() - jrj.getStartTime().getTime());
                    
                    // avoid building the message if its not gonna be recorded
                    // record TaskName jobNumber, elapsed, queued, filename, bytes, rows (optional), cols (optional)
                    if (elapsedTimeLog.isTraceEnabled() || elapsedTimeLog.isDebugEnabled()){
                        writer.write(" # file size " + fileSize);
                        StringBuffer logMsg = new StringBuffer(jobInfoWrapper.getTaskName());
                        logMsg.append("\t");
                        logMsg.append(jobInfoWrapper.getJobNumber() );
                        logMsg.append("\t");
                        logMsg.append(elapsed);
                        logMsg.append("\t");
                        logMsg.append(queued);
                        logMsg.append("\t");
                        logMsg.append(inputParam.getDisplayValue());
                        logMsg.append("\t");
                        logMsg.append(fileSize);
                        elapsedTimeLog.trace(logMsg.toString());
                    }
                }
            } catch (Exception e){
                
            }
        }
        writer.write("\n");
    }

    public static String getFileSize(ParameterInfoWrapper inputParam, String matchFileUploadPrefix) throws IOException{
        String link = inputParam.getLink();
        //case 1: an external URL
        if (link.equals(inputParam.getDisplayValue())) {
           // external file
            if (link.toLowerCase().endsWith(".gct")){
                return getGctFileSize( link);
            } else {
                try {
                    int size = getFileSize(new URL(link));
                    return "\t" + size ;
                } catch (IOException e){
                    // let it flow through to the no size found clause
                }
            }
        }
        //case 2: an uploaded input file
        else if (link.startsWith(matchFileUploadPrefix)) {
            // uploaded input
            try {
                GpConfig gpConfig = ServerConfigurationFactory.instance();
                GpFilePath inputFilePath = GpFileObjFactory.getRequestedGpFileObj(gpConfig, link);
                long byteLength = inputFilePath.getFileLength();
                if (inputFilePath.getExtension().equalsIgnoreCase("gct")){
                    // for a gct lets see if we can get the matrix size
                    ExternalFileManager extFileManager = DataManager.getExternalFileManager(GpContext.getServerContext());
                    if (extFileManager != null){
                        String extUrl = extFileManager.getDownloadURL(GpContext.getServerContext(), inputFilePath.getServerFile());
                        return getGctFileSize( extUrl);
                    } else {
                        File f = inputFilePath.getServerFile();
                        return getGctFileSize( f);
                    }
                }  else {
                    return "\t length = " + byteLength + " bytes";
                }
            } catch (Exception ee){
                // let it flow through
            }
        }
        //case 3: an input which is part of the pipeline created from an output of a previous job with an uploaded file
        //case 4: an output from a previous step in a pipeline
        else {
            // uploaded input
            try {
                GpConfig gpConfig = ServerConfigurationFactory.instance();
                GpFilePath inputFilePath = GpFileObjFactory.getRequestedGpFileObj(gpConfig, link);
                long byteLength = inputFilePath.getFileLength();
                if (inputFilePath.getName().endsWith("gct")){
                    // for a gct lets see if we can get the matrix size
                    ExternalFileManager extFileManager = DataManager.getExternalFileManager(GpContext.getServerContext());
                    if (extFileManager != null){
                        String extUrl = extFileManager.getDownloadURL(GpContext.getServerContext(), inputFilePath.getServerFile());
                        return getGctFileSize( extUrl);
                    } else {
                        File f = inputFilePath.getServerFile();
                        return getGctFileSize( f);
                    }
                } else {
                    if (byteLength <=0){
                        ExternalFileManager extFileManager = DataManager.getExternalFileManager(GpContext.getServerContext());
                        if (extFileManager != null){
                            String extUrl = extFileManager.getDownloadURL(GpContext.getServerContext(), inputFilePath.getServerFile());
                            return "\t"+ getFileSize(new URL( extUrl));
                        } else {
                            File f = inputFilePath.getServerFile();
                            return "\t"+f.length();
                        }
                        
                    }
                    return "\t"+ byteLength ;
                }
            } catch (Exception ee){
                // let it flow through
            }
        }
        
        return "\t# file size not available";
    }
    
    private static int getFileSize(URL url) {
        URLConnection conn = null;
        try {
            conn = url.openConnection();
            if(conn instanceof HttpURLConnection) {
                ((HttpURLConnection)conn).setRequestMethod("GET");
                ((HttpURLConnection)conn).setRequestProperty("Range","bytes=1");
            }
            conn.getInputStream();
            return conn.getContentLength();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if(conn instanceof HttpURLConnection) {
                ((HttpURLConnection)conn).disconnect();
            }
        }
    }
    
    
    public static String getGctFileSize(String link){
        BufferedReader in =null;
        try {
            URL anUrl = new URL(link);
            
            String nBytes = ""+getFileSize(anUrl);
            
            in = new BufferedReader(
                    new InputStreamReader(
                    anUrl.openStream()));
    
            String inputLine;
    
            int i=2; /* number lines */
            String sizeLine = null;
            while (i>0 && (inputLine = in.readLine()) != null) {
                sizeLine = inputLine;
                i--;
            }   
            in.close();
            // expect nBytes\tnrows\tncols
            return "\t " + nBytes + "\t" + sizeLine;
        } catch (Exception e){
            return "\t# file size not available";
        } finally {
            if (in != null){
                try {
                    in.close();
                } catch (IOException ioe){
                    log.error(ioe);
                }
            }   
        }
    }
    public static String getGctFileSize(File  gctFile){
        BufferedReader in =null;
        try {
           
            String nBytes = ""+ gctFile.length();
           
            in = new BufferedReader( new FileReader(gctFile));
    
            String inputLine;
    
            int i=2; /* number lines */
            String sizeLine = null;
            while (i>0 && (inputLine = in.readLine()) != null) {
                sizeLine = inputLine;
                i--;
            }   
            in.close();
            return "\t" + nBytes + "\t"+ sizeLine;
        } catch (Exception e){
            return "\t# file size not available";
        } finally {
            if (in != null){
                try {
                    in.close();
                } catch (IOException ioe){
                    log.error(ioe);
                }
            }   
        }
    }
    
    
    public static File writePipelineExecutionLog(File jobDir, JobInfoWrapper jobInfo) {
        File logFile = new File(jobDir, jobInfo.getTaskName() + "_execution_log.html");
        WritePipelineExecutionLog w = new WritePipelineExecutionLog(logFile, jobInfo);
        w.writeLogFile();
        return logFile;
    }

    public static void writeOutputFilesToZipStream(OutputStream os, JobInfoWrapper jobInfo, GpContext gpContext) throws IOException {
        if (DataManager.isUseS3NonLocalFiles(gpContext)) {
            ExternalFileManager externalFileManager = DataManager.getExternalFileManager(gpContext);
            // if an ExternalFileManager is in play, we need to make sure that all of the desired 
            // files are local because we cannot be certain (e.g. S3) that the files can be zipped 
            // wherever they actually are
            for(OutputFile outputFile : jobInfo.getOutputFiles()) {
                System.out.println("1. Fetch for download zip "+outputFile.getOutputFile()+"   " + outputFile.getOutputFile().exists()  );
                if (!outputFile.getOutputFile().exists())
                    externalFileManager.syncRemoteFileToLocal(gpContext, outputFile.getOutputFile());
                
            }
            for(JobInfoWrapper step : jobInfo.getAllSteps()) {
                for(OutputFile outputFile : step.getOutputFiles()) {
                    System.out.println("2. Fetch for download zip "+outputFile.getOutputFile()+"   " + outputFile.getOutputFile().exists()  );
                    
                    if (!outputFile.getOutputFile().exists())
                        externalFileManager.syncRemoteFileToLocal(gpContext, outputFile.getOutputFile());
                }
            }
        }
        
        
        ZipOutputStream zipStream = new ZipOutputStream(os);
        JobInfoZipFileWriter w = new JobInfoZipFileWriter(jobInfo);
        w.writeOutputFilesToZip(zipStream);
    }

    /** @deprecated */
    public static String getLaunchUrl(final int jobNumber) throws IOException {
        GpConfig gpConfig = ServerConfigurationFactory.instance();
        GpContext context = GpContext.getServerContext();
        return getLaunchUrl(gpConfig, context, jobNumber);
    }
    
    public static String getLaunchUrl(final GpConfig gpConfig, final GpContext jobContext, final int jobNumber) throws IOException {
        final File jobDir=new File(GenePatternAnalysisTask.getJobDir(gpConfig, jobContext, ""+jobNumber));
        return getLaunchUrlFromJobDir(jobDir);        
    } 
    
    public static String getLaunchUrlFromJobDir(final File jobDir) throws IOException {
        final File launchUrlFile = new File(jobDir, JavascriptHandler.LAUNCH_URL_FILE);
     
        /**
         * Its totally unclear if we need the launchUrl.txt file for anything.  It was added in 2012 but nothing
         * in the commit logs say why.  I think its unused but cannot be sure so to be safe we will
         * grab it from the externalFileManager just to be safe  JTL 07/03/21 
         */
        
        if (!launchUrlFile.exists()){
            GpContext serverContext = GpContext.getServerContext();
            final ExternalFileManager externalFileManager = DataManager.getExternalFileManager(serverContext);
            if (externalFileManager != null){
                externalFileManager.syncRemoteFileToLocal(serverContext, launchUrlFile);
            }
        }
        
        
        final String launchUrl = FileUtils.readFileToString(launchUrlFile, "UTF-8").trim();
        return launchUrl;
    }
    
}

