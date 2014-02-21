package org.genepattern.server.config;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.server.executor.CommandProperties;
import org.genepattern.server.repository.ConfigRepositoryInfoLoader;
import org.genepattern.server.repository.RepositoryInfo;

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

    ServerConfiguration() {
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
    
    synchronized void reloadConfiguration() {
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
    
    synchronized void reloadConfiguration(String configFilepath) {
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

        // parse the repo.yaml and optional repo_custom.yaml file
        boolean parsedRepoInfo=false;
        try {
            final File defaultRepositoryFile=new File(System.getProperty("resources"), "repo.yaml");
            this.repositoryDetails=ConfigRepositoryInfoLoader.parseRepositoryDetailsYaml(defaultRepositoryFile);
            parsedRepoInfo=true;
        }
        catch (Throwable t) {
            log.error("Error in repo.yaml", t);
            errors.add(new Exception("Error in repo.yaml: "+t.getLocalizedMessage(), t));
        }
        if (parsedRepoInfo) {            
            try {
                final File customRepositoryFile=new File(System.getProperty("resources"), "repo_custom.yaml");
                final Map<String,RepositoryInfo> custom=ConfigRepositoryInfoLoader.parseRepositoryDetailsYaml(customRepositoryFile);
                for(Entry<String,RepositoryInfo> entry : custom.entrySet()) {
                    this.repositoryDetails.put(entry.getKey(), entry.getValue());
                }
            }
            catch (Throwable t) {
                log.error("Error in repo_custom.yaml", t);
                errors.add(new Exception("Error in repo_custom.yaml: "+t.getLocalizedMessage(), t));
            }
            ConfigRepositoryInfoLoader.clearCache();
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
    private JobConfigObj jobConfig = new  JobConfigObj(null);
    public JobConfigObj getJobConfiguration() {
        return jobConfig;
    }
    private Map<String,RepositoryInfo> repositoryDetails=Collections.emptyMap();
    public Set<String> getRepositoryUrls() {
        if (repositoryDetails==null || repositoryDetails.size()==0) {
            return Collections.emptySet();
        }
        return repositoryDetails.keySet();
    }
    public RepositoryInfo getRepositoryInfo(final String url) {
        if (repositoryDetails==null) {
            return null;
        }
        return repositoryDetails.get(url);
    }


    /**
     * Utility method for parsing properties as a boolean.
     * The current implementation uses Boolean.parseBoolean, 
     * which returns true iff the property is set and equalsIgnoreCase 'true'.
     * 
     * @param key
     * @return
     */
    public boolean getGPBooleanProperty(final GpContext context, final String key) {
        String prop = getGPProperty(context, key);
        return Boolean.parseBoolean(prop);
    }
    
    public boolean getGPBooleanProperty(final GpContext context, final String key, final boolean defaultValue) {
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
    public Integer getGPIntegerProperty(final GpContext context, final String key, final Integer defaultValue) {
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
    
    public Long getGPLongProperty(final GpContext context, final String key, final Long defaultValue) {
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
    public String getGPProperty(final GpContext context, final String key) {
        if (cmdMgrProps == null) {
            log.error("Invalid server configuration in getGPProperty("+key+")");
            return null;
        }
        return cmdMgrProps.getProperty(context, key);
    }
    
    public String getGPProperty(final GpContext context, final String key, final String defaultValue) {
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
    
    public CommandProperties.Value getValue(final GpContext context, final String key) {
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
    public File getUserDir(final GpContext context) {
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
    public File getRootJobDir(final GpContext context) throws ServerConfigurationException {
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
            throw new ServerConfigurationException("Missing required propery, 'jobs'");
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
//    private File getJobDir(Context context) throws ServerConfigurationException {
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
     * By default, user uploads are stored in ../users/<user.id>/uploads/
     * 
     * The default location can be overridden with the 'user.upload.dir' property.
     * If there is no 'user.upload.dir' or 'gp.user.dir' set, then 'java.io.tempdir' is returned.
     * 
     * @param context
     * @return
     * @throws IllegalArgumentException if a directory is not found for the userId.
     */
    public File getUserUploadDir(final GpContext context) throws IllegalArgumentException {
        boolean configError = false;
        if (context == null) {
            configError = true;
            //throw new IllegalArgumentException("context is null");
            log.error("context is null");
        }
        else if (context.getUserId() == null) {
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
    
    public boolean getAllowInputFilePaths(final GpContext context) {
        return getGPBooleanProperty(context, "allow.input.file.paths", false);
    }
    
    public File getTemporaryUploadDir(final GpContext context) throws IOException, Exception {
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
