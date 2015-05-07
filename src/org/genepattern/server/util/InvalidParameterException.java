/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.server.util;

/**
 * InvalidParameterException.java
 * 
 * @author rajesh kuttan
 * @version
 */
public class InvalidParameterException extends java.lang.Exception {

	/**
	 * Creates new <code>InvalidParameterException</code> without detail
	 * message.
	 */
	public InvalidParameterException() {
	}

	/**
	 * Constructs an <code>InvalidParameterException</code> with the specified
	 * detail message.
	 * 
	 * @param msg
	 *            the detail message.
	 */
	public InvalidParameterException(String msg) {
		super(msg);
	}
}

