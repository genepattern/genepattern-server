/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.server.util;

/**
 * InvalidParameterFormatException.java
 * 
 * @author rajesh kuttan
 * @version
 */
public class InvalidParameterFormatException extends java.lang.Exception {

	/**
	 * Creates new <code>InvalidParameterFormatException</code> without detail
	 * message.
	 */
	public InvalidParameterFormatException() {
	}

	/**
	 * Constructs an <code>InvalidParameterFormatException</code> with the
	 * specified detail message.
	 * 
	 * @param msg
	 *            the detail message.
	 */
	public InvalidParameterFormatException(String msg) {
		super(msg);
	}
}

