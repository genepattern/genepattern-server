/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import javax.faces.context.FacesContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.server.webservice.server.dao.UsageStatsDAO;
import org.genepattern.util.GPConstants;
import org.json.JSONArray;

import static org.genepattern.server.webapp.rest.api.v1.oauth.AuthResource.TOKEN_EXPIRY_TIME;

/**
 * User login and logout for the web application session.
 * SOAP requests are handled by the AuthenticationHandler.
 * 
 * @author pcarr
 */
public class LoginManager {
    private static Logger log = Logger.getLogger(LoginManager.class);

    private static LoginManager loginManager = null;

    private LoginManager() {
    }
    
    public static LoginManager instance() {
        if (loginManager == null) {
            loginManager = new LoginManager();
        }
        return loginManager;
    }
    
    public static String getReferrer(HttpServletRequest request) {
        String referrer = (String) request.getSession().getAttribute("origin");
        request.getSession().removeAttribute("origin");
        if (referrer == null || referrer.length() == 0) {
            referrer = request.getParameter("origin");
        }

        if (referrer == null || referrer.length() == 0) {
            referrer = request.getContextPath() + "/pages/index.jsf";
        }
        return referrer;
    }

    /**
     * Authenticate and then login. 
     * A new GenePattern user account is created during login the first time a userid is seen.
     * When creating a new account, the following optional request parameters are used,
     *     password and email.
     * 
     * @param request
     * @param response
     * @param redirect
     * @throws AuthenticationException
     * @throws IOException
     */
    public void login(HttpServletRequest request, HttpServletResponse response, boolean redirect) 
    throws AuthenticationException, IOException {
        String gp_username = null;

    
        log.debug("authenticating from HTTP request...");
        gp_username = UserAccountManager.instance().getAuthentication().authenticate(request, response);
        if (log.isDebugEnabled()) {
            if (gp_username == null) {
                log.debug("not authenticated (IAuthenticationPlugin.authenticate returned null)");
            }
            else {
                log.debug("authenticated user='"+gp_username+"'");
            }
        }
        
        if (gp_username == null) {
            return;
        }
        try {
        if (!UserAccountManager.instance().userExists(gp_username)) {
            String gp_email = (String) request.getAttribute("email"); //can be null
            String gp_password = (String) request.getAttribute("password"); //can be null
            UserAccountManager.instance().createUser(gp_username, gp_password, gp_email);
        }
        } catch (Exception e){
            e.printStackTrace();
            throw e;
        }
        addUserIdToSession(request, gp_username);
        attachAccessCookie(response, gp_username);
       
        if (redirect) {
            redirect(request, response);
        }
    }

    public void anonymousLogin(HttpServletRequest request, HttpServletResponse response, boolean redirect) 
    throws AuthenticationException, IOException, Exception {
      
       
        long numusers = UserAccountManager.getTotalUserCount();
        String gp_username = "guest_"+(numusers+1);
        String gp_email = gp_username+"@noreply.genepattern.org";
        String gp_password = UUID.randomUUID().toString(); // password is another random UUID
        try {
           
            UserAccountManager.instance().createUser(gp_username, gp_password, gp_email);
        } catch (Exception e){
            e.printStackTrace();
            throw e;
        }
        addUserIdToSession(request, gp_username);
        attachAccessCookie(response, gp_username);
       
        if (redirect) {
            redirect(request, response);
        }
    }
    
    
    public void attachAccessCookie(HttpServletResponse response, String username) {
        try {
            OAuthIssuer oauthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
            String token = oauthIssuerImpl.accessToken();
            OAuthManager.instance().createTokenSession(username, token, "GenePatternServer", OAuthManager.calcExpiry(TOKEN_EXPIRY_TIME));
            Cookie cookie = new Cookie("GenePatternAccess", token);
            cookie.setPath("/");
            response.addCookie(cookie);
        }
        catch (OAuthSystemException e) {
            // Something went wrong, don't attach the cookie
            log.error("Something went wrong generating access cookie token: " + e);
        }
    }
    
    
    public void addUserIdToSession(HttpServletRequest request, String gp_username) {
        HttpSession session = request.getSession();
        if (session == null) {
            log.error("LoginManager: unable to addUserIdToSession, session is null.");
            return;
        }
        session.setAttribute(GPConstants.USERID, gp_username);
        //TODO: replace all references to 'userID' with 'userid'
        session.setAttribute("userID", gp_username);

        //dynamically modify session timeout on login (units=seconds)
        int maxInactiveInterval = 14400; //default to 4hrs.
        //configurable in genepattern.properties
        String maxInactiveIntervalProp = System.getProperty("session.maxInactiveInterval");
        if (maxInactiveIntervalProp != null) {
            maxInactiveIntervalProp = maxInactiveIntervalProp.trim();
            try {
                maxInactiveInterval = Integer.parseInt(maxInactiveIntervalProp);
            }
            catch (NumberFormatException e) {
                log.error("Invalid value for property, 'session.maxInactiveInterval="+maxInactiveIntervalProp+"': "+e.getLocalizedMessage());
            }
        }
        session.setMaxInactiveInterval(maxInactiveInterval);

        logUserLogin(gp_username, request);
    }

    /**
     * Track of user login stats in the gp user database.
     * 
     * @param username
     * @param request
     */
    private void logUserLogin(String username, HttpServletRequest request) {
        User user = new UserDAO().findById(username);
        if (user == null) {
            log.error("LoginManager, unable to logUserLogin, user is null.");
            return;
        }

        user.incrementLoginCount();
        user.setLastLoginDate(new Date());
        String forwarded = request.getHeader("X-Forwarded-For");
        if ((forwarded == null) || (forwarded.length() == 0)) forwarded = request.getRemoteAddr();
        user.setLastLoginIP(forwarded);
    }
    
    public String getUserIdFromSession(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        HttpSession session = request.getSession();
        if (session == null) {
            return null;
        }
        return (String) session.getAttribute(GPConstants.USERID);
    }

    private void redirect(HttpServletRequest request, HttpServletResponse response) 
    throws IOException
    {
        String referrer = LoginManager.getReferrer(request);
        response.sendRedirect(referrer);
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc != null) {
            fc.responseComplete();
        }
    }
    
    public void logout(HttpServletRequest request, HttpServletResponse response) 
    throws IOException
    {
        logout(request, response, true);
    }
    
    public void logout(HttpServletRequest request, HttpServletResponse response, boolean redirect) 
    throws IOException
    {
        String userid = getUserIdFromSession(request);
        log.debug("logging out, userid="+userid);
        UserAccountManager.instance().getAuthentication().logout(userid, request, response);

        HttpSession session = request.getSession();
       
        if (session != null) {
            session.removeAttribute(GPConstants.USERID);
            session.removeAttribute( "userID");
            session.invalidate();
        }

        //redirect to a page which doesn't require authentication
        if (redirect) {
            String redirectTo = (String) request.getAttribute("redirectTo");
            if (redirectTo == null) {
                redirectTo = request.getContextPath() + "/";
            }
          
            response.sendRedirect( redirectTo );
        }
    }
    
}
