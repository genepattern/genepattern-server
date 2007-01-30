package org.genepattern.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

/**
 * Class used to authenticate with server and download job result files over
 * HTTP.
 * 
 * @author Joshua Gould
 * 
 */
public class JobDownloader {
    HttpClient client = new HttpClient();

    private String username;

    private String password;

    private String server;

    public JobDownloader(String server, String username, String password) {
        this.username = username;
        this.password = password;
        this.server = server;
        client.setState(new HttpState());
        client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
    }

    private void login() throws IOException {
        PostMethod postMethod = new PostMethod();
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

    public void download(int jobNumber, String filename, File outputFile) throws IOException {
        GetMethod get = new GetMethod(server + "/gp/jobResults/" + jobNumber + "/" + filename);
        server = get.getURI().toString();
        int response = client.executeMethod(get);
        if (response == 403) {
            login();
            download(jobNumber, filename, outputFile);
            return;
        }
        InputStream is = get.getResponseBodyAsStream();
        BufferedOutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(outputFile));
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
            outputFile.setLastModified(System.currentTimeMillis());
            get.releaseConnection();
        }

    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
