/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2008) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp.jsf;

import java.io.IOException;

import javax.faces.event.ActionEvent;

import org.apache.log4j.Logger;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.auth.DefaultGenePatternAuthentication;
import org.genepattern.server.auth.IAuthenticationPlugin;

/**
 * Backing bean for pages/login.
 * 
 * @author jrobinso
 * 
 */
public class LoginBean {
    private static Logger log = Logger.getLogger(LoginBean.class);
    private IAuthenticationPlugin authentication = null;

    private String username;
    private String password;
    private boolean passwordRequired;
    private boolean unknownUser = false;
    private boolean invalidPassword = false;
    private boolean createAccountAllowed;

    public LoginBean() {
        String prop = System.getProperty("require.password", "false").toLowerCase();
        passwordRequired = (prop.equals("true") || prop.equals("y") || prop.equals("yes"));

        String createAccountAllowedProp = System.getProperty("create.account.allowed", "true").toLowerCase();
        createAccountAllowed = 
            createAccountAllowedProp.equals("true") || 
            createAccountAllowedProp.equals("y") || 
            createAccountAllowedProp.equals("yes");
        
        authentication = new DefaultGenePatternAuthentication();
    }

    public String getPassword() {
        return this.password;
    }

    public String getUsername() {
        return username;
    }

    public boolean isCreateAccountAllowed() {
        return createAccountAllowed;
    }

    public boolean isInvalidPassword() {
        return invalidPassword;
    }

    public boolean isPasswordRequired() {
        return passwordRequired;
    }

    public boolean isUnknownUser() {
        return unknownUser;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Submit the user / password. For now this uses an action listener since we are redirecting to a page outside of
     * the JSF framework. This should be changed to an action to use jsf navigation in the future.
     * 
     * @param event --
     *                ignored
     */
    public void submitLogin(ActionEvent event) {
        if (username == null) {
            unknownUser = true;
            return;
        }

        byte[] credentials = password != null ? password.getBytes() : new byte[0];
        try {
            authentication.authenticate(username, credentials);
            UIBeanHelper.login(username);
        }
        catch (AuthenticationException e) {
            if (AuthenticationException.Type.INVALID_USERNAME.equals(e.getType())) {
                unknownUser = true;
            }
            if (AuthenticationException.Type.INVALID_CREDENTIALS.equals(e.getType())) {
                invalidPassword = true;
            }
            if (AuthenticationException.Type.SERVICE_NOT_AVAILABLE.equals(e.getType())) {
                log.error(e);
                throw new RuntimeException(e);
            }
        }
        catch (IOException e) {
            log.error(e);
            throw new RuntimeException(e); // @TODO -- wrap in gp system exception.
        }
    }

}
