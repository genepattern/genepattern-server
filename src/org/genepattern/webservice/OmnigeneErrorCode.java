/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.webservice;

import java.util.Hashtable;

/**
 * OmnigeneErrorCodes Used in referencing error number with messages.
 * 
 * @author Rajesh Kuttan
 */
public class OmnigeneErrorCode {

	//Omnidas Errors start here

	//Omnibus Errors
	/**
	 * Server busy error in Omnibus
	 */
	public static final int E_OMNIBUS_BUSY = 20000;

	/**
	 * Fatal Error in Omnibus
	 */
	public static final int E_OMNIBUS_FATAL_ERROR = 20001;

	/**
	 *  
	 */
	public static final int E_OMNIBUS_UNRECOGNIZED_VERSION = 20002;

	//Omnitide Errors

	//Octopus Errors

	//Omnigraph Errors

	//Util Errors
	/**
	 * Property value not found
	 */
	public static final int E_PROPERTY_NOT_FOUND = 60000;

	/**
	 * App server not started
	 *  
	 */
	public static final int E_SERVER_NOT_STARTED = 60001;

	private static final Hashtable lookuptable = new Hashtable();

	static {
		lookuptable.put(Integer.toString(E_OMNIBUS_BUSY),
				"Server Busy. Pl try again.");
		lookuptable.put(Integer.toString(E_OMNIBUS_FATAL_ERROR),
				"Server Fatal Error");
		lookuptable.put(Integer.toString(E_OMNIBUS_UNRECOGNIZED_VERSION),
				"Unregnoized Version");
	}

	/**
	 * Returns a text message describing the errno code passed in.
	 * 
	 * @param errno
	 *            Error Number
	 * @return Error Message String
	 */
	public static String getMessage(int errno) {
		return (String) lookuptable.get(Integer.toString(errno));
	}
}
