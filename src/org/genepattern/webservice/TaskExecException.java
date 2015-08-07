/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


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
