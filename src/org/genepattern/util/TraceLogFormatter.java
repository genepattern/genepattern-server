package org.genepattern.util;

//import java.util.logging.Level;
//import java.util.logging.LogRecord;
//import java.util.logging.SimpleFormatter;

/**
 * 
 * A log formatter that prints out a click-and-browsable line to an outputstream
 * (typically a console)
 * 
 * Note
 *  - format only tested with the jbuilder stdout terminal - generating the
 * stack trace is more expensive than a simple log output. So use with care when
 * in production.
 * 
 * @author Aravind Subramanian
 * @version %I%, %G%
 */

public class TraceLogFormatter /* extends SimpleFormatter */{

	/** Placed as a system property */
	public static final String PROP_LOG_FORMATTER = "log.formatter";

	public static final String JBUILDER_FORMAT = "jbuilder";

	public static final String NETBEANS_FORMAT = "netbeans";

	/**
	 * Whatever the default java format is This is also the defaulkt formatter
	 * if no PROP_LOG_FORMATTER System property is specified
	 */
	public static final String JAVA_FORMAT = "java";

	private String fFormat;

	/**
	 * Class ocnstructor set to sysps prop
	 */
	public TraceLogFormatter() {
		this(System.getProperty(PROP_LOG_FORMATTER));
	}

	public TraceLogFormatter(String format) {
		super();
		fFormat = format;

		//if (fFormat == null) fFormat = JAVA_FORMAT;

		if (fFormat == null)
			fFormat = JBUILDER_FORMAT;
		System.out.println("Setting log formatter format to: " + fFormat);
	}

	/**
	 * Cistomize so that the log lines are clic-and-browseable. We only want to
	 * customize records with no associated exception. (the rest automaticlaly
	 * have the stack trace)
	 * 
	 * Need the format for jbuilder as:
	 * 
	 * <pre>
	 * 
	 *  whatever(std log output)
	 *           at some.pkg.Test.foo(Test.java:7)
	 *  
	 * </pre>
	 */

	//    public String format(LogRecord recd) {
	//        // quickly do the super
	//        if (recd.getThrown() != null) return super.format(recd);
	//
	//        else {
	//            if (fFormat.equals(JBUILDER_FORMAT)) return formatForJBuilder(recd);
	//            else if (fFormat.equals(NETBEANS_FORMAT)) throw new
	// NotImplementedException("format no impl yet for netbeans");
	//            else return super.format(recd);
	//        }
	//    }
	// unfortunately can get to line number w/o generating a dummy exception
	// so may not be cross-platform/work across JVM's
	//    public String formatForJBuilder2(LogRecord recd) {
	//        StringBuffer buf = new StringBuffer(recd.getLevel().toString()).append(":
	// ");
	//        buf.append(recd.getMessage()).append('\n');
	//
	//        Exception e = new Exception();
	//        //e.printStackTrace();
	//        StackTraceElement[] ste = e.getStackTrace();
	//
	//        StackTraceElement el = ste[ste.length - 3];
	//        buf.append("\tat ");
	//        buf.append(el.getClassName()).append('.').append(el.getMethodName());
	//        buf.append('(').append(el.getFileName()).append(':').append(el.getLineNumber()).append(')');
	//        buf.append('\n');
	//
	//        el = ste[ste.length - 2];
	//        buf.append("\tat ");
	//        buf.append(el.getClassName()).append('.').append(el.getMethodName());
	//        buf.append('(').append(el.getFileName()).append(':').append(el.getLineNumber()).append(')');
	//        buf.append('\n');
	//
	//        el = ste[ste.length - 1];
	//        buf.append("\tat ");
	//        buf.append(el.getClassName()).append('.').append(el.getMethodName());
	//        buf.append('(').append(el.getFileName()).append(':').append(el.getLineNumber()).append(')');
	//        buf.append('\n').append('\n');
	//
	//        return buf.toString();
	//    }
	/**
	 * java.lang.Exception at
	 * edu.mit.genome.edu.mit.genome.gp.util.TraceLogFormatter.formatForJBuilder(TraceLogFormatter.java:87)
	 * at
	 * edu.mit.genome.edu.mit.genome.gp.util.TraceLogFormatter.format(TraceLogFormatter.java:75)
	 * at java.util.logging.StreamHandler.publish(StreamHandler.java:181) at
	 * java.util.logging.ConsoleHandler.publish(ConsoleHandler.java:90) at
	 * java.util.logging.Logger.log(Logger.java:427) at
	 * java.util.logging.Logger.doLog(Logger.java:449) at
	 * java.util.logging.Logger.log(Logger.java:472) at
	 * edu.mit.genome.edu.mit.genome.gp.XLogger.warn(XLogger.java:150) at
	 * edu.mit.genome.org.genepattern.io.FileCatalog.initContents(FileCatalog.java:136)
	 * at edu.mit.genome.org.genepattern.io.FileCatalog.
	 * <init>(FileCatalog.java:62) at
	 * edu.mit.genome.edu.mit.genome.gp.TaskManager.
	 * <init>(TaskManager.java:118) at
	 * edu.mit.genome.edu.mit.genome.gp.TaskManager.getInstance(TaskManager.java:102)
	 * at
	 * edu.mit.genome.edu.mit.genome.gp.edu.mit.genome.gp.xtest.junit.TaskTest.testTask(TaskTest.java:45)
	 * at
	 * edu.mit.genome.edu.mit.genome.gp.edu.mit.genome.gp.xtest.junit.TaskTest.main(TaskTest.java:96)
	 * 
	 * WARNING: No PersistentObject and Node registered for item:
	 * edu.mit.genome.org.genepattern.io.FileCatalogItem@7c4c51 (store has
	 * datatype: class edu.mit.genome.edu.mit.genome.gp.concurrent.TaskInfo) ...
	 * coercing into UnknownNode at
	 * edu.mit.genome.edu.mit.genome.gp.TaskManager.getInstance(TaskManager.java:102)
	 * at
	 * edu.mit.genome.edu.mit.genome.gp.edu.mit.genome.gp.xtest.junit.TaskTest.testTask(TaskTest.java:45)
	 * at
	 * edu.mit.genome.edu.mit.genome.gp.edu.mit.genome.gp.xtest.junit.TaskTest.main(TaskTest.java:96)
	 * 
	 * 
	 * Need something more fancy as cant really say in whch index the trace
	 * element we want is. - it changes based on whther the class was run from
	 * the cmd line, through junit, through junit swing etc. Instead do a search
	 * as below fo rthe first XLogger call and then use the iummeadiatenext
	 * line. fragile!
	 */
	//      public String formatForJBuilder(LogRecord recd) {
	//        StringBuffer buf = new StringBuffer();
	//        //buf.append(recd.getMillis());
	//        buf.append('[');
	//        if (recd.getLevel() == Level.WARNING) buf.append("WARN").append("]\t");
	//        else buf.append(recd.getLevel().toString()).append("]\t");
	//        buf.append(recd.getMessage()).append('\n');
	//
	//        Exception e = new Exception();
	//        //e.printStackTrace();
	//        StackTraceElement[] ste = e.getStackTrace();
	//        int useindx = -1;
	//        for (int i=0; i < ste.length; i++) {
	//            StackTraceElement el = ste[i];
	//            if (el.getClassName().indexOf("XLogger") != -1) {
	//                useindx = i+1;
	//                break;
	//            }
	//        }
	//
	//        // try some more - sometimes direclty calls a logging api
	//        if (useindx == -1) {
	//            for (int i=0; i < ste.length; i++) {
	//                StackTraceElement el = ste[i];
	//                if (el.getClassName().indexOf("logging.Logger") != -1) {
	//                    useindx = i+1;
	//                    //break; dont! as there might be several and we want the last
	//                }
	//            }
	//        }
	//
	//        if ( (useindx > -1) && (useindx < ste.length) ) {
	//            StackTraceElement el = ste[useindx];
	//            buf.append("\tat ");
	//            buf.append(el.getClassName()).append('.').append(el.getMethodName());
	//            buf.append('(').append(el.getFileName()).append(':').append(el.getLineNumber()).append(')');
	//            buf.append('\n');
	//        }
	//        else {
	//             buf.append("No appropriate trace element findable\n");
	//             e.printStackTrace();
	//        }
	//
	//        return buf.toString();
	//    }

} // End TraceLogFormatter
