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
     *            command line arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        List submittedJobs = new LinkedList();
        List completedJobs = new ArrayList();

        GPServer gpServer = new GPServer("http://localhost:8080", "jgould");

        for (int i = 0; i < 10; i++) {
            int jobNumber = gpServer
                    .runAnalysisNoWait("ComparativeMarkerSelection",
                            new Parameter[] { new Parameter("input.filename",
                                    "foo.txt") });
            submittedJobs.add(new Integer(jobNumber));
        }

        Thread.sleep(1000 * 60 * 10); // wait a while before we start asking
        // server if jobs have finished
        while (submittedJobs.size() > 0) {
            for (int i = 0; i < submittedJobs.size(); i++) {
                Integer jobNumber = (Integer) submittedJobs.get(i);
                if (gpServer.isComplete(jobNumber.intValue())) {
                    submittedJobs.remove(i--);
                    JobResult jr = gpServer.createJobResult(jobNumber
                            .intValue());
                    completedJobs.add(jr);
                }
            }
            Thread.sleep(2000);
        }
    }
}