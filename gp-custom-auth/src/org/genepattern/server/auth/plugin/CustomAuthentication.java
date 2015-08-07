/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.auth.plugin;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.auth.IAuthenticationPlugin;

/**
 * Demo Custom Authentication plugin.
 * 
 * @author pcarr
 */
public class CustomAuthentication implements IAuthenticationPlugin {
    public CustomAuthentication() {
    }
    
    /**
     * Redirect to the gp login page.
     */
    public void requestAuthentication(HttpServletRequest request, HttpServletResponse response) 
    throws IOException
    {
        response.sendRedirect(request.getContextPath() + "/pages/login.jsf");        
    }

    /**
     * Parse the http request for username and password pair.
     */
    public String authenticate(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        String gp_username = request.getParameter("username");
        String passwordString = request.getParameter("password");
        if (passwordString == null) {
            passwordString = request.getParameter("loginForm:password");
        }
        byte[] password = null;
        if (passwordString != null) {
            password = passwordString.getBytes();
        }
        boolean authenticated = authenticate(gp_username, password);
        if (authenticated) {
            return gp_username;
        }
        return null;
    }

    /**
     * Authenticate the username:password by lookup into the GP database.
     */
    public boolean authenticate(String username, byte[] password) throws AuthenticationException {
        return UserAccountManager.instance().authenticateUser(username, password);
    }

    public void logout(String userid, HttpServletRequest request, HttpServletResponse response) {
        //ignore: no action required
    }
}
