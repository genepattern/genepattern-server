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

public class TestConsensusClustering extends TestCallTasks {
    
    public TestConsensusClustering(String name){
	super(name);
    }

    
    public void testCallHierarchical() throws Exception{
	AnalysisService svc = (AnalysisService)serviceMap.get("ConsensusClustering");
	
	// set up parameters
	ParameterInfo params[] = new ParameterInfo[11];
	params[0] = getInputFileParam("input.filename","ALL_vs_AML_train_set_38_sorted.res");
	params[1] = new ParameterInfo("kmax", "2", "");
	params[2] = new ParameterInfo("niter", "150", "");
	params[3] = new ParameterInfo("normalize.type", "1", "");
	params[4] = new ParameterInfo("norm.iter", "12", "");
	params[5] = new ParameterInfo("algo", "hierarchical", "");
	params[6] = new ParameterInfo("resample", "subsample", "");
	params[7] = new ParameterInfo("merge.type", "average", "");
	params[8] = new ParameterInfo("som.iter", "10", "");
	params[9] = new ParameterInfo("pink.size", "10", "");
	params[10] = new ParameterInfo("out.stub", "conclus", "");
			
	// call and wait for completion or error
	AnalysisJob job = submitJob(svc, params);
	JobInfo jobInfo = waitForErrorOrCompletion(job);
	
	// look for successful completion (not an error)
	assertTrue("Status not Finished", "Finished".equalsIgnoreCase(jobInfo.getStatus()));
	assertNoStddErrFileGenerated(job);
	assertFileWithNameGenerated(job, "conclus");
    }
        
    public void testCallSOMClustering() throws Exception{
	AnalysisService svc = (AnalysisService)serviceMap.get("ConsensusClustering");
	
	// set up parameters
	ParameterInfo params[] = new ParameterInfo[11];
	params[0] = getInputFileParam("input.filename","ALL_vs_AML_train_set_38_sorted.res");
	params[1] = new ParameterInfo("kmax", "2", "");
	params[2] = new ParameterInfo("niter", "150", "");
	params[3] = new ParameterInfo("normalize.type", "1", "");
	params[4] = new ParameterInfo("norm.iter", "12", "");
	params[5] = new ParameterInfo("algo", "SOM", "");
	params[6] = new ParameterInfo("resample", "subsample", "");
	params[7] = new ParameterInfo("merge.type", "average", "");
	params[8] = new ParameterInfo("som.iter", "10", "");
	params[9] = new ParameterInfo("pink.size", "10", "");
	params[10] = new ParameterInfo("out.stub", "conclus", "");
			
	// call and wait for completion or error
	AnalysisJob job = submitJob(svc, params);
	JobInfo jobInfo = waitForErrorOrCompletion(job);
	
	// look for successful completion (not an error)
	assertTrue("Status not Finished", "Finished".equalsIgnoreCase(jobInfo.getStatus()));
	assertNoStddErrFileGenerated(job);
	assertFileWithNameGenerated(job, "conclus");
    }
        


}
