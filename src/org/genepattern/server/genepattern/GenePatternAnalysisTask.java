/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2009) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.genepattern;

import static org.genepattern.util.GPConstants.ACCESS_PRIVATE;
import static org.genepattern.util.GPConstants.ANY;
import static org.genepattern.util.GPConstants.COMMAND_LINE;
import static org.genepattern.util.GPConstants.COMMAND_PREFIX;
import static org.genepattern.util.GPConstants.CPU_TYPE;
import static org.genepattern.util.GPConstants.DEFAULT_PATCH_URL;
import static org.genepattern.util.GPConstants.DESCRIPTION;
import static org.genepattern.util.GPConstants.INPUT_BASENAME;
import static org.genepattern.util.GPConstants.INPUT_EXTENSION;
import static org.genepattern.util.GPConstants.INPUT_FILE;
import static org.genepattern.util.GPConstants.INPUT_PATH;
import static org.genepattern.util.GPConstants.INSTALLED_PATCH_LSIDS;
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
import static org.genepattern.util.GPConstants.PATCH_ERROR_EXIT_VALUE;
import static org.genepattern.util.GPConstants.PATCH_SUCCESS_EXIT_VALUE;
import static org.genepattern.util.GPConstants.PIPELINE_ARG_STOP_AFTER_TASK_NUM;
import static org.genepattern.util.GPConstants.PRIVACY;
import static org.genepattern.util.GPConstants.PRIVATE;
import static org.genepattern.util.GPConstants.PUBLIC;
import static org.genepattern.util.GPConstants.REQUIRED_PATCH_LSIDS;
import static org.genepattern.util.GPConstants.REQUIRED_PATCH_URLS;
import static org.genepattern.util.GPConstants.RESERVED_PARAMETER_NAMES;
import static org.genepattern.util.GPConstants.RIGHT_DELIMITER;
import static org.genepattern.util.GPConstants.STDERR;
import static org.genepattern.util.GPConstants.STDERR_REDIRECT;
import static org.genepattern.util.GPConstants.STDIN_REDIRECT;
import static org.genepattern.util.GPConstants.STDOUT;
import static org.genepattern.util.GPConstants.STDOUT_REDIRECT;
import static org.genepattern.util.GPConstants.TASKLOG;
import static org.genepattern.util.GPConstants.TASK_ID;
import static org.genepattern.util.GPConstants.TASK_NAMESPACE;
import static org.genepattern.util.GPConstants.TASK_TYPE_VISUALIZER;
import static org.genepattern.util.GPConstants.TOMCAT;
import static org.genepattern.util.GPConstants.UNREQUIRED_PARAMETER_NAMES;
import static org.genepattern.util.GPConstants.USERID;
import static org.genepattern.util.GPConstants.UTF8;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.Expand;
import org.genepattern.codegenerator.AbstractPipelineCodeGenerator;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.AnalysisServiceException;
import org.genepattern.server.EncryptionUtil;
import org.genepattern.server.JobInfoManager;
import org.genepattern.server.JobInfoWrapper;
import org.genepattern.server.PermissionsHelper;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.AnalysisJob;
import org.genepattern.server.domain.AnalysisJobDAO;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.domain.JobStatusDAO;
import org.genepattern.server.user.UsageLog;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.util.JobResultsFilenameFilter;
import org.genepattern.server.util.PropertiesManager;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.webservice.server.Status;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.server.webservice.server.dao.BaseDAO;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterFormatConverter;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.WebServiceException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
 * @see org.genepattern.server.AnalysisTask
 * @see org.genepattern.webservice.TaskInfoAttributes
 */

public class GenePatternAnalysisTask {

    private static Logger log = Logger.getLogger(GenePatternAnalysisTask.class);

    protected static final String CLASSPATH = "classpath";

    protected static final String OUTPUT_FILENAME = "output_filename";

    protected static final String ORIGINAL_PATH = "originalPath";

    public static final String TASK_NAME = "GenePatternAnalysisTask";

    /**
     * hashtable of running jobs. key=jobID (as String), value=Process
     */
    protected static Hashtable<String, Process> htRunningJobs = new Hashtable<String, Process>();

    /**
     * hashtable of running pipelines. key=jobID (as String), value=Process
     */
    protected static Hashtable<String, Process> htRunningPipelines = new Hashtable<String, Process>();

    /**
     * indicates whether version string has been displayed by init already
     */
    protected static boolean bAnnounced = false;

    private enum INPUT_FILE_MODE {
	COPY, MOVE, PATH
    };

    /** set with property input.file.mode */
    private INPUT_FILE_MODE inputFileMode = INPUT_FILE_MODE.MOVE;

    /** set with property allow.input.file.paths */
    private boolean allowInputFilePaths = true;

    /**
     * GenePatternAnalysisTask constructor. Loads properties from genepattern.properties
     */
    public GenePatternAnalysisTask() {

	if (System.getProperty("GenePatternVersion") == null) {
	    // System properties are already loaded by StartupServlet
	    File propFile = new File(System.getProperty("genepattern.properties"), "genepattern.properties");
	    FileInputStream fis = null;
	    try {
		fis = new FileInputStream(propFile);
		System.getProperties().load(fis);
	    } catch (IOException ioe) {
		log.error(propFile.getName() + " cannot be loaded.  " + ioe.getMessage());
	    } finally {
		try {
		    if (fis != null) {
			fis.close();
		    }
		} catch (IOException ioe) {
		}
	    }
	}

	String canonicalPathNames[] = new String[] { TOMCAT };
	String oldName;
	String newName;
	for (int i = 0; i < canonicalPathNames.length; i++) {
	    oldName = System.getProperty(canonicalPathNames[i]);
	    if (oldName == null) {
		continue;
	    }
	    try {
		newName = new File(oldName).getCanonicalPath();
		System.setProperty(canonicalPathNames[i], newName);
	    } catch (IOException ioe) {
		log.error("GenePattern init: " + ioe + " while getting canonical path for " + oldName);
	    }
	}

	if (!bAnnounced) {
	    log.info("GenePattern version " + System.getProperty("GenePatternVersion") + " build " + System.getProperty("tag")
		    + " loaded");
	    bAnnounced = true;
	}

	String inputFilePathProp = System.getProperty("allow.input.file.paths");
	if (inputFilePathProp != null) {
	    if (inputFilePathProp.equalsIgnoreCase("true") || inputFilePathProp.equals("1")) {
		allowInputFilePaths = true;
		log.info("Allowing input file paths.");
	    } else if (inputFilePathProp.equalsIgnoreCase("false") || inputFilePathProp.equals("0")) {
		allowInputFilePaths = false;
	    }
	}

	String inputFileModeProp = System.getProperty("input.file.mode");
	if (inputFileModeProp != null) {
	    if (inputFileModeProp.equals("copy")) {
		inputFileMode = INPUT_FILE_MODE.COPY;
		log.info("Input file mode set to copy.");
	    } else if (inputFileModeProp.equals("move")) {
		inputFileMode = INPUT_FILE_MODE.MOVE;
		log.info("Input file mode set to move.");
	    } else if (inputFileModeProp.equals("path")) {
		inputFileMode = INPUT_FILE_MODE.PATH;
		log.info("Input file mode set to path.");
	    } else {
		log.info("Unknown value for property input.file.mode. Valid options are 'copy', 'move', and 'path'.");
	    }
	}
    }

    /**
     * Tests whether the specified URL refers to the local host.
     * 
     * @param url
     *            The URL to check whether it refers to the local host.
     * @return <tt>true</tt> if the specified URL refers to the local host.
     */
    protected boolean isLocalHost(URL url) {
	String gpHost = null;
	String hostAddress = null;
	try {
	    if (System.getProperty("GenePatternURL") != null) {
		URL gpUrl = new URL(System.getProperty("GenePatternURL"));
		gpHost = gpUrl.getHost();
		hostAddress = InetAddress.getLocalHost().getHostAddress();
	    }
	} catch (MalformedURLException mfe) {
	} catch (UnknownHostException uhe) {
	}

	String requestedHost = url.getHost();
	try {
	    return (url.toString().startsWith("<GenePatternURL>") || requestedHost.equals("localhost")
		    || requestedHost.equals("127.0.0.1")
		    || requestedHost.equals(InetAddress.getLocalHost().getCanonicalHostName()) || requestedHost.equals(gpHost) || InetAddress
		    .getByName(requestedHost).getHostAddress().equals(hostAddress));

	} catch (UnknownHostException x) {
	    log.error("Unknown host", x);
	    return false;
	}
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
    protected File localInputUrlToFile(URL url, String userId) {
        String path = url.getPath();
        try {
            path = URLDecoder.decode(path, "UTF-8");
        } 
        catch (UnsupportedEncodingException e) {
            log.error("Error", e);
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
            int idx3 = params.indexOf("job=");
            int endIdx3 = params.indexOf('&', idx3);
            if (endIdx3 == -1) {
                endIdx3 = params.length();
            }
            int jobNumber = -1;
            String jobNumberParam = params.substring(idx3 + 4, endIdx3);
            if (jobNumberParam != null) {
                try {
                    jobNumber = Integer.parseInt(jobNumberParam);
                }
                catch (NumberFormatException e) {
                    log.error("Invalid request parameter, job="+jobNumberParam, e);
                }
            }

            if (lsid == null || lsid.trim().equals("")) { 
                // input file look in temp for pipelines run without saving
                File in = new File(System.getProperty("java.io.tmpdir"), filename);
                // check whether this is the user or an admin requesting the file
                if (in.exists()) {
                    PermissionsHelper perm = new PermissionsHelper(userId, jobNumber);
                    boolean canRead = perm.canReadJob();
                    if (canRead) {
                        return in;
                    }
                    throw new IllegalArgumentException("You are not permitted to access the requested file: "+in.getName());
                }
                return null;
            }
            // check that user can access requested module
            try {
                if (new LocalAdminClient(userId).getTask(lsid) != null) {
                    File file = new File(DirectoryManager.getTaskLibDir(lsid, lsid, userId), filename);
                    if (file.exists()) {
                        return file;
                    }
                } 
                else {
                    throw new IllegalArgumentException("You are not permitted to access the requested file: "+filename);
                }
            } 
            catch (WebServiceException e) {
                log.error("Error", e);
                throw new IllegalArgumentException("Error connecting to database.");
            } 
            catch (MalformedURLException e) {
                log.error("Error", e);
                throw new IllegalArgumentException("Invalid LSID.");
            } 
            catch (IllegalArgumentException e) {
                log.error("Error", e);
                throw new IllegalArgumentException("Module not found.");
            }
        }

        File jobsDir = new File(System.getProperty("jobs"));
        String jobDirName = jobsDir.getName();
        int jobDirIndex = -1;
        if ((jobDirIndex = path.lastIndexOf(jobDirName)) != -1) {
            path = path.substring(jobDirIndex + jobDirName.length());
            StringTokenizer strtok = new StringTokenizer(path, "/");
            String job = null;

            if (strtok.hasMoreTokens()) {
                job = strtok.nextToken();
            }
            String requestedFilename = null;
            if (strtok.hasMoreTokens()) {
                requestedFilename = strtok.nextToken();
            }
            if (job == null || requestedFilename == null) {
                return null;
            }
            int jobNumber = -1;
            try {
                jobNumber = Integer.parseInt(job);
            }
            catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("You are not permitted to access the requested file: "+requestedFilename+", Error processing job number: "+job);
            }
            
            PermissionsHelper perm = new PermissionsHelper(userId, jobNumber);
            boolean canRead = perm.canReadJob();
            if (canRead) {
                File jobDir = new File(jobsDir, job);
                File file = new File(jobDir, requestedFilename);
                if (file.exists()) {
                    return file;
                }                
            }
            else {
                throw new IllegalArgumentException("You are not permitted to access the requested file: "+job+"/"+requestedFilename);                
            }
        }
        return null;
    }

    private boolean isJobOwner(String user, String jobId) {
	try {
	    if (user == null) {
		return false;
	    }
	    int jobID = Integer.parseInt(jobId);
	    AnalysisDAO ds = new AnalysisDAO();
	    JobInfo jobInfo = ds.getJobInfo(jobID);
	    return user.equals(jobInfo.getUserId());
	} catch (NumberFormatException nfe) {
	    return false;
	}

    }

    /**
     * Called by Omnigene Analysis engine to run a single analysis job, wait for completion, then report the results to
     * the analysis_job database table. Running a job involves looking up the TaskInfo and TaskInfoAttributes for the
     * job, validating and formatting a command line based on the formal and actual arguments to the task, downloading
     * any input URLs to the local filesystem, executing the application, and then returning any of the output files
     * from the sandbox directory where it ran to the analysis_job database (and ultimately to the caller).
     * 
     * @param o
     *            JobInfo object
     */
    public void onJob(Object o) {
	JobInfo jobInfo = (JobInfo) o;

	if (log.isDebugEnabled()) {
	    log.debug("Start onJob id=" + jobInfo.getJobNumber() + " (" + jobInfo.getTaskName());
	}

	// pipelines run from the webapp show up as BaseDAO.UNPROCESSABLE_TASKID
	// and are being run by other means so bail out now
	if (jobInfo.getTaskID() == BaseDAO.UNPROCESSABLE_TASKID) {
	    log.error("pipelines run from the webapp should no longer show with taskID="+BaseDAO.UNPROCESSABLE_TASKID);
	    return;
	}

	JobInfo parentJobInfo = null;
	int jobStatus = JobStatus.JOB_ERROR;
	String outDirName = getJobDir(Integer.toString(jobInfo.getJobNumber()));
	File taskLog = null;
	String taskName = "";
	long jobStartTime = System.currentTimeMillis();
	String userKey = "";
	try {
	    // make directory to hold input and output files
	    File outDir = new File(outDirName);
	    if (!outDir.exists()) {
	        if (!outDir.mkdirs()) {
	            log.error("onJob error making directory " + outDirName);
	            throw new AnalysisServiceException("Error creating output directory " + outDirName);
	        }
	    } 
	    else {
	        // clean out existing directory
	        if (log.isDebugEnabled()) {
	            log.debug("clean out existing directory");
	        }
	        File[] old = outDir.listFiles();
	        for (int i = 0; old != null && i < old.length; i++) {
	            old[i].delete();
	        }
	    }

	    TaskInfo taskInfo = getTaskInfo(jobInfo);
	    taskName = taskInfo.getName();
	    if (log.isDebugEnabled()) {
	        log.debug("taskName=" + taskName);
	    }

	    int formalParamsLength = 0;
	    ParameterInfo[] formalParams = taskInfo.getParameterInfoArray();
	    if (formalParams != null) {
	        formalParamsLength = formalParams.length;
	    }

	    TaskInfoAttributes taskInfoAttributes = taskInfo.giveTaskInfoAttributes();
	    if (taskInfoAttributes == null || taskInfoAttributes.size() == 0) {
	        throw new Exception(taskName + ": missing all TaskInfoAttributes!");
	    }

	    // check OS and CPU restrictions of TaskInfoAttributes against this server
	    // eg. "x86", "ppc", "alpha", "sparc"
	    validateCPU(taskInfoAttributes.get(CPU_TYPE));
	    String expected = taskInfoAttributes.get(OS);
	    // eg. "Windows", "linux", "Mac OS X", "OSF1", "Solaris"
	    validateOS(expected, "run " + taskName);
	    validatePatches(taskInfo, null);

	    User user = (new UserDAO()).findById(jobInfo.getUserId());
	    userKey = EncryptionUtil.getInstance().pushPipelineUserKey(user);

	    Map<String, String> environmentVariables = new HashMap<String, String>();
	    environmentVariables.put(EncryptionUtil.PROP_PIPELINE_USER_KEY, userKey);

	    JobInfo parentJI = getParentJobInfo(jobInfo.getJobNumber());
	    int parent = -1;
	    if (parentJI != null) {
	        parent = parentJI.getJobNumber();
	    }
	    ParameterInfo[] params = jobInfo.getParameterInfoArray();
	    Properties props = setupProps(taskName, parent, jobInfo.getJobNumber(), jobInfo.getTaskID(),
		    taskInfoAttributes, params, environmentVariables, taskInfo.getParameterInfoArray(), jobInfo.getUserId());
	    Vector<String> vProblems = new Vector<String>();
	    long inputLastModified[] = new long[0];
	    long inputLength[] = new long[0];
	    if (params != null) {
	        inputLastModified = new long[params.length];
	        inputLength = new long[params.length];
	        for (int i = 0; i < params.length; i++) {
	            HashMap<String, String> attrsActual = params[i].getAttributes();
	            if (attrsActual == null) {
	                attrsActual = new HashMap<String, String>();
	            }
	            String fileType = attrsActual.get(ParameterInfo.TYPE);
	            String mode = attrsActual.get(ParameterInfo.MODE);
	            String originalPath = params[i].getValue();
	            // allow parameter value substitutions within file input parameters
	            originalPath = substitute(originalPath, props, params);
	            boolean isOptional = "on".equals(attrsActual.get("optional"));
	            if (fileType != null && fileType.equals(ParameterInfo.FILE_TYPE) && mode != null && !mode.equals(ParameterInfo.OUTPUT_MODE)) {
	                if (originalPath == null) {
	                    if (isOptional) {
	                        continue;
	                    }
	                    throw new IOException("Non-optional parameter " + params[i].getName() + " has not been assigned a filename.");
	                }
	                if (mode.equals("CACHED_IN")) {
	                    // param is existing job output file
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
	                    PermissionsHelper perm = new PermissionsHelper(jobInfo.getUserId(), jobNumber);
	                    boolean canRead = perm.canReadJob();
	                    if (canRead) {
	                        originalPath = System.getProperty("jobs") + "/" + originalPath;
	                    }
	                    else {
	                        vProblems.add("You are not permitted to access the requested file: "+requestedFilename);
	                        continue;                        
	                    }
	                } 
	                else if (mode.equals(ParameterInfo.INPUT_MODE)) {
	                    log.error("IN " + params[i].getName() + "=" + originalPath);
	                    // file provided via SOAP or by web form upload
	                    // ensure file is in GenePatternServer/temp/attachments/username or in
	                    // GenePatternServer/Tomcat/temp/username_run[0-9]+.tmp
	                    String webUploadDirectory = new File(System.getProperty("java.io.tmpdir")).getCanonicalPath();
	                    String soapAttachmentDir = new File(System.getProperty("soap.attachment.dir") + File.separator + jobInfo.getUserId()).getCanonicalPath();

	                    File inputFile = new File(originalPath);
	                    String inputFileDirectory = inputFile.getParentFile().getCanonicalPath();

	                    if (inputFile.getParentFile().getParentFile().getCanonicalPath().equals(webUploadDirectory)) {
	                        PermissionsHelper perm = new PermissionsHelper(jobInfo.getUserId(), jobInfo.getJobNumber());
	                        boolean canRead = perm.canReadJob();
	                        if (!canRead) {
	                            vProblems.add("You are not permitted to access the requested file: "+inputFile.getName());
	                            continue;
	                        }
	                    } 
	                    else if (!inputFileDirectory.equals(soapAttachmentDir)) {
	                        vProblems.add("Input file " + new File(originalPath).getName() + " must be in SOAP attachment directory or web upload directory.");
	                        continue;
	                    }
	                } 
	                else {
	                    vProblems.add("Unknown mode for parameter " + params[i].getName() + ".");
	                    continue;
	                }
	                File inFile = new File(originalPath);
	                if (!inFile.exists()) {
	                    vProblems.add("Input file " + inFile + " does not exist.");
	                    continue;
	                }

	                if (inputFileMode == INPUT_FILE_MODE.PATH) {
	                    params[i].setValue(inFile.getCanonicalPath());
	                    attrsActual.remove(ParameterInfo.TYPE);
	                    attrsActual.remove(ParameterInfo.INPUT_MODE);
	                } 
	                else {
	                    File outFile = null;
	                    String inputFilename = new File(originalPath).getName();
	                    // strip off the AxisNNNNNaxis_ prefix
	                    int underscoreIndex = -1;
	                    if (inputFilename.startsWith("Axis") && (underscoreIndex = inputFilename.indexOf("_")) != -1) {
	                        inputFilename = inputFilename.substring(underscoreIndex + 1);
	                    }
	                    // outDirName is job directory
	                    outFile = new File(outDirName, inputFilename);
	                    int counter = 1;
	                    while (outFile.exists()) { // in case two input files have the same name
	                        outFile = new File(outDirName, inputFilename + "-" + counter);
	                        counter++;
	                    }

	                    if (inputFileMode == INPUT_FILE_MODE.COPY) {
	                        if (!copyFile(inFile, outFile)) {
	                            vProblems.add("Unable to copy " + inFile + " to " + outFile);
	                            continue;
	                        }
	                        outFile.deleteOnExit(); // mark for delete, just in case
	                    } 
	                    else if (inputFileMode == INPUT_FILE_MODE.MOVE) {
	                        if (!rename(inFile, outFile, true)) {
	                            vProblems.add("Unable to move " + inFile + " to " + outFile);
	                            continue;
	                        }
	                    }
	                    params[i].getAttributes().put(ORIGINAL_PATH, originalPath);
	                    params[i].setValue(outFile.getCanonicalPath());
	                    inputLastModified[i] = outFile.lastModified();
	                    inputLength[i] = outFile.length();
	                }
	            } 
	            else if (i >= formalParamsLength) {
	                log.debug("params[" + i + "]=" + params[i].getName() + " has no formal parameter defined");
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
	                    if (formals[formal].getName().equals(params[i].getName())) {
	                        attrFormals = formals[formal].getAttributes();
	                        fileType = attrFormals.get(ParameterInfo.TYPE);
	                        mode = attrFormals.get(ParameterInfo.MODE);
	                        break;
	                    }
	                }
	                boolean isURL = false;
	                if (fileType != null && fileType.equals(ParameterInfo.FILE_TYPE) && mode != null && !mode.equals(ParameterInfo.OUTPUT_MODE) && originalPath != null) {
	                    // handle http files by downloading them and substituting the downloaded filename for the URL in the command line.
	                    if (inputFileMode == INPUT_FILE_MODE.PATH && new File(originalPath).exists()) {
	                        if (!allowInputFilePaths) {
	                            vProblems.add("You are not permitted to access the requested file: "+originalPath);
	                            continue;
	                        }
	                        params[i].setValue(new File(originalPath).getCanonicalPath());
	                        attrsActual.remove(ParameterInfo.TYPE);
	                        attrsActual.remove(ParameterInfo.INPUT_MODE);
	                    } 
	                    else {
	                        try {
	                            if (originalPath != null) {
	                                new URL(originalPath);
	                                isURL = true;
	                            }
	                        } 
	                        catch (MalformedURLException mfe) {
	                            // path on server
	                            if (!allowInputFilePaths) {
	                                vProblems.add("You are not permitted to access the requested file: "+originalPath);
	                                continue;
	                            }
	                        }
	                    }
	                }
	                if (isURL && !taskInfo.isVisualizer()) {
	                    URI uri = new URI(originalPath);
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
	                        if ("file".equalsIgnoreCase(uri.getScheme()) && allowInputFilePaths) {
	                            File f = new File(uri);
	                            if (inputFileMode == INPUT_FILE_MODE.PATH) {
	                                params[i].setValue(f.getCanonicalPath());
	                                attrsActual.remove(ParameterInfo.TYPE);
	                                attrsActual.remove(ParameterInfo.INPUT_MODE);
	                                downloadUrl = false;
	                            } 
	                            else {
	                                is = new FileInputStream(f);
	                                name = f.getName();
	                            }
	                        } 
	                        else if ("file".equalsIgnoreCase(uri.getScheme()) && !allowInputFilePaths) {
	                            // prompt when run input files in pipelines are saved as
	                            // file:/Applications/GenePatternServer/Tomcat/temp/username_run27407.tmp/filename
	                            // through the web client and
	                            // file:/Applications/GenePatternServer/Tomcat/webapps/gp/jobResults/jobNumber/
	                            // filename through SOAP
	                            File inputFile = new File(uri);
	                            String grandParentPath = inputFile.getParentFile().getParentFile().getCanonicalPath();

	                            String webUploadDirectory = new File(System.getProperty("java.io.tmpdir")).getCanonicalPath();
	                            boolean isWebUpload = false;
	                            isWebUpload = webUploadDirectory.equals( grandParentPath );
	                            if (! ( isWebUpload
	                                    && 
	                                    ( AuthorizationHelper.adminJobs(jobInfo.getUserId()) 
	                                            || 
	                                            inputFile.getParentFile().getName().startsWith(jobInfo.getUserId() + "_")
	                                    )
	                            )
	                            ) {
	                                String jobsDirectory = new File(System.getProperty("jobs")).getCanonicalPath();
	                                boolean isJobOutput = false;
	                                isJobOutput = jobsDirectory.equals(grandParentPath);
	                                String jobNumber = inputFile.getParentFile().getName();
	                                if (!isJobOutput
	                                        || 
	                                        ( !isJobOwner(jobInfo.getUserId(), jobNumber) 
	                                                && 
	                                                !AuthorizationHelper.adminJobs(jobInfo.getUserId())
	                                        )
	                                ) {
	                                    vProblems.add("File input URLs are not allowed on this GenePattern server.");
	                                    continue;
	                                }
	                            }
	                            File f = new File(uri);
	                            if (inputFileMode == INPUT_FILE_MODE.PATH) {
	                                params[i].setValue(f.getCanonicalPath());
	                                attrsActual.remove(ParameterInfo.TYPE);
	                                attrsActual.remove(ParameterInfo.INPUT_MODE);
	                                downloadUrl = false;
	                            } 
	                            else {
	                                is = new FileInputStream(f);
	                                name = f.getName();
	                            }
	                        } 
	                        else {
	                            URL url = uri.toURL();
	                            if (isLocalHost(url)) {
	                                try {
	                                    File file = localInputUrlToFile(url, jobInfo.getUserId());
	                                    if (file != null) {
	                                        if (inputFileMode == INPUT_FILE_MODE.PATH) {
	                                            params[i].setValue(file.getCanonicalPath());
	                                            attrsActual.remove(ParameterInfo.TYPE);
	                                            attrsActual.remove(ParameterInfo.INPUT_MODE);
	                                            downloadUrl = false;
	                                        } 
	                                        else {
	                                            name = file.getName();
	                                            is = new BufferedInputStream(new FileInputStream(file));
	                                        }
	                                    }
	                                } 
	                                catch (IllegalArgumentException e) {
	                                    // user tried to access file that he is not allowed to
	                                    vProblems.add(e.getMessage());
	                                    downloadUrl = false;
	                                }
	                            }
	                            if (is == null && downloadUrl) {
	                                try {
	                                    URLConnection conn = url.openConnection();
	                                    name = getDownloadFileName(conn, url);
	                                    is = conn.getInputStream();
	                                } 
	                                catch (IOException e) {
	                                    vProblems.add("Unable to connect to " + url + ".");
	                                    downloadUrl = false;
	                                }
	                            }
	                        }
	                        if (downloadUrl) {
	                            outFile = new File(outDirName, name);
	                            if (outFile.exists()) {
	                                // ensure that 2 file downloads for a job don't have the same name
	                                if (name.length() < 3) {
	                                    name = "download";
	                                }
	                                outFile = File.createTempFile(name, null, new File(outDirName));
	                            }
	                            os = new FileOutputStream(outFile);
	                            byte[] buf = new byte[100000];
	                            int bytesRead;
	                            while ((bytesRead = is.read(buf, 0, buf.length)) != -1) {
	                                os.write(buf, 0, bytesRead);
	                            }
	                            params[i].getAttributes().put(ORIGINAL_PATH, originalPath);
	                            params[i].setValue(outFile.getCanonicalPath());
	                            inputLastModified[i] = outFile.lastModified();
	                            inputLength[i] = outFile.length();
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
	                            inputLastModified[i] = outFile.lastModified();
	                        }
	                    }
	                }
	            }
	        } // end for each parameter
	    } // end if parameters not null

	    // build the command line, replacing <variableName> with the same
	    // name from the properties
	    // (ParameterInfo[], System properties, environment variables, and built-ins merged)
	    // build props again, now that downloaded files are set
	    props = setupProps(taskName, parent, jobInfo.getJobNumber(), jobInfo.getTaskID(), taskInfoAttributes, params,
	            environmentVariables, taskInfo.getParameterInfoArray(), jobInfo.getUserId());

	    params = stripOutSpecialParams(params);
	    // check that all parameters are used in the command line
	    // and that all non-optional parameters that are cited actually exist
	    ParameterInfo[] formalParameters = taskInfo.getParameterInfoArray();
	    Vector<String> parameterProblems = validateParameters(props, taskName, taskInfoAttributes.get(COMMAND_LINE), params,
	            formalParameters, true);

	    vProblems.addAll(parameterProblems);
	    String c = substitute(substitute(taskInfoAttributes.get(COMMAND_LINE), props, formalParameters), props,
	            formalParameters);
	    if (c == null || c.trim().length() == 0) {
	        vProblems.add("Command line not defined");
	    }

	    if (!taskInfo.isVisualizer()) {
	        //TODO: special case for visualizer
	        setCommandPrefix(taskInfoAttributes);
	    }

	    // create an array of Strings for Runtime.exec to fix bug 55
	    // (filenames in spaces cause invalid command line)
	    String cmdLine = taskInfoAttributes.get(COMMAND_LINE);
	    StringTokenizer stCommandLine;
	    String[] commandTokens = null;
	    String firstToken;
	    String token;

	    // TODO: handle quoted arguments within the command line (eg. echo "<p1> <p2>" as a single token)

	    // check that the user didn't quote the program name
	    if (!cmdLine.startsWith("\"")) {
	        // since we could have a definition like "<perl>=perl -Ifoo",
	        // we need to double-tokenize the first token to extract just "perl"
	        stCommandLine = new StringTokenizer(cmdLine);
	        firstToken = stCommandLine.nextToken();
	        // now the command line contains the real first word (perl)
	        // followed by the rest, ready for space-tokenizing
	        cmdLine = substitute(firstToken, props, formalParameters) + cmdLine.substring(firstToken.length());
	        stCommandLine = new StringTokenizer(cmdLine);
	        commandTokens = new String[stCommandLine.countTokens()];
	        for (int i = 0; stCommandLine.hasMoreTokens(); i++) {
	            token = stCommandLine.nextToken();
	            commandTokens[i] = substitute(token, props, formalParameters);
	            if (commandTokens[i] == null) {
	                String[] copy = new String[commandTokens.length - 1];
	                System.arraycopy(commandTokens, 0, copy, 0, i);
	                if ((i + 1) < commandTokens.length) {
	                    System.arraycopy(commandTokens, i + 1, copy, i, commandTokens.length - i - 1);
	                }
	                commandTokens = copy;
	                i--;
	            }
	        }
	    } else {
	        // the user quoted the command, so it has to be handled specially
	        int endQuote = cmdLine.indexOf("\"", 1);
	        // find the matching closing quote
	        if (endQuote == -1) {
	            vProblems.add("Missing closing quote on command line: " + cmdLine);
	        } else {
	            firstToken = cmdLine.substring(1, endQuote);
	            stCommandLine = new StringTokenizer(cmdLine.substring(endQuote + 1));
	            commandTokens = new String[stCommandLine.countTokens() + 1];
	            commandTokens[0] = substitute(firstToken, props, formalParameters);
	            for (int i = 1; stCommandLine.hasMoreTokens(); i++) {
	                token = stCommandLine.nextToken();
	                commandTokens[i] = substitute(token, props, formalParameters);
	                // empty token?
	                if (commandTokens[i] == null) {
	                    String[] copy = new String[commandTokens.length - 1];
	                    System.arraycopy(commandTokens, 0, copy, 0, i);
	                    if ((i + 1) < commandTokens.length) {
	                        System.arraycopy(commandTokens, i + 1, copy, i, commandTokens.length - i - 1);
	                    }
	                    commandTokens = copy;
	                    i--;
	                }
	            }
	        }
	    }

	    // do the substitutions one more time to allow, for example, p2=<p1>.res
	    for (int i = 1; i < commandTokens.length; i++) {
	        commandTokens[i] = substitute(commandTokens[i], props, formalParameters);
	        if (commandTokens[i] == null) {
	            String[] copy = new String[commandTokens.length - 1];
	            System.arraycopy(commandTokens, 0, copy, 0, i);
	            if ((i + 1) < commandTokens.length) {
	                System.arraycopy(commandTokens, i + 1, copy, i, commandTokens.length - i - 1);
	            }
	            commandTokens = copy;
	            i--;
	        }
	    }
	    String stdoutFilename = STDOUT;
	    String stderrFilename = STDERR;
	    String stdinFilename = null;
	    StringBuffer commandLine = new StringBuffer();
	    List<String> commandLineList = new ArrayList<String>(commandTokens.length);
	    boolean addLast = true;
	    for (int j = 0; j < commandTokens.length - 1; j++) {
	        if (commandTokens[j].equals(STDOUT_REDIRECT)) {
	            stdoutFilename = commandTokens[++j];
	            if ("".equals(stdoutFilename)) {
	                vProblems.add("Missing name for standard output redirect");
	            }
	            addLast = false;
	        } else if (commandTokens[j].equals(STDERR_REDIRECT)) {
	            stderrFilename = commandTokens[++j];
	            if ("".equals(stderrFilename)) {
	                vProblems.add("Missing name for standard error redirect");
	            }
	            addLast = false;
	        } else if (commandTokens[j].equals(STDIN_REDIRECT)) {
	            stdinFilename = commandTokens[++j];
	            if ("".equals(stdinFilename)) {
	                vProblems.add("Missing name for standard input redirect");
	            }
	            addLast = false;
	        } else {
	            addLast = true;
	            commandLine.append(commandTokens[j]);
	            commandLine.append(" ");
	            commandLineList.add(commandTokens[j]);
	        }
	    }
	    if (addLast) {
	        commandLineList.add(commandTokens[commandTokens.length - 1]);
	        commandLine.append(commandTokens[commandTokens.length - 1]);
	    }
	    commandTokens = commandLineList.toArray(new String[0]);
	    String lastToken = commandTokens[commandTokens.length - 1];
	    if (lastToken.equals(STDOUT_REDIRECT)) {
	        vProblems.add("Missing name for standard output redirect");
	    } else if (lastToken.equals(STDERR_REDIRECT)) {
	        vProblems.add("Missing name for standard error redirect");
	    } else if (lastToken.equals(STDIN_REDIRECT)) {
	        vProblems.add("Missing name for standard input redirect");
	    }
	    StringBuffer stderrBuffer = new StringBuffer();
	    if (vProblems.size() > 0) {
	        for (Enumeration<String> eProblems = vProblems.elements(); eProblems.hasMoreElements();) {
	            stderrBuffer.append(eProblems.nextElement() + "\n");
	        }
	        jobStatus = JobStatus.JOB_ERROR;
	    } 
	    else {
	        // run the task and wait for completion.
	        log.info("running " + taskName + " (job " + jobInfo.getJobNumber() + ") command: " + commandLine.toString());
	        File stdoutFile;
	        File stderrFile;
	        boolean renameStdout = stdoutFilename == null;
	        if (renameStdout) {
	            stdoutFile = File.createTempFile("stdout", null);
	            stdoutFilename = STDOUT;
	        } else {
	            stdoutFile = new File(outDir, stdoutFilename);
	        }
	        boolean renameStderr = stderrFilename == null;
	        if (renameStderr) {
	            stderrFile = File.createTempFile("stderr", null);
	            stderrFilename = STDERR;
	        } 
	        else {
	            stderrFile = new File(outDir, stderrFilename);
	        }
	        try {
	            if (taskInfo.isVisualizer()) {
	                jobStatus = JobStatus.JOB_FINISHED;
	            }
	            else {
	                runCommand(commandTokens, environmentVariables, outDir, stdoutFile, stderrFile, jobInfo, stdinFilename, stderrBuffer);
	                if (stderrFile != null && stderrFile.exists() && stderrFile.length() > 0) {
	                    jobStatus = JobStatus.JOB_ERROR;
	                }
	                else {
	                    jobStatus = JobStatus.JOB_FINISHED;
	                }
	                log.info(taskName + " (" + jobInfo.getJobNumber() + ") done.");
	            }
	        } 
	        catch (Throwable t) {
	            jobStatus = JobStatus.JOB_ERROR;
	            log.info(taskName + " (" + jobInfo.getJobNumber() + ") done with error: " + t.getMessage());
	            t.printStackTrace();
	            stderrBuffer.append(t.getMessage() + "\n\n");
	        } 
	        finally {
	            if (renameStdout) {
	                stdoutFile.renameTo(new File(outDir, STDOUT));
	            }
	            if (renameStderr) {
	                stderrFile.renameTo(new File(outDir, STDERR));
	            }
	        }
	    }

	    //write execution log
        if (!taskInfo.isVisualizer()) {
            JobInfoManager m = new JobInfoManager();
            //TODO: compute contextPath
            String contextPath = "/gp";
            String cookie = "";
            JobInfoWrapper jobInfoWrapper = m.getJobInfo(cookie, contextPath, jobInfo.getUserId(), jobInfo.getJobNumber());
            taskLog = JobInfoManager.writeExecutionLog(outDirName, jobInfoWrapper, props);
        }

	    // move input files back into Axis attachments directory
	    if (params != null) {
	        for (int i = 0; i < params.length; i++) {
	            HashMap attrsActual = params[i].getAttributes();
	            String fileType = (attrsActual != null ? (String) attrsActual.get(ParameterInfo.TYPE) : null);
	            String mode = (attrsActual != null ? (String) attrsActual.get(ParameterInfo.MODE) : null);
	            if (fileType != null && fileType.equals(ParameterInfo.FILE_TYPE) && mode != null
	                    && !mode.equals(ParameterInfo.OUTPUT_MODE)) {
	                if (params[i].getValue() == null) {
	                    throw new IOException(params[i].getName() + " has no filename association");
	                }
	                File inFile = new File(params[i].getValue());
	                String originalPath = (String) params[i].getAttributes().remove(ORIGINAL_PATH);
	                log.debug(params[i].getName() + ", original path='" + originalPath + "', inFile " + params[i].getValue()
	                        + ", exists " + inFile.exists());
	                if (originalPath == null || originalPath.length() == 0) {
	                    continue;
	                }
	                File originalFile = new File(originalPath);

	                // un-borrow the input file, moving it from the job's directory back to where it came from
	                if (inFile.exists()
	                        && !originalFile.exists()
	                        && (inputFileMode == INPUT_FILE_MODE.COPY ? !inFile.delete()
	                                : !rename(inFile, originalFile, true))) {
	                    log.warn("Failed to rename " + inFile + " to " + originalFile + ".");
	                } else {
	                    if (inputLastModified[i] != originalFile.lastModified() || inputLength[i] != originalFile.length()) {
	                        if (inputLastModified[i] != originalFile.lastModified()) {
	                            log.warn("File " + originalFile + ", job number " + jobInfo.getJobNumber()
	                                    + " last modfied date was changed. Original date: " + new Date(inputLastModified[i])
	                            + ", current date: " + new Date(originalFile.lastModified()));
	                        }
	                        if (inputLength[i] != originalFile.length()) {
	                            log.warn("File " + originalFile + ", job number " + jobInfo.getJobNumber()
	                                    + " size was changed. Original size: " + inputLength[i] + ", current size: "
	                                    + originalFile.length());
	                        }
	                    }
	                    params[i].setValue(originalPath);
	                }
	            } else {
	                // TODO: what if the input file is also supposed to be one of the outputs?
	                String originalPath = (String) params[i].getAttributes().remove(ORIGINAL_PATH);
	                boolean isURL = false;
	                if (originalPath != null) {
	                    try {
	                        new URL(originalPath);
	                        isURL = true;
	                    } catch (MalformedURLException e) {
	                    }
	                }
	                if (originalPath != null && isURL) {
	                    File outFile = new File(params[i].getValue());
	                    if (inputLastModified[i] != outFile.lastModified() || inputLength[i] != outFile.length()) {
	                        String errorMessage = outFile.toString()
	                        + " may have been overwritten during execution of module " + taskName + ", job number "
	                        + jobInfo.getJobNumber() + "\n";
	                        if (inputLastModified[i] != outFile.lastModified()) {
	                            errorMessage = errorMessage + "original date: " + new Date(inputLastModified[i])
	                            + ", current date: " + new Date(outFile.lastModified()) + "\n";
	                        }
	                        if (inputLength[i] != outFile.length()) {
	                            errorMessage = errorMessage + "original size: " + inputLength[i] + ", current size: "
	                            + outFile.length() + "\n";
	                        }
	                        log.warn(errorMessage);
	                    }
	                    outFile.delete();
	                    params[i].setValue(originalPath);
	                    continue;
	                }
	            }
	        } // end for each parameter
	    } // end if parameters not null

	    // reload jobInfo to pick up any output parameters were added by the job explicitly (eg. pipelines)
	    HibernateUtil.beginTransaction();
	    jobInfo = (new AnalysisDAO()).getJobInfo(jobInfo.getJobNumber());
	    HibernateUtil.commitTransaction();

	    // touch the taskLog file to make sure it is the oldest/last file
	    if (taskLog != null) {
	        taskLog.setLastModified(System.currentTimeMillis() + 500);
	    }

	    // any files that are left in outDir are output files
	    final String _stdoutFilename = stdoutFilename;
	    final String _stderrFilename = stderrFilename;
	    JobResultsFilenameFilter filenameFilter = new JobResultsFilenameFilter();
	    filenameFilter.addExactMatch(STDERR);
	    filenameFilter.addExactMatch(STDOUT);
	    filenameFilter.addExactMatch(TASKLOG);
	    filenameFilter.addExactMatch(_stdoutFilename);
	    filenameFilter.addExactMatch(_stderrFilename);
	    filenameFilter.setGlob(System.getProperty(JobResultsFilenameFilter.KEY));

	    File[] outputFiles = new File(outDirName).listFiles(filenameFilter);
	    sortOutputFiles(outputFiles);

	    parentJobInfo = getParentJobInfo(jobInfo.getJobNumber());

	    for (int i = 0; i < outputFiles.length; i++) {
	        File f = outputFiles[i];
	        log.debug("adding output file to output parameters " + f.getName() + " from " + outDirName);
	        addFileToOutputParameters(jobInfo, f.getName(), f.getName(), parentJobInfo);
	    }

	    if (stdoutFilename == null) {
	        stdoutFilename = STDOUT;
	    }
	    if (stderrFilename == null) {
	        stderrFilename = STDERR;
	    }
	    if (new File(outDir, stdoutFilename).exists()) {
	        addFileToOutputParameters(jobInfo, stdoutFilename, stdoutFilename, parentJobInfo);
	    }
	    if (new File(outDir, stderrFilename).exists()) {
	        addFileToOutputParameters(jobInfo, stderrFilename, stderrFilename, parentJobInfo);
	    }
	    if (stderrBuffer.length() > 0) {
	        writeStringToFile(outDirName, STDERR, stderrBuffer.toString());
	        addFileToOutputParameters(jobInfo, STDERR, STDERR, parentJobInfo);
	    }
	    if (taskLog != null) {
	        addFileToOutputParameters(jobInfo, TASKLOG, TASKLOG, parentJobInfo);
	    }

	    recordJobCompletion(jobInfo, parentJobInfo, jobStatus, jobStartTime);

	    if (!taskInfo.isVisualizer()) {
	        if (outputFiles.length == 0 && !new File(outDir, stderrFilename).exists()
	                && !new File(outDir, stdoutFilename).exists()) {
	            log.error("no output for " + taskName + " (job " + jobInfo.getJobNumber() + ").");
	        }
	    }
	    // IndexerDaemon.notifyJobComplete(jobInfo.getJobNumber());
	} 
	catch (Throwable e) {
	    if (e.getCause() != null) {
	        e = e.getCause();
	    }
	    log.error(taskName + " error: " + e);
	    try {
	        File outFile = writeStringToFile(outDirName, STDERR, e.getMessage() + "\n\n");
	        addFileToOutputParameters(jobInfo, STDERR, STDERR, parentJobInfo);

	        recordJobCompletion(jobInfo, parentJobInfo, JobStatus.JOB_ERROR, jobStartTime);
	    } catch (Exception e2) {
	        log.error(taskName + " error: unable to update job error status" + e2);
	    }
	    // IndexerDaemon.notifyJobComplete(jobInfo.getJobNumber());
	} finally {
	    // remove currPipelineUserKey from system memory
	    if (userKey != null && !userKey.equals("")) {
	        EncryptionUtil.getInstance().removePipelineUserKey(userKey);
	    }
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
                return 0;
            }
            return 1;
        }
    };
    /**
     * Sorted output files by lastModified() date.
     * @param outputFiles
     */
    private void sortOutputFiles(File[] outputFiles) {
        Arrays.sort(outputFiles, fileComparator);
    }


    /**
     * Record job completion status in the database.
     * 
     * @param jobInfo
     * @param parentJobInfo
     * @param jobStatus
     * @param jobStartTime
     */
    private void recordJobCompletion(JobInfo jobInfo, JobInfo parentJobInfo, int jobStatus, long jobStartTime) {
	try {
	    if (log.isDebugEnabled()) {
		log.debug("Recording job completion for job: " + jobInfo.getJobNumber() + " (" + jobInfo.getTaskName() + ")");
	    }
	    HibernateUtil.commitTransaction(); // TODO: JTL 8/21/07 oracle
	    HibernateUtil.beginTransaction();
	    long elapsedTime = (System.currentTimeMillis() - jobStartTime) / 1000;
	    Date now = new Date(Calendar.getInstance().getTimeInMillis());
	    updateJobInfo(jobInfo, parentJobInfo, jobStatus, now);
	    HibernateUtil.commitTransaction(); // TODO: JTL 8/21/07 oracle
	    HibernateUtil.beginTransaction(); // TODO: JTL 8/21/07 oracle

	    UsageLog.logJobCompletion(jobInfo, parentJobInfo, now, elapsedTime);
	    if (log.isDebugEnabled()) {
		log.debug("Recording job completion complete " + jobInfo.getJobNumber() + " (" + jobInfo.getTaskName() + ")");
	    }
	    HibernateUtil.commitTransaction();
	} catch (RuntimeException e) {
	    log.error("Rolling back transaction", e);
	    HibernateUtil.rollbackTransaction();
	}
    }

    /**
     * Get the appropriate command prefix to use for this module. The hierarchy goes like this; 1. task version specific
     * entry in task prefix mapping 2. task versionless entry in task prefix mapping 3. default command prefix
     * 
     * @param taskInfoAttributes
     * @param props
     */
    protected void setCommandPrefix(TaskInfoAttributes taskInfoAttributes) throws MalformedURLException {
	String lsidStr = taskInfoAttributes.get(LSID);
	PropertiesManager pm = PropertiesManager.getInstance();
	Properties tpm = pm.getTaskPrefixMapping();
	Properties prefixes = pm.getCommandPrefixes();

	String commandPrefixName = tpm.getProperty(lsidStr);
	String commandPrefix = null;

	if (commandPrefixName == null) {
	    LSID lsid = new LSID(lsidStr);
	    lsidStr = lsid.toStringNoVersion();
	    commandPrefixName = tpm.getProperty(lsidStr);

	}
	if (commandPrefixName != null) {
	    commandPrefix = prefixes.getProperty(commandPrefixName);
	}

	if (commandPrefix == null) {
	    // check for default prefix
	    commandPrefix = prefixes.getProperty("default", null);
	}

	if (commandPrefix != null && commandPrefix.length() > 0) {
	    taskInfoAttributes.put(COMMAND_LINE, commandPrefix + " " + taskInfoAttributes.get(COMMAND_LINE));
	}
    }

    private TaskInfo getTaskInfo(JobInfo jobInfo) throws Exception {
	if (log.isDebugEnabled()) {
	    log.debug("getTaskInfo for job: " + jobInfo.getJobNumber());
	}

	try {
	    HibernateUtil.beginTransaction();
	    AdminDAO ds = new AdminDAO();
	    TaskInfo taskInfo = ds.getTask(jobInfo.getTaskID());
	    if (taskInfo == null) {
		throw new Exception("No such taskID (" + jobInfo.getTaskID() + " for job " + jobInfo.getJobNumber());
	    }
	    HibernateUtil.commitTransaction();
	    return taskInfo;
	} catch (RuntimeException e) {
	    log.error("Error getting taskInfo for job: " + jobInfo.getJobNumber());
	    HibernateUtil.rollbackTransaction();
	    throw e;
	}
    }

    /**
     * Update AnalysisJob
     * 
     * @param jobInfo
     * @param parentJobInfo
     * @param jobStatus
     */
    private void updateJobInfo(JobInfo jobInfo, JobInfo parentJobInfo, int jobStatus, Date completionDate) {
	log.debug("Updating jobInfo");

	AnalysisJobDAO home = new AnalysisJobDAO();

	AnalysisJob aJob = home.findById(jobInfo.getJobNumber());
	aJob.setJobNo(jobInfo.getJobNumber());

	String paramString = jobInfo.getParameterInfo();
	if (jobStatus == JobStatus.JOB_ERROR || jobStatus == JobStatus.JOB_FINISHED || jobStatus == JobStatus.JOB_PROCESSING) {
	    paramString = ParameterFormatConverter.stripPasswords(paramString);
	}

	JobStatus newJobStatus = (new JobStatusDAO()).findById(jobStatus);
	aJob.setParameterInfo(paramString);
	aJob.setJobStatus(newJobStatus);
	aJob.setCompletedDate(completionDate);

	// TODO: JTL 8/21/07 oracle begin
	HibernateUtil.commitTransaction();
	HibernateUtil.isInTransaction();
	HibernateUtil.beginTransaction();
	// TODO: JTL 8/21/07 oracle end
	if (parentJobInfo != null) {
	    AnalysisJob parentJob = home.findById(parentJobInfo.getJobNumber());
	    parentJob.setCompletedDate(completionDate);
	}
	// TODO: JTL 8/21/07 oracle begin
	HibernateUtil.commitTransaction();
	HibernateUtil.isInTransaction();
	HibernateUtil.beginTransaction();
	// TODO: JTL 8/21/07 oracle end
    }

    private JobInfo getParentJobInfo(int jobNumber) {
	try {
	    HibernateUtil.beginTransaction();
	    JobInfo parentJI = getDS().getParent(jobNumber);
	    HibernateUtil.commitTransaction();
	    return parentJI;
	} catch (RuntimeException e) {
	    HibernateUtil.rollbackTransaction();
	    throw e;
	}
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

    protected static boolean validateCPU(String expected) throws Exception {
	String actual = System.getProperty("os.arch");
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
	if (System.getProperty(COMMAND_PREFIX, null) != null) {
	    return true; // don't validate for LSF
	}
	throw new Exception("Cannot run on this platform.  Task requires a " + expected + " CPU, but this is a " + actual);
    }

    protected static boolean validateOS(String expected, String action) throws Exception {
	String actual = System.getProperty("os.name");
	// eg. "Windows XP", "Linux", "Mac OS X", "OSF1"
	if (expected.equals("")) {
	    return true;
	}
	if (expected.equals(ANY)) {
	    return true;
	}
	if (expected.equalsIgnoreCase(actual)) {
	    return true;
	}
	String MicrosoftBeginning = "Windows"; // Windows XP, Windows ME,
	// Windows XP, Windows 2000, etc.
	if (expected.startsWith(MicrosoftBeginning) && actual.startsWith(MicrosoftBeginning)) {
	    return true;
	}
	if (System.getProperty(COMMAND_PREFIX, null) != null) {
	    return true; // don't validate for LSF
	}
	throw new Exception("Cannot " + action + " on this platform. Task requires a " + expected
		+ " operating system, but this server is running " + actual);
    }

    // check that each patch listed in the TaskInfoAttributes for this task is
    // installed.
    // if not, download and install it.
    // For any problems, throw an exception
    protected static boolean validatePatches(TaskInfo taskInfo, Status taskIntegrator) throws Exception {
	TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
	String requiredPatchLSID = tia.get(REQUIRED_PATCH_LSIDS);
	// no patches required?
	if (requiredPatchLSID == null || requiredPatchLSID.length() == 0) {
	    return true;
	}

	// some patches required, check which are already installed
	String[] requiredPatchLSIDs = requiredPatchLSID.split(",");
	String requiredPatchURL = tia.get(REQUIRED_PATCH_URLS);
	String[] patchURLs = (requiredPatchURL != null && requiredPatchURL.length() > 0 ? requiredPatchURL.split(",")
		: new String[requiredPatchLSIDs.length]);
	if (patchURLs != null && patchURLs.length != requiredPatchLSIDs.length) {
	    throw new Exception(taskInfo.getName() + " has " + requiredPatchLSIDs.length + " patch LSIDs but " + patchURLs.length
		    + " URLs");
	}
	eachRequiredPatch: for (int requiredPatchNum = 0; requiredPatchNum < requiredPatchLSIDs.length; requiredPatchNum++) {
	    String installedPatches = System.getProperty(INSTALLED_PATCH_LSIDS);
	    String[] installedPatchLSIDs = new String[0];
	    if (installedPatches != null) {
		installedPatchLSIDs = installedPatches.split(",");
	    }
	    requiredPatchLSID = requiredPatchLSIDs[requiredPatchNum];
	    LSID requiredLSID = new LSID(requiredPatchLSID);
	    log.debug("Checking whether " + requiredPatchLSID + " is already installed...");
	    for (int p = 0; p < installedPatchLSIDs.length; p++) {
		LSID installedLSID = new LSID(installedPatchLSIDs[p]);
		if (installedLSID.isEquivalent(requiredLSID)) {
		    // there are installed patches, and there is an LSID match
		    // to this one
		    log.info(requiredLSID.toString() + " is already installed");
		    continue eachRequiredPatch;
		}
	    }

	    // download and install this patch
	    installPatch(requiredPatchLSIDs[requiredPatchNum], patchURLs[requiredPatchNum], taskIntegrator);
	} // end of loop for each patch LSID for the task
	return true;
    }

    public static void installPatch(String requiredPatchLSID, String requiredPatchURL) throws Exception {
	String installedPatches = System.getProperty(INSTALLED_PATCH_LSIDS);
	String[] installedPatchLSIDs = new String[0];
	if (installedPatches != null) {
	    installedPatchLSIDs = installedPatches.split(",");
	}
	LSID requiredLSID = new LSID(requiredPatchLSID);
	log.debug("Checking whether " + requiredPatchLSID + " is already installed...");
	for (int p = 0; p < installedPatchLSIDs.length; p++) {
	    LSID installedLSID = new LSID(installedPatchLSIDs[p]);
	    if (installedLSID.isEquivalent(requiredLSID)) {
		// there are installed patches, and there is an LSID match to
		// this one
		log.info(requiredLSID.toString() + " is already installed");
		return;
	    }
	}
	installPatch(requiredPatchLSID, requiredPatchURL, null);
    }

    // install a specific patch, downloading a zip file with a manifest
    // containing a command line,
    // running that command line after substitutions, and recording the result
    // in the genepattern.properties patch registry
    public static void installPatch(String requiredPatchLSID, String requiredPatchURL, Status taskIntegrator) throws Exception {
	LSID patchLSID = new LSID(requiredPatchLSID);
	boolean wasNullURL = (requiredPatchURL == null || requiredPatchURL.length() == 0);
	if (wasNullURL) {
	    requiredPatchURL = System.getProperty(DEFAULT_PATCH_URL);
	}
	HashMap hmProps = new HashMap();
	if (wasNullURL) {
	    taskIntegrator.statusMessage("Fetching patch information from " + requiredPatchURL);
	    URL url = new URL(requiredPatchURL);
	    URLConnection connection = url.openConnection();
	    connection.setUseCaches(false);
	    if (connection instanceof HttpURLConnection) {
		connection.setDoOutput(true);
		PrintWriter pw = new PrintWriter(connection.getOutputStream());
		String[] patchQualifiers = System.getProperty("patchQualifiers", "").split(",");
		pw.print("patch");
		pw.print("=");
		pw.print(URLEncoder.encode(requiredPatchLSID, UTF8));
		for (int p = 0; p < patchQualifiers.length; p++) {
		    pw.print("&");
		    pw.print(URLEncoder.encode(patchQualifiers[p], UTF8));
		    pw.print("=");
		    pw.print(URLEncoder.encode(System.getProperty(patchQualifiers[p], ""), UTF8));
		}
		pw.close();
	    }
	    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(connection.getInputStream());
	    Element root = doc.getDocumentElement();
	    processNode(root, hmProps);
	    String result = (String) hmProps.get("result");
	    if (!result.equals("Success")) {
		throw new Exception("Error requesting patch: " + result + " in request for " + requiredPatchURL);
	    }
	    requiredPatchURL = (String) hmProps.get("site_module.url");
	}
	if (taskIntegrator != null) {
	    taskIntegrator.statusMessage("Downloading required patch from " + requiredPatchURL + "...");
	}
	String zipFilename = downloadPatch(requiredPatchURL, taskIntegrator, (String) hmProps.get("site_module.zipfilesize"));
	String patchName = patchLSID.getAuthority() + "." + patchLSID.getNamespace() + "." + patchLSID.getIdentifier() + "."
		+ patchLSID.getVersion();
	File patchDirectory = new File(System.getProperty("patches"), patchName);
	// if (taskIntegrator != null) taskIntegrator.statusMessage("Download
	// complete. Installing patch from " + zipFilename + " to " +
	// patchDirectory.getAbsolutePath() + ".");
	if (taskIntegrator != null) {
	    taskIntegrator.statusMessage("Installing patch from " + patchDirectory.getPath() + ".");
	}
	explodePatch(zipFilename, patchDirectory, taskIntegrator);
	new File(zipFilename).delete();

	// entire zip file has been exploded, now load the manifest, get the
	// command line, and execute it
	Properties props = loadManifest(patchDirectory);
	String nomDePatch = props.getProperty("name");
	String commandLine = getPatchCommandLine(props);
	// if (taskIntegrator != null) taskIntegrator.statusMessage("Running " +
	// commandLine + " in " + patchDirectory.getAbsolutePath());
	if (taskIntegrator != null) {
	    taskIntegrator.statusMessage("Running " + nomDePatch + " Installer.");
	}
	String exitValue = "" + executePatch(commandLine, patchDirectory, taskIntegrator);
	if (taskIntegrator != null) {
	    taskIntegrator.statusMessage("Patch installed, exit code " + exitValue);
	}
	String goodExitValue = props.getProperty(PATCH_SUCCESS_EXIT_VALUE, "0");
	String failureExitValue = props.getProperty(PATCH_ERROR_EXIT_VALUE, "");
	if (exitValue.equals(goodExitValue) || !exitValue.equals(failureExitValue)) {
	    recordPatch(requiredPatchLSID);
	    if (taskIntegrator != null) {
		taskIntegrator.statusMessage("Patch LSID recorded");
	    }

	    // keep the manifest file around for future reference
	    if (!new File(patchDirectory, MANIFEST_FILENAME).exists()) {
		explodePatch(zipFilename, patchDirectory, null, MANIFEST_FILENAME);
		if (props.getProperty(REQUIRED_PATCH_URLS, null) == null) {
		    try {
			File f = new File(patchDirectory, MANIFEST_FILENAME);
			Properties mprops = new Properties();
			mprops.load(new FileInputStream(f));
			mprops.setProperty(REQUIRED_PATCH_URLS, requiredPatchURL);
			mprops.store(new FileOutputStream(f), "added required patch");
		    } catch (IOException ioe) {
			ioe.printStackTrace();
		    }

		    // Properties properties = readPropertiesFile(f);
		    // properties = addProperty(properties, REQUIRED_PATCH_URLS,
		    // requiredPatchURL);
		    // writePropertiesFile(f, properties);
		}
	    }
	} else {
	    if (taskIntegrator != null) {
		taskIntegrator.statusMessage("Deleting patch directory after installation failure");
	    }
	    // delete patch directory
	    File[] old = patchDirectory.listFiles();
	    for (int i = 0; old != null && i < old.length; i++) {
		old[i].delete();
	    }
	    patchDirectory.delete();
	    throw new Exception("Could not install required patch: " + props.get("name") + "  " + props.get("LSID"));
	}
    }

    // download the patch zip file from a URL
    protected static String downloadPatch(String url, Status taskIntegrator, String contentLength) throws IOException {
	try {
	    long len = -1;
	    try {
		len = Long.parseLong(contentLength);
	    } catch (NullPointerException npe) {
		// ignore
	    } catch (NumberFormatException nfe) {
		// ignore
	    }
	    return downloadTask(url, taskIntegrator, len, false);
	    // return downloadTask(url, null, len); // null task integrator to
	    // suppress output
	} catch (IOException ioe) {
	    if (ioe.getCause() != null) {
		ioe = (IOException) ioe.getCause();
	    }
	    throw new IOException(ioe.toString() + " while downloading " + url);
	}
    }

    // unzip the patch files into their own directory
    protected static void explodePatch(String zipFilename, File patchDirectory, Status taskIntegrator) throws IOException {
	explodePatch(zipFilename, patchDirectory, taskIntegrator, null);
    }

    // unzip the patch files into their own directory
    protected static void explodePatch(String zipFilename, File patchDirectory, Status taskIntegrator, String zipEntryName)
	    throws IOException {
	ZipFile zipFile = new ZipFile(zipFilename);
	InputStream is = null;
	patchDirectory.mkdirs();
	if (zipEntryName == null) {
	    // clean out existing directory
	    File[] old = patchDirectory.listFiles();
	    for (int i = 0; old != null && i < old.length; i++) {
		old[i].delete();
	    }
	}
	for (Enumeration eEntries = zipFile.entries(); eEntries.hasMoreElements();) {
	    ZipEntry zipEntry = (ZipEntry) eEntries.nextElement();
	    if (zipEntryName != null && !zipEntryName.equals(zipEntry.getName())) {
		continue;
	    }
	    File outFile = new File(patchDirectory, zipEntry.getName());
	    if (zipEntry.isDirectory()) {
		if (taskIntegrator != null) {
		    taskIntegrator.statusMessage("Creating subdirectory " + outFile.getAbsolutePath());
		}
		outFile.mkdirs();
		continue;
	    }
	    is = zipFile.getInputStream(zipEntry);
	    OutputStream os = new FileOutputStream(outFile);
	    long fileLength = zipEntry.getSize();
	    // if (taskIntegrator != null)
	    // taskIntegrator.statusMessage("Extracting " + zipEntry.getName() +
	    // ", " + fileLength + " bytes");
	    long numRead = 0;
	    byte[] buf = new byte[100000];
	    int i;
	    while ((i = is.read(buf, 0, buf.length)) > 0) {
		os.write(buf, 0, i);
		numRead += i;
	    }
	    os.close();
	    os = null;
	    if (numRead != fileLength) {
		throw new IOException("only read " + numRead + " of " + fileLength + " bytes in " + zipFile.getName() + "'s "
			+ zipEntry.getName());
	    }
	    is.close();
	} // end of loop for each file in zip file
	zipFile.close();
    }

    // retrieve the command line from the patch manifest file and perform
    // <substitutions>
    protected static String getPatchCommandLine(Properties props) throws Exception {
	String commandLine = props.getProperty(COMMAND_LINE);
	Properties systemProps = new Properties(System.getProperties());
	if (commandLine == null || commandLine.length() == 0) {
	    throw new Exception("No command line defined in " + MANIFEST_FILENAME);
	}

	// command line substitutions for <ant>, etc.
	commandLine = substitute(commandLine, systemProps, null);
	commandLine = substitute(commandLine, systemProps, null);
	return commandLine;
    }

    // load the patch manifest file into a Properties object
    protected static Properties loadManifest(File patchDirectory) throws IOException {
	File manifestFile = new File(patchDirectory, MANIFEST_FILENAME);
	if (!manifestFile.exists()) {
	    throw new IOException(MANIFEST_FILENAME + " missing from patch " + patchDirectory.getName());
	}
	Properties props = new Properties();
	FileInputStream manifest = new FileInputStream(manifestFile);
	props.load(manifest);
	manifest.close();
	return props;
    }

    // run the patch command line in the patch directory, returning the exit
    // code from the executable
    protected static int executePatch(String commandLine, File patchDirectory, Status taskIntegrator) throws Exception {
	// spawn the command
	Process process = Runtime.getRuntime().exec(commandLine, null, patchDirectory);

	// BUG: there is race condition during a tiny time window between
	// the exec and the close
	// (the lines above and below this comment) during which it is
	// possible for an application
	// to imagine that there might be useful input coming from stdin.
	// This seemed to be
	// the case for Perl 5.0.1 on Wilkins, and might be a problem in
	// other applications as well.
	process.getOutputStream().close(); // there is no stdin to feed to
	// the program. So if it asks,
	// let it see EOF!

	// create threads to read from the command's stdout and stderr
	// streams

	Thread outputReader = (taskIntegrator != null) ? antStreamCopier(process.getInputStream(), taskIntegrator)
		: streamCopier(process.getInputStream(), System.out);
	Thread errorReader = (taskIntegrator != null) ? antStreamCopier(process.getErrorStream(), taskIntegrator) : streamCopier(
		process.getInputStream(), System.err);

	// drain the output and error streams
	outputReader.start();
	errorReader.start();

	// wait for all output
	outputReader.join();
	errorReader.join();

	// the process will be dead by now
	process.waitFor();
	int exitValue = process.exitValue();
	return exitValue;
    }

    // record the patch LSID in the genepattern.properties file
    public static synchronized void recordPatch(String patchLSID) throws IOException {
	// add this LSID to the installed patches repository
	String installedPatches = System.getProperty(INSTALLED_PATCH_LSIDS);
	if (installedPatches == null || installedPatches.length() == 0) {
	    installedPatches = "";
	} else {
	    installedPatches = installedPatches + ",";
	}
	installedPatches = installedPatches + patchLSID;
	// String properties = readGenePatternProperties();
	// properties = addProperty(properties, INSTALLED_PATCH_LSIDS,
	// installedPatches);
	// writeGenePatternProperties(properties);
	System.setProperty(INSTALLED_PATCH_LSIDS, installedPatches);
	Properties props = new Properties();
	props.load(new FileInputStream(new File(System.getProperty("resources"), "genepattern.properties")));

	// make sure any changes are properly set in the System props
	props.setProperty(INSTALLED_PATCH_LSIDS, installedPatches);
	props.store(new FileOutputStream(new File(System.getProperty("resources"), "genepattern.properties")),
		"added installed patch LSID");

	for (Iterator iter = props.keySet().iterator(); iter.hasNext();) {
	    String k = (String) iter.next();
	    String v = (String) props.get(k);
	    System.setProperty(k, v);
	}

    }

    /**
     * // read the genepattern.properties file into a String (preserving comments!) public static String
     * readGenePatternProperties() throws IOException { File gpPropertiesFile = new
     * File(System.getProperty("resources"), "genepattern.properties"); return readPropertiesFile(gpPropertiesFile); }
     * // read the genepattern.properties file into a String (preserving comments!) protected static String
     * readPropertiesFile(File propertiesFile) throws IOException { FileReader fr = new FileReader(propertiesFile); char
     * buf[] = new char[(int)propertiesFile.length()]; int len = fr.read(buf, 0, buf.length); fr.close(); String
     * properties = new String(buf, 0, len); return properties; } // write a String as a genepattern.properties file
     * (preserving comments) public static void writeGenePatternProperties(String properties) throws IOException { File
     * gpPropertiesFile = new File(System.getProperty("resources"), "genepattern.properties");
     * writePropertiesFile(gpPropertiesFile, properties); }
     * <p/>
     * protected static void writePropertiesFile(File propertiesFile, String properties) throws IOException { FileWriter
     * fw = new FileWriter(propertiesFile, false); fw.write(properties); fw.close(); } // add or set the value of a
     * particular key in the String representation of a properties file public static String addProperty(String
     * properties, String key, String value) { int ipStart = properties.indexOf(key + "="); if (ipStart == -1) {
     * properties = properties + System.getProperty("line.separator") + key + "=" + value +
     * System.getProperty("line.separator"); } else { int ipEnd =
     * properties.indexOf(System.getProperty("line.separator"), ipStart); properties = properties.substring(0, ipStart +
     * key.length() + "=".length()) + value; if (ipEnd != -1) properties = properties + "," +
     * properties.substring(ipEnd); } return properties; }
     */

    protected static void processNode(Node node, HashMap hmProps) {
	if (node.getNodeType() == Node.ELEMENT_NODE) {
	    Element c_elt = (Element) node;
	    String nodeValue = c_elt.getFirstChild().getNodeValue();
	    log.debug("GPAT.processNode: adding " + c_elt.getTagName() + "=" + nodeValue);
	    hmProps.put(c_elt.getTagName(), nodeValue);
	    NamedNodeMap attributes = c_elt.getAttributes();
	    if (attributes != null) {
		for (int i = 0; i < attributes.getLength(); i++) {
		    String attrName = ((Attr) attributes.item(i)).getName();
		    String attrValue = ((Attr) attributes.item(i)).getValue();
		    log.debug("GPAT.processNode: adding " + c_elt.getTagName() + "." + attrName + "=" + attrValue);
		    hmProps.put(c_elt.getTagName() + "." + attrName, attrValue);
		}
	    }
	} else {
	    log.debug("non-Element node: " + node.getNodeName() + "=" + node.getNodeValue());
	}
	NodeList childNodes = node.getChildNodes();
	for (int i = 0; i < childNodes.getLength(); i++) {
	    processNode(childNodes.item(i), hmProps);
	}
    }

    // copy an InputStream to a PrintStream until EOF
    public static Thread streamCopier(final InputStream is, final PrintStream ps) throws IOException {
	// create thread to read from the a process' output or error stream
	return new Thread(new Runnable() {
	    public void run() {
		BufferedReader in = new BufferedReader(new InputStreamReader(is));
		String line;
		try {
		    while ((line = in.readLine()) != null) {
			ps.println(line);
			ps.flush();
		    }
		} catch (IOException ioe) {
		    System.err.println(ioe + " while reading from process stream");
		}
	    }
	});
    }

    // copy an InputStream to a PrintStream until EOF
    public static Thread streamCopier(final InputStream is, final Status taskIntegrator) throws IOException {
	// create thread to read from the a process' output or error stream
	return new Thread(new Runnable() {
	    public void run() {
		BufferedReader in = new BufferedReader(new InputStreamReader(is));
		String line;
		try {
		    while ((line = in.readLine()) != null) {
			if (taskIntegrator != null && line != null) {
			    taskIntegrator.statusMessage(line);
			}
		    }
		} catch (IOException ioe) {
		    System.err.println(ioe + " while reading from process stream");
		}
	    }
	});
    }

    // copy an InputStream to a PrintStream until EOF
    public static Thread antStreamCopier(final InputStream is, final Status taskIntegrator) throws IOException {
	// create thread to read from the a process' output or error stream
	return new Thread(new Runnable() {
	    public void run() {
		BufferedReader in = new BufferedReader(new InputStreamReader(is));
		String line;
		try {
		    while ((line = in.readLine()) != null) {
			int idx = 0;
			if ((idx = line.indexOf("[echo]")) >= 0) {
			    line = line.substring(idx + 6);
			}
			if (taskIntegrator != null && line != null) {
			    taskIntegrator.statusMessage(line);
			}
		    }
		} catch (IOException ioe) {
		    System.err.println(ioe + " while reading from process stream");
		}
	    }
	});
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

    public static String substitute(String commandLine, Properties props, ParameterInfo[] params) {
	if (commandLine == null) {
	    return null;
	}
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
		// if there's a space in the name, then it's a redirection of
		// stdin
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
		// special treatment: make this an absolute path so that
		// pipeline jobs running in their own directories see the right
		// path
		replacement = new File(replacement).getAbsolutePath();
	    }
	    if (varName.equals("userid")) { // special treatment to catch spaces
		// in names
		replacement = props.getProperty(varName);
		int idx = replacement.indexOf(" ");
		if (idx >= 0) {
		    replacement = "\"" + replacement + "\"";
		}
	    }
	    if (replacement.length() == 0) {
		log.debug("GPAT.substitute: replaced " + varName + " with empty string");
	    }
	    p = htParams.get(varName);
	    if (p != null) {
		HashMap hmAttributes = p.getAttributes();
		if (hmAttributes != null) {
		    if (hmAttributes.get(PARAM_INFO_OPTIONAL[PARAM_INFO_NAME_OFFSET]) == null) {
			isOptional = false;
		    }
		    String optionalPrefix = (String) hmAttributes.get(PARAM_INFO_PREFIX[PARAM_INFO_NAME_OFFSET]);
		    if (replacement.length() > 0 && optionalPrefix != null && optionalPrefix.length() > 0) {
			replacement = optionalPrefix + replacement;
		    }
		}
	    }
	    if (replacement.indexOf("Program Files") != -1) {
		replacement = replace(replacement, "Program Files", "Progra~1");
	    }
	    newString = newString.replace(start - LEFT_DELIMITER.length(), end + RIGHT_DELIMITER.length(), replacement);
	    start = start + replacement.length() - LEFT_DELIMITER.length();
	}
	if (newString.length() == 0 && isOptional) {
	    return null;
	}
	return newString.toString();
    }

    /**
     * Deletes a task, by name, from the Omnigene task_master database.
     * 
     * @param lsid
     *            name of task to delete
     * @author Jim Lerner
     */
    public static void deleteTask(String lsid) throws OmnigeneException, RemoteException {
	String username = null;
	TaskInfo ti = GenePatternAnalysisTask.getTaskInfo(lsid, username);
	File libdir = null;
	try {
	    libdir = new File(DirectoryManager.getTaskLibDir(ti.getName(), (String) ti.getTaskInfoAttributes().get(LSID),
		    username));
	} catch (Exception e) {
	    // ignore
	}
	GenePatternTaskDBLoader loader = new GenePatternTaskDBLoader(lsid, null, null, null, username, 0);
	int formerID = loader.getTaskIDByName(lsid, username);
	loader.run(GenePatternTaskDBLoader.DELETE);
	try {
	    // remove taskLib directory for this task
	    if (libdir != null) {
		libdir.delete();
	    }
	    // delete all searchable indexes for this task
	    // Indexer.deleteTask(formerID);
	} catch (Exception ioe) {
	    System.err.println(ioe + " while deleting taskLib and search index for task " + ti.getName());
	}
    }

    /**
     * Provides a TreeMap, sorted by case-insensitive task name, of all of the tasks registered in the task_master table
     * that are handled by the GenePatternAnalysisTask class.
     * 
     * @return TreeMap whose key is task name, and whose value is a TaskInfo object (with nested TaskInfoAttributes and
     *         ParameterInfo[]).
     * @author Jim Lerner
     */
    public static Collection getTasks() throws OmnigeneException, RemoteException {
	return getTasks(null);
    }

    public static AnalysisDAO getDS() {
	return new AnalysisDAO();
    }

    /**
     * getTasks for a specific userID returns a TreeMap of all of the GenePatternAnalysisTask-supported tasks that are
     * visible to a particular userID. Tasks are presented in case-insensitive alphabetical order.
     * 
     * @param userID
     *            userID controlling which private tasks will be returned. All public tasks are also returned, and are
     *            interleaved alphabetically with the private tasks.
     * @return TreeMap whose key is task name, and whose value is a TaskInfo object (with nested TaskInfoAttributes and
     *         ParameterInfo[]).
     * @author Jim Lerner
     */

    public static List getTasksSorted(String userID) throws OmnigeneException, RemoteException {
	List vTasks = getTasks(userID); // get vector of TaskInfos
	if (vTasks != null) {
	    Collections.sort(vTasks, new Comparator() {
		// case-insensitive compare on task name, then LSID
		public int compare(Object o1, Object o2) {
		    TaskInfo t1 = (TaskInfo) o1;
		    TaskInfo t2 = (TaskInfo) o2;
		    int c = t1.getName().compareToIgnoreCase(t2.getName());
		    if (c == 0) {
			String lsid1 = t1.giveTaskInfoAttributes().get(LSID);
			String lsid2 = t2.giveTaskInfoAttributes().get(LSID);
			if (lsid1 == null) {
			    return 1;
			}
			if (lsid2 == null) {
			    return -1;
			}
			return -lsid1.compareToIgnoreCase(lsid2);
		    }
		    return c;
		}
	    });
	}
	return vTasks;
    }

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
		GenePatternTaskDBLoader loader = new GenePatternTaskDBLoader(taskName, null, null, null, username, 0);
		taskID = loader.getTaskIDByName(taskName, username);
		if (taskID != -1) {
		    taskInfo = (new AdminDAO()).getTask(taskID);
		}
	    } catch (OmnigeneException e) {
		// this is a new task, no taskID exists
		// do nothing
		throw new OmnigeneException("no such task: " + taskName + " for user " + username);
	    } catch (RemoteException re) {
		throw new OmnigeneException("Unable to load the " + taskName + " task: " + re.getMessage());
	    }
	} catch (Exception e) {
	    throw new OmnigeneException(e.getMessage() + " in getTaskInfo(" + taskName + ", " + username + ")");
	}
	return taskInfo;
    }

    /**
     * Fill returned Properties with everything that the user can get a substitution for, including all
     * System.getProperties() properties plus all of the actual ParameterInfo name/value pairs.
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
     */
    public Properties setupProps(String taskName, int parentJobNumber, int jobNumber, int taskID,
	    TaskInfoAttributes taskInfoAttributes, ParameterInfo[] actuals, Map<String, String> env,
	    ParameterInfo[] formalParameters, String userID) throws Exception {
	Properties props = new Properties();
	int formalParamsLength = 0;
	if (formalParameters != null) {
	    formalParamsLength = formalParameters.length;
	}
	try {
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
	    props.put(NAME, taskName);
	    props.put(JOB_ID, Integer.toString(jobNumber));
	    props.put("parent_" + JOB_ID, Integer.toString(parentJobNumber));
	    props.put(TASK_ID, Integer.toString(taskID));
	    props.put(USERID, "" + userID);
	    props.put(PIPELINE_ARG_STOP_AFTER_TASK_NUM, ""); // should be
	    // overridden by
	    // actuals if
	    // provided

	    String sLSID = taskInfoAttributes.get(LSID);
	    props.put(LSID, sLSID);

	    // as a convenience to the user, create a <libdir> property which is
	    // where DLLs, JARs, EXEs, etc. are dumped to when adding tasks
	    String taskLibDir = (taskID != -1 ? new File(DirectoryManager.getTaskLibDir(taskName, sLSID, userID)).getPath()
		    + System.getProperty("file.separator") : "taskLibDir");
	    props.put(LIBDIR, taskLibDir);

	    // set the java flags if they have been overridden in the java_flags.properties file
	    PropertiesManager pm = PropertiesManager.getInstance();
	    Properties javaFlagProps = pm.getJavaFlags();
	    String javaFlags = javaFlagProps.getProperty(sLSID);
	    if (javaFlags == null) {
		LSID lsid = new LSID(sLSID);
		javaFlags = javaFlagProps.getProperty(lsid.toStringNoVersion());
	    }
	    if (javaFlags != null)
		props.put(GPConstants.JAVA_FLAGS, javaFlags);

	    // populate props with the input parameters so that they can be
	    // looked up by name
	    if (actuals != null) {
		for (int i = 0; i < actuals.length; i++) {
		    value = actuals[i].getValue();
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
	    if (actuals != null) {
		for (int i = 0; i < actuals.length; i++) {
		    for (int f = 0; f < formalParamsLength; f++) {
			if (actuals[i].getName().equals(formalParameters[f].getName())) {
			    if (formalParameters[f].isInputFile()) {
				inputFilename = actuals[i].getValue();
				if (inputFilename == null || inputFilename.length() == 0) {
				    continue;
				}
				inputParamName = actuals[i].getName();
				File inFile = new File(outDirName, new File(inputFilename).getName());
				if (inputFileMode != INPUT_FILE_MODE.PATH) {
				    // file is moved to job directory
				    props.put(inputParamName, inFile.getName());
				}
				props.put(inputParamName + INPUT_PATH, new String(outDirName));

				String baseName = inFile.getName();
				if (baseName.startsWith("Axis")) {
				    // strip off the AxisNNNNNaxis_ prefix
				    if (baseName.indexOf("_") != -1) {
					baseName = baseName.substring(baseName.indexOf("_") + 1);
				    }
				}
				props.put(inputParamName + INPUT_FILE, new String(baseName)); // filename
				// without path
				j = baseName.lastIndexOf(".");
				if (j != -1) {
				    props.put(inputParamName + INPUT_EXTENSION, new String(baseName.substring(j + 1))); // filename
				    // extension
				    baseName = baseName.substring(0, j);
				} else {
				    props.put(inputParamName + INPUT_EXTENSION, ""); // filename
				    // extension
				}
				if (inputFilename.startsWith("http:") || inputFilename.startsWith("https:")
					|| inputFilename.startsWith("ftp:")) {
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
				props.put(inputParamName + INPUT_BASENAME, new String(baseName)); // filename
				// without path
				// or extension
			    }
			    break;
			}
		    }
		}
	    }
	    return props;
	} catch (NullPointerException npe) {
	    log.error(npe + " in setupProps.  Currently have:\n" + props);
	    throw npe;
	}
    }

    /**
     * Takes care of quotes in command line. Ensures that quoted arguments are placed into a single element in the
     * command array
     * 
     * @param commandLine
     * @return the new command line
     */
    private static String[] translateCommandline(String[] commandLine) {
	if (commandLine == null || commandLine.length == 0) {
	    return commandLine;
	}
	ArrayList v = new ArrayList();
	int end = commandLine.length;
	int i = 0;
	while (i < end) {
	    // read until find another "
	    if (commandLine[i].charAt(0) == '"' && commandLine[i].charAt(commandLine[i].length() - 1) != '"') {
		StringBuffer buf = new StringBuffer();
		buf.append(commandLine[i].substring(1, commandLine[i].length()));
		i++;
		boolean foundEndQuote = false;
		while (i < end && !foundEndQuote) {
		    foundEndQuote = commandLine[i].charAt(commandLine[i].length() - 1) == '"';
		    buf.append(" ");
		    buf.append(commandLine[i].substring(0, commandLine[i].length() - 1));
		    i++;
		}
		if (!foundEndQuote) {
		    throw new IllegalArgumentException("Missing end quote");
		}
		v.add(buf.toString());
	    } else {
		v.add(commandLine[i]);
		i++;
	    }
	}
	return (String[]) v.toArray(new String[0]);
    }

    /**
     * Spawns a separate process to execute the requested analysis task. It copies the stdout and stderr output streams
     * to StringBuffers so that they can be returned to the invoker. The stdin input stream is closed immediately after
     * execution in order to ensure that the running task has no misconceptions about being able to read anything from
     * it. runCommand maintains entries in the htRunningJobs Hashtable whose keys are jobIDs and whose values are
     * running Process objects. This allows Processes to be stopped by jobID.
     * <p/>
     * <p/>
     * Please read about the BUG in the runCommand comments related to a race condition in the closure of the stdin
     * stream after forking the process.
     * 
     * @param commandLine
     *            String representation of the command line to run with all substitutions for parameters made.
     * @param env
     *            Hashtable of environment name/value pairs. Used to provide the environment to the exec method,
     *            including the modified PATH value.
     * @param runDir
     *            The directory in which to start the process running (it will be a temporary directory with only input
     *            files in it).
     * @param stdoutFile
     *            file to capture stdout output from the running process
     * @param stderrFile
     *            file to capture stderr output from the running process
     * @param jobInfo
     *            JobInfo object for this instance
     * @param stdin
     *            file path that is set to standard input of the running process or <tt>null</tt>
     * @param stderrBuffer
     *            buffer to append GenePattern errors to
     * @author Jim Lerner
     */
    protected void runCommand(String commandLine[], Map<String, String> environmentVariables, File runDir, File stdoutFile,
	    File stderrFile, JobInfo jobInfo, String stdin, StringBuffer stderrBuffer) {
	Process process = null;
	String jobID = null;
	try {
	    if (false) { // for debugging only
	        String[] debugCmdLine = new String[commandLine.length + 3];
	        debugCmdLine[0] = commandLine[0];
	        debugCmdLine[1] = "-Xdebug";
	        debugCmdLine[2] = "-Xnoagent";
	        debugCmdLine[3] = "-Xrunjdwp:transport=dt_socket,server=y,address=5001,suspend=y";
	        for (int i = 1; i < commandLine.length; ++i) {
	            debugCmdLine[i + 3] = commandLine[i];
	        }
	        commandLine = debugCmdLine;
	    }
	    commandLine = translateCommandline(commandLine);

	    ProcessBuilder pb = new ProcessBuilder(commandLine);
	    Map<String, String> env = pb.environment();
	    env.putAll(environmentVariables);
	    pb.directory(runDir);

	    // spawn the command
	    process = pb.start();
	    // BUG: there is race condition during a tiny time window between
	    // the exec and the close
	    // (the lines above and below this comment) during which it is
	    // possible for an application
	    // to imagine that there might be useful input coming from stdin.
	    // This seemed to be
	    // the case for Perl 5.0.1, and might be a problem in
	    // other applications as well.
	    OutputStream standardInStream = process.getOutputStream();
	    if (stdin == null) {
		standardInStream.close();
	    } else {
		byte[] b = new byte[2048];
		int bytesRead;
		FileInputStream fis = null;
		try {
		    fis = new FileInputStream(new File(runDir, stdin));
		    while ((bytesRead = fis.read(b)) >= 0) {
			standardInStream.write(b, 0, bytesRead);
		    }
		} finally {
		    if (fis != null) {
			fis.close();
		    }
		    standardInStream.close();
		}
	    }
	    jobID = "" + jobInfo.getJobNumber();
	    htRunningJobs.put(jobID, process);

	    // create threads to read from the command's stdout and stderr
	    // streams
	    Thread outputReader = streamToFile(process.getInputStream(), stdoutFile);
	    Thread errorReader = streamToFile(process.getErrorStream(), stderrFile);

	    // drain the output and error streams
	    outputReader.start();
	    errorReader.start();

	    // wait for all output before attempting to send it back to the
	    // client
	    outputReader.join();
	    errorReader.join();

	    // the process will be dead by now

	    process.waitFor();
	    // TODO: cleanup input file(s)
	} catch (Throwable t) {
	    log.error(t + " in runCommand, reporting to stderr");
	    stderrBuffer.append(t.toString());
	} finally {
	    if (jobID != null) {
		htRunningJobs.remove(jobID);
	    }
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
    protected void addFileToOutputParameters(JobInfo jobInfo, String fileName, String label, JobInfo parentJobInfo) {
	fileName = jobInfo.getJobNumber() + "/" + fileName;
	ParameterInfo paramOut = new ParameterInfo(label, fileName, "");
	paramOut.setAsOutputFile();
	jobInfo.addParameterInfo(paramOut);
	if (parentJobInfo != null) {
	    parentJobInfo.addParameterInfo(paramOut);
	}
    }

    /**
     * takes a jobID and a Hashtable in which the jobID is putatively listed, and attempts to terminate the job. Note
     * that Process.destroy() is not always successful. If a process cannot be killed without a "kill -9", it seems not
     * to die from a Process.destroy() either.
     * 
     * @param jobID
     *            JobInfo jobID number
     * @param htWhere
     *            Hashtable in which the job was listed when it was invoked
     * @return true if the job was found, false if not listed (already deleted)
     * @author Jim Lerner
     */
    protected static boolean terminateJob(String jobID, Hashtable htWhere) {
	Process p = (Process) htWhere.get(jobID);
	if (p != null) {
	    p.destroy();
	}
	return (p != null);
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
    protected Vector<String> validateParameters(Properties props, String taskName, String commandLine,
	    ParameterInfo[] actualParams, ParameterInfo[] formalParams, boolean enforceOptionalNonBlank) {
	Vector<String> vProblems = new Vector<String>();
	String name;
	boolean runtimeValidation = (actualParams != formalParams);
	int formalParamsLength = 0;
	if (formalParams != null) {
	    formalParamsLength = formalParams.length;
	}
	// validate R-safe task name
	if (!isRSafe(taskName)) {
	    vProblems
		    .add("'"
			    + taskName
			    + "' is not a legal task name.  It must contain only letters, digits, and periods, and may not begin with a period or digit.\n It must not be a reserved keyword in R ('if', 'else', 'repeat', 'while', 'function', 'for', 'in', 'next', 'break', 'true', 'false', 'null', 'na', 'inf', 'nan').");
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

		// for non-optional parameters, make sure they are mentioned in
		// the command line
		if (hmAttributes == null || hmAttributes.get(PARAM_INFO_OPTIONAL[PARAM_INFO_NAME_OFFSET]) == null
			|| ((String) hmAttributes.get(PARAM_INFO_OPTIONAL[PARAM_INFO_NAME_OFFSET])).length() == 0) {
		    if (commandLine.indexOf(name) == -1) {
			vProblems.add(taskName + ": non-optional parameter " + name + " is not cited in the command line.");
		    } else if (enforceOptionalNonBlank
			    && (actualParams[actual].getValue() == null || actualParams[actual].getValue().length() == 0)
			    && formalParams[formal].getValue().length() == 0) {
			vProblems.add(taskName + ": non-optional parameter " + name + " is blank.");
		    }
		}
		// check that parameter is not named the same as a predefined
		// parameter
		for (int j = 0; j < RESERVED_PARAMETER_NAMES.length; j++) {
		    if (actualParams[actual].getName().equalsIgnoreCase(RESERVED_PARAMETER_NAMES[j])) {
			vProblems.add(taskName + ": parameter " + name
				+ " is a reserved name and cannot be used as a parameter name.");
		    }
		}

		// if the parameter is part of a choice list, verify that the
		// default is on the list
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
		    if (!foundActual) {
			vProblems.add("Value '" + actualValue + "' for parameter " + name + " was not found in the choice list '"
				+ choices + "'.");
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
    protected Vector<String> validateSubstitutions(Properties props, 
            String taskName, String commandLine,
	    String source, Vector<String> vProblems, ParameterInfo[] formalParams) {
	// check that each substitution variable listed in the command line is
	// actually in props
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
    public static Vector<String> validateInputs(TaskInfo taskInfo, String taskName, TaskInfoAttributes tia, ParameterInfo[] params) {
	GenePatternAnalysisTask gp = new GenePatternAnalysisTask();
	Vector<String> vProblems = null;
	try {
	    Properties props = gp.setupProps(taskName, -1, 0, -1, tia, params, new HashMap<String, String>(), params, null);
	    vProblems = gp.validateParameters(props, taskName, tia.get(COMMAND_LINE), params, params, false);
	} catch (Exception e) {
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
     * @param varName
     *            proposed variable name
     * @return boolean if the proposed name is R-legal
     * @author Jim Lerner
     */
    public static boolean isRSafe(String varName) {
	// anything but letters, digits, and period is an invalid R identifier
	// that must be quoted
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
     * @param varName
     *            variable name
     * @return variable name, quoted if necessary
     * @author Jim Lerner
     */
    public static String rEncode(String varName) {
	// anything but letters, digits, and period is an invalid R identifier
	// that must be quoted
	if (isRSafe(varName)) {
	    return varName;
	} else {
	    return "\"" + replace(varName, "\"", "\\\"") + "\"";
	}
    }

    /**
     * marshalls all of the attributes which make up a task (name, description, TaskInfoAttributes, ParameterInfo[]),
     * validates that they will ostensibly work (parameter substitutions all accounted for), and creates a new or
     * updated task database entry (via a DBLoader invocation). If there are validation errors, the task is not created
     * and the error message(s) are returned to the caller. Otherwise (all okay), null is returned.
     * 
     * @param name
     *            task name
     * @param description
     *            description of task
     * @param params
     *            ParameterInfo[] of formal parameters for the task
     * @param taskInfoAttributes
     *            GenePattern TaskInfoAttributes describing metadata for the task
     * @return Vector of String error messages if there was an error validating the command line and input parameters,
     *         otherwise null to indicate success
     * @throws OmnigeneException
     *             if DBLoader is unhappy when connecting to Omnigene
     * @throws RemoteException
     *             if DBLoader is unhappy when connecting to Omnigene
     * @author Jim Lerner
     */
    public static Vector installTask(String name, String description, ParameterInfo[] params,
	    TaskInfoAttributes taskInfoAttributes, String username, int access_id, Status taskIntegrator)
	    throws OmnigeneException, RemoteException {
	String originalUsername = username;
	TaskInfo taskInfo = new TaskInfo();
	taskInfo.setName(name);
	taskInfo.setDescription(description);
	taskInfo.setUserId(username);
	taskInfo.setTaskInfoAttributes(taskInfoAttributes);
	taskInfo.setParameterInfoArray(params);
	Vector vProblems = GenePatternAnalysisTask.validateInputs(taskInfo, name, taskInfoAttributes, params);

	try {
	    String expected = taskInfoAttributes.get(OS);
	    if (validateOS(expected, "install " + name)) {
		validatePatches(taskInfo, taskIntegrator);
	    }
	} catch (Throwable e) {
	    if (e.getCause() != null) {
		e = e.getCause();
	    }
	    System.err.println(e.toString() + " while installing " + name);
	    vProblems.add(e.getMessage());
	}
	if (vProblems.size() > 0) {
	    return vProblems;
	}

	// System.out.println("GPAT.installTask: installing " + name + " with
	// LSID " + taskInfoAttributes.get(LSID));

	// privacy is stored both in the task_master table as a field, and in
	// the taskInfoAttributes
	taskInfoAttributes.put(PRIVACY, access_id == ACCESS_PRIVATE ? PRIVATE : PUBLIC);
	if (access_id == ACCESS_PRIVATE) {
	    taskInfoAttributes.put(USERID, username);
	}
	String lsid = taskInfoAttributes.get(LSID);
	if (lsid == null || lsid.equals("")) {
	    // System.out.println("installTask: creating new LSID");
	    lsid = LSIDManager.getInstance().createNewID(TASK_NAMESPACE).toString();
	    taskInfoAttributes.put(LSID, lsid);
	}

	// TODO: if the task is a pipeline, generate the serialized model right
	// now too
	GenePatternTaskDBLoader loader = new GenePatternTaskDBLoader(name, description, params, taskInfoAttributes.encode(),
		username, access_id);
	int formerID = loader.getTaskIDByName(lsid, originalUsername);
	boolean isNew = (formerID == -1);
	if (!isNew) {
	    try {
		// delete the search engine indexes for this task so that it
		// will be reindexed
		log.debug("installTask: deleting index for previous task ID " + formerID);
		// Indexer.deleteTask(formerID);
		log.debug("installTask: deleted index");
	    } catch (Exception ioe) {
		log.info(ioe + " while deleting search index for task " + name + " during update");
		System.err.println(ioe + " while deleting search index for task " + name + " during update");
	    }
	}
	loader.run(isNew ? GenePatternTaskDBLoader.CREATE : GenePatternTaskDBLoader.UPDATE);
	// IndexerDaemon.notifyTaskUpdate(loader.getTaskIDByName(LSID != null ? lsid : name, username));
	return null;
    }

    public static LSID getNextTaskLsid(String requestedLSID) throws java.rmi.RemoteException {
	LSID taskLSID = null;
	if (requestedLSID != null && requestedLSID.length() > 0) {
	    try {
		taskLSID = new LSID(requestedLSID);
	    } catch (MalformedURLException mue) {
		mue.printStackTrace();
		// XXX what to do here? Create a new one from scratch!
	    }
	}
	LSIDManager lsidManager = LSIDManager.getInstance();
	if (taskLSID == null) {
	    // System.out.println("installNewTask: creating new LSID");
	    taskLSID = lsidManager.createNewID(TASK_NAMESPACE);
	} else if (lsidManager.getAuthority().equalsIgnoreCase(taskLSID.getAuthority())) {
	    taskLSID = lsidManager.getNextIDVersion(requestedLSID);
	} else {
	    taskLSID = lsidManager.createNewID(TASK_NAMESPACE);
	}

	return taskLSID;
    }

    /**
     * use installTask but first manage the LSID. if it has one, keep it unchanged. If not, create a new one to be used
     * when creating a new task or installing from a zip file
     */
    public static String installNewTask(String name, String description, ParameterInfo[] params,
	    TaskInfoAttributes taskInfoAttributes, String username, int access_id, Status taskIntegrator)
	    throws OmnigeneException, RemoteException, TaskInstallationException {
	LSID taskLSID = null;
	String requestedLSID = taskInfoAttributes.get(LSID);

	taskLSID = getNextTaskLsid(requestedLSID);

	taskInfoAttributes.put(GPConstants.LSID, taskLSID.toString());
	// System.out.println("GPAT.installNewTask: new LSID=" +
	// taskLSID.toString());
	Vector probs = installTask(name, description, params, taskInfoAttributes, username, access_id, taskIntegrator);
	if ((probs != null) && (probs.size() > 0)) {
	    throw new TaskInstallationException(probs);
	}
	return taskLSID.toString();
    }

    /**
     * use installTask but first manage LSID, If it has a local one, update the version. If it has an external LSID
     * create a new one. Used when modifying an existing task in an editor
     */
    public static String updateTask(String name, String description, ParameterInfo[] params,
	    TaskInfoAttributes taskInfoAttributes, String username, int access_id) throws OmnigeneException, RemoteException,
	    TaskInstallationException {
	LSID requestedLSID = null;
	try {
	    // System.out.println("updateTask: old LSID=" +
	    // taskInfoAttributes.get(LSID));
	    requestedLSID = new LSID(taskInfoAttributes.get(LSID));
	} catch (MalformedURLException mue) {
	    mue.printStackTrace();
	    // XXX what to do here?
	    System.err.println("updateTask: " + mue);
	}

	LSID taskLSID = getNextTaskLsid(requestedLSID.toString());
	LSIDManager lsidManager = LSIDManager.getInstance();
	if (!lsidManager.getAuthority().equalsIgnoreCase(taskLSID.getAuthority())) {

	    String provenance = taskInfoAttributes.get(GPConstants.LSID_PROVENANCE);
	    provenance = provenance + "  " + taskLSID.toString();
	    taskInfoAttributes.put(GPConstants.LSID_PROVENANCE, provenance);
	}
	taskInfoAttributes.put(LSID, taskLSID.toString());

	Vector probs = installTask(name, description, params, taskInfoAttributes, username, access_id, null);
	if ((probs != null) && (probs.size() > 0)) {
	    throw new TaskInstallationException(probs);
	}
	return taskLSID.toString();
    }

    public static boolean taskExists(String taskName, String user) throws OmnigeneException {
	TaskInfo existingTaskInfo = null;
	try {
	    existingTaskInfo = GenePatternAnalysisTask.getTaskInfo(taskName, user);
	} catch (OmnigeneException oe) {
	    // ignore
	}
	return (existingTaskInfo != null);
    }

    /**
     * takes a job number and returns the directory where output files from that job are/will be stored. <b>This routine
     * depends on having the System property java.io.tmpdir set the same for both the Tomcat and JBoss instantiations.
     * </b>
     * 
     * @param jobNumber
     *            the job number whose storage directory is being sought
     * @return String directory name on server of this job's files
     * @author Jim Lerner
     */
    public static String getJobDir(String jobNumber) {
	String tmpDir = System.getProperty("jobs");
	if (!tmpDir.endsWith(File.separator)) {
	    tmpDir = tmpDir + "/";
	}
	tmpDir = tmpDir + jobNumber;
	return tmpDir;
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
    public static String installNewTask(String zipFilename, String username, int access_id, boolean recursive,
	    Status taskIntegrator) throws TaskInstallationException {
	Vector vProblems = new Vector();
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
	    } catch (IOException ioe) {
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
		// if we get here, the zip file contains only other zip files
		// recursively install them
		String firstLSID = null;
		for (Enumeration eEntries = zipFile.entries(); eEntries.hasMoreElements();) {
		    zipEntry = (ZipEntry) eEntries.nextElement();
		    is = zipFile.getInputStream(zipEntry);
		    outFile = new File(System.getProperty("java.io.tmpdir"), zipEntry.getName());
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
			vProblems.add("only read " + numRead + " of " + fileLength + " bytes in " + zipFilename + "'s "
				+ zipEntry.getName());
		    }
		    is.close();
		    log.info("installing " + outFile.getAbsolutePath());
		    lsid = installNewTask(outFile.getAbsolutePath(), username, access_id, taskIntegrator);
		    log.info("installed " + lsid);
		    if (firstLSID == null) {
			firstLSID = lsid;
		    }
		    outFile.delete();
		    if (!recursive) {
			break; // only install the top level (first entry)
		    }
		}
		return firstLSID;
	    }

	    // TODO: see TaskUtil.getTaskInfoFromManifest, may be able to get rid of duplicate code
	    Properties props = new Properties();
	    props.load(zipFile.getInputStream(manifestEntry));

	    taskName = (String) props.remove(NAME);
	    lsid = (String) props.get(LSID);
	    LSID l = new LSID(lsid); // ; throw MalformedURLException if this
	    // is
	    // a bad LSID
	    if (taskName == null || taskName.length() == 0) {
		vProblems.add("Missing task name in manifest in " + new File(zipFilename).getName());
		throw new TaskInstallationException(vProblems); // abandon ship!
	    }
	    String taskDescription = (String) props.remove(DESCRIPTION);

	    // ParameterInfo entries consist of name/value/description triplets,
	    // of which the value and description are optional
	    // It is assumed that the names are p[1-n]_name, p[1-n]_value, and
	    // p[1-n]_description
	    // and that the numbering runs consecutively. When there is no
	    // p[m]_name value, then there are m-1 ParameterInfos
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
		    // _cat.debug("installTask: " + taskName + ": parameter " +
		    // name + "=" + value);
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
	    if (taskType.toLowerCase().endsWith("pipeline")) {
		// it is a pipeline
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
		vProblems = GenePatternAnalysisTask.installTask(taskName, taskDescription, params, tia, username, access_id,
			taskIntegrator);
		if (vProblems == null) {
		    vProblems = new Vector();
		}
		if (vProblems.size() == 0) {
		    // get the newly assigned LSID
		    lsid = (String) tia.get(GPConstants.LSID);

		    // extract files from zip file
		    String taskDir = DirectoryManager.getTaskLibDir(null, (String) tia.get(GPConstants.LSID), username);
		    File dir = new File(taskDir);

		    // if there are any existing files from a previous
		    // installation
		    // of this task,
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

			// copy attachments to the taskLib BEFORE installing the
			// task, so that there is no time window when
			// the task is installed in Omnigene's database but the
			// files aren't decoded and so the task can't yet
			// be properly invoked

			// TODO: allow names to have paths, so long as they are
			// below the current point and not above or a peer
			// strip absolute or ../relative path names from zip
			// entry
			// name so that they dump into the tasklib directory
			// only
			i = name.lastIndexOf("/");
			if (i != -1) {
			    name = name.substring(i + 1);
			}
			i = name.lastIndexOf("\\");
			if (i != -1) {
			    name = name.substring(i + 1);
			}
			try {
			    // TODO: support directory structure within zip file
			    outFile = new File(taskDir, name);
			    if (outFile.exists()) {
				File oldVersion = new File(taskDir, name + ".old");
				log.warn("replacing " + name + " (" + outFile.length() + " bytes) in " + taskDir
					+ ".  Renaming old one to " + oldVersion.getName());
				oldVersion.delete(); // delete the previous
				// .old
				// file
				boolean renamed = rename(outFile, oldVersion, true);
				if (!renamed) {
				    log.error("failed to rename " + outFile.getCanonicalPath() + " to "
					    + oldVersion.getCanonicalPath());
				}
			    }

			} catch (IOException ioe) {
			    String msg = "error unzipping file " + name + " from " + zipFilename + ": " + ioe.getMessage();
			    vProblems.add(msg);
			}
			is.close();
			if (os != null) {
			    os.close();
			    os = null;
			}
		    }
		    //
		    // unzip using ants classes to allow file permissions to be
		    // retained
		    boolean useAntUnzip = true;
		    if (!System.getProperty("os.name").toLowerCase().startsWith("windows")) {
			useAntUnzip = false;
			Execute execute = new Execute();
			execute.setCommandline(new String[] { "unzip", zipFilename, "-d", taskDir });
			try {
			    int result = execute.execute();
			    if (result != 0) {
				useAntUnzip = true;
			    }
			} catch (IOException ioe) {
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
	} catch (Exception e) {
	    log.error(e);
	    e.printStackTrace();
	    vProblems.add(e.getMessage() + " while installing task");
	} finally {
	    try {
		if (zipFile != null) {
		    zipFile.close();
		}
	    } catch (IOException ioe) {
	    }
	}
	if ((vProblems != null) && (vProblems.size() > 0)) {
	    for (Enumeration eProblems = vProblems.elements(); eProblems.hasMoreElements();) {
		log.error(eProblems.nextElement());
	    }
	    throw new TaskInstallationException(vProblems);
	}
	return lsid;
    }

    public static String installNewTask(String zipFilename, String username, int access_id, Status taskIntegrator)
	    throws TaskInstallationException {
	return installNewTask(zipFilename, username, access_id, true, taskIntegrator);
    }

    public static String downloadTask(String zipURL) throws IOException {
	return downloadTask(zipURL, null, -1);
    }

    /**
     * downloads a file from a URL and returns the path to the local file to the caller.
     * 
     * @param zipURL
     *            String URL of file to download
     * @return String filename of temporary downloaded file on server
     * @throws IOException
     *             if any problems occured in accessing the remote file or storing it locally
     * @author Jim Lerner
     */
    public static String downloadTask(String zipURL, Status statusMonitor, long expectedLength) throws IOException {
	return downloadTask(zipURL, statusMonitor, expectedLength, true);
    }

    public static String downloadTask(String zipURL, Status statusMonitor, long expectedLength, boolean verbose)
	    throws IOException {
	File zipFile = null;
	long downloadedBytes = 0;
	try {
	    zipFile = File.createTempFile("task", ".zip");
	    zipFile.deleteOnExit();
	    FileOutputStream os = new FileOutputStream(zipFile);
	    URLConnection uc = new URL(zipURL).openConnection();
	    log.info("opened connection");
	    long downloadSize = -1;
	    Map headerFields = uc.getHeaderFields();
	    for (Iterator itHeaders = headerFields.keySet().iterator(); itHeaders.hasNext();) {
		String name = (String) itHeaders.next();
		String value = uc.getHeaderField(name);
		// System.out.println(name + "=" + value);
	    }
	    if (uc instanceof HttpURLConnection) {
		downloadSize = ((HttpURLConnection) uc).getHeaderFieldInt("Content-Length", -1);
	    } else if (expectedLength == -1) {
		downloadSize = uc.getContentLength();
		// downloadSize = expectedLength;
	    } else {
		downloadSize = expectedLength;
	    }
	    if ((statusMonitor != null) && (downloadSize != -1) && verbose) {
		statusMonitor.statusMessage("Download length: " + (long) downloadSize + " bytes."); // Each
		// dot
	    }
	    // represents
	    // 100KB.");
	    if ((statusMonitor != null)) {
		statusMonitor.beginProgress("download");
	    }
	    InputStream is = uc.getInputStream();
	    byte[] buf = new byte[100000];
	    int i;
	    long lastPercent = 0;
	    while ((i = is.read(buf, 0, buf.length)) > 0) {
		downloadedBytes += i;
		os.write(buf, 0, i);
		// System.out.print(new String(buf, 0, i));
		if (downloadSize > -1) {
		    long pctComplete = 100 * downloadedBytes / downloadSize;
		    if (lastPercent != pctComplete) {
			if (statusMonitor != null) {
			    statusMonitor.continueProgress((int) pctComplete);
			}
			lastPercent = pctComplete;
		    }
		}
	    }
	    is.close();
	    os.close();
	    if (downloadedBytes == 0) {
		throw new IOException("Nothing downloaded from " + zipURL);
	    }
	    return zipFile.getPath();
	} catch (IOException ioe) {
	    log.info("Error in downloadTask: " + ioe.getMessage());
	    zipFile.delete();
	    throw ioe;
	} finally {
	    System.out.println("downloaded " + downloadedBytes + " bytes");
	    if (statusMonitor != null) {
		statusMonitor.endProgress();
		if (verbose) {
		    statusMonitor.statusMessage("downloaded " + downloadedBytes + " bytes");
		}
	    }
	}
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

    // pipeline support:

    /**
     * accepts a jobID and Process object, logging them in the htRunningPipelines Hashtable. When the pipeline
     * terminates, they will be removed from the Hashtable by terminateJob.
     * 
     * @param jobID
     *            job ID number
     * @param p
     *            Process object for running R pipeline
     * @author Jim Lerner
     * @see #terminateJob(String,Hashtable)
     * @see #terminatePipeline(String)
     */
    public static void storeProcessInHash(String jobID, Process p) {
	htRunningPipelines.put(jobID, p);
    }

    /**
     * Creates an Omnigene database entry in the analysis_job table. Unlike other entries, this one is not dispatchable
     * to any known analysis task because it has a bogus taskID. Since it is a pipeline, it is actually being invoked by
     * a separate process (not GenePatternAnalysisTask), but is using the rest of the infrastructure to get input files,
     * store output files, and retrieve status and result files.
     * 
     * @param userID
     *            user who owns this pipeline data instance
     * @param parameter_info
     *            ParameterInfo array containing pipeline data file output entries
     * @throws OmnigeneException
     *             if thrown by Omnigene
     * @throws RemoteException
     *             if thrown by Omnigene
     * @author Jim Lerner
     * @see #startPipeline(String,Process)
     * @see #terminatePipeline(String)
     */
    public static JobInfo createPipelineJob(String userID, String parameter_info, String pipelineName, String lsid) {
	return createPipelineJob(userID, parameter_info, pipelineName, lsid, JobStatus.JOB_PENDING);
    }

    public static JobInfo createPipelineJob(String userID, String parameter_info, String pipelineName, String lsid, int status) {
	JobInfo job;
	try {
	    HibernateUtil.beginTransaction();
	    if (log.isDebugEnabled()) {
		log.debug("Creating pipeline job");
	    }
	    Integer jobNo = getDS().addNewJob(BaseDAO.UNPROCESSABLE_TASKID, userID, parameter_info, pipelineName, null, lsid,
		    status);
	    job = getDS().getJobInfo(jobNo);

	    if (log.isDebugEnabled()) {
		log.debug("New pipeline jobNo= " + jobNo);
	    }
	    HibernateUtil.commitTransaction();
	} catch (RuntimeException e) {
	    log.error("Error creating pipeline.", e);
	    HibernateUtil.rollbackTransaction();
	    throw e;
	}

	return job;
    }

    public static JobInfo createVisualizerJob(String userID, String parameter_info, String visualizerName, String lsid)
	    throws OmnigeneException, RemoteException {
	try {
	    int taskId = new org.genepattern.server.webservice.server.local.LocalAdminClient(userID).getTask(lsid).getID();
	    Integer jobNo = getDS().recordClientJob(taskId, userID, parameter_info, -1);
	    return getDS().getJobInfo(jobNo);

	} catch (org.genepattern.webservice.WebServiceException wse) {
	    throw new OmnigeneException("Unable to record job");
	}
    }

    /**
     * Changes the JobStatus of a pipeline job, and appends zero or more output parameters (output filenames) to the
     * JobInfo ParameterInfo array for eventual return to the invoker. The jobStatus constants are those defined in
     * edu.mit.wi.omnigene.framework.analysis.JobStatus
     * 
     * This method gets called from many threads which might or might not be wrapped in transactions. This needs to be
     * committed immediately since other threads or processes might be waiting on the update.
     * 
     * @param jobNumber
     *            jobID of the pipeline whose status is to be updated
     * @param jobStatus
     *            new status (eg. JobStatus.PROCESSING, JobStatus.DONE, etc.)
     * @param additionalParams
     *            array of ParameterInfo objects which represent additional output parameters from the pipeline job
     * @throws OmnigeneException
     *             if thrown by Omnigene
     * @throws RemoteException
     *             if thrown by Omnigene
     * @author Jim Lerner
     * @see org.genepattern.webservice.JobStatus
     */
    public static void updatePipelineStatus(int jobNumber, int jobStatus, ParameterInfo[] additionalParams)
	    throws OmnigeneException, RemoteException {
	if (log.isDebugEnabled()) {
	    log.debug("Updating pipeline status.  job# = " + jobNumber);
	}
	try {
	    HibernateUtil.beginTransaction();
	    JobInfo jobInfo = getDS().getJobInfo(jobNumber);
	    if (additionalParams != null) {
		for (int i = 0; i < additionalParams.length; i++) {
		    jobInfo.addParameterInfo(additionalParams[i]);
		}
	    }
	    if (jobStatus < JobStatus.JOB_PENDING) {
		jobStatus = ((Integer) JobStatus.STATUS_MAP.get(jobInfo.getStatus())).intValue();
	    }
	    getDS().updateJob(jobNumber, jobInfo.getParameterInfo(), jobStatus);
	    HibernateUtil.commitTransaction();
	} catch (OmnigeneException ex) {
	    log.error("Error updating pipeline status.  jobNumber=" + jobNumber);
	    HibernateUtil.rollbackTransaction();
	}
    }

    /**
     * Changes the JobStatus of a pipeline job, and appends zero or one output parameters (output filenames) to the
     * jobs's JobInfo ParameterInfo array for eventual return to the invoker. This routine is actually invoked from
     * updatePipelineStatus.jsp. The jobStatus constants are those defined in
     * edu.mit.wi.omnigene.framework.analysis.JobStatus
     * 
     * @param jobNumber
     *            jobID of the pipeline whose status is to be updated
     * @param jobStatus
     *            new status (eg. JobStatus.PROCESSING, JobStatus.DONE, etc.)
     * @param name
     *            optional [short] name of filename parameter, ie. without directory information
     * @param additionalFilename
     *            optional filename of output file for this job
     * @throws OmnigeneException
     *             if thrown by Omnigene
     * @throws RemoteException
     *             if thrown by Omnigene
     * @author Jim Lerner
     * @see org.genepattern.webservice.JobStatus
     */
    public static void updatePipelineStatus(int jobNumber, int jobStatus, String name, String additionalFilename)
	    throws OmnigeneException, RemoteException {

	if (name != null && additionalFilename != null) {
	    ParameterInfo additionalParam = new ParameterInfo();
	    additionalParam.setAsOutputFile();
	    additionalParam.setName(name);
	    additionalParam.setValue(additionalFilename);
	    updatePipelineStatus(jobNumber, jobStatus, new ParameterInfo[] { additionalParam });
	} else {
	    updatePipelineStatus(jobNumber, jobStatus, null);
	}
    }

    /**
     * accepts a jobID and attempts to terminate the running pipeline process. Pipelines are notable only in that they
     * are sometimes run not as Omnigene tasks, but as R code that runs through each task serially. The running R
     * process itself is the "pipeline", although it isn't strictly speaking a task. When the pipeline is run as a task,
     * it is not treated as a pipeline in this code. The pipeline behavior only occurs when run via runPipeline.jsp,
     * allowing intermediate results of the task to appear, which would not happen if it were run as a task (all or none
     * for output).
     * 
     * @param jobID
     *            JobInfo jobNumber
     * @return Process of the pipeline if running, else null
     * @author Jim Lerner
     */
    public static Process terminatePipeline(String jobID) {
	Process p = (Process) htRunningPipelines.remove(jobID);
	if (p != null) {
	    p.destroy();
	} else {
	    p = (Process) htRunningJobs.get(jobID);
	    if (p != null) {
		p.destroy();
	    }
	}
	return p;
    }

    public static void terminateAll(String message) {
	log.debug(message);
	String jobID;
	Enumeration eJobs;
	int numTerminated = 0;
	for (eJobs = htRunningPipelines.keys(); eJobs.hasMoreElements();) {
	    jobID = (String) eJobs.nextElement();
	    log.info("Terminating job " + jobID);
	    Process p = terminatePipeline(jobID);
	    if (p != null) {
		try {
		    updatePipelineStatus(Integer.parseInt(jobID), JobStatus.JOB_ERROR, null);
		} catch (Exception e) { /* ignore */
		}
	    }
	    numTerminated++;
	}
	for (eJobs = htRunningJobs.keys(); eJobs.hasMoreElements();) {
	    jobID = (String) eJobs.nextElement();
	    log.info("Terminating job " + jobID);
	    terminateJob(jobID, htRunningJobs);
	    numTerminated++;
	}
	if (numTerminated > 0) {
	    // let the processes terminate, clean up, and record their outputs
	    // in the database
	    Thread.yield();
	}
    }

    // utility methods:

    /**
     * Creates a new Thread which blocks on reads to an InputStream, appends their output to the given file. The thread
     * terminates upon EOF from the InputStream.
     * 
     * @param is
     *            InputStream to read from
     * @param file
     *            file to write to
     * @author Jim Lerner
     */
    protected Thread streamToFile(final InputStream is, final File file) {
	// create thread to read from a process' output or error stream
	return new Thread() {
	    public void run() {
		byte[] b = new byte[2048];
		int bytesRead;
		BufferedOutputStream fis = null;
		boolean wroteBytes = false;
		try {
		    fis = new BufferedOutputStream(new FileOutputStream(file));
		    while ((bytesRead = is.read(b)) >= 0) {
			wroteBytes = true;
			fis.write(b, 0, bytesRead);
		    }
		} catch (IOException e) {
		    e.printStackTrace();
		    log.error(e);
		} finally {
		    if (fis != null) {
			try {
			    fis.flush();
			    fis.close();
			} catch (IOException e) {
			    e.printStackTrace();
			}
		    }
		    if (!wroteBytes) {
			file.delete();
		    }
		}
	    }
	};
    }

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
    protected File writeStringToFile(String dirName, String filename, String outputString) {
	File outFile = null;
	try {
	    outFile = new File(dirName, filename);
	    FileWriter fw = new FileWriter(outFile, true);
	    fw.write(outputString != null ? outputString : "");
	    fw.close();
	} catch (NullPointerException npe) {
	    log.error(getClass().getName() + ": writeStringToFile(" + dirName + ", " + filename + ", " + outputString + "): "
		    + npe.getMessage());
	    npe.printStackTrace();
	} catch (IOException ioe) {
	    log.error(getClass().getName() + ": writeStringToFile(" + dirName + ", " + filename + ", " + outputString + "): "
		    + ioe.getMessage());
	    ioe.printStackTrace();
	} finally {
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
	try {
	    log.info("Have to copy, rename failed: " + from.getCanonicalPath() + " -> " + to.getCanonicalPath());
	} catch (IOException ioe) {
	}
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
	GenePatternAnalysisTask gpat = new GenePatternAnalysisTask();
	log.info("GenePattern server version " + System.getProperty("GenePatternVersion") + " is ready.");
    }

    /**
     * return boolean indicating whether a filename represents a code file
     */
    public static boolean isCodeFile(String filename) {
	return hasEnding(filename, "code");
    }

    /**
     * return boolean indicating whether a filename represents a documentation file
     */
    public static boolean isDocFile(String filename) {
	return hasEnding(filename, "doc");
    }

    /**
     * return boolean indicating whether a filename represents a binary file
     */
    public static boolean isBinaryFile(String filename) {
	return hasEnding(filename, "binary");
    }

    /**
     * return boolean indicating whether a filename represents a file type (as found in
     * System.getProperties(files.{code,doc,binary}))
     */
    protected static boolean hasEnding(String filename, String fileType) {
	String endings = System.getProperty("files." + fileType, "");
	Vector vEndings = csvToVector(endings.toLowerCase());
	boolean ret = false;
	filename = new File(filename).getName().toLowerCase();
	int lastDot = filename.lastIndexOf(".");
	if (lastDot == -1) {
	    ret = vEndings.contains("");
	} else {
	    ret = vEndings.contains(filename.substring(lastDot + 1));
	}
	return ret;
    }

    /**
     * convert a CSV list into a Vector
     */
    protected static Vector csvToVector(String csv) {
	StringTokenizer stEntries = new StringTokenizer(csv, ",; ");
	Vector vEntries = new Vector();
	while (stEntries.hasMoreTokens()) {
	    vEntries.add(stEntries.nextToken());
	}
	return vEntries;
    }

    // implements FilenameFilter, but static
    public static boolean accept(File dir, String name) {
	return isDocFile(name);
    }

    public static Properties loadGenePatternProperties(ServletContext application, String filename) throws IOException {
	return appendProperties(application, filename, new Properties());
    }

    public static Properties appendProperties(ServletContext application, String filename, Properties props) throws IOException {
	// append build.properties to the genepattern properties
	return appendProperties((String) application.getAttribute("genepattern.properties"), filename, props);
    }

    public static Properties appendProperties(String propsDir, String filename, Properties props) throws IOException {
	// append build.properties to the genepattern properties
	File propFile = new File(propsDir + File.separatorChar + filename);
	FileInputStream fis = null;
	try {
	    fis = new FileInputStream(propFile);
	    props.load(fis);
	} catch (IOException ioe) {
	    throw new IOException(propFile.getAbsolutePath() + " cannot be loaded, reason: " + ioe.getMessage());
	} finally {
	    try {
		if (fis != null) {
		    fis.close();
		}
		fis = null;
	    } catch (IOException ioe) {
	    }
	}
	return props;
    }

    /* TODO: put all of this stuff in database and look it up when requested */

    // LHS is what is presented to user, RHS is what java System.getProperty()
    // returns
    public static String[] getCPUTypes() {
	return new String[] { ANY, "Alpha=alpha", "Intel=x86", "PowerPC=ppc", "Sparc=sparc" };
    }

    // LHS=show to user, RHS=what System.getProperty("os.name") returns
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
     * The Expander uses ant's unzip instead of Java's to preserve file permissions
     */
    private static class Expander extends Expand {
	public Expander() {
	    project = new Project();
	    project.init();
	    taskType = "unzip";
	    taskName = "unzip";
	    target = new Target();
	}
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

	public void updateTaskInfoAttributes(String taskInfoAttributes) {
	    this._taskInfoAttributes = taskInfoAttributes;
	}
    }
}
