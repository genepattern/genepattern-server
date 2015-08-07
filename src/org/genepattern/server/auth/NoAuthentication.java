/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.auth;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * AuthenticationPlugin which blocks all requests for authentication; 
 * Use this when there is a configuration error in the authentication plugin factory.
 * 
 * @author pcarr
 */
public class NoAuthentication implements IAuthenticationPlugin {
    private String errorMessage = "";
    public NoAuthentication() {
    }
    public NoAuthentication(Exception e) {
        this.errorMessage = "Authentication configuration error: "+e.getClass().getName()+" : "+e.getLocalizedMessage();
    }
    public NoAuthentication(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String authenticate(HttpServletRequest request, HttpServletResponse response) 
    throws AuthenticationException {
        return null;
    }
    public boolean authenticate(String user, byte[] credentials) throws AuthenticationException {
        return false;
    }
    public void logout(String userid, HttpServletRequest request, HttpServletResponse response) {
    }
    public void requestAuthentication(HttpServletRequest request, HttpServletResponse response)
    throws IOException {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, errorMessage);
    }
}
