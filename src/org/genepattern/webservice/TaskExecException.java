/*******************************************************************************
 * Copyright (c) 2003-2021 Regents of the University of California and Broad Institute. All rights reserved.
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
