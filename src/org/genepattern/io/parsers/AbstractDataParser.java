/*
 * AbstractDataParser.java
 *
 * Created on February 18, 2003, 10:41 PM
 */

package org.genepattern.io.parsers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.genepattern.util.AbstractReporter;
import org.genepattern.util.Reporter;

/**
 * Abstract implemetation of a DataParser
 * 
 * @author keith
 */
abstract public class AbstractDataParser implements DataParser {

	/**
	 * Creates a new instance of AbstractDataParser
	 * 
	 * @param extensions
	 *            the file extensions that this parser supports
	 */
	protected AbstractDataParser(final String[] extensions) {
		final String[] exts = (String[]) extensions.clone(); // just to be
															 // safe-should be
															 // small arrays
		// create array of extensions that are lower case and sorted
		final int limit = exts.length;
		for (int i = 0; i < limit; i++) {
			exts[i] = exts[i].toLowerCase();
		}
		java.util.Arrays.sort(exts);
		this.extensions = exts;
	}

	// helper methods

	/**
	 * extracts the file extension from the file name
	 * 
	 * @param file
	 *            the file
	 * @return String the file extension of the file
	 */
	public static final String getLowercaseFileExtension(final java.io.File file) {
		if (!file.isFile())
			return null; // should this be an error condition throws
						 // SomekindaException
		final String name = file.getName();
		return getLowercaseFileExtension(name);
	}

	/**
	 * extracts the file extension from the name of the file
	 * 
	 * @param name
	 *            the name of the file
	 * @return String, the file extension of the file name
	 */
	public static final String getLowercaseFileExtension(final String name) {
		final int index = name.lastIndexOf(FILE_EXT_SEPARATOR);
		if (index < 0)
			return EMPTY;
		return name.substring(index + 1).toLowerCase();

	}

	/**
	 * utility method that get the name of the file with out its' file extension
	 * 
	 * @param file
	 *            the file
	 * @return String, the file name without file extension
	 */
	public static final String getFileNameNoExt(final java.io.File file) {
		final String base = file.getName();
		return getFileNameNoExt(base);
	}

	/**
	 * utility method that get the name of the file with out its' file extension
	 * 
	 * @param base
	 *            the file name
	 * @return String the file name without file extension
	 */
	public static final String getFileNameNoExt(final String base) {
		final int index = base.lastIndexOf(FILE_EXT_SEPARATOR);
		if (index < 0)
			return base;
		return base.substring(0, index);
	}

	/** String rep of this object */
	public final String toString() {
		return extensions[0] + " parser";
	}

	// DataParser interface methods

	/**
	 * returns an array of file extensions that files of the format that this
	 * parser can handle. Of couse this is no garantee that the file really is
	 * of the proper format. Note that the file extensions are without the
	 * extension seperator wich is usually a dot, '.', a.k.a. a period
	 * character.
	 * 
	 * Note that the file extension strings are expected be in lower case for
	 * convenience and the array sorted!
	 * 
	 * @return String[], the file extension this parser supports
	 */
	public String[] getFileExtensions() {
		return (String[]) extensions.clone();
	}

	// fields
	/** this is the platform dependent new line string */
	public static final String NEW_LINE;

	/** the empty String */
	public static final String EMPTY = "";

	/**
	 * the file extension separator character FIXME this could be system
	 * dependent
	 */
	public static final char FILE_EXT_SEPARATOR;

	/** where error/warning messages go */
	protected static final Reporter REPORTER;

	/** the file extensions in lower case and sorted */
	private final String[] extensions;

	/** static initializer */
	static {
		//get the reporter
		REPORTER = AbstractReporter.getInstance();

		NEW_LINE = System.getProperty("line.separator");

		final String fes = org.genepattern.util.GPpropertiesManager
				.getProperty("gp.dataparser.file.ext.separator", ".");
		FILE_EXT_SEPARATOR = fes.trim().charAt(0);
	}

	// I N N E R C L A S S E S
	/** this class records all lines read to a StringBuffer */
	static class LineRecorder extends LineReader {
		/** Creates a new instance of LineReader */
		public LineRecorder(final InputStream input, final StringBuffer buf)
				throws FileNotFoundException {
			super(input);
			this.buffer = buf;
		}

		/** Creates a new instance of LineReader */
		public LineRecorder(final File file, final StringBuffer buf)
				throws FileNotFoundException {
			super(file);
			this.buffer = buf;
		}

		/**
		 * sets the current line This can be overridden by supclasses but must
		 * be called to set the line variable
		 * 
		 * protected String setCurrentLine(final String current) { // do
		 * somthing with it final String something = processString(current);
		 * return super.setCurrentLine(something); }
		 *  
		 */
		protected final String setCurrentLine(final String current) {
			buffer.append(current);
			buffer.append(NEW_LINE);
			return super.setCurrentLine(current);
		}

		// fields
		/** the buffer */
		private final StringBuffer buffer;

	}
}