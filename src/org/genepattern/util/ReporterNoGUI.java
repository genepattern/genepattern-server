/*
 * ReporterNoGUI.java
 *
 * Created on August 21, 2002, 10:43 PM
 */

package org.genepattern.util;

/**
 * 
 * @author kohm
 */
public final class ReporterNoGUI extends AbstractReporter {

	/** Creates a new instance of ReporterNoGUI */
	protected ReporterNoGUI() {
	}

	/**
	 * shows the error message either via dialog if operating as gui or to file
	 * or standard out if no gui.
	 */
	public void showError(final String message) {
		if (VERBOSE)
			System.err.println(message);
	}

	/**
	 * shows the error message either via dialog if operating as gui or to file
	 * or standard out if no gui.
	 *  
	 */
	public void showError(final String message, final Throwable th) {
		showError(null, message, th);
	}

	/**
	 * shows the error message either via dialog if operating as gui or to file
	 * or standard out if no gui.
	 *  
	 */
	public void showError(final String title, final String message,
			final Throwable th) {
		final StringBuffer buf = new StringBuffer();
		;
		if (title != null) {
			buf.append(title);
			buf.append('\n');
		}
		if (message != null) {
			buf.append(message);
			buf.append('\n');
		}
		if (th != null) {
			buf.append(th);
			showError(buf.toString());
			th.printStackTrace();
		} else
			showError(buf.toString());

	}

	/**
	 * shows the warning message optionally via dialog if operating as gui or to
	 * file or standard out if no gui.
	 */
	public void showWarning(final String message) {
		if (VERBOSE)
			System.out.println(message);
	}

	/**
	 * shows the warning message optionally via dialog if operating as gui or to
	 * file or standard out if no gui.
	 */
	public void showWarning(final String message, final Exception w) {
		showWarning(null, message, w);
	}

	/**
	 * shows the warning message optionally via dialog if operating as gui or to
	 * file or standard out if no gui.
	 */
	public void showWarning(final String title, final String message,
			final Exception w) {
		final StringBuffer buf = new StringBuffer();
		;
		if (title != null) {
			buf.append(title);
			buf.append('\n');
		}
		if (message != null) {
			buf.append(message);
			buf.append('\n');
		}
		if (w != null) {
			buf.append(w);
			showWarning(buf.toString());
			w.printStackTrace();
		} else
			showWarning(buf.toString());
	}

	/**
	 * logs the message
	 */
	public void logWarning(final String message) {
		if (VERBOSE)
			System.out.println(message);
	}

	/**
	 * logs the message
	 */
	public void logWarning(final String message, final Exception w) {
		showWarning(message, w);
	}

}