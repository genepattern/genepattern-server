package org.genepattern.gpge.ui.tasks.pipeline;

import java.util.List;

/**
 * Encapsulates the missing tasks on a server that a pipeline contains
 * 
 * @author jgould
 * 
 */
public class JobSubmissionsNotFoundException extends Exception {

	private List jobSubmissions;

	public JobSubmissionsNotFoundException(List jobSubmissions) {
		super();
		this.jobSubmissions = jobSubmissions;
	}

	public List getjobSubmissions() {
		return jobSubmissions;
	}

}
