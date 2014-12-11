package org.genepattern.server;

import static org.genepattern.util.GPConstants.TASKLOG;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;
import org.genepattern.server.JobInfoWrapper.ParameterInfoWrapper;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.serverfile.ServerFilePath;
import org.genepattern.server.dm.tasklib.TasklibPath;
import org.genepattern.server.dm.webupload.WebUploadPath;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.executor.pipeline.PipelineHandler;
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
        TaskInfo taskInfo = null;
        try {
            //calls HibernateUtil.beginTransaction...
            AdminDAO ds = new AdminDAO();
            taskInfo = ds.getTask(taskId);
            return taskInfo;
        } finally {
            //...must close the session here, or in an enclosing method
            if (closeDbSession) {
                HibernateUtil.closeCurrentSession();
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
        AnalysisDAO analysisDao = new AnalysisDAO();
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

        AnalysisDAO analysisDao = new AnalysisDAO();
        UserDAO userDao = new UserDAO();
        boolean showExecutionLogs = userDao.getPropertyShowExecutionLogs(currentUser);
        String visualizerJavaFlags = getVisualizerJavaFlags(userDao, currentUser);

        AdminDAO adminDao = new AdminDAO();

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
            Status jobStatus = new JobStatusLoaderFromDb(gpUrl).loadJobStatus(jobContext);
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
                    + "\" archive=\"runVisualizer.jar,commons-httpclient.jar,commons-codec-1.3.jar\" codebase=\"/gp/downloads\" width=\"1\" height=\"1\" alt=\"Your browser can not run applets\">");

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

    public static void writeExecutionLog(Writer writer, JobInfoWrapper jobInfoWrapper)
            throws IOException {
        writer.write("# Created: " + new Date() + " by " + jobInfoWrapper.getUserId());
        writer.write("\n# Job: " + jobInfoWrapper.getJobNumber());

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
        }
        writer.write("\n");
    }

    public static File writePipelineExecutionLog(File jobDir, JobInfoWrapper jobInfo) {
        File logFile = new File(jobDir, jobInfo.getTaskName() + "_execution_log.html");
        WritePipelineExecutionLog w = new WritePipelineExecutionLog(logFile, jobInfo);
        w.writeLogFile();
        return logFile;
    }

    public static void writeOutputFilesToZipStream(OutputStream os, JobInfoWrapper jobInfo) throws IOException {
        ZipOutputStream zipStream = new ZipOutputStream(os);
        JobInfoZipFileWriter w = new JobInfoZipFileWriter(jobInfo);
        w.writeOutputFilesToZip(zipStream);
    }

    public static String generateLaunchURL(TaskInfo taskInfo, JobInfo jobInfo) throws Exception {
        String launchUrl = null;
        TaskInfoAttributes tia = taskInfo.getTaskInfoAttributes();
        if(tia.get(GPConstants.CATEGORIES).contains(GPConstants.TASK_CATEGORY_JSVIEWER)) {
            String mainFile = (String)taskInfo.getAttributes().get("commandLine");
            mainFile = mainFile.substring(0, mainFile.indexOf("?")).trim();
            TasklibPath tasklibPath = new TasklibPath(taskInfo, mainFile);
            launchUrl = ServerConfigurationFactory.instance().getGenePatternURL() + tasklibPath.getRelativeUri().toString();
            ParameterInfo[] parameterInfos = jobInfo.getParameterInfoArray();
            for (ParameterInfo parameterInfo : parameterInfos)
            {
                try {
                    String value=parameterInfo.getValue();

                    if (parameterInfo.getValue().endsWith(".list.txt")) {
                        List<String> fileList = PipelineHandler.parseFileList(GpFileObjFactory.getRequestedGpFileObj(parameterInfo.getValue()).getServerFile());

                        for (String file : fileList) {
                            GpFilePath gpPath = new ServerFilePath(new File(file));
                            value = gpPath.getUrl().toExternalForm();
                        }
                    }

                    launchUrl += "&" + parameterInfo.getName() + "=" + value;

                } catch (Exception io) {
                    log.error(io);
                }
            }
        }
        return launchUrl;
    }
}
