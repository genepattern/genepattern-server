/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.util;

import java.io.IOException;
import java.io.PrintStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;

/**
 * Example code for requesting email notification upon job completion.
 * This uses an HTTP client to login to genepattern and then makes an 
 * HTTP POST to request notification on job completion.
 *
 * @author Peter Carr
 */
public class NotifyEmailDemo {

    /**
     * Example usage can be run as a unit test.
     */
    public static void main(String[] args) {
        PrintStream out = System.out;
        if (args.length < 4 || args.length > 5) {
            out.println("Usage: NotifyEmailDemo <url:http://localhost:8080> <username> [<password>] <email> <job #>");
            return;
        }
        int i=0;
        final String url = args[i++];
        final String username = args[i++];
        final String password = args.length == 5 ? args[i++] : null;
        final String userEmail = args[i++];
        final String jobID = args[i++];

        //initialize an http client
        HttpClient client = new HttpClient();

        //login to the server
        out.println("logging into "+url+" as "+username+" ...");
        LoginHttpClient testLogin = new LoginHttpClient();
        testLogin.setServerUrl(url);
        testLogin.setUsername(username);
        testLogin.setPassword(password);

        LoginHttpClient.LoginState state = null;
        state = testLogin.login(client);
        if (! LoginHttpClient.LoginState.SUCCESS.equals(state)) {
            System.err.println("Unable to connect to server: "+state);
            return;
        }

        //use the same session to send an HTTP POST (must be a POST) to request email notification
        out.println("sending HTTP POST to request email notification for job # "+jobID);
        PostMethod post = new PostMethod(url + "/gp/notifyJobCompletion.ajax");
        post.setFollowRedirects(false);
        post.addParameter("jobID", jobID);
        post.addParameter("cmd", "notifyEmailJobCompletion");
        post.addParameter("userID", username);
        post.addParameter("userEmail", userEmail);
        try {
            client.executeMethod(post);
        }
        catch (IOException e) {
            System.err.println("EmailNotification failed, because ...");
            System.err.println("HTTP POST failed: "+e.getLocalizedMessage());
            e.printStackTrace();
            return;
        }
        finally { 
            post.releaseConnection();
        }
        out.println("notification (will be) sent to "+userEmail);
    }

}
