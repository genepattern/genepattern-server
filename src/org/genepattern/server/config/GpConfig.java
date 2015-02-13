package org.genepattern.server.config;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
     * The version of the database for saving GP session data, default value is 'HSQL'. Other supported
     * options include 'ORACLE' and 'MYSQL'.
     */
    public static final String PROP_DATABASE_VENDOR="database.vendor";

    /**
     * When true, display the 'Estimated Queue Time' details for the Congestion Indicator.
     */
    public static final String PROP_SHOW_ESTIMATED_QUEUETIME="gp.showEstimatedQueuetime";

    /**
     * The directory to write temporary files to
     */
    public static final String PROP_GP_TMPDIR="gp.tmpdir";

    /**
     * The directory to write files uploaded from SOAP
     */
    public static final String PROP_SOAP_ATT_DIR="soap.attachment.dir";
    
    /**
     * The location for job results files. 
     * Each job runs in a new working directory, by default it is created in the 'jobs' folder,
     *     mkdir <jobs>/<jobId>, e.g.
     *     mkdir /Applications/GenePatternServer/jobResults/1
     */
    public static final String PROP_JOBS="jobs";
    
    /**
     * The location for installed patches (aka plugins).
     */
    public static final String PROP_PLUGIN_DIR="patches";
    
    /**
     * The location for installed tasks (aka modules and pipelines)
     */
    public static final String PROP_TASKLIB_DIR="tasklib";
    
    /**
     * Set the 'googleAnalytics.enabled' flag to true to enable Google Analytics for the GP server.
     * When 'true' the ./pages/gpTracking.xhtml file is loaded into the header page for the GP server.
     * You must also set the 'googleAnalytics.trackingId' property in the config yaml file.
     * 
     * For full customization, edit to the gpTracking.xhtml to include whatever code snippet suggested
     * by Google Analytics.
     * 
     */
    public static final String PROP_GA_ENABLED="googleAnalytics.enabled";

    /**
     * Set the 'googleAnalytics.trackingId' for the server.
     * @see PROP_GA_ENABLED
     */
    public static final String PROP_GA_TRACKING_ID="googleAnalytics.trackingId";

    public static String normalizePath(String pathStr) {
        if (pathStr==null) {
            return pathStr;
        }
        try {
            Path thePath=Paths.get(pathStr);
            thePath=thePath.normalize();
            URI uri=thePath.toUri();
            String rval=uri.getPath();
            return rval;
        }
        catch (Throwable t) {
            System.err.print("Error intializing path from String="+pathStr);
            t.printStackTrace();
            return pathStr;
        }
    }
    
    /**
     * Get the current version of GenePattern, (e.g. '3.9.1'). 
     * Automatic schema update is based on the difference between this value (as defined by the GP installation)
     * and the entry in the database.
     * 
     */
    protected String initGenePatternVersion(GpContext gpContext) {
        String gpVersion=this.getGPProperty(gpContext, "GenePatternVersion", "3.9.2");
        //for junit testing, if the property is not in ServerProperties, check System properties
        if ("$GENEPATTERN_VERSION$".equals(gpVersion)) {
            log.info("GenePatternVersion=$GENEPATTERN_VERSION$, using hard-coded value");
            gpVersion="3.9.1";
        }
        return gpVersion;
    }
    
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

    private final File gpHomeDir;
    private final URL genePatternURL;
    private final String gpUrl;
    private final String genePatternVersion;
    private final File logDir;
    private final File gpLogFile;
    private final File webserverLogFile;
    private final File resourcesDir;
    private final File gpWorkingDir;
    private final File jobsDir;
    private final File userRootDir;
    private final File soapAttachmentDir;
    private final File gpTmpDir;
    private final File gpPluginDir;
    private final List<Throwable> initErrors;
    private final GpRepositoryProperties repoConfig;
    private final GpServerProperties serverProperties;
    private final ConfigYamlProperties yamlProperties;
    private final File configFile;
    private final Properties dbProperties;
    private final String dbVendor;
    /**
     *  Special-case, some properties can be set by convention rather than declared in a config file.
     *  For example,  patches=$GENEPATTERN_HOME$/patches
     *  When this is the case, save the lookup into the subsitutionParams map when initializing the config.
     */
    private final Map<String,String> substitutionParams=new HashMap<String,String>();
    private final ValueLookup valueLookup;

    public GpConfig(final Builder in) {
        GpContext gpContext=GpContext.getServerContext();
        this.gpHomeDir=in.gpHomeDir;
        if (in.logDir!=null) {
            this.logDir=in.logDir;
        }
        else {
            this.logDir=initLogDir();
        }
        this.gpLogFile=new File(logDir, "genepattern.log");
        this.webserverLogFile=new File(logDir, "webserver.log");
        this.resourcesDir=in.resourcesDir;
        if (in.gpWorkingDir==null) {
            // legacy server, assume startup in <GenePatternServer>/Tomcat folder.
            this.gpWorkingDir=new File("").getAbsoluteFile();
        }
        else {
            this.gpWorkingDir=in.gpWorkingDir;
        }
        this.serverProperties=in.serverProperties;
        if (in.configFromYaml != null && in.configFromYaml.getConfigYamlProperties() != null) {
            this.yamlProperties=in.configFromYaml.getConfigYamlProperties();
        }
        else {
            this.yamlProperties=null;
        }
        this.valueLookup=new ValueLookupFromConfigYaml(this.serverProperties, this.yamlProperties);

        if (in.genePatternURL!=null) {
            this.genePatternURL=in.genePatternURL;
        }
        else {
            this.genePatternURL=initGpUrl(this.serverProperties);
        }
        this.gpUrl=this.genePatternURL.toExternalForm();
        if (in.genePatternVersion==null || in.genePatternVersion.equals("$GENEPATTERN_VERSION$")) {
            this.genePatternVersion=initGenePatternVersion(gpContext);
        }
        else {
            this.genePatternVersion=in.genePatternVersion;
        }
        if (in.initErrors==null) {
            this.initErrors=Collections.emptyList();
        }
        else {
            this.initErrors=ImmutableList.copyOf(in.initErrors);
        }
        this.configFile=in.configFile;
        this.repoConfig=initRepoConfig(this.resourcesDir);
        this.jobsDir=initJobsDir(gpContext);
        this.userRootDir=initUserRootDir();
        this.soapAttachmentDir=initSoapAttachmentDir(gpContext);
        this.gpTmpDir=initGpTmpDir(gpContext);
        this.dbProperties=initDbProperties(gpContext, this.resourcesDir);
        this.dbVendor=initDbVendor(gpContext);
        this.gpPluginDir=initRootDir(gpContext, PROP_PLUGIN_DIR, "patches");
    }
    
    /**
     * Initialize the database properties from the resources directory.
     * Load properties from 'resources/database_default.properties', if present.
     * Load additional properties from 'resources/database_custom.properties', if present.
     * The custom properties take precedence.
     * 
     * When 'database.vendor=HSQL', attempt to set the jdbcUrl based on the value of the HSQL_port property.
     * 
     * If neither file is present, log an error and return null.
     * 
     * @param gpConfig
     * @return 
     */
    protected Properties initDbProperties(final GpContext gpContext, final File resourcesDir) {
        Properties hibProps=null;
        File hibPropsDefault=new File(resourcesDir, "database_default.properties");
        
        if (hibPropsDefault.exists()) {
            if (!hibPropsDefault.canRead()) {
                log.error("Can't read 'database_default.properties' file="+hibPropsDefault);
            }
            else {
                hibProps=GpServerProperties.loadProps(hibPropsDefault);
            }
        }
        File hibPropsCustom=new File(resourcesDir, "database_custom.properties");
        if (hibPropsCustom.exists()) {
            if (!hibPropsCustom.canRead()) {
                log.error("Can't read 'database_custom.properties' file="+hibPropsCustom);
            }
            else {
                if (hibProps==null) {
                    hibProps=new Properties();
                }
                GpServerProperties.loadProps(hibProps, hibPropsCustom);
            }
        }
        
        if (hibProps==null) {
            log.error("Error, missing required configuration file 'database_default.properties'");
            return null;
        }
        
        initHsqlConnectionUrl(gpContext, hibProps);
        return hibProps;
    }
    
    /**
     * Generate hard-coded database properties.
     * @param gpContext
     * @return
     */
    public Properties getDbPropertiesDefault(GpContext gpContext) {
        log.warn("Using hard-coded database properties");
        // use hard-coded DB properties
        Properties hibProps=new Properties();
        hibProps=new Properties();
        hibProps.setProperty("database.vendor","HSQL");
        hibProps.setProperty("HSQL_port","9001");
        hibProps.setProperty("hibernate.current_session_context_class","thread");
        hibProps.setProperty("hibernate.transaction.factory_class","org.hibernate.transaction.JDBCTransactionFactory");
        hibProps.setProperty("hibernate.connection.provider_class","org.hibernate.connection.C3P0ConnectionProvider");
        hibProps.setProperty("hibernate.jdbc.batch_size","20");
        hibProps.setProperty("hibernate.statement_cache.size","0");
        hibProps.setProperty("hibernate.connection.driver_class","org.hsqldb.jdbcDriver");
        hibProps.setProperty("hibernate.username","sa");
        hibProps.setProperty("hibernate.password","");
        hibProps.setProperty("hibernate.dialect","org.hibernate.dialect.HSQLDialect");
        hibProps.setProperty("hibernate.default_schema","PUBLIC");
        initHsqlConnectionUrl(gpContext, hibProps);
        return hibProps;
    }

    protected String initDbVendor(final GpContext gpContext) {
        if (dbProperties != null) {
            return dbProperties.getProperty(PROP_DATABASE_VENDOR, "HSQL");
        }
        else {
            return getGPProperty(gpContext, PROP_DATABASE_VENDOR, "HSQL");
        }
    }
    
    /**
     * Special-case for default database.vendor=HSQL, set the 'hibernate.connection.url' from the 'HSQL_port'.
     * @param gpConfig
     * @param gpContext
     * @param hibProps
     */
    private void initHsqlConnectionUrl(GpContext gpContext, Properties hibProps) {
        //special-case for default database.vendor=HSQL
        if ("hsql".equalsIgnoreCase(hibProps.getProperty(PROP_DATABASE_VENDOR))) {
            Integer hsqlPort=null;
            final String PROP_HSQL_PORT="HSQL_port";
            if (hibProps.containsKey(PROP_HSQL_PORT)) {
                try {
                    hsqlPort=Integer.parseInt( hibProps.getProperty(PROP_HSQL_PORT) );
                }
                catch (Throwable t) {
                    log.error("Error in config file, expecting an Integer value for "+PROP_HSQL_PORT+"="+hibProps.getProperty(PROP_HSQL_PORT), t);
                }
            }
            if (hsqlPort==null) {
                hsqlPort=getGPIntegerProperty(gpContext, PROP_HSQL_PORT, 9001);
            }
            final String PROP_HIBERNATE_CONNECTION_URL="hibernate.connection.url";
            if (!hibProps.containsKey(PROP_HIBERNATE_CONNECTION_URL)) {
                String jdbcUrl="jdbc:hsqldb:hsql://127.0.0.1:"+hsqlPort+"/xdb";
                hibProps.setProperty(PROP_HIBERNATE_CONNECTION_URL, jdbcUrl);
                log.debug("setting "+PROP_HIBERNATE_CONNECTION_URL+"="+jdbcUrl);
            }
        }
    }
    
    /**
     * Initialize root 'jobs' directory, the globally set path to the jobResults directory.
     * Lecacy (GP <= 3.9.0) default location is './Tomcat/webapps/gp/jobResults'.
     * Newer default location is a fully qualified path to the installation directory: <GenePatternServer>/jobResults.
     * 
     * @param valueLookup
     * @return
     */
    private File initJobsDir(final GpContext gpContext) { 
        File jobsDir=relativize(gpWorkingDir, getGPProperty(gpContext, GpConfig.PROP_JOBS, "../jobResults"));
        jobsDir=new File(normalizePath(jobsDir.getPath()));
        if (!jobsDir.exists()) {
            boolean success=jobsDir.mkdirs();
            if (success) {
                log.info("created '"+PROP_JOBS+"' directory="+jobsDir);
            }
        }
        return jobsDir;
    }

    public static File relativize(final File rootDir, final String pathStr) {
        File path=new File(pathStr);
        if (path.isAbsolute()) {
            return path;
        }
        else if (rootDir != null) {
            return new File(rootDir, pathStr);
        }
        else {
            return path.getAbsoluteFile();
        }
    }
    
    /**
     * Convert the given file path into an absolute path if necessary.
     * If GP_HOME is set, assume the path is relative to GP_HOME,
     * else if GP_WORKING_DIR is set, assume the path is relative to GP_WORKING_DIR,
     * else assume the path is relative to the current working dir, System.getProperty("user.dir").
     * 
     * @param gpContext
     * @param pathOrRelativePath
     * @return
     */
    protected File initAbsolutePath(final GpContext gpContext, final String pathOrRelativePath) { 
        final File rootDir;
        if (this.gpHomeDir != null) {
            rootDir=this.gpHomeDir;
        }
        else if (this.gpWorkingDir != null) {
            rootDir=this.gpWorkingDir;
        }
        else {
            rootDir=new File(System.getProperty("user.dir"));
        }
        File f = relativize(rootDir, pathOrRelativePath);
        f = new File(normalizePath(f.getPath()));
        return f;
    }
    
    /**
     * Helper method for initializing an absolute path to a data file directory, for example for the 'patches' or 'taskLib'.
     * This takes care of legacy support where data file paths were declared in the genepattern.properties file as relative
     * paths to the working directory for the application server, e.g.
     *     patches=../patches
     *     tasklib=../taskLib
     * 
     * When GENEPATTERN_HOME is defined, default paths are in the GENEPATTERN_HOME directory.
     * When not defined, default paths are one level up from the GENEPATTERN_WORKING_DIRECTORY.
     * 
     * @param serverContext, a valid server context
     * @param propName, the name of the property (optionally loaded from the config file)
     * @param defaultDirName, the default file system name for the data directory.
     * 
     * @return
     */
    protected File initRootDir(final GpContext serverContext, String propName, String defaultDirName) {
        String dirProp=getGPProperty(serverContext, propName);
        boolean isSubstitutionParam=false;
        if (dirProp == null) {
            isSubstitutionParam=true;
            if (gpHomeDir != null) {
                dirProp=defaultDirName;
            }
            else if (gpWorkingDir != null) {
                dirProp="../"+defaultDirName;
            }
        }
        File f=initAbsolutePath(serverContext, dirProp);
        if (isSubstitutionParam) {
            this.substitutionParams.put(propName, ""+f);
        }
        return f;
    }
    
    protected File initUserRootDir() {
        String userRootDirProp=getGPProperty(GpContext.getServerContext(), "user.root.dir");
        File userRootDir;
        if (userRootDirProp != null) {
            userRootDir=relativize(gpWorkingDir, userRootDirProp);
        }
        else {
            userRootDir=relativize(gpWorkingDir, "../users");
        }
        userRootDir=new File(normalizePath(userRootDir.getPath()));
        if (!userRootDir.exists()) {
            boolean success=userRootDir.mkdirs();
            if (success) {
                log.info("created 'user.root.dir' directory="+userRootDir);
            }
        }
        return userRootDir;
    }
    
    protected File initSoapAttachmentDir(final GpContext gpContext) {
        File soapAttDir=relativize(gpWorkingDir, getGPProperty(gpContext, GpConfig.PROP_SOAP_ATT_DIR, "../temp/attachments"));
        soapAttDir=new File(normalizePath(soapAttDir.getPath()));
        if (!soapAttDir.exists()) {
            boolean success=soapAttDir.mkdirs();
            if (success) {
                log.info("created '"+PROP_SOAP_ATT_DIR+"' directory="+soapAttDir);
            }
        }
        return soapAttDir;
    }
    
    /**
     * Initialize the global path to the temp directory.
     * If 'gp.tmpdir' is defined use that value, otherwise fall back to use 'java.io.tmpdir'.
     * Relative paths are relative to the gpWorkingDir.
     * 
     * @see PROP_GP_TMPDIR
     * 
     * @param gpContext
     * @return
     */
    protected File initGpTmpDir(final GpContext gpContext) {
        File gpTmpDir=relativize(gpWorkingDir, getGPProperty(gpContext, GpConfig.PROP_GP_TMPDIR, System.getProperty("java.io.tmpdir")));
        gpTmpDir=new File(normalizePath(gpTmpDir.getPath())); 
        if (!gpTmpDir.exists()) {
            boolean success=gpTmpDir.mkdirs();
            if (success) {
                log.info("created '"+PROP_GP_TMPDIR+"' directory="+gpTmpDir);
            }
        }
        return gpTmpDir;
    }

    /**
     * Get the public facing URL for this GenePattern Server.
     * Note: replaces <pre>System.getProperty("GenePatternURL");</pre>
     * @return
     */
    public URL getGenePatternURL() {
        return genePatternURL;
    }

    /**
     * Get the String representation of the <GenePatternURL>, including the trailing slash.
     * @return
     */
    public String getGpUrl() {
        return gpUrl;
    }
    
    /**
     * Get the servlet path.
     * Note: replaces <pre>System.getProperty("GP_Path", "/gp");</pre>
     * @return
     */
    public String getGpPath() {
        return System.getProperty("GP_Path", "/gp");
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

    /**
     * Get the database configuration properties loaded from the <resources>/database_default.properties and
     * optionally from the <resources>/database_custom.properties files.
     * @return
     */
    public Properties getDbProperties() {
        return dbProperties;
    }

    /**
     * Get the database vendor which can be one of 'HSQL', 'ORACLE', or 'MYSQL'.
     * @return
     */
    public String getDbVendor() {
        return dbVendor;
    }

    /**
     * Get the database schema prefix, a prefix pattern (e.g. 'analysis_hypersonic', 'analysis_oracle') 
     * which is used to get a listing of the DDL scripts for automatic initialization of the database after
     * installing or updating the genepattern server.
     * 
     * @return
     */
    public String getDbSchemaPrefix() {
        if (this.dbVendor.equalsIgnoreCase("HSQL")) {
            return "analysis_hypersonic-";
        }
        else {
            return "analysis_"+dbVendor.toLowerCase()+"-";
        }
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
            return this.substitutionParams.get(key);
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
     * Get the queueId for a job, this is the value which should be logged into the 'job_runner_job' table in the GP database.
     * This method was added to support the 'job.virtualQueue' feature.
     * 
     * Use the getGPProperty(gpContext, JobRunner.PROP_QUEUE) method to get the actual queue name to be submitted to an external queuing system.
     * 
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
     * By default, user home directories are created in the <user.root.dir> directory.
     * The 'gp.user.dir' property can be set on a per user basis to change the default location.
     *
     * Note: If an admin sets a custom 'gp.user.dir' for an existing user, they must manually migrate pre-existing files.
     *     (1) Move existing files into the correct 'gp.user.dir'
     *     (2) Resync Files from the Adminstration -> Server Settings -> Uploaded Files page
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
        File gpUserDir;
        if (userDirPath != null) {
            gpUserDir=new File(userDirPath);
        }
        else {
            gpUserDir=new File(userRootDir, context.getUserId());
        }
        if (gpUserDir.exists()) {
            return gpUserDir;
        }
        boolean success = gpUserDir.mkdirs();
        if (!success) {
            throw new IllegalArgumentException("Unable to create home directory for user "+context.getUserId()+", userDir="+gpUserDir.getAbsolutePath());
        }
        return gpUserDir;
    }

    /**
     * Get the job results directory for the server. This is a global server property.
     * In GP > 3.9.0, the default location is the GenePattern installation directory,
     *     <GenePatternServer>/jobResults
     * 
     * In GP <= 3.9.0 and earlier, the default location is the web application directory,
     *     <GenePatternServer>/Tomcat/webapps/gp/jobResults
     *
     * The 'jobs' property sets an alternate location. For best results set this to a global path in the 
     * 'default' section of the config_yaml file.
     * 
     * Note: Custom settings are not enabled.
     *
     * @return the parent directory in which to create the new working directory for a job.
     */
    public File getRootJobDir(final GpContext context) throws ServerConfigurationException {
        return jobsDir;
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
    
    /**
     * Get the globally configured location for installing plugins (aka patches). In GP <= 3.9.1 
     * this is defined in the 'genepattern.properties' file via the template:
     * <pre>
$USER_INSTALL_DIR$/patches
     * </pre>
     * In newer installations it should not be declared in the properties or config file. 
     * It is defined relative to GENEPATTERN_HOME:
     * <pre>
$GENEPATTERN_HOME$/patches
     * </pre>
     * @param serverContext
     * @return
     */
    public File getRootPluginDir(GpContext serverContext) {
        return gpPluginDir;
    }
    
    /**
     * Get the globally configured location for installing modules and pipelines.
     * In GP <= 3.9.1 this is defined in the 'genepattern.properties' file via the template:
     * <pre>
tasklib=$USER_INSTALL_DIR$/taskLib
     * </pre>
     * In newer versions of GP, the default location is relative to GENEPATTERN_HOME:
     * <pre>
$GENEPATTERN_HOME$/tasklib
     * </pre>
     * The 'tasklib' property can be overwritten in the config_yaml file.
     * <pre>
    tasklib: /fully/qualified/path/to/tasklib
     * </pre>
     * 
     * @param serverContext
     * @return
     */
    public File getRootTasklibDir(GpContext serverContext) {
        return initRootDir(serverContext, PROP_TASKLIB_DIR, "taskLib");
    }

    public File getGPFileProperty(final GpContext gpContext, final String key) {
        return getGPFileProperty(gpContext, key, null);
    }

    public File getGPFileProperty(final GpContext gpContext, final String key, final File defaultValue)
    {
        Value val = getValue(gpContext, key);

        if (val == null) {
            return defaultValue;
        }

        try {
            return new File(val.getValue());
        }
        catch (Throwable t) {
            log.error("Error parsing memory value for property, "+key+"="+val, t);
            return defaultValue;
        }
    }

    public File getSoapAttDir(GpContext gpContext) {
        return this.soapAttachmentDir;
    }

    public File getTempDir(GpContext gpContext) {
        return gpTmpDir;
    }

    private File initLogDir() 
    {
        File logDir = null;
        if(System.getProperty("GENEPATTERN_HOME") != null)
        {
            logDir = new File(System.getProperty("GENEPATTERN_HOME"), "logs");
        }
        else
        {
            logDir = new File("../logs");
        }

        return logDir;
    }

    public File getGPLogFile(GpContext gpContext)
    {
        return gpLogFile;
    }

    public File getWsLogFile(GpContext gpContext)
    {
        return webserverLogFile;
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
        private File gpHomeDir=null;
        private File logDir=null;
        private File resourcesDir=null;
        private File configFile=null;
        private File gpWorkingDir=null;
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
        
        public Builder gpHomeDir(final File gpHomeDir) {
            this.gpHomeDir=gpHomeDir;
            return this;
        }

        public Builder gpWorkingDir(final File gpWorkingDir) {
            this.gpWorkingDir=gpWorkingDir;
            return this;
        }
        
        public Builder logDir(final File logDir) {
            this.logDir=logDir;
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

        public Builder addProperties(Properties props) {
            for(final Object keyObj : props.keySet()) {
                String key = keyObj.toString();
                addProperty(key, props.getProperty(key));
            }
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
                else if (gpHomeDir != null) {
                    resourcesDir=new File(gpHomeDir,"resources");
                }
            }

            if (serverProperties==null && serverPropertiesBuilder != null) {
                serverProperties=serverPropertiesBuilder.build();
            }

            return new GpConfig(this);
        }
    }
}
