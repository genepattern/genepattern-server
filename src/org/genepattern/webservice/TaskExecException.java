package org.genepattern.webservice;

/**
 *@author    Joshua Gould
 */
public class TaskExecException extends Exception {
	public TaskExecException(String message) {
		super(message);
	}


	public TaskExecException(Throwable cause) {
		super(cause);

	}
}
