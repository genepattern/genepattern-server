package org.genepattern.server.webapp;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.auth.DefaultGenePatternAuthentication;
import org.genepattern.server.auth.IAuthenticationPlugin;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.util.GPConstants;

/**
 * Handles all aspects of authenticating users in the GenePattern web application,
 * from web browser and programmatic HTTP requests.
 * 
 * SOAP requests are handled by the AuthenticationHandler.
 * 
 * @author pcarr
 */
public class LoginManager {
    private IAuthenticationPlugin authentication = null;
    
    public LoginManager() {
        authentication = new DefaultGenePatternAuthentication();
    }

    public void loginFromWebClient(HttpServletRequest request) throws AuthenticationException {
        String gp_username = request.getParameter("username");
        String passwordString = request.getParameter("password");
        byte[] password = null;
        if (passwordString != null) {
            password = passwordString.getBytes();
        }

        boolean authenticated = authentication.authenticate(gp_username, password);
        if (!authenticated) {
            //TODO: log
            return;
        }

        //log user login stats
        User user = new UserDAO().findById(gp_username);
        user.incrementLoginCount();
        user.setLastLoginDate(new Date());
        user.setLastLoginIP(request.getRemoteAddr());

        //update the session
        request.getSession().setAttribute(GPConstants.USERID, user.getUserId());
    }
}
