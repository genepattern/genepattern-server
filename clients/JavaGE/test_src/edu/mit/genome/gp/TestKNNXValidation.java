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

public class TestKNNXValidation extends TestCallTasks {
    
    public TestKNNXValidation(String name){
	super(name);
    }

    
    public void testCallNoWeighting() throws Exception{
	AnalysisService svc = (AnalysisService)serviceMap.get("KNNXValidation");
	String outfileName = "knnx";
	// set up parameters
	ParameterInfo params[] = new ParameterInfo[10];
	params[0] = getInputFileParam("data.filename","ALL_vs_AML_train_set_38_sorted.res");
	params[1] = getInputFileParam("class.filename","ALL_vs_AML_train_set_38_sorted.cls");
	params[2] = new ParameterInfo("pred.results.file",outfileName ,"");
	params[3] = new ParameterInfo("thresh.min", "20","");
	params[4] = new ParameterInfo("thresh.max", "16000","");
	params[5] = new ParameterInfo("fold.diff", "5","");
	params[6] = new ParameterInfo("absolute.diff", "50","");
	params[7] = new ParameterInfo("num.features", "10","");
	params[8] = new ParameterInfo("num.neighbors", "3","");
	params[9] = new ParameterInfo("weighting.type", "0","");
	
	// call and wait for completion or error
	AnalysisJob job = submitJob(svc, params);
	JobInfo jobInfo = waitForErrorOrCompletion(job);
	
	// look for successful completion (not an error)
	assertTrue("Status not Finished", "Finished".equalsIgnoreCase(jobInfo.getStatus()));
	assertNoStddErrFileGenerated(job);
	assertFileWithNameGenerated(job, outfileName);
    }

    public void testCallOneOverKWeighting() throws Exception{
	AnalysisService svc = (AnalysisService)serviceMap.get("KNNXValidation");
	String outfileName = "knnx";
	// set up parameters
	ParameterInfo params[] = new ParameterInfo[10];
	params[0] = getInputFileParam("data.filename","ALL_vs_AML_train_set_38_sorted.res");
	params[1] = getInputFileParam("class.filename","ALL_vs_AML_train_set_38_sorted.cls");
	params[2] = new ParameterInfo("pred.results.file",outfileName ,"");
	params[3] = new ParameterInfo("thresh.min", "20","");
	params[4] = new ParameterInfo("thresh.max", "16000","");
	params[5] = new ParameterInfo("fold.diff", "5","");
	params[6] = new ParameterInfo("absolute.diff", "50","");
	params[7] = new ParameterInfo("num.features", "10","");
	params[8] = new ParameterInfo("num.neighbors", "3","");
	params[9] = new ParameterInfo("weighting.type", "1","");
	
	// call and wait for completion or error
	AnalysisJob job = submitJob(svc, params);
	JobInfo jobInfo = waitForErrorOrCompletion(job);
	
	// look for successful completion (not an error)
	assertTrue("Status not Finished", "Finished".equalsIgnoreCase(jobInfo.getStatus()));
	assertNoStddErrFileGenerated(job);
	assertFileWithNameGenerated(job, outfileName);
    }  


}
