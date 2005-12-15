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

