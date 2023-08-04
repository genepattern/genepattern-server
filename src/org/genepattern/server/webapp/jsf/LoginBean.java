/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webapp.jsf;

import java.io.IOException;

import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.webapp.LoginManager;
import org.genepattern.server.webapp.rest.api.v1.oauth.OAuthConstants;

/**
 * Backing bean for pages/login.
 * 
 * @author jrobinso
 * 
 */
public class LoginBean {
    private static Logger log = Logger.getLogger(LoginBean.class);

    private String username;
    private String password;
    private boolean unknownUser = false;
    private boolean invalidPassword = false;
    private final boolean isShowRegistrationLink;
    private final boolean isPasswordRequired;
    private final boolean isCreateAccountAllowed;
    private final boolean isGlobusEnabled;
    private final boolean isAnonymousAllowed;

    public LoginBean() {
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        final GpContext serverContext=GpContext.getServerContext();
        this.isShowRegistrationLink=gpConfig.isShowRegistrationLink(serverContext);
        this.isPasswordRequired=gpConfig.isPasswordRequired(serverContext);
        this.isCreateAccountAllowed=gpConfig.isCreateAccountAllowed(serverContext);
        this.isAnonymousAllowed=gpConfig.isAnonymousAllowed(serverContext);
        
        String authClass = gpConfig.getGPProperty(serverContext, "authentication.class");
        if (authClass == null) {
            isGlobusEnabled = false;
        } else {
            // use just the class name to protect against package rearrangement in the future
            isGlobusEnabled = (authClass.endsWith("GlobusAuthentication"));
        }
    }

    public String getPassword() {
        return this.password;
    }

    public String getUsername() {
        return username;
    }

    public boolean isCreateAccountAllowed() {
        return isCreateAccountAllowed;
    }
    
    public boolean isShowRegistrationLink() {
        return isShowRegistrationLink;
    }

    public boolean isGlobusEnabled(){
         return isGlobusEnabled;
    }
    
    public boolean isInvalidPassword() {
        return invalidPassword;
    }

    public boolean isPasswordRequired() {
        return isPasswordRequired;
    }

    public boolean isAnonymousAllowed() {
        return isAnonymousAllowed;
    }
    
    public boolean isUnknownUser() {
        return unknownUser;
    }

    public boolean isEmailSent() {
        return UIBeanHelper.getRequest().getParameter("emailConfirm") != null;
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

        try {
            HttpServletRequest request = UIBeanHelper.getRequest();
            HttpServletResponse response = UIBeanHelper.getResponse();
            LoginManager.instance().login(request, response, true);
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
    
    /**
     * This method is synchronized to prevent two anonymous accounts from being created at the same moment
     * since the anonymous usernames append the {@link #anonymousLogin(ActionEvent)}of users to make them
     * unique (UUIDs were too long to display)
     * @param event
     */
    public synchronized void anonymousLogin(ActionEvent event) {
        
        try {
            HttpServletRequest request = UIBeanHelper.getRequest();
            HttpServletResponse response = UIBeanHelper.getResponse();
            LoginManager.instance().anonymousLogin(request, response, true);
        }
        catch (Exception e) {
           
                throw new RuntimeException(e);
            
        }
        
    }
    

}
