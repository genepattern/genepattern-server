/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Security;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Vector;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.database.HsqlDbUtil;
import org.genepattern.server.database.SchemaUpdater;
import org.genepattern.server.executor.CommandManagerFactory;
import org.genepattern.server.message.SystemAlertFactory;
import org.genepattern.server.plugin.MigratePlugins;
import org.genepattern.server.purger.PurgerFactory;
import org.genepattern.server.taskinstall.MigrateTaskCategories;
import org.genepattern.server.util.JobResultsFilenameFilter;
import org.genepattern.server.webapp.jsf.AboutBean;
import org.genepattern.webservice.TaskInfoCache;

import com.google.common.base.Strings;

/*
 * GenePattern startup servlet
 * 
 * This servlet performs periodic maintenance, based on definitions in the <genepattern.properties>/genepattern.properties file
 * @author Jim Lerner
 */
public class StartupServlet extends HttpServlet {

    private static Vector<Thread> vThreads = new Vector<Thread>();

    private Logger log=null;
    private Logger getLog() {
        if (this.log==null) {
            return Logger.getLogger(StartupServlet.class);
        }
        return this.log;
    }

    private File webappDir=null;
    private File gpWorkingDir=null;
    private File gpHomeDir=null;
    private File gpResourcesDir=null;
    private File gpJobResultsDir=null;

    public StartupServlet() {
    }
    
    public String getServletInfo() {
        return "GenePatternStartupServlet";
    }
    
    protected File getWebappDir() {
        return webappDir;
    }
    
    protected void setGpWorkingDir(final File gpWorkingDir) {
        this.gpWorkingDir=gpWorkingDir;
    }
    
    protected File getGpWorkingDir() {
        return this.gpWorkingDir;
    }
    
    protected void setGpHomeDir(final File gpHomeDir) {
        this.gpHomeDir=gpHomeDir;
    }

    public File getGpHomeDir() {
        return gpHomeDir;
    }
    
    protected void setGpJobResultsDir(final File gpJobResultsDir) {
        this.gpJobResultsDir=gpJobResultsDir;
    }
    
    protected File getGpJobResultsDir() {
        return this.gpJobResultsDir;
    }
    
    /**
     * Initialize the webapp dir (e.g. ./Tomcat/webapps/gp) for the servlet.
     * Implemented to work with Servlet spec <= 2.5, e.g. Tomcat-5.5
     */
    protected static File initWebappDir(final ServletConfig servletConfig) {
        return new File(servletConfig.getServletContext().getRealPath(""));    
    }
    
    /**
     * Initialize the 'working directory' from which to resolve relative file paths.
     * In previous versions (GP <= 3.9.0) relative paths were resolved relative to the working directory
     * from which the GenePattern Server (or application server) is launched.
     *     <pre>System.getProperty("user.dir");</pre>
     * 
     * In GP >=3.9.1, the default location is computed from the webappDir.
     * E.g.
     *     webappDir=<GenePatternServer>/Tomcat/webapps/gp
     *     workingDir=<GenePatternServer>/Tomcat
     *     <pre></pre>
     *     
     * For debugging/developing, you can override the default settings with a system property, 
     *     -DGENEPATTERN_WORKING_DIR=/fully/qualified/path
     */
    protected static File initGpWorkingDir(final ServletConfig servletConfig, final File gpHomeDir) {
        return initGpWorkingDir(System.getProperty("GENEPATTERN_WORKING_DIR"), servletConfig, gpHomeDir);
    }
    
    protected static File initGpWorkingDir(final String gpWorkingDirProp, final ServletConfig servletConfig, final File gpHomeDir) {
        if (!Strings.isNullOrEmpty(gpWorkingDirProp)) {
            return new File(gpWorkingDirProp);
        }
        
        if (gpHomeDir != null) {
            return null;
        }

        String gpWorkingDir=GpConfig.normalizePath(servletConfig.getServletContext().getRealPath("../../"));
        return new File(gpWorkingDir);
    }
    
    protected static String getGpHomeDirProp(final ServletConfig config) {
        String gpHome=System.getProperty("GENEPATTERN_HOME", System.getProperty("gp.home", null));
        if (!Strings.isNullOrEmpty(gpHome)) {
            return gpHome;
        }
        gpHome = config.getInitParameter("GENEPATTERN_HOME");
        if (!Strings.isNullOrEmpty(gpHome)) {
            return gpHome;
        }
        gpHome = config.getInitParameter("gp.home");
        if (!Strings.isNullOrEmpty(gpHome)) {
            return gpHome;
        }
        gpHome = System.getenv("GENEPATTERN_HOME");
        if (!Strings.isNullOrEmpty(gpHome)) {
            return gpHome;
        }
        return null;
    }

    /**
     * Initialize the path to the GENEPATTERN_HOME directory for the web application.
     * 
     * If it's set as a system property, then use that value.
     * If it's not already set as a system property then ...
     *     Check the config.initParmater
     * If it's not set as an initParameter then ...
     *     Assume it's relative to the web application directory.
     *     
     * @param config
     * @return
     */
    protected static File initGpHomeDir(final ServletConfig config) {
        final String gpHomeProp=getGpHomeDirProp(config);
        if (Strings.isNullOrEmpty(gpHomeProp)) {
            return null;
        }
        
        //normalize
        final String gpHome=GpConfig.normalizePath(gpHomeProp);
        File gpHomeDir=new File(gpHome);
        if (gpHomeDir.isAbsolute()) {
            return gpHomeDir;
        }
        else { 
            // special-case: handle relative path
            System.err.println("GENEPATTERN_HOME='"+gpHome+"': Should not be a relative path");
            gpHomeDir=new File(config.getServletContext().getRealPath("../../../"), gpHome).getAbsoluteFile();
            System.err.println("Setting GENEPATTERN_HOME="+gpHomeDir);
            return gpHomeDir;
        } 
    }
    
    /**
     * Get the path to the 'resources' directory for the web application.
     * @param gpHomeDir, the GENEPATTERN_HOME directory, can be null for legacy servers
     * @param gpWorkingDir, the working directory for the GenePattern Server
     */
    protected static File initResourcesDir(final File gpHomeDir, final File gpWorkingDir) {
        File resourcesDir;
        if (gpHomeDir != null) {
            resourcesDir=new File(gpHomeDir, "resources");
        }
        else {
            resourcesDir=new File(gpWorkingDir, "../resources");
            if (!resourcesDir.exists()) {
                // check for a path relative to working dir
                resourcesDir=new File("../resources");
            }
        }
        return new File(GpConfig.normalizePath(resourcesDir.getPath())).getAbsoluteFile();
    }

    /**
     * Get the optional custom path to the 'gp.log' directory
     * @return the 'gp.log' property or null if not set
     */
    protected static String getCustomGpLog(final ServletConfig config) {
        String gpLog=System.getProperty("GP_LOG", System.getProperty("gp.log", null));
        if (!Strings.isNullOrEmpty(gpLog)) {
            return gpLog;
        }
        gpLog = config.getInitParameter("GP_LOG");
        if (!Strings.isNullOrEmpty(gpLog)) {
            return gpLog;
        }
        gpLog = config.getInitParameter("gp.log");
        if (!Strings.isNullOrEmpty(gpLog)) {
            return gpLog;
        }
        gpLog = System.getenv("GP_LOG");
        if (!Strings.isNullOrEmpty(gpLog)) {
            return gpLog;
        }
        gpLog = System.getenv("gp.log");
        if (!Strings.isNullOrEmpty(gpLog)) {
            return gpLog;
        }
        return null;
    }

    /**
     * Initialize the path to the Log4J logs directory.
     * Default:
     *   <gp.home>/logs
     * Legacy:
     *   ../logs
     * Custom:
     *   <gp.log>
     */
    protected static File initLogDirPath(final ServletConfig servletConfig, final File gpHomeDir, final File gpWorkingDir) {
        // customize by setting 'gp.log' as a system property
        final String customGpLog=getCustomGpLog(servletConfig);
        if (!Strings.isNullOrEmpty(customGpLog)) {
            return new File(customGpLog);
        }
        // By default, logDir is '<gpHomeDir>/logs'
        if (gpHomeDir != null) {
            return new File(gpHomeDir, "logs");
        }
        // On older versions of GP (which don't define gpHomeDir) the logDir is '<gpWorkingDir>../logs'
        else if (gpWorkingDir != null) {
            return new File(gpWorkingDir.getParentFile(), "logs");
        }
        else {
            System.err.println("gpHomeDir and gpWorkingDir are not defined, setting log dir relative to 'user.dir'");
            return new File(GpConfig.getJavaProperty("user.dir"), "logs");
        }
    }

    /**
     * Initialize the Log4J Logger instance. Creates the 'logDir' if necessary.
     * Sets the 'gp.log' system property. This is required for the ${gp.log} 
     * substitutions in the ./resources/log4j.properties file.
     * 
     * @param logDir the location for the log files
     * @param log4jProps the path to the log4j.properties file
     */
    protected static Logger initLogger(final File logDir, final File log4jProps) {
        try {
            // create the gp.log directory
            if (!logDir.exists()) {
                System.out.println("creating logDir='"+logDir+"' ... ");
                boolean success=logDir.mkdirs();
                if (success) {
                    System.out.println("    Success!");
                }
                else {
                    System.out.println("    Failed!");
                }
            }

            System.setProperty("gp.log", logDir.getAbsolutePath());
            ServerConfigurationFactory.setLogDir(logDir);
            PropertyConfigurator.configure(log4jProps.getAbsolutePath());
            return Logger.getLogger(StartupServlet.class);
        }
        catch (Throwable t) {
            System.err.println("Error initializing logger");
            t.printStackTrace();
            return null;
        }
    }

    /**
     * Initialize the Log4J Logger instance
     */
    protected static Logger initLogger(final ServletConfig servletConfig, final File gpHomeDir, final File gpWorkingDir, final File gpResourcesDir) {
        try {
            final File logDirPath=initLogDirPath(servletConfig, gpHomeDir, gpWorkingDir);
            final File logDir=new File(GpConfig.normalizePath(logDirPath.getPath()));
            //final File log4jProps=new File(GpConfig.normalizePath(new File(gpResourcesDir, "log4j.properties").getPath()));
            final File log4j2Props=new File(GpConfig.normalizePath(new File(gpResourcesDir, "log4j2.xml").getPath()));
            return initLogger(logDir, log4j2Props);
        }
        catch (Throwable t) {
            System.err.println("Error initializing logger");
            t.printStackTrace();
            return null;
        }
    }

    /**
     * Initialize the GenePattern properties and start the database and other background threads.
     * 
     * Note that this initialization is copied in the org.genepattern.server.purger.impl02.Purger02
     * to allow it to be run separately from the GenePattern server.  Refactoring of these two
     * methods to eliminate duplicates is still TBD.
     */
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        this.webappDir=initWebappDir(servletConfig);
        ServerConfigurationFactory.setWebappDir(webappDir);
        this.gpHomeDir=initGpHomeDir(servletConfig);
        ServerConfigurationFactory.setGpHomeDir(gpHomeDir);

        this.gpWorkingDir=initGpWorkingDir(servletConfig, this.gpHomeDir);
        ServerConfigurationFactory.setGpWorkingDir(gpWorkingDir);
        
        // must init resourcesDir ...
        this.gpResourcesDir=initResourcesDir(gpHomeDir, gpWorkingDir);
        ServerConfigurationFactory.setResourcesDir(gpResourcesDir);
        // ...  before initializing the logger 
        this.log = initLogger(servletConfig, gpHomeDir, gpWorkingDir, gpResourcesDir);

        // must initialize logger before calling any methods which output to the log
        announceStartup();

        getLog().info("\tinitializing properties...");
        getLog().info("\tGENEPATTERN_HOME="+gpHomeDir);
        getLog().info("\tgpWorkingDir="+gpWorkingDir);
        getLog().info("\tresources="+gpResourcesDir);

        loadProperties(servletConfig); // assumes this.gpHomeDir, this.gpResourcesDir, and this.gpWorkingDir are initialized
        setServerURLs(servletConfig);

        ServerConfigurationFactory.reloadConfiguration();
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        GpContext gpContext=GpContext.getServerContext();

        if ("HSQL".equals(gpConfig.getDbVendor())) {
            // automatically start the DB
            try {
                String[] hsqlArgs=HsqlDbUtil.initHsqlArgs(gpConfig, gpContext); 
                getLog().info("\tstarting HSQL database...");
                HsqlDbUtil.startDatabase(hsqlArgs);
            }
            catch (Throwable t) {
                getLog().error("Unable to start HSQL Database!", t);
                return;
            }
        }
        
        getLog().info("\tchecking database connection...");
        try {
            HibernateUtil.beginTransaction();
        }
        catch (Throwable t) {
            getLog().debug("Error connecting to the database", t);
            Throwable cause = t.getCause();
            if (cause == null) {
                cause = t;
            }
            getLog().error("Error connecting to the database: " + cause);
            getLog().error("Error starting GenePatternServer, abandoning servlet init, throwing servlet exception.", t);
            throw new ServletException(t);
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }

        try {
            getLog().info("\tinitializing database schema ...");
            SchemaUpdater.updateSchema(gpConfig, HibernateUtil.instance());
        }
        catch (Throwable t) {
            getLog().error("Error initializing DB schema", t);
        }
        
        //load the configuration file
        try {
            getLog().info("\tinitializing ServerConfiguration...");
            String configFilepath = ServerConfigurationFactory.instance().getConfigFilepath();
        }
        catch (Throwable t) {
            getLog().error("error initializing ServerConfiguration", t);
        }
        
        //initialize the taskInfoCache
        try {
            getLog().info("\tinitializing TaskInfoCache...");
            TaskInfoCache.instance();
        }
        catch (Throwable t) {
            t.printStackTrace();
            getLog().error("error initializing taskInfo cache", t);
        }

        try {
            getLog().info("\tstarting job queue...");
            CommandManagerFactory.startJobQueue();
        }
        catch (Throwable t) {
            getLog().error("error starting job queue", t);
        }
        
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        HttpsURLConnection.setDefaultHostnameVerifier(new SessionHostnameVerifier());

        //clear system alert messages
        getLog().info("\tinitializing system messages...");
        try {
            SystemAlertFactory.getSystemAlert().deleteOnRestart();
        }
        catch (Exception e) {
            getLog().error("Error clearing system messages on restart: " + e.getLocalizedMessage(), e);
        }
        
        // import installed plugins (aka patches) from the root plugin directory into the GP database
        try {
            getLog().info("\tmigrating installed plugins ...");
            MigratePlugins migratePlugins=new MigratePlugins(HibernateUtil.instance(), gpConfig, gpContext);
            migratePlugins.migratePlugins();
        }
        catch (Throwable t) {
            getLog().error("Error migrating installed plugins: " + t.getLocalizedMessage(), t);
        }
        
        // copy task categories from CLOB
        try {
            MigrateTaskCategories mtc=new MigrateTaskCategories();
            if (!mtc.isComplete()) {
                getLog().info("\tcopying categories from CLOB into task_install_category table ...");
                mtc.copyCategoriesFromClobs();
            }
        }
        catch (Throwable t) {
            getLog().error("Error copying module categories: " + t.getLocalizedMessage(), t);
        }
        
        //start the JobPurger
        PurgerFactory.instance().start();

        announceReady();
    }
    
    /**
     * Set the servletContext; for Tomcat 5.5 this is set in the genepattern.properties file.
     * 
     * For newer versions (>= 2.5) of the Servlet Spec (not yet implemented) this can be derived from the 
     * ServletConfig.
     *     see: http://stackoverflow.com/questions/3120860/servletcontext-getcontextpath
     *     
     * @return the servlet context (e.g. "/gp").
     */
    protected String initGpServletContext(final ServletConfig servletConfig) {
        if (log.isDebugEnabled()) {
            log.debug("servlet version: "+
                    servletConfig.getServletContext().getMajorVersion() + "." +
                    servletConfig.getServletContext().getMinorVersion());
        }
        
        // set the servlet context; 
        String gpServletContext=System.getProperty("GP_Path", "/gp");
        if (!gpServletContext.startsWith("/")) {
            getLog().warn("prepending '/' to gpServletContext");
            gpServletContext = "/" + gpServletContext;
        }
        if (gpServletContext.endsWith("/")) {
            getLog().warn("removing trailing '/' from gpServletContext");
            gpServletContext = gpServletContext.substring(0, gpServletContext.length()-1);
        }
        ServerConfigurationFactory.setGpServletContext(gpServletContext);
        return gpServletContext;
    }
    
    /**
     * Set the GenePatternURL property dynamically using
     * the current canonical host name and servlet context path.
     * Dynamic lookup works for Tomcat but may not work on other containers... 
     * Initialize the gpServletContext variable.
     * 
     * @param config
     */
    private void setServerURLs(ServletConfig config) {
        final String gpServletContext=initGpServletContext(config);
        String genePatternURL = System.getProperty("GenePatternURL", "");
        if (genePatternURL == null || genePatternURL.trim().length() == 0) {
            try {
                getLog().error("Error, GenePatternURL not set, initializing from canonical host name ... ");
                InetAddress addr = InetAddress.getLocalHost();
                String host_address = addr.getCanonicalHostName();
                String portStr = System.getProperty("GENEPATTERN_PORT", "");
                portStr = portStr.trim();
                if (portStr.length()>0) {
                    portStr = ":"+portStr;
                }
                String genePatternServerURL = "http://" + host_address + portStr + gpServletContext + "/";
                System.setProperty("GenePatternURL", genePatternServerURL);
                getLog().error("setting GenePatternURL to " + genePatternServerURL);
            } 
            catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
    }

    private void announceStartup() {
        final String NL = GpConfig.getJavaProperty("line.separator");
        final String STARS = "****************************************************************************";
        StringBuffer startupMessage = new StringBuffer();
        startupMessage.append(NL + STARS + NL);
        startupMessage.append("Starting GenePatternServer ... ");
        getLog().info(startupMessage);
    }
 
    protected void announceReady() {
        AboutBean about = new AboutBean();
        String message = "GenePattern server version " + about.getFull() + 
            " build " + about.getBuildTag() + 
            " built " + about.getDate() + " is ready.";

        String defaultRootJobDir = "";
        GpContext serverContext = GpContext.getServerContext();

        try {
            File rootJobDir = ServerConfigurationFactory.instance().getRootJobDir(serverContext);
            defaultRootJobDir = rootJobDir.getAbsolutePath();
        }
        catch (Throwable t) {
            defaultRootJobDir = "Server configuration error: "+t.getLocalizedMessage();
        }

        GpConfig gpConfig=ServerConfigurationFactory.instance();
        String stars = "******************************************************************************************************************************************"
            .substring(0, message.length());
        StringBuffer startupMessage = new StringBuffer();
        final String NL = GpConfig.getJavaProperty("line.separator");
        startupMessage.append(""+NL);
        startupMessage.append(stars + NL);
        startupMessage.append(message + NL);
        startupMessage.append("\tGenePatternURL: " + gpConfig.getGpUrl() + NL );
        startupMessage.append("\t---------- Java system properties ----------" + NL );
        startupMessage.append("\tjava.version: " + GpConfig.getJavaProperty("java.version") + NL );
        startupMessage.append("\tuser.dir: " + GpConfig.getJavaProperty("user.dir") + NL);
        startupMessage.append("\tjava.io.tmpdir: " + GpConfig.getJavaProperty("java.io.tmpdir") + NL );
        startupMessage.append("\t---------- Web application properties ----------" + NL );
        startupMessage.append("\twebappDir: " + this.webappDir + NL );
        startupMessage.append("\t---------- GenePattern configuration ----------" + NL );
        startupMessage.append("\tGENEPATTERN_HOME: "+ gpConfig.getGpHomeDir() + NL);
        startupMessage.append("\tresourcesDir: "+ gpConfig.getResourcesDir() + NL);
        startupMessage.append("\tresources: "+ gpConfig.getGPProperty(serverContext, "resources") + NL);
        startupMessage.append("\t" + GpConfig.PROP_TASKLIB_DIR+": "+ gpConfig.getRootTasklibDir(serverContext) + NL);
        startupMessage.append("\t" + GpConfig.PROP_PLUGIN_DIR+": "+ gpConfig.getRootPluginDir(serverContext) + NL);
        // PROP_USER_ROOT_DIR
        startupMessage.append("\t" + GpConfig.PROP_USER_ROOT_DIR+": "+ gpConfig.getGPProperty(serverContext, GpConfig.PROP_USER_ROOT_DIR) + NL);
        startupMessage.append("\tjobs: " + defaultRootJobDir + NL);
        startupMessage.append("\t" + GpConfig.PROP_GP_TMPDIR+": "+ gpConfig.getTempDir(serverContext).getAbsolutePath() + NL);
        startupMessage.append("\t" + GpConfig.PROP_SOAP_ATT_DIR+": "+ gpConfig.getSoapAttDir(serverContext) + NL);
        startupMessage.append("\tconfig.file: " + gpConfig.getConfigFilepath() + NL);
        startupMessage.append("\tcatalina.home: " + GpConfig.getJavaProperty("catalina.home") + NL );
        startupMessage.append("\tcatalina.base: " + GpConfig.getJavaProperty("catalina.base") + NL );

        startupMessage.append(stars);

        getLog().info(startupMessage);
    }

    public void destroy() {
        getLog().info("StartupServlet: destroy called");
        
        //stop the job purger
        PurgerFactory.instance().stop();

        try {
            getLog().info("stopping job queue ...");
            CommandManagerFactory.stopJobQueue();
            getLog().info("done!");
        }
        catch (Throwable t) {
            getLog().error("Error stopping job queue: " + t.getLocalizedMessage(), t);
        }

        if ("HSQL".equals(ServerConfigurationFactory.instance().getDbVendor())) {
            try {
                getLog().info("stopping HSQLDB ...");
                HsqlDbUtil.shutdownDatabase();
                getLog().info("done!");
            }
            catch (Throwable t) {
                getLog().error("Error stopoping HSQLDB: " + t.getLocalizedMessage(), t);
            }
        }

        for (Enumeration<Thread> eThreads = vThreads.elements(); eThreads.hasMoreElements();) {
            Thread t = eThreads.nextElement();
            try {
                if (t.isAlive()) {
                    getLog().info("Interrupting " + t.getName());
                    t.interrupt();
                    t.setPriority(Thread.NORM_PRIORITY);
                    t.join();
                    getLog().info(t.getName() + " exited");
                }
            } 
            catch (Throwable e) {
                getLog().error(e);
            }
        }
        vThreads.removeAllElements();

        getLog().info("StartupServlet: destroy, calling dumpThreads...");
        dumpThreads();
        getLog().info("StartupServlet: destroy done");
    }

    /**
     * Set System properties to the union of all settings in:
     * servlet init parameters
     * resources/genepattern.properties
     * resources/custom.properties
     * 
     * @param config
     * @param workingDir, the root directory for resolving relative paths defined in the 'genepattern.properties' file
     * 
     * @throws ServletException
     */
    protected void loadProperties(final ServletConfig config) throws ServletException {
        File propFile = null;
        File customPropFile = null;
        FileInputStream fis = null;
        FileInputStream customFis = null;
        try {
            for (Enumeration<String> eConfigProps = config.getInitParameterNames(); eConfigProps.hasMoreElements();) {
                String propName = eConfigProps.nextElement();
                String propValue = config.getInitParameter(propName);
                if (propValue.startsWith(".")) {
                    propValue = new File(this.gpWorkingDir, propValue).getAbsolutePath();
                    propValue=GpConfig.normalizePath(propValue);
                }
                System.setProperty(propName, propValue);
            }
            Properties sysProps = System.getProperties();
            propFile = new File(this.gpResourcesDir, "genepattern.properties");
            customPropFile = new File(this.gpResourcesDir, "custom.properties");
            Properties props = new Properties();
            
            if (propFile.exists()) {
                fis = new FileInputStream(propFile);
                props.load(fis);
                getLog().info("\tloaded GP properties from " + propFile.getCanonicalPath());
            }
            else {
                getLog().error("\t"+propFile.getAbsolutePath()+" (No such file or directory)");
            }
            if (customPropFile.exists()) {
                customFis = new FileInputStream(customPropFile);
                props.load(customFis);
                getLog().info("\tloaded Custom GP properties from " + customPropFile.getCanonicalPath());
            }

            // copy all of the new properties to System properties
            for (Iterator<?> iter = props.keySet().iterator(); iter.hasNext();) {
                String key = (String) iter.next();
                String val = props.getProperty(key);
                if (val.startsWith(".")) {
                    //HACK: don't rewrite my value
                    if (! key.equals(JobResultsFilenameFilter.KEY)) {
                        val = new File(this.gpWorkingDir, val).getAbsolutePath();
                        val=GpConfig.normalizePath(val);
                    }
                }
                sysProps.setProperty(key, val);
            }

            if (fis != null) {
                fis.close();
                fis = null;
            }

            // copy all of the new properties to System properties
            for (Iterator<?> iter = props.keySet().iterator(); iter.hasNext();) {
                String key = (String) iter.next();
                String val = props.getProperty(key);
                if (key.equals("HSQL.args")) {
                    //special-case for default path to the HSQL database
                    //   replace 'file:../resources/GenePatternDB' with 'file:<workingDir>/resources/GenePatternDB'
                    String dbPath=new File(this.gpWorkingDir,"../resources/GenePatternDB").getAbsolutePath();
                    dbPath=GpConfig.normalizePath(dbPath);
                    val = val.replace("file:../resources/GenePatternDB", "file:"+dbPath);
                }
                else if (val.startsWith(".")) {
                    //HACK: don't rewrite my value
                    if (! key.equals(JobResultsFilenameFilter.KEY)) {
                        val = new File(this.gpWorkingDir, val).getAbsolutePath();
                        val=GpConfig.normalizePath(val);
                    }
                }
                sysProps.setProperty(key, val);
            }

            TreeMap tmProps = new TreeMap(sysProps);
            for (Iterator<?> iProps = tmProps.keySet().iterator(); iProps.hasNext();) {
                String propName = (String) iProps.next();
                String propValue = (String) tmProps.get(propName);
                getLog().debug(propName + "=" + propValue);
            }
        } 
        catch (IOException ioe) {
            ioe.printStackTrace();
            String path = null;
            try {
                path = propFile.getCanonicalPath();
            } 
            catch (IOException ioe2) {
            }
            throw new ServletException(path + " cannot be loaded.  " + ioe.getMessage());
        } 
        finally {
            try {
                if (fis != null)
                    fis.close();
            } 
            catch (IOException ioe) {
            }
        }
    }

    protected void dumpThreads() {
        getLog().info("StartupServlet.dumpThreads: what's still running...");
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        while (tg.getParent() != null) {
            tg = tg.getParent();
        }
        int MAX_THREADS = 100;
        Thread threads[] = new Thread[MAX_THREADS];
        int numThreads = tg.enumerate(threads, true);
        Thread t = null;
        for (int i = 0; i < numThreads; i++) {
            t = threads[i];
            if (t == null) {
                continue;
            } 
            if (!t.isAlive()) {
                continue;
            }
            getLog().info(t.getName() + " is running at " + t.getPriority() + " priority.  " + (t.isDaemon() ? "Is" : "Is not")
                    + " daemon.  " + (t.isInterrupted() ? "Is" : "Is not") + " interrupted.  ");

            //for debugging            
            //if (t.getName().startsWith("Thread-")) {
            //    getLog().info("what is this thread?");
            //    t.dumpStack();
            //    
            //    for(StackTraceElement e : t.getStackTrace()) {
            //        String m = ""+e.getClassName()+"."+e.getMethodName();
            //        String f = ""+e.getFileName()+":"+ e.getLineNumber();
            //        getLog().info(""+m+", "+f);
            //    }
            //
            //    getLog().info("calling Thread.stop()...");
            //    t.stop();
            //}
            
        }
        if (numThreads == MAX_THREADS) {
            getLog().info("Possibly more than " + MAX_THREADS + " are running.");
        }
    }
}

class LaunchThread extends Thread {
    Method mainMethod;
    String[] args;
    StartupServlet parent;

    private static Logger getLog()
    {
        return Logger.getLogger(LaunchThread.class);
    }

    public LaunchThread(String taskName, Method mainMethod, String[] args, StartupServlet parent) {
        super(taskName);
        this.mainMethod = mainMethod;
        this.args = args;
        this.parent = parent;
        this.setDaemon(true);
    }

    public void run() {
        try {
            getLog().debug("invoking " + mainMethod.getDeclaringClass().getName() + "." + mainMethod.getName());
            mainMethod.invoke(null, new Object[] { args });
            parent.log(getName() + " " + mainMethod.getDeclaringClass().getName() + "." + mainMethod.getName()
                    + " returned from execution");
        } 
        catch (IllegalAccessException iae) {
            getLog().error("Can't invoke main in " + getName(), iae);
        } 
        catch (IllegalArgumentException iae2) {
            getLog().error("Bad args for " + getName(), iae2);
        } 
        catch (InvocationTargetException ite) {
            getLog().error("InvocationTargetException for " + getName(), ite.getTargetException());
        } 
        catch (Exception e) {
            getLog().error("Exception for " + getName(), e);
        }
    }
}
