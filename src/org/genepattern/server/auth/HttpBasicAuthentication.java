/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.auth;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.genepattern.server.UserAccountManager;

/**
 * Configure GenePattern to use HTTP Basic Authentication.
 * 
 * Note special case so that logging out forces the client to re-authenticate 
 * (even if the web browser hasn't been shut down).
 * 
 * @author pcarr
 */
public class HttpBasicAuthentication implements IAuthenticationPlugin {
    private Set<String> loggingInSessions = new HashSet<String>();

    public String authenticate(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        if (!getIsLoggingIn(request)) {
            return null;
        }
        
        String authorizedUserId = allowUser(request);
        if (authorizedUserId != null) {
            setIsLoggingIn(request, false);
        }
        return authorizedUserId;
    }

    public boolean authenticate(String username, byte[] credentials) throws AuthenticationException {
        return UserAccountManager.instance().authenticateUser(username, credentials);
    }

    public void requestAuthentication(HttpServletRequest request, HttpServletResponse response) throws IOException {
        //track sessions here: 
        setIsLoggingIn(request, true);
        
        String realm = "GenePattern";
        response.setHeader("WWW-Authenticate", "BASIC realm=\""+realm+"\"");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    public void logout(String userid, HttpServletRequest request, HttpServletResponse response) {
        // no special processing is required on logout
    }

    /**
     * Flag the session associated with this request as requiring parsing login credentials.
     * 
     * @param request
     * @param add, if true add the session to the list of logging in sessions, otherwise remove from the list
     * @return
     */
    private void setIsLoggingIn(HttpServletRequest request, boolean add) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String sessionId = session.getId();
            if (add) {
                loggingInSessions.add(sessionId);
            }
            else {
                loggingInSessions.remove(sessionId);
            }
        }
    }
    
    private boolean getIsLoggingIn(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String id = session.getId();
            return loggingInSessions.contains(id);
        }
        return false;
    }
 
    /**
     * Authenticate the username:password pair from the request header.
     * 
     * @param request
     * @return
     */
    private String allowUser(HttpServletRequest request) {
        // Get Authorization header
        String auth = request.getHeader("Authorization");
        String[] up = getUsernamePassword(auth);
        String username = up[0];
        String passwordStr = up[1];
        byte[] password = passwordStr != null ? passwordStr.getBytes() : null;
        try {
            boolean allow = UserAccountManager.instance().authenticateUser(username, password);
            if (allow) {
                return username;
            }
            else {
                return null;
            }
        }
        catch (AuthenticationException e) {
            return null;
        }        
    }

    /**
     * Parse out the username:password pair from the authorization header.
     * 
     * @param auth
     * @return <pre>new String[] {<username>, <password>};</pre>
     */
    private String[] getUsernamePassword(String auth) {
        String[] up = new String[2];
        up[0] = null;
        up[1] = null;
        if (auth == null) {
            return up;
        }

        if (!auth.toUpperCase().startsWith("BASIC "))  {
            return up;
        }

        // Get encoded user and password, comes after "BASIC "
        String userpassEncoded = auth.substring(6);

        // Decode it, using any base 64 decoder
        sun.misc.BASE64Decoder dec = new sun.misc.BASE64Decoder();
        String userpassDecoded = null;
        try {
            userpassDecoded = new String(dec.decodeBuffer(userpassEncoded));
        }
        catch (IOException e) {
            //TODO: log error
            return up;
        }
        String username = "";
        String passwordStr = null;
        int idx = userpassDecoded.indexOf(":");
        if (idx >= 0) {
            username = userpassDecoded.substring(0, idx);
            passwordStr = userpassDecoded.substring(idx+1);
        }
        up[0] = username;
        up[1] = passwordStr;
        return up;
    }

}
