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

public class TestNMF extends TestCallTasks {
    
    public TestNMF(String name){
        super(name);
    }

    
    public void testSimpleCall() throws Exception{
        AnalysisService svc = (AnalysisService)serviceMap.get("NMF");
        ParameterInfo params[] = new ParameterInfo[2];
        params[0] = getInputFileParam("input.filename","AML_vs_ALL.gct");
        
        params[1] = new ParameterInfo("output.file", "out","");
       // call and wait for completion or error
        AnalysisJob job = submitJob(svc, params);
        JobInfo jobInfo = waitForErrorOrCompletion(job);
        // look for successful completion (not an error)        
        assertTrue("Status not Finished", "Finished".equalsIgnoreCase(jobInfo.getStatus()));
	assertNoStddErrFileGenerated(job);  
		//assertFileWithNameGenerated(job, "ALL_vs_AML_train_set_38_sorted.cdt");
    }
    
   }
