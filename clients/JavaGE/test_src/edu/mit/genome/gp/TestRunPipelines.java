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

public class TestRunPipelines extends TestCallTasks {
    
    public TestRunPipelines(String name){
	super(name);
    }

    
    public void testCyclinD1nearestNeighborsGCM() throws Exception{
	execPipelineNoArgs("cyclinD1nearestneighboursGCM.pipeline");
    }
        

    public void testCyclinD1nearestNeighborsNCI60() throws Exception{
	execPipelineNoArgs("cyclinD1nearestneighboursNCI60.pipeline");
    }
     
    public void testCyclinD1nearestNeighborsProstate() throws Exception{
	execPipelineNoArgs("cyclinD1nearestneighboursprostate.pipeline");
	
    }

    public void testKSwithcyclinD1targets() throws Exception{
	execPipelineNoArgs("KSwithcyclinD1targets.pipeline");
    }
 
    /** runs a pipeline that has no input params 
     * @param name the name of the module/pipeline
     */
    private void execPipelineNoArgs(final String name) throws Exception{
	AnalysisService svc = (AnalysisService)serviceMap.get(name);
        logDebug("Testing "+svc.getTaskInfo().getName());
	// set up parameters
	ParameterInfo params[] = new ParameterInfo[0];
		
	// call and wait for completion or error
	AnalysisJob job = submitJob(svc, params);
	JobInfo jobInfo = waitForErrorOrCompletion(job, 40, 2000);
	logDebug("Job # " + jobInfo.getJobNumber());
	// look for successful completion (not an error)
	assertTrue("Status not Finished", "Finished".equalsIgnoreCase(jobInfo.getStatus()));
	assertNoStddErrFileGenerated(job);
    }
}
