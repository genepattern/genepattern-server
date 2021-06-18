package org.genepattern.server.webapp.rest.api.v1.oauth;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.auth.DefaultGenePatternAuthentication;

public class GlobusAuthentication extends DefaultGenePatternAuthentication {
    /**
     * Handle call from the genepattern runtime and indicate whether or not the request is from a validated user.
     * The GlobusOAuthCallbackServlet is responsible for adding 'globus.userid' to the session so all we have
     * to do here is check for the existence of the session attribute.
     * 
     * @return the userid for an authenticated session or null if the session is not authenticated.
     */
    public String authenticate(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        String globusEmail = (String)request.getSession().getAttribute("globus.email");
        String globusIdentity = (String)request.getSession().getAttribute("globus.identity");
        if (globusEmail != null) {
            // The GenePattern login manager uses the 'email' and 'password' request attributes 
            // when creating new user accounts
            request.setAttribute("email", globusEmail);
            request.setAttribute("password", globusIdentity);
            return globusEmail;
        }
        
        // [optionally] use default authentication
        return super.authenticate(request, response);
    }

    /**
     * TODO implement OpenID authentication via SOAP interface!
     * Without this your server can't authenticate users who connect to GenePattern from the SOAP interface.
     */
    public boolean authenticate(String user, byte[] credentials) throws AuthenticationException {
        return super.authenticate(user, credentials);
    }

    public void logout(String userid, HttpServletRequest request, HttpServletResponse response) {
        request.getSession().removeAttribute("globus.identity");
        request.getSession().removeAttribute("globus.email");
        request.getSession().removeAttribute("globus.access_token_json");
        super.logout(userid, request, response);
    }

    /**
     * redirect back to the login page
     */
    public void requestAuthentication(HttpServletRequest request, HttpServletResponse response) throws IOException {
        
        super.requestAuthentication(request, response);
    }
}
