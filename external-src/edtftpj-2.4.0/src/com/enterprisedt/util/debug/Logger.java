/**
 * 
 *  Copyright (C) 2000-2004 Enterprise Distributed Technologies Ltd
 *
 *  www.enterprisedt.com
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  Bug fixes, suggestions and comments should be sent to bruce@enterprisedt.com
 *
 *  Change Log:
 *
 *    $Log: Logger.java,v $
 *    Revision 1.29  2011-06-09 07:37:09  bruceb
 *    make sure globalLevel is not null
 *
 *    Revision 1.28  2011-03-28 03:30:18  hans
 *    Read logger prefix in static initializer.
 *
 *    Revision 1.27  2011-03-18 06:13:03  hans
 *    Catch exception in getLoggerPrefix().
 *
 *    Revision 1.26  2010-10-28 01:39:39  bruceb
 *    configurable prefix
 *
 *    Revision 1.25  2010-05-07 23:11:42  hans
 *    Fixed spelling mistake in comments.
 *
 *    Revision 1.24  2008-09-18 07:04:02  bruceb
 *    log4j fixes
 *
 *    Revision 1.23  2008-04-01 06:41:16  hans
 *    Added addFileAppender and addStandardOutputAppender
 *
 *    Revision 1.22  2008-01-09 03:58:54  bruceb
 *    include stack trace
 *
 *    Revision 1.21  2007-05-29 03:08:53  bruceb
 *    remove comment conflict
 *
 *    Revision 1.20  2007-05-29 03:08:04  bruceb
 *    add code for enabling logging of thread names
 *
 *    Revision 1.19  2007-05-15 04:31:59  hans
 *    Made sure each Appender only gets added once in addAppender.
 *
 *    Revision 1.18  2007/04/26 04:22:53  hans
 *    Added removeAppender
 *
 *    Revision 1.17  2007/03/27 10:23:08  bruceb
 *    clearAllElements()
 *
 *    Revision 1.16  2007/03/26 05:23:32  bruceb
 *    add clearAppenders
 *
 *    Revision 1.15  2007/02/14 00:56:13  hans
 *    Added getLevel
 *
 *    Revision 1.14  2006/11/14 12:13:24  bruceb
 *    fix bug whereby ALL level was causing exception in log4j
 *
 *    Revision 1.13  2006/10/27 16:30:04  bruceb
 *    fixed javadoc
 *
 *    Revision 1.12  2006/10/12 12:38:58  bruceb
 *    synchronized methods
 *
 *    Revision 1.11  2006/09/08 07:49:35  hans
 *    Fixed error where isEnabledFor didn't work if log4j is being used.
 *    Also added debug methods with MessageFormat-type parameters.
 *
 *    Revision 1.10  2006/05/22 01:53:03  hans
 *    Added method for dumping byte-arrays as hex
 *
 *    Revision 1.9  2006/03/16 21:49:07  hans
 *    Added support for logging of exception causes
 *
 *    Revision 1.8  2005/02/04 12:29:08  bruceb
 *    add exception message to output
 *
 *    Revision 1.7  2004/10/20 21:03:09  bruceb
 *    catch SecurityExceptions
 *
 *    Revision 1.6  2004/09/17 12:27:11  bruceb
 *    1.1 compat
 *
 *    Revision 1.5  2004/08/31 13:54:50  bruceb
 *    remove compile warnings
 *
 *    Revision 1.4  2004/08/16 21:08:08  bruceb
 *    made cvsids public
 *
 *    Revision 1.3  2004/06/25 11:52:26  bruceb
 *    fixed logging bug
 *
 *    Revision 1.2  2004/05/08 21:13:51  bruceb
 *    renamed property
 *
 *    Revision 1.1  2004/05/01 16:55:42  bruceb
 *    first cut
 *
 *
 */
package com.enterprisedt.util.debug;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import com.enterprisedt.BaseIOException;

/**
 *  Logger class that mimics log4j Logger class. If log4j integration
 *  is desired, the "edtftp.log4j" property should be set to "true" and
 *  log4j classes must be in the classpath
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.29 $
 */
public class Logger { 
    
    /**
     *  Revision control id
     */
    public static String cvsId = "@(#)$Id: Logger.java,v 1.29 2011-06-09 07:37:09 bruceb Exp $";
    
    /**
     * Level of all loggers
     */
    private static Level globalLevel;
    
    /**
     * Log thread names of all loggers
     */
    private static boolean logThreadNames = false;
    
    /**
     * Timestamp formatter
     */
    private SimpleDateFormat format = new SimpleDateFormat("d MMM yyyy HH:mm:ss.SSS");
    
    /**
     * Hash of all loggers that exist
     */
    private static Hashtable loggers = new Hashtable(10);
    
    /**
     * Vector of all appenders
     */
    private static Vector appenders = new Vector(2);
    
    /**
     * Shall we use log4j or not?
     */
    private boolean useLog4j = false;
    
    /**
     * Log this logger's thread name
     */
    private boolean logThreadName = false;
    
    /**
     * Timestamp
     */
    private Date ts = new Date();
    
    /**
     * Class name for this logger
     */
    private String clazz;
    
    /**
     *  Log4j logging methods
     */
    private Method[][] logMethods = null;

    /**
     * Log4j toPriority method
     */
    private Method toLevelMethod = null;
    
    /**
     * Log4j isEnabledFor method
     */
    private Method isEnabledForMethod = null;
    
    /**
     * Logger log4j object
     */
    private Object logger = null;
    
    /**
     * Arg arrays for use in invoke
     */
    private Object[] argsPlain = new Object[1];
    
    /**
     * Arg arrays for use in invoke
     */
    private Object[] argsThrowable = new Object[2];
    
    /**
     * Prefix for logger names.
     */
    private static String prefix;
    
    /**
     * Determine the logging level
     */
    static {
        String level = Level.OFF.toString();
        try {
            level = System.getProperty("edtftp.log.level", Level.OFF.toString());
        }
        catch (SecurityException ex) {
            System.out.println("Could not read property 'edtftp.log.level' due to security permissions");
        }

    	try {
	        prefix = System.getProperty("edtftp.log.prefix");
	        if (prefix==null)
	        	prefix = "";
    	} catch (Throwable t) {
            System.out.println("Could not read property 'edtftp.log.prefix' due to security permissions");
    		prefix = "";
    	}

        globalLevel = Level.getLevel(level);
        if (globalLevel == null)
            globalLevel = Level.OFF;
    }
    
    /**
     * Constructor
     * 
     * @param clazz     class this logger is for
     * @param uselog4j  true if using log4j
     */
    private Logger(String clazz, boolean uselog4j) {
        this.clazz = clazz;
        this.useLog4j = uselog4j;
        if (uselog4j)
            setupLog4j();
    }
    
    /**
     * Attempt to set up log4j logging. Of course, the classes
     * must be in the classpath
     */
    private synchronized void setupLog4j() {
        logMethods = new Method[Level.LEVEL_COUNT][2];
        try {
            Class log4jLogger = Class.forName("org.apache.log4j.Logger");
            Class log4jLevel = Class.forName("org.apache.log4j.Level");
            Class log4jPriority = Class.forName("org.apache.log4j.Priority");
            
            // get static logger method & use to get our logger
            Class[] args = { String.class };
            Method getLogger = log4jLogger.getMethod("getLogger", args);
            Object[] invokeArgs = {clazz};
            logger = getLogger.invoke(null, invokeArgs);
            
            // get the logger's methods and store them
            Class[] plainArgs = {Object.class};
            Class[] throwableArgs = {Object.class,Throwable.class};
            logMethods[Level.FATAL_INT][0] = log4jLogger.getMethod("fatal", plainArgs);
            logMethods[Level.FATAL_INT][1] = log4jLogger.getMethod("fatal", throwableArgs);
            logMethods[Level.ERROR_INT][0] = log4jLogger.getMethod("error", plainArgs);
            logMethods[Level.ERROR_INT][1] = log4jLogger.getMethod("error", throwableArgs);
            logMethods[Level.WARN_INT][0] = log4jLogger.getMethod("warn", plainArgs);
            logMethods[Level.WARN_INT][1] = log4jLogger.getMethod("warn", throwableArgs);
            logMethods[Level.INFO_INT][0] = log4jLogger.getMethod("info", plainArgs);
            logMethods[Level.INFO_INT][1] = log4jLogger.getMethod("info", throwableArgs);
            logMethods[Level.DEBUG_INT][0] = log4jLogger.getMethod("debug", plainArgs);
            logMethods[Level.DEBUG_INT][1] = log4jLogger.getMethod("debug", throwableArgs);
            
            // get the toLevel and isEnabledFor methods
            Class[] toLevelArgs = {String.class};
            toLevelMethod = log4jLevel.getMethod("toLevel", toLevelArgs);
            Class[] isEnabledForArgs = {log4jPriority};
            isEnabledForMethod = log4jLogger.getMethod("isEnabledFor", isEnabledForArgs);
        } 
        catch (Exception ex) {
            useLog4j = false;
            error("Failed to initialize log4j logging", ex);
        } 
    }
    
    /**
     * Returns the logging level for all loggers.
     * 
     * @return current logging level.
     */
    public static synchronized Level getLevel() {
    		return globalLevel;
    }

    /**
     * Set all loggers to this level
     * 
     * @param level  new level
     */
    public static synchronized void setLevel(Level level) {
        globalLevel = level;
    }
    
    /**
     * Get a logger for the supplied class
     * 
     * @param clazz    full class name
     * @return  logger for class
     */
    public static Logger getLogger(Class clazz) {
        return getLogger(clazz.getName());
    }
           
    /**
     * Get a logger for the supplied class
     * 
     * @param clazz    full class name
     * @return  logger for class
     */
    public static synchronized Logger getLogger(String clazz) {
        clazz = prefix + clazz;
        Logger logger = (Logger)loggers.get(clazz);
        if (logger == null) {
            boolean useLog4j = false;
            try {
                String log4j = System.getProperty("edtftp.log.log4j");
                if (log4j != null && log4j.equalsIgnoreCase("true")) {
                    useLog4j = true;
                }
            }
            catch (SecurityException ex) {
                System.out.println("Could not read property 'edtftp.log.log4j' due to security permissions");
            }
            logger = new Logger(clazz, useLog4j);
            loggers.put(clazz, logger);
        }
        return logger;
    }
    
    /**
     * Add an appender to our list
     * 
     * @param newAppender
     */
    public static synchronized void addAppender(Appender newAppender) {
    	if (!appenders.contains(newAppender))
    		appenders.addElement(newAppender);
    }

    /**
     * Add a file-appender to our list
     * 
     * @param fileName Path of file.
     * @throws IOException
     */
    public static synchronized void addFileAppender(String fileName) throws IOException {
    	addAppender(new FileAppender(fileName));
    }
    
    /**
     * Add a standard-output appender to our list.
     */
    public static synchronized void addStandardOutputAppender() {
    	addAppender(new StandardOutputAppender());
    }
    
    /**
     * Remove an appender to from list
     * 
     * @param appender
     */
    public static synchronized void removeAppender(Appender appender) {
    	appender.close();
        appenders.removeElement(appender);
    }
   
    /**
     * Clear all appenders
     */
    public static synchronized void clearAppenders() {
        appenders.removeAllElements();
    }
    
    /**
     * Close all appenders
     */
    public static synchronized void shutdown() {
        for (int i = 0; i < appenders.size(); i++) {
            Appender a = (Appender)appenders.elementAt(i);
            a.close();
        }        
    }
    
    /**
     * Set global flag for logging thread names as part of the logger names.
     * 
     * @param logThreadNames true if logging thread names, false otherwise
     */
    public static synchronized void logThreadNames(boolean logThreadNames) {
        Logger.logThreadNames = logThreadNames;
    }
    
    /**
     * Set flag for logging thread names as part of the logger names for this instance
     * of the logger.
     * 
     * @param logThreadName true if logging thread names, false otherwise
     */
    public synchronized void logThreadName(boolean logThreadName) {
        this.logThreadName = logThreadName;
    }
    
    /**
     * Log a message 
     * 
     * @param level     log level
     * @param message   message to log
     * @param t         throwable object
     */
    public synchronized void log(Level level, String message, Throwable t) {
    	if (isEnabledFor(level))
    	{
	        if (useLog4j)
	            log4jLog(level, message, t);
	        else
	            ourLog(level, message, t);
    	}
    }
    
    /**
     * Calls log4j's isEnabledFor method.
     * 
     * @param level logging level to check
     * @return true if logging is enabled
     */
    private boolean log4jIsEnabledFor(Level level)
    {
        if (level.equals(Level.ALL)) // log4j doesn't have an 'ALL' level
            level = Level.DEBUG;

    	try
    	{
    		// convert the level to a Log4j Level object
	    	Object[] toLevelArgs = new Object[] { level.toString() };
	    	Object l = toLevelMethod.invoke(null, toLevelArgs);
	
	    	// call isEnabled
	    	Object[] isEnabledArgs = new Object[] { l };
	    	Object isEnabled = isEnabledForMethod.invoke(logger, isEnabledArgs);
	    	
	    	return ((Boolean)isEnabled).booleanValue();
        } 
        catch (Exception ex) { // there's a few, we don't care what they are
            ourLog(Level.ERROR, "Failed to invoke log4j toLevel/isEnabledFor method", ex);
            useLog4j = false;
            return false;
        }
    }
    
    /**
     * Log a message to log4j
     * 
     * @param level     log level
     * @param message   message to log
     * @param t         throwable object
     */
    private void log4jLog(Level level, String message, Throwable t) {
        
        if (level.equals(Level.ALL)) // log4j doesn't have an 'ALL' level
            level = Level.DEBUG;
        
        // set up arguments
        Object[] args = null;
        int pos = -1;
        if (t == null) {
            args = argsPlain;
            pos = 0;
        }
        else {
            args = argsThrowable;
            args[1] = t;
            pos = 1;
        }
        args[0] = message;
        
        // retrieve the correct method
        Method method = logMethods[level.getLevel()][pos];
        
        // and invoke the method
        try {
            method.invoke(logger, args);
        } 
        catch (Exception ex) { // there's a few, we don't care what they are
            ourLog(Level.ERROR, "Failed to invoke log4j logging method", ex);
            ourLog(level, message, t);
            useLog4j = false;
        }
    }

    /**
     * Log a message to our logging system
     * 
     * @param level     log level
     * @param message   message to log
     * @param t         throwable object
     */
    private void ourLog(Level level, String message, Throwable t) {
        ts.setTime(System.currentTimeMillis());
        String stamp = format.format(ts);
        StringBuffer buf = new StringBuffer(level.toString());
        buf.append(" [");
        if (logThreadNames || logThreadName)
            buf.append(Thread.currentThread().getName()).append("_");
        buf.append(clazz).append("] ").append(stamp).
        append(" : ").append(message);
        if (t != null) {
            buf.append(" : ").append(t.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println();
            t.printStackTrace(pw);
            pw.println();
            buf.append(sw.toString());
        }
        if (appenders.size() == 0) { // by default to stdout
            System.out.println(buf.toString());
            while (t != null) {
                t.printStackTrace(System.out);
                if (t instanceof BaseIOException) {
                	t = ((BaseIOException)t).getInnerThrowable();
                	if (t!=null)
                		System.out.println("CAUSED BY:");
                }
                else
                	t = null;
            }
        }
        else {
            for (int i = 0; i < appenders.size(); i++) {
                Appender a = (Appender)appenders.elementAt(i);
                a.log(buf.toString());
                while (t != null) {
                    a.log(t);
                    if (t instanceof BaseIOException) {
                    	t = ((BaseIOException)t).getInnerThrowable();
                    	if (t!=null)
                    		a.log("CAUSED BY:");
                    }
                    else
                    	t = null;
                }
            }
        }
    }
        
    /**
     * Log an info level message
     * 
     * @param message   message to log
     */
    public void info(String message)  {
        log(Level.INFO, message, null); 
    }
    
    /**
     * Log an info level message
     * 
     * @param message   message to log
     * @param t         throwable object
     */
    public void info(String message, Throwable t)  {
        log(Level.INFO, message, t); 
    }

    /**
     * Log a warning level message
     * 
     * @param message   message to log
     */
    public void warn(String message)  {
        log(Level.WARN, message, null); 
    }
    
    /**
     * Log a warning level message
     * 
     * @param message   message to log
     * @param t         throwable object
     */
    public void warn(String message, Throwable t)  {
        log(Level.WARN, message, t); 
    }
    
    /**
     * Log an error level message
     * 
     * @param message   message to log
     */
    public void error(String message)  {
        log(Level.ERROR, message, null);   
    }
    
    /**
     * Log an error level message
     * 
     * @param message   message to log
     * @param t         throwable object
     */
    public void error(String message, Throwable t)  {
        log(Level.ERROR, message, t);   
    } 
    
    /**
     * Log a fatal level message
     * 
     * @param message   message to log
     */
    public void fatal(String message)  {
        log(Level.FATAL, message, null); 
    }

    /**
     * Log a fatal level message
     * 
     * @param message   message to log
      * @param t         throwable object
    */
    public void fatal(String message, Throwable t)  {
        log(Level.FATAL, message, t); 
    }
    
    /**
     * Log a debug level message
     * 
     * @param message   message to log
     */
    public void debug(String message)  {
        log(Level.DEBUG, message, null); 
    }
    
	private static String hex[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
		"a", "b", "c", "d", "e", "f" };
    
    /**
     * Log a debug level message
     * 
     * @param message   message to log
     */
    public void debug(String message, byte[] data)  {
        log(Level.DEBUG, message, null); 
		int i;

		StringBuffer hexStr = new StringBuffer();
		StringBuffer charStr = new StringBuffer();
		for (i = 0; i < data.length; i++) {
			byte b = data[i];
			if ((i > 0) && ((i % 12) == 0)) {
				log(Level.DEBUG, hexStr.toString() + "  " + charStr.toString(), null);
				hexStr = new StringBuffer();
				charStr = new StringBuffer();
			}

			hexStr.append(hex[(b >> 4) & 0x0f] + hex[b & 0x0f] + " ");
			charStr.append(b >= ' ' && b <= '~' ? (char)b : '?');
		}

		log(Level.DEBUG, hexStr.toString() + "  " + charStr.toString(), null);
    }
    
    /**
     * Logs by substituting in the argument at the location marked in the message
     * argument by {0}.
     * Additional MessageFormat formatting instructions may be included.  Note that
     * this method saves processing time by not building the complete string unless
     * it is necessary; this saves the need for encapsulating
     * many complete logging statements in an "if (log.isDebugEnabled())" block.
     * @param message Message containing "substitution marks"
     * @param arg argument to be substituted at the marked location.
     */
    public void debug(String message, Object arg)
    {
    	if (isDebugEnabled())
    		log(Level.DEBUG, MessageFormat.format(message, new Object[]{arg}), null);
    }
    
    /**
     * Logs by substituting in the arguments at the locations marked in the message
     * argument by {#} (where # is a number).
     * Additional MessageFormat formatting instructions may be included.Note that
     * this method saves processing time by not building the complete string unless
     * it is necessary; this saves the need for encapsulating
     * many complete logging statements in an "if (log.isDebugEnabled())" block.
     * @param message Message containing "substitution marks"
     * @param arg0 argument to be substituted at the marked location.
     * @param arg1 argument to be substituted at the marked location.
     */
    public void debug(String message, Object arg0, Object arg1)
    {
    	if (isDebugEnabled())
    		log(Level.DEBUG, MessageFormat.format(message, new Object[]{arg0, arg1}), null);
    }
    
    /**
     * Logs by substituting in the arguments at the locations marked in the message
     * argument by {#} (where # is a number).
     * Additional MessageFormat formatting instructions may be included.Note that
     * this method saves processing time by not building the complete string unless
     * it is necessary; this saves the need for encapsulating
     * many complete logging statements in an "if (log.isDebugEnabled())" block.
     * @param message Message containing "substitution marks"
     * @param arg0 argument to be substituted at the marked location.
     * @param arg1 argument to be substituted at the marked location.
     * @param arg2 argument to be substituted at the marked location.
     */
    public void debug(String message, Object arg0, Object arg1, Object arg2)
    {
    	if (isDebugEnabled())
    		log(Level.DEBUG, MessageFormat.format(message, new Object[]{arg0, arg1, arg2}), null);
    }
    
    /**
     * Logs by substituting in the arguments at the locations marked in the message
     * argument by {#} (where # is a number).
     * Additional MessageFormat formatting instructions may be included.Note that
     * this method saves processing time by not building the complete string unless
     * it is necessary; this saves the need for encapsulating
     * many complete logging statements in an "if (log.isDebugEnabled())" block.
     * @param message Message containing "substitution marks"
     * @param arg0 argument to be substituted at the marked location.
     * @param arg1 argument to be substituted at the marked location.
     * @param arg2 argument to be substituted at the marked location.
     * @param arg3 argument to be substituted at the marked location.
     */
    public void debug(String message, Object arg0, Object arg1, Object arg2, Object arg3)
    {
    	if (isDebugEnabled())
    		log(Level.DEBUG, MessageFormat.format(message, new Object[]{arg0, arg1, arg2, arg3}), null);
    }
    
    /**
     * Logs by substituting in the arguments at the locations marked in the message
     * argument by {#} (where # is a number).
     * Additional MessageFormat formatting instructions may be included.Note that
     * this method saves processing time by not building the complete string unless
     * it is necessary; this saves the need for encapsulating
     * many complete logging statements in an "if (log.isDebugEnabled())" block.
     * @param message Message containing "substitution marks"
     * @param arg0 argument to be substituted at the marked location.
     * @param arg1 argument to be substituted at the marked location.
     * @param arg2 argument to be substituted at the marked location.
     * @param arg3 argument to be substituted at the marked location.
     * @param arg4 argument to be substituted at the marked location.
     */
    public void debug(String message, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4)
    {
    	if (isDebugEnabled())
    		log(Level.DEBUG, MessageFormat.format(message, new Object[]{arg0, arg1, arg2, arg3, arg4}), null);
    }

    /**
     * Log a debug level message
     * 
     * @param message   message to log
     * @param t         throwable object
     */
    public void debug(String message, Throwable t)  {
        log(Level.DEBUG, message, t); 
    }
    
    /**
     * Is logging enabled for the supplied level?
     * 
     * @param level   level to test for
     * @return true   if enabled
     */
    public synchronized boolean isEnabledFor(Level level) {
    	if (useLog4j) {
    		return log4jIsEnabledFor(level);
        }
    	else 
    		return globalLevel.isGreaterOrEqual(level);
    }
    
    /**
     * Is logging enabled for the supplied level?
     * 
     * @return true if enabled
     */
    public boolean isDebugEnabled() {
        return isEnabledFor(Level.DEBUG);
    }
    
    /**
     * Is logging enabled for the supplied level?
     * 
     * @return true if enabled
     */
    public boolean isInfoEnabled()  {
        return isEnabledFor(Level.INFO);
    }
}
