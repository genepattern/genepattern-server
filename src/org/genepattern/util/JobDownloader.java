/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * Class used to authenticate with server and download job result files over HTTP.
 * 
 * @author Joshua Gould
 * 
 */
public class JobDownloader {
    private HttpClient client = new HttpClient();
    private LoginHttpClient login = new LoginHttpClient();
    private String server;
    private String contextPath = "/gp";
    

    public JobDownloader(String server, String username, String password) {
        if (server != null && server.endsWith("/")) {
            //remove trailing slash
            server = server.substring(0, server.length() - 1);
        }
        this.server = server;
        login = new LoginHttpClient();
        login.setUsername(username);
        login.setPassword(password);
        login.setServerUrl(server);
        client.setState(new HttpState());
        client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
    }

    public void download(int jobNumber, String filename, File outputFile) throws IOException {
        login();
        _download(server + contextPath + "/jobResults/" + jobNumber + "/" + filename, outputFile);
    }

    public void download(String url, File outputFile) throws IOException {
        URL u = new URL(url);
        server = u.getProtocol() + "://" + u.getHost() + ":" + u.getPort();
        login();
        _download(url, outputFile);
    }

    public GetMethod getGetMethod(int jobNumber, String filename) throws IOException {
        login();
        GetMethod get = new GetMethod(server + contextPath + "/jobResults/" + jobNumber + "/" + filename);
        client.executeMethod(get);
        return get;
    }

    private void _download(String url, File outputFile) throws IOException {
        InputStream is = null;
        BufferedOutputStream os = null;
        GetMethod get = new GetMethod(url);
        try {
            client.executeMethod(get);
            os = new BufferedOutputStream(new FileOutputStream(outputFile));
            is = get.getResponseBodyAsStream();
            byte[] b = new byte[10000];
            int bytesRead = 0;
            while ((bytesRead = is.read(b)) != -1) {
                os.write(b, 0, bytesRead);
            }
        } 
        finally {
            if (os != null) {
                try {
                    os.close();
                } 
                catch (IOException x) {
                }
            }
            if (is != null) {
                try {
                    is.close();
                } 
                catch (IOException x) {
                }
            }
            outputFile.setLastModified(System.currentTimeMillis());
            get.releaseConnection();
        }
    }

    private void login() throws IOException {
        LoginHttpClient.LoginState state = login.login(client);
        String serverUrl = login.getServerUrl();
        if (!this.server.equals(serverUrl)) {
            this.server = serverUrl;
            state = login.login(client);
        }
        if (state == LoginHttpClient.LoginState.IO_EXCEPTION) {
            throw new IOException("Unable to login to GenePattern server");
        }
    }
}
