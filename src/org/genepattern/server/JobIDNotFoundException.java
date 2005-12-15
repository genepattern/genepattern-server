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


package org.genepattern.server;

/**
 * This Exception is used when Jobid is not found in analysis service
 * 
 * @author Rajesh Kuttan
 * @version 1.0
 */
import org.genepattern.webservice.OmnigeneException;

public class JobIDNotFoundException extends OmnigeneException {

	/** Creates new JobIDNotFoundException */
	public JobIDNotFoundException() {
		super();
	}

	public JobIDNotFoundException(String strMessage) {
		super(strMessage);
	}

	public JobIDNotFoundException(int errno) {
		super(errno);
	}
}

