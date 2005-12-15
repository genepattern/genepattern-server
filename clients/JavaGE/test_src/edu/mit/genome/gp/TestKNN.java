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

public class TestKNN extends TestCallTasks {
    
    public TestKNN(String name){
	super(name);
    }

    
    public void testNoWeighting() throws Exception{
	AnalysisService svc = (AnalysisService)serviceMap.get("KNN");
	String outfileName = "predresults";
	// set up parameters
	ParameterInfo params[] = new ParameterInfo[12];
	params[0] = getInputFileParam("train.filename","ALL_vs_AML_train_set_38_sorted.res");
	params[1] = getInputFileParam("train.class.filename","ALL_vs_AML_train_set_38_sorted.cls");
	params[2] = getInputFileParam("test.filename","Leuk_ALL_AML.test.res");
	params[3] = getInputFileParam("test.class.filename","Leuk_ALL_AML.test.cls");
	params[4] = new ParameterInfo("pred.results.file",outfileName ,"");
	params[5] = new ParameterInfo("thresh.min", "20","");
	params[6] = new ParameterInfo("thresh.max", "16000","");
	params[7] = new ParameterInfo("fold.diff", "5","");
	params[8] = new ParameterInfo("num.features", "10","");
	params[9] = new ParameterInfo("num.neighbors", "3","");
	params[10] = new ParameterInfo("weighting.type", "0","");
	params[11] = new ParameterInfo("absolute.diff", "50","");
	
	
	// call and wait for completion or error
	AnalysisJob job = submitJob(svc, params);
	JobInfo jobInfo = waitForErrorOrCompletion(job);
	
	// look for successful completion (not an error)
	assertTrue("Status not Finished", "Finished".equalsIgnoreCase(jobInfo.getStatus()));
	assertNoStddErrFileGenerated(job);
	assertFileWithNameGenerated(job, outfileName);
    }
        


}
