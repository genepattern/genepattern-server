package org.genepattern.server.config;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.executor.CommandProperties;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * Server configuration.
 * 
 * @author pcarr
 */
public class ServerConfiguration {
    private static Logger log = Logger.getLogger(ServerConfiguration.class);
    public static final String PROP_CONFIG_FILE = "config.file";
    //for compatibility with GP 3.2.3 and GP 3.2.4
    public static final String PROP_LEGACY_CONFIG_FILE = "command.manager.config.file";
    
    public static class Exception extends java.lang.Exception {
        public Exception() {
            super();
        }
        public Exception(String message) {
            super(message);
        }
        public Exception(String message, Throwable t) {
            super(message, t);
        }
    }

    public static class Context {
        //hard-coded default value is true for compatibility with GP 3.2.4 and earlier
        private boolean checkSystemProperties = true;
        //hard-coded default value is true for compatibility with GP 3.2.4 and earlier
        private boolean checkPropertiesFiles = true;
        private String userId = null;
        private TaskInfo taskInfo = null;
        private JobInfo jobInfo = null;
        private boolean isAdmin=false;
        
        public static Context getServerContext() {
            Context context = new Context();
            return context;
        }

        public static Context getContextForUser(String userId) {
            Context context = new Context();
            if (userId != null) {
                context.setUserId(userId);
            }
            return context;
        }

        public static Context getContextForJob(JobInfo jobInfo) {
            Context context = new Context();
            if (jobInfo != null) {
                context.setJobInfo(jobInfo);
                if (jobInfo.getUserId() != null) {
                    context.setUserId(jobInfo.getUserId());
                }
            }
            return context;
        }
        
        public static Context getContextForJob(JobInfo jobInfo, TaskInfo taskInfo) {
            Context context = getContextForJob(jobInfo);
            if (taskInfo != null) {
                context.setTaskInfo(taskInfo);
            }
            return context;
        }
        
        public void setCheckSystemProperties(boolean b) {
            this.checkSystemProperties = b;
        }

        public boolean getCheckSystemProperties() {
            return checkSystemProperties;
        }

        public void setCheckPropertiesFiles(boolean b) {
            this.checkPropertiesFiles = b;
        }
        
        public boolean getCheckPropertiesFiles() {
            return checkPropertiesFiles;
        }
        
        public void setUserId(String userId) {
            this.userId = userId;
        }
        public String getUserId() {
            return userId;
        }
        
        public void setTaskInfo(TaskInfo taskInfo) {
            this.taskInfo = taskInfo;
        }
        public TaskInfo getTaskInfo() {
            return this.taskInfo;
        } 
        
        public void setJobInfo(JobInfo jobInfo) {
            this.jobInfo = jobInfo;
        }
        public JobInfo getJobInfo() {
            return jobInfo;
        }

        public void setIsAdmin(final boolean b) {
            this.isAdmin=b;
        }
        public boolean isAdmin() {
            return isAdmin;
        }
    }

    private static ServerConfiguration singleton = new ServerConfiguration();
    public static ServerConfiguration instance() {
        return singleton;
    }
    
    private ServerConfiguration() {
        try {
            reloadConfiguration();
        }
        catch (Throwable t) {
            errors.add(t);
            log.error("Error creating ServerConfiguration instance: "+t.getLocalizedMessage());
        }
    }

    private URL genePatternUrl = null;
    private String configFilepath = null;
    private File configFile = null;

    //cache any errors thrown while loading/reloading the configuration file
    private List<Throwable> errors = new ArrayList<Throwable>();
    
    public synchronized void reloadConfiguration() {
        this.configFilepath = ServerProperties.instance().getProperty(PROP_CONFIG_FILE);
        if (configFilepath == null) {
            this.configFilepath = ServerProperties.instance().getProperty(PROP_LEGACY_CONFIG_FILE);
            log.info(""+PROP_CONFIG_FILE+" not set, checking "+PROP_LEGACY_CONFIG_FILE);
        }
        if (configFilepath == null) {
            configFilepath = "config_default.yaml";
            log.info(""+PROP_CONFIG_FILE+" not set, using default config file: "+configFilepath);
        }
        reloadConfiguration(configFilepath);
    }
    
    public synchronized void reloadConfiguration(String configFilepath) {
        try {
            log.info("loading configuration from '"+configFilepath+"' ...");
            this.configFilepath = configFilepath;
            errors.clear();
            ConfigFileParser parser = new ConfigFileParser();
            parser.parseConfigFile(configFilepath);
            this.configFile = parser.getConfigFile();
            this.cmdMgrProps = parser.getConfig();
            this.jobConfig = parser.getJobConfig();
        }
        catch (Throwable t) {
            errors.add(t);
            log.error(t);
        }
    }
    
    public String getConfigFilepath() {
        return configFilepath;
    }
    
    public File getConfigFile() {
        return configFile;
    }

    /**
     * Get the list of errors, if any, which resulted from parsing the configuration file.
     * 
     * @return
     */
    public List<Throwable> getInitializationErrors() {
        return errors;
    }

    //legacy (circa GP 3.2.3 ;) code ... in transition from old job configuration file to server configuration file 
    private CommandManagerProperties cmdMgrProps = new CommandManagerProperties();
    public CommandManagerProperties getCommandManagerProperties() {
        return cmdMgrProps;
    }
    private JobConfigObj jobConfig = new  JobConfigObj();
    public JobConfigObj getJobConfiguration() {
        return jobConfig;
    }

    /**
     * Utility method for parsing properties as a boolean.
     * The current implementation uses Boolean.parseBoolean, 
     * which returns true iff the property is set and equalsIgnoreCase 'true'.
     * 
     * @param key
     * @return
     */
    public boolean getGPBooleanProperty(Context context, String key) {
        String prop = getGPProperty(context, key);
        return Boolean.parseBoolean(prop);
    }
    
    public boolean getGPBooleanProperty(Context context, String key, boolean defaultValue) {
        String prop = getGPProperty(context, key);
        if (prop == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(prop);
    }
    
    /**
     * Utility method for parsing a property as an Integer.
     * 
     * When a non integer value is set in the config file, the default value is returned.
     * Errors are logged, but exceptions are not thrown.
     * 
     * @param key
     * @param defaultValue
     * 
     * @return the int value for the property, or the default value, can return null.
     */
    public Integer getGPIntegerProperty(Context context, String key, Integer defaultValue) {
        String val = getGPProperty(context, key);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val);
        }
        catch (NumberFormatException e) {
            log.error("Error parsing integer value for property, "+key+"="+val);
            return defaultValue;
        }
    }
    
    public Long getGPLongProperty(Context context, String key, Long defaultValue) {
        String val = getGPProperty(context, key);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(val);
        }
        catch (NumberFormatException e) {
            log.error("Error parsing long value for property, "+key+"="+val);
            return defaultValue;
        }
    }

    /**
     * @deprecated, use getValue instead, which supports lists.
     * @param context
     * @param key
     * @return
     */
    public String getGPProperty(Context context, String key) {
        if (cmdMgrProps == null) {
            log.error("Invalid server configuration in getGPProperty("+key+")");
            return null;
        }
        return cmdMgrProps.getProperty(context, key);
    }
    
    public String getGPProperty(Context context, String key, String defaultValue) {
        if (cmdMgrProps == null) {
            log.error("Invalid server configuration in getGPProperty("+key+")");
            return defaultValue;
        }
        String value = cmdMgrProps.getProperty(context, key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
    
    public CommandProperties.Value getValue(Context context, String key) {
        if (cmdMgrProps == null) {
            log.error("Invalid server configuration in getGPProperty("+key+")");
            return null;
        }
        return cmdMgrProps.getValue(context, key);
    }

    /**
     * Get the public facing URL for this GenePattern Server.
     * Note: replaces <pre>System.getProperty("GenePatternURL");</pre>
     * @return
     */
    public URL getGenePatternURL() {
        if (genePatternUrl == null) {
            initGpUrl();
        }
        return genePatternUrl;
    }

    private void initGpUrl() {
        String urlStr = "";
        try {
            urlStr = System.getProperty("GenePatternURL");
            this.genePatternUrl = new URL(urlStr);
        }
        catch (Throwable t) {
            log.error("Error initializing GenePatternURL="+urlStr);
            
            try {
                this.genePatternUrl = new URL("http://127.0.0.1:8080/gp/");
            }
            catch(Throwable t1) {
                throw new IllegalArgumentException("shouldn't ever be here", t1);
            }
        }
    }
    
    //helper methods for locating server files and folders
    /**
     * Get the 'home directory' for a gp user account. This is the location for user data.
     * Home directories are created in the  in the "../users" directory.
     * The default location can be changed with the 'user.root.dir' configuration property. 
     * The 'gp.user.dir' property can be set on a per user basis to change from the standard location.
     * 
     * Note: The 'gp.user.dir' property is an untested feature. If an admin sets a non-standard user dir,
     *     they need to take measures (undocumented and unsupported, @see gp-help) to deal with 
     *     pre-existing files and file entries in the DB.
     * 
     * @param context
     * @return
     */
    public File getUserDir(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context is null");
        }
        if (context.getUserId() == null) {
            throw new IllegalArgumentException("context.userId is null");
        }
        String userDirPath = getGPProperty(context, "gp.user.dir");
        if (userDirPath == null) {
            String userRootDirPath = getGPProperty(context, "user.root.dir", "../users");
            File p_for_parent = new File(userRootDirPath);
            File f_for_file = new File(p_for_parent,context.getUserId());
            userDirPath = f_for_file.getPath();
        }
        
        File userDir = new File(userDirPath);
        if (userDir.exists()) {
            return userDir;
        }
        boolean success = userDir.mkdirs();
        if (!success) {
            throw new IllegalArgumentException("Unable to create home directory for user "+context.getUserId()+", userDir="+userDir.getAbsolutePath());
        }
        return userDir;
    }
    
    /**
     * Get the jobs directory for the given user. Each job runs in a new working directory. 
     * 
     * circa, GP 3.2.4 and earlier, the working directory is created in the root job dir for the server,
     *     which defaults to './webapps/gp/jobResults'.
     * Edit the 'jobs' property to customize this location. The server configuration system enables setting
     * this property on a per user, group, executor or module basis.
     * 
     * coming soon, job directories, by default, will be created in ../users/<user.id>/jobs/
     * To test this feature, remove the 'jobs' property from genepattern.properties and the configuration file.
     * 
     * @return the parent directory in which to create the new working directory for a job.
     */
    public File getRootJobDir(Context context) throws Exception {
        //default behavior, circa GP 3.2.4 and earlier, hard code path based on 'jobs' property
        String jobsDirPath = getGPProperty(context, "jobs");
        if (jobsDirPath != null) {
            return new File(jobsDirPath);
        }

        //prototype behavior, circa GP 3.3.1 and later, default location in ../users/<user.id>/jobs/
        boolean invalidContext = false;
        if (context == null) {
            invalidContext = true;
            log.error("context == null");
        }
        else if (context.getUserId() == null) {
            log.error("context.userId == null");
            invalidContext = true;
        }
        if (invalidContext) {
            log.error("Missing required configuration property, 'jobs', using default location");
            jobsDirPath = "./webapps/gp/jobResults";
            return new File(jobsDirPath);
        }
        
        File userDir = getUserDir(context);
        if (userDir != null) {
            File root = new File(userDir, "jobs");
            jobsDirPath = root.getPath();
        }

        if (jobsDirPath == null) {
            throw new Exception("Missing required propery, 'jobs'");
        }
        File rootJobDir = new File(jobsDirPath);
        return rootJobDir;
    }

//    /**
//     * Note: untested prototype code, for use in next rev of GP.
//     * 
//     * Get the working directory for a given job, create the job directory if necessary.
//     * @param context, requires a jobInfo with a valid and unique jobId.
//     * @return
//     */
//    private File getJobDir(Context context) throws Exception {
//        if (context == null) {
//            throw new Exception("context is null");
//        }
//        if (context.getJobInfo() == null) {
//            throw new Exception("context.jobInfo is null");
//        }
//        if (context.getJobInfo().getJobNumber() < 0) {
//            throw new Exception("invalid jobNumber, jobNumber="+context.getJobInfo().getJobNumber());
//        }
//
//        File parent = getRootJobDir(context);
//        File workingDir = new File(parent, ""+context.getJobInfo().getJobNumber());
//        if (workingDir.exists()) {
//            return workingDir;
//        }
//        boolean success = workingDir.mkdirs();
//        if (success) {
//            return workingDir;
//        }
//        throw new Exception("Error getting working directory for job="+context.getJobInfo().getJobNumber());
//    }

    /**
     * Get the upload directory for the given user, the location for files uploaded directly from the Uploads tab.
     * By default, user uploads are stored in ../users/<user.id>/user.uploads/
     * 
     * The default location can be overridden with the 'user.upload.dir' property.
     * If there is no 'user.upload.dir' or 'gp.user.dir' set, then 'java.io.tempdir' is returned.
     * 
     * @param context
     * @return
     * @throws IllegalArgumentException if a directory is not found for the userId.
     */
    public File getUserUploadDir(Context context) throws IllegalArgumentException {
        boolean configError = false;
        if (context == null) {
            configError = true;
            //throw new IllegalArgumentException("context is null");
            log.error("context is null");
        }
        if (context.getUserId() == null) {
            configError = true;
            //throw new IllegalArgumentException("context.userId is null");
            log.error("context.userId is null");
        }
        
        if (configError) {
            return getTempDir();
        }

        String userUploadPath = getGPProperty(context, "user.upload.dir");
        if (userUploadPath == null) {
            File userDir = getUserDir(context);
            if (userDir != null) {
                //TODO: this setting should be part of the UserUploadFile class
                File f = new File(userDir, "uploads");
                userUploadPath = f.getPath();
            }
        }
            
        File userUploadDir = new File(userUploadPath);
        if (userUploadDir.exists()) {
            return userUploadDir;
        }
        boolean success = userUploadDir.mkdirs();
        if (success) {
            return userUploadDir;
        } 
        
        //otherwise, use the web upload dir
        log.error("Unable to create user.uploads directory for '"+context.getUserId()+"', userUploadDir="+userUploadDir.getAbsolutePath());
        return getTempDir();
    } 
    
    public File getTempDir() {
        String str = System.getProperty("java.io.tmpdir");
        return new File(str);
    }
    
    public boolean getAllowInputFilePaths(Context context) {
        return getGPBooleanProperty(context, "allow.input.file.paths", false);
    }
    
    public File getTemporaryUploadDir(Context context) throws IOException, Exception {
        String username = context.getUserId();
        if (username == null || username.length() == 0) {
            throw new Exception("userid not set");
        }

        File serverTempDir = getTempDir();
        // prefix is used to restrict access to input files based on username
        String prefix = username + "_";
        File tempDir = File.createTempFile(prefix + "run", null, serverTempDir);
        tempDir.delete();
        tempDir.mkdir();
        return tempDir;
    }

}
