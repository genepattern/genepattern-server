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


package org.genepattern.webservice;

/**
 * @author Joshua Gould
 */
public class TaskExecException extends Exception {
	public TaskExecException(String message) {
		super(message);
	}

	public TaskExecException(Throwable cause) {
		super(cause);

	}
}