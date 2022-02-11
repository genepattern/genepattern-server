/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.ParameterStyle;
import org.apache.oltu.oauth2.rs.request.OAuthAccessResourceRequest;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;

/**
 * Utility methods for implementing HTTP Basic Authentication.
 *
 * @author pcarr
 */
public class AuthenticationUtil {
    private static Logger log = Logger.getLogger(AuthenticationUtil.class);

    /**
     * Check the servlet request for an authenticated user. 
     * This method returns quickly with the userid from the session if there is
     * already a logged in user.
     *
     * Next, it authenticates the GP account with the username/password credentials from the HTTP Basic Auth headers.
     * If the credentials don't match and existing GP account, try to authenticate to GenomeSpace.
     * For GenomeSpace, the credentials must match and there must be a linked GenePattern account.
     *
     * Need to add and remove userid from gp session as necessary because
     * this is invoked from both a Servlet and a Filter.
     *
     * @param req
     * @param resp
     * @return a valid userid, or null if not authenticated.
     * @throws AuthenticationException - indicating that the current request is not authorized.
     */
    static public String getAuthenticatedUserId(HttpServletRequest req, HttpServletResponse resp) throws AuthenticationException {
        String userIdFromSession = LoginManager.instance().getUserIdFromSession(req);
        boolean authenticated = false;
        String gpUserId = null;

        // Grab authorization header and determine Basic Auth, OAuth2 or neither
        String auth = req.getHeader("Authorization");
        boolean basicAuth = false;
        boolean oauth2 = false;
        if (auth != null) {
            basicAuth = auth.startsWith("Basic ");
            oauth2 = auth.startsWith("Bearer ");
        }

        // Basic auth style authentication parsing (needing for both basic auth and GenomeSpace)
        String userIdFromAuthorizationHeader = null;
        byte[] password = null;
        if (auth != null) {
            String[] up = getBasicAuthCredentials(auth);
            userIdFromAuthorizationHeader = up[0];
            String passwordStr = up[1];
            password = passwordStr != null ? passwordStr.getBytes() : null;
        }

        /*
         * First try Basic Auth
         */

        if (auth == null || basicAuth) {

            //if the session is already authenticated
            if (userIdFromSession != null) {
                if (userIdFromAuthorizationHeader == null || userIdFromSession.equals(userIdFromAuthorizationHeader)) {
                    return userIdFromSession;
                }
                
                //special-case when the userId from the session doesn't match the one in the authorization header
                try {
                    boolean redirect = false;
                    LoginManager.instance().logout(req, resp, redirect);
                } catch (IOException e) {
                    //ignoring IOException because the redirect arg is false
                    log.error("Unexpected IOException", e);
                }
                userIdFromSession = null;
            }

            //if we are here, check the authorization header ...
            log.debug("authenticating userIdFromAuthorizationHeader=" + userIdFromAuthorizationHeader);
            try {
                authenticated = UserAccountManager.instance().getAuthentication().authenticate(userIdFromAuthorizationHeader, password);
                if (authenticated) {
                    gpUserId = userIdFromAuthorizationHeader;
                }
            } catch (AuthenticationException ex) {
                //ignore it
                authenticated = false;
            }

        }

        /*
         * If not Basic Auth, try OAuth
         */
        if (!authenticated && oauth2) {
            try {
                OAuthAccessResourceRequest oauthRequest = new OAuthAccessResourceRequest(req, ParameterStyle.HEADER, ParameterStyle.QUERY);
                String accessToken = oauthRequest.getAccessToken();

                if (OAuthManager.instance().isTokenValid(accessToken)) {
                    String usernameFromToken = OAuthManager.instance().getUsernameFromToken(accessToken);
                    authenticated = true;
                    gpUserId = usernameFromToken;
                }

                if (authenticated) {
                    log.debug("gpUserId=" + gpUserId);
                    LoginManager.instance().addUserIdToSession(req, gpUserId);
                    return gpUserId;
                }
            } catch (OAuthSystemException e) {
                //ignore it
                authenticated = false;
            } catch (OAuthProblemException e) {
                //ignore it, probably simply lacks a token
                authenticated = false;
            }
        }

        /*
         * If other auth fails, try GenomeSpace auth
         */

        log.debug("authenticated="+authenticated);
        if (authenticated) {
            log.debug("gpUserId="+gpUserId);
            LoginManager.instance().addUserIdToSession(req, gpUserId);
            return gpUserId;
        }

        /*
         * If still not authenticated, return null
         */

        //if we are here, the user was not authenticated, an AuthenticationException should have been thrown
        log.debug("AuthenticationException was not thrown, returning null userId instead.");
        return null;
    }

   

    /**
     * Parse out the username:password pair from the authorization header.
     *
     * @param auth
     * @return <pre>new String[] {<username>, <password>};</pre>
     */
    static private String[] getBasicAuthCredentials(String auth) {
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
            log.error("Error decoding username and password from HTTP request header", e);
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

    /**
     * Call this method when the current request is not authenticated. 
     * Fail with a 401 status code (UNAUTHORIZED) 
     * and respond with the WWW-Authenticate header for this servlet.
     *
     * Note that this is the normal situation the first time you request a protected resource.
     * The client web browser will prompt for userID and password and cache them
     * so that it doesn't have to prompt you again.
     *
     * 1. request requires a valid username/password, but none has been provided.
     * 2a. request refused, even though a username/password was provided. Server says it's a bogus username/password pair.
     * 2b. request refused, Server says the user does not have permission to read the requested resource.
     *
     * @param response
     * @throws IOException
     */
    static public void requestBasicAuth(HttpServletRequest request, HttpServletResponse response) throws IOException {
        requestBasicAuth(response, "You must log in to view the page: " + request.getPathInfo());
    }

    static public void requestBasicAuth(HttpServletResponse response, String message) throws IOException {
        final String realm = "GenePattern";
        response.setHeader("WWW-Authenticate", "BASIC realm=\""+realm+"\"");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
    }

}
