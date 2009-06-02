package org.genepattern.integration;

import java.io.File;
import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.broadinstitute.io.matrix.ParseException;
import org.genepattern.util.LoginHttpClient;
import org.genepattern.util.LoginHttpClient.LoginState;
import org.genepattern.webservice.Parameter;
import org.genepattern.webservice.WebServiceException;



public class TestResultsPage extends GenePatternTest {
	
	Logger logger = Logger.getLogger(TestResultsPage.class.getName());
	
	public static final String PREPROCESS_LSID = "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00020";
	public static final String SMALL_GCT = "resources/data/small.gct";
	public static final String SMALL_PREPROCESS_RESULT_GCT = "small.preprocessed.gct";
	
	// TODO fix handling of gp url 
	private static final String RESULTS_URL = "http://localhost:8080/gp/jobResults";
	
	public void testRunPreprocesCheckResultsPage() throws Exception {
		
		for (int i = 0; i < 50; i++) {
			logger.debug("\n***** Starting increment : " + i);
			long start = System.currentTimeMillis();
			runPreprocess();
			long elapsedTimeMillis = System.currentTimeMillis() - start;
			logger.debug("elapsed time for running 100 jobs in this increment: " + (elapsedTimeMillis/1000F) + " seconds");
			start = System.currentTimeMillis();
			checkResultsPage();
			elapsedTimeMillis = System.currentTimeMillis() - start;
			logger.debug("elapsed time to check results page: " + (elapsedTimeMillis/1000F) + " seconds");
		}
	}

	private void runPreprocess() throws IOException, WebServiceException,
			ParseException {
		for (int i = 0; i < 100; i++) {
			logger.debug("\t***** starting job: " + i);
			File testFile = new File(SMALL_GCT);		
			Parameter[] params = new Parameter[1];
			Parameter fileParam = new Parameter("input.filename", testFile);
			params[0] = fileParam;
			File[] files = runModule(params, PREPROCESS_LSID, 1);
			validateResultFile(files, SMALL_PREPROCESS_RESULT_GCT, 2, 3);
		}
	}
	
	private void checkResultsPage() throws Exception {
		// TODO fix handling of login params
		HttpClient client = login("test", "test", "http://localhost:8080/gp");
		GetMethod get = new GetMethod(RESULTS_URL);
		get.setFollowRedirects(true);
		client.executeMethod(get);
		String body = get.getResponseBodyAsString();
		assertNotNull("get returned a null body", body);
		assertNotSame("get returned an empty body", 0, body.length());
		assertTrue("get did not return the results page", body.contains("Show Execution Logs"));
	}
	
	/**
	 * TODO throw exception if login fails
	 * @throws LoginException 
	 */
	public static HttpClient login(String username, String password, String loginUrl) throws HttpException, IOException, Exception {
		LoginHttpClient lClient = new LoginHttpClient();
		lClient.setUsername(username);
		lClient.setPassword(password);
		// Check that URL is correct.
		lClient.setServerUrl(loginUrl);
		
		HttpClient client = new HttpClient();
		LoginState lState = lClient.login(client);
		if (lState != LoginState.SUCCESS) {
			String message = "login failure: " + lState;
			System.err.println(message);
			throw new Exception(message);
		}
		return client;
	}	

}
