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

public class TestPCA extends TestCallTasks {
    
    public TestPCA(String name){
        super(name);
    }

    
    public void testCallClusterByRow() throws Exception{
        AnalysisService svc = (AnalysisService)serviceMap.get("PCA");
        ParameterInfo params[] = new ParameterInfo[3];
        params[0] = getInputFileParam("input_filename","ALL_vs_AML_train_set_38_sorted.res");
	params[1] = new ParameterInfo("cluster_by", "1","");
	params[2] = new ParameterInfo("output_file", "test","");

       
        // call and wait for completion or error
        AnalysisJob job = submitJob(svc, params);
        JobInfo jobInfo = waitForErrorOrCompletion(job);
        // look for successful completion (not an error)        
        assertTrue("Status not Finished", "Finished".equalsIgnoreCase(jobInfo.getStatus()));
	assertNoStddErrFileGenerated(job);  
	assertFileWithNameGenerated(job, "test_s.odf");
	assertFileWithNameGenerated(job, "test_t.odf");
	assertFileWithNameGenerated(job, "test_u.odf");
    }
    
   public void testCallClusterByCol() throws Exception{
        AnalysisService svc = (AnalysisService)serviceMap.get("PCA");
        ParameterInfo params[] = new ParameterInfo[3];
        params[0] = getInputFileParam("input_filename","ALL_vs_AML_train_set_38_sorted.res");
	params[1] = new ParameterInfo("cluster_by", "3","");
	params[2] = new ParameterInfo("output_file", "test","");

       
        // call and wait for completion or error
        AnalysisJob job = submitJob(svc, params);
        JobInfo jobInfo = waitForErrorOrCompletion(job);
        // look for successful completion (not an error)        
        assertTrue("Status not Finished", "Finished".equalsIgnoreCase(jobInfo.getStatus()));
	assertNoStddErrFileGenerated(job);  
	assertFileWithNameGenerated(job, "test_s.odf");
	assertFileWithNameGenerated(job, "test_t.odf");
	assertFileWithNameGenerated(job, "test_u.odf");
    }
    



   }
