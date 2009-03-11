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
            //TODO: optionally plug in to external authentication system to get user's email address and add it to gp database
            UserAccountManager.instance().createUser(gp_username);
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
        this.logout(request, response, true);
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
            String contextPath = request.getContextPath();
            response.sendRedirect( contextPath + "/" );
        }
    }
    
}
