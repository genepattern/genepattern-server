/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.auth;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WorldAccessAuthentication implements IAuthenticationPlugin {

    public String authenticate(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {        
        String gp_username = "WORLD_" + System.currentTimeMillis();
        return gp_username;
    }

    public boolean authenticate(String user, byte[] credentials) throws AuthenticationException {
        // Don't allow authentication from SOAP client
        return false;
    }

    public void requestAuthentication(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // always automatically authenticate using 'world_<timestamp>'
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    public void logout(String userid, HttpServletRequest request, HttpServletResponse response) {
        // No logout steps necessary
    }
}
