/*******************************************************************************
 * Copyright (c) 2003-2018 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.jsf;

import java.net.HttpURLConnection;
import java.net.URL;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.validator.ValidatorException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.recaptcha.ReCaptchaSession;
import org.genepattern.server.webapp.LoginManager;

/**
 * Backing bean for creating a new user.
 * 
 * @see UserAccountManager#createUser(String, String, String)
 * 
 * @author jrobinso
 * 
 */
public class RegistrationBean {
    private static final Logger log = Logger.getLogger(RegistrationBean.class);

    private final GpConfig gpConfig;
    private String username;
    private String password = "";
    private String passwordConfirm;
    private String email;
    private String emailConfirm;
    private UIInput passwordConfirmComponent;
    private UIInput emailConfirmComponent;
    private boolean passwordRequired = true;
    private boolean joinMailingList = true;
    private final boolean showTermsOfService;
    private String termsOfService =
        "GenePattern Terms of Service\n"+
        "============================\n"+
        "\n"+
        "The hosted GenePattern server is provided free of charge.\n"+
        "We make no guarantees whatsoever.";
    private ReCaptchaSession recaptcha = null;

    public RegistrationBean() {
        this.gpConfig=ServerConfigurationFactory.instance();
        final GpContext serverContext=GpContext.getServerContext();
        this.passwordRequired=gpConfig.isPasswordRequired(serverContext);

        final boolean createAccountAllowed=gpConfig.isCreateAccountAllowed(serverContext);
        if (!createAccountAllowed) {
            log.info("Unauthorized attempt to create new user by " + UIBeanHelper.getRequest().getRemoteAddr() + ".");
            throw new SecurityException("Unauthorized attempt to create new user.");
        }

        //show registration agreement?
        this.showTermsOfService=gpConfig.getGPTrueProperty(serverContext, GpConfig.PROP_SHOW_TERMS_OF_SERVICE, false);

        //show reCAPTCHA?
        this.recaptcha=ReCaptchaSession.init(gpConfig, serverContext);
    }

    public String getEmail() {
    return email;
    }

    public String getEmailConfirm() {
    return emailConfirm;
    }

    public UIInput getEmailConfirmComponent() {
    return emailConfirmComponent;
    }

    public boolean isPasswordRequired() {
        return passwordRequired;
    }

    public String getPassword() {
    return password;
    }

    public String getPasswordConfirm() {
    return passwordConfirm;
    }

    public UIInput getPasswordConfirmComponent() {
    return passwordConfirmComponent;
    }

    public String getUsername() {
    return username;
    }

    public boolean isJoinMailingList() {
        return joinMailingList;
    }

    public void setJoinMailingList(boolean joinMailingList) {
        this.joinMailingList = joinMailingList;
    }
    
    /**
     * for client side reCAPTCHA, add a div to the registration form 
     * <pre>
       <div class="g-recaptcha" data-sitekey="#{registrationBean.recaptchaSiteKey}"></div>
     * </pre>
     */
    public String getRecaptchaSiteKey() {
        if (recaptcha != null) {
            return recaptcha.getSiteKey();
        }
        return "";
    }

    /**
     * verify server side reCAPTCHA
     */
    protected void verifyReCaptcha(final HttpServletRequest request) {
        if (recaptcha == null) {
            // short circuit
            return;
        }
        boolean success=false;
        String errorMessage=null;
        try {
            success=recaptcha.verifyReCaptcha(request);
        }
        catch (Throwable t) {
            errorMessage=t.getLocalizedMessage();
        }
        if (!success) {
            final String message=errorMessage!=null?errorMessage:"reCAPTCHA not verified";
            UIBeanHelper.setErrorMessage(message);
            FacesMessage facesMessage = new FacesMessage(message);
            throw new ValidatorException(facesMessage);
        }
    }

    /**
     * Register a new user. For now this uses an action listener since we are redirecting to a page outside of the JSF
     * framework. This should be changed to an action to use jsf navigation in the future.
     * 
     * @param event --
     *                ignored
     */
    private void registerUser(final ActionEvent event) {
        try {
            verifyReCaptcha(UIBeanHelper.getRequest());
            UserAccountManager.createUser(
                    gpConfig, HibernateUtil.instance(), 
                    username, password, email);
            LoginManager.instance().addUserIdToSession(UIBeanHelper.getRequest(), username);
            if (this.isJoinMailingList()){
                sendJoinMailingListRequest();
            }
            //redirect to main page
            HttpServletRequest request = UIBeanHelper.getRequest();
            HttpServletResponse response = UIBeanHelper.getResponse();
            String contextPath = request.getContextPath();
            response.sendRedirect( contextPath );
        }
        catch (Exception e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }
    

    public void sendJoinMailingListRequest() {
        final GpContext serverContext=GpContext.getServerContext();
        final String mailingListURL=gpConfig.getGPProperty(serverContext, 
            "gp.mailinglist.registration.url", 
            "http://www.broadinstitute.org/cgi-bin/cancer/software/genepattern/gp_mail_list.cgi"
        );
        StringBuffer buff = new StringBuffer(mailingListURL);
        buff.append("?choice=Add&email="+ this.getEmail()); 
        
        try {
            URL regUrl = new URL(buff.toString());
            regUrl.openConnection();
            
            HttpURLConnection conn = (HttpURLConnection)regUrl.openConnection();
            conn.getContent();
            int response = conn.getResponseCode();
            if (response < 300) {
                UIBeanHelper.setInfoMessage("You have successfully been registered in the GenePattern users mailing list.");
            } else{
                UIBeanHelper.setInfoMessage("Automatic registration in the GenePattern mailing list failed. You can do this manually from the Resources/Mailing list menu.");
            }
            
        } catch (Exception e){
         // do nothing.  we loose one registration   
            e.printStackTrace();
          
        }
    }
    
    public void setEmail(String email) {
    this.email = email;
    }

    public void setEmailConfirm(String emailConfirm) {
    this.emailConfirm = emailConfirm;
    }

    public void setEmailConfirmComponent(UIInput emailConfirmComponent) {
    this.emailConfirmComponent = emailConfirmComponent;
    }

    public void setPassword(String password) {
    this.password = password;
    }

    public void setPasswordConfirm(String passwordConfirm) {
    this.passwordConfirm = passwordConfirm;
    }

    public void setPasswordConfirmComponent(UIInput passwordConfirmComponent) {
    this.passwordConfirmComponent = passwordConfirmComponent;
    }

    public void setUsername(String username) {
    this.username = username;
    }

    public void validateEmail(FacesContext context, UIComponent component, Object value) throws ValidatorException {
    if (!value.equals(emailConfirmComponent.getSubmittedValue())) {
        String message = "Email entries do not match.";
        UIBeanHelper.setErrorMessage(message);
        FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
        ((UIInput) component).setValid(false);
        throw new ValidatorException(facesMessage);
    }
    }

    public void validateNewUsername(FacesContext context, UIComponent component, Object value)
    throws ValidatorException 
    {
        try {
            UserAccountManager.validateNewUsername(
                gpConfig, HibernateUtil.instance(), 
                value.toString());
        }
        catch (AuthenticationException e) {
            UIBeanHelper.setErrorMessage(e.getMessage());
            FacesMessage facesMessage = new FacesMessage(e.getLocalizedMessage());
            ((UIInput) component).setValid(false);
            throw new ValidatorException(facesMessage);
        }
    }

    public void validatePassword(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (!value.equals(passwordConfirmComponent.getSubmittedValue())) {
            String message = "Password entries do not match.";
            UIBeanHelper.setErrorMessage(message);
            FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
            ((UIInput) component).setValid(false);
            throw new ValidatorException(facesMessage);
        }
        }
    
    public boolean isShowTermsOfService() {
        return this.showTermsOfService;
    }
    
    public String getTermsOfService() {
        return this.termsOfService;
    }
    
    public boolean isRecaptchaEnabled() {
        if (recaptcha != null) {
            return recaptcha.isEnabled();
        }
        return false;
    }

}
