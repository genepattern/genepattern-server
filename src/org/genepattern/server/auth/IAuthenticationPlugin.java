package org.genepattern.server.auth;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface IAuthenticationPlugin {
    /**
     * Redirect to login form.
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
     * Helper method (can be ignored) which authenticates given a username and some credentials such as a password.
     * 
     * @param user
     * @param credentials
     * @return
     * @throws AuthenticationException
     */
    public boolean authenticate(String user, byte[] credentials) throws AuthenticationException;
}
