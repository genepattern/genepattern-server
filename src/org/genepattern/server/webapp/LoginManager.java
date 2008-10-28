package org.genepattern.server.webapp;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.AuthenticationException;
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

    public void login(HttpServletRequest request, HttpServletResponse response, boolean redirect) 
    throws AuthenticationException, IOException {
        String gp_username = request.getParameter("username");
        String passwordString = request.getParameter("password");
        login(request, response, gp_username, passwordString, redirect);
    }

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
        
        UIBeanHelper.login(gp_username, redirect, request, response);
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
