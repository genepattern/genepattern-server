/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.server;

import org.genepattern.webservice.OmnigeneException;

/**
 * This Exception is used when Taskid is not found in analysis service
 * 
 * @author Rajesh Kuttan
 * @version 1.0
 */

public class TaskIDNotFoundException extends OmnigeneException {

	public TaskIDNotFoundException(int taskId) {
	    super("Not a valid taskId: "+taskId);
	}

}

