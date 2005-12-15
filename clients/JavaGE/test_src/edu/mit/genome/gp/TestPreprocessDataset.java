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

public class TestPreprocessDataset extends TestCallTasks {
    
    public TestPreprocessDataset(String name){
        super(name);
    }

    
    public void testSimpleCall() throws Exception{
        AnalysisService svc = (AnalysisService)serviceMap.get("PreprocessDataset");
        ParameterInfo params[] = new ParameterInfo[1];
        params[0] = getInputFileParam("input.filename","ALL_vs_AML_train_set_38_sorted.res");

  /*      params[1] = new ParameterInfo("input.file.format","1", "");
        params[2] = new ParameterInfo("output.file.format","1", "");
        params[3] = new ParameterInfo("filter.flag","0", "");
        params[4] = new ParameterInfo("preprocessing.flag","0", "");
        params[5] = new ParameterInfo("minchange","1", "");
        params[6] = new ParameterInfo("mindelta","0", "");
        params[7] = new ParameterInfo("threshold","20", "");
        params[8] = new ParameterInfo("ceiling","16000", "");
        params[9] = new ParameterInfo("max.sigma.binning","1", "");
        params[10] = new ParameterInfo("prob.thres","1", "");
        params[11] = new ParameterInfo("num.excl","0", "");
        params[12] = new ParameterInfo("output.file","output", "");
      
*/
        // call and wait for completion or error
        AnalysisJob job = submitJob(svc, params);
        JobInfo jobInfo = waitForErrorOrCompletion(job);
        // look for successful completion (not an error)        
        assertTrue("Status not Finished", "Finished".equalsIgnoreCase(jobInfo.getStatus()));
		assertNoStddErrFileGenerated(job);  
    }
    
   }
