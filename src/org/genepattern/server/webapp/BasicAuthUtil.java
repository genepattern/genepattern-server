package org.genepattern.server.webapp;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.AuthenticationException;

/**
 * Utility methods for implementing HTTP Basic Authentication.
 * 
 * @author pcarr
 */
public class BasicAuthUtil {
    private static Logger log = Logger.getLogger(BasicAuthUtil.class);

    /**
     * Check the servlet request for an authenticated user.
     * First check for a gp userid from the session, then
     * do basic HTTP Authentication.
     * 
     * Need to add and remove userid from gp session as necessary because
     * this is invoked from both a Servlet and a Filter.
     * 
     * @param req
     * @param resp
     * @return a valid userid, or null if not authenticated.
     * @throws IOException
     */
    static public String getAuthenticatedUserId(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String userIdFromSession = LoginManager.instance().getUserIdFromSession(req);

        // Get Authorization header
        String userIdFromAuthorizationHeader = null;
        byte[] password = null;
        String auth = req.getHeader("Authorization");
        if (auth != null) {
            String[] up = getUsernamePassword(auth);
            userIdFromAuthorizationHeader = up[0];
            String passwordStr = up[1];
            password = passwordStr != null ? passwordStr.getBytes() : null;
        }

        //if the session is already authenticated
        if (userIdFromSession != null) {
            if (userIdFromAuthorizationHeader == null || userIdFromSession.equals(userIdFromAuthorizationHeader)) {
                return userIdFromSession;
            }
            //special-case when the userId from the session doesn't match the one in the authorization header
            LoginManager.instance().logout(req, resp, false);
            userIdFromSession = null;
        }

        //if we are here, check the authorization header ...
        try {
            boolean authenticated = UserAccountManager.instance().authenticateUser(userIdFromAuthorizationHeader, password);
            if (authenticated) {
                LoginManager.instance().addUserIdToSession(req, userIdFromAuthorizationHeader);
                return userIdFromAuthorizationHeader;
            }
        }
        catch (AuthenticationException e) {
        }

        //if we are here, it means we are not authenticated, return null
        return null;
    }
    
    /**
     * Parse out the username:password pair from the authorization header.
     * 
     * @param auth
     * @return <pre>new String[] {<username>, <password>};</pre>
     */
    static private String[] getUsernamePassword(String auth) {
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
     * @param response
     * @throws IOException
     */
    static public void requestAuthentication(HttpServletResponse response) throws IOException {
        final String realm = "GenePattern";
        response.setHeader("WWW-Authenticate", "BASIC realm=\""+realm+"\"");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

}
