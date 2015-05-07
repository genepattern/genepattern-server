/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.webservice;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *  
 * Gets the parameters about Job status
 * 
 * @author Hui Gong
 * @version 1.0
 */

public class JobStatus {
	public static int JOB_NOT_STARTED = 1;

	public static int JOB_PROCESSING = 2;

	public static int JOB_FINISHED = 3;

	public static int JOB_ERROR = 4;

	public static String NOT_STARTED = "Pending";

	public static String PROCESSING = "Processing";

	public static String FINISHED = "Finished";

	public static String ERROR = "Error";

   /** an unmodifiable map that maps a string representation of the status to the numberic representation */
	public static final Map STATUS_MAP;
   
	static {
      Map statusHash = new HashMap();
		statusHash.put(NOT_STARTED, new Integer(JOB_NOT_STARTED));
		statusHash.put(PROCESSING, new Integer(JOB_PROCESSING));
		statusHash.put(FINISHED, new Integer(JOB_FINISHED));
		statusHash.put(ERROR, new Integer(JOB_ERROR));
      STATUS_MAP = Collections.unmodifiableMap(statusHash);
	}

}
