/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.genepattern.webservice.AdminProxy;
import org.genepattern.webservice.WebServiceException;

/**
 * Login to the GenePattern server with an HttpClient.
 * 
 * @author pcarr
 * @see org.genepattern.server.webapp.LoginServlet
 */
public class LoginHttpClient {
    private static Logger log = Logger.getLogger(LoginHttpClient.class);

    public enum LoginState { 
        SUCCESS, 
        INVALID, //incorrect username and/or password
        IO_EXCEPTION,  //problem connecting to the server
    }

    private String serverUrl = ""; //e.g. http://127.0.0.1:8080
    private String contextPath = "/gp";

    private String username = "";
    private String password = null;

    /**
     * The max number of times to follow a redirect to the login page. One, 1, redirect is the max necessary for the default configuration. 
     */
    private int maxRedirectCount = 5;

    public LoginHttpClient() {
        checkMaxRedirectCount();
    }

    /**
     * The property name of an optional System property which can be used to override the default maxRedirectCount.
     * If, most likely for debugging, you need to change this property you can do so by setting a System property.
     * One way to do this is with a command line argument to the java program, e.g.,
     * <code>
       java -Dorg.genepattern.util.LoginHttpClient.MAX_REDIRECT_COUNT=100 ...
     * </code>
     * To disable redirects, set the value to 0.
     */
    final static public String MAX_REDIRECT_COUNT_PROP = "org.genepattern.util.LoginHttpClient.MAX_REDIRECT_COUNT";
    /**
     * Check for property override of default maxRedirectCount.
     */
    private void checkMaxRedirectCount() {
        String maxRedirectCountProp = System.getProperty(MAX_REDIRECT_COUNT_PROP);
        if (maxRedirectCountProp != null) {
            try {
                maxRedirectCount = Integer.parseInt(maxRedirectCountProp);
            } 
            catch (Exception e) {
                log.error("Unable to set org.genepattern.util.LoginHttpClient.MAX_REDIRECT_COUNT: "+e.getMessage(), e);
            }
        }        
    }

    /**
     * @param the hostname and port of the GenePattern Server, e.g. <pre>http://localhost:8080</pre>.
     *     Trailing slash character is removed from the url.
     */
    public void setServerUrl(String url) {
        if (url != null && url.endsWith("/")) {
            log.error("Invalid input to LoginHttpClient.setServerUrl: "+url);
            log.error("Trailing slash ignored");
            url = url.substring(0, url.length() - 1);
        }
        this.serverUrl = url;
    }
    
    /**
     * @return the serverUrl, which can change after a call to login.
     */
    public String getServerUrl() {
        return this.serverUrl;
    }

    /**
     * @param contextPath - the servlet context path, e.g. <pre>/gp</pre>.
     */
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
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

    public LoginState login(HttpClient client) {
        return loginWithVersionCheck(client);
    }

    /**
     * Login to a GenePattern 3.2+ server.
     * @param client
     * @return
     */
    public LoginState loginLatest(HttpClient client) {
        String loginServletLocation = serverUrl + contextPath + "/login";
        return loginAndHandleRedirect(client, loginServletLocation, 0);
    }
    
    /**
     * Post login credentials to the login servlet 
     * and handle special case when the GenePattern Server requires a redirect in order to POST to the login servlet.
     * This can happen when, <ul>
     *   <li>the user enters 'localhost' as the server name, or
     *   <li>the server is configured to redirect to fully qualified host name (fqhn) and the user does not enter the fqhn.
     * </ul>
     * 
     * @param client, the http client
     * @param loginServletLocation, the url of the login servlet
     * @param redirectCount, current number of times that a redirect request has been sent.
     * @return
     */
    private LoginState loginAndHandleRedirect(HttpClient client, String loginServletLocation, int redirectCount) {
        if (redirectCount > maxRedirectCount) {
            log.error("LoginHttpClient: Too many redirects: "+ redirectCount);
            return LoginState.IO_EXCEPTION;
        }
        
        PostMethod loginPost = null;
        loginPost = new PostMethod(loginServletLocation);
        List<NameValuePair> fields = new ArrayList<NameValuePair>();
        fields.add(new NameValuePair("username", username));
        if (password != null) {
            fields.add(new NameValuePair("password", password));
        }
        //tell the login servlet to not send a redirect request
        fields.add(new NameValuePair("redirect", "false"));
        NameValuePair[] fieldsArr = new NameValuePair[fields.size()];
        fieldsArr = fields.toArray(fieldsArr);
        loginPost.setRequestBody(fieldsArr);
        try {
            int responseCode = client.executeMethod(loginPost);
            if (responseCode >= 200 && responseCode < 300) {
                updateServerUrl(loginPost);
                //status OK
                return LoginState.SUCCESS;
            }
            if (responseCode >= 300 && responseCode < 400) {
                //special case: check for redirect to login
                Header locationHeader = loginPost.getResponseHeader("Location");
                String redirectTo = locationHeader.getValue();
                if (redirectTo.toLowerCase().indexOf("/login") >= 0) {
                    ++redirectCount;
                    //note: recursive call, make sure to aviod infinite loops
                    return loginAndHandleRedirect(client, redirectTo, redirectCount);
                }
            }
            return LoginState.INVALID;
        }
        catch (IOException e) {
            return LoginState.IO_EXCEPTION;
        }
        finally {
            if (loginPost != null) {
                loginPost.releaseConnection();
            }
        }
    }

    //------------ legacy code for connecting to GenePattern 3.1 and 3.1.1 -------------------- //
    private LoginState loginWithVersionCheck(HttpClient client) {
        //1. get the version
        String version = null;
        try {
            version = getVersion();
        }
        catch (WebServiceException e) {
            log.warn("Unable to get GenePattern Version: "+e.getLocalizedMessage(), e);
        }
        
        return loginWithVersion(client, version);
    }

    /**
     * If you have an instance of AdminService, just do the following instead of calling this method.
     * <code>
     * Map map = admin.getServiceInfo;
     * String version = (String) map.get("genepattern.version");
     * </code>
     * @param gpProps
     * @return
     */
    private String getVersion() throws WebServiceException 
    {
        AdminProxy adminProxy = new AdminProxy(serverUrl, username, password);
        
        String version = "";
        Map map = adminProxy.getServiceInfo();
        Object gpVersion = map.get("genepattern.version");
        if (gpVersion instanceof String) {
            version = (String) gpVersion;
        }
        return version;
    }

    public LoginState loginWithVersion(HttpClient client, String version) {
        //special cases for 3.0 and 3.1 and 3.1.1 servers
        if (version != null && version.startsWith("3.0")) {
            return login3_0(client);
        }
        else if (version != null && (version.equals("3.1") || version.equals("3.1.1"))) {
            return login3_1(client);
        }
        
        return loginLatest(client);
    }

    /**
     * Login to a GenePattern 3.1 or 3.1.1 server.
     * @param client - the http client with which to perform subsequent http requests.
     * @throws Exception
     * @return a LoginState indicating success or failure.
     */
    public LoginState login3_1(HttpClient client) {
        final String LOGIN_PAGE = contextPath + "/pages/login.jsf";
        final String HOME_PAGE = contextPath + "/pages/index.jsf";

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
            viewStateValue = parse3_1LoginForm(loginGet);
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
            fields.add(new NameValuePair("loginForm:signIn", "Sign in"));
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
    private static String parse3_1LoginForm(HttpMethod method) {
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
            log.error("Error parsing "+method.getPath()+": "+e.getLocalizedMessage(), e);
        }
        log.error("Didn't find expected parameter in loginForm: "+matchViewState);
        return rval;
    }
    
    /**
     * Login to a GenePattern 3.0 Server.
     * @param client - an instance of an http client.
     * @return a LoginState indicating success or failure.
     */
    public LoginState login3_0(HttpClient client) {
        final String LOGIN_PAGE = contextPath + "/pages/login.jsf";
        
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
        final String LOGIN_PAGE = contextPath + "/pages/login.jsf";
        final String HOME_PAGE = contextPath + "/pages/index.jsf";

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
