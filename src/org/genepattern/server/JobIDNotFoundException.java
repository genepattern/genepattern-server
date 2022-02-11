/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/


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

