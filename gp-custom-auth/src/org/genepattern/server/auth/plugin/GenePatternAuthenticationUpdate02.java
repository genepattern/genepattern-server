package org.genepattern.server.auth.plugin;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.genepattern.server.EncryptionUtil;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.auth.IAuthenticationPlugin;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;

/**
 * Update the GenePattern Authentication (circa GP 3.2.0-build-8571) to fix a bug when authenticating
 * from the Word Add-In.
 * 
 * @author pcarr
 */
public class GenePatternAuthenticationUpdate02 implements IAuthenticationPlugin {
    private String loginPage = "/pages/login.jsf";
    private static final String changePasswordPage = "/pages/requireChangePassword.jsf";
    
    public GenePatternAuthenticationUpdate02() {
    }
    
    public void setLoginPage(String loginPage) {
        this.loginPage = loginPage;
    }
    
    public void requestAuthentication(HttpServletRequest request, HttpServletResponse response) 
    throws IOException
    {
        response.sendRedirect(request.getContextPath() + loginPage);        
    }

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
            //special case: when the server configuration changes from not-requiring to requiring passwords
            //   the first time a user logs in, redirect to the change password page
            if (isChangePasswordRequired(gp_username, request, response)) {
                String redirectTo = request.getContextPath() + changePasswordPage;
                request.getSession().setAttribute("origin", redirectTo);
            }
            return gp_username;
        }
        return null;
    }

    public boolean authenticate(String username, byte[] password) throws AuthenticationException {
        return UserAccountManager.instance().authenticateUser(username, password);
    }

    public void logout(String userid, HttpServletRequest request, HttpServletResponse response) {
        //ignore: no action required
    }
    
    /**
     * Special case when using default GenePattern authentication and the server configuration has changed from not requiring passwords to requiring passwords.
     * Any user account created before passwords were required will be automatically redirected to the change password page the next time they log in.
     * 
     * @param request
     * @param response
     * @return
     */
    private boolean isChangePasswordRequired(String userId, HttpServletRequest request, HttpServletResponse response) {
        if (!UserAccountManager.instance().isPasswordRequired()) {
            return false;
        }
        
        if (request.getRequestURI().contains("requireChangePassword")) {
            return false;
        }

        HibernateUtil.beginTransaction();
        User user = new UserDAO().findById(userId);
        HibernateUtil.commitTransaction();

        //check for users with empty passwords
        if (user != null && EncryptionUtil.isEmpty(user.getPassword())) {
            return true;
        }
        return false;
    }

}
