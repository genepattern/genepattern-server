/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
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
    
    protected void setGpResourcesDir(final File gpResourcesDir) {
        this.gpResourcesDir=gpResourcesDir;
    }
    
    protected File getGpResourcesDir() {
        return this.gpResourcesDir;
    }
    
    protected void setGpJobResultsDir(final File gpJobResultsDir) {
        this.gpJobResultsDir=gpJobResultsDir;
    }
    
    protected File getGpJobResultsDir() {
        return this.gpJobResultsDir;
    }
    
    /**
     * Initialize the webapp dir (e.g. ./Tomcat/webapps/gp) for the servlet.
     * @return
     */
    protected File initWebappDir(final ServletConfig servletConfig) {
        // implemented to work with Servlet spec <= 2.5, e.g. Tomcat-5.5
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
    protected File initGpWorkingDir(final ServletConfig servletConfig) {
        return initGpWorkingDir(System.getProperty("GENEPATTERN_WORKING_DIR"), servletConfig);
    }
    
    protected File initGpWorkingDir(final String gpWorkingDirProp, final ServletConfig servletConfig) {
        if (!Strings.isNullOrEmpty(gpWorkingDirProp)) {
            return new File(gpWorkingDirProp);
        }
        
        if (this.gpHomeDir != null) {
            return null;
        }

        String gpWorkingDir=GpConfig.normalizePath(servletConfig.getServletContext().getRealPath("../../"));
        return new File(gpWorkingDir);
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
    protected File initGpHomeDir(ServletConfig config) {
        String gpHome=System.getProperty("GENEPATTERN_HOME", System.getProperty("gp.home", null));
        return initGpHomeDir(gpHome, config);
    }

    protected File initGpHomeDir(final String gpHomeProp, final ServletConfig config) {
        String gpHome=gpHomeProp;
        
        if (Strings.isNullOrEmpty(gpHome)) {
            gpHome = config.getInitParameter("GENEPATTERN_HOME");
        }
        if (Strings.isNullOrEmpty(gpHome)) {
            gpHome = config.getInitParameter("gp.home");
        }
        
        if (Strings.isNullOrEmpty(gpHome)) {
            return null;
        }
        
        //normalize
        gpHome=GpConfig.normalizePath(gpHome);
        
        File gpHomeDir=new File(gpHome);
        if (gpHomeDir.isAbsolute()) {
            return gpHomeDir;
        }
        else { 
            // special-case: handle relative path
            System.err.println("GENEPATTERN_HOME='"+gpHome+"': Should not be a relative path");
            gpHomeDir=new File(config.getServletContext().getRealPath("../../../"), gpHome).getAbsoluteFile();
            System.err.println("Setting GENEPATTERN_HOME="+gpHomeDir);
            return new File(gpHome);
        } 
    }
    
    /**
     * Get the path to the 'resources' directory for the web application.
     * 
     * @param gpWorkingDir, the working director for the GenePattern Server.
     * @return
     */
    protected File initResourcesDir(final File gpWorkingDir) {
        File resourcesDir;
        if (this.gpHomeDir != null) {
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
     * Initialize the path to the Log4J logging directory.
     */
    protected void initLogDir() {
        File logDir=null;
        // By default, logDir is '<gpHomeDir>/logs'
        if (this.gpHomeDir != null) {
            logDir=new File(this.gpHomeDir, "logs");
        }
        // On older version of GP (which don't define a gpHomeDir) the logDir is '<gpWorkingDir>../logs'
        else if (this.gpWorkingDir != null) {
            logDir=new File(this.gpWorkingDir, "../logs");
        }
        else {
            System.err.println("gpHomeDir and gpWorkingDir are not defined, setting log dir relative to 'user.dir'");
            logDir=new File(GpConfig.getJavaProperty("user.dir"), "logs");
        }
        
        try {
            //File logDir=new File(this.gpWorkingDir, "../logs");
            logDir=new File(GpConfig.normalizePath(logDir.getPath()));
            if (!logDir.exists()) {
                boolean success=logDir.mkdirs();
                if (success) {
                    System.out.println("Created log directory: "+logDir);
                }
            }
            System.setProperty("gp.log", logDir.getAbsolutePath());

            File log4jProps=new File(GpConfig.normalizePath(new File(gpResourcesDir, "log4j.properties").getPath()));
            PropertyConfigurator.configure(log4jProps.getAbsolutePath());
            this.log=Logger.getLogger(StartupServlet.class);
            ServerConfigurationFactory.setLogDir(logDir);
        }
        catch (Throwable t) {
            System.err.println("Error initializing logger");
            t.printStackTrace();
        }
    }
    
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        this.webappDir=initWebappDir(servletConfig);
        ServerConfigurationFactory.setWebappDir(webappDir);
        this.gpHomeDir=initGpHomeDir(servletConfig);
        ServerConfigurationFactory.setGpHomeDir(gpHomeDir);

        this.gpWorkingDir=initGpWorkingDir(servletConfig);
        ServerConfigurationFactory.setGpWorkingDir(gpWorkingDir);
        
        // must init resourcesDir ...
        this.gpResourcesDir=initResourcesDir(gpWorkingDir);
        ServerConfigurationFactory.setResourcesDir(gpResourcesDir);
        // ...  before initializing logDir 
        initLogDir();

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
        startupMessage.append("\tJava Version: " + GpConfig.getJavaProperty("java.version") + NL );
        startupMessage.append("\twebappDir: " + this.webappDir + NL );
        startupMessage.append("\tuser.dir: " + GpConfig.getJavaProperty("user.dir") + NL);
        startupMessage.append("\tGENEPATTERN_HOME: "+ gpConfig.getGpHomeDir() + NL);
        startupMessage.append("\tresourcesDir: "+ gpConfig.getResourcesDir() + NL);
        startupMessage.append("\tresources: "+ gpConfig.getGPProperty(serverContext, "resources") + NL);
        startupMessage.append("\t" + GpConfig.PROP_TASKLIB_DIR+": "+ gpConfig.getRootTasklibDir(serverContext) + NL);
        startupMessage.append("\t" + GpConfig.PROP_PLUGIN_DIR+": "+ gpConfig.getRootPluginDir(serverContext) + NL);
        startupMessage.append("\tjobs: " + defaultRootJobDir + NL);
        startupMessage.append("\tjava.io.tmpdir: " + GpConfig.getJavaProperty("java.io.tmpdir") + NL );
        startupMessage.append("\t" + GpConfig.PROP_GP_TMPDIR+": "+ gpConfig.getTempDir(serverContext).getAbsolutePath() + NL);
        startupMessage.append("\t" + GpConfig.PROP_SOAP_ATT_DIR+": "+ gpConfig.getSoapAttDir(serverContext) + NL);
        startupMessage.append("\tconfig.file: " + gpConfig.getConfigFilepath() + NL);
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

            //System.setProperty("serverInfo", config.getServletContext().getServerInfo());
	    
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
