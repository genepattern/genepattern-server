/*
 * DataHandlerOmniView.java
 *
 * Created on June 30, 2003, 3:04 PM
 */

package org.genepattern.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;

import javax.beans.ExceptionListener;
import javax.beans.XMLDecoder;

/**
 * Class for deserializing java Objects saved in a XML format
 * 
 * @author kohm
 */
public class DeSerializer implements ExceptionListener {

	/** Creates a new instance of DataHandlerOmniView */
	private DeSerializer() {
		out = System.err;
	}

	// method from ExceptionListener
	public final void exceptionThrown(final Exception exception) {
		exception.printStackTrace(out);
		out.println("***    ***\n\n");
		out.flush();
	}

	/**
	 * Load data from a xml file and restore the Object that it represents
	 * 
	 * @param file
	 *            XML file
	 * @return Object an instance of a Java Object
	 * @throws FileNotFoundException
	 *             if the file does not exist, is a directory rather than a
	 *             regular file, or for some other reason cannot be opened for
	 *             reading
	 */
	public static final Object loadData(final File file)
			throws FileNotFoundException {
		final BufferedInputStream in = new BufferedInputStream(
				new FileInputStream(file));

		return decode(in, LISTENER, LISTENER);
	}

	/**
	 * Load data from a xml formatted input stream and restore the Object that
	 * it represents
	 * 
	 * @param in
	 *            the input stream
	 * @param owner
	 *            the owner of the input stream
	 * @param exceptionListener
	 *            the listener for exceptions that may occure while attempting
	 *            to deserialize the XML data stream
	 * @return Object an instance of a Java Object
	 */
	public static final Object decode(final InputStream in, final Object owner,
			final ExceptionListener exceptionListener) {
		final XMLDecoder d = new XMLDecoder(in, owner, exceptionListener);
		final Object result = d.readObject();
		d.close();
		return result;
	}

	/**
	 * Load data from a xml formatted input stream and restore the Object that
	 * it represents the owner of the input stream and the ExceptionListener
	 * becomes the DeSerializer
	 * 
	 * @param in
	 *            the input stream
	 * @return Object an instance of a Java Object
	 */
	public static final Object decode(final InputStream in) {
		return decode(in, LISTENER, LISTENER);
	}

	//fields
	/** the current instance of DeSerializer */
	protected static final DeSerializer LISTENER;

	/** where the error text goes */
	protected final PrintStream out;

	/** static initializer */
	static {
		LISTENER = new DeSerializer();
	}
}