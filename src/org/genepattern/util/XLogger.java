/*
 * WHITEHEAD INSTITUTE
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2001 by the
 * Whitehead Institute for Biomedical Research.  All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever.  The Whitehead Institute can not be responsible for its
 * use, misuse, or functionality.
 */

package org.genepattern.util;

//import java.util.logging.ConsoleHandler;
//import java.util.logging.Handler;
//import java.util.logging.Level;
//import java.util.logging.Logger;


/**
 *
 * main reason for use - the junit swing thig doesnt thrown an error and its
 * output is now accesible.
 *
 * advs of custom logger:
 *
 * easly lallows us to swap b/w llogging impl - (java.util.logging is an impl so
 * its not by itself swappable)
 * easier to ensure that the appender/category is always inited
 * easier to impl a saving some logs in user env schme
 * hide whether log4j or java.util.logging is used
 *
 * basically support for any kind of apender (format) developer wants +
 * auto saving of some types of log messages to user dir
 *
 * needs
 * xml formatted file log for easy vieweing
 * rotating of log files every month?
 * only log to file warn and above
 * UI for user to inspect logs
 * should log files be purged?
 *
 * need -> some mechanism of visually indicating to user a warning or error - these
 * sometimes throw an exception but maybe not alway and hence the need.
 *
 * log file should be viewable in ext viewer such as ie - hence name file .xml
 * (so that if edu.mit.genome.gp app dois start it can still be browesed)
 *
 * IMP: client code can prefer using getLogger rthatr than getInstanceZ() as that returns
 * a XLogger() which makes it uneecc to do a Log4j import
 * (just the Xlogger import will do).
 *
 * from: org.apache.log4j.examples;
 *  A simple example showing category subclassing.

   <p>The example should make it clear that subclasses follow the
   hierarchy. You should also try running this example with a <a
   href="doc-files/mylog.bad">bad</a> and <a
   href="doc-files/mylog.good">good</a> configuration file samples.

   <p>See <b><a
   href="doc-files/MyCategory.java">source code</a></b> for more details.
 *
 *
 *
 * IMP: any methods call through the log4j log method will not be overriden - thats bad.
 *
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 *
 * todo Need switches for setting he Handler and the logging level
 * todo Log level highlighting would be nice
 *
 * veto change to extending Logger once log4j 1.2 is out
 *       -> Moved to jdk1.4 util.logging instead.
 *
 * done IMP wo perrmance hits - log4j docs say using the conf that i have
 *       is slow perf. In addition i do the jbuilder additional exception thing
 *       which makes it even more of a drag.
 *       <br>-> no real solution. Development benefits of logging with click'ngo justify usage.
 *       <br>-> Maybe comment out before building a deployable version.
 *       <br>-> solns: dont use warn(obj) use warn(onj, e) as mush as possible
 *       <br>-> dont override error(msg) and fatal(msg) here or better ditto usage guideline
 *       <br>-> dont override info here as thats by far the most used in the exceptionless mode.
 *
 */

public class XLogger /*extends Logger*/ {

    /**
     * Cconstants encapsulated from Level.
     * So that client code doesnt need to import java.util.logging.Level
     */
//    public static Level warn = Level.WARNING;
//
//    public static Level debug = new Debug();

    /** The singleton instance */
    private static XLogger kInstance;

    // block initializes a configurator on loading
    // so any thing that uses XLogging is guaranteed to have a Configurator setup
    // log4j settings never throw an exception
    // though they do print to stderr if they cant initialize properly
    static {

        if(kInstance == null) { //don't want to block here
            synchronized(XLogger.class) {
                if(kInstance == null) {
//                    kInstance = new XLogger("edu.mit.genome.edu.mit.genome.gp.XLogger", null); // no res bundle
//                    // Hmm also need system switches for the level and handler too?
//                    kInstance.setLevel(Level.ALL);
//                    Handler handler = new ConsoleHandler();
//                    handler.setFormatter(new TraceLogFormatter()); // init to whatever is specified in the sys property
//                    kInstance.addHandler(handler);
                }
            } // End synch
        }

    } // End static block


    /**
     * Class constructor.
     * Just calls the parent constuctor.
     */
    private XLogger(String name, String resbundle) {
//        super(name, resbundle);


    }

    // deprecate? The class things isnt being used
    // maybe keep around for other compatibily or later use
    public static XLogger getLogger(Class cs) {
        return kInstance;
    }

    /*
    // deprecate?
    public static XLogger getLogger(String name) {
        return kInstance;
    }
    */

    //public static XLogger getDefault() {
        //return kInstance;
    //}

    public void info(String msg, Throwable t) {
//        super.log(Level.INFO, msg, t);
    }

    public void warn(String msg, Throwable t) {
//        super.log(Level.WARNING, msg, t);
    }

    public void warn(String msg) {
//        super.log(Level.WARNING, msg);
    }

    public void debug(String msg) {
//        super.log(debug, msg);
    }

    public boolean isDebugEnabled() {
        return false;
//        return super.isLoggable(debug);
    }

    public boolean isInfoEnabled() {
        return false;
//        return super.isLoggable(Level.INFO);
    }

    public void error(String msg) {
//        super.severe(msg);
    }

    public void error(String msg, Throwable t) {
//        super.log(Level.SEVERE, msg, t);
    }

    public void error(Throwable t) {
//        super.log(Level.SEVERE, "Error", t);
    }

// there inst a Debug level in Level. Why?
//private static class Debug extends Level {
//    private Debug() {
//        super("DEBUG", 85552);
//    }
//}


} // End XLogger

