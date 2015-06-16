/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.client;

import org.genepattern.webservice.Parameter;

/**
 * Simple example of how to run a job
 * 
 * @author Joshua Gould
 */
public class RunJob {
    /**
     * Runs the program
     * 
     * @param args
     *                command line arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
	String server = System.getProperty("server");
	String user = System.getProperty("user");
	String pw = System.getProperty("password");
	GPClient gpClient = new GPClient(server, user, pw);
	if (args.length < 1) {
	    System.err.println("No module specified.");
	    System.exit(1);
	}
	String module = args[0];
	Parameter[] parameters = new Parameter[args.length - 1];
	for (int i = 1; i < args.length; i++) {
	    String[] tokens = args[i].split("=");
	    if (tokens.length != 2) {
		System.err.println(tokens[0] + " value not found.");
		System.exit(1);
	    }
	    parameters[i - 1] = new Parameter(tokens[0].trim(), tokens[1].trim());
	}
	int jobNumber = gpClient.runAnalysisNoWait(module, parameters);
	System.exit(jobNumber);
    }
}
