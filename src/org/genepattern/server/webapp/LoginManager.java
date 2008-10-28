package org.genepattern.server.webapp;

import java.io.IOException;
import java.util.Date;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
        String gp_username = request.getParameter("username");
        String passwordString = request.getParameter("password");
        login(request, response, gp_username, passwordString, redirect);
    }

    /**
     * Authenticate and then login.
     * 
     * @param request
     * @param response
     * @param gp_username
     * @param passwordString
     * @param redirect
     * @throws AuthenticationException
     * @throws IOException
     */
    public void login(HttpServletRequest request, HttpServletResponse response, String gp_username, String passwordString, boolean redirect) 
    throws AuthenticationException, IOException {
        byte[] password = null;
        if (passwordString != null) {
            password = passwordString.getBytes();
        }

        boolean authenticated = UserAccountManager.instance().getAuthentication().authenticate(gp_username, password);
        if (!authenticated) {
            return;
        }
        
        if (!UserAccountManager.instance().userExists(gp_username)) {
            //Automatically create a genepattern account the first time a user connects            
            //TODO: optionally plugin to external authentication system to get user's email address and add it to gp database
            //TODO: optionally create password to prevent inadvertent logon if the authentication system is not available
            UserAccountManager.instance().createUser(gp_username);
        }
        
        startUserSession(gp_username, request);
        if (redirect) {
            redirect(request, response);
        }
    }
    
    /**
     * Initiate a user session.
     * 
     * @param username
     * @param request
     */
    public void startUserSession(String username, HttpServletRequest request) {
        User user = new UserDAO().findById(username);
        if (user == null) {
            //TODO: log exception
            return;
        }

        user.incrementLoginCount();
        user.setLastLoginDate(new Date());
        user.setLastLoginIP(request.getRemoteAddr());
        
        HttpSession session = request.getSession();
        if (session == null) {
            //TODO: log exception
            return;
        }
        request.getSession().setAttribute(GPConstants.USERID, user.getUserId());
        request.getSession().setAttribute("userID", username); //TODO: replace all references to 'userID' with 'userid'
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
        HttpSession session = request.getSession();
        session.removeAttribute(GPConstants.USERID);
        session.invalidate();

        //redirect to main page
        String contextPath = request.getContextPath();
        response.sendRedirect( contextPath );
    }

}
