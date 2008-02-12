/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2008) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/
package org.genepattern.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;

/**
 * Login to the GenePattern server with an HttpClient.
 * @author pcarr
 */
public class LoginHttpClient {
    private static Logger log = Logger.getLogger(LoginHttpClient.class);

    public enum LoginState { 
        SUCCESS, 
        INVALID, //incorrect username and/or password
        IO_EXCEPTION,  //problem connecting to the server
    }
    
    final static String LOGIN_PAGE = "/gp/pages/login.jsf";
    final static String HOME_PAGE = "/gp/pages/index.jsf";
    
    private String serverUrl = "";
    private String username = "";
    private String password = null;

    public LoginHttpClient() {
    }

    /**
     * @param the url to the GenePattern Server, e.g. <pre>http://localhost:8080</pre>.
     */
    public void setServerUrl(String url) {
        this.serverUrl = url;
    }
    
    /**
     * @return the serverUrl, which can change after a call to login.
     */
    public String getServerUrl() {
        return this.serverUrl;
    }

    /**
     * @param username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @param password, an optional parameter.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Login to a GenePattern 3.1 server.
     * @param client - the http client with which to perform subsequent http requests.
     * @throws Exception
     * @return a LoginState indicating success or failure.
     */
    public LoginState login(HttpClient client) {
        GetMethod loginGet = null;
        String viewStateValue = "";
        try {
            loginGet = new GetMethod(serverUrl + HOME_PAGE);
            loginGet.setFollowRedirects(true);
            client.executeMethod(loginGet);
            //check if already logged in
            if (! LOGIN_PAGE.equals(loginGet.getPath())) {
                return LoginState.SUCCESS;
            }
            viewStateValue = parseLoginForm(loginGet);
            updateServerUrl(loginGet);
        }
        catch (IOException e) {
            log.error(e);
            return LoginState.IO_EXCEPTION;
        }
        finally {
            if (loginGet != null) {
                loginGet.releaseConnection();
            }
        }
        
        PostMethod loginPost = null;
        try {
            loginPost = new PostMethod(serverUrl + LOGIN_PAGE);
            List<NameValuePair> fields = new ArrayList<NameValuePair>();
            fields.add(new NameValuePair("loginForm", "loginForm"));
            fields.add(new NameValuePair("javax.faces.ViewState", viewStateValue));
            fields.add(new NameValuePair("username", username));
            if (password != null) {
                fields.add(new NameValuePair("loginForm:password", password));
            }
            fields.add(new NameValuePair("loginForm:signIn", "Sign In"));
            NameValuePair[] fieldsArr = new NameValuePair[fields.size()];
            fieldsArr = fields.toArray(fieldsArr);
            loginPost.setRequestBody(fieldsArr);
            client.executeMethod(loginPost);
        }
        catch (IOException e) {
            log.error(e);
            return LoginState.IO_EXCEPTION;
        }
        finally {
            if (loginPost != null) {
                loginPost.releaseConnection();
            }
        }
        return validateLogin(client);
    }
    
    /**
     * Parse the contents of the login form to find the required dynamically generated ViewState value.
     * Here is an example snippet from the generated login form.
     <pre>
     <input type="hidden" name="javax.faces.ViewState" id="javax.faces.ViewState" value="j_id2006:j_id2007" />
     </pre>
     *
     * @param method an HttpGetMethod after requesting the login page.
     * @return the dynamically generated value for ViewState
     */
    private static String parseLoginForm(HttpMethod method) {
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
            log.error("Error parsing "+LOGIN_PAGE+": "+e.getLocalizedMessage(), e);
        }
        log.error("Didn't find expected parameter in loginForm: "+matchViewState);
        return rval;
    }
    
    /**
     * Login to a GenePattern 3.0 Server.
     * @param client - an instance of an http client.
     * @return a LoginState indicating sucess or failure.
     */
    public LoginState login3_0(HttpClient client) {
        GetMethod loginGet = null;
        try {
            String reqParams = "?username="+username;
            if (password != null) {
                reqParams += "&password="+password;
            }
            loginGet = new GetMethod(serverUrl + LOGIN_PAGE + reqParams);
            loginGet.setFollowRedirects(true);
            client.executeMethod(loginGet);
        }
        catch (IOException e) {
            log.error(e);
            return LoginState.IO_EXCEPTION;
        }
        finally {
            if (loginGet != null) {
                loginGet.releaseConnection();
            }
        }
        return validateLogin(client);
    }

    /**
     * Validate login by 'get'ting the home page.
     * @param client
     * @return SUCCESS or FAILURE
     */
    private LoginState validateLogin(HttpClient client) {
        //validate the login 
        GetMethod get = null;
        try {
            get = new GetMethod(serverUrl + HOME_PAGE);
            client.executeMethod(get);
            if (HOME_PAGE.equals(get.getPath())) {
                return LoginState.SUCCESS;
            }
            else if (LOGIN_PAGE.equals(get.getPath())) {
                return LoginState.INVALID;
            }
            else {
                return LoginState.IO_EXCEPTION;
            }
        }
        catch (IOException e) {
            log.error(e);
            return LoginState.IO_EXCEPTION;
        }
        finally {
            if (get != null) {
                get.releaseConnection();
            }
        }        
    }

     /**
     * Automatically set the serverUrl based on the results of the page request.
     * This handles conversion from localhost to fully qualified host name.
     * @param method - the HttpGet after it has been executed.
     */
    private void updateServerUrl(HttpMethod method) {
        try {
            URI uri = method.getURI();
            int port = uri.getPort();
            String portStr = port > 0 ? ":" + port : "";
            this.serverUrl = uri.getScheme() +  "://" + uri.getHost() + portStr;
        }
        catch (IOException e) {
            log.error("Unable to set server url: "+e.getMessage(),e);
        }
    }

}
