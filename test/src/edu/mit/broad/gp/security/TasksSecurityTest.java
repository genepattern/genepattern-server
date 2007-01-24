package edu.mit.broad.gp.security;

import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.AdminProxy;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskIntegratorProxy;
import org.genepattern.webservice.WebServiceException;
import org.junit.*;
import static org.junit.Assert.*;

public class TasksSecurityTest {	
	AdminProxy adminProxyAsAdmin;
	AdminProxy adminProxyNoPrivs;
	TaskIntegratorProxy taskIntegratorAsAdmin;
	TaskIntegratorProxy taskIntegratorNoPrivs;
	
	String suiteName = "SuiteSecurityTestSuite";
	
	String adminUser = "foo";// this user NEEDS admin or createTask permission
	String password="";
	String noPrivsUser = "bar";  // this user MUST NOT have admin, deleteTask or createTask permission
	String url="http://127.0.0.1:8080/";
	
	String privateLsid = null;
	String publicLsid = "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:1";
	
	
	@Before public void setUp() throws WebServiceException {
		try {		
		adminProxyAsAdmin = new AdminProxy( url,  adminUser,  password);
		adminProxyNoPrivs = new AdminProxy( url,  noPrivsUser,  password);
		taskIntegratorAsAdmin = new TaskIntegratorProxy(url, adminUser, password);
	    taskIntegratorNoPrivs = new TaskIntegratorProxy(url, noPrivsUser, password);
		    
	    // copy ConvertLineEndings as user1 to get us a private task to test with
	    // since it deefaults to private
	    privateLsid = taskIntegratorAsAdmin.cloneTask(publicLsid, "copyOfCLE");
	    
	    
		} catch (WebServiceException wse){
			wse.printStackTrace();
			throw wse;
		}
	}
	
	/*
	 * delete a publicly visible task as a user without privs
	 */
	@Test public void deletePublicTaskWithoutPermission() throws WebServiceException {
			try {
				taskIntegratorNoPrivs.deleteTask(publicLsid);
				assertTrue(false);//should not get here
			} catch (WebServiceException wse){
				// should get an exception if we don't own it
				assert(true);
			}
	}
	
	/*
	 * delete a publicly visible task as a user without privs
	 */
	@Test public void deletePublicTaskWithoutPermission2() throws WebServiceException, RemoteException {
			try {
				taskIntegratorNoPrivs.getStub().delete(publicLsid);
				assertTrue(false);//should not get here
			} catch (Exception wse){
				// should get an exception if we don't own it
				assert(true);
			}
	}
	
	/*
	 * delete a non visible task as a user without privs
	 */
	@Test public void deletePrivateTaskWithoutPermissionOrVisibility() throws WebServiceException {
			try {
				taskIntegratorNoPrivs.deleteTask(privateLsid);
				assertTrue(false);//should not get here
			} catch (WebServiceException wse){
				// should get an exception if we don't own it
				assert(true);
			}
	}
	
	/*
	 * delete a non visible task as a user without privs
	 */
	@Test public void deletePrivateTaskWithoutPermissionOrVisibility2() throws WebServiceException, RemoteException {
			try {
				taskIntegratorNoPrivs.getStub().delete(privateLsid);
				assertTrue(false);//should not get here
			} catch (Exception wse){
				// should get an exception if we don't own it
				assert(true);
			}
	}
	
	/*
	 * delete a private, visible task as a user with privs
	 */
	@Test public void deleteMyPrivateTask() throws WebServiceException {
			try {
				taskIntegratorAsAdmin.deleteTask(privateLsid);
				assertTrue(true);//should  get here
			} catch (WebServiceException wse){
				// should not get an exception if we don't own it
				wse.printStackTrace();
				assert(false);
			}
	}
	/*
	 * delete a private, visible task as a user with privs
	 */
	@Test public void deleteMyPrivateTask2() throws WebServiceException, RemoteException {
			try {
				taskIntegratorAsAdmin.getStub().delete(privateLsid);
				assertTrue(true);//should  get here
			} catch (WebServiceException wse){
				// should not get an exception if we  own it
				wse.printStackTrace();
				assertTrue(false);
			}
	}

	/**
	 * Try to get the support file names for a task you are not allowed to see
	 * @throws WebServiceException
	 */	
	@Test public void getSomeoneElsesPrivateTaskSupportFileNames() throws WebServiceException {
		try {
			taskIntegratorNoPrivs.getSupportFileNames(privateLsid);
			assertTrue(false);//should not get here
		} catch (WebServiceException wse){
			// should get an exception if we don't own it
			wse.printStackTrace();
			assertTrue(true);
		}
	}
	
	/**
	 * Try to get the doc file names for a task you are not allowed to see
	 * @throws WebServiceException
	 */	
	@Test public void getSomeoneElsesPrivateTaskDocFileNames() throws WebServiceException {
		try {
			taskIntegratorNoPrivs.getDocFileNames(privateLsid);
			assertTrue(false);//should not get here
		} catch (WebServiceException wse){
			// should get an exception if we don't own it
			//wse.printStackTrace();
			assertTrue(true);
		}
	}
		
	/**
	 * Try to get the support files for a task you are not allowed to see
	 * @throws WebServiceException
	 */	
	@Test public void getSomeoneElsesPrivateTaskSupportFiles() throws WebServiceException {
		try {
			String[] filenames = new String[1];
			filenames[0] = "to_host.pl"; // we know this for convertLineEndings
			taskIntegratorNoPrivs.getSupportFiles(privateLsid, filenames, new File("."));
			assertTrue(false);//should not get here
		} catch (WebServiceException wse){
			// should get an exception if we don't own it
			//wse.printStackTrace();
			assertTrue(true);
		}
	}
	
	/**
	 * Try to get the support files for a task you are not allowed to see
	 * @throws WebServiceException
	 */	
	@Test public void deleteSomeoneElsesPrivateTaskSupportFiles() throws WebServiceException {
		try {
			String[] filenames = new String[1];
			filenames[0] = "to_host.pl"; // we know this for convertLineEndings
			taskIntegratorNoPrivs.deleteFiles(privateLsid, filenames);
			assertTrue(false);//should not get here
		} catch (WebServiceException wse){
			// should get an exception if we don't own it
			//wse.printStackTrace();
			assertTrue(true);
		}
	}
	
	/**
	 * Try to get the support files for a task you are not allowed to see
	 * @throws WebServiceException
	 */	
	@Test public void deletePublicTaskSupportFilesNoPrivs() throws WebServiceException {
		try {
			String[] filenames = new String[1];
			filenames[0] = "to_host.pl"; // we know this for convertLineEndings
			taskIntegratorNoPrivs.deleteFiles(publicLsid, filenames);
			assertTrue(false);//should not get here
		} catch (WebServiceException wse){
			// should get an exception if we don't own it
			//wse.printStackTrace();
			assertTrue(true);
		}
	}
	
	@After public void cleanUp(){
		try {
			// get rid of extra copies
			taskIntegratorAsAdmin.deleteTask(privateLsid);
		} catch (Exception e){}
	}
	
}
