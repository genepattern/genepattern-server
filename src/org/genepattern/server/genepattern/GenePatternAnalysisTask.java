/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/

package org.genepattern.server.genepattern;

import static org.genepattern.util.GPConstants.ACCESS_PRIVATE;
import static org.genepattern.util.GPConstants.ANY;
import static org.genepattern.util.GPConstants.COMMAND_LINE;
import static org.genepattern.util.GPConstants.CPU_TYPE;
import static org.genepattern.util.GPConstants.DESCRIPTION;
import static org.genepattern.util.GPConstants.INPUT_BASENAME;
import static org.genepattern.util.GPConstants.INPUT_EXTENSION;
import static org.genepattern.util.GPConstants.INPUT_FILE;
import static org.genepattern.util.GPConstants.INPUT_PATH;
import static org.genepattern.util.GPConstants.JOB_ID;
import static org.genepattern.util.GPConstants.LEFT_DELIMITER;
import static org.genepattern.util.GPConstants.LIBDIR;
import static org.genepattern.util.GPConstants.LSID;
import static org.genepattern.util.GPConstants.MANIFEST_FILENAME;
import static org.genepattern.util.GPConstants.MAX_PARAMETERS;
import static org.genepattern.util.GPConstants.NAME;
import static org.genepattern.util.GPConstants.OS;
import static org.genepattern.util.GPConstants.PARAM_INFO_ATTRIBUTES;
import static org.genepattern.util.GPConstants.PARAM_INFO_CHOICE_DELIMITER;
import static org.genepattern.util.GPConstants.PARAM_INFO_DEFAULT_VALUE;
import static org.genepattern.util.GPConstants.PARAM_INFO_NAME_OFFSET;
import static org.genepattern.util.GPConstants.PARAM_INFO_OPTIONAL;
import static org.genepattern.util.GPConstants.PARAM_INFO_PREFIX;
import static org.genepattern.util.GPConstants.PARAM_INFO_TYPE;
import static org.genepattern.util.GPConstants.PARAM_INFO_TYPE_INPUT_FILE;
import static org.genepattern.util.GPConstants.PARAM_INFO_TYPE_SEPARATOR;
import static org.genepattern.util.GPConstants.PIPELINE_ARG_STOP_AFTER_TASK_NUM;
import static org.genepattern.util.GPConstants.PRIVACY;
import static org.genepattern.util.GPConstants.PRIVATE;
import static org.genepattern.util.GPConstants.PUBLIC;
import static org.genepattern.util.GPConstants.RESERVED_PARAMETER_NAMES;
import static org.genepattern.util.GPConstants.RIGHT_DELIMITER;
import static org.genepattern.util.GPConstants.STDERR;
import static org.genepattern.util.GPConstants.STDERR_REDIRECT;
import static org.genepattern.util.GPConstants.STDIN_REDIRECT;
import static org.genepattern.util.GPConstants.STDOUT;
import static org.genepattern.util.GPConstants.STDOUT_REDIRECT;
import static org.genepattern.util.GPConstants.TASK_ID;
import static org.genepattern.util.GPConstants.TASK_NAMESPACE;
import static org.genepattern.util.GPConstants.TASK_TYPE_VISUALIZER;
import static org.genepattern.util.GPConstants.UNREQUIRED_PARAMETER_NAMES;
import static org.genepattern.util.GPConstants.USERID;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.Expand;
import org.codehaus.jackson.impl.JsonParserBase;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.genepattern.codegenerator.AbstractPipelineCodeGenerator;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.drm.JobRunner;
import org.genepattern.server.DataManager;
import org.genepattern.server.DbException;
import org.genepattern.server.InputFilePermissionsHelper;
import org.genepattern.server.JobInfoManager;
import org.genepattern.server.JobInfoWrapper;
import org.genepattern.server.JobInfoWrapper.InputFile;
import org.genepattern.server.JobManager;
import org.genepattern.server.PermissionsHelper;
import org.genepattern.server.TaskIDNotFoundException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.config.Value;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.ExternalFileManager;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.GpFilePathException;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.server.domain.AnalysisJob;
import org.genepattern.server.domain.AnalysisJobDAO;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.domain.JobStatusDAO;
import org.genepattern.server.eula.EulaManager;
import org.genepattern.server.executor.AnalysisJobScheduler;
import org.genepattern.server.executor.CommandExecutor;
import org.genepattern.server.executor.CommandExecutor2;
import org.genepattern.server.executor.CommandExecutor2Wrapper;
import org.genepattern.server.executor.CommandManagerFactory;
import org.genepattern.server.executor.JobDispatchException;
import org.genepattern.server.executor.JobSubmissionException;
import org.genepattern.server.executor.drm.dao.JobRunnerJob;
import org.genepattern.server.executor.drm.dao.JobRunnerJobDao;
import org.genepattern.server.executor.events.GpJobRecordedEvent;
import org.genepattern.server.executor.events.JobEventBus;
import org.genepattern.server.executor.pipeline.PipelineException;
import org.genepattern.server.executor.pipeline.PipelineHandler;
import org.genepattern.server.job.input.NumValues;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamId;
import org.genepattern.server.job.input.ParamListHelper;
import org.genepattern.server.job.input.ParamListValue;
import org.genepattern.server.job.input.ParamValue;
import org.genepattern.server.job.input.cache.FileCache;
import org.genepattern.server.job.input.choice.Choice;
import org.genepattern.server.job.input.choice.ChoiceInfo;
import org.genepattern.server.job.output.JobOutputRecorder;
import org.genepattern.server.job.status.Status;
import org.genepattern.server.plugin.PluginManagerLegacy;
import org.genepattern.server.quota.DiskInfo;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.server.taskinstall.InstallInfo;
import org.genepattern.server.taskinstall.InstallInfo.Type;
import org.genepattern.server.user.UsageLog;
import org.genepattern.server.util.Expander;
import org.genepattern.server.util.FTPDownloader;
import org.genepattern.server.util.JobResultsFilenameFilter;
import org.genepattern.server.util.MailSender;
import org.genepattern.server.util.ProcReadStream;
import org.genepattern.server.util.PropertiesManager_3_2;
import org.genepattern.server.util.UrlPrefixFilter;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.util.LsidVersion;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterFormatConverter;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.TaskInfoCache;
import org.genepattern.webservice.WebServiceException;

import com.google.common.base.Strings;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

/**
 * Enables definition, execution, and sharing of AnalysisTasks using extensive metadata descriptions and obviating
 * programming effort by the task creator or user. Like other Omnigene AnalysisTasks, this one has an onJob(JobInfo
 * jobInfo) method which executes an analysis task to completion (or error) and returns results. Unlike all of the
 * others, GenePatternAnalysisTask is not a wrapper for a specific application. It is a wrapper to a user-defined task,
 * whose command line is defined in the metadata captured in a TaskInfoAttributes. The rich metadata known about a task
 * is almost entirely stored in well-known entries in the task's TaskInfoAttributes HashMap.
 * <p/>
 * <p/>
 * A typical GenePattern command line will be something like this: <br>
 * <blockquote>perl foo.pl &lt;input_filename&gt; &lt;num_iter&gt; &lt;max_attempts&gt; </blockquote> <br>
 * in which there are three substitutions to be made at invocation time. These substitutions replace the &lt;bracketed
 * variable names&gt; with the values supplied by the caller. Some parameters have a prefix included, meaning that when
 * they are substituted, they are prefixed by some fixed text as well (eg. <code>-F<i>filename</i></code>). By default
 * parameters are mandatory, however, the user, in defining the task parameters, may indicate that some are optional,
 * meaning that they may be replaced with empty strings at command line substitution time rather than being rejected for
 * execution.
 * <p/>
 * <p/>
 * There are <i>many </i> other supporting methods included in this class. Among them:
 * <ul>
 * <li><b>Task definition </b></li>
 * <ul>
 * <li>A host of attributes for documenting tasks allows for categorization when search for them to build a pipeline,
 * for sharing them with others, for [future] automated selection of most appropriate execution platform, etc.</li>
 * <li>Validation at task definition time and task execution time of correct and complete parameter definitions.</li>
 * <li>Storage of a task's associated files (scripts, DLLs, executables, property files, etc) in isolation from other
 * tasks</li>
 * <li>Ability to add and delete tasks without writing a new wrapper extending AnalysisTask or a DBLoader. Built-in
 * substitution variables allow the user to create platform-independent command lines that will work on both Windows and
 * Unix.</li>
 * <li>Public and private task types, of which only a user's own private tasks will appear in the task catalog they
 * request</li>
 * </ul>
 * <p/>
 * <li><b>Task execution </b></li>
 * <ul>
 * <li>Conversion of URLs (http://, ftp://) to local files and substition with local filenames for task inputs.</li>
 * <li>Execution of each task in its own "sandbox" directory</li>
 * <li>Ability to stop a running task</li>
 * <li>Support for pipelining of tasks as a form of composite pseudo-task</li>
 * </ul>
 * <p/>
 * <li><b>Task sharing/publication </b></li>
 * <ul>
 * <li>Ability to export all information about a task in the form of a zip file</li>
 * <li>Ability to import a zip file containing a task definition, allowing browsing and installation</li>
 * <li>Integration with stored tasks archived on SourceForge.net (browse, download, install)</li>
 * </ul>
 * <p/>
 * <li><b>Browser support </b></li>
 * <ul>
 * <li>Access to all of the above features (task definition, execution, sharing) can be accomplished using a web browser
 * </li>
 * </ul>
 * </ul>
 * 
 * @author Jim Lerner
 * @version 1.0
 * @see org.genepattern.webservice.TaskInfoAttributes
 */

public class GenePatternAnalysisTask {
    private static final Logger log = Logger.getLogger(GenePatternAnalysisTask.class);

    private static final String ORIGINAL_PATH = "originalPath";
    private static final String DATA_SERVLET_TOKEN = "/gp/data/";

    public enum JOB_TYPE {
        JOB,
        VISUALIZER,
        JAVASCRIPT,
        PIPELINE,
        /** @depecated */
        PASS_BY_REFERENCE
    };

    public enum INPUT_FILE_MODE {
        COPY, MOVE, PATH
    };

    /**
     * Use an executor to dispatch jobs so that job dispatch can be cancelled after a timeout period.
     */
    private ExecutorService executor = null;

    /**
     * 
     * @param executor, an ExecutorService instance for dispatching jobs.
     */
    public GenePatternAnalysisTask(ExecutorService executor) {
        this.executor = executor;
    }

    /**
     * Returns a local URL as a File object or <tt>null</tt> if the URL can not be represented as a File
     * 
     * @param url, The URL to convert to a File.
     * @param userId, The user id of the user running the job.
     * @param jobNumber, The job number of the running job.
     * @throws IllegalArgumentException, If the URL refers to a file that the specified userId does not have permission to access.
     * @return The file or <tt>null</tt>
     */
    private File localInputUrlToFile(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext jobContext, final URL url) {
        final JobInfo jobInfo = jobContext.getJobInfo();
        //new way of converting server url to file path
        GpFilePath inputFilePath = null;
        try {
            inputFilePath = GpFileObjFactory.getRequestedGpFileObj(gpConfig, url);
        }
        catch (Throwable t) {
            //ignore exception, because we have not fully implemented GpFileObjFactory methods
            //TODO: eventually we should not ignore this exception
        }
        if (inputFilePath != null) {
            boolean canRead = inputFilePath.canRead(jobContext.isAdmin(), jobContext);
            if (!canRead) {
                String errorMessage = "You are not permitted to access the requested file: "+url;
                log.error(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
            return inputFilePath.getServerFile().getAbsoluteFile();
        }
        
        //TODO: eventually, we shouldn't need any code beyond this point
        
        String path = url.getPath();
        try {
            path = URLDecoder.decode(path, "UTF-8");
        } 
        catch (UnsupportedEncodingException e) {
            log.error("Error", e);
        }

        String userId = null;
        if (jobInfo != null) {
            userId = jobInfo.getUserId();
        }

        if (path.endsWith("getFile.jsp")) {
            // request parameters are: task=lsid & job=<job_number> & file=filename
            String params = url.getQuery();
            int idx1 = params.indexOf("task=");
            int endIdx1 = params.indexOf('&', idx1);
            if (endIdx1 == -1) {
                endIdx1 = params.length();
            }
            int idx2 = params.indexOf("file=");
            int endIdx2 = params.indexOf('&', idx2);
            if (endIdx2 == -1) {
                endIdx2 = params.length();
            }
            String lsid = params.substring(idx1 + 5, endIdx1);
            try {
                lsid = URLDecoder.decode(lsid, "UTF-8");
            } 
            catch (UnsupportedEncodingException e) {
                log.error("Error", e);
            }
            String filename = params.substring(idx2 + 5, endIdx2);
            if (filename == null) {
                return null;
            }
            try {
                filename = URLDecoder.decode(filename, "UTF-8");
            } 
            catch (UnsupportedEncodingException e) {
                log.error("Error", e);
            }
            int jobNumber = -1;
            int idx3 = params.indexOf("job=");
            if (idx3 >= 0) {
                int endIdx3 = params.indexOf('&', idx3);
                if (endIdx3 == -1) {
                    endIdx3 = params.length();
                }
                String jobNumberParam = params.substring(idx3 + 4, endIdx3);
                if (jobNumberParam != null) {
                    try {
                        jobNumber = Integer.parseInt(jobNumberParam);
                    }
                    catch (NumberFormatException e) {
                        log.error("Invalid request parameter, job="+jobNumberParam, e);
                    }
                }
            }
            if (lsid == null || lsid.trim().equals("")) { 
                // input file look in temp for pipelines run without saving
                File parentTempdir = ServerConfigurationFactory.instance().getTempDir(jobContext);
                File in = new File(parentTempdir, filename);
                if (in.exists() && jobNumber >= 0) {
                    // check whether the current user has access to the job
                    if (jobContext.canReadJob()) {
                        return in;
                    }
                    throw new IllegalArgumentException("You are not permitted to access the requested file: "+in.getName());
                }
                else if (in.exists()) {
                    InputFilePermissionsHelper perm = new InputFilePermissionsHelper(userId, filename);
                    if (perm.isCanRead()) return in;
                }                      

                //special case: look for file among the user uploaded files
                try {
                    File userUploadDir = gpConfig.getUserUploadDir(jobContext);
                    in = new File(userUploadDir, filename);
                    boolean foundUserUpload = in.canRead();
                    if (foundUserUpload) {
                        return in;
                    }
                }
                catch (Throwable t) {
                    log.error("Unexpected error getting useruploadDir", t);
                }

                //special case: Axis
                in = new File(ServerConfigurationFactory.instance().getSoapAttDir(jobContext), filename);
                if (in.exists()) {
                    //TODO: permissions check for SOAP upload, see *similar* code in getFile.jsp
                    return in;
                }
                return null;
            }
            // check that user can access requested module
            TaskInfo taskInfo = null;
            try {
                LocalAdminClient adminClient = new LocalAdminClient(userId);
                taskInfo = adminClient.getTask(lsid);
            }
            catch (WebServiceException e) {
                String errorMessage = "Unable to find file: "+filename+ " ("+url+"). Because of a database connection error: "+e.getLocalizedMessage();
                log.error(errorMessage, e);
                throw new IllegalArgumentException(errorMessage,e);
            } 
            if (taskInfo == null) {
                String errorMessage = "You are not permitted to access the requested file: "+filename+ " ("+url+")";
                log.error(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
            File file = null;
            String taskLibDir = DirectoryManager.getTaskLibDir(taskInfo);
            if (taskLibDir == null) {
                String errorMessage = "You are not permitted to access the requested file: "+filename+ " ("+url+")";
                log.error(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
            file = new File(taskLibDir, filename);
            if (file.exists()) {
                return file;
            }
            else {
                //new code circa 3.6.0 release (just before 3.6.1)
                String errorMessage = "Requested file does not exist: "+filename+ " ("+url+")";
                log.error(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
        }
        
        // If the URL passed in uses the data servlet
        if (path.contains(DATA_SERVLET_TOKEN)) {
            String filePath = path.substring(path.indexOf(DATA_SERVLET_TOKEN) + DATA_SERVLET_TOKEN.length());
            File file = new File(filePath);
            if (file.exists()) {
                return file;
            }
            else {
                return null;
            }
        }
        
        // Assumes that the file is passed in through a /jobResults/ URL and will throw an error otherwise
        LocalUrlParser parser = new LocalUrlParser(url);
        try {
            parser.parse();
        }
        catch (Throwable t) {
            throw new IllegalArgumentException(t);
        }
        if (jobContext.canReadJob()) {
            File localFile = null;
            try {
                GpContext context = GpContext.getContextForJob(jobInfo);
                File rootJobDir = ServerConfigurationFactory.instance().getRootJobDir(context);
                File jobDir = new File(rootJobDir, ""+parser.getJobNumber());
                localFile = new File(jobDir, parser.getRelativeFilePath());
            }
            catch (Exception e) {
                log.error(e);
            }
            if (localFile != null && localFile.exists()) {
                return localFile;
            }
        }
        return null;
    }
    
    /**
     * Helper class for localInputUrlToFile, split the given, presumably local, url, 
     * into a jobNumber and a relativeFilePath, relative to the jobs directory.
     * 
     * Example inputs, 
     *     http://127.0.0.1:8080/gp/jobResults/42/e_coli_1000_2.fa
     *     http://127.0.0.1:8080/gp/jobResults/48/e_coli/e_coli_1000_2.fa
     * Expected outputs,
     *     e_coli_2.fa
     *     48/e_coli/e_coli_1000_2.fa
     *     
     * @param urlPath
     * @return
     */
    private static class LocalUrlParser {
        //the url, presumably to a file in the job results directory for the server
        URL url;
        int jobNumber;
        String relativeFilePath;
        
        public LocalUrlParser(URL url) {
            this.url = url;
        }
        
        /**
         * 
         * @throws Exception if url path doesn't contain jobResults, or if url path doesn't contain a valid (integer) job number.
         */
        public void parse() throws Exception {
            String urlPath = url.getPath();
            int idx = urlPath.indexOf("jobResults/");
            if (idx < 0) {
                throw new Exception("Expecting 'jobResults/' on path: "+url.getPath());
            }
            
            urlPath = urlPath.substring( idx + "jobResults/".length());
            idx = urlPath.indexOf("/");
            if (idx < 0) {
                throw new Exception("Expecting 'jobResults/<job>/' on path: "+url.getPath());
            }
            String jobId = urlPath.substring(0, idx);
            try {
                jobNumber = Integer.parseInt(jobId);
            }
            catch (NumberFormatException nfe) {
                throw new Exception("Expecting <job> to be an integer in 'jobResults/<job>/<filepath>': "+url.getPath());
            }
            //drop the leading '/'
            relativeFilePath = urlPath.substring(idx+1);
        }
        
        public int getJobNumber() {
            return jobNumber;
        }
        
        public String getRelativeFilePath() {
            return relativeFilePath;
        }
    }

    private boolean canReadJob(final HibernateSessionManager mgr, boolean isAdmin, String userId, int jobNumber) {
        try {
            PermissionsHelper perm = new PermissionsHelper(mgr, isAdmin, userId, jobNumber);
            return perm.canReadJob();
        }
        catch (Throwable t) {
            log.error("Error checking permissions for userId="+userId+" and jobId="+jobNumber, t);
            return false;
        }
        finally {
            mgr.closeCurrentSession();
        }
    }
    
    public static JOB_TYPE initJobType(final TaskInfo taskInfo) {
        JOB_TYPE jobType = JOB_TYPE.JOB;
        if (TaskInfo.isVisualizer(taskInfo.getTaskInfoAttributes())) {
            jobType = JOB_TYPE.VISUALIZER;
        }
        if (TaskInfo.isJavascript(taskInfo.getTaskInfoAttributes())) {
            jobType = JOB_TYPE.JAVASCRIPT;
        }
        else if (taskInfo.isPipeline()) {
            jobType = JOB_TYPE.PIPELINE;
        }
        else {
            //special-case: hard-coded 'pass-by-reference' input files for IGV and GENE-E
            if ("IGV".equals(taskInfo.getName()) || "GENE_E".equals(taskInfo.getName()) || "GENEE".equals(taskInfo.getName())) {
                jobType = JOB_TYPE.PASS_BY_REFERENCE;
            }
        }
        return jobType;
    }
    
    
    private void debugProcessStdOutAndErr(Process proc, String name) {
        try {
              
            ProcReadStream s1 = new ProcReadStream(name +" stdin", proc.getInputStream ());
            ProcReadStream s2 = new ProcReadStream(name + " stderr", proc.getErrorStream ());
            s1.start ();
            s2.start ();
            
        } catch (Exception e){
            log.error(e);
        }
    }
    

    /**
     * Called by Omnigene Analysis engine to run a single analysis job, wait for completion, then report the results to
     * the analysis_job database table. Running a job involves looking up the TaskInfo and TaskInfoAttributes for the
     * job, validating and formatting a command line based on the formal and actual arguments to the task, downloading
     * any input URLs to the local filesystem, executing the application, and then returning any of the output files
     * from the sandbox directory where it ran to the analysis_job database (and ultimately to the caller).
     * 
     * TODO: improve error handling for this method, exceptions should include:
     *     1) null arg
     *     2) server error (hibernate, db, et cetera)
     *     3) job already PROCESSING or TERMINATED or ERROR
     * 
     * @param jobId, the job_no from the ANALYSIS_JOB table
     */
    public void onJob(Integer jobId) throws JobDispatchException {
        log.debug("onJob("+jobId+")");
        if (jobId == null) {
            throw new JobDispatchException("Invalid arg to onJob, jobId="+jobId);
        }
        final HibernateSessionManager mgr=org.genepattern.server.database.HibernateUtil.instance();
        final GpConfig gpConfig = ServerConfigurationFactory.instance();
        final GpContext jobContext;
        try {
            jobContext=GpContext.createContextForJob(mgr, jobId);
        }
        catch (Throwable t) {
            log.error("Error initializing jobContext for jobId="+jobId, t);
            throw new JobDispatchException("Error initializing jobContext for jobId="+jobId);
        }
        finally {
            mgr.closeCurrentSession();
        }
        final String baseGpHref=jobContext.getBaseGpHref(); 
        final JobInfo jobInfo=jobContext.getJobInfo();
        final int parentJobId=jobInfo._getParentJobNumber();
        final TaskInfo taskInfo=jobContext.getTaskInfo();
        final String taskName=taskInfo.getName();
 
        //  handle special-case: job was terminated before it was started
        if (JobStatus.ERROR.equals(jobInfo.getStatus()) || JobStatus.FINISHED.equals(jobInfo.getStatus())) {
            log.info("job #"+jobId+" already finished, status="+jobInfo.getStatus());
            return;
        }

        checkDiskQuota(mgr, gpConfig, jobContext, taskInfo.getName());

        File rootJobDir = null;
        try {
            rootJobDir = gpConfig.getRootJobDir(jobContext);
        }
        catch (Exception e) {
            throw new JobDispatchException("Error getting root job directory for jobId="+jobId, e);
        }
        final File outDir;
        try {
            //even though the job directory gets created when the job is added to the queue,
            //create it again, if necessary, to deal with resubmitted jobs
            //TODO: improve this by requiring the job dir to be cleared (but not created) before running the job
            final File path = JobManager.createJobDirectory(jobInfo);
            //require absolute path
            outDir = path.getAbsoluteFile();
        }
        catch (JobSubmissionException e) {
            throw new JobDispatchException("Error getting job directory for jobId="+jobId, e);
        }
        
        //does the task have an EULA
        boolean requiresEULA=false;
        try {
            requiresEULA = EulaManager.instance(jobContext).requiresEula(jobContext);
        }
        catch (Throwable t) {
            String message="Unexpected error checking for EULA for job #"+jobInfo.getJobNumber()+", task="+jobInfo.getTaskName()+": "+t.getLocalizedMessage();
            log.error(message, t);
            throw new JobDispatchException(message);
        }
        if (requiresEULA) {
            throw new JobDispatchException(taskInfo.getName()+" requires an End-user license agreement. "+
                    "There is no record of agreement for userId="+jobInfo.getUserId());
        }
       
        final JOB_TYPE jobType = initJobType(taskInfo);

        int formalParamsLength = 0;
        ParameterInfo[] formalParams = taskInfo.getParameterInfoArray();
        if (formalParams != null) {
            formalParamsLength = formalParams.length;
        }

        TaskInfoAttributes taskInfoAttributes = taskInfo.giveTaskInfoAttributes();
        if (taskInfoAttributes == null || taskInfoAttributes.size() == 0) {
            throw new JobDispatchException(taskName + ": missing all TaskInfoAttributes!");
        }

        // check OS and CPU restrictions of TaskInfoAttributes against this server
        // eg. "x86", "ppc", "alpha", "sparc"
        validateCPU(gpConfig, taskInfoAttributes.get(CPU_TYPE));
        String expected = taskInfoAttributes.get(OS);
        // eg. "Windows", "linux", "Mac OS X", "OSF1", "Solaris"
        validateOS(gpConfig, expected, "run " + taskName);
        try {
            PluginManagerLegacy pluginManager=new PluginManagerLegacy(mgr, gpConfig, jobContext);
            pluginManager.validatePatches(taskInfo, null);
        }
        catch (Exception e) {
            throw new JobDispatchException("Error validating patches for task="+taskInfo.getName(), e);
        }
        Map<String, String> environmentVariables = new HashMap<String, String>();

        // handle special-case: this job is part of a pipeline, update input file parameters which use the output of previous steps
        if (parentJobId >= 0) {
            try {
                PipelineHandler.prepareNextStep(mgr, gpConfig, jobContext);
            }
            catch (PipelineException e) {
                throw new JobDispatchException(e);
            }
        }

        ParameterInfo[] paramsCopy = copyParameterInfoArray(jobInfo);
        final Properties propsPre; // <---- initialize properties, needed to compute the originalPath
        try {
            propsPre = setupProps(taskInfo, taskName, parentJobId, jobId, jobInfo.getTaskID(), 
                    taskInfoAttributes, paramsCopy, environmentVariables, taskInfo.getParameterInfoArray(), jobInfo.getUserId());
            if (!Strings.isNullOrEmpty(baseGpHref)) {
                propsPre.setProperty("GenePatternURL", baseGpHref+"/");
            }
        }
        catch (MalformedURLException e) {
            throw new JobDispatchException(e);
        }
        Vector<String> vProblems = new Vector<String>();
        final Map<String,ParameterInfoRecord> paramInfoMap=ParameterInfoRecord.initParamInfoMap(taskInfo);
        if (paramsCopy != null) {
            for (int j = 0; j < paramsCopy.length; j++) {
                final ParameterInfo pinfo=paramsCopy[j];
                if (log.isDebugEnabled()) {
                    log.debug(pinfo.getName()+": "+pinfo.getValue());
                }
                HashMap attrsCopy = pinfo.getAttributes();
                if (attrsCopy == null) {
                    attrsCopy = new HashMap();
                }
                String fileType = (String) attrsCopy.get(ParameterInfo.TYPE);
                String mode = (String) attrsCopy.get(ParameterInfo.MODE);
                String originalPath = pinfo.getValue();
                // allow parameter value substitutions within file input parameters
                originalPath = substitute(originalPath, propsPre, paramsCopy);
                boolean isOptional = "on".equals(attrsCopy.get("optional"));
                // if necessary use the URL value instead of the server file path value
                final boolean isUrlMode=pinfo._isUrlMode();
                final ParameterInfoRecord pinfoRecord=paramInfoMap.get(pinfo.getName());                
                final boolean isDirectoryInputParam=pinfoRecord.getFormal()._isDirectory();
                final ChoiceInfo choiceInfo=initChoiceInfo(jobContext, pinfoRecord, pinfo);
                final Choice selectedChoice= choiceInfo == null ? null : choiceInfo.getValue(pinfo.getValue());
                final boolean isFileChoiceSelection=
                        pinfoRecord.getFormal().isInputFile()  &&
                        selectedChoice != null && 
                        selectedChoice.getValue() != null && 
                        selectedChoice.getValue().length() > 0;
                        
                final boolean isS3FileChoice = pinfoRecord.getFormal().isInputFile() &&
                        selectedChoice != null && 
                        selectedChoice.getValue() != null && 
                        selectedChoice.getValue().toLowerCase().startsWith("s3://");
                
                final boolean isCachedValue=UrlPrefixFilter.isCachedValue(gpConfig, jobContext, jobType, pinfoRecord.getFormal(), pinfo.getValue());
                if (isDirectoryInputParam) {
                    setPinfoValueForDirectoryInputParam(mgr, gpConfig, jobContext, pinfo, pinfoRecord); 
                }
                //special-case for File Choice parameters, cached values but not cached if from S3
                else if (isFileChoiceSelection && !isS3FileChoice) {
                    //If necessary, wait for the remote file to transfer to local cache before starting the job.
                    final GpFilePath cachedFile = FileCache.downloadCachedFile(mgr, gpConfig, jobContext, selectedChoice.getValue(), selectedChoice.isRemoteDir());
                    final String serverPath=cachedFile.getServerFile().getAbsolutePath();
                    if (log.isDebugEnabled()) {
                        log.debug("setting cached value for file drop-down param: "+pinfo.getName()+"="+pinfo.getValue()+", localPath="+serverPath);
                    }
                    pinfo.setValue(serverPath);
                }
                else if (isCachedValue) {
                    //If necessary, wait for the remote file to transfer to local cache before starting the job.
                    final GpFilePath cachedFile = FileCache.downloadCachedFile(mgr, gpConfig, jobContext, pinfo.getValue());
                    final String serverPath=cachedFile.getServerFile().getAbsolutePath();
                    if (log.isDebugEnabled()) {
                        log.debug("setting cached value for file param: "+pinfo.getName()+"="+pinfo.getValue()+", localPath="+serverPath);
                    }
                    pinfo.setValue(serverPath);
                }
                else if (fileType != null && fileType.equals(ParameterInfo.FILE_TYPE) && mode != null && !mode.equals(ParameterInfo.OUTPUT_MODE)) {
                    if (originalPath == null) {
                        if (isOptional) {
                            continue;
                        }
                        throw new JobDispatchException("Non-optional parameter " + pinfo.getName() + " has not been assigned a filename.");
                    }
                    if (mode.equals("CACHED_IN")) {
                        //param is existing job output file
                        StringTokenizer strtok = new StringTokenizer(originalPath, "/");
                        String job = null;
                        int jobNumber = -1;
                        if (strtok.hasMoreTokens()) {
                            job = strtok.nextToken();
                        }
                        String requestedFilename = null;
                        if (strtok.hasMoreTokens()) {
                            requestedFilename = strtok.nextToken();
                        }
                        if (job == null || requestedFilename == null) {
                            vProblems.add("You are not permitted to access the requested file: Unknown filename or job number.");
                            continue;
                        }
                        try {
                            jobNumber = Integer.parseInt(job);
                        }
                        catch (NumberFormatException e) {
                            vProblems.add("You are not permitted to access the requested file: Invalid job number, job='"+job+"'");
                            continue;
                        }
                        if (canReadJob(mgr, jobContext.isAdmin(), jobInfo.getUserId(), jobNumber)) {
                            originalPath = rootJobDir.getPath() + "/" + originalPath;
                        }
                        else {
                            vProblems.add("You are not permitted to access the requested file: "+requestedFilename);
                            continue;                        
                        }
                    } 
                    else if (mode.equals(ParameterInfo.INPUT_MODE)) {
                        log.debug("IN " + pinfo.getName() + "=" + originalPath);
                        //web form upload: <java.io.tmpdir>/<user_id>_run[0-9]+.tmp/<filename>
                        //SOAP client upload: <soap.attachment.dir>/<user_id>/<filename>
                        //inherited input file from parent pipeline job: <jobResults>/<different_job_id>/<filename>
                        Boolean isWebUpload = null;
                        Boolean isSoapUpload = null;
                        Boolean isInherited = null;
                        File inputFile = new File(originalPath);
                        File inputFileParent = inputFile.getParentFile();
                        String inputFileGrandParent = null;
                        if (inputFileParent != null) {
                            File ifpp = inputFileParent.getParentFile();
                            if (ifpp != null) {
                                try {
                                    inputFileGrandParent = ifpp.getCanonicalPath();
                                }
                                catch (IOException e) {
                                    throw new JobDispatchException(e);
                                }
                            }
                        }
                        String webUploadDirectory = null;
                        String tmpDir = gpConfig.getTempDir(jobContext).getAbsolutePath();
                        if (tmpDir != null) {
                            try {
                                webUploadDirectory = new File(tmpDir).getCanonicalPath();
                            }
                            catch (Throwable t) {
                                log.error(t);
                            }
                        }
                        isWebUpload = inputFileGrandParent != null && inputFileGrandParent.equals(webUploadDirectory);	                    
                        if (!isWebUpload) {
                            String soapAttachmentDir = ServerConfigurationFactory.instance().getSoapAttDir(jobContext).getAbsolutePath();
                            if (soapAttachmentDir != null) {
                                soapAttachmentDir = soapAttachmentDir + File.separator + jobInfo.getUserId();
                                try {
                                    soapAttachmentDir = new File(soapAttachmentDir).getCanonicalPath();
                                }
                                catch (Throwable t) {
                                    log.error(t);
                                }
                            }
                            File parentFile = inputFile.getParentFile();
                            String inputFileDirectory = null;
                            if (parentFile != null) {
                                try {
                                    inputFileDirectory = parentFile.getCanonicalPath();
                                }
                                catch (IOException e) {
                                    throw new JobDispatchException(e);
                                }
                            }
                            isSoapUpload = inputFileDirectory != null && inputFileDirectory.equals(soapAttachmentDir);
                        }

                        if (!isWebUpload && !isSoapUpload) {
                            if (inputFileGrandParent != null) {
                                try {
                                    isInherited = inputFileGrandParent.equals(rootJobDir.getCanonicalPath());
                                }
                                catch (IOException e) {
                                    throw new JobDispatchException(e);
                                }
                            }
                        }

                        if (isWebUpload) {
                            if (!jobContext.canReadJob()) {
                                vProblems.add("You are not permitted to access the requested file: "+inputFile.getName());
                                continue;
                            }
                        } 
                        else if (!isSoapUpload && !isInherited) {
                            vProblems.add("Input file " + new File(originalPath).getName() + " must be in SOAP attachment directory or web upload directory or a parent job directory");
                            continue;
                        }
                    } 
                    else {
                        vProblems.add("Unknown mode for parameter " + pinfo.getName() + ".");
                        continue;
                    }
                    File inFile = new File(originalPath);
                    if (!inFile.exists()) {
                        vProblems.add("Input file " + inFile + " does not exist.");
                        continue;
                    }

                    attrsCopy.remove(ParameterInfo.TYPE);
                    attrsCopy.remove(ParameterInfo.INPUT_MODE);
                } 
                else if (j >= formalParamsLength) {
                    log.debug("params[" + j + "]=" + paramsCopy[j].getName() + " has no formal parameter defined");
                } 
                else {
                    // check formal parameters for a file input type that
                    // was in fact sent as a string (ie. cached, http, or file path on server)
                    // find the formal parameter corresponding to this actual parameter
                    ParameterInfo[] formals = taskInfo.getParameterInfoArray();
                    HashMap<String, String> attrFormals = null;
                    fileType = null;
                    mode = null;
                    for (int formal = 0; formals != null && formal < formals.length; formal++) {
                        if (formals[formal].getName().equals(pinfo.getName())) {
                            attrFormals = formals[formal].getAttributes();
                            fileType = attrFormals.get(ParameterInfo.TYPE);
                            mode = attrFormals.get(ParameterInfo.MODE);
                            break;
                        }
                    }
                    boolean isURL = false;
                    boolean isS3URI = false;
                    
                    if (fileType != null && fileType.equals(ParameterInfo.FILE_TYPE) && mode != null && !mode.equals(ParameterInfo.OUTPUT_MODE) && originalPath != null) {
                        // handle http files by downloading them and substituting the downloaded filename for the URL in the command line.
                        if (new File(originalPath).exists()) {
                            boolean isInTaskLib = false;
                            boolean canRead = false;
                            try {
                                //does the current user have permission to access the file?
                                final GpFilePath serverFile=GpFileObjFactory.getRequestedGpFileObj("/data", "/"+originalPath);
                                canRead=serverFile.canRead(jobContext.isAdmin(), jobContext);
                            }
                            catch (Throwable t) {
                                log.error(t);
                            }
                            if (!canRead) {
                                //special-case: check if this is a file in the taskLib
                                isInTaskLib = isInTaskLib(taskInfo, originalPath);
                                if (!isInTaskLib) {
                                    vProblems.add("You are not permitted to access the requested file: "+originalPath);
                                    continue;
                                }
                            }
                            attrsCopy.remove(ParameterInfo.TYPE);
                            attrsCopy.remove(ParameterInfo.INPUT_MODE);
                        } 
                        else {
                            try {
                                if (!originalPath.startsWith("s3://")){
                                    if (originalPath != null) {
                                        new URL(originalPath);
                                        isURL = true;
                                    }
                                } else {
                                    isS3URI=true;
                                }
                            } 
                            catch (MalformedURLException mfe) {
                                // path on server
                                final boolean allowInputFilePaths = gpConfig.getAllowInputFilePaths(jobContext);
                                if (!allowInputFilePaths) {
                                    vProblems.add("You are not permitted to access the requested file: "+originalPath);
                                    continue;
                                }
                            }
                        }
                    }
                    if ((isURL || isS3URI) && jobType == JOB_TYPE.JOB && !isUrlMode) {
                        //don't translate input urls for visualizers and pipelines
                        //    and passByReference parameters
                        //    including special-case for IGV, GENEE, and GENE_E
                        
                        URI uri = null;
                        try {
                            uri = new URI(originalPath);
                        }
                        catch (URISyntaxException e) {
                            throw new JobDispatchException(e);
                        }

                        final String userInfo = uri.getUserInfo();
                        if (userInfo != null) {
                            final String[] usernamePassword = userInfo.split(":");
                            if (usernamePassword.length == 2) {
                                Authenticator.setDefault(new Authenticator() {
                                    @Override
                                    protected PasswordAuthentication getPasswordAuthentication() {
                                        return new PasswordAuthentication(usernamePassword[0], usernamePassword[1].toCharArray());
                                    }
                                });
                            }
                        }
                        InputStream is = null;
                        FileOutputStream os = null;
                        File outFile = null;
                        try {
                            String name = null;
                            boolean downloadUrl = true;
                            if ("file".equalsIgnoreCase(uri.getScheme())) {
                                log.debug("handling 'file:///' url: "+originalPath);
                                
                                final boolean allowInputFilePaths = gpConfig.getAllowInputFilePaths(jobContext);
                                if (allowInputFilePaths) {
                                    //check permissions and optionally convert value from url to server file path
                                    final String pname=pinfoRecord.getFormal().getName();
                                    final Param inputParam=new Param(new ParamId(pname), false);
                                    inputParam.addValue(new ParamValue(pinfo.getValue()));
                                    final boolean initDefault=false;
                                    ParamListHelper plh=new ParamListHelper(mgr, gpConfig, jobContext, pinfoRecord, jobContext.getJobInput(), inputParam, initDefault);
                                    GpFilePath gpFilePath=null;
                                    try {
                                        gpFilePath=plh.initGpFilePath(inputParam.getValues().get(0));
                                    }
                                    catch (Exception e) {
                                        throw new JobDispatchException(e.getLocalizedMessage());
                                    }
                                    if (gpFilePath != null) {
                                        final String serverPath=gpFilePath.getServerFile().getAbsolutePath();
                                        boolean canRead=gpFilePath.canRead(jobContext.isAdmin(), jobContext);
                                        if (!canRead) {
                                            throw new JobDispatchException("You are not permitted to access the file: "+pinfo.getValue());
                                        }
                                        pinfo.setValue(serverPath);
                                        attrsCopy.remove(ParameterInfo.TYPE);
                                        attrsCopy.remove(ParameterInfo.INPUT_MODE);
                                        downloadUrl = false;
                                    } 
                                } 
                                else {
                                    boolean isAllowed = false;
                                    File inputFile = new File(uri);
                                    String inputFileDirectory = inputFile.getParentFile().getCanonicalPath();
                                    String inputFileGrandParent = inputFile.getParentFile().getParentFile().getCanonicalPath();

                                    //special case: uploaded file from web client
                                    //                <java.io.tmpdir>/<user_id>_run[0-9]+.tmp/<filename>
                                    String webUploadDirectory = gpConfig.getTempDir(null).getCanonicalPath();
                                    boolean isWebUpload = inputFileGrandParent.equals(webUploadDirectory);
                                    isAllowed = isWebUpload;

                                    //special case: uploaded file from SOAP client
                                    //                <soap.attachment.dir>/<user_id>/<filename>
                                    if (!isAllowed) {
                                        String soapAttachmentDir = ServerConfigurationFactory.instance().getSoapAttDir(jobContext).getCanonicalPath();
                                        soapAttachmentDir =new File(soapAttachmentDir + File.separator + jobInfo.getUserId()).getCanonicalPath();
                                        boolean isSoapUpload = inputFileDirectory.equals(soapAttachmentDir);
                                        isAllowed = isSoapUpload;
                                    }

                                    //special case: output from a previous job
                                    if (!isAllowed) {
                                        String jobsDirectory = rootJobDir.getCanonicalPath();
                                        boolean isJobOutput = jobsDirectory.equals(inputFileGrandParent);
                                        if (isJobOutput) {
                                            try {
                                                String parentFileName = inputFile.getParentFile().getName();
                                                int jobNumber = Integer.parseInt(parentFileName);
                                                //only allow access if the owner of this job has at least read access to the job which output this input file
                                                boolean canRead = canReadJob(mgr, jobContext.isAdmin(), jobInfo.getUserId(), jobNumber);
                                                isAllowed = isJobOutput && canRead;
                                            }
                                            catch (NumberFormatException e) {
                                                log.error("Invalid job number in file path: jobId="+jobId+", file="+inputFile.getAbsolutePath());
                                            }
                                        } 
                                    }
                                    if (!isAllowed) {
                                        vProblems.add("File input URLs are not allowed on this GenePattern server: " + inputFile.getAbsolutePath());
                                        continue;                                    
                                    }

                                    File f = new File(uri);
                                    pinfo.setValue(f.getAbsolutePath());
                                    attrsCopy.remove(ParameterInfo.TYPE);
                                    attrsCopy.remove(ParameterInfo.INPUT_MODE);
                                    downloadUrl = false;
                                }
                            }
                            else if ("s3".equalsIgnoreCase(uri.getScheme())) {
                                   
                                        //URLConnection conn = url.openConnection();
                                        //is = conn.getInputStream();
                                // always need the name        
                                int idx = uri.getPath().lastIndexOf("/");
                                name = uri.getPath().substring(idx+1);
                                
                                GpConfig jobConfig = ServerConfigurationFactory.instance();
                                final boolean directExternalUploadEnabled = (jobConfig.getGPIntegerProperty(jobContext, "direct_external_upload_trigger_size", -1) >= 0);
                                final boolean directDownloadEnabled = (jobConfig.getGPProperty(jobContext, ExternalFileManager.classPropertyKey, null) != null);
                               
                                if (is == null && downloadUrl && !(directExternalUploadEnabled || directDownloadEnabled )) {
                                    // its a remote S3 file but we are not running on AWS  
                                    // Therefore we need to either throw an error or download the
                                    // S3 URL to local, but since no AWSExternalFileManager exists we
                                    // don't know how to set the AWS env (i.e. no init-aws-cli.sh will exist)
                                    // so we will just give a try to exec and copy the file down and error if
                                    // it does not work. We will try to get an "aws" command line expansion
                                    // so that this can be setup if need be by someone running locally
                                    File tmp = File.createTempFile("name","");
                                    String filename = tmp.getName();
                                    tmp.delete();
                                    String aws = jobConfig.getGPProperty(jobContext, "aws", "aws");
                                    BufferedWriter bw = new BufferedWriter(new StringWriter());
                                    bw.write("aws s3 sync --exclude \"*\" --include \"");
                                    bw.write(name);
                                    bw.write("\"  ");
                                    bw.write(uri.toString());
                                    bw.write("  ");
                                    bw.write(tmp.getAbsolutePath());
                                    Process proc = Runtime.getRuntime().exec(bw.toString());
                                    debugProcessStdOutAndErr(proc, "s3download");
                                    is = new FileInputStream(tmp);
                                    
                                }
                            
                            } else {
                                final URL url = uri.toURL();
                                final boolean isLocalHost=UrlUtil.isLocalHost(gpConfig, baseGpHref, uri);
                                if (log.isDebugEnabled()) {
                                    log.debug("isLocalHost("+uri+")="+isLocalHost);
                                }
                                if (isLocalHost) {
                                    try {
                                        File file = localInputUrlToFile(mgr, gpConfig, jobContext, url);
                                        if (file != null) {
                                            pinfo.setValue(file.getAbsolutePath());
                                            attrsCopy.remove(ParameterInfo.TYPE);
                                            attrsCopy.remove(ParameterInfo.INPUT_MODE);
                                            downloadUrl = false;
                                        }
                                    } 
                                    catch (IllegalArgumentException e) {
                                        // user tried to access file that he is not allowed to
                                        vProblems.add(e.getMessage());
                                        downloadUrl = false;
                                    }
                                }
                                
                                // TODO: check for previously cached version of the file
                                // TODO: if necessary cache the file
                                
                                if (is == null && downloadUrl) {
                                    try {
                                        URLConnection conn = url.openConnection();
                                        name = getDownloadFileName(conn, url);
                                        is = conn.getInputStream();
                                    } 
                                    catch (IOException e) {
                                        // FTP downloads fail a lot.  Try again a different way
                                        // JTL 1/11/2022  FTPClient.Stream retrieveFileStream(String remote)
                                        if (url.getProtocol().equalsIgnoreCase("ftp")){
                                            FTPDownloader ftpDownloader;
                                            try {
                                                File tmpFile = File.createTempFile("ftp", "tmp");
                                                System.out.println(tmpFile.getAbsolutePath());
                                                tmpFile.deleteOnExit();
                                                ftpDownloader = new FTPDownloader(url.getHost(), "anonymous", "genepattern@ucsd.edu");
                                                ftpDownloader.downloadFile(url.getPath(), tmpFile.getAbsolutePath());
                                                System.out.println("FTP DOWNLOAD MADE IT   " + outDir);
                                                
                                                ftpDownloader = new FTPDownloader(url.getHost(), "anonymous", "genepattern@ucsd.edu");
                                                // replace the inputStream
                                                is = ftpDownloader.downloadFileStream(url.getPath());
                                                
                                            }
                                            catch (Exception e1) {
                                                // TODO Auto-generated catch block
                                                e1.printStackTrace();
                                                downloadUrl = false;
                                            }
                                                
                                               
                                        } else {
                                            e.printStackTrace();
                                            vProblems.add("Unable to connect to " + url + ". ");
                                            downloadUrl = false;
                                        }
                                    }
                                }
                            }
                            if (downloadUrl) {
                                outFile = new File(outDir, name);
                                
                                if (outFile.exists()) {
                                    // ensure that 2 file downloads for a job don't have the same name
                                    if (name.length() < 3) {
                                        name = "download";
                                    }
                                    outFile = File.createTempFile(name, null, outDir);
                                }
                                GpConfig jobConfig = ServerConfigurationFactory.instance();

                                // Handle URLs pointed at GenePattern Notebook projects as a special case
                                boolean gpnbDownloadsEnabled = jobConfig.getGPBooleanProperty(jobContext, GPNBDownloadHelper.ENBLED_KEY, true);
                                if (gpnbDownloadsEnabled && GPNBDownloadHelper.isGPNBFile(jobContext, uri)) {
                                    uri = GPNBDownloadHelper.constructDownloadURL(jobContext, uri);
                                    is = GPNBDownloadHelper.constructInputStream(uri);
                                }

                                // Handle S3 support
                                final boolean directExternalUploadEnabled = (jobConfig.getGPIntegerProperty(jobContext, "direct_external_upload_trigger_size", -1) >= 0);
                                final boolean directDownloadEnabled = (jobConfig.getGPProperty(jobContext, ExternalFileManager.classPropertyKey, null) != null);
                               
                                if (directExternalUploadEnabled || directDownloadEnabled ){
                                    // Using s3 direct up/downloads so we do not actually grab the file here
                                    // instead we write the URL and destination filename to a hidden file
                                    // that will be used to tell the JobRunner what needs to be done
                                    // It will then setup a script that will be run on the compute node
                                    // to do the actual download
                                    ParameterInfo formalParam = null;
                                    for (int formal = 0; formals != null && formal < formals.length; formal++) {
                                        if (formals[formal].getName().equals(pinfo.getName())) {
                                            formalParam = formals[formal];
                                            break;
                                        }
                                    }
                                    try {
                                        final ParamListValue rec=ParamListHelper.initFromValue(mgr, gpConfig, jobContext, 
                                            jobContext.getJobInput().getBaseGpHref(), formalParam, new ParamValue(uri.toString()));
                                   
                                        outFile = rec.getGpFilePath().getServerFile();
                                    } catch (GpFilePathException gpe){
                                        log.error("Could not update file path for URL parameter " + gpe.getMessage());
                                    }
                                    
                                    // JTL for URL download deferral to compute nodes
                                    File downloadListingFile = new File(outDir,ExternalFileManager.downloadListingFileName);
                                    BufferedWriter writer = new BufferedWriter(new FileWriter(downloadListingFile, true));    
                                    writer.write(uri + "\t" + outFile.getAbsolutePath());
                                    writer.close();
                                } else {
                                    // traditional GenePattern, the head-node does the download
                                    log.debug("job "+jobId+"."+pinfo.getName()+" starting download to "+outFile);
    
                                    os = new FileOutputStream(outFile);
                                    byte[] buf = new byte[1024];
                                    int bytesRead;
                                    while ((bytesRead = is.read(buf, 0, buf.length)) != -1) {
                                        os.write(buf, 0, bytesRead);
                                    }
                                    log.debug("job "+jobId+"."+pinfo.getName()+" finished download to "+outFile);
                                }
                                //TODO: mark file for delete from job results directory on handle job completion
                                attrsCopy.put(ORIGINAL_PATH, originalPath);
                                pinfo.setValue(outFile.getAbsolutePath());
                 
                            }
                        } 
                        catch (IOException ioe) {
                            vProblems.add("An error occurred while downloading " + uri);
                        }
                        finally {
                            if (userInfo != null) {
                                Authenticator.setDefault(null);
                            }
                            if (is != null) {
                                try {
                                    is.close();
                                } 
                                catch (IOException x) {
                                }
                            }
                            if (os != null) {
                                try {
                                    os.close();
                                } 
                                catch (IOException x) {
                                }
                            }
                            // don't set this until after the close...
                            if (outFile != null) {
                                //attrsActual.put(ORIGINAL_LAST_MODIFIED, ""+outFile.lastModified());
                            }
                        }
                    }
                }
            } // end for each parameter
        } // end if parameters not null

        // add local file paths to the jobContext ...
        final List<String> inputFiles=listInputFiles(mgr, gpConfig, jobContext, paramsCopy, paramInfoMap);
        for(final String inputFile : inputFiles) {
            jobContext.addLocalFilePath(inputFile);
            
            try {
                File f = new File(inputFile);
                if (f.exists()) f.setLastModified(System.currentTimeMillis());
             } catch (Exception e){
                System.out.println("  PROBLEM WITH setting last modified on Input File: " + inputFile);
                e.printStackTrace();
            }
        }

        // build the command line, replacing <variableName> with the same name from the properties
        // (ParameterInfo[], System properties, environment variables, and built-ins merged)
        // build props again, now that downloaded files are set
        final Properties props;
        try {
            props = setupProps(taskInfo, taskName, parentJobId, jobId, jobInfo.getTaskID(), taskInfoAttributes, paramsCopy,
                    environmentVariables, taskInfo.getParameterInfoArray(), jobInfo.getUserId(), false);
            if (!Strings.isNullOrEmpty(baseGpHref)) {
                props.setProperty("GenePatternURL", baseGpHref+"/");
            }
        }
        catch (MalformedURLException e) {
            throw new JobDispatchException(e);
        }
        // optionally, override the java flags if they have been overridden in the job configuration file
        String javaFlags = gpConfig.getGPProperty(jobContext, "java_flags");
        if (javaFlags != null) {
            props.setProperty("java_flags", javaFlags);
        }
        // ensure that the docker image is set before the command line is created.  This has to be handled specially
        // to get the correct default
        String dockerImage=(gpConfig.getValue(jobContext,JobRunner.PROP_DOCKER_IMAGE)).getValue();
        if (Strings.isNullOrEmpty(dockerImage)) {
            dockerImage=(gpConfig.getValue(jobContext,JobRunner.PROP_DOCKER_IMAGE_DEFAULT)).getValue();
        }
        props.setProperty(JobRunner.PROP_DOCKER_IMAGE, dockerImage);
        
        paramsCopy = stripOutSpecialParams(paramsCopy);
        // check that all parameters are used in the command line
        // and that all non-optional parameters that are cited actually exist
        ParameterInfo[] formalParameters = taskInfo.getParameterInfoArray();
        Vector<String> parameterProblems = validateParameters(props, taskName, taskInfoAttributes.get(COMMAND_LINE), paramsCopy, formalParameters, true);
        vProblems.addAll(parameterProblems);

        String[] commandTokens = new String[0];
        try {
            String cmdLine = taskInfoAttributes.get(COMMAND_LINE);
            if (cmdLine == null || cmdLine.trim().length() == 0) {
                vProblems.add("Command line not defined");
            }
            else {
                String commandPrefix = null;
                try {
                    commandPrefix = gpConfig.getCommandPrefix(jobContext);
                }
                catch (MalformedURLException e) {
                    throw new JobDispatchException(e);
                }
                if (!Strings.isNullOrEmpty(commandPrefix)) {
                    cmdLine = commandPrefix + " " + cmdLine;
                }

                final Map<String,String> propsMap=CommandLineParser.propsToMap(props);
                final List<String> cmdLineArgs = CommandLineParser.createCmdLine(gpConfig, jobContext, cmdLine, propsMap, paramInfoMap);
                if (log.isDebugEnabled()) {
                    final List<String> cmdLineArgsC = CommandLineParser.createCmdLine(gpConfig, jobContext, cmdLine, paramInfoMap);
                    log.debug("cmdLineArgs      : "+cmdLineArgs); // 3.9.2, it works!
                    log.debug("cmdLineArgs (new): "+cmdLineArgsC); // under development, file input params not yet implemented
                }
                if (cmdLineArgs == null || cmdLineArgs.size() == 0) {
                    vProblems.add("Command line not defined");
                }
                commandTokens = new String[cmdLineArgs.size()];
                int i=0;
                for(String arg : cmdLineArgs) {
                    commandTokens[i++] = arg;
                }
            }
        }
        catch (Throwable t) {
            vProblems.add(t.getLocalizedMessage());
        }
        
        String stdoutFilename = STDOUT;
        String stderrFilename = STDERR;
        String stdinFilename = null;
        int exitCode = 0;
        List<String> commandLineList = new ArrayList<String>(commandTokens.length);
        boolean addLast = true;
        for (int j = 0; j < commandTokens.length - 1; j++) {
            if (commandTokens[j].equals(STDOUT_REDIRECT)) {
                stdoutFilename = commandTokens[++j];
                if ("".equals(stdoutFilename)) {
                    vProblems.add("Missing name for standard output redirect");
                }
                addLast = false;
            }
            else if (commandTokens[j].equals(STDERR_REDIRECT)) {
                stderrFilename = commandTokens[++j];
                if ("".equals(stderrFilename)) {
                    vProblems.add("Missing name for standard error redirect");
                }
                addLast = false;
            }
            else if (commandTokens[j].equals(STDIN_REDIRECT)) {
                stdinFilename = commandTokens[++j];
                if ("".equals(stdinFilename)) {
                    vProblems.add("Missing name for standard input redirect");
                }
                addLast = false;
            }
            else {
                addLast = true;
                commandLineList.add(commandTokens[j]);
            }
        }

        if (addLast && commandTokens != null && commandTokens.length > 0) {
            commandLineList.add(commandTokens[commandTokens.length - 1]);
        }
        commandTokens = commandLineList.toArray(new String[0]);
        String lastToken = commandTokens[commandTokens.length - 1];
        if (lastToken.equals(STDOUT_REDIRECT)) {
            vProblems.add("Missing name for standard output redirect");
        } 
        else if (lastToken.equals(STDERR_REDIRECT)) {
            vProblems.add("Missing name for standard error redirect");
        } 
        else if (lastToken.equals(STDIN_REDIRECT)) {
            vProblems.add("Missing name for standard input redirect");
        } 
        //check for stdin before starting the process...
        File stdinFile = null;
        if (stdinFilename != null) {
            stdinFile = new File(stdinFilename);
            if (!stdinFile.isAbsolute()) {
                //if not absolute use a path relative to the output directory
                stdinFile = new File(outDir, stdinFilename);
            }
            stdinFilename=stdinFile.getAbsolutePath();
            if (!stdinFile.canRead()) {
                //... so that errors can be thrown before starting the process
                vProblems.add("Can't read file for standard input redirect: "+stdinFilename);
                stdinFile = null;
            }
        }
        
        //close hibernate session before running the job, but don't save the parameter info
        mgr.closeCurrentSession();

        //check for errors
        StringBuffer stderrBuffer = new StringBuffer();
        if (vProblems.size() > 0) {
            for (Enumeration<String> eProblems = vProblems.elements(); eProblems.hasMoreElements();) {
                stderrBuffer.append(eProblems.nextElement() + "\n");
            }
            if (stderrBuffer.length() > 0) {
                if (exitCode == 0) {
                    exitCode = -1;
                }
                writeStringToFile(outDir, STDERR, stderrBuffer.toString());
                try {
                    GenePatternAnalysisTask.handleJobCompletion(jobId, -1);
                }
                catch (Exception e) {
                    throw new JobDispatchException("Error recording job submission error: "+stderrBuffer.toString(), e);
                }
            }
            return;
        } 

        final File stdoutFile;
        final File stderrFile;
        boolean renameStdout = stdoutFilename == null;
        boolean renameStderr = stderrFilename == null;
        try {
            if (renameStdout) {
                stdoutFile = File.createTempFile("stdout", null);
                stdoutFilename = STDOUT;
            } 
            else {
                stdoutFile = new File(outDir, stdoutFilename);
            }
            if (renameStderr) {
                stderrFile = File.createTempFile("stderr", null);
                stderrFilename = STDERR;
            }
            else {
                stderrFile = new File(outDir, stderrFilename);
            }
        }
        catch (IOException e) {
            throw new JobDispatchException(e);
        }


        //special-case for visualizer
        if (jobType == JOB_TYPE.VISUALIZER) {
            try {
                GenePatternAnalysisTask.handleJobCompletion(jobId, 0);
            }
            catch (Exception e) {
                throw new JobDispatchException("Error handling visualizer", e);
            }
            return;
        }

        //special-case for visualizer
        if (jobType == JOB_TYPE.JAVASCRIPT) {
            try
            {
                //For javascript modules, output the launch url as a hidden file to the jobResultsDirectory
                HashMap<String, List<String>> substituteParamValuesMap = ValueResolver.getParamValues(gpConfig, jobContext, props, paramInfoMap);
                String launchUrl = JavascriptHandler.saveLaunchUrl(gpConfig, taskInfo, outDir, substituteParamValuesMap);
                if (log.isDebugEnabled()) {
                    log.debug("jobId="+jobId+", launchUrl="+launchUrl);
                }
                GenePatternAnalysisTask.handleJobCompletion(mgr, jobId, 0, null);
            }
            catch (Exception e) {
                throw new JobDispatchException("Error handling visualizer", e);
            }
            return;
        }

        //special-case, for -Xmx flag
        try {
            commandTokens=CustomXmxFlags.addOrReplaceXmxFlag(gpConfig, jobContext, commandTokens);
        }
        catch (Throwable t) {
            log.error("Unexpected error initializing custom xmx flags for job="+jobId, t);
        }

        if (log.isInfoEnabled()) {
            final StringBuffer commandLine = new StringBuffer();
            for(final String arg : commandTokens) {
                commandLine.append(arg+" ");
            }
            log.info("running " + taskName + " (job " + jobId + ") command: " + commandLine.toString());
        }

        runCommand(mgr, gpConfig, jobContext, commandTokens, environmentVariables, outDir, stdoutFile, stderrFile, stdinFile);
    }

    protected List<String> listInputFiles(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext jobContext, final ParameterInfo[] paramsCopy, final Map<String, ParameterInfoRecord> paramInfoMap) {
        final List<String> inputFilePaths=new ArrayList<String>();
        if (log.isDebugEnabled()) {
            log.debug("listing paramsCopy ...");
        }
        
        BufferedWriter writer = null;
        
        try {
            Map<String, URI>  fileUrlMap = new HashMap< String,URI>(); 
            try {
                final File path = JobManager.getWorkingDirectory(jobContext.getJobInfo());
                if (!path.exists()){
                    // create it if not already there, but leave the contents alone if it is
                    path.mkdirs();
                }
                File outDir = path.getAbsoluteFile();
                File downloadListingFile = new File(outDir,ExternalFileManager.downloadListingFileName);
                // existing file contents, we don't want to download the same file twice needlessly
               
                writer = new BufferedWriter(new FileWriter(downloadListingFile, true));  
            } catch (Exception e){
                // swallow it, we want to do the rest anyway
            }
            
            
            for(final ParameterInfo copy : paramsCopy) {
                log.debug("    "+copy.getName()+"="+copy.getValue());
                log.debug("      copy.isInputFile: "+copy.isInputFile());
                log.debug("      copy._isUrlMode: "+copy._isUrlMode());

                final ParameterInfoRecord record=paramInfoMap.get(copy.getName());
                final ParameterInfo formal;
                final ParameterInfo actual;
                if (record!=null) {
                    formal=record.getFormal();
                    actual=record.getActual();
                }
                else {
                    formal=null;
                    actual=null;
                }
                if (actual != null) {
                    log.debug("      actual.value="+actual.getValue());
                }
                if (formal != null) {
                    log.debug("      formal.isInputFile="+formal.isInputFile());
                }
                if (formal != null && formal.isInputFile()) {
                    inputFilePaths.add(copy.getValue());

                    //check for file list files
                    final Param actualValues=jobContext.getJobInput().getParam(formal.getName());
                    final boolean hasFilelist=ParamListHelper.isCreateFilelist(formal, actualValues);
                    
                    int i=0;
                    // create a map of gp files to urls so we can tell the external file manager to have them downloaded.
                    // necessary here because batch jobs and file lists do their downloads at a different point where
                    // the job is not yet created so we cannot add the URL as we do for other files
                    if ((actualValues != null) && (actualValues.getValues() != null)){
                        for(final ParamValue actualValue : actualValues.getValues()) {
                            log.debug("        actual.value["+(i++)+"]="+actualValue.getValue());
                            if (DataManager.getExternalFileManager(jobContext) != null){
                                // we need to add the contents to the downloadFileListing
                                try {
                                    final ParamListValue rec=ParamListHelper.initFromValue(mgr, gpConfig, jobContext, 
                                            jobContext.getJobInput().getBaseGpHref(), formal, actualValue);
                                    if (rec.getUrl() != null) {                                    
                                        fileUrlMap.put(rec.getGpFilePath().getServerFile().getAbsolutePath(), rec.getUrl().toURI());
                                    } else if (rec.getUri() != null){
                                        fileUrlMap.put(rec.getGpFilePath().getServerFile().getAbsolutePath(), rec.getUri());
                                    }
                                } catch(Exception e){
    
                                }
                            }
                        }
                    }
                    try {
                        final List<GpFilePath> gpFilePaths=ParamListHelper.getListOfValues(mgr, gpConfig, jobContext, jobContext.getJobInput(), formal, actualValues, false);
                        if (gpFilePaths != null) {
                            int j=0;
                            for(final GpFilePath gpFilePath : gpFilePaths) {
                                File localPath=gpFilePath.getServerFile();
                                log.debug("        localPath["+(j++)+"]"+localPath);
                                if (localPath!=null) {
                                    inputFilePaths.add(localPath.getPath());
                                    if (!localPath.exists()){
                                        
                                        URI uri = fileUrlMap.get(localPath.getAbsolutePath());
                                        
                                        if ((uri != null) && (writer != null) ){
                                            writer.newLine();
                                            writer.write(uri + "\t" + localPath.getAbsolutePath());
                                            
                                        }
                                    }
                                }
                            }
                        }
                    }
                    catch (Throwable t) {
                        log.error("Error getting gpFilePaths, job="+jobContext.getJobNumber()+", pname="+formal.getName(), t);
                    }
                }        
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if (writer != null){
                try {
                    writer.flush();
                    writer.close();
                } catch (Exception e){
                    log.error(e);
                }
                
            }
        }
        return inputFilePaths;
    }

    protected void setPinfoValueForDirectoryInputParam(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext jobContext, final ParameterInfo pinfo, final ParameterInfoRecord pinfoRecord) throws JobDispatchException {
        //check permissions and optionally convert value from url to server file path
        final String pname=pinfoRecord.getFormal().getName();
        final Param inputParam=new Param(new ParamId(pname), false);
        inputParam.addValue(new ParamValue(pinfo.getValue()));
        final boolean initDefault=false;
        ParamListHelper plh=new ParamListHelper(mgr, gpConfig, jobContext, pinfoRecord, jobContext.getJobInput(), inputParam, initDefault);
        GpFilePath directory=null;
        try {
            directory=plh.initDirectoryInputValue(inputParam.getValues().get(0));
        }
        catch (Exception e) {
            throw new JobDispatchException(e.getLocalizedMessage());
        }
        if (directory != null) {
            final String serverPath=directory.getServerFile().getAbsolutePath();
            pinfo.setValue(serverPath);
            final boolean canRead=directory.canRead(jobContext.isAdmin(), jobContext);
            if (!canRead) {
                throw new JobDispatchException("You are not permitted to access the directory: "+pinfo.getValue());
            }
        }
    }

    /**
     * Check the user's disk usage quota before running the job. If the user is at or above their quota a 
     * JobDispatchException is thrown.
     * 
     * @param gpConfig
     * @param jobContext
     * @throws JobDispatchException
     */
    private void checkDiskQuota(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext jobContext, final String taskName) throws JobDispatchException {
        //is disk space available
        final boolean allowNewJob = gpConfig.getGPBooleanProperty(jobContext, "allow.new.job", true);
        if (!allowNewJob) {
            String errorMessage = 
                "Job did not run because there is not enough disk space available.\n";
            throw new JobDispatchException(errorMessage);
        }

        //check if the user is above their disk quota
        try
        {
            DiskInfo diskInfo = DiskInfo.createDiskInfo(mgr, gpConfig, jobContext);

            if(diskInfo.isAboveQuota())
            {
                String errorMessage = "Job did not run because disk usage quota exceeded." +
                        "DiskUsage: " + diskInfo.getDiskUsageFilesTab().getDisplayValue()
                        + ". Disk Quota: " + diskInfo.getDiskQuota().getDisplayValue();
                //disk usage exceeded so do not allow user to run a job
                throw new JobDispatchException(errorMessage);
            }
            
            if(diskInfo.isAboveMaxSimultaneousJobs())
            {
                String errorMessage = "Job did not run because maximum simultaneous processing jobs exceeded." +
                        "Processing: " + diskInfo.getNumProcessingJobs()
                        + ". Max simultaneous: " + diskInfo.getMaxSimultaneousJobs();
                //  max simultaneous jobs exceeded so do not allow user to run a job
                final GpContext gpContext=GpContext.getServerContext();
                boolean throwException = diskInfo.notifyMaxJobsExceeded( jobContext, gpConfig, taskName);
                //
                // Sometimes we want warning but will allow jobs to go on to pending state.  They will be delayed 
                // from running by the AnalysisJobScheduler until the max simultaneous is not exceeded
                //
                if (throwException)  throw new JobDispatchException(errorMessage);
            }
            
        }
        catch(DbException db)
        {
            //just log exception and continue
            //do not want to prevent job from running if there was an
            //error getting the disk usage
            log.error(db);
        }
    }
    
    private ChoiceInfo initChoiceInfo(final GpContext jobContext, final ParameterInfoRecord pinfoRecord, final ParameterInfo pinfo) {
        if (ChoiceInfo.hasChoiceInfo(pinfoRecord.getFormal())) {
            //it's a file choice
            log.debug("Checking for cached value for File Choice, "+pinfo.getName()+"="+pinfo.getValue());
            ChoiceInfo choiceInfo = ChoiceInfo.getChoiceInfoParser(jobContext).initChoiceInfo(pinfoRecord.getFormal());
            return choiceInfo;
        }
        return null;
    }

    private static CommandExecutor2 initCmdExec2(final GpConfig gpConfig, final GpContext jobContext) throws JobDispatchException {
        final String executorId=gpConfig.getExecutorId(jobContext);
        if (executorId==null) {
            throw new JobDispatchException("Server error: 'executor' not set for job="+jobContext.getJobInfo().getJobNumber());
        }
        final CommandExecutor cmdExec=CommandManagerFactory.getCommandManager().getCommandExecutorsMap().get(executorId);
        if (cmdExec==null) {
            throw new JobDispatchException("Server error: CommandExecutor not set for executorId="+executorId);
        }
        return CommandExecutor2Wrapper.createCmdExecutor(cmdExec);
    }

    private void runCommand(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext jobContext, final String[] cmdLineArgs, final Map<String,String> environmentVariables, final File runDir, final File stdoutFile, final File stderrFile, final File stdinFile) 
    throws JobDispatchException
    {
        final boolean isPipeline=jobContext.getTaskInfo().isPipeline();
        final long jobDispatchTimeout = gpConfig.getGPIntegerProperty(jobContext, "job.dispatch.timeout", 300000);
        Future<Integer> task = null;
        try {
            task = executor.submit(new Callable<Integer>() {
                public Integer call() throws Exception {
                    final CommandExecutor2 cmdExec=initCmdExec2(gpConfig, jobContext);
                    cmdExec.runCommand(jobContext, cmdLineArgs, environmentVariables, runDir, stdoutFile, stderrFile, stdinFile);
                    return JobStatus.JOB_PROCESSING;
                }
            });
            int job_status = task.get(jobDispatchTimeout, TimeUnit.MILLISECONDS);
            try {
                if (!isPipeline) {
                    //pipeline handler sets the status for the pipeline job
                    mgr.beginTransaction();
                    AnalysisJobScheduler.setJobStatus(mgr, jobContext.getJobInfo().getJobNumber(), job_status);
                    mgr.commitTransaction();
                }
                if (isPipeline) {
                    CommandManagerFactory.getCommandManager().wakeupJobQueue();
                }
            }
            catch (Throwable t) {
                mgr.rollbackTransaction();
                throw new JobDispatchException("Error changing job status for job #"+jobContext.getJobInfo().getJobNumber(), t);
            }
        }
        catch (ExecutionException e) {
            throw new JobDispatchException(e);
        }
        catch (TimeoutException e) {
            task.cancel(true);
            throw new JobDispatchException("Timeout after "+jobDispatchTimeout+" ms while dispatching job #"+jobContext.getJobInfo().getJobNumber()+" to queue.", e);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 
     * @param taskInfo
     * @param serverFilePath
     * @return true iff the originalPath is to a file in the taskLib directory for the given taskInfo.
     */
    private static boolean isInTaskLib(TaskInfo taskInfo, String originalPath) {
        if (taskInfo == null) {
            log.error("Null taskInfo arg");
            return false;
        }
        String taskLibDir = DirectoryManager.getTaskLibDir(taskInfo);
        if (taskLibDir == null) {
            log.error("Unable to get taskLibDir for taskInfo: "+taskInfo.getName());
            return false;
        }
        File serverFile = new File(originalPath);
        String filename = serverFile.getName();
        File file = new File(taskLibDir, filename);
        return file != null && file.canRead();
    }

    /**
     * Make a deep copy of the ParameterInfo[] for the given job info. 
     * So that onJob can work on a local copy of the array without inadvertently
     * saving changes back to the DB.
     * 
     * @param jobInfo
     */
    private static final ParameterInfo[] copyParameterInfoArray(JobInfo jobInfo) {
        final ParameterInfo[] paramsOrig = jobInfo.getParameterInfoArray();
        ParameterInfo[] params = null;
        if (paramsOrig != null) {
            params = new ParameterInfo[paramsOrig.length];
            int i=0;
            for(ParameterInfo paramOrig : paramsOrig) {
                params[i++]=copyParameterInfo(paramOrig);
            }
        }
        return params;
    }

    private static final ParameterInfo copyParameterInfo(ParameterInfo orig) {
        return ParameterInfo._deepCopy(orig);
    }

    private static JobInfoWrapper getJobInfoWrapper(final HibernateSessionManager mgr, final GpConfig gpConfig, String userId, int jobNumber) {
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            String contextPath = gpConfig.getGpPath();
            if (!contextPath.startsWith("/")) {
                contextPath = "/" + contextPath;
            }
            String cookie = "";
            JobInfoManager m = new JobInfoManager();
            return m.getJobInfo(cookie, contextPath, userId, jobNumber);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    /** @deprecated pass in a valid Hibernate session */
    public static void handleJobCompletion(int jobId, int exitCode) {
        handleJobCompletion(jobId, exitCode, null);
    }

    /** @deprecated pass in a valid Hibernate session */
    public static void handleJobCompletion(int jobId, int exitCode, String errorMessage) {
        handleJobCompletion(org.genepattern.server.database.HibernateUtil.instance(),
                jobId, exitCode, errorMessage);
    }

    public static void handleJobCompletion(final HibernateSessionManager mgr, int jobId, int exitCode, String errorMessage) {
        File jobDir = new File(GenePatternAnalysisTask.getJobDir(""+jobId));
        File stdoutFile = new File(jobDir, STDOUT);
        File stderrFile = new File(jobDir, STDERR);
        handleJobCompletion(mgr, jobId, exitCode, errorMessage, jobDir, stdoutFile, stderrFile);
    }

    /** @deprecated pass in a valid Hibernate session */
    public static void handleJobCompletion(int jobId, int exitCode, String errorMessage, String stdoutFilename, String stderrFilename) {
        File jobDir = new File(GenePatternAnalysisTask.getJobDir(""+jobId));
        File stdoutFile = new File(jobDir, stdoutFilename);
        File stderrFile = new File(jobDir, stderrFilename);
        handleJobCompletion(jobId, exitCode, errorMessage, jobDir, stdoutFile, stderrFile);
    }

    /** @deprecated pass in a valid Hibernate session */
    public static void handleJobCompletion(final int jobId, final int exitValue, final String errorMessage, final File jobDir, final File stdoutFile, final File stderrFile) {
        handleJobCompletion(org.genepattern.server.database.HibernateUtil.instance(), 
                jobId, exitValue, errorMessage, jobDir, stdoutFile, stderrFile);
    }
    
    public static void handleJobCompletion(final HibernateSessionManager mgr, final int jobId, final int exitValue, final String errorMessage, final File jobDir, final File stdoutFile, final File stderrFile) {
        log.debug("job "+jobId+" completed with exitValue="+exitValue);
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            final JobInfo jobInfo = new AnalysisDAO(mgr).getJobInfo(jobId);
            //handle special-case when the job is deleted before we get to handle the job results, e.g. a running pipeline was deleted
            if (jobInfo == null) {
                log.error("job #"+jobId+"was deleted before handleJobCompletion");
                return;
            }
            handleJobCompletionInThread(mgr, jobInfo, exitValue, errorMessage, jobDir, stdoutFile, stderrFile);
        }
        catch (Throwable t) {
            log.error("Unexpected exception in handleJobCompletion for jobId="+jobId, t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

    private static void handleJobCompletionInThread(final HibernateSessionManager mgr, final JobInfo jobInfo, int exitValue, String errorMessage, File jobDir, File stdoutFile, File stderrFile) {
        //handle special-case when the job is deleted before we get to handle the job results, e.g. a running pipeline was deleted
        if (jobInfo == null) {
            log.error("jobInfo==null");
            return;
        }
        final int jobId=jobInfo.getJobNumber();
        log.debug("job "+jobId+" completed with exitValue="+exitValue);
        
        //handle special-case when handleJobCompletion has already been called for this job
        if (isFinished(jobInfo)) {
            log.debug("job #"+jobId+" is already finished, with status="+jobInfo.getStatus());
            return;
        }
        
        GpConfig gpConfig = ServerConfigurationFactory.instance();
        GpContext jobContext = GpContext.getContextForJob(jobInfo);
        
        //validate the jobDir
        //Note: hard-coded rules, circa GP 3.2.4 and earlier, if we ever want to allow alternate locations for
        //    the job dir we will have to modify this restriction
        if (jobDir == null) {
            log.error("Invalid arg, jobDir=null");
            return;
        }
        try {
            File expectedJobDir = new File(GenePatternAnalysisTask.getJobDir(gpConfig, jobContext, ""+jobId));
            if (!expectedJobDir.getCanonicalPath().equals( jobDir.getCanonicalPath() )) {
                log.error("Invalid arg, jobDir is not in the expected location\n"+
                    "\tjobDir="+jobDir.getCanonicalPath()+"\n"+
                    "\texpectedJobDir="+expectedJobDir.getCanonicalPath());
                return;
            }
        }
        catch (IOException e) {
            log.error("Error validating jobDir="+jobDir.getAbsolutePath(), e);
            return;
        }
        if (!jobDir.exists()) {
            //can happen if deleting a pending job, for which the output dir has not been created
            //create it here so we can output an error message if necessary
            log.error("Invalid arg, jobDir does not exist, jobDir="+jobDir.getAbsolutePath());
            if (!jobDir.mkdirs()) {
                log.error("Error creating jobDir for jobId="+jobId+", jobDir=" + jobDir);
                return;
            }
        }
        if (!jobDir.canWrite()) {
            log.error("Invalid arg, can't write to jobDir, jobDir="+jobDir.getAbsolutePath());
        }
        
        JobInfoWrapper jobInfoWrapper = getJobInfoWrapper(mgr, gpConfig, jobInfo.getUserId(), jobInfo.getJobNumber());
        cleanupInputFiles(jobDir, jobInfoWrapper);
        File taskLog = writeExecutionLog(jobDir, jobInfoWrapper);
        
        boolean checkExitValue = ServerConfigurationFactory.instance().getGPBooleanProperty(jobContext, 
                JobRunner.PROP_ERROR_STATUS_EXIT_VALUE, true);
        boolean checkStderr = ServerConfigurationFactory.instance().getGPBooleanProperty(jobContext, 
                JobRunner.PROP_ERROR_STATUS_STDERR, false);

        log.debug("for job#"+jobId+" checkExitValue="+checkExitValue+", checkStderr="+checkStderr);

        //handle stdout stream
        if (stdoutFile == null) {
            stdoutFile = new File(jobDir, STDOUT);
        }
        if (stdoutFile.exists() && stdoutFile.length() <= 0L) {
            boolean isCustom = !STDOUT.equals(stdoutFile.getName());
            boolean deleteEmptyStdout = ServerConfigurationFactory.instance().getGPBooleanProperty(jobContext, "job.deleteEmptyStdout", false);
            boolean deleteEmptyStdoutDefault = ServerConfigurationFactory.instance().getGPBooleanProperty(jobContext, "job.deleteEmptyStdout.default", true);
            boolean deleteEmptyStdoutCustom = ServerConfigurationFactory.instance().getGPBooleanProperty(jobContext, "job.deleteEmptyStdout.custom", false);
            
            boolean delete = deleteEmptyStdout 
                    || (!isCustom && deleteEmptyStdoutDefault)
                    || (isCustom && deleteEmptyStdoutCustom);
            if (delete) {
                log.debug("deleting empty stdout file for job #"+jobId+", stdout="+stdoutFile.getName());
                boolean deleted = stdoutFile.delete();
                if (!deleted) {
                    log.error("Error deleting empty stdout stream for job #"+jobId+", stdoutFile="+stdoutFile.getAbsolutePath());
                }
            }
        }
        //handle stderr stream
        if (stderrFile == null) {
            stderrFile = new File(jobDir, STDERR);
        }
        if (errorMessage != null && errorMessage.trim().length() > 0) {
            GenePatternAnalysisTask.writeStringToFile(jobDir, STDERR, errorMessage);
        }

        if (stderrFile.exists() && stderrFile.length() <= 0L) {
            boolean deleted = stderrFile.delete();
            if (!deleted) {
                log.error("Error deleting empty stderr stream for job #"+jobId+", stderrFile="+stderrFile.getAbsolutePath());
            }
        }
        
        //process results files
        // any files that are left in outDir are output files
        JobResultsFilenameFilter filenameFilter = new JobResultsFilenameFilter();
        filenameFilter.addExactMatch(stderrFile.getName());
        filenameFilter.addExactMatch(stdoutFile.getName());
        if (taskLog != null) {
            filenameFilter.addExactMatch(taskLog.getName());
        }
        
        Value globPatterns = ServerConfigurationFactory.instance().getValue(jobContext, "job.FilenameFilter");
        if (globPatterns != null) {
            for(String globPattern : globPatterns.getValues()) {
                filenameFilter.addGlob(globPattern);
            }
        }

        List<File> outputFiles = findAllFiles(jobDir, filenameFilter);

        //sort output files by lastModified() date.
        if (outputFiles != null) {
            Collections.sort(outputFiles, fileComparator);
        }

        String jobDirPath = jobDir.getAbsolutePath();
        File ef = new File(jobDirPath + "/" + ExternalFileManager.nonRetrievedFilesFileName);
        JSONArray externalFileList = null;
        
        if (ef.exists()){
            // we have output files that are external and not on the local disk.
            // add them as outputs as if they were present JTL 01/19/2021
            // see AWSBatchJobRunner>>awsFakeSyncDirectory()
           try {
               String externalFileJson = new String(Files.readAllBytes(ef.toPath()));
               externalFileList = new JSONArray(externalFileJson);
               for (int i=0; i<externalFileList.length(); i++){
                   JSONObject obj = externalFileList.getJSONObject(i);
                   String fileName = obj.getString("filename");
                   File aFile = new File(fileName); // need the relative name only, XXX update for sub directories
                   if (filenameFilter.accept(jobDir, aFile.getName())){
                       String fPath = aFile.getAbsolutePath();
                       if (fPath.startsWith(jobDirPath)) {
                           fPath = fPath.substring(jobDirPath.length() + 1); //skip the file separator character
                       }
                       addFileToOutputParameters(jobInfo, fPath, fPath, null);
                   }
               }
               
           } catch (Exception ee){
               log.error(ee);
           }
        }
            
       for (File f : outputFiles) {
            log.debug("adding output file to output parameters " + f.getName() + " from " + jobDirPath);
            //get the file path relative to the outputDir
            String fPath = f.getAbsolutePath();
            if (fPath.startsWith(jobDirPath)) {
                fPath = fPath.substring(jobDirPath.length() + 1); //skip the file separator character
            }
            addFileToOutputParameters(jobInfo, fPath, fPath, null);
        }
        
        int jobStatus = JobStatus.JOB_FINISHED;
        if (checkExitValue) {
            if (exitValue != 0) {
                jobStatus = JobStatus.JOB_ERROR;
            }
        }
        
        if (stderrFile != null && stderrFile.exists() && stderrFile.length() > 0L) {
            addFileToOutputParameters(jobInfo, stderrFile.getName(), stderrFile.getName(), null, false, true);
            if (checkStderr) {
                jobStatus = JobStatus.JOB_ERROR;
            }
        }

        if (stdoutFile != null && stdoutFile.exists() && stdoutFile.length() > 0L) {
            addFileToOutputParameters(jobInfo, stdoutFile.getName(), stdoutFile.getName(), null, true, false);
        }
        
        if (taskLog != null) {
            addFileToOutputParameters(jobInfo, taskLog.getName(), taskLog.getName(), null);
        }

        JobInfo updatedJobInfo;
        try {
            mgr.beginTransaction();
            updatedJobInfo=recordJobCompletion(mgr, jobInfo, jobStatus);
            mgr.commitTransaction();
        }
        catch (Throwable t) {
            log.error("Error recording job completion for job #"+jobInfo.getJobNumber(), t);
            mgr.rollbackTransaction();
            updatedJobInfo=jobInfo;
        }
        finally {
            mgr.closeCurrentSession();
        }
        
        // new api, in a new transaction, just in case of errors
        try {
            JobOutputRecorder.recordOutputFilesToDb(mgr, gpConfig, jobContext, jobDir, externalFileList);
        }
        catch (DbException e) {
            //ignore, error is already logged
        }
        
        //if the job is in a pipeline, notify the pipeline handler
        boolean isInPipeline = jobInfo._getParentJobNumber() >= 0;
        if (isInPipeline) {
            boolean wakeupJobQueue = PipelineHandler.handleJobCompletion(mgr, updatedJobInfo);
            if (wakeupJobQueue) {
                //if the pipeline has more steps, wake up the job queue
                CommandManagerFactory.getCommandManager().wakeupJobQueue();
            }
        }

        // Publish a job completion event for the JobEventBus
        JobRunnerJob jrj=null;
        try {
            jrj=new JobRunnerJobDao().selectJobRunnerJob(mgr, jobInfo.getJobNumber());
        }
        catch (DbException e) {
            //ignore, innner method logs the error
        }
        fireGpJobRecordedEvent(jrj, updatedJobInfo);
    }
    
    protected static void fireGpJobRecordedEvent(final JobRunnerJob jobRunnerJob, final JobInfo jobInfo) {
        String lsid=jobInfo.getTaskLSID();
        Status jobStatus=new Status.Builder()
            .jobInfo(jobInfo)
            .jobStatusRecord(jobRunnerJob)
            .dateCompletedInGp(new Date())
        .build();
        boolean isInPipeline = jobInfo._getParentJobNumber() >= 0;
        JobEventBus.instance().post(new GpJobRecordedEvent(isInPipeline, lsid, jobStatus));
    }

    private static boolean isFinished(JobInfo jobInfo) {
        return isFinished(jobInfo.getStatus());
    }
    
    private static boolean isFinished(String jobStatus) {
        if ( JobStatus.FINISHED.equals(jobStatus) ||
                JobStatus.ERROR.equals(jobStatus) ) {
            return true;
        }
        return false;        
    }

    /**
     * Delete the files from the job results directory which were added before job execution.
     * For example, external urls are downloaded into the job results directory and must be cleaned up before processing the job results.
     * 
     * @param jobDir, the job results directory, e.g. GenePatternServer/Tomcat/webapps/gp/jobResults/<job_no>
     * @param jobInfoWrapper
     * 
     * @deprecated should re-implement this method without use of the JobInfoWrapper class
     */
    private static void cleanupInputFiles(File jobDir, JobInfoWrapper jobInfoWrapper) {
        for (InputFile inputFile : jobInfoWrapper.getInputFiles()) {
            if (inputFile.isUrl()) {
                if (inputFile.isExternalLink()) {
                    log.debug("isExternalLink: "+inputFile.getValue());
                    String path=null;
                    final URL url = inputFile.getUrl();
                    if (url==null) {
                        log.error("can't initialize url for input file: "+inputFile.getValue());
                    }
                    else {
                        try {
                            final URI uri=url.toURI();
                            path=uri.getPath();
                        } 
                        catch (URISyntaxException e) {
                            log.error("can't initialize url for input file: "+inputFile.getValue(), e);
                        }
                        if (path != null) {
                            String filename = path;
                            int idx = path.lastIndexOf('/');
                            if (idx > 0) {
                                ++idx;
                                filename = path.substring(idx);
                            }
                            File inputFileToDelete = new File(jobDir, filename);
                            if (inputFileToDelete.canWrite()) {
                                boolean deleted = inputFileToDelete.delete();
                                log.debug("deleted input file from job results directory, '"+inputFileToDelete.getAbsolutePath()+"', deleted="+deleted);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Write the execution log to the job result directory.
     * 
     * @param jobDir
     * @param jobInfoWrapper
     * 
     * @return the execution log or null if none was written.
     */
    private static File writeExecutionLog(File jobDir, JobInfoWrapper jobInfoWrapper) {
        //write execution log
        File taskLog = null;
        File pipelineTaskLog = null;
        
        if (jobInfoWrapper == null) {
            return null;
        }
        
        if (jobInfoWrapper.isVisualizer()) {
            //no execution log for visualizers
            return null;
        } 
        else if (jobInfoWrapper.isPipeline()) {
            //output pipeline _execution_log only for the root pipeline, exclude nested pipelines
            if (jobInfoWrapper.isRoot()) {
                pipelineTaskLog = JobInfoManager.writePipelineExecutionLog(jobDir, jobInfoWrapper);
            }
            if (pipelineTaskLog != null) {
                pipelineTaskLog.setLastModified(System.currentTimeMillis() + 500);
            }
            return pipelineTaskLog;
        }
        else {
            taskLog = JobInfoManager.writeExecutionLog(jobDir, jobInfoWrapper);
            // touch the taskLog file to make sure it is the oldest/last file
            if (taskLog != null) {
                taskLog.setLastModified(System.currentTimeMillis() + 500);
            }
            return taskLog;
        }
    }

    /**
     * Get the relative path from the given jobResultDir to the given outputFile.
     * This helper method is here to resolve stdout and stderr files passed as fully qualified
     * path names to handleJobCompletion.
     * 
     * Expecting a file relative to the job results directory for the given job,
     *     e.g. /GenePatternServer/Tomcat/webapps/gp/jobResults/<jobid>/stdout.txt
     * If the given outputFile is absolute, return its path relative to the jobResultsDir.
     * 
     * @param jobDir, e.g. /GenePatternServer/Tomcat/webapps/gp/jobResults/<jobid>
     * @param outputFile, e.g. stdout.txt or /GenePatternServer/Tomcat/webapps/gp/jobResults/<jobid>/stdout.txt
     * 
     * @return a relative File or null if the outputFile is not an ancestor of the jobResultDir.
     */
    public static File getRelativePath(File jobDir, File outputFile) {
        File p=outputFile.getParentFile();
        if(p==null) {
            return outputFile;
        }

        String rval = outputFile.getName();
        while(p!=null) {
            if (jobDir.equals(p)) {
                return new File(rval);
            }
            rval = p.getName() + File.separator + rval;
            p = p.getParentFile();
        }
        return null;
    }

    private static List<File> findAllFiles(File root, FilenameFilter filenameFilter) {
        List<File> all = new ArrayList<File>();
        addAllFiles(all, root, filenameFilter);
        return all;
    }

    private static void addAllFiles(List<File> all, File root, FilenameFilter filenameFilter) {
        if (root == null) {
            return;
        }
        if (!root.canRead()) {
            log.error("Can't read file: "+root.getPath());
            return;
        }
        if (root.isFile()) {
            all.add(root);
        }
        else if (root.isDirectory()) {
            File[] files = root.listFiles(filenameFilter);
            for(File f : files) {
                addAllFiles(all,f,filenameFilter);
            }
        }
        else {
            log.error("File is neither file nor directory: "+root.getPath());
        }
    }
    
    private static final Comparator<File> fileComparator = new Comparator<File>() {
        public int compare(File o1, File o2) {
            long f1Date = o1.lastModified();
            long f2Date = o2.lastModified();
            if (f1Date < f2Date) {
                return -1;
            }
            if (f1Date == f2Date) {
                //sort by filename if the timestamps are identical
                return o1.compareTo(o2);
            }
            return 1;
        }
    };
    
    private static JobInfo recordJobCompletion(final HibernateSessionManager mgr, final JobInfo jobInfo, final int jobStatus) {
        if (jobInfo == null) {
            log.error("jobInfo == null, not recording job completion");
            return null;
        }
        long jobStartTime = jobInfo.getDateSubmitted().getTime();
        try {
            long elapsedTime = (System.currentTimeMillis() - jobStartTime) / 1000;
            Date now = new Date(Calendar.getInstance().getTimeInMillis());
            JobInfo updatedJobInfo=updateJobInfo(mgr, jobInfo, jobStatus, now);
            UsageLog.logJobCompletion(jobInfo, now, elapsedTime);
            return updatedJobInfo;
        } 
        catch (Throwable t) {
            log.error(t);
        }
        return null;
    }
    
    private static JobInfo updateJobInfo(final HibernateSessionManager mgr, final JobInfo jobInfo, final int jobStatus, final Date completionDate) {
        if (jobInfo == null) {
            log.error("jobInfo == null");
            return null;
        }

        AnalysisJobDAO home = new AnalysisJobDAO(mgr);
        AnalysisJob aJob = home.findById(jobInfo.getJobNumber());
        aJob.setJobNo(jobInfo.getJobNumber());

        String paramString = jobInfo.getParameterInfo();
        if (jobStatus == JobStatus.JOB_ERROR || jobStatus == JobStatus.JOB_FINISHED || jobStatus == JobStatus.JOB_PROCESSING) {
            paramString = ParameterFormatConverter.stripPasswords(paramString);
        }
        aJob.setParameterInfo(paramString);

        JobStatus newJobStatus = (new JobStatusDAO()).findById(mgr, jobStatus);
        aJob.setJobStatus(newJobStatus);
        aJob.setCompletedDate(completionDate);
        
        mgr.getSession().update(aJob);
        
        return new JobInfo(aJob);
    }

    /**
     * remove special params that should not be added to the command line.
     */
    public static ParameterInfo[] stripOutSpecialParams(ParameterInfo[] inParams) {
	ArrayList<ParameterInfo> strippedParams = new ArrayList<ParameterInfo>();

	if (inParams != null) {
	    for (int i = 0; i < inParams.length; i++) {
		ParameterInfo pi = inParams[i];

		if (pi.getName().equals(PIPELINE_ARG_STOP_AFTER_TASK_NUM))
		    continue;

		strippedParams.add(pi);
	    }
	}
	return strippedParams.toArray(new ParameterInfo[strippedParams.size()]);

    }
    
    public static String getGSDownloadFileName(URLConnection conn, URL url) {
        String baseFilename = getDownloadFileName(conn, url);
        String query = url.getQuery();
        boolean converted = query != null && query.startsWith("dataformat");
        String extension = null;

        if (converted) {
            boolean nextIsIt = false;
            for (String i : query.split("/")) {
                if (nextIsIt) {
                    extension = i; 
                    break;
                }
                if (i.equals("dataformat")) {
                    nextIsIt= true;
                }
            } 
            if (extension == null) return baseFilename;
            
            int dotIndex = baseFilename.lastIndexOf('.');
            if (dotIndex < 0) return baseFilename;
            
            String filename = baseFilename.subSequence(0, dotIndex + 1) + extension;
            return filename;
        }
        else {
            return baseFilename;
        }
    }

    /**
     * Gets a filename that is as similar as possible to the given url
     * 
     * @param conn
     *            The connection
     * @param u
     *            the URL that the connection was created from
     * @return the filename
     */
    public static String getDownloadFileName(URLConnection conn, URL u) {
	try {
	   
	    String contentDis = conn.getHeaderField("Content-Disposition");
	    if (contentDis != null) {
		String[] tokens = contentDis.split(";");
		if (tokens != null) {
		    for (int k = 0, length = tokens.length; k < length; k++) {
			String[] filename = tokens[k].split("=");
			if (filename.length == 2 && (filename[0].equals("file") || filename[0].equals("filename"))) {
			    return filename[1].trim();
			}
		    }
		}
	    }
	} catch (Throwable t) {
	    t.printStackTrace();
	}
	String path = u.getPath();
	try {
	    if (path != null) {
		path = URLDecoder.decode(path, "UTF-8");
	    }
	} catch (UnsupportedEncodingException e) {
	}
	if (path != null && !path.equals("") && path.charAt(path.length() - 1) == '/') {
	    path = path.substring(0, path.length() - 1);
	}
	String value = null;
	if (path != null && ((path.indexOf("getFile.jsp") >= 0))) {
	    String query = u.getQuery();
	    if (query != null && !query.equals("")) {
		try {
		    query = URLDecoder.decode(query, "UTF-8");
		} catch (UnsupportedEncodingException e) {
		}
		String[] tokens = query.split("&");
		for (int i = 0; i < tokens.length; i++) {
		    String[] nameValue = tokens[i].split("=");
		    String name = nameValue[0];
		    if (name.equalsIgnoreCase("file") || name.equalsIgnoreCase("filename")) {
			value = nameValue[1];
			int slashIndex = value.lastIndexOf("/");
			if (slashIndex != -1) {
			    slashIndex = value.lastIndexOf("/");
			    value = value.substring(slashIndex + 1);
			}
			break;
		    }
		}
	    }
	} else if (path != null && !path.equals("")) {
	    int slashIndex = path.lastIndexOf("/");
	    value = slashIndex != -1 ? path.substring(slashIndex + 1) : path;
	}
	if (value == null) {
	    if (path != null && !path.equals("")) {
		int slashIndex = path.lastIndexOf("/");
		value = slashIndex != -1 ? path.substring(slashIndex + 1) : path;
	    } else {
		value = "index";
	    }
	}
	int j = value.indexOf("Axis");
	// strip off the AxisNNNNNaxis_ prefix
	if (j == 0) {
	    value = value.substring(value.indexOf("_") + 1);
	}
	return value;
    }

    private static boolean validateCPU(final GpConfig gpConfig, final String expected) throws JobDispatchException {
        final String actual = GpConfig.getJavaProperty("os.arch");
        // eg. "x86", "i386", "ppc", "alpha", "sparc"
        if (expected.equals("")) {
            return true;
        }
        if (expected.equals(ANY)) {
            return true;
        }
        if (expected.equalsIgnoreCase(actual)) {
            return true;
        }
        String intelEnding = "86"; // x86, i386, i586, etc.
        if (expected.endsWith(intelEnding) && actual.endsWith(intelEnding)) {
            return true;
        }
        if (!Strings.isNullOrEmpty(gpConfig.getDefaultCommandPrefix())) {
            return true; // don't validate for LSF
        }
        throw new JobDispatchException("Cannot run on this platform.  Task requires a " + expected + " CPU, but this is a " + actual);
    }

    private static boolean validateOS(final GpConfig gpConfig, String expected, String action) throws JobDispatchException {
        if (expected == null || expected.trim().length() == 0) {
            return true;
        }
        //split on ';'
        String[] entries = expected.split(";");
        if (entries.length == 1) {
            return validateOSEntry(gpConfig, entries[0], action);
        }
        
        //only need one valid entry
        boolean valid = false;
        for(String entry : entries) {
            try {
                boolean v = validateOSEntry(gpConfig, entry.trim(), action);
                if (v) {
                    valid = true;
                }
            }
            catch (JobDispatchException e) {
                //ignore
            }
        }
        if (!valid) {
            String actual = GpConfig.getJavaProperty("os.name");
            throw new JobDispatchException("Cannot " + action + " on this platform. Task requires on of " + expected
                    + " operating systems, but this server is running " + actual);
        }
        return true;
    }
    
    private static boolean validateOSEntry(final GpConfig gpConfig, final String _expected, final String _action) throws JobDispatchException {
        final String _actual = GpConfig.getJavaProperty("os.name");
        // eg. "Windows XP", "Linux", "Mac OS X", "OSF1"
        final String expected = _expected.toLowerCase();
        final String actual = _actual.toLowerCase();
        
        if (expected.equals("")) {
            return true;
        }
        if (expected.equalsIgnoreCase(ANY)) {
            return true;
        }
        if (expected.equalsIgnoreCase(actual)) {
            return true;
        }
        // Windows XP, Windows ME, Windows 2000, etc.
        String MicrosoftBeginning = "windows"; 
        if (expected.startsWith(MicrosoftBeginning) && actual.startsWith(MicrosoftBeginning)) {
            return true;
        }
        // 'Mac OS X' or 'MacOSX'
        if (expected.startsWith("mac") && actual.startsWith("mac")) {
            return true;
        }
        if (!Strings.isNullOrEmpty(gpConfig.getDefaultCommandPrefix())) {
            return true; // don't validate for LSF 
        }
        throw new JobDispatchException("Cannot " + _action + " on this platform. Task requires a " + _expected
                + " operating system, but this server is running " + actual);
    }
    
    public static String substitute(String commandLine, Properties props, ParameterInfo[] params) {
        //legacy, by default, don't split optional args
        boolean split = false;
        List<String> subbed = substituteSplit(commandLine, props, params, split );
        if (subbed == null) {
            return null;
        }
        StringBuffer rval = new StringBuffer();
        boolean first = true;
        for(String sub : subbed) {
            if (first) {
                first = false;
            }
            else {
                rval.append(" ");
            }
            rval.append(sub);
        }
        return rval.toString();
    }

    /**
     * Performs substitutions of parameters within the commandLine string where there is a &lt;variable&gt; whose
     * substitution value is defined as a key by that name in props. If the parameters is one which has a "prefix", that
     * prefix is prepended to the substitution value as the substitution is made. For example, if the prefix is "-f "
     * and the parameter "/foo/bar" is supplied, the ultimate substitution will be "-f /foo/bar".
     * 
     * @param commandLine
     *            command line with just variable names rather than values
     * @param props
     *            Properties object containing name/value pairs for parameter substitution in the command line
     * @param params
     *            ParameterInfo[] describing whether each parameter has a prefix defined.
     * @return String command line with all substitutions made
     * @author Jim Lerner
     */
    private static List<String> substituteSplit(String commandLine, Properties props, ParameterInfo[] params, boolean split) {
        if (commandLine == null) {
            return null;
        }
        List<String> rval = new ArrayList<String>();
        int start = 0, end = 0, blank;
        String varName = null;
        String replacement = null;
        boolean isOptional = true;
        // create a hashtable of parameters keyed on name for attribute lookup
        Hashtable<String, ParameterInfo> htParams = new Hashtable<String, ParameterInfo>();
        for (int i = 0; params != null && i < params.length; i++) {
            htParams.put(params[i].getName(), params[i]);
        }
        ParameterInfo p = null;
        StringBuffer newString = new StringBuffer(commandLine);
        while (start < newString.length() && (start = newString.toString().indexOf(LEFT_DELIMITER, start)) != -1) {
            start += LEFT_DELIMITER.length();
            int index = start - LEFT_DELIMITER.length() - 1;
            if ((index > 0 && index <= newString.length() && newString.substring(index).startsWith(STDIN_REDIRECT))
                    || commandLine.equals(STDIN_REDIRECT)) {
                continue;
            }
            end = newString.toString().indexOf(RIGHT_DELIMITER, start);
            if (end == -1) {
                log.error("Missing " + RIGHT_DELIMITER + " delimiter in " + commandLine);
                break; // no right delimiter means no substitution
            }
            blank = newString.toString().indexOf(" ", start);
            if (blank != -1 && blank < end) {
                // if there's a space in the name, then it's a redirection of stdin
                start = blank + 1;
                continue;
            }
            varName = newString.substring(start, end);
            replacement = props.getProperty(varName);
            if (replacement == null) {
                // don't sweat inability to substitute for optional parameters.
                // They've already been validated by this point.
                log.info("no substitution available for parameter " + varName);
                // System.out.println(props);
                // replacement = LEFT_DELIMITER + varName + RIGHT_DELIMITER;
                replacement = "";
            }
            if (varName.equals("resources")) {
                // special treatment: make this an absolute path so that pipeline jobs running in their own directories see the right path
                replacement = new File(replacement).getAbsolutePath();
            }
            // special case - make sure GenepatternURL ends with a '/'
            if (varName.equals("GenePatternURL")) {
                // special treatment: make this an absolute path so that pipeline jobs running in their own directories see the right path
                if (!replacement.endsWith("/"))
                    replacement = replacement + "/";
            }
            
            if (replacement.length() == 0) {
                log.debug("GPAT.substitute: replaced " + varName + " with empty string");
            }
            p = htParams.get(varName);
            if (p != null) {
                @SuppressWarnings("rawtypes")
                HashMap hmAttributes = p.getAttributes();
                if (hmAttributes != null) {
                    if (hmAttributes.get(PARAM_INFO_OPTIONAL[PARAM_INFO_NAME_OFFSET]) == null) {
                        isOptional = false;
                    }
                    String optionalPrefix = (String) hmAttributes.get(PARAM_INFO_PREFIX[PARAM_INFO_NAME_OFFSET]);
                    if (replacement.length() > 0 && optionalPrefix != null && optionalPrefix.length() > 0) {
                        if (split) {
                            if (optionalPrefix.endsWith(" ")) {
                                //special-case: GP-2866
                                //    if optionalPrefix ends with a space, split into two args
                                rval.add(optionalPrefix.substring(0, optionalPrefix.length()-1));                                
                                rval.add(replacement);
                            }
                            else {
                                rval.add(optionalPrefix + replacement);
                            } 
                            //HACK: assumes split parameters are passed one at a time
                            return rval;
                        }
                        replacement = optionalPrefix + replacement;
                    }
                }
            }
            newString = newString.replace(start - LEFT_DELIMITER.length(), end + RIGHT_DELIMITER.length(), replacement);
            start = start + replacement.length() - LEFT_DELIMITER.length();
        }
        if (newString.length() == 0 && isOptional) {
            return null;
        }
        rval.add(newString.toString());
        return rval;
    }

    /**
     * Deletes a task, by name, from the Omnigene task_master database.
     * 
     * @param lsid, name of task to delete
     * @author Jim Lerner
     */
    public static void deleteTask(String lsid) throws OmnigeneException, RemoteException {
        String username = null;
        TaskInfo ti = GenePatternAnalysisTask.getTaskInfo(lsid, username);
        File libdir = null;
        try {
            libdir = new File(DirectoryManager.getTaskLibDir(ti.getName(), (String) ti.getTaskInfoAttributes().get(LSID), username));
        } 
        catch (Exception e) {
            // ignore
        }
        GenePatternTaskDBLoader loader = new GenePatternTaskDBLoader(lsid, null, null, null, username, 0);
        int formerID = GenePatternTaskDBLoader.getTaskIDByName(lsid, username);
        loader.run(GenePatternTaskDBLoader.DELETE);
        try {
            // remove taskLib directory for this task
            if (libdir != null) {
                libdir.delete();
            }
            // remove the entry from the cache
            TaskInfoCache.instance().removeFromCache(formerID);
        } 
        catch (Exception ioe) {
            System.err.println(ioe + " while deleting taskLib and search index for task " + ti.getName());
        }
    }

//    public static AnalysisDAO getDS() {
//        return new AnalysisDAO();
//    }

    public static List getTasks(String userID) throws OmnigeneException, RemoteException {
        AdminDAO adminDAO = new AdminDAO();
        TaskInfo[] taskArray = (userID == null ? adminDAO.getAllTasks() : adminDAO.getAllTasksForUser(userID));
        return Arrays.asList(taskArray);
    }

    /**
     * For a given taskName, look up the TaskInfo object in the database and return it to the caller. TODO: involve
     * userID in the search!
     * 
     * @param taskName
     *            name of the task to locate
     * @return TaskInfo complete description of the task (including nested TaskInfoAttributes and ParameterInfo[]).
     * @author Jim Lerner
     * 
     * @deprecated, not sure this plays well with TaskInfoCache
     */
    public static TaskInfo getTaskInfo(String taskName, String username) throws OmnigeneException {
        TaskInfo taskInfo = null;
        try {
            int taskID = -1;
            try {
                if (org.genepattern.util.LSID.isLSID(taskName)) {
                    taskName = new LSID(taskName).toString();
                }

                // search for an existing task with the same name
                taskID = GenePatternTaskDBLoader.getTaskIDByName(taskName, username);
                if (taskID != -1) {
                    taskInfo = (new AdminDAO()).getTask(taskID);
                }
            } 
            catch (OmnigeneException e) {
                // this is a new task, no taskID exists do nothing
                throw new OmnigeneException("no such task: " + taskName + " for user " + username);
            } 
            catch (RemoteException re) {
                throw new OmnigeneException("Unable to load the " + taskName + " task: " + re.getMessage());
            }
        } 
        catch (Exception e) {
            throw new OmnigeneException(e.getMessage() + " in getTaskInfo(" + taskName + ", " + username + ")");
        }
        return taskInfo;
    }

    /**
     * Fill returned Properties with everything that the user can get a substitution for, including all
     * System properties plus all of the actual ParameterInfo name/value pairs.
     * <p/>
     * <p/>
     * Each input file gets additional entries for the directory (INPUT_PATH) the file name (just filename, no path) aka
     * INPUT_FILE, and the base name (no path, no extension), aka INPUT_BASENAME. These are considered helper parameters
     * which can be used in command line substitutions.
     * <p/>
     * <p/>
     * Other properties added to the command line substitution environment are:
     * <ul>
     * <li>NAME (task name)</li>
     * <li>JOB_ID (job number when executing)</li>
     * <li>TASK_ID (task ID number from task_master table)</li>
     * <li>&lt;JAVA&gt; fully qualified filename to Java VM running the GenePatternAnalysis engine</li>
     * <li>LIBDIR directory containing the task's support files (post-fixed by a path separator for convenience of task
     * writer)</li>
     * </ul>
     * <p/>
     * <p/>
     * Called by onJob() to create actual run-time parameter lookup, and by validateInputs() for both task save-time and
     * task run-time parameter validation.
     * <p/>
     * 
     * @param taskName
     *            name of task to be run
     * @param jobNumber
     *            job number of job to be run
     * @param taskID
     *            task ID of job to be run
     * @param taskInfoAttributes
     *            TaskInfoAttributes metadata of job to be run
     * @param actuals
     *            actual parameters to substitute for job to be run
     * @param env
     *            Hashtable of environment variables values
     * @param formalParameters
     *            ParameterInfo[] of formal parameter definitions, used to determine which parameters are input files
     *            (therefore needing additional attributes added to substitution table)
     * @return Properties Properties object with all substitution name/value pairs defined
     * @author Jim Lerner
     * @deprecated
     */
    private static Properties setupProps(TaskInfo taskInfo, String taskName, int parentJobNumber, int jobNumber, int taskID,
        TaskInfoAttributes taskInfoAttributes, ParameterInfo[] actuals, Map<String, String> env,
        ParameterInfo[] formalParameters, String userID) 
    throws MalformedURLException 
    {
        return setupProps(taskInfo, taskName, parentJobNumber, jobNumber, taskID, 
                taskInfoAttributes, actuals, env,
                formalParameters, userID,
                true);  // <--- legacy (pre 3.9.7, copy all properties from System.properties)
}
    private static Properties setupProps(TaskInfo taskInfo, String taskName, int parentJobNumber, int jobNumber, int taskID,
	    TaskInfoAttributes taskInfoAttributes, ParameterInfo[] actuals, Map<String, String> env,
	    ParameterInfo[] formalParameters, String userID, final boolean copyEnv) 
    throws MalformedURLException 
    {
        Properties props = new Properties();
        int formalParamsLength = 0;
        if (formalParameters != null) {
            formalParamsLength = formalParameters.length;
        }
        try {
            if (copyEnv) {
                // copy environment variables into props
                String key = null;
                String value = null;

                for (Enumeration<?> eVariables = System.getProperties().propertyNames(); eVariables.hasMoreElements();) {
                    key = (String) eVariables.nextElement();
                    value = System.getProperty(key, "");
                    props.put(key, value);
                }
                for (Iterator<String> eVariables = env.keySet().iterator(); eVariables.hasNext();) {
                    key = eVariables.next();
                    value = (String) env.get(key);
                    if (value == null) {
                        value = "";
                    }
                    props.put(key, value);
                }
            }
            props.put(NAME, taskName);
            props.put(JOB_ID, Integer.toString(jobNumber));
            props.put("parent_" + JOB_ID, Integer.toString(parentJobNumber));
            props.put(TASK_ID, Integer.toString(taskID));
            props.put(USERID, "" + userID);
            props.put(PIPELINE_ARG_STOP_AFTER_TASK_NUM, ""); // should be overridden by actuals if provided
            String sLSID = taskInfoAttributes.get(LSID);
            props.put(LSID, sLSID);
            // as a convenience to the user, create a <libdir> property which is where DLLs, JARs, EXEs, etc. are dumped to when adding tasks
            String taskLibDir = "taskLibDir";
            if (taskID != -1) {
                taskLibDir = DirectoryManager.getTaskLibDir(taskInfo);
                File f = new File(taskLibDir);
                taskLibDir = f.getPath() + GpConfig.getJavaProperty("file.separator");
            }
            props.put(LIBDIR, taskLibDir);
            
            // explicitly add the '<patches>' substitution parameter
            File pluginDir=ServerConfigurationFactory.instance().getRootPluginDir(GpContext.getServerContext());
            if (pluginDir!=null) {
                props.put(GpConfig.PROP_PLUGIN_DIR, pluginDir.getPath());
            }

            // set the java flags if they have been overridden in the java_flags.properties file
            PropertiesManager_3_2 pm = PropertiesManager_3_2.getInstance();
            Properties javaFlagProps = pm.getJavaFlags();
            String javaFlags = javaFlagProps.getProperty(sLSID);
            if (javaFlags == null) {
                LSID lsid = new LSID(sLSID);
                javaFlags = javaFlagProps.getProperty(lsid.toStringNoVersion());
            }
            if (javaFlags != null) {
                props.put(GPConstants.JAVA_FLAGS, javaFlags);
            }
            // populate props with the input parameters so that they can be looked up by name
            if (actuals != null) {
                for (int i = 0; i < actuals.length; i++) {
                    String value = actuals[i].getValue();
                    if (value == null) {
                        value = "";
                    }
                    props.put(actuals[i].getName(), value);
                }
            }
            String inputFilename = null;
            String inputParamName = null;
            String outDirName = getJobDir(Integer.toString(jobNumber));
            new File(outDirName).mkdirs();
            int j;
            // find input filenames, create _path, _file, and _basename props for each
            boolean isPipeline = taskInfo != null && taskInfo.isPipeline();
            if (actuals != null) { 
                for (int i = 0; i < actuals.length; i++) {
                    for (int f = 0; f < formalParamsLength; f++) {
                        if (actuals[i].getName().equals(formalParameters[f].getName())) {
                            if (formalParameters[f].isInputFile() && !isPipeline) { //don't change parameter values for input files to pipelines
                                inputFilename = actuals[i].getValue();
                                if (inputFilename == null || inputFilename.length() == 0) {
                                    continue;
                                }
                                inputParamName = actuals[i].getName();
                                File inFile = new File(outDirName, new File(inputFilename).getName());
                                String baseName = inFile.getName();
                                if (baseName.startsWith("Axis")) {
                                    // strip off the AxisNNNNNaxis_ prefix
                                    if (baseName.indexOf("_") != -1) {
                                        baseName = baseName.substring(baseName.indexOf("_") + 1);
                                    }
                                }

                                props.put(inputParamName + INPUT_PATH, new String(outDirName));

                                // filename without path
                                props.put(inputParamName + INPUT_FILE, new String(baseName));
                                j = baseName.lastIndexOf(".");
                                if (j != -1) {
                                    props.put(inputParamName + INPUT_EXTENSION, new String(baseName.substring(j + 1))); // filename
                                    // extension
                                    baseName = baseName.substring(0, j);
                                } 
                                else {
                                    props.put(inputParamName + INPUT_EXTENSION, ""); // filename
                                    // extension
                                }
                                if (inputFilename.startsWith("http:") || inputFilename.startsWith("https:")  || inputFilename.startsWith("ftp:") || inputFilename.startsWith("s3:")) {
                                    j = baseName.lastIndexOf("?");
                                    if (j != -1) {
                                        baseName = baseName.substring(j + 1);
                                    }
                                    j = baseName.lastIndexOf("&");
                                    if (j != -1) {
                                        baseName = baseName.substring(j + 1);
                                    }
                                    j = baseName.lastIndexOf("=");
                                    if (j != -1) {
                                        baseName = baseName.substring(j + 1);
                                    }
                                }
                                // filename without path or extension
                                props.put(inputParamName + INPUT_BASENAME, new String(baseName));
                            }
                            break;
                        }
                    }
                }
            }
            return props;
        } 
        catch (NullPointerException npe) {
            log.error(npe + " in setupProps.  Currently have:\n" + props);
            throw npe;
        }
    }

    /**
     * Takes care of quotes in command line. 
     * Ensures that quoted arguments are placed into a single element in the command array.
     * 
     * @param commandLine
     * @return the new command line
     */
    public static String[] translateCommandline(String[] commandLine) {
        if (commandLine == null || commandLine.length == 0) {
            return commandLine;
        }
        List<String> v = new ArrayList<String>();
        int end = commandLine.length;
        int i = 0;
        while (i < end) {
            // read until find another "
            if (commandLine[i] != null && commandLine[i].length()>0 && commandLine[i].charAt(0) == '"' && commandLine[i].charAt(commandLine[i].length() - 1) != '"') {
                StringBuffer buf = new StringBuffer();
                buf.append(commandLine[i].substring(0, commandLine[i].length()));
                i++;
                boolean foundEndQuote = false;
                while (i < end && !foundEndQuote) {
                    foundEndQuote = commandLine[i].charAt(commandLine[i].length() - 1) == '"';
                    buf.append(" ");
                    buf.append(commandLine[i].substring(0, commandLine[i].length()));
                    i++;
                }
                if (!foundEndQuote) {
                    throw new IllegalArgumentException("Missing end quote");
                }
                v.add(buf.toString());
            } 
            else {
                v.add(commandLine[i]);
                i++;
            }
        }
        return (String[]) v.toArray(new String[0]);
    }
    
    /**
     * finds members of the commandLine array that were created using the prefix when specified option
     * and splits them into 2 parts if appropriate. e.g. "--intervals path_to_file" would come in
     * as a single element but really should be 2, "--intervals" and "path_to_file". This method
     * will convert anything matching the regular expression (-[^= ]+) (.+) into 2 elements.
     * 
     * Note: (from pjc), I added the '=' to the regexp so that the hack does not over aggresively split the r_flags for R modules.
     *     -Dr_flags='--no-save --quiet --slave --no-restore'
     *     Should not be split into two args.
     * 
     * See jira GP-2866.
     * 
     * @param commandLine
     * @return
     */
    private String[] hackFixForGP2866(final String[] commandLine) {
        final List<String> fixedCommandLine = new ArrayList<String>();
        //final Pattern pattern = Pattern.compile("(-[^ =]+) (.+)");
        for(final String arg : commandLine) {
            String[] args = splitOptionalParam(arg);
            for(final String item : args) {
                fixedCommandLine.add(item);
            }
        } 
        return fixedCommandLine.toArray(new String[fixedCommandLine.size()]);
    }
    
    private static final Pattern pattern = Pattern.compile("(-[^ =]+) (.+)");
    private static String[] splitOptionalParam(String arg) {
        final Matcher matcher = pattern.matcher(arg);
        if (matcher.matches()) {
            String[] rval = new String[2];
            rval[0] = matcher.group(1);
            rval[1] = matcher.group(2);
            return rval;
        } 
        else {
            String[] rval = new String[1];
            rval[0] = arg;
            return rval;
        }
    }

    /**
     * takes a filename, "short name" of a file, and JobInfo object and adds the descriptor of the file to the JobInfo
     * as an output file.
     * 
     * @param jobInfo
     *            JobInfo object that will hold output file descriptor
     * @param fileName
     *            full name of the file on the server
     * @param label
     *            "short name of the file", ie. the basename without the directory
     * @param parentJobInfo
     *            the parent job of the given jobInfo or <tt>null</tt> if no parent exists
     * @author Jim Lerner
     */
    private static void addFileToOutputParameters(JobInfo jobInfo, String fileName, String label, JobInfo parentJobInfo) {
        boolean isStdout = false;
        boolean isStderr = false;
        addFileToOutputParameters(jobInfo, fileName, label, parentJobInfo, isStdout, isStderr);
    }

    /**
     * Optionally flag the result file as STDOUT or STDERR. This is necessary because we allow for the name of the STDOUT and STDERR files
     * to be customized by each module.
     */
    private static void addFileToOutputParameters(JobInfo jobInfo, String fileName, String label, JobInfo parentJobInfo, boolean isStdout, boolean isStderr) {
        if (jobInfo == null) {
            log.error("null jobInfo arg!");
            return;
        }
        fileName = jobInfo.getJobNumber() + "/" + fileName;
        ParameterInfo paramOut = new ParameterInfo(label, fileName, "");
        paramOut.setAsOutputFile();
        if (isStdout) {
            paramOut._setAsStdoutFile();
        }
        if (isStderr) {
            paramOut._setAsStderrFile();
        }
        
        ParameterInfo[] params =  jobInfo.getParameterInfoArray();
        for (int i=0; i< params.length; i++){
            if (params[i].isOutputFile()){
                if (params[i].getName().equals(label) && params[i].getValue().equals(fileName)){
                    // don't add it again
                    return;
                }
            }
            
        }
        
        jobInfo.addParameterInfo(paramOut);
        if (parentJobInfo != null) {
            parentJobInfo.addParameterInfo(paramOut);
        }
    }

    /**
     * checks that all task parameters are used in the command line and that all parameters that are cited actually
     * exist. Optional parameters need not be cited in the command line. Parameter names that match a list of reserved
     * names are also called out.
     * 
     * @param props
     *            Properties containing environment variables
     * @param taskName
     *            name of task that is being checked. Used in error messages.
     * @param commandLine
     *            command line for task execution prior to parameter substitutions
     * @param actualParams
     *            array of ParameterInfo objects for actual parameter values
     * @param formalParams
     *            array of ParameterInfo objects for formal parameter values (used for optional determination)
     * @param enforceOptionalNonBlank
     *            boolean determining whether to complain if non-optional parameters are not supplied (true for
     *            run-time, false for design-time)
     * @return Vector of error messages (zero length if no problems found)
     * @author Jim Lerner
     */
    private static Vector<String> validateParameters(Properties props, String taskName, String commandLine, ParameterInfo[] actualParams, ParameterInfo[] formalParams, boolean enforceOptionalNonBlank) {
        Vector<String> vProblems = new Vector<String>();
        String name;
        boolean runtimeValidation = (actualParams != formalParams);
        int formalParamsLength = 0;
        if (formalParams != null) {
            formalParamsLength = formalParams.length;
        }
        // validate R-safe task name
        if (!isRSafe(taskName)) {
            vProblems.add("'" + taskName + "' is not a legal task name.  "+
                    "It must contain only letters, digits, and periods, and may not begin with a period or digit.\n It must not be a reserved keyword in R ('if', 'else', 'repeat', 'while', 'function', 'for', 'in', 'next', 'break', 'true', 'false', 'null', 'na', 'inf', 'nan').");
        }
        if (commandLine.trim().length() == 0) {
            vProblems.add("Command line not defined");
        }

        // check that each parameter is cited in either the command line or the
        // output filename pattern
        if (actualParams != null) {
            Vector paramNames = new Vector();
            next_parameter: for (int actual = 0; actual < actualParams.length; actual++) {
                name = LEFT_DELIMITER + actualParams[actual].getName() + RIGHT_DELIMITER;
                if (paramNames.contains(actualParams[actual].getName())) {
                    vProblems.add(taskName + ": " + actualParams[actual].getName()
                            + " has been declared as a parameter more than once");
                }
                paramNames.add(actualParams[actual].getName());
                /*
                 * if (!isRSafe(actualParams[actual].getName())) { vProblems.add(actualParams[actual].getName() + " is
                 * not a legal parameter name. It must contain only letters, digits, and periods, and may not begin with
                 * a period or digit" + " for task " + props.get(GPConstants.LSID)); }
                 */
                for (int j = 0; j < UNREQUIRED_PARAMETER_NAMES.length; j++) {
                    if (name.equals(UNREQUIRED_PARAMETER_NAMES[j])) {
                        continue next_parameter;
                    }
                }
                HashMap hmAttributes = null;
                boolean foundFormal = false;
                int formal;
                for (formal = 0; formal < formalParamsLength; formal++) {
                    if (formalParams[formal].getName().equals(actualParams[actual].getName())) {
                        hmAttributes = formalParams[formal].getAttributes();
                        foundFormal = true;
                        break;
                    }
                }

                if (!foundFormal) {
                    vProblems.add(taskName + ": supplied parameter " + name + " is not part of the definition.");
                    continue;
                }

                // for non-optional parameters, make sure they are mentioned in the command line
                if (hmAttributes == null || hmAttributes.get(PARAM_INFO_OPTIONAL[PARAM_INFO_NAME_OFFSET]) == null
                        || ((String) hmAttributes.get(PARAM_INFO_OPTIONAL[PARAM_INFO_NAME_OFFSET])).length() == 0) {
                    if (commandLine.indexOf(name) == -1) {
                        vProblems.add(taskName + ": non-optional parameter " + name + " is not cited in the command line.");
                    } 
                    else if (enforceOptionalNonBlank
                            && (actualParams[actual].getValue() == null || actualParams[actual].getValue().length() == 0)
                            && formalParams[formal].getValue().length() == 0) {
                        vProblems.add(taskName + ": non-optional parameter " + name + " is blank.");
                    }
                }
                // check that parameter is not named the same as a predefined parameter
                for (int j = 0; j < RESERVED_PARAMETER_NAMES.length; j++) {
                    if (actualParams[actual].getName().equalsIgnoreCase(RESERVED_PARAMETER_NAMES[j])) {
                        vProblems.add(taskName + ": parameter " + name
                                + " is a reserved name and cannot be used as a parameter name.");
                    }
                }

                // if the parameter is part of a choice list, verify that the default is on the list
                String dflt = (String) hmAttributes.get(PARAM_INFO_DEFAULT_VALUE[PARAM_INFO_NAME_OFFSET]);
                String actualValue = actualParams[actual].getValue();
                String choices = formalParams[formal].getValue();
                String[] stChoices = formalParams[formal].getChoices(PARAM_INFO_CHOICE_DELIMITER);
                if (dflt != null && dflt.length() > 0 && formalParams[formal].hasChoices(PARAM_INFO_CHOICE_DELIMITER)) {
                    boolean foundDefault = false;
                    boolean foundActual = false;
                    for (int iChoice = 0; iChoice < stChoices.length; iChoice++) {
                        String entry = stChoices[iChoice];
                        StringTokenizer stChoiceEntry = new StringTokenizer(entry, PARAM_INFO_TYPE_SEPARATOR);
                        String sLHS = "";
                        String sRHS = "";
                        if (stChoiceEntry.hasMoreTokens()) {
                            sLHS = stChoiceEntry.nextToken();
                        }
                        if (stChoiceEntry.hasMoreTokens()) {
                            sRHS = stChoiceEntry.nextToken();
                        }
                        if (sLHS.equals(dflt) || sRHS.equals(dflt)) {
                            foundDefault = true;
                            break;
                        }
                    }
                    if (!foundDefault) {
                        vProblems.add("Default value '" + dflt + "' for parameter " + name
                                + " was not found in the choice list '" + choices + "'.");
                    }
                }

                // check for valid choice selection
                if (runtimeValidation && formalParams[formal].hasChoices(PARAM_INFO_CHOICE_DELIMITER)) {
                    boolean foundActual = false;
                    for (int iChoice = 0; iChoice < stChoices.length; iChoice++) {
                        String entry = stChoices[iChoice];
                        StringTokenizer stChoiceEntry = new StringTokenizer(entry, PARAM_INFO_TYPE_SEPARATOR);
                        String sLHS = "";
                        String sRHS = "";
                        if (stChoiceEntry.hasMoreTokens()) {
                            sLHS = stChoiceEntry.nextToken();
                        }
                        if (stChoiceEntry.hasMoreTokens()) {
                            sRHS = stChoiceEntry.nextToken();
                        }
                        if (sLHS.equals(actualValue) || sRHS.equals(actualValue)) {
                            foundActual = true;
                            break;
                        }
                    }

                    String listMode = hmAttributes != null ? (String)hmAttributes.get(NumValues.PROP_LIST_MODE) : null;
                    //only verify if list mode is not CMD or CMD_OPT
                    if (!foundActual && hmAttributes != null && listMode != null && !listMode.equalsIgnoreCase("cmd") && !listMode.equalsIgnoreCase("cmd_opt")) {
                        if (actualValue != null && actualValue.length() > 0) {
                            // only a problem if it's not an empty string
                            vProblems.add("Value '" + actualValue + "' for parameter " + name + " was not found in the choice list '"
                                + choices + "'.");
                        }
                    }
                }
            }
        }

        vProblems = validateSubstitutions(props, taskName, commandLine, "command line", vProblems, formalParams);
        return vProblems;
    }

    /**
     * checks that each substition variable listed in the task command line actually exists in the ParameterInfo array
     * for the task.
     * 
     * @param props
     *            Properties object containing substitution variable name/value pairs
     * @param taskName
     *            name of task to be validated (used in error messages)
     * @param commandLine
     *            command line to be validated
     * @param source
     *            identifier for what is being checked (command line) for use in error messages
     * @param vProblems
     *            Vector of problems already found, to be appended with new problems and returned from this method
     * @param formalParams
     *            ParameterInfo array of formal parameter definitions (used for optional determination)
     * @return Vector of error messages (vProblems with new errors appended)
     * @author Jim Lerner
     */
    private static Vector<String> validateSubstitutions(Properties props, String taskName, String commandLine, String source, Vector<String> vProblems, ParameterInfo[] formalParams) {
        // check that each substitution variable listed in the command line is actually in props
        int start = 0;
        int end;
        int blank;
        String varName;
        while (start < commandLine.length() && (start = commandLine.indexOf(LEFT_DELIMITER, start)) != -1) {
            end = commandLine.indexOf(RIGHT_DELIMITER, start);
            if (end == -1) {
                break;
            }
            blank = commandLine.indexOf(" ", start) + 1;
            if (blank != 0 && blank < end) {
                // if there's a space in the name, then it's a redirection of
                // stdin
                start = blank;
                continue;
            }
            varName = commandLine.substring(start + LEFT_DELIMITER.length(), end);

            if (!varName.endsWith(INPUT_PATH)) {
                if (!props.containsKey(varName)) {
                    boolean isOptional = false;
                    for (int i = 0; i < formalParams.length; i++) {
                        if (!formalParams[i].getName().equals(varName)) {
                            continue;
                        }
                        HashMap hmAttributes = formalParams[i].getAttributes();
                        if (hmAttributes != null && hmAttributes.get(PARAM_INFO_OPTIONAL[PARAM_INFO_NAME_OFFSET]) != null
                                && ((String) hmAttributes.get(PARAM_INFO_OPTIONAL[PARAM_INFO_NAME_OFFSET])).length() != 0) {
                            isOptional = true;
                        }
                        break;
                    }
                    if (!isOptional) {
                        //check system properties
                        GpContext serverContext = GpContext.getServerContext();
                        String prop = ServerConfigurationFactory.instance().getGPProperty(serverContext, varName);
                        if (prop != null) {
                            //server properties are optional
                            isOptional = true;
                        }
                    }
                    if (!isOptional) {
                        vProblems.add(taskName + ": no substitution available for " + LEFT_DELIMITER + varName + RIGHT_DELIMITER
                                + " in " + source + " " + commandLine + ".");
                    }
                }
            }
            start = end + RIGHT_DELIMITER.length();
        }
        return vProblems;
    }

    /**
     * takes a taskInfoAttributes and ParameterInfo array for a new task and validates that the input parameters are all
     * accounted for. It returns a Vector of error messages to the caller (zero length if all okay).
     * 
     * @param taskName
     *            name of task (used in error messages)
     * @param tia
     *            TaskInfoAttributes (HashMap) containing command line
     * @param params
     *            ParameterInfo array of formal parameter definitions
     * @return Vector of error messages from validation of inputs
     * @author Jim Lerner
     */
    private static Vector<String> validateInputs(TaskInfo taskInfo, String taskName, TaskInfoAttributes tia, ParameterInfo[] params) {
        Vector<String> vProblems = null;
        try {
            Properties props = GenePatternAnalysisTask.setupProps(taskInfo, taskName, -1, 0, -1, tia, params, new HashMap<String, String>(), params, null);
            vProblems = GenePatternAnalysisTask.validateParameters(props, taskName, tia.get(COMMAND_LINE), params, params, false);
        } 
        catch (Exception e) {
            vProblems = new Vector<String>();
            vProblems.add(e.toString() + " while validating inputs for " + tia.get(GPConstants.LSID));
            e.printStackTrace();
        }
        return vProblems;
    }

    /**
     * Determine whether a proposed method or identifier name is a legal identifier. Although there are many possible
     * standards, the R language defines what seems to be both a strict and reasonable definition, and has the added
     * bonus of making R scripts work properly.
     * 
     * Note: referenced by pipelineDesigner.jsp
     * 
     * @param varName, proposed variable name
     * @return boolean if the proposed name is R-legal
     * @author Jim Lerner
     */
    public static boolean isRSafe(String varName) {
        // anything but letters, digits, and period is an invalid R identifier that must be quoted
        String validCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789._";
        String[] reservedNames = new String[] { "if", "else", "repeat", "while", "function", "for", "in", "next", "break",
		"true", "false", "null", "na", "inf", "nan" };
        boolean isReservedName = false;
        for (int i = 0; i < reservedNames.length; i++) {
            if (varName.equals(reservedNames[i])) {
                isReservedName = true;
            }
        }
        StringTokenizer stVarName = new StringTokenizer(varName, validCharacters);
        boolean ret = varName.length() > 0 && // the name is not empty
        stVarName.countTokens() == 0 && // it consists of only letters,
        // digits, and periods
        varName.charAt(0) != '.' && // it doesn't begin with a period
        !Character.isDigit(varName.charAt(0)) && // it doesn't begin
        // with a digit
        !isReservedName; // it isn't a reserved name
        return ret;
    }

    /**
     * encapsulate an invalid R identifier name in quotes if necessary
     * 
     * @param varName, variable name
     * @return variable name, quoted if necessary
     * @author Jim Lerner
     */
    public static String rEncode(String varName) {
        // anything but letters, digits, and period is an invalid R identifier that must be quoted
        if (isRSafe(varName)) {
            return varName;
        } 
        else {
            return "\"" + replace(varName, "\"", "\\\"") + "\"";
        }
    }

    /**
     * marshalls all of the attributes which make up a task (name, description, TaskInfoAttributes, ParameterInfo[]),
     * validates that they will ostensibly work (parameter substitutions all accounted for), and creates a new or
     * updated task database entry (via a DBLoader invocation). If there are validation errors, the task is not created
     * and the error message(s) are returned to the caller. Otherwise (all okay), null is returned.
     * 
     * @param name, task name
     * @param description, description of task
     * @param params, ParameterInfo[] of formal parameters for the task
     * @param taskInfoAttributes, GenePattern TaskInfoAttributes describing metadata for the task
     * 
     * @return Vector of String error messages if there was an error validating the command line and input parameters,
     *         otherwise null to indicate success
     * @throws OmnigeneException, if DBLoader is unhappy when connecting to Omnigene
     * @throws RemoteException, if DBLoader is unhappy when connecting to Omnigene
     * 
     * @author Jim Lerner
     */
    public static Vector<String> installTask(final String name, final String description, final ParameterInfo[] params, final TaskInfoAttributes taskInfoAttributes, final String requestedTaskOwner, final int requestedAccessId, final org.genepattern.server.webservice.server.Status taskIntegrator, final InstallInfo taskInstallInfo)
    throws OmnigeneException, RemoteException 
    {
        final String originalUsername = requestedTaskOwner;
        final TaskInfo taskInfo = new TaskInfo();
        taskInfo.setName(name);
        taskInfo.setDescription(description);
        taskInfo.setUserId(requestedTaskOwner);
        taskInfo.setTaskInfoAttributes(taskInfoAttributes);
        taskInfo.setParameterInfoArray(params);

        try {
            final GpConfig gpConfig=ServerConfigurationFactory.instance();
            final GpContext gpContext=GpContext.getServerContext();
            //validate the OS
            final String os = taskInfoAttributes.get(OS);
            boolean validOs=validateOS(gpConfig, os, "install " + name);
            if (!validOs) {
                Vector<String> v=new Vector<String>();
                v.add("validOs==false, "+OS+"="+os);
                return v;
            }
            //if necessary, install patches
            PluginManagerLegacy pluginManager=new PluginManagerLegacy(org.genepattern.server.database.HibernateUtil.instance(), gpConfig, gpContext);
            pluginManager.validatePatches(taskInfo, taskIntegrator);
            //validate input parameters, must call this after validatePatches because some patches add substitution parameters
            final Vector<String> vProblems=GenePatternAnalysisTask.validateInputs(taskInfo, name, taskInfoAttributes, params);
            if (vProblems != null && vProblems.size()>0) {
                return vProblems;
            }
        }
        catch (Throwable t) {
            if (log.isDebugEnabled()) {
                log.debug("Error while installing " + name, t);
            }
            Vector<String> v=new Vector<String>();
            v.add(t.getLocalizedMessage());
            return v;            
        }

        String lsid = taskInfoAttributes.get(LSID);
        if (lsid == null || lsid.equals("")) {
            // System.out.println("installTask: creating new LSID");
            lsid = LSIDManager.createNewID(TASK_NAMESPACE).toString();
            taskInfoAttributes.put(LSID, lsid);
        }

        final int formerID = GenePatternTaskDBLoader.getTaskIDByName(lsid, originalUsername);
        boolean isNew = (formerID == -1);

        //for new tasks, set the owner and privacy based on the calling method
        // privacy is stored both in the task_master table as a field, and in the taskInfoAttributes
        String taskOwner=requestedTaskOwner;
        int accessId=requestedAccessId;
        taskInfoAttributes.put(PRIVACY, requestedAccessId == ACCESS_PRIVATE ? PRIVATE : PUBLIC);
        if (requestedAccessId == ACCESS_PRIVATE) {
            taskInfoAttributes.put(USERID, requestedTaskOwner);
        }
        if (!isNew) {
            //special-case, GP-3745, when installing task from within a pipeline or suite don't change the permissions
            //    reminder, we could be installing a private pipeline which includes public modules            
            //the following code simplifies by never changing the owner or privacy flag of an existing module
            TaskInfo existingTask=null;
            try {
                HibernateSessionManager mgr=HibernateUtil.instance();
                existingTask=TaskInfoCache.instance().getTask(mgr, formerID);
                final int existingAccessId=existingTask.getAccessId();
                final String existingUserId=existingTask.getUserId();
                final boolean diffAccessId=existingAccessId != requestedAccessId;
                final boolean diffUserId=!requestedTaskOwner.equals(existingUserId);
                // allow the access id to be changed as long as the owner does not change
                if (diffAccessId && diffUserId) {
                    log.debug("Ignoring request to change the 'access_id' for an existing module from "+existingAccessId+" to "+requestedAccessId);
                    accessId=existingAccessId;
                }
                if (diffUserId) {
                    taskOwner=existingUserId;
                    log.debug("Ignoring request to change the owner of an existing module from "+existingUserId+" to "+requestedTaskOwner);
                }
                
                //preserve task owner and access permissions
                // taskInfoAttributes.put(PRIVACY, existingAccessId == ACCESS_PRIVATE ? PRIVATE : PUBLIC);
                // taskInfoAttributes.put(USERID, existingUserId);
                taskInfoAttributes.put(PRIVACY, accessId == ACCESS_PRIVATE ? PRIVATE : PUBLIC);
                taskInfoAttributes.put(USERID, taskOwner);
                
                
                
            }
            catch (TaskIDNotFoundException e) {
                log.debug("taskIDNotFound: "+formerID);
            }
            catch (Throwable t) {
                log.error("Unexpected error loading TaskInfo from cache, formerID="+formerID, t);
            }
            //remove the previous entry from the cache
            TaskInfoCache.instance().removeFromCache(formerID);
        }
        // TODO: if the task is a pipeline, generate the serialized model right now too
        GenePatternTaskDBLoader loader = new GenePatternTaskDBLoader(name, description, params, taskInfoAttributes.encode(), taskOwner, accessId);
        loader.setInstallInfo(taskInstallInfo);
        loader.run(isNew ? GenePatternTaskDBLoader.CREATE : GenePatternTaskDBLoader.UPDATE);
        return null;
    }

    public static String installNewTask(String name, String description, ParameterInfo[] params, TaskInfoAttributes taskInfoAttributes, String username, int access_id, final LsidVersion.Increment versionIncrement, org.genepattern.server.webservice.server.Status taskIntegrator, InstallInfo installInfo)
    throws OmnigeneException, RemoteException, TaskInstallationException {
        LSID taskLSID = null;
        String requestedLSID = taskInfoAttributes.get(LSID);
        taskLSID = LSIDManager.getNextTaskLsid(requestedLSID, versionIncrement,username);
        taskInfoAttributes.put(GPConstants.LSID, taskLSID.toString());
        Vector probs = installTask(name, description, params, taskInfoAttributes, username, access_id, taskIntegrator, installInfo);
        if ((probs != null) && (probs.size() > 0)) {
            throw new TaskInstallationException(probs);
        }
        return taskLSID.toString();
    }

    public static boolean taskExists(String taskName, String user) throws OmnigeneException {
        TaskInfo existingTaskInfo = null;
        try {
            existingTaskInfo = GenePatternAnalysisTask.getTaskInfo(taskName, user);
        } 
        catch (OmnigeneException oe) {
            // ignore
        }
        return (existingTaskInfo != null);
    }

    /**
     * takes a job number and returns the directory where output files from that job are/will be stored. <b>This routine
     * depends on having the System property java.io.tmpdir set the same for both the Tomcat and JBoss instantiations.
     * </b>
     * 
     * @param jobNumber, the job number whose storage directory is being sought
     * @return String directory name on server of this job's files
     * @author Jim Lerner
     * 
     * @deprecated - should pass in a valid GpConfig and GpContext
     */
    public static String getJobDir(final String jobNumber) {
        GpConfig gpConfig = ServerConfigurationFactory.instance();
        GpContext context = GpContext.getServerContext();
        return getJobDir(gpConfig, context, jobNumber);
    }

    public static String getJobDir(final GpConfig gpConfig, final GpContext jobContext, final String jobNumber) {
        try {
            File rootJobDir = gpConfig.getRootJobDir(jobContext);
            String tmpDir = rootJobDir.getPath();
            if (!tmpDir.endsWith(File.separator)) {
                tmpDir = tmpDir + "/";
            }
            tmpDir = tmpDir + jobNumber;
            return tmpDir;
        }
        catch (Exception e) {
            log.error(e);
        }
        
        return "../jobs/"+jobNumber;
    }

    // zip file support:

    /**
     * inspects a GenePattern-packaged task in a zip file and returns the name of the task contained therein
     * 
     * @param zipFilename
     *            filename of zip file containing a GenePattern task
     * @return name of task in zip file
     * @throws IOException
     *             if an error occurs opening the zip file (eg. file not found)
     * @author Jim Lerner
     */
    public static String getTaskNameFromZipFile(String zipFilename) throws IOException {
	Properties props = getPropsFromZipFile(zipFilename);
	return props.getProperty(NAME);
    }

    /**
     * opens a GenePattern-packaged task and returns a Properties object containing all of the TaskInfo,
     * TaskInfoAttributes, and ParameterInfo[] data for the task.
     * 
     * @param zipFilename
     *            filename of the GenePattern task zip file
     * @return Properties object containing key/value pairs for all of the TaskInfo, TaskInfoAttributes, and
     *         ParameterInfo[]
     * @throws IOException
     *             if an error occurs opening the zip file
     * @author Jim Lerner
     */
    public static Properties getPropsFromZipFile(String zipFilename) throws IOException {
	if (!zipFilename.endsWith(".zip")) {
	    throw new IOException(zipFilename + " is not a zip file");
	}
	ZipFile zipFile = new ZipFile(zipFilename);
	ZipEntry manifestEntry = zipFile.getEntry(GPConstants.MANIFEST_FILENAME);
	if (manifestEntry == null) {
	    zipFile.close();
	    throw new IOException(zipFilename
		    + " is missing a GenePattern manifest file.  It probably isn't a GenePattern task package.");
	}
	Properties props = new Properties();
	try {
	    props.load(zipFile.getInputStream(manifestEntry));
	} catch (IOException ioe) {
	    throw new IOException(zipFilename + " is probably not a GenePattern zip file.  The manifest file cannot be loaded.  "
		    + ioe.getMessage());
	} finally {
	    zipFile.close();
	}
	return props;
    }

    /**
     * opens a GenePattern-packaged task in the form of a remote URL and returns a Properties object containing all of
     * the TaskInfo, TaskInfoAttributes, and ParameterInfo[] data for the task.
     * 
     * @param zipURL
     *            URL of the GenePattern task zip file
     * @return Properties object containing key/value pairs for all of the TaskInfo, TaskInfoAttributes, and
     *         ParameterInfo[]
     * @throws Exception
     *             if an error occurs accessing the URL (no such host, no such URL, not a zip file, etc.)
     * @author Jim Lerner
     */
    public static Properties getPropsFromZipURL(String zipURL) throws Exception {
	try {
	    URL url = new URL(zipURL);
	    URLConnection conn = url.openConnection();
	    if (conn == null) {
		log.error("null conn in getPropsFromZipURL");
	    }
	    InputStream is = conn.getInputStream();
	    if (is == null) {
		log.error("null is in getPropsFromZipURL");
	    }
	    ZipInputStream zis = new ZipInputStream(is);
	    ZipEntry zipEntry = null;
	    Properties props = null;
	    while (true) {
		try {
		    zipEntry = zis.getNextEntry();
		    if (zipEntry == null) {
			break;
		    }
		} catch (ZipException ze) {
		    break; // EOF
		}
		if (zipEntry.getName().equals(GPConstants.MANIFEST_FILENAME)) {
		    long manifestSize = zipEntry.getSize();
		    if (manifestSize == -1) {
			manifestSize = 10000;
		    }
		    byte b[] = new byte[(int) manifestSize];
		    int numRead = zis.read(b, 0, (int) manifestSize);
		    props = new Properties();
		    props.load(new ByteArrayInputStream(b, 0, numRead));
		    props.setProperty("size", "" + conn.getContentLength());
		    props.setProperty("created", "" + conn.getLastModified());
		    break;
		}
	    }
	    zis.close();
	    is.close();
	    return props;
	} catch (Exception e) {
	    log.error(e + " in getPropsFromZipURL while reading " + zipURL);
	    throw e;
	}
    }

    /**
     * @deprecated - should pass in a TaskInstallInfo arg
     */
    private static String installNewTask(final String zipFilename, final String username, final int access_id, final boolean recursive, final org.genepattern.server.webservice.server.Status taskIntegrator) 
    throws TaskInstallationException 
    {
        return installNewTask(zipFilename, username, access_id, recursive, taskIntegrator, null);
    }

    /**
     * accepts the filename of a GenePattern-packaged task in the form of a zip file, unpacks it, and installs the task
     * in the Omnigene task database. Any taskLib entries (files such as scripts, DLLs, properties, etc.) from the zip
     * file are installed in the appropriate taskLib directory.
     * 
     * @param zipFilename
     *            filename of zip file containing task to install
     * @return Vector of String error messages if unsuccessful, null if okay
     * @author Jim Lerner
     * @see #installTask
     */
    public static String installNewTask(final String zipFilename, final String username, final int access_id, final boolean recursive, final org.genepattern.server.webservice.server.Status taskIntegrator, final InstallInfo installInfo) 
    throws TaskInstallationException 
    {
        Vector<String> vProblems = new Vector<String>();
        int i;
        ZipFile zipFile = null;
        InputStream is = null;
        File outFile = null;
        FileOutputStream os = null;
        String taskName = zipFilename;
        String lsid = null;
        try {
            String name;
            try {
                zipFile = new ZipFile(zipFilename);
            } 
            catch (IOException ioe) {
                throw new Exception("Couldn't open " + zipFilename + ": " + ioe.getMessage());
            }
            ZipEntry manifestEntry = zipFile.getEntry(MANIFEST_FILENAME);
            ZipEntry zipEntry = null;
            long fileLength = 0;
            int numRead = 0;
            if (manifestEntry == null) {
                // is it a zip of zips?
                for (Enumeration eEntries = zipFile.entries(); eEntries.hasMoreElements();) {
                    zipEntry = (ZipEntry) eEntries.nextElement();
                    if (zipEntry.getName().endsWith(".zip")) {
                        continue;
                    }
                    throw new Exception(MANIFEST_FILENAME + " file not found in " + zipFilename);
                }
                // if we get here, the zip file contains only other zip files recursively install them
                String firstLSID = null;
                final File tempDir=ServerConfigurationFactory.instance().getTempDir(GpContext.getServerContext());
                for (Enumeration eEntries = zipFile.entries(); eEntries.hasMoreElements();) {
                    zipEntry = (ZipEntry) eEntries.nextElement();
                    is = zipFile.getInputStream(zipEntry);
                    outFile = new File(tempDir, zipEntry.getName());
                    outFile.deleteOnExit();
                    os = new FileOutputStream(outFile);
                    fileLength = zipEntry.getSize();
                    numRead = 0;
                    byte[] buf = new byte[100000];
                    while ((i = is.read(buf, 0, buf.length)) > 0) {
                        os.write(buf, 0, i);
                        numRead += i;
                    }
                    os.close();
                    os = null;
                    outFile.setLastModified(zipEntry.getTime());
                    if (numRead != fileLength) {
                        vProblems.add("only read " + numRead + " of " + fileLength + " bytes in " + zipFilename + "'s " + zipEntry.getName());
                    }
                    is.close();
                    log.info("installing " + outFile.getAbsolutePath());
                    
                    InstallInfo installInfoEntry = new InstallInfo(InstallInfo.Type.ZIP);
                    lsid = installNewTask(outFile.getAbsolutePath(), username, access_id, taskIntegrator, installInfoEntry);
                    log.info("installed " + lsid);
                    if (firstLSID == null) {
                        firstLSID = lsid;
                    }
                    outFile.delete();
                    if (!recursive) {
                        break; // only install the top level (first entry)
                    }
                }
                TaskInfoCache.instance().removeFromCache(HibernateUtil.instance(), firstLSID);
                return firstLSID;
            }

            // TODO: see TaskUtil.getTaskInfoFromManifest, may be able to get rid of duplicate code
            Properties props = new Properties();
            props.load(zipFile.getInputStream(manifestEntry));

            taskName = (String) props.remove(NAME);
            lsid = (String) props.get(LSID);
            LSID l = new LSID(lsid); 
            if (taskName == null || taskName.length() == 0) {
                // ; throw MalformedURLException if this is a bad LSID
                vProblems.add("Missing task name in manifest in " + new File(zipFilename).getName());
                throw new TaskInstallationException(vProblems); // abandon ship!
            }

            String taskDescription = (String) props.remove(DESCRIPTION);
            // ParameterInfo entries consist of name/value/description triplets,
            // of which the value and description are optional
            // It is assumed that the names are p[1-n]_name, p[1-n]_value, and p[1-n]_description
            // and that the numbering runs consecutively. 
            // When there is no p[m]_name value, then there are m-1 ParameterInfos
            String value;
            String description;
            Vector vParams = new Vector();
            ParameterInfo pi = null;
            boolean found = true;
            for (i = 1; found; i++) { // loop until we don't find p_i_name
                name = (String) props.remove("p" + i + "_name");
                if (name == null) {
                    found = false;
                    continue;
                }
                if (name == null || name.length() == 0) {
                    throw new Exception("missing parameter name for " + "p" + i + "_name");
                }
                value = (String) props.remove("p" + i + "_value");
                if (value == null) {
                    value = "";
                }
                description = (String) props.remove("p" + i + "_description");
                if (description == null) {
                    description = "";
                }
                pi = new ParameterInfo(name, value, description);
                HashMap attributes = new HashMap();
                for (int attribute = 0; attribute < PARAM_INFO_ATTRIBUTES.length; attribute++) {
                    name = (String) PARAM_INFO_ATTRIBUTES[attribute][0];
                    value = (String) props.remove("p" + i + "_" + name);
                    if (value != null) {
                        attributes.put(name, value);
                    }
                    if (name.equals(PARAM_INFO_TYPE[0]) && value != null && value.equals(PARAM_INFO_TYPE_INPUT_FILE)) {
                        attributes.put(ParameterInfo.MODE, ParameterInfo.INPUT_MODE);
                        attributes.put(ParameterInfo.TYPE, ParameterInfo.FILE_TYPE);
                    }
                }
                for (Enumeration p = props.propertyNames(); p.hasMoreElements();) {
                    name = (String) p.nextElement();
                    if (!name.startsWith("p" + i + "_")) {
                        continue;
                    }
                    value = (String) props.remove(name);
                    // _cat.debug("installTask: " + taskName + ": parameter " + name + "=" + value);
                    name = name.substring(name.indexOf("_") + 1);
                    attributes.put(name, value);
                }
                if (attributes.size() > 0) {
                    pi.setAttributes(attributes);
                }
                vParams.add(pi);
            }
            ParameterInfo[] params = new ParameterInfo[vParams.size()];
            vParams.copyInto(params);

            // if it's a pipeline, generate the commandLine (bug 2105)
            String taskType = props.getProperty(GPConstants.TASK_TYPE);
            //TODO: no longer need special-case pipeline command line
            if (taskType.toLowerCase().endsWith("pipeline")) {
                // replace command line with generated command line for pipelines
                String serializedModel = props.getProperty("serializedModel");
                PipelineModel model = PipelineModel.toPipelineModel(serializedModel);
                String commandLine = AbstractPipelineCodeGenerator.generateCommandLine(model);
                props.setProperty(GPConstants.COMMAND_LINE, commandLine);
            }

            // all remaining properties are assumed to be TaskInfoAttributes
            TaskInfoAttributes tia = new TaskInfoAttributes();
            for (Enumeration eProps = props.propertyNames(); eProps.hasMoreElements();) {
                name = (String) eProps.nextElement();
                value = props.getProperty(name);
                tia.put(name, value);
            }

            // System.out.println("installTask (zip): username=" + username + ",
            // access_id=" + access_id + ", tia.owner=" + tia.get(USERID) + ",
            // tia.privacy=" + tia.get(PRIVACY));
            if (vProblems.size() == 0) {
                log.info("installing " + taskName + " into database");
                vProblems = GenePatternAnalysisTask.installTask(taskName, taskDescription, params, tia, username, access_id, taskIntegrator, installInfo);
                if (vProblems == null) {
                    vProblems = new Vector<String>();
                }
                if (vProblems.size() == 0) {
                    // get the newly assigned LSID
                    lsid = (String) tia.get(GPConstants.LSID);

                    // extract files from zip file
                    String taskDir = DirectoryManager.getTaskLibDir(null, (String) tia.get(GPConstants.LSID), username);
                    File dir = new File(taskDir);

                    // if there are any existing files from a previous installation of this task,
                    // clean them out so there is no interference
                    File[] fileList = dir.listFiles();
                    for (i = 0; i < fileList.length; i++) {
                        fileList[i].delete();
                    }

                    // XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
                    String folder = null;
                    for (Enumeration eEntries = zipFile.entries(); eEntries.hasMoreElements();) {
                        zipEntry = (ZipEntry) eEntries.nextElement();
                        if (zipEntry.getName().equals(MANIFEST_FILENAME)) {
                            continue;
                        }
                        is = zipFile.getInputStream(zipEntry);
                        name = zipEntry.getName();
                        if (zipEntry.isDirectory() || name.indexOf("/") != -1 || name.indexOf("\\") != -1) {
                            // TODO: mkdirs()
                            log.warn("installTask: skipping hierarchically-entered name: " + name);
                            continue;
                        }

                        // copy attachments to the taskLib BEFORE installing the task, 
                        // so that there is no time window when the task is installed in Omnigene's database 
                        // but the files aren't decoded and so the task can't yet be properly invoked

                        // TODO: allow names to have paths, 
                        // so long as they are below the current point and not above or a peer
                        // strip absolute or ../relative path names from zip entry name so that they dump into the tasklib directory only
                        i = name.lastIndexOf("/");
                        if (i != -1) {
                            name = name.substring(i + 1);
                        }
                        i = name.lastIndexOf("\\");
                        if (i != -1) {
                            name = name.substring(i + 1);
                        }
                        //try {
                        // TODO: support directory structure within zip file
                        outFile = new File(taskDir, name);
                        if (outFile.exists()) {
                            File oldVersion = new File(taskDir, name + ".old");
                            log.warn("replacing " + name + " (" + outFile.length() + " bytes) in " + taskDir
                                    + ".  Renaming old one to " + oldVersion.getName());
                            oldVersion.delete(); 
                            // delete the previous .old file
                            boolean renamed = rename(outFile, oldVersion, true);
                            if (!renamed) {
                                log.error("failed to rename " + outFile.getAbsolutePath() + " to " + oldVersion.getAbsolutePath());
                            }
                        }
                        is.close();
                        if (os != null) {
                            os.close();
                            os = null;
                        }
                    }

                    // unzip using ants classes to allow file permissions to be retained
                    boolean useAntUnzip = true;
                    if (!GpConfig.getJavaProperty("os.name").toLowerCase().startsWith("windows")) {
                        useAntUnzip = false;
                        Execute execute = new Execute();
                        execute.setCommandline(new String[] { "unzip", zipFilename, "-d", taskDir });
                        try {
                            int result = execute.execute();
                            if (result != 0) {
                                useAntUnzip = true;
                            }
                        } 
                        catch (IOException ioe) {
                            log.error(ioe);
                            useAntUnzip = true;
                        }
                    }
                    if (useAntUnzip) {
                        Expander expander = new Expander();
                        expander.setSrc(new File(zipFilename));
                        expander.setDest(new File(taskDir));
                        expander.execute();
                    }

                    // XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
                }
            }
        } 
        catch (Exception e) {
            log.error(e);
            e.printStackTrace();
            vProblems.add(e.getMessage() + " while installing task");
        } 
        finally {
            try {
                if (zipFile != null) {
                    zipFile.close();
                }
            } 
            catch (IOException ioe) {
            }
        }
        if ((vProblems != null) && (vProblems.size() > 0)) {
            for (Enumeration<String> eProblems = vProblems.elements(); eProblems.hasMoreElements();) {
                log.error(eProblems.nextElement());
            }
            throw new TaskInstallationException(vProblems);
        }
        TaskInfoCache.instance().removeFromCache(HibernateUtil.instance(), lsid);
        return lsid;
    }

    public static String installNewTaskFromRepository(final URL repositoryUrl, final String zipUrl, final String zipFilename, final String username, final int access_id, final org.genepattern.server.webservice.server.Status taskIntegrator) 
    throws TaskInstallationException
    {
        log.debug("Installing task from repository");
        log.debug("repositoryUrl="+repositoryUrl);
        log.debug("zipUrl="+zipUrl);
        log.debug("zipFilename="+zipFilename);
        log.debug("username="+username);
        log.debug("access_id="+access_id);
        
        InstallInfo taskInstallInfo = new InstallInfo(Type.REPOSITORY);
        taskInstallInfo.setRepositoryUrl(repositoryUrl);

        String rval= installNewTask(zipFilename, username, access_id, true, taskIntegrator, taskInstallInfo);
        
        //TODO: record 'task_install_info' to DB
        return rval;
    }

    public static String installNewTask(String zipFilename, String username, int access_id, org.genepattern.server.webservice.server.Status taskIntegrator, InstallInfo installInfo)
        throws TaskInstallationException {
        return installNewTask(zipFilename, username, access_id, true, taskIntegrator, installInfo);
    }

    public static final String downloadTask(final String zipURL) throws IOException {
        return FileDownloader.downloadTask(zipURL, null);
    }

    /**
     * downloads a file from a URL and returns the path to the local file to the caller.
     * 
     * @param zipURL String URL of file to download
     * @param statusMonitor
     * @param expectedLength
     * @param verbose
     * @return String filename of temporary downloaded file on server
     * @throws IOException
     */
    public static String downloadTask(final String zipURL, final org.genepattern.server.webservice.server.Status statusMonitor)
            throws IOException {
        return FileDownloader.downloadTask(zipURL, statusMonitor);
    }

    /**
     * returns a Vector of TaskInfos of the contents of zip-of-zips file. The 0th index of the returned vector holds the
     * TaskInfo for the pipeline itself. Note that the returned <code>TaskInfo</code> instances have getID() equal to
     * -1, getParameterInfo() will be <code>null</code>, getUserId is <code>null</code>, and getAccessId is 0.
     * 
     * @throws IOException
     */
    public static Vector getZipOfZipsTaskInfos(File zipf) throws IOException {
	Vector vTaskInfos = new Vector();
	ZipFile zipFile = null;
	try {
	    zipFile = new ZipFile(zipf);
	    for (Enumeration eEntries = zipFile.entries(); eEntries.hasMoreElements();) {
		ZipEntry zipEntry = (ZipEntry) eEntries.nextElement();
		if (!zipEntry.getName().endsWith(".zip")) {
		    throw new IllegalArgumentException("not a GenePattern zip-of-zips file");
		}
		InputStream is = null;
		File subFile = null;
		OutputStream os = null;
		try {
		    is = zipFile.getInputStream(zipEntry);
		    // there is no way to create a ZipFile from an input stream,
		    // so
		    // every file within the
		    // stream must be extracted before it can be processed
		    subFile = File.createTempFile("sub", ".zip");
		    os = new FileOutputStream(subFile);
		    byte[] buf = new byte[100000];
		    int bytesRead;
		    while ((bytesRead = is.read(buf, 0, buf.length)) >= 0) {
			os.write(buf, 0, bytesRead);
		    }
		    os.close();
		    Properties props = new Properties();
		    ZipFile subZipFile = new ZipFile(subFile);
		    ZipEntry manifestEntry = subZipFile.getEntry(GPConstants.MANIFEST_FILENAME);
		    props.load(subZipFile.getInputStream(manifestEntry));
		    subZipFile.close();
		    subFile.delete();
		    TaskInfo ti = new TaskInfo();
		    ti.setName((String) props.remove(NAME));
		    ti.setDescription((String) props.remove(DESCRIPTION));

		    // ParameterInfo entries consist of name/value/description
		    // triplets,
		    // of which the value and description are optional
		    // It is assumed that the names are p[1-n]_name,
		    // p[1-n]_value,
		    // and
		    // p[1-n]_description
		    // and that the numbering runs consecutively. When there is
		    // no
		    // p[m]_name value, then there are m-1 ParameterInfos

		    // count ParameterInfo entries
		    String name;
		    String value;
		    String description;
		    Vector vParams = new Vector();
		    ParameterInfo pi = null;
		    for (int i = 1; i <= MAX_PARAMETERS; i++) {
			name = (String) props.remove("p" + i + "_name");
			if (name == null) {
			    continue;
			}
			if (name == null || name.length() == 0) {
			    throw new IOException("missing parameter name for " + "p" + i + "_name");
			}
			value = (String) props.remove("p" + i + "_value");
			if (value == null) {
			    value = "";
			}
			description = (String) props.remove("p" + i + "_description");
			if (description == null) {
			    description = "";
			}
			pi = new ParameterInfo(name, value, description);
			HashMap attributes = new HashMap();
			for (int attribute = 0; attribute < PARAM_INFO_ATTRIBUTES.length; attribute++) {
			    name = (String) PARAM_INFO_ATTRIBUTES[attribute][0];
			    value = (String) props.remove("p" + i + "_" + name);
			    if (value != null) {
				attributes.put(name, value);
			    }
			    if (name.equals(PARAM_INFO_TYPE[0]) && value != null && value.equals(PARAM_INFO_TYPE_INPUT_FILE)) {
				attributes.put(ParameterInfo.MODE, ParameterInfo.INPUT_MODE);
				attributes.put(ParameterInfo.TYPE, ParameterInfo.FILE_TYPE);
			    }
			}
			for (Enumeration p = props.propertyNames(); p.hasMoreElements();) {
			    name = (String) p.nextElement();
			    if (!name.startsWith("p" + i + "_")) {
				continue;
			    }
			    value = (String) props.remove(name);
			    name = name.substring(name.indexOf("_") + 1);
			    attributes.put(name, value);
			}
			if (attributes.size() > 0) {
			    pi.setAttributes(attributes);
			}
			vParams.add(pi);
		    }
		    ParameterInfo[] params = new ParameterInfo[vParams.size()];
		    ti.setParameterInfoArray((ParameterInfo[]) vParams.toArray(new ParameterInfo[0]));

		    // all remaining properties are assumed to be
		    // TaskInfoAttributes
		    TaskInfoAttributes tia = new TaskInfoAttributes();
		    for (Enumeration eProps = props.propertyNames(); eProps.hasMoreElements();) {
			name = (String) eProps.nextElement();
			value = props.getProperty(name);
			tia.put(name, value);
		    }
		    ti.setTaskInfoAttributes(tia);
		    vTaskInfos.add(ti);
		} finally {
		    if (is != null) {
			is.close();
		    }
		    if (os != null) {
			os.close();
		    }
		}
	    }
	} finally {
	    zipFile.close();
	}
	return vTaskInfos;
    }

    // utility methods:

    /**
     * writes a string to a file
     * 
     * @param dirName
     *            directory in which to create the file
     * @param filename
     *            filename within the directory
     * @param outputString
     *            String to write to file
     * @return File that was written
     * @author Jim Lerner
     */
    public static File writeStringToFile(File parentDir, String filename, String outputString) {
        File outFile = null;
        try {
            outFile = new File(parentDir, filename);
            FileWriter fw = new FileWriter(outFile, true);
            fw.write(outputString != null ? outputString : "");
            fw.close();
        } 
        catch (NullPointerException npe) {
            log.error("writeStringToFile(" + parentDir + ", " + filename + ", " + outputString + "): " + npe.getMessage(), npe);
        } 
        catch (IOException ioe) {
            log.error("writeStringToFile(" + parentDir + ", " + filename + ", " + outputString + "): " + ioe.getMessage(), ioe);
        } 
        finally {
            if (true) {
                return outFile;
            }
        }
        return outFile;
    }

    /**
     * replace all instances of "find" in "original" string and substitute "replace" for them
     * 
     * @param original
     *            String before replacements are made
     * @param find
     *            String to search for
     * @param replace
     *            String to replace the sought string with
     * @return String String with all replacements made
     * @author Jim Lerner
     */
    public static final String replace(String original, String find, String replace) {
	StringBuffer res = new StringBuffer();
	int idx = 0;
	int i = 0;
	while (true) {
	    i = idx;
	    idx = original.indexOf(find, idx);
	    if (idx == -1) {
		res.append(original.substring(i));
		break;
	    } else {
		res.append(original.substring(i, idx));
		res.append(replace);
		idx += find.length();
	    }
	}
	return res.toString();
    }

    /**
     * renames a file, even across filesystems. If the underlying Java rename() fails because the source and destination
     * are not on the same filesystem, this method performs a copy instead.
     * 
     * @param from
     *            File which is to be renamed
     * @param to
     *            File which will be the new name
     * @param deleteIfCopied
     *            boolean indicating whether to delete the source file if it was copied to a different filesystem
     * @return true if the rename was accomplished
     * @author Jim Lerner
     */

    public static boolean rename(File from, File to, boolean deleteIfCopied) {
	if (!from.exists()) {
	    log.error(from.toString() + " doesn't exist for rename");
	    return false;
	}
	if (!to.getParentFile().exists()) {
	    log.info(to.getParent() + " directory does not exist");
	    to.getParentFile().mkdirs();
	}
	if (from.equals(to)) {
	    return true;
	}
	if (to.exists()) {
	    log.info(to.toString() + " already exists for rename.");
	    if (!from.equals(to)) {
		to.delete();
	    }
	}
	for (int retries = 1; retries < 20; retries++) {
	    if (from.renameTo(to)) {
		return true;
	    }
	    // sleep and retry in case Indexer is busy with this file right now
	    try {
		Thread.sleep(100 * retries);
	    } catch (InterruptedException ie) {
	    }
	}
	log.info("Have to copy, rename failed: " + from.getAbsolutePath() + " -> " + to.getAbsolutePath());
	// if can't rename, then copy to destination and delete original
	if (copyFile(from, to)) {
	    if (deleteIfCopied) {
		if (!from.delete()) {
		    log.info("Unable to delete source of copy/rename: " + from.toString());
		}
	    }
	    return true;
	} else {
	    return false;
	}
    }

    public static boolean copyFile(File from, File to) {
	if (!from.exists()) {
	    log.error(from + " doesn't exist to copy.");
	    return false;
	}
	if (to.exists()) {
	    to.delete();
	}
	FileInputStream is = null;
	FileOutputStream os = null;
	try {
	    is = new FileInputStream(from);
	    os = new FileOutputStream(to);
	    byte[] buf = new byte[100000];
	    int i;
	    while ((i = is.read(buf, 0, buf.length)) != -1) {
		os.write(buf, 0, i);
	    }
	    to.setLastModified(from.lastModified());

	    return true;
	} catch (IOException e) {
	    log.error("Error copying " + from.getAbsolutePath() + " to " + to.getAbsolutePath() + ": " + e.getMessage());
	    return false;
	} finally {
	    if (is != null) {
		try {
		    is.close();
		} catch (IOException e1) {

		}
	    }
	    if (os != null) {
		try {
		    os.close();
		} catch (IOException e1) {

		}
	    }
	}
    }

    public static void announceReady() {
        log.info("GenePattern server version " 
                + ServerConfigurationFactory.instance().getGenePatternVersion()
                + " is ready.");
    }

    /**
     * return boolean indicating whether a filename represents a code file.
     */
    public static boolean isCodeFile(String filename) {
        return hasEnding(filename, codeEndings);
    }

    /**
     * return boolean indicating whether a filename represents a documentation file.
     */
    public static boolean isDocFile(String filename) {
        return hasEnding(filename, docEndings);
    }

    /**
     * return boolean indicating whether a filename represents a binary file.
     */
    public static boolean isBinaryFile(String filename) {
        return hasEnding(filename, binaryEndings);
    }

    //initialize hasEnding support     
    private static List<String> codeEndings;
    private static List<String> docEndings;
    private static List<String> binaryEndings;
    static {
        codeEndings = csvToList(System.getProperty("files.code", "").toLowerCase());
        docEndings = csvToList(System.getProperty("files.doc", "").toLowerCase());
        binaryEndings = csvToList(System.getProperty("files.binary", "").toLowerCase());
    }

    /**
     * return boolean indicating whether a filename represents a file type 
     * as found in 'files.code', 'files.doc', or 'files.binary' properties.
     */
    private static boolean hasEnding(String filename, List<String> fileEndings) {
        boolean ret = false;
        filename = new File(filename).getName().toLowerCase();
        int lastDot = filename.lastIndexOf(".");
        if (lastDot == -1) {
            ret = fileEndings.contains("");
        }
        else {
            ret = fileEndings.contains(filename.substring(lastDot + 1));
        }
        return ret;
    }

    /**
     * convert a CSV list into a List<String>.
     */
    private static List<String> csvToList(String csv) {
        StringTokenizer stEntries = new StringTokenizer(csv, ",; ");
        List<String> vEntries = new ArrayList<String>();
        while (stEntries.hasMoreTokens()) {
            vEntries.add(stEntries.nextToken());
        }
        return vEntries;
    }

    /** implements FilenameFilter, but static */
    public static boolean accept(File dir, String name) {
        return isDocFile(name);
    }

    /**
     * LHS is presented to user, RHS is the Java System property.
     */
    public static String[] getCPUTypes() {
	    return new String[] { ANY, "Alpha=alpha", "Intel=x86", "PowerPC=ppc", "Sparc=sparc" };
    }

    /**
     * LHS is presented to user, RHS is the Java System property 'os.name'
     */
    public static String[] getOSTypes() {
        return new String[] { ANY, "Linux=linux", "MacOS=Mac OS X", "Solaris=solaris", "Tru64=OSF1", "Windows=Windows" };
    }

    public static String[] getTaskTypes() {
	return new String[] { "", "Clustering", "Gene List Selection", "Image Creator", "Method", GPConstants.TASK_TYPE_PIPELINE,
		"Prediction", "Preprocess & Utilities", "Projection", "Statistical Methods", "Sequence Analysis",
		TASK_TYPE_VISUALIZER };
    }

    public static String[] getLanguages() {
	return new String[] { ANY, "C", "C++", "Java", "MATLAB", "Perl", "Python", "R" };
    }

  
   

    /**
     * The GenePatternTaskDBLoader dynamically creates Omnigene TASK_MASTER table entries for new or modified
     * GenePatternAnalysisTasks. Each task has a name, description, array of ParameterInfo declarations, and an
     * XML-encoded form of TaskInfoAttributes. These are all persisted in the Omnigene database and recalled when a task
     * is going to be invoked.
     * 
     * @author Jim Lerner
     * @see DBLoader;
     */

    private static class GenePatternTaskDBLoader extends DBLoader {
	public void setup() {
	}

	public GenePatternTaskDBLoader(String name, String description, ParameterInfo[] params, String taskInfoAttributes,
		String username, int access_id) {
	    this._name = name;
	    this._taskDescription = description;
	    this._params = params;
	    this._taskInfoAttributes = taskInfoAttributes;
	    this.access_id = access_id;
	    this.user_id = username;
	}

    }
}
