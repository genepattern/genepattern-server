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

public class TestClassNeighbors extends TestCallTasks {
    
    public TestClassNeighbors(String name){
	super(name);
    }

    
    public void testSimpleCall() throws Exception{
	AnalysisService svc = (AnalysisService)serviceMap.get("ClassNeighbors");
	String predResOutFileName = "predres";
	String dataResOutFileName = "datares";

	// set up parameters
	ParameterInfo params[] = new ParameterInfo[11];
	params[0] = getInputFileParam("data.filename","ALL_vs_AML_train_set_38_sorted.res");
	params[1] = getInputFileParam("class.filename","ALL_vs_AML_train_set_38_sorted.cls");
	params[2] = new ParameterInfo("pred.results.file", predResOutFileName, "");
	params[3] = new ParameterInfo("data.results.file", dataResOutFileName, "");
	params[4] = new ParameterInfo("num.neighbors", "50","");
	params[5] = new ParameterInfo("num.permutations", "100","");
	params[6] = new ParameterInfo("user.pval", "0.5","");
	params[7] = new ParameterInfo("min.threshold", "10","");
	params[8] = new ParameterInfo("max.threshold", "16000","");
	params[9] = new ParameterInfo("min.fold.diff", "5","");
	params[10] = new ParameterInfo("min.abs.diff", "50","");
		
	// call and wait for completion or error
	AnalysisJob job = submitJob(svc, params);
	JobInfo jobInfo = waitForErrorOrCompletion(job, 30, 2000);
	
	// look for successful completion (not an error)
	assertTrue("Status not Finished", "Finished".equalsIgnoreCase(jobInfo.getStatus()));
	assertNoStddErrFileGenerated(job);
	assertFileWithNameGenerated(job, predResOutFileName);
	assertFileWithNameGenerated(job, dataResOutFileName);
    }
        
 

}
