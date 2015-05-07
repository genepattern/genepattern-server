/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.util;

import java.io.PrintStream;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.genepattern.webservice.AdminProxy;
import org.genepattern.webservice.WebServiceException;

/**
 * Example code for logging an http client into a GenePattern Server.
 * This uses a SOAP client to detect the version of GenePattern (3.0 or 3.1).
 * Because the login process is different for each version.
 * 
 * @author pcarr
 */
public class LoginHttpClientDemo {
    
    private static AdminProxy getAdmin(String server, String username, String password) 
    throws WebServiceException
    {
        AdminProxy adminProxy = new AdminProxy(server, username, password);
        return adminProxy;
        
    }
    
    /**
     * If you have an instance of AdminService, just do the following:
     * <code>
     * Map map = admin.getServiceInfo;
     * String version = (String) map.get("genepattern.version");
     * </code>
     * @param gpProps
     * @return
     */
    private static String getVersion(AdminProxy adminProxy) throws WebServiceException 
    {
        String version = "";
        Map map = adminProxy.getServiceInfo();
        Object gpVersion = map.get("genepattern.version");
        if (gpVersion instanceof String) {
            version = (String) gpVersion;
        }
        return version;
    }

    /**
     * Exampe usage can be run as a unit test.
     */
    public static void main(String[] args) {
        PrintStream out = System.out;
        if (args.length < 2 || args.length > 3) {
            out.println("Usage: TestLogin <url:http://localhost:8080/gp> <username> <password>");
            return;
        }
        final String url = args[0];
        final String username = args[1];
        final String password = args.length == 3 ? args[2] : null;
        
        LoginHttpClient testLogin = new LoginHttpClient();
        testLogin.setServerUrl(url);
        testLogin.setUsername(username);
        testLogin.setPassword(password);
        
        boolean is3_0 = false;
        boolean is3_1 = false;
        try {
            AdminProxy admin = getAdmin(url, username, password);
            String v = getVersion(admin);
            is3_0 = v != null && v.startsWith("3.0");
            is3_1 = v != null && (v.equals("3.1") || v.startsWith("3.1.1"));
        }
        catch (Exception e) {
            //TODO: plug in some sort of logging mechanism
            e.printStackTrace();
        }

        HttpClient client = new HttpClient();
        LoginHttpClient.LoginState state = null;

        if (is3_0) {
            out.print("logging in (to 3.0 server)...");
            state = testLogin.login3_0(client);
        }
        else if (is3_1) {
            out.print("logging in (to 3.1 server)...");
            state = testLogin.login3_1(client);
        }
        else {
            out.print("logging in...");
            state = testLogin.login(client);
        }
        out.println(state);

        //duplicate login
        out.print("logging in again...");
        state = testLogin.login(client);
        out.println(state);

        //get a page
        GetMethod get = new GetMethod(url + "/gp/pages/index.jsf?PreprocessDataset");
        get.setFollowRedirects(true);
        try {
            client.executeMethod(get);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        //if login failed the path would give you the login page again
        out.println("path: "+get.getPath());
    }

}
