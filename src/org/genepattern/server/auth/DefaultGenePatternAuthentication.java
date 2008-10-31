package org.genepattern.server.auth;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.genepattern.server.UserAccountManager;

/**
 * Default GenePattern Authentication which authenticates using the GenePattern database.
 * The input credentials are not encrypted.
 * 
 * @author pcarr
 */
public class DefaultGenePatternAuthentication implements IAuthenticationPlugin {
    private String loginPage = "/pages/login.jsf";
    
    public DefaultGenePatternAuthentication() {
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
        byte[] password = null;
        if (passwordString != null) {
            password = passwordString.getBytes();
        }
        boolean authenticated = authenticate(gp_username, password);
        if (authenticated) {
            //addUserIdToSession(request, gp_username);
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
}
