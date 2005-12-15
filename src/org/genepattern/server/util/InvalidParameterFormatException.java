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

