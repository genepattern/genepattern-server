package org.genepattern.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;

/**
 * Class used to authenticate with server and download job result files over
 * HTTP.
 *
 * @author Joshua Gould
 *
 */
public class JobDownloader {
    private static Logger log = Logger.getLogger(JobDownloader.class);

    private HttpClient client = new HttpClient();

    private String password;

    private String server;

    private String username;

    public JobDownloader(String server, String username, String password) {
        this.username = username;
        this.password = password;
        this.server = server;
        client.setState(new HttpState());
        client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
    }

    public void download(int jobNumber, String filename, File outputFile) throws IOException {
        login();
        _download(server + "/gp/jobResults/" + jobNumber + "/" + filename, outputFile);
    }

    public void download(String url, File outputFile) throws IOException {
        URL u = new URL(url);
        server = u.getProtocol() + "://" + u.getHost() + ":" + u.getPort();
        login();
        url = server + u.getPath();
        _download(url, outputFile);
    }

    public GetMethod getGetMethod(int jobNumber, String filename) throws IOException {
        login();
        GetMethod get = new GetMethod(server + "/gp/jobResults/" + jobNumber + "/" + filename);
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
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException x) {
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException x) {
                }
            }

            outputFile.setLastModified(System.currentTimeMillis());
            get.releaseConnection();
        }
    }

    private void login() throws IOException {
        GetMethod get = new GetMethod(server + "/gp/pages/login.jsf");
        client.executeMethod(get);

        URL serverUrl = new URL(server);
        server = serverUrl.getProtocol() + "://" + get.getURI().getHost() + ":" + serverUrl.getPort();
        get.releaseConnection();

        String url = server + "/gp/pages/login.jsf";
        PostMethod postMethod = new PostMethod(url);
        NameValuePair useridPair = new NameValuePair("username", username);
        if (password != null) {
            NameValuePair passwordPair = new NameValuePair("password", password);
            postMethod.setRequestBody(new NameValuePair[] { useridPair, passwordPair });
        } else {
            postMethod.setRequestBody(new NameValuePair[] { useridPair });
        }
        client.executeMethod(postMethod);
        postMethod.releaseConnection();
    }
}
