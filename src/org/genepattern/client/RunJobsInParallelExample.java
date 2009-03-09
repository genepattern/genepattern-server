/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2009) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.client;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.genepattern.webservice.JobResult;
import org.genepattern.webservice.Parameter;

/**
 * Simple example of how to run jobs in parallel
 * 
 * @author Joshua Gould
 */
public class RunJobsInParallelExample {
    /**
     * Runs the program
     * 
     * @param args
     *                command line arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
	List<Integer> submittedJobs = new LinkedList<Integer>();
	List<JobResult> completedJobs = new ArrayList<JobResult>();

	GPClient gpClient = new GPClient("http://localhost:8080", "GenePattern");
	int numJobs = 4;

	for (int i = 0; i < numJobs; i++) {
	    int jobNumber = gpClient.runAnalysisNoWait("ConvertLineEndings", new Parameter[] { new Parameter(
		    "input.filename", "foo.txt") });
	    submittedJobs.add(new Integer(jobNumber));
	}

	Thread.sleep(1000 * 60 * 10); // wait a while before we start asking
	// server jobs have finished
	while (submittedJobs.size() > 0) {
	    for (int i = 0; i < submittedJobs.size(); i++) {
		Integer jobNumber = submittedJobs.get(i);
		if (gpClient.isComplete(jobNumber.intValue())) {
		    submittedJobs.remove(i--);
		    JobResult jr = gpClient.createJobResult(jobNumber.intValue());
		    completedJobs.add(jr);
		}
	    }
	    Thread.sleep(2000);
	}
    }
}
