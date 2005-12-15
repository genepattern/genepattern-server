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

public class TestHierarchicalClustering extends TestCallTasks {
    
    public TestHierarchicalClustering(String name){
        super(name);
    }

    
    public void testSimpleCall() throws Exception{
        AnalysisService svc = (AnalysisService)serviceMap.get("HierarchicalClustering");
        ParameterInfo params[] = new ParameterInfo[7];
        params[0] = getInputFileParam("input.filename","ALL_vs_AML_train_set_38_sorted.res");
        
        params[1] = new ParameterInfo("data.format", "0","");
        params[2] = new ParameterInfo("normalize.type", "3","");
        params[3] = new ParameterInfo("num.iter", "0","");
        params[4] = new ParameterInfo("cluster.by", "2","");
      	params[5] = new ParameterInfo("num.classes", "1","");
		params[6] = new ParameterInfo("merge.type", "average","");
        
        // call and wait for completion or error
        AnalysisJob job = submitJob(svc, params);
        JobInfo jobInfo = waitForErrorOrCompletion(job);
        
        // look for successful completion (not an error)        
        assertTrue("Status not Finished", "Finished".equalsIgnoreCase(jobInfo.getStatus()));
	assertNoStddErrFileGenerated(job);  
		assertFileWithNameGenerated(job, "ALL_vs_AML_train_set_38_sorted.cdt");
		assertFileWithNameGenerated(job, "ALL_vs_AML_train_set_38_sorted.atr");
    }
    
   }
