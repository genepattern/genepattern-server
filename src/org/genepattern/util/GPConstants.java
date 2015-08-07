/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.util;

public class GPConstants {

    // strings which are shared with addTask.jsp and saveTask.jsp

    /** TaskInfo,ParamInfo.name */
    public static final String NAME = "name";

    /** Alternative name key for a ParameterInfo */
    public static final String ALTNAME = "altName";

    /** TaskInfo.description */
    public static final String DESCRIPTION = "description";

    /** Location where scripts, DLLs, etc are stored */
    public static final String LIBDIR = "libdir";

    /** input filename without directory */
    public static final String INPUT_FILE = "_file";

    /** input filename without directory or extension */
    public static final String INPUT_BASENAME = "_basename";

    /** input filename extension only */
    public static final String INPUT_EXTENSION = "_extension";

    /** input directory */
    public static final String INPUT_PATH = "_path";

    /** job ID (analysis_job ID) of running job */
    public static final String JOB_ID = "job_id";

    /** task ID (task_master ID) of running job */
    public static final String TASK_ID = "task_id";

    public static final String ANT = "ant";

    public static final String COMMAND_PREFIX = "commandPrefix"; // eg. LSF

    // prefix: bsub
    // -K -o
    // lsf_stdout
    // -e
    // lsf_stderr >
    // & /dev/null

    /** TaskInfoAttributes author key */
    public static final String AUTHOR = "author";

    /**
     * when a task is being renamed, this well known parameter describes the former task name
     */
    public static final String FORMER_NAME = "formerName";

    // TaskInfoAttributes names

    /** TaskInfoAttributes command line key */
    public static final String COMMAND_LINE = "commandLine";

    /** TaskInfoAttributes task type key */
    public static final String TASK_TYPE = "taskType";
    
    /** TaskInfoAttributes 'categories' key, added after the 3.7.0 release, to allow custom taskType for a pipeline or visualizer */
    public static final String CATEGORIES = "categories";

    /** any */
    public static final String ANY = "any";

    /** TaskInfoAttributes CPU type key */
    public static final String CPU_TYPE = "cpuType";

    /** TaskInfoAttributes OS key */
    public static final String OS = "os";

    /** TaskInfoAttributes JVM version number key */
    public static final String JVM_LEVEL = "JVMLevel";

    /** TaskInfoAttributes language key */
    public static final String LANGUAGE = "language";

    public static final String REQUIRED_PATCH_LSIDS = "requiredPatchLSIDs";

    public static final String LICENSE = "license";

    public static final String TASK_DOC = "taskDoc";
    
    public static final String CREATION_DATE = "creationDate";
    
    public static final String GP_VERSION = "gpVersion";

    public static final String REQUIRED_PATCH_URLS = "requiredPatchURLs";

    public static final String INSTALLED_PATCH_LSIDS = "installedPatchLSIDs";

    public static final String DEFAULT_PATCH_URL = "DefaultPatchURL";

    public static final String DEFAULT_PATCH_REPOSITORY_URL = "DefaultPatchRepositoryURL";

    public static final String PATCH_SUCCESS_EXIT_VALUE = "successExitValue";

    public static final String PATCH_ERROR_EXIT_VALUE = "failureExitValue";

    /**
     * TaskInfoAttributes pipeline invocation key (prefixed by language, eg. RInvoke)
     */
    public static final String INVOKE = "Invoke";

    /** ParameterInfo attribute for prefix string for command line generation */
    public static final String PREFIX = "prefix";

    /** TaskInfoAttributes quality key */
    public static final String QUALITY = "quality";

    /** TaskInfoAttributes privacy key */
    public static final String PRIVACY = "privacy";

    /** TaskInfoAttributes userID key */
    public static final String USERID = "userid";

    public static final String PASSWORD = "password";

    /** TaskInfoAttributes version key */
    public static final String VERSION = "version";

    /** TaskInfoAttributes pipeline code key */
    public static final String PIPELINE_SCRIPT = "pipelineModel";

    public static final String SERIALIZED_MODEL = "serializedModel";

    // well-known task types that get special handling

    /** well known task type that gets special handling in menus and browsers */
    public static final String TASK_TYPE_VISUALIZER = "Visualizer";

    /** well known task category that gets special handling in menus and browsers */
    public static final String TASK_TYPE_JAVASCRIPT = "javascript";

    /** well known task type that gets special handling in menus and browsers */
    public static final String TASK_TYPE_PIPELINE = "pipeline";

    /** three levels of quality defined: development, preproduction, production */
    public static final String QUALITY_DEVELOPMENT = "development";

    /** three levels of quality defined: development, preproduction, production */
    public static final String QUALITY_PREPRODUCTION = "preproduction";

    /** three levels of quality defined: development, preproduction, production */
    public static final String QUALITY_PRODUCTION = "production";

    /** array of quality levels for user choices */
    public static final String[] QUALITY_LEVELS = { QUALITY_DEVELOPMENT, QUALITY_PREPRODUCTION, QUALITY_PRODUCTION };

    /**
     * two levels of privacy defined: public, private. Group is complicated for users and may not be useful
     */
    public static final String PUBLIC = "public";

    /**
     * two levels of privacy defined: public, private. Group is complicated for users and may not be useful
     */
    public static final String PRIVATE = "private";

    /** array of privacy levels for user choices */
    public static final String[] PRIVACY_LEVELS = { PRIVATE, PUBLIC };

    /** access_id values for database */
    public static final int ACCESS_PRIVATE = 2;

    public static final int ACCESS_PUBLIC = 1;

    public static final String LSID = "LSID";

    public static final String LSID_PROVENANCE = "LSID_PROVENANCE"; // lsid a

    // task was
    // derived
    // from

    public static final String FILE_FORMAT = "fileFormat";

    /**
     * names of all expected TaskInfoAttributes from manifest file (or other new task submission)
     */
    public static final String[] TASK_INFO_ATTRIBUTES = { COMMAND_LINE, TASK_TYPE, CPU_TYPE, OS, JVM_LEVEL, LANGUAGE,
	    VERSION, AUTHOR, USERID, PRIVACY, QUALITY, PIPELINE_SCRIPT, LSID, SERIALIZED_MODEL, FILE_FORMAT,
	    REQUIRED_PATCH_LSIDS, REQUIRED_PATCH_URLS, LICENSE, 
	};

    public static final char PARAM_INFO_SPACER = '_'; // replace this

    // character

    // with a space for user
    // presentation

    public static final String PARAM_INFO_TYPE_SEPARATOR = "=";

    public static final String PARAM_INFO_CHOICE_DELIMITER = ";";

    public static final String PARAM_INFO_CHOICE = "choice";

    public static final String PARAM_INFO_STRING = "string";

    public static final String PARAM_INFO_PASSWORD = "PASSWORD";

    public static final String PARAM_INFO_CHECKBOX = "checkbox";

    public static final String PARAM_INFO_TYPE_INTEGER = "java.lang.Integer";

    public static final String PARAM_INFO_TYPE_TEXT = "java.lang.String";

    public static final String PARAM_INFO_TYPE_PASSWORD = "PASSWORD";

    public static final String PARAM_INFO_TYPE_FLOAT = "java.lang.Float";

    public static final String PARAM_INFO_TYPE_INPUT_FILE = "java.io.File";

    public static final String PARAM_INFO_TYPE_DIR = "DIRECTORY";
    public static final int PARAM_INFO_TYPE_NAME_OFFSET = 0; // offset into

    public static final String JAVA_FLAGS = "java_flags";

    
    // one
    // of the following
    // array entries

    public static final int PARAM_INFO_TYPE_CLASS_OFFSET = 1;

    public static final String[][] PARAM_INFO_TYPES = { { "text", PARAM_INFO_TYPE_TEXT },
	    { "integer", PARAM_INFO_TYPE_INTEGER }, { "floating point", PARAM_INFO_TYPE_FLOAT },
	    { "input file", PARAM_INFO_TYPE_INPUT_FILE }, { "directory", PARAM_INFO_TYPE_DIR}, { "password", PARAM_INFO_TYPE_PASSWORD } };

    // well-known ParameterInfo attributes
    public static final int PARAM_INFO_NAME_OFFSET = 0; // offset into one of

    // arrays

    public static final int PARAM_INFO_TYPE_OFFSET = 1;

    public static final int PARAM_INFO_CHOICE_TYPES_OFFSET = 2;

    public static final int PARAM_INFO_CHOICE_TYPES_MULTIPLE_OFFSET = 3;

    public static final String MULTIPLE = "multiple";

    public static final String[] PARAM_INFO_CLIENT_FILENAME = { "client_filename", PARAM_INFO_STRING };

    public static final String[] PARAM_INFO_DEFAULT_VALUE = { "default_value", PARAM_INFO_STRING };

    public static final Object[] PARAM_INFO_TYPE = { "type", PARAM_INFO_CHOICE, PARAM_INFO_TYPES };

    public static final String[] PARAM_INFO_OPTIONAL = { "optional", PARAM_INFO_CHECKBOX };
    
    public static final String[] PARAM_INFO_URL_MODE = { "url", PARAM_INFO_CHECKBOX };

    public static final String[] PARAM_INFO_PREFIX = { "prefix_when_specified", PARAM_INFO_STRING };

    public static Object[] PARAM_INFO_FILE_FORMAT = { FILE_FORMAT, PARAM_INFO_CHOICE, null, MULTIPLE };

    public static final Object[][] PARAM_INFO_ATTRIBUTES = { PARAM_INFO_DEFAULT_VALUE, PARAM_INFO_OPTIONAL,
	    PARAM_INFO_PREFIX, PARAM_INFO_TYPE, PARAM_INFO_FILE_FORMAT };

    public static int MAX_PARAMETERS = 20;

    /** parameter delimiters for commandLine and output_filename contents */
    public static final String LEFT_DELIMITER = "<";

    /** parameter delimiters for commandLine and output_filename contents */
    public static final String RIGHT_DELIMITER = ">";

    /**
     * well-known name of manifest file within zip file containing GenePatternAnalysisTask descriptors and support files
     */
    public static final String MANIFEST_FILENAME = "manifest";

    public static final String[] RESERVED_PARAMETER_NAMES = { JOB_ID, TASK_ID, LIBDIR, NAME };

    public static final String[] UNREQUIRED_PARAMETER_NAMES = {};

    /**
     * filename of well-known file that may be output by a job, representing the output to the stdout stream of the
     * process
     */
    public static final String STDOUT = "stdout.txt";

    /**
     * filename of well-known file that may be output by a job, representing the output to the stderr stream of the
     * process
     */
    public static final String STDERR = "stderr.txt";

    /**
     * filename of well-known file that may be output by a job, representing the provenance data of the process
     */
    public static final String TASKLOG = "gp_execution_log.txt";
    public static final String PIPELINE_TASKLOG_ENDING = "_execution_log.html";

    /**
     * fileextension of ODF
     */
    public final static String ODF = "ODF";

    public static String UTF8 = "UTF-8";

    public static String TASK_NAMESPACE = "genepatternmodules";

    public static String SUITE_NAMESPACE = "genepatternsuites";

    public static final String SUITE_MANIFEST_FILENAME = "suiteManifest.xml";

    /** token to redirect standard output */
    public static final String STDOUT_REDIRECT = "\\>";

    /** token to redirect standard input */
    public static final String STDIN_REDIRECT = "\\<";

    /** token to redirect standard error */
    public static final String STDERR_REDIRECT = "\\>&";

    /**
     * maximum number of concurrent jobs to run before next one will have to wait
     */
    public static final String NUM_THREADS = "num.threads";

    public static final String PIPELINE_ARG_STOP_AFTER_TASK_NUM = "PIPELINE_ARG.StopAfterTask";

    public static final String TASK_PREFIX_MAPPING = "taskPrefixMapping";

    public static final String REGISTERED_SERVER = "registeredServer";

}
