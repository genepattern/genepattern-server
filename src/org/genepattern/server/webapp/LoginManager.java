package org.genepattern.server.webapp;

import java.io.IOException;
import java.util.Date;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.util.GPConstants;

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
        String gp_username = UserAccountManager.instance().getAuthentication().authenticate(request, response);
        if (gp_username == null) {
            return;
        }
        
        if (!UserAccountManager.instance().userExists(gp_username)) {
            String gp_email = request.getParameter("email"); //can be null
            String gp_password = request.getParameter("password"); //can be null
            UserAccountManager.instance().createUser(gp_username, gp_password, gp_email);
        }

        addUserIdToSession(request, gp_username);

        if (redirect) {
            redirect(request, response);
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

        //dynamically modify session timeout on login
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
        user.setLastLoginIP(request.getRemoteAddr());
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
        String referrer = UIBeanHelper.getReferrer(request);
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
        UserAccountManager.instance().getAuthentication().logout(userid, request, response);

        HttpSession session = request.getSession();
        if (session != null) {
            session.removeAttribute(GPConstants.USERID);
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
