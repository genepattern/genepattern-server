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
