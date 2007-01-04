package edu.mit.broad.gp.security;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.genepattern.util.IGPConstants;
import org.genepattern.webservice.AdminProxy;
import org.genepattern.webservice.AnalysisWebServiceProxy;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskIntegratorProxy;
import org.genepattern.webservice.WebServiceException;
import org.junit.*;

import static org.junit.Assert.*;



public class JobStatusSecurityTest {
	AnalysisWebServiceProxy analysisProxy1;
	AnalysisWebServiceProxy analysisProxy2;
	AdminProxy adminProxy1;
	TaskInfo convertLineEndings;
	
	String username1 = "foo";
	String password="";
	String username2 = "bar";
	String url="http://127.0.0.1:8080/";

	@Before public void setUp() throws WebServiceException {
		try {		
			analysisProxy1 = new AnalysisWebServiceProxy(url, username1, password);
			analysisProxy2 = new AnalysisWebServiceProxy(url, username2, password);
			adminProxy1 = new AdminProxy( url,  username1,  password);
			
			convertLineEndings = adminProxy1.getTask("urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:1");
		} catch (WebServiceException wse){
			wse.printStackTrace();
			throw wse;
		}
	}
	
	/*
	 * try to call terminateJob on a job owned by user1 as user2. This should
	 * return an exception and not terminate it 
	 */
	@Test public void terminateSomeoneElsesJob() throws WebServiceException, IOException {
		JobInfo job = runJob(analysisProxy1);
		
		try {
			analysisProxy2.terminateJob(job.getJobNumber());
			assertTrue(false); // should not get here
			
		} catch (WebServiceException wse){
			//wse.printStackTrace();
			assertTrue(true); //  should fail
		}
	
	}
	/*
	 * try to call terminateJob on a job owned by user1 as user1. This should
	 * work and terminate it if still running
	 */
	@Test public void terminateMyJob() throws WebServiceException, IOException {
		JobInfo job = runJob(analysisProxy1);
		
		try {
			analysisProxy1.terminateJob(job.getJobNumber());
			assertTrue(true); // should not get here
			
		} catch (WebServiceException wse){
			wse.printStackTrace();
			assertTrue(false); //  should not fail
		}
	
	}
	/*
	 * try to call deleteJob on a job owned by user1 as user2. This should
	 * return an exception  
	 */
	@Test public void deleteSomeoneElsesJob() throws WebServiceException, IOException {
		JobInfo job = runJob(analysisProxy1);
		
		try {
			analysisProxy2.deleteJob(job.getJobNumber());
			assertTrue(false); // should not get here
			
		} catch (WebServiceException wse){
			//wse.printStackTrace();
			assertTrue(true); //  should fail
		}
	
	}
	/*
	 * try to call deleteJob on a job owned by user1 as user1. This should
	 * work
	 */
	@Test public void deleteMyJob() throws WebServiceException, IOException {
		JobInfo job = runJob(analysisProxy1);
		
		try {
			analysisProxy1.deleteJob(job.getJobNumber());
			assertTrue(true); // should not get here
			
		} catch (WebServiceException wse){
			wse.printStackTrace();
			assertTrue(false); //  should not fail
		}
	
	}
	
	/*
	 * try to call getChildren on a job owned by user1 as user2. This should
	 * return an exception  
	 */
	@Test public void getSomeoneElsesJobChildren() throws WebServiceException, IOException {
		JobInfo job = runJob(analysisProxy1);
		
		try {
			analysisProxy2.getChildren(job.getJobNumber());
			assertTrue(false); // should not get here
			
		} catch (WebServiceException wse){
			//wse.printStackTrace();
			assertTrue(true); //  should fail
		}
	
	}
	/*
	 * try to call getChildren on a job owned by user1 as user1. This should
	 * work 
	 */
	@Test public void getMyJobChildren() throws WebServiceException, IOException {
		JobInfo job = runJob(analysisProxy1);
		
		try {
			analysisProxy1.getChildren(job.getJobNumber());
			assertTrue(true); // should not get here
			
		} catch (WebServiceException wse){
			wse.printStackTrace();
			assertTrue(false); //  should not fail
		}
	
	}
	
	
	/*
	 * try to call checkStatus on a job owned by user1 as user1. This should
	 * work 
	 */
	@Test public void getMyJobStatus() throws WebServiceException, IOException {
		JobInfo job = runJob(analysisProxy1);
		try {
			analysisProxy1.checkStatus(job.getJobNumber());
			assertTrue(true); // should not get here
		} catch (WebServiceException wse){
			wse.printStackTrace();
			assertTrue(false); //  should not fail
		}
	
	}
	/*
	 * try to call checkStatus on a job owned by user1 as user2. This should
	 * throw an exception 
	 */
	@Test public void getSomeoneElsesJobStatus() throws WebServiceException, IOException {
		JobInfo job = runJob(analysisProxy1);	
		try {
			analysisProxy2.checkStatus(job.getJobNumber());
			assertTrue(false); // should not get here		
		} catch (WebServiceException wse){
			//wse.printStackTrace();
			assertTrue(true); //  should fail
		}
	
	}
	/*
	 * try to call getJob on a job owned by user1 as user1. This should
	 * work and terminate it if still running
	 */
	@Test public void getMyJob() throws WebServiceException, IOException {
		JobInfo job = runJob(analysisProxy1);
		try {
			analysisProxy1.getStub().getJob(job.getJobNumber());
			assertTrue(true); // should not get here
		} catch (WebServiceException wse){
			wse.printStackTrace();
			assertTrue(false); //  should not fail
		}
	}
	/*
	 * try to call getJob on a job owned by user1 as user2. This should
	 * work and terminate it if still running
	 */
	@Test public void getSomeoneElsesJob() throws WebServiceException, IOException {
		JobInfo job = runJob(analysisProxy1);	
		try {
			analysisProxy2.getStub().getJob(job.getJobNumber());
			assertTrue(false); // should not get here		
		} catch (Exception wse){
			//wse.printStackTrace();
			assertTrue(true); //  should fail
		}
	}
	
	/*
	 * try to call getJobs on a job owned by user1 as user1. This should
	 * work and terminate it if still running
	 */
	@Test public void getMyJobs() throws WebServiceException, IOException {
		try {
			analysisProxy1.getJobs(username1, false);
			assertTrue(true); // should not get here
		} catch (WebServiceException wse){
			wse.printStackTrace();
			assertTrue(false); //  should not fail
		}
	}
	/*
	 * try to call getJobs on a job owned by user1 as user2. This should
	 * work and terminate it if still running
	 */
	@Test public void getSomeoneElsesJobs() throws WebServiceException, IOException {
		try {
			analysisProxy2.getJobs(username1, false);
			assertTrue(false); // should not get here		
		} catch (WebServiceException wse){
			//wse.printStackTrace();
			assertTrue(true); //  should fail
		}
	}
	
	/*
	 * try to call getResultFiles on a job owned by user1 as user1. This should
	 * work and terminate it if still running
	 */
	@Test public void getMyJobResultFiles() throws WebServiceException, IOException {
		JobInfo job = runJob(analysisProxy1);
		try {
			analysisProxy1.getResultFiles(job.getJobNumber());
			assertTrue(true); // should not get here
		} catch (WebServiceException wse){
			wse.printStackTrace();
			assertTrue(false); //  should not fail
		}
	}
	/*
	 * try to call getResultFiles on a job owned by user1 as user2. This should
	 * work and terminate it if still running
	 */
	@Test public void getSomeoneElsesJobResultFiles() throws WebServiceException, IOException {
		JobInfo job = runJob(analysisProxy1);	
		try {
			analysisProxy2.getResultFiles(job.getJobNumber());
			assertTrue(false); // should not get here		
		} catch (WebServiceException wse){
			//wse.printStackTrace();
			assertTrue(true); //  should fail
		}
	}
	
	/*
	 * try to call purgeJob on a job owned by user1 as user1. This should
	 * work and terminate it if still running
	 */
	@Test public void purgeMyJob() throws WebServiceException, IOException {
		JobInfo job = runJob(analysisProxy1);
		try {
			analysisProxy1.purgeJob(job.getJobNumber());
			assertTrue(true); // should not get here
		} catch (WebServiceException wse){
			wse.printStackTrace();
			assertTrue(false); //  should not fail
		}
	}
	/*
	 * try to call purgeJob on a job owned by user1 as user2. This should
	 * work and terminate it if still running
	 */
	@Test public void purgeSomeoneElsesJob() throws WebServiceException, IOException {
		JobInfo job = runJob(analysisProxy1);	
		try {
			analysisProxy2.purgeJob(job.getJobNumber());
			assertTrue(false); // should not get here		
		} catch (WebServiceException wse){
			wse.printStackTrace();
			assertTrue(true); //  should fail
		}
	}
	
	/*
	 * try to call deleteJobResultsFile on a job owned by user1 as user1. This should
	 * work and terminate it if still running
	 */
	@Test public void deleteJobResultFileMyJob() throws WebServiceException, IOException {
		JobInfo job = runJob(analysisProxy1);
		try {
			String status = job.getStatus();
			 while (!(status.equalsIgnoreCase("ERROR") || (status
		                .equalsIgnoreCase("Finished")))) {
				 try {
					 Thread.currentThread().sleep(1000);
				 } catch (Exception e){}
				 job = analysisProxy1.checkStatus(job.getJobNumber());
				 status = job.getStatus();
			}
				 
			
			analysisProxy1.deleteJobResultFile(job.getJobNumber(),""+job.getJobNumber()+"/"+outFileName);
			assertTrue(true); // should not get here
		} catch (WebServiceException wse){
			wse.printStackTrace();
			assertTrue(false); //  should fail
		}
	}
	/*
	 * try to call deleteJobResultFile on a job owned by user1 as user2. This should
	 * work and terminate it if still running
	 */
	@Test public void deleteJobResultsFileSomeoneElsesJob() throws WebServiceException, IOException {
		JobInfo job = runJob(analysisProxy1);	
		try {
			analysisProxy2.deleteJobResultFile(job.getJobNumber(),outFileName);
			assertTrue(false); // should not get here		
		} catch (WebServiceException wse){
			//wse.printStackTrace();
			assertTrue(true); //  should fail
		}
	}
	
	
	/*
	 * try to call createProvenancePipeline on a job owned by user1 as user1. This should
	 * work and terminate it if still running
	 */
	@Test public void createProvenancePipelineMyJob() throws WebServiceException, IOException {
		JobInfo job = runJob(analysisProxy1);
		try {
			JobInfo[] jobs = new JobInfo[1];
			jobs[0] = job;
			analysisProxy1.createProvenancePipeline(jobs, "somePipe");
			assertTrue(true); // should not get here
		} catch (WebServiceException wse){
			wse.printStackTrace();
			assertTrue(false); //  should not fail
		}
	}
	/*
	 * try to call createProvenancePipeline on a job owned by user1 as user2. This should
	 * work and terminate it if still running
	 */
	@Test public void createProvenancePipelineSomeoneElsesJob() throws WebServiceException, IOException {
		JobInfo job = runJob(analysisProxy1);	
		try {
			JobInfo[] jobs = new JobInfo[1];
			jobs[0] = job;
			analysisProxy2.createProvenancePipeline(jobs, "somePipe");
			assertTrue(false); // should not get here		
		} catch (WebServiceException wse){
			//wse.printStackTrace();
			assertTrue(true); //  should fail
		}
	}
	
	/*
	 * try to call createProvenancePipeline on a job owned by user1 as user1. This should
	 * work and terminate it if still running
	 */
	@Test public void createProvenancePipeline2MyJob() throws WebServiceException, IOException {
		JobInfo job = runJob(analysisProxy1);
		try {
			
			analysisProxy1.createProvenancePipeline(""+job.getJobNumber(), "somePipe");
			assertTrue(true); // should not get here
		} catch (WebServiceException wse){
			wse.printStackTrace();
			assertTrue(false); //  should not fail
		}
	}
	/*
	 * try to call createProvenancePipeline on a job owned by user1 as user2. This should
	 * work and terminate it if still running
	 */
	@Test public void createProvenancePipeline2SomeoneElsesJob() throws WebServiceException, IOException {
		JobInfo job = runJob(analysisProxy1);	
		try {
			analysisProxy2.createProvenancePipeline(""+job.getJobNumber(), "somePipe");
			assertTrue(false); // should not get here		
		} catch (WebServiceException wse){
			//wse.printStackTrace();
			assertTrue(true); //  should fail
		}
	}
	
	/*
	 * try to call findJobsThatCreatedFile on a job owned by user1 as user1. This should
	 * work and terminate it if still running
	 */
	@Test public void findJobsThatCreatedFileMyJob() throws WebServiceException, IOException {
		JobInfo job = runJob(analysisProxy1);
		try {
			
			analysisProxy1.findJobsThatCreatedFile(""+job.getJobNumber());
			assertTrue(true); // should not get here
		} catch (WebServiceException wse){
			wse.printStackTrace();
			assertTrue(false); //  should not fail
		}
	}
	/*
	 * try to call createProvenancePipeline on a job owned by user1 as user2. This should
	 * work and terminate it if still running
	 */
	@Test public void findJobsThatCreatedFileSomeoneElsesJob() throws WebServiceException, IOException {
		JobInfo job = runJob(analysisProxy1);	
		try {
			analysisProxy2.findJobsThatCreatedFile(""+job.getJobNumber());
			assertTrue(false); // should not get here		
		} catch (WebServiceException wse){
			//wse.printStackTrace();
			assertTrue(true); //  should fail
		}
	}
	
	String outFileName = "foo.out";
	/**
	 * a simple method to run ConvertLineEndings with a file it creates if necessary
	 * 
	 * @param proxy
	 * @return the JobInfo object for the job it launched
	 * @throws WebServiceException
	 * @throws IOException
	 */
	protected JobInfo runJob(AnalysisWebServiceProxy proxy) throws WebServiceException, IOException {
		ParameterInfo[] parameters = new ParameterInfo[2];
		File f = new File("tempfile.txt");
		if (!f.exists()){
			try {
				FileWriter fw = new FileWriter(f);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write("test file");
				bw.close();
			}catch (IOException ioe){
				ioe.printStackTrace();
			}
		}
		parameters[0] = new ParameterInfo("input.filename", f.getCanonicalPath() ,"");
		parameters[0].setAsInputFile();
		parameters[1] = new ParameterInfo("output.file", "foo.out" ,"");
		
		JobInfo ji = proxy.submitJob(convertLineEndings.getID(), parameters);
		return ji;
	}
	
	
}
