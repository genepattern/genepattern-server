/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
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

