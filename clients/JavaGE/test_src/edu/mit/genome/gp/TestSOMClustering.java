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

public class TestSOMClustering extends TestCallTasks {
    
    public TestSOMClustering(String name){
	super(name);
    }

    
    public void testCallVectorBubble() throws Exception{
	AnalysisService svc = (AnalysisService)serviceMap.get("SOMClustering");
	
	// set up parameters
	ParameterInfo params[] = new ParameterInfo[12];
	params[0] = getInputFileParam("dataset.filename","ALL_vs_AML_train_set_38_sorted.res");
	params[1] = new ParameterInfo("cluster.range", "2-3", "");
	params[2] = new ParameterInfo("iterations", "50000", "");
	params[3] = new ParameterInfo("seed.range", "42", "");
	params[4] = new ParameterInfo("som.rows", "0", "");
	params[5] = new ParameterInfo("som.cols", "0", "");
	params[6] = new ParameterInfo("initialization", "Random_Vectors", "");
	params[7] = new ParameterInfo("neighborhood", "Bubble", "");
	params[8] = new ParameterInfo("alpha.initial", "0.1", "");
	params[9] = new ParameterInfo("alpha.final", "0.005", "");
	params[10] = new ParameterInfo("sigma.initial", "5.0", "");
	params[11] = new ParameterInfo("sigma.final", "0.5", "");
	
	// call and wait for completion or error
	AnalysisJob job = submitJob(svc, params);
	JobInfo jobInfo = waitForErrorOrCompletion(job);
	
	// look for successful completion (not an error)
	assertTrue("Status not Finished", "Finished".equalsIgnoreCase(jobInfo.getStatus()));
	assertNoStddErrFileGenerated(job);
	assertFileWithNameGenerated(job, "som");
    }


     public void testCallVectorGaussian() throws Exception{
	AnalysisService svc = (AnalysisService)serviceMap.get("SOMClustering");
	
	// set up parameters
	ParameterInfo params[] = new ParameterInfo[12];
	params[0] = getInputFileParam("dataset.filename","ALL_vs_AML_train_set_38_sorted.res");
	params[1] = new ParameterInfo("cluster.range", "2-3", "");
	params[2] = new ParameterInfo("iterations", "48000", "");
	params[3] = new ParameterInfo("seed.range", "49", "");
	params[4] = new ParameterInfo("som.rows", "0", "");
	params[5] = new ParameterInfo("som.cols", "0", "");
	params[6] = new ParameterInfo("initialization", "Random_Vectors", "");
	params[7] = new ParameterInfo("neighborhood", "Gaussian", "");
	params[8] = new ParameterInfo("alpha.initial", "0.1", "");
	params[9] = new ParameterInfo("alpha.final", "0.005", "");
	params[10] = new ParameterInfo("sigma.initial", "5.0", "");
	params[11] = new ParameterInfo("sigma.final", "0.5", "");
	
	// call and wait for completion or error
	AnalysisJob job = submitJob(svc, params);
	JobInfo jobInfo = waitForErrorOrCompletion(job);
	
	// look for successful completion (not an error)
	assertTrue("Status not Finished", "Finished".equalsIgnoreCase(jobInfo.getStatus()));
	assertNoStddErrFileGenerated(job);
	assertFileWithNameGenerated(job, "som");
    }

    public void testCallDatapointsGaussian() throws Exception{
	AnalysisService svc = (AnalysisService)serviceMap.get("SOMClustering");
	
	// set up parameters
	ParameterInfo params[] = new ParameterInfo[12];
	params[0] = getInputFileParam("dataset.filename","ALL_vs_AML_train_set_38_sorted.res");
	params[1] = new ParameterInfo("cluster.range", "2-4", "");
	params[2] = new ParameterInfo("iterations", "48000", "");
	params[3] = new ParameterInfo("seed.range", "49", "");
	params[4] = new ParameterInfo("som.rows", "0", "");
	params[5] = new ParameterInfo("som.cols", "0", "");
	params[6] = new ParameterInfo("initialization", "Random_Datapoints", "");
	params[7] = new ParameterInfo("neighborhood", "Gaussian", "");
	params[8] = new ParameterInfo("alpha.initial", "0.1", "");
	params[9] = new ParameterInfo("alpha.final", "0.005", "");
	params[10] = new ParameterInfo("sigma.initial", "5.0", "");
	params[11] = new ParameterInfo("sigma.final", "0.5", "");
	
	// call and wait for completion or error
	AnalysisJob job = submitJob(svc, params);
	JobInfo jobInfo = waitForErrorOrCompletion(job);
	
	// look for successful completion (not an error)
	assertTrue("Status not Finished", "Finished".equalsIgnoreCase(jobInfo.getStatus()));
	assertNoStddErrFileGenerated(job);
	assertFileWithNameGenerated(job, "som");
    }

    public void testCallDatapointsBubble() throws Exception{
	AnalysisService svc = (AnalysisService)serviceMap.get("SOMClustering");
	
	// set up parameters
	ParameterInfo params[] = new ParameterInfo[12];
	params[0] = getInputFileParam("dataset.filename","ALL_vs_AML_train_set_38_sorted.res");
	params[1] = new ParameterInfo("cluster.range", "2-3", "");
	params[2] = new ParameterInfo("iterations", "47654", "");
	params[3] = new ParameterInfo("seed.range", "38", "");
	params[4] = new ParameterInfo("som.rows", "0", "");
	params[5] = new ParameterInfo("som.cols", "0", "");
	params[6] = new ParameterInfo("initialization", "Random_Datapoints", "");
	params[7] = new ParameterInfo("neighborhood", "Bubble", "");
	params[8] = new ParameterInfo("alpha.initial", "0.1", "");
	params[9] = new ParameterInfo("alpha.final", "0.005", "");
	params[10] = new ParameterInfo("sigma.initial", "5.0", "");
	params[11] = new ParameterInfo("sigma.final", "0.5", "");
	
	// call and wait for completion or error
	AnalysisJob job = submitJob(svc, params);
	JobInfo jobInfo = waitForErrorOrCompletion(job);
	
	// look for successful completion (not an error)
	assertTrue("Status not Finished", "Finished".equalsIgnoreCase(jobInfo.getStatus()));
	assertNoStddErrFileGenerated(job);
	assertFileWithNameGenerated(job, "som");
    }              


}
