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

public class TestWeightedVoting extends TestCallTasks {
    
    public TestWeightedVoting(String name){
	super(name);
    }

    
    public void testSimpleCall() throws Exception{
	AnalysisService svc = (AnalysisService)serviceMap.get("WeightedVoting");
	String outfileName = "predresults";
	// set up parameters
	ParameterInfo params[] = new ParameterInfo[10];
	params[0] = getInputFileParam("train.filename","ALL_vs_AML_train_set_38_sorted.res");
	params[1] = getInputFileParam("train.class.filename","ALL_vs_AML_train_set_38_sorted.cls");
	params[2] = getInputFileParam("test.filename","Leuk_ALL_AML.test.res");
	params[3] = getInputFileParam("test.class.filename","Leuk_ALL_AML.test.cls");
	params[4] = new ParameterInfo("pred.results.file",outfileName ,"");
	params[5] = new ParameterInfo("thresh.min", "20","");
	params[6] = new ParameterInfo("thresh.max", "16000","");
	params[7] = new ParameterInfo("fold.diff", "5","");
	params[8] = new ParameterInfo("num.features", "10","");
	params[9] = new ParameterInfo("delta", "50","");
	
	
	// call and wait for completion or error
	AnalysisJob job = submitJob(svc, params);
	JobInfo jobInfo = waitForErrorOrCompletion(job);
	
	// look for successful completion (not an error)
	assertTrue("Status not Finished", "Finished".equalsIgnoreCase(jobInfo.getStatus()));
	assertNoStddErrFileGenerated(job);
	assertFileWithNameGenerated(job, outfileName);
    }
        


}
