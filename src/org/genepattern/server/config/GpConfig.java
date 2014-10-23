package org.genepattern.server.config;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.drm.JobRunner;
import org.genepattern.drm.Memory;
import org.genepattern.server.auth.IGroupMembershipPlugin;
import org.genepattern.server.executor.CommandExecutorMapper;
import org.genepattern.server.executor.CommandProperties;
import org.genepattern.server.repository.RepositoryInfo;
import org.genepattern.webservice.JobInfo;

import com.google.common.collect.ImmutableList;

public class GpConfig {
    private static Logger log = Logger.getLogger(GpConfig.class);

    /**
     * When true, display the 'Estimated Queue Time' details for the Congestion Indicator.
     */
    public static final String PROP_SHOW_ESTIMATED_QUEUETIME="gp.showEstimatedQueuetime";

    /**
     * Initialize the GenePatternURL from System.property
     * @return
     */
    private static URL initGpUrl(GpServerProperties serverProperties) {
        log.debug("Initializing GenePatternURL from server properties ...");
        URL gpUrl=null;
        String urlStr = null;
        if (serverProperties != null) {
            urlStr=serverProperties.getProperty("GenePatternURL");
        }
        if (urlStr==null) {
            urlStr="http://127.0.0.1:8080/gp/";
        }
        try {
            gpUrl=new URL(urlStr);
        }
        catch (Throwable t) {
            log.error("Error initializing GenePatternURL="+urlStr);
            try {
                gpUrl=new URL("http://127.0.0.1:8080/gp/");
            }
            catch(Throwable t1) {
                throw new IllegalArgumentException("shouldn't ever be here", t1);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("GenePatternURL="+gpUrl);
        }
        return gpUrl;
    }

    private static GpRepositoryProperties initRepoConfig(final File resourcesDir) {
        return new GpRepositoryProperties.Builder()
            .resourcesDir(resourcesDir)
            .build();
    }

    private final URL genePatternURL;
    private final String genePatternVersion;
    private final File resourcesDir;
    private final List<Throwable> initErrors;
    private final GpRepositoryProperties repoConfig;
    private final GpServerProperties serverProperties;
    private final ConfigYamlProperties yamlProperties;
    private final File configFile;
    // config helper method
    private final ValueLookup valueLookup;

    public GpConfig(final Builder in) {
        this.resourcesDir=in.resourcesDir;
        this.serverProperties=in.serverProperties;
        if (in.genePatternVersion != null) {
            this.genePatternVersion=in.genePatternVersion;
        }
        else if (this.serverProperties != null) {
            this.genePatternVersion=this.serverProperties.getProperty("GenePatternVersion");
        }
        else {
            log.error("GenePatternVersion not set");
            this.genePatternVersion="";
        }
        if (in.genePatternURL!=null) {
            this.genePatternURL=in.genePatternURL;
        }
        else {
            this.genePatternURL=initGpUrl(this.serverProperties);
        }
        if (in.configFromYaml != null && in.configFromYaml.getConfigYamlProperties() != null) {
            this.yamlProperties=in.configFromYaml.getConfigYamlProperties();
        }
        else {
            this.yamlProperties=null;
        }
        this.valueLookup=new ValueLookupFromConfigYaml(this.serverProperties, this.yamlProperties);

        if (in.initErrors==null) {
            this.initErrors=Collections.emptyList();
        }
        else {
            this.initErrors=ImmutableList.copyOf(in.initErrors);
        }
        this.configFile=in.configFile;
        this.repoConfig=initRepoConfig(this.resourcesDir);
    }

    /**
     * Get the public facing URL for this GenePattern Server.
     * Note: replaces <pre>System.getProperty("GenePatternURL");</pre>
     * @return
     */
    public URL getGenePatternURL() {
        return genePatternURL;
    }

    public String getGenePatternVersion() {
        return genePatternVersion;
    }
    
    public File getConfigFile() {
        return configFile;
    }
    
    public String getConfigFilepath() {
        if (configFile != null) {
            return configFile.getAbsolutePath();
        }
        return null;
    }

    public boolean hasInitErrors() {
        return initErrors != null && initErrors.size()>0;
    }
    
    public List<Throwable> getInitializationErrors() {
        return initErrors;
    }
    public File getResourcesDir() {
        return resourcesDir;
    }

    public Value getValue(final GpContext context, final String key) {
        if (valueLookup==null) {
            return null;
        }
        return valueLookup.getValue(context, key);
    }

    public Value getValue(final GpContext context, final String key, final Value defaultValue) {
        Value value=getValue(context, key);
        if (value==null) {
            return defaultValue;
        }
        return value;
    }
    
    /**
     * @deprecated, use getValue instead, which supports lists.
     * @param context
     * @param key
     * @return
     */
    public String getGPProperty(final GpContext context, final String key) {
        final Value value = getValue(context, key);
        if (value == null) {
            return null;
        }
        if (value.getNumValues() > 1) {
            log.error("returning first item of a "+value.getNumValues()+" item list");
        }
        return value.getValue();
    }
    
    public String getGPProperty(final GpContext context, final String key, final String defaultValue) {
        final Value value = getValue(context, key);
        if (value == null) {
            return defaultValue;
        }
        if (value.getNumValues() > 1) {
            log.error("returning first item of a "+value.getNumValues()+" item list");
        }
        return value.getValue();
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
     * Utility method for parsing a property as an Integer. If there is no property set
     * return null.
     * 
     * When a non integer value is set in the config file, return null.
     * Errors are logged, but exceptions are not thrown.
     * 
     * @param context
     * @param key
     * @return
     */
    public Integer getGPIntegerProperty(final GpContext context, final String key) {
        return getGPIntegerProperty(context, key, null);
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
    
    public Memory getGPMemoryProperty(final GpContext gpContext, final String key) {
        return getGPMemoryProperty(gpContext, key, null);

    }

    public Memory getGPMemoryProperty(final GpContext gpContext, final String key, final Memory defaultValue) {
        String val = getGPProperty(gpContext, key);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Memory.fromString(val);
        }
        catch (Throwable t) {
            log.error("Error parsing memory value for property, "+key+"="+val, t);
            return defaultValue;
        }
    }

    /**
     * Helper method for getting the queueId for a job. In most cases this is equivalent to the 'job.queue' property.
     * When the 'job.virtualQueue' is set, then that value is used.
     * The default value is the empty string when neither property is set.
     * 
     * @param gpContext
     * @return
     */
    public String getQueueId(final GpContext gpContext) {
        return getQueueId(gpContext, "");
    }
    
    public String getQueueId(final GpContext gpContext, final String defaultValue) {
        String queueId=getGPProperty(gpContext, JobRunner.PROP_VIRTUAL_QUEUE);
        if (queueId!=null) {
            return queueId;
        }
        queueId=getGPProperty(gpContext, JobRunner.PROP_QUEUE);
        if (queueId!=null) {
            return queueId;
        }
        return defaultValue;
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
            return getTempDir(null);
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
        return getTempDir(null);
    } 

    public File getTempDir(GpContext gpContext) {
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

        File serverTempDir = getTempDir(null);
        // prefix is used to restrict access to input files based on username
        String prefix = username + "_";
        File tempDir = File.createTempFile(prefix + "run", null, serverTempDir);
        tempDir.delete();
        tempDir.mkdir();
        return tempDir;
    }

    public Set<String> getRepositoryUrls() {
        if (repoConfig==null) {
            return Collections.emptySet();
        }
        return repoConfig.getRepositoryUrls();
    }
    public RepositoryInfo getRepositoryInfo(final String url) {
        if (repoConfig==null) {
            return null;
        }
        return repoConfig.getRepositoryInfo(url);
    }

    /**
     * @deprecated
     * @return
     */
    public JobConfigObj getJobConfiguration() {
        if (yamlProperties==null) {
            return null;
        }
        return yamlProperties.getJobConfiguration();
    }
    
    public String getExecutorId(final GpContext gpContext) {
        //special-case for pipelines
        if (gpContext != null && gpContext.getTaskInfo() != null) {
            final boolean isPipeline=gpContext.getTaskInfo().isPipeline();
            if (isPipeline) {
                return CommandExecutorMapper.PIPELINE_EXEC_ID;
            }
        }
        return getGPProperty(gpContext, "executor");
    }

    /**
     * @deprecated, should just call getValue(GpContext jobContext, "executor")
     * @param jobInfo
     * @return
     */
    public String getCommandExecutorId(final JobInfo jobInfo) {
        if (yamlProperties == null) {
            return null;
        }        
        return yamlProperties.getCommandExecutorId(jobInfo);
    }

    /**
     * @deprecated, should make direct calls to getValue with a jobContext instead.
     * @param jobInfo
     * @return
     */
    public CommandProperties getCommandProperties(JobInfo jobInfo) {
        if (yamlProperties == null) {
            return null;
        }
        return yamlProperties.getCommandProperties(jobInfo);
    }
    
    public static final class Builder {
        private URL genePatternURL=null;
        private String genePatternVersion=null;
        private File resourcesDir=null;
        private File configFile=null;
        private GpServerProperties.Builder serverPropertiesBuilder=null;
        private GpServerProperties serverProperties=null;
        private ConfigFromYaml configFromYaml=null;
        private IGroupMembershipPlugin groupInfo=null;
        private List<Throwable> initErrors=null;
        
        public Builder() {
        }

        public Builder genePatternURL(final URL gpUrl) {
            this.genePatternURL=gpUrl;
            return this;
        }
        public Builder addError(final Throwable t) {
            if (initErrors==null) {
                initErrors=new ArrayList<Throwable>();
            }
            initErrors.add(t);
            return this;
        }
        
        public Builder serverProperties(final GpServerProperties serverProperties) {
            this.serverProperties=serverProperties;
            return this;
        }

        public Builder resourcesDir(final File resourcesDir) {
            this.resourcesDir=resourcesDir;
            return this;
        }

        public Builder configFile(final File configFile) {
            this.configFile=configFile;
            return this;
        }
        
        public Builder groupInfo(final IGroupMembershipPlugin groupInfo) {
            this.groupInfo=groupInfo;
            return this;
        }
        
        public Builder addProperty(String key, String value) {
            if (serverPropertiesBuilder==null) {
                serverPropertiesBuilder=new GpServerProperties.Builder();
            }
            serverPropertiesBuilder.addCustomProperty(key, value);
            return this;
        }

        public GpConfig build() { 
            //parse the config file here
            if (configFile != null) {
                try {
                    configFromYaml=ConfigFileParser.parseYamlFile(configFile, groupInfo);
                }
                catch (ConfigurationException e) {
                    addError(e);
                }
            }

            //if not already set, set the resourcesDir
            if (resourcesDir == null) {
                if (configFile != null) {
                    resourcesDir=configFile.getParentFile();
                }
                else if (serverProperties != null) {
                    resourcesDir=serverProperties.getResourcesDir();
                }
            }
            
            if (serverProperties==null && serverPropertiesBuilder != null) {
                serverProperties=serverPropertiesBuilder.build();
            }

            return new GpConfig(this);
        }
    }
}
