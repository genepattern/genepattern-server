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

public class TestGeneNeighbors extends TestCallTasks {
    
    public TestGeneNeighbors(String name){
	super(name);
    }

    
    public void testCallCosineDistanceMetric() throws Exception{
	AnalysisService svc = (AnalysisService)serviceMap.get("GeneNeighbors");
	String predResOutFileName = "predres";
	String dataResOutFileName = "datares";

	// set up parameters
	ParameterInfo params[] = new ParameterInfo[10];
	params[0] = getInputFileParam("data.filename","ALL_vs_AML_train_set_38_sorted.res");
	params[1] = new ParameterInfo("pred.results.file", predResOutFileName, "");
	params[2] = new ParameterInfo("data.results.file", dataResOutFileName, "");
	params[3] = new ParameterInfo("num.neighbors", "50","");
	params[4] = new ParameterInfo("gene.accession", "D17357_at","");
	params[5] = new ParameterInfo("distance.metric", "0","");
	params[6] = new ParameterInfo("min.threshold", "10","");
	params[7] = new ParameterInfo("max.threshold", "16000","");
	params[8] = new ParameterInfo("min.fold.diff", "5","");
	params[9] = new ParameterInfo("min.abs.diff", "50","");
		
	// call and wait for completion or error
	AnalysisJob job = submitJob(svc, params);
	JobInfo jobInfo = waitForErrorOrCompletion(job, 30, 2000);
	
	// look for successful completion (not an error)
	assertTrue("Status not Finished", "Finished".equalsIgnoreCase(jobInfo.getStatus()));
	assertNoStddErrFileGenerated(job);
	assertFileWithNameGenerated(job, predResOutFileName);
	assertFileWithNameGenerated(job, dataResOutFileName);
    }
        
  
    public void testCallEuclideanDistanceMetric() throws Exception{
	AnalysisService svc = (AnalysisService)serviceMap.get("GeneNeighbors");
	String predResOutFileName = "predres";
	String dataResOutFileName = "datares";

	// set up parameters
	ParameterInfo params[] = new ParameterInfo[10];
	params[0] = getInputFileParam("data.filename","ALL_vs_AML_train_set_38_sorted.res");
	params[1] = new ParameterInfo("pred.results.file", predResOutFileName, "");
	params[2] = new ParameterInfo("data.results.file", dataResOutFileName, "");
	params[3] = new ParameterInfo("num.neighbors", "51","");
	params[4] = new ParameterInfo("gene.accession", "D17357_at","");
	params[5] = new ParameterInfo("distance.metric", "1","");
	params[6] = new ParameterInfo("min.threshold", "11","");
	params[7] = new ParameterInfo("max.threshold", "15000","");
	params[8] = new ParameterInfo("min.fold.diff", "5","");
	params[9] = new ParameterInfo("min.abs.diff", "50","");
		
	// call and wait for completion or error
	AnalysisJob job = submitJob(svc, params);
	JobInfo jobInfo = waitForErrorOrCompletion(job, 30, 2000);
	
	// look for successful completion (not an error)
	assertTrue("Status not Finished", "Finished".equalsIgnoreCase(jobInfo.getStatus()));
	assertNoStddErrFileGenerated(job);
	assertFileWithNameGenerated(job, predResOutFileName);
	assertFileWithNameGenerated(job, dataResOutFileName);
    }


    public void testCallManhattanDistanceMetric() throws Exception{
	AnalysisService svc = (AnalysisService)serviceMap.get("GeneNeighbors");
	String predResOutFileName = "predres";
	String dataResOutFileName = "datares";

	// set up parameters
	ParameterInfo params[] = new ParameterInfo[10];
	params[0] = getInputFileParam("data.filename","ALL_vs_AML_train_set_38_sorted.res");
	params[1] = new ParameterInfo("pred.results.file", predResOutFileName, "");
	params[2] = new ParameterInfo("data.results.file", dataResOutFileName, "");
	params[3] = new ParameterInfo("num.neighbors", "50","");
	params[4] = new ParameterInfo("gene.accession", "D17357_at","");
	params[5] = new ParameterInfo("distance.metric", "2","");
	params[6] = new ParameterInfo("min.threshold", "10","");
	params[7] = new ParameterInfo("max.threshold", "16000","");
	params[8] = new ParameterInfo("min.fold.diff", "5","");
	params[9] = new ParameterInfo("min.abs.diff", "50","");
		
	// call and wait for completion or error
	AnalysisJob job = submitJob(svc, params);
	JobInfo jobInfo = waitForErrorOrCompletion(job, 30, 2000);
	
	// look for successful completion (not an error)
	assertTrue("Status not Finished", "Finished".equalsIgnoreCase(jobInfo.getStatus()));
	assertNoStddErrFileGenerated(job);
	assertFileWithNameGenerated(job, predResOutFileName);
	assertFileWithNameGenerated(job, dataResOutFileName);
    }
  
    public void testCallpearsonDistanceMetric() throws Exception{
	AnalysisService svc = (AnalysisService)serviceMap.get("GeneNeighbors");
	String predResOutFileName = "predres";
	String dataResOutFileName = "datares";

	// set up parameters
	ParameterInfo params[] = new ParameterInfo[10];
	params[0] = getInputFileParam("data.filename","ALL_vs_AML_train_set_38_sorted.res");
	params[1] = new ParameterInfo("pred.results.file", predResOutFileName, "");
	params[2] = new ParameterInfo("data.results.file", dataResOutFileName, "");
	params[3] = new ParameterInfo("num.neighbors", "47","");
	params[4] = new ParameterInfo("gene.accession", "D17357_at","");
	params[5] = new ParameterInfo("distance.metric", "3","");
	params[6] = new ParameterInfo("min.threshold", "12","");
	params[7] = new ParameterInfo("max.threshold", "14000","");
	params[8] = new ParameterInfo("min.fold.diff", "6","");
	params[9] = new ParameterInfo("min.abs.diff", "51","");
		
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
