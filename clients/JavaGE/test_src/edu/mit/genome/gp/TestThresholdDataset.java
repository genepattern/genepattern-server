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

public class TestThresholdDataset extends TestCallTasks {
    
    public TestThresholdDataset(String name){
	super(name);
    }

    
    public void testSimpleCall() throws Exception{
	AnalysisService svc = (AnalysisService)serviceMap.get("ThresholdDataset");
	String outfileName = "testThreshold";
	// set up parameters
	ParameterInfo params[] = new ParameterInfo[4];
	params[0] = new ParameterInfo("min", "1","");
	params[1] = new ParameterInfo("max", "50","");
	params[2] = new ParameterInfo("output", outfileName,"");
	params[3] = getInputFileParam("input.filename","ALL_vs_AML_train_set_38_sorted.res");
	
	
	// call and wait for completion or error
	AnalysisJob job = submitJob(svc, params);
	JobInfo jobInfo = waitForErrorOrCompletion(job);
	
	// look for successful completion (not an error)
	assertTrue("Status not Finished", "Finished".equalsIgnoreCase(jobInfo.getStatus()));
	assertNoStddErrFileGenerated(job);
	assertFileWithNameGenerated(job, outfileName);
    }
        


}
