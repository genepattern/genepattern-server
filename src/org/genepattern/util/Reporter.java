/*
 * Reporter.java
 *
 * Created on August 21, 2002, 10:39 PM
 */

package org.genepattern.util;

/**
 * 
 * @author kohm
 */
public interface Reporter {
	/**
	 * shows the error message either via dialog if operating as gui or to file
	 * or standard out if no gui.
	 */
	public void showError(String message);

	/**
	 * shows the error message either via dialog if operating as gui or to file
	 * or standard out if no gui.
	 */
	public void showError(String message, Throwable th);

	/**
	 * shows the error message either via dialog if operating as gui or to file
	 * or standard out if no gui.
	 */
	public void showError(String title, String message, Throwable th);

	/**
	 * shows the warning message optionally via dialog if operating as gui or to
	 * file or standard out if no gui.
	 */
	public void showWarning(String message);

	/**
	 * shows the warning message optionally via dialog if operating as gui or to
	 * file or standard out if no gui.
	 */
	//public void showWarning(String message, Warning w);
	public void showWarning(String message, Exception w);

	/**
	 * shows the warning message optionally via dialog if operating as gui or to
	 * file or standard out if no gui.
	 */
	//public void showWarning(String message, Warning w);
	public void showWarning(String title, String message, Exception w);

	/**
	 * logs the message
	 */
	public void logWarning(String message);

	/**
	 * logs the message
	 */
	public void logWarning(String message, Exception w);
}