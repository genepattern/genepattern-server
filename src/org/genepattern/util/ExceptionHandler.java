/*
 * ExceptionHandler.java
 *
 * Created on October 1, 2003, 1:23 AM
 */

package org.genepattern.util;

/**
 * This class is a handler for exceptions thrown by Listener interface methods.
 * It is used by the java.awt.EventDispatchThread when the System properties,
 * sun.awt.exception.handler, is set to this class.
 * System.setProperty("sun.awt.exception.handler",
 * "edu.mit.genome.util.ExceptionHandler")
 * 
 * From the command line: java
 * -Dsun.awt.exception.handler=edu.mit.genome.util.ExceptionHandler ...
 * 
 * @author kohm
 */
final public class ExceptionHandler {

	/** Creates a new instance of ExceptionHandler */
	public ExceptionHandler() {
		System.out.println("Creating another ExceptionHandler");
	}

	public void handle(final Throwable thw) {
		handleException(thw);
	}

	// static methods
	public static void handleException(final Throwable thw) {
		SafeRun.handleException(thw);
	}

	//fields

}