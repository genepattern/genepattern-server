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


/*
 * BuildProperties.java
 *
 * Created on May 20, 2003, 7:43 PM
 */

package org.genepattern.gpge.util;

import java.io.InputStream;
import java.util.Properties;


//import java.util.Date;

/**
 * Convenience class for getting the build information.
 * 
 * @author kohm
 */
public class BuildProperties {

	/** private to restrict creating a new instance of BuildProperties */
	private BuildProperties() {
	}

	/**
	 * makes available the value for some build information not captured in the
	 * public fields
	 */
	public static final String getValue(final String key) {
		return PROPERTIES.getProperty(key);
	}

	public static final long getLong(final String key) {
		String value = null;
		try {
			value = getValue(key);
			return Long.parseLong(value);
		} catch (NumberFormatException ex) {
			ex.printStackTrace();
			return -1;
		}
	}

	//    public static final Date getDate(final String key) {
	//        String value;
	//        try {
	//            value = getValue(key);
	//            return Date.(value);
	//        } catch (NumberFormatException ex) {
	//            AbstractReporter.getInstance().logWarning(
	//                "While trying to parse the build.properties key "+key
	//                +" returned value id not a number '"+value+"'", ex);
	//            return null;
	//        }
	//    }
	/** test the props */
	public static void main(final String[] args) {
		show();
	}

	public static void show() {
		System.out.println("BUILD=" + BUILD);
		System.out.println("BUILD_COUNT=" + BUILD_COUNT);
		System.out.println("BUILD_DATE=" + BUILD_DATE);
		System.out.println("BUILD_TAG=" + BUILD_TAG);

		System.out.println("FULL_VERSION=" + FULL_VERSION);
		System.out.println("MAJOR_VERSION=" + MAJOR_VERSION);
		System.out.println("MINOR_VERSION=" + MINOR_VERSION);
		System.out.println("PROGRAM_NAME=" + PROGRAM_NAME);
		System.out.println("RELEASE=" + RELEASE);
		System.out.println("REVISION=" + REVISION);
	}

	// fields
	/** the build properties object */
	private static final Properties PROPERTIES;

	/** The build number */
	public static final long BUILD;

	/** The number of builds - this may be unused and just zero */
	public static final int BUILD_COUNT;

	/** The Date of the build */
	public static final String BUILD_DATE;

	/** the CVS tag of the build */
	public static final String BUILD_TAG;

	/** The name of the Program */
	public static final String PROGRAM_NAME;

	/** The release type: alpha, beta, final etc. */
	public static final String RELEASE;

	/** The major version */
	public static final String MAJOR_VERSION;

	/** The minor version */
	public static final String MINOR_VERSION;

	/** The revision */
	public static final String REVISION;

	/** The full version as a String */
	public static final String FULL_VERSION;

	/** static initializer */
	static {
		long build = 0;
		int count = 0;
		String name = "GenePattern", full = "0.0.0", release = "?", date = "The Past";
		String buildTag = "", major = "", minor = "", revision = "";

		PROPERTIES = new Properties();
		try {
			System.out.println("Looking for properties: build.properties");
			final InputStream in = ClassLoader
					.getSystemResourceAsStream("build.properties");
			PROPERTIES.load(in);

			// discover the properties
			build = getLong("build");
			count = (int) getLong("count");
			date = getValue("date");
			name = getValue("program.name");
			release = getValue("release");
			buildTag = getValue("build.tag");
			major = getValue("version.major");
			minor = getValue("version.minor");
			revision = getValue("version.revision");
			final String rev = (revision.length() > 0) ? "." + revision : "";
			full = major + "." + minor + rev + release;

		} catch (java.io.IOException ex) {
			final String msg = "Internal Error: Could not load the"
					+ " build.properties file";
			System.err.println(msg);
			ex.printStackTrace();
		} finally {
			BUILD = build;
			BUILD_COUNT = count;
			BUILD_DATE = date;
			BUILD_TAG = buildTag;
			FULL_VERSION = full;
			MAJOR_VERSION = major;
			MINOR_VERSION = minor;
			PROGRAM_NAME = name;
			RELEASE = release;
			REVISION = revision;
		}
	}
}