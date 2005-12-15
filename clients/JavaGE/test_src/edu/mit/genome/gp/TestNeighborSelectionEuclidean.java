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


package edu.mit.genome.gp;

import junit.framework.TestCase;
import edu.mit.wi.omnigene.framework.analysis.*;
import edu.mit.wi.omnigene.util.*;
import edu.mit.wi.omniview.analysis.*;
import edu.mit.wi.omnigene.framework.analysis.webservice.client.*;
import java.io.*;
import java.net.*;
import java.util.*;
import edu.mit.broad.util.*;

public class TestNeighborSelectionEuclidean extends TestCallTasks {
    
    public TestNeighborSelectionEuclidean(String name){
	super(name);
    }

    
    public void testCallOrderFromBottom() throws Exception{
	AnalysisService svc = (AnalysisService)serviceMap.get("NeighborSelectionEuclidean.lamb");
	String outFileName = "sfrout";
	
	// set up parameters
	ParameterInfo params[] = new ParameterInfo[5];
	params[0] = new ParameterInfo("query.gene", "D17357_at", "");
	params[1] = new ParameterInfo("top.or.bottom", "0", "");
	params[2] = new ParameterInfo("number.neighbors", "100", "");
	params[3] = new ParameterInfo("output", outFileName, "");
	params[4] = getInputFileParam("input.filename","ALL_vs_AML_train_set_38_sorted.res");
	
	// call and wait for completion or error
	AnalysisJob job = submitJob(svc, params);
	JobInfo jobInfo = waitForErrorOrCompletion(job);
	
	// look for successful completion (not an error)
	assertTrue("Status not Finished", "Finished".equalsIgnoreCase(jobInfo.getStatus()));
	assertNoStddErrFileGenerated(job);
	assertFileWithNameGenerated(job, outFileName);
	
    }
        
    public void testCallOrderFromTop() throws Exception{
	AnalysisService svc = (AnalysisService)serviceMap.get("NeighborSelectionEuclidean.lamb");
	String outFileName = "sfrout";
	
	// set up parameters
	ParameterInfo params[] = new ParameterInfo[5];
	params[0] = new ParameterInfo("query.gene", "X93017_at", "");
	params[1] = new ParameterInfo("top.or.bottom", "1", "");
	params[2] = new ParameterInfo("number.neighbors", "130", "");
	params[3] = new ParameterInfo("output", outFileName, "");
	params[4] = getInputFileParam("input.filename","ALL_vs_AML_train_set_38_sorted.res");
	
	// call and wait for completion or error
	AnalysisJob job = submitJob(svc, params);
	JobInfo jobInfo = waitForErrorOrCompletion(job);
	
	// look for successful completion (not an error)
	assertTrue("Status not Finished", "Finished".equalsIgnoreCase(jobInfo.getStatus()));
	assertNoStddErrFileGenerated(job);
	assertFileWithNameGenerated(job, outFileName);
	
    }
        

}
