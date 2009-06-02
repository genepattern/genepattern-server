package org.genepattern.integration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.log4j.Logger;

import junit.framework.TestCase;


public class TestRegistration extends TestCase {
	
	private static final String REGISTRATION_URL = "http://127.0.0.1:8020/gp/pages/registerServer.jsf";
//	private static final String REGISTRATION_URL = "http://genepatterntest.broad.mit.edu/gp/pages/registerServer.jsf";
	private static Logger logger = Logger.getLogger(TestRegistration.class.getName());	
	
	/**
	 * Try to register with GenePattern, without all required parameters.
	 * Response should be to return us to the registration page.
	 * @throws IOException 
	 * @throws HttpException 
	 */
	public void testRegistrationNegativeTest() throws HttpException, IOException {
		
		HttpClient client = new HttpClient();
		// TODO externalize base url
		
		String viewStateValue = getFormValues(client);
		if ((null == viewStateValue) || (0 == viewStateValue.length()))
			fail("did not get proper viewStateValue");
		
		PostMethod method = new PostMethod(REGISTRATION_URL);
//		ArrayList<Part> parts = new ArrayList<Part>();
//		parts.add(new StringPart("registrationForm:name", "registrationTest"));
		
		
//		parts.add(new StringPart("registrationForm:email", "jnedzel@broad.mit.edu"));
//		parts.add(new StringPart("registrationForm:emailUpdates", "false"));
//		parts.add(new StringPart("registrationForm:organization", "Broad"));
//		parts.add(new StringPart("registrationForm:address1", "7 Cambridge Center"));
//		parts.add(new StringPart("registrationForm:city", "Cambridge"));
//		parts.add(new StringPart("registrationForm:state", "MA"));
//		parts.add(new StringPart("registrationForm:country", "United States of America"));
//		parts.add(new StringPart("registrationForm", "registrationForm"));
//		parts.add(new StringPart("javax.faces.viewState", viewStateValue));
//		
//		Part[] partsArray = parts.toArray(new Part[parts.size()]);
		List<NameValuePair> fields = new ArrayList<NameValuePair>();
		fields.add(new NameValuePair("registrationForm:name", "registrationTest"));
		fields.add(new NameValuePair("registrationForm:email", "jnedzel@broad.mit.edu"));
		fields.add(new NameValuePair("registrationForm:emailUpdates", "false"));
		fields.add(new NameValuePair("registrationForm:organization", "Broad"));
		fields.add(new NameValuePair("registrationForm:address1", "7 Cambridge Center"));
		fields.add(new NameValuePair("registrationForm:city", "Cambridge"));
		fields.add(new NameValuePair("registrationForm:state", "MA"));
		fields.add(new NameValuePair("registrationForm:country", "United States of America"));
		fields.add(new NameValuePair("registrationForm", "registrationForm"));
		fields.add(new NameValuePair("javax.faces.ViewState", viewStateValue));	
        fields.add(new NameValuePair("registrationForm:iAgree", "I AGREE"));		
        NameValuePair[] fieldsArr = new NameValuePair[fields.size()];
        fieldsArr = fields.toArray(fieldsArr);
        method.setRequestBody(fieldsArr);
		method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
				new DefaultHttpMethodRetryHandler(3, false));
		
		int statusCode = client.executeMethod(method);
		
		if (statusCode != HttpStatus.SC_OK) {
			String message = "Method failed";
			logError(message);
			fail("registration failed");
		}	
		
		String responseBody = method.getResponseBodyAsString();
		assertNotNull(responseBody);
		logDebug(responseBody);
		if (responseBody.contains("Please enter your name")) {
			// we're back at the registration page, so this succeeded
			assertTrue(true);
		} else {
			fail("We should have been sent back to the registration page with message indicating missing name.");
		}
	}
	
	/**
	 * Does a get method to retrieve required viewStateValue;
	 * @param client
	 * @throws IOException 
	 * @throws HttpException 
	 */
	private String getFormValues(HttpClient client) throws HttpException, IOException {
		GetMethod get = new GetMethod(REGISTRATION_URL);
		get.setFollowRedirects(true);
		client.executeMethod(get);
//		if (!REGISTRATION_URL.equals(get.getPath())) {
//			// unclear what this state is
//			fail("should not have gotten here");
//		}
		
		String viewStateValue = parseRegistrationForm(get);
		System.out.println("viewStateValue = " + viewStateValue);
		return viewStateValue;
	}

	/**
	 * TODO fix logging
	 * @param method
	 * @return
	 */
	private String parseRegistrationForm(GetMethod method) {
        final String matchViewState = "name=\"javax.faces.ViewState\" id=\"javax.faces.ViewState\" value=\"";
        
        String rval = "";
        try {
            String body = method.getResponseBodyAsString();
            int i = body.indexOf(matchViewState);
            if (i < 0) {
                System.out.println("login.httpResponse did not contain javax.jaces.ViewState");
                return rval;
            }
            i += matchViewState.length();
            int j = body.indexOf('"', i);
            rval = body.substring(i,j);
            
            return rval;
        }
        catch (IOException e) {
            System.err.println("Error parsing " + REGISTRATION_URL+ ": " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        System.err.println("Didn't find expected parameter in loginForm: "+matchViewState);
        	
        return rval;		
		
	}

	/*
	 * Logs an error message.
	 */
	private static void logError(String message) {
		logger.error(message);
	}	

	private static void logDebug(String message) {
		logger.debug(message);
	}
	

}
