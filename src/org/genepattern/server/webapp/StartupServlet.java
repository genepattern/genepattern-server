/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.xml.parsers.DocumentBuilderFactory;

import org.genepattern.server.AnalysisManager;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.indexer.IndexerDaemon;
import org.genepattern.server.process.CreateDatabase;
import org.genepattern.server.process.JSPPrecompiler;
import org.genepattern.server.process.JobPurger;
import org.genepattern.server.webservice.server.dao.AnalysisJobService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

// for SSL support
import javax.net.ssl.SSLSocketFactory;
import java.security.Security;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

/*
 * GenePattern startup servlet
 * 
 * This servlet performs periodic maintenance, based on definitions in the
 * <omnigene.conf>/genepattern.properties file @author Jim Lerner
 */

public class StartupServlet extends HttpServlet {

    public static String NAME = "GenePatternStartupServlet";

    SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static Vector vThreads = new Vector();

    public StartupServlet() {
        System.out.println("Creating StartupServlet");

    }

    public void init(ServletConfig config) throws ServletException {

        super.init(config);

        System.out.println("StartupServlet.init thread = " + Thread.currentThread().getName());
        System.out.flush();

        log("StartupServlet: user.dir=" + System.getProperty("user.dir"));
        ServletContext application = config.getServletContext();
        application.setAttribute("genepattern.properties", config.getInitParameter("genepattern.properties"));
        loadProperties(config);

        // @TODO -- temporary hack for testing.
        //try {
        //    String[] args = { "-database.0", "file:../resources/GenePatternDB", "-dbname.0", "xdb" };
        //    org.hsqldb.Server.main(args);
        //}
        //catch (Exception e) {
        //    // TODO Auto-generated catch block
        //    e.printStackTrace();
        //}

        launchTasks();
        // get a single instance of AnalysisManager so that it doesn't
        // get
        // re-instantiated with every invocation of GP
        application.setAttribute("AnalysisManager", AnalysisManager.getInstance());
        startDaemons(System.getProperties(), application);
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        //
        // Probably best to put this code in a function somewhere...
        //
        HostnameVerifier hv = new HostnameVerifier() {

            public boolean verify(String urlHostName, SSLSession session) {
                if (!urlHostName.equals(session.getPeerHost())) System.out.println("Warning: URL Host: " + urlHostName
                        + " vs. " + session.getPeerHost());
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(hv);
        announceReady(System.getProperties());

    }

    protected void startDaemons(Properties props, ServletContext application) throws ServletException {
        // startJobPurger(props);
        // startIndexerDaemon(props);
        // startJSPCompiler(props, application);
        Thread.currentThread().yield(); // allow a bit of runtime to the
        // independent threads
    }

    protected void startJobPurger(Properties props) {
        Thread tJobPurger = JobPurger.startJobPurger(props);
        addDaemonThread(tJobPurger);
    }

    public static void startJobPurger() {
        Thread tJobPurger = JobPurger.startJobPurger(System.getProperties());
        addDaemonThread(tJobPurger);
    }

    protected static void addDaemonThread(Thread t) {
        vThreads.add(t);
    }

    protected void startIndexerDaemon(Properties props) throws ServletException {
        String daemonName = "IndexerDaemon";
        if (!findThreadByName(daemonName)) {
            log("starting " + daemonName);
            try {
                Thread tIndexerDaemon = new Thread(IndexerDaemon.getDaemon(), daemonName);
                tIndexerDaemon.setPriority(Thread.NORM_PRIORITY - 2);
                tIndexerDaemon.setDaemon(true);
                tIndexerDaemon.start();
                addDaemonThread(tIndexerDaemon);
            }
            catch (IOException ioe) {
                throw new ServletException(ioe.getMessage() + " while starting " + daemonName);
            }
        }
    }

    protected void startJSPCompiler(Properties props, ServletContext application) {
        String webmaster = props.getProperty("webmaster");
        log("starting JSP precompiler, will notify " + webmaster + " of any errors");
        Thread jspPrecompiler = new Thread(new JSPPrecompiler(application.getRealPath("/"), props
                .getProperty("GenePatternURL"), webmaster), "JSPPrecompiler");
        jspPrecompiler.setPriority(Thread.MIN_PRIORITY + 1);
        jspPrecompiler.setDaemon(true);
        jspPrecompiler.start();
        addDaemonThread(jspPrecompiler);
    }

    protected void announceReady(Properties props) {
        String message = "GenePattern server version " + System.getProperty("GenePatternVersion") + " build "
                + System.getProperty("build.tag") + " built " + System.getProperty("date") + " is ready.";
        String stars = "******************************************************************************************************************************************"
                .substring(0, message.length());
        System.out.println("Listening on " + System.getProperty("GenePatternURL"));
        System.out.println("");
        System.out.println(stars);
        System.out.println(message);
        System.out.println(stars);
    }

    protected boolean findThreadByName(String name) {
        return false;
    }

    public String getServletInfo() {
        return NAME;
    }

    public void log(String message) {
        super.log(dateTimeFormat.format(new Date()) + ": " + message);
        // System.out.println(message);
    }

    public void log(String message, Throwable t) {
        super.log(dateTimeFormat.format(new Date()) + ": " + message, t);
        // System.out.println(message);
    }

    public void destroy() {
        log("StartupServlet: destroy called");
        for (Enumeration eThreads = vThreads.elements(); eThreads.hasMoreElements();) {
            Thread t = (Thread) eThreads.nextElement();
            try {
                if (t.isAlive()) {
                    log("Interrupting " + t.getName());
                    t.interrupt();
                    t.setPriority(Thread.NORM_PRIORITY);
                    t.join();
                    log(t.getName() + " exited");
                }
            }
            catch (Throwable e) {
            }
        }
        vThreads.removeAllElements();
        shutdownDatabase();
        GenePatternAnalysisTask.terminateAll("--> Shutting down server");
        log("StartupServlet: destroy done");
        dumpThreads();
    }

    protected void shutdownDatabase() {
        try {
            log("Checkpointing database");
            AnalysisJobService.getInstance().executeUpdate("CHECKPOINT");
            log("Checkpointed.");
            AnalysisJobService.getInstance().executeUpdate("SHUTDOWN");
        }
        catch (Throwable t) {
            t.printStackTrace();
            log("checkpoint database in StartupServlet.destroy", t);
        }
    }

    // read a CSV list of tasks from the launchTask property. For each entry,
    // lookup properties and
    // launch the task in a separate thread in this JVM
    protected void launchTasks() {
        Properties props = System.getProperties();
        StringTokenizer stTasks = new StringTokenizer(props.getProperty("launchTasks", ""), ",");

        String taskName = null;
        String className = null;
        String classPath = null;

        Thread threads[] = new Thread[Thread.currentThread().activeCount()];
        Thread.currentThread().enumerate(threads);

        nextTask: while (stTasks.hasMoreTokens()) {
            try {
                taskName = stTasks.nextToken();
                String expectedThreadName = props.getProperty(taskName + ".taskName", taskName);
                for (int t = 0; t < threads.length; t++) {
                    if (threads[t] == null) continue;
                    String threadName = threads[t].getName();
                    if (threadName == null) continue;
                    if (threadName.startsWith(expectedThreadName)) {
                        System.out.println(expectedThreadName + " is already running");
                        continue nextTask;
                    }
                }

                // Optionally use a new class loader.

                className = props.getProperty(taskName + ".class");
                // classPath = props.getProperty(taskName + ".classPath", "");
                // StringTokenizer classPathElements = new
                // StringTokenizer(classPath, ";");
                // URL[] classPathURLs = new
                // URL[classPathElements.countTokens()];
                // int i = 0;
                // log(taskName + " has classpath URLs: ");
                // while (classPathElements.hasMoreTokens()) {
                // classPathURLs[i++] = new
                // File(classPathElements.nextToken()).toURL();
                // log(classPathURLs[i-1].toString());
                // }
                String args = props.getProperty(taskName + ".args", "");
                StringTokenizer stArgs = new StringTokenizer(args, " ");
                String[] argsArray = new String[stArgs.countTokens()];
                int i = 0;
                // log(" and args ");
                while (stArgs.hasMoreTokens()) {
                    argsArray[i++] = stArgs.nextToken();
                    // log(argsArray[i-1]);
                }
                String userDir = props.getProperty(taskName + ".dir", System.getProperty("user.dir"));
                System.setProperty("user.dir", userDir);
                // log(" in directory " + System.getProperty("user.dir"));

                // URLClassLoader classLoader = new
                // URLClassLoader(classPathURLs, null);
                // log("Looking for " + className + ".main(String[] args)");
                Class theClass = Class.forName(className);
                if (theClass == null) {
                    throw new ClassNotFoundException("unable to find class " + className);
                    // + " using classpath " + classPathElements);
                }

                Method main = theClass.getMethod("main", new Class[] { String[].class });
                // log("found main, creating LaunchThread");
                LaunchThread thread = new LaunchThread(taskName, main, argsArray, this);
                log("starting " + taskName + " with " + args);
                // start the new thread and let it run until it is idle (ie.
                // inited)
                thread.setPriority(Thread.currentThread().getPriority() + 1);
                thread.setDaemon(true);
                thread.start();
                // just in case, give other threads a chance
                Thread.currentThread().yield();
                log(taskName + " thread running");
            }
            catch (SecurityException se) {
                log("unable to launch " + taskName, se);
            }
            // catch (MalformedURLException mue) {
            // log("Bad URL: ", mue);
            // }
            catch (ClassNotFoundException cnfe) {
                log("Can't find class " + className, cnfe);
            }
            catch (NoSuchMethodException nsme) {
                log("Can't find main in " + className, nsme);
            }
            catch (Exception e) {
                log("Exception while launching " + taskName, e);
            }
        } // end for each task
    }

    // set System properties to the union of all settings in:
    // servlet init parameters
    // resources/genepattern.properties
    // resources/build.properties
    protected void loadProperties(ServletConfig config) throws ServletException {
        File propFile = null;
        FileInputStream fis = null;
        try {
            for (Enumeration eConfigProps = config.getInitParameterNames(); eConfigProps.hasMoreElements();) {
                String propName = (String) eConfigProps.nextElement();
                String propValue = config.getInitParameter(propName);
                if (propValue.startsWith(".")) {
                    propValue = new File(propValue).getCanonicalPath();
                }
                System.setProperty(propName, propValue);
            }

            Properties sysProps = System.getProperties();
            String dir = sysProps.getProperty("genepattern.properties");
            propFile = new File(dir, "genepattern.properties");
            Properties props = new Properties();

            fis = new FileInputStream(propFile);
            props.load(fis);
            log("loaded GP properties from " + propFile.getCanonicalPath());
            // copy all of the new properties to System properties
            for (Iterator iter = props.keySet().iterator(); iter.hasNext();) {
                String key = (String) iter.next();
                String val = (String) props.getProperty(key);
                if (val.startsWith(".")) {
                    val = new File(val).getAbsolutePath();
                }
                sysProps.setProperty(key, val);
            }

            propFile = new File(dir, "build.properties");
            fis = new FileInputStream(propFile);
            props.load(fis);
            fis.close();
            fis = null;
            // copy all of the new properties to System properties
            for (Iterator iter = props.keySet().iterator(); iter.hasNext();) {
                String key = (String) iter.next();
                String val = (String) props.getProperty(key);
                if (val.startsWith(".")) {
                    val = new File(val).getCanonicalPath();
                }
                sysProps.setProperty(key, val);
            }

            System.setProperty("serverInfo", config.getServletContext().getServerInfo());
            // parse Tomcat's server.xml file to get definitive GenePatternURL
            // and GENEPATTERN_PORT settings
            setupWebserverProps(config, sysProps);

            TreeMap tmProps = new TreeMap(sysProps);
            for (Iterator iProps = tmProps.keySet().iterator(); iProps.hasNext();) {
                String propName = (String) iProps.next();
                String propValue = (String) tmProps.get(propName);
                log(propName + "=" + propValue);
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
                if (fis != null) fis.close();
            }
            catch (IOException ioe) {
            }
        }
    }

    // read Tomcat's server.xml file and set the GenePatternURL and
    // GENEPATTERN_PORT properties according to the actual configuration

    // Why are we doing this? This is weird. JTR.
    protected void setupWebserverProps(ServletConfig config, Properties props) {

        /*
         * if (config.getServletContext().getServerInfo().indexOf("Apache
         * Tomcat") != -1) { try { File tomcatConf = new
         * File(System.getProperty("tomcat"), "conf"); File fileServerXml = new
         * File(tomcatConf, "server.xml"); Document doc =
         * DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( new
         * InputSource(new FileReader(fileServerXml))); Element root =
         * doc.getDocumentElement();
         * 
         * HashMap hmProps = new HashMap(); processNode(root, hmProps); if
         * (hmProps.containsKey("port") && hmProps.containsKey("path")) { String
         * scheme = "http"; if (hmProps.containsKey("scheme")) scheme = (String)
         * hmProps.get("scheme");
         * 
         * props.setProperty("GenePatternURL", scheme + "://127.0.0.1:" +
         * hmProps.get("port") + hmProps.get("path") + "/"); } if
         * (hmProps.containsKey("port")) { props.setProperty("GENEPATTERN_PORT",
         * (String) hmProps.get("port")); } if (hmProps.containsKey("path")) {
         * props.setProperty("GP_Path", (String) hmProps.get("path")); } if
         * (hmProps.containsKey("docBase")) { props.setProperty("GP_docBase",
         * (String) hmProps.get("docBase")); } } catch (Exception e) {
         * System.err.println(e.getMessage() + " in
         * StartupServlet.parseTomcatServerXml"); } } else { // unknown server }
         */

    }

    protected void processNode(Node node, HashMap hmProps) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element c_elt = (Element) node;
            if (c_elt.getTagName().equals("Connector")) {
                String port = c_elt.hasAttribute("port") ? c_elt.getAttribute("port") : null;
                hmProps.put("port", port);
                String scheme = c_elt.hasAttribute("scheme") ? c_elt.getAttribute("scheme") : null;
                hmProps.put("scheme", scheme);

            }
            else if (c_elt.getTagName().equals("Context")) {
                String path = c_elt.hasAttribute("path") ? c_elt.getAttribute("path") : "";
                String docBase = c_elt.hasAttribute("docBase") ? c_elt.getAttribute("docBase") : "";
                if (path.indexOf("gp") != -1 || docBase.indexOf("gp") != -1) {
                    hmProps.put("path", path);
                    hmProps.put("docBase", docBase);
                }
            }
        }
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            processNode(childNodes.item(i), hmProps);
        }
    }

    protected void dumpThreads() {
        log("StartupServlet.dumpThreads: what's still running...");
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
            if (t == null) continue;
            if (!t.isAlive()) continue;
            log(t.getName() + " is running at " + t.getPriority() + " priority.  " + (t.isDaemon() ? "Is" : "Is not")
                    + " daemon.  " + (t.isInterrupted() ? "Is" : "Is not") + " interrupted.  ");
        }
        if (numThreads == MAX_THREADS) {
            log("Possibly more than " + MAX_THREADS + " are running.");
        }
    }
}

class LaunchThread extends Thread {

    Method mainMethod;

    String[] args;

    StartupServlet parent;

    public LaunchThread(String taskName, Method mainMethod, String[] args, StartupServlet parent) {
        super(taskName);
        this.mainMethod = mainMethod;
        this.args = args;
        this.parent = parent;
        this.setDaemon(true);
    }

    public void run() {
        try {
            parent.log("invoking " + mainMethod.getDeclaringClass().getName() + "." + mainMethod.getName());
            mainMethod.invoke(null, new Object[] { args });
            parent.log(getName() + " " + mainMethod.getDeclaringClass().getName() + "." + mainMethod.getName()
                    + " returned from execution");
        }
        catch (IllegalAccessException iae) {
            parent.log("Can't invoke main in " + getName(), iae);
        }
        catch (IllegalArgumentException iae2) {
            parent.log("Bad args for " + getName(), iae2);
        }
        catch (InvocationTargetException ite) {
            parent.log("InvocationTargetException for " + getName(), ite.getTargetException());
        }
        catch (Exception e) {
            parent.log("Exception for " + getName(), e);
        }
    }
}