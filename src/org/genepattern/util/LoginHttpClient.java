package org.genepattern.util;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.Cookie;
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
        LoginFormFields formValues = null;
        try {
            loginGet = new GetMethod(serverUrl + HOME_PAGE);
            loginGet.setFollowRedirects(true);
            client.executeMethod(loginGet);
            //check if already logged in
            if (! LOGIN_PAGE.equals(loginGet.getPath())) {
                return LoginState.SUCCESS;
            }
            formValues = parseLoginForm(loginGet);
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
            fields.add(new NameValuePair("javax.faces.ViewState", formValues.viewStateValue));
            fields.add(new NameValuePair("username", username));
            if (password != null) {
                fields.add(new NameValuePair("loginForm:password", password));
            }
            fields.add(new NameValuePair(formValues.submitName, "Sign In"));
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
    
    private static class LoginFormFields {
        public String viewStateValue = "";
        public String submitName = "";
    }

    /**
     * Parse the contents of the login form to find the rquired dynamically generated form names and values.
     *
     * Here is an example snippet from the generated login form.
     <pre>
     <input type="hidden" name="javax.faces.ViewState" id="javax.faces.ViewState" value="j_id2006:j_id2007" />
     <input type="submit" name="loginForm:j_id35" value="Sign In" />
     </pre>
     This method must find the corresponding value for ViewState and the correct name for the submit button.
     *
     * @param method an HttpGetMethod after requesting the login page.
     * @return the dynamically generated form names and values to post with the login form.
     */
    private static LoginFormFields parseLoginForm(HttpMethod method) {
        final String matchViewState = "name=\"javax.faces.ViewState\" id=\"javax.faces.ViewState\" value=\"";
        final String matchSubmit = "<input type=\"submit\" name=\"";
        
        LoginFormFields rval = new LoginFormFields();
        try {
            String body = method.getResponseBodyAsString();
            int i = body.indexOf(matchViewState);
            if (i < 0) {
                System.out.println("login.httpResponse did not contain javax.jaces.ViewState");
                return rval;
            }
            i += matchViewState.length();
            int j = body.indexOf('"', i);
            rval.viewStateValue = body.substring(i,j);
            
            i = body.indexOf(matchSubmit, j);
            if (i < 0) {
                System.out.println("login.httpResponse did not contain '"+matchSubmit+"'");
                return rval;
            }
            i += matchSubmit.length();
            j = body.indexOf('"', i);
            rval.submitName = body.substring(i,j);
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
            String newUrl = 
                uri.getScheme() + 
                "://" +
                uri.getHost() + 
                ":" + 
                uri.getPort();
            this.serverUrl = newUrl;
        }
        catch (IOException e) {
            log.error("Unable to set server url: "+e.getMessage(),e);
        }
    }

    //just here for unit testing
    private static void printCookies(PrintStream out, HttpClient httpClient) {
        out.println("Cookies...");
        for(Cookie cookie : httpClient.getState().getCookies()) {
            out.print("\t"+cookie.getName());
            out.print(": ");
            out.println(cookie.getValue());
        }
    }
    
    //TODO: This is a unit test
    public static void main(String[] args) {
        PrintStream out = System.out;
        if (args.length < 2 || args.length > 3) {
            out.println("Usage: TestLogin <url:http://localhost:8080> <username> <password>");
            return;
        }
        final String url = args[0];
        final String username = args[1];
        final String password = args.length == 3 ? args[2] : null;
        
        LoginHttpClient testLogin = new LoginHttpClient();
        testLogin.setServerUrl(url);
        testLogin.setUsername(username);
        testLogin.setPassword(password);

        HttpClient client = new HttpClient();
        LoginHttpClient.LoginState state = null;

        out.print("logging in...");
        state = testLogin.login(client);
        out.println(state);
            
        if (state.equals(LoginState.IO_EXCEPTION)) {
            out.print("logging in (to 3.0 server)...");
            state = testLogin.login3_0(client);
            out.println(state);
        }

        //duplicate login
        out.print("logging in...");
        state = testLogin.login(client);
        out.println(state);

        //get a page
        GetMethod get = new GetMethod(url + HOME_PAGE + "?PreprocessDataset");
        get.setFollowRedirects(true);
        try {
            client.executeMethod(get);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        out.println("path: "+get.getPath());
        printCookies(out, client);
    }
}
