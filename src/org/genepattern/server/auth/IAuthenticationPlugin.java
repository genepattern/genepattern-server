/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.auth;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface IAuthenticationPlugin {
    /**
     * Redirect to login form.
     * 
     * Use-case: A user-agent requests a resource which requires authentication. The session has not yet been authenticated,
     * so the server requests authentication via this plugin. 
     *
     * It is up to the plugin to decide how to request authentication from the user-agent; one example is to redirect the
     * user-agent to a login page.
     * 
     * @param request
     * @param response
     * @throws IOException
     */
    public void requestAuthentication(HttpServletRequest request, HttpServletResponse response) throws IOException;

    /**
     * Authenticate request from GenePattern web application.
     * 
     * @param request
     * @param response
     * @return the user id to be mapped to a GenePattern account.
     * @throws AuthenticationException
     */
    public String authenticate(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException;

    /**
     * Authenticate using the username and password passed in via a SOAP request.
     * 
     * @param user
     * @param credentials
     * @return true iff the username and credentials are valid.
     * @throws AuthenticationException to indicate invalid username or credentials.
     */
    public boolean authenticate(String user, byte[] credentials) throws AuthenticationException;

    /**
     * Logout user from session.
     * 
     * Use-case: A user clicks on the GenePattern logout link.
     * 
     * @param GenePattern userid
     * @param request
     * @param response
     */
    public void logout(String userid, HttpServletRequest request, HttpServletResponse response);

}
