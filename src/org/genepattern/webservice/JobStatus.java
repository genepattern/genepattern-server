package org.genepattern.webservice;

import java.util.Hashtable;

/**
 *  
 * Gets the parameters about Job status
 * </p>
 * 
 * @author Hui Gong
 * @version 1.0
 */

public class JobStatus {
	public static int JOB_NOT_STARTED = 1;

	public static int JOB_PROCESSING = 2;

	public static int JOB_FINISHED = 3;

	public static int JOB_ERROR = 4;

	public static int JOB_TIMEOUT = 5;

	public static String NOT_STARTED = "Not Started";

	public static String PROCESSING = "Processing";

	public static String FINISHED = "Finished";

	public static String ERROR = "Error";

	public static String TIMEOUT = "Time Out";

	public static Hashtable statusHash = new Hashtable();
	static {
		statusHash.put(NOT_STARTED, new Integer(JOB_NOT_STARTED));
		statusHash.put(PROCESSING, new Integer(JOB_PROCESSING));
		statusHash.put(FINISHED, new Integer(JOB_FINISHED));
		statusHash.put(ERROR, new Integer(JOB_ERROR));
		statusHash.put(TIMEOUT, new Integer(JOB_TIMEOUT));
	}

}