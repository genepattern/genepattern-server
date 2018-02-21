/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
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
import org.genepattern.server.genomespace.GenomeSpaceException;
import org.genepattern.server.genomespace.GenomeSpaceLoginManager;
import org.genepattern.server.recaptcha.ReCaptchaException;
import org.genepattern.server.recaptcha.ReCaptchaUtil;
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

    private static Logger log = Logger.getLogger(RegistrationBean.class);
    private String username;
    private String password = "";
    private String passwordConfirm;
    private String email;
    private String emailConfirm;
    private UIInput passwordConfirmComponent;
    private UIInput emailConfirmComponent;
    private boolean passwordRequired = true;
    private boolean recaptchaEnabled = false;
    private String recaptchaSiteKey = "";
    private boolean joinMailingList = true;
    private boolean showTermsOfService = false;
    //private String pathToTerms = "gp/pages/terms.txt";
    private String termsOfService =
        "GenePattern Terms of Service\n"+
        "============================\n"+
        "\n"+
        "The hosted GenePattern server is provided free of charge.\n"+
        "We make no guarantees whatsoever.";
    

    public RegistrationBean() {
        String prop = System.getProperty("require.password", "false").toLowerCase();
        passwordRequired = (prop.equals("true") || prop.equals("y") || prop.equals("yes"));
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        final GpContext serverContext=GpContext.getServerContext();
        this.recaptchaEnabled=gpConfig.getGPBooleanProperty(serverContext, ReCaptchaUtil.PROP_ENABLED, false);
        this.recaptchaSiteKey=gpConfig.getGPProperty(serverContext, ReCaptchaUtil.PROP_SITE_KEY, "");

        String createAccountAllowedProp = System.getProperty("create.account.allowed", "true").toLowerCase();
        boolean createAccountAllowed = (
                createAccountAllowedProp.equals("true") || 
                createAccountAllowedProp.equals("y") || 
                createAccountAllowedProp.equals("yes"));
        if (!createAccountAllowed) {
            log.info("Unauthorized attempt to create new user by " + UIBeanHelper.getRequest().getRemoteAddr() + ".");
            throw new SecurityException("Unauthorized attempt to create new user.");
        }

        //show registration agreement?
        if (System.getProperty("show.terms.of.service", "false").equalsIgnoreCase("false")) {
            showTermsOfService = false;
        }
        else {
            showTermsOfService = true;
        }
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
     * 
     * <div class="g-recaptcha" data-sitekey="6LefkkcUAAAAAP3FxMP8iTWb0ZiTpDJ0xJff_8JZ"></div>
     * @param event
     */
    public String getRecaptchaSiteKey() {
        return this.recaptchaSiteKey;
    }

    private void registerUserSSO(ActionEvent event) {
        HttpServletRequest request = UIBeanHelper.getRequest();
        try {
            GenomeSpaceLoginManager.registerUser(request, username, password, email);
        }
        catch (GenomeSpaceException e) {
            UIBeanHelper.setErrorMessage("Invalid username or email.");
            return;
        }

        try {
            // Create the user
            //UserAccountManager.instance().createUser(username, password, email);
            //LoginManager.instance().addUserIdToSession(UIBeanHelper.getRequest(), username);

            // Handle the mailing list
            if (this.isJoinMailingList()){
                sendJoinMailingListRequest();
            }

            // Email confirmation message
            UIBeanHelper.setInfoMessage("A confirmation email has been sent to your email address. Please confirm your email and then log in.");

            // Redirect to main page
            HttpServletResponse response = UIBeanHelper.getResponse();
            response.sendRedirect("/gp/pages/login.jsf?emailConfirm=true");
        }
        catch (Exception e) {
            log.error(e);
            UIBeanHelper.setErrorMessage("");
            throw new RuntimeException(e);
        }
    }

    protected void validateReCaptcha(final HttpServletRequest request) {
        if (recaptchaEnabled) {
            final GpConfig gpConfig=ServerConfigurationFactory.instance();
            boolean success=false;
            String errorMessage=null;
            final ReCaptchaUtil r = ReCaptchaUtil.init(gpConfig);
            final String recaptchaResponse=request.getParameter(ReCaptchaUtil.G_RECAPTCHA_RESPONSE);
            try {
                success=r.verifyReCaptcha(recaptchaResponse);
            }
            catch (final ReCaptchaException e) {
                errorMessage=e.getLocalizedMessage();
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
    }

    private void registerUserDefault(ActionEvent event) {
        try {
            final GpConfig gpConfig=ServerConfigurationFactory.instance();
            validateReCaptcha(UIBeanHelper.getRequest());
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

    /**
     * Register a new user. For now this uses an action listener since we are redirecting to a page outside of the JSF
     * framework. This should be changed to an action to use jsf navigation in the future.
     * 
     * @param event --
     *                ignored
     */
    public void registerUser(ActionEvent event) {
        GpContext context = UIBeanHelper.getUserContext();
        boolean genepatternSSO = ServerConfigurationFactory.instance().getGPBooleanProperty(context, "ssoAuthentication", false);
        if (genepatternSSO) {
            registerUserSSO(event);
        }
        else {
            registerUserDefault(event);
        }
    }

    public void sendJoinMailingListRequest(){
        String mailingListURL = System.getProperty("gp.mailinglist.registration.url","http://www.broadinstitute.org/cgi-bin/cancer/software/genepattern/gp_mail_list.cgi");
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
            UserAccountManager.instance().validateNewUsername(value.toString());
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
    
    public void setShowTermsOfService(boolean b) {
        this.showTermsOfService = b;
    }
    
    public String getTermsOfService() {
        return this.termsOfService;
    }
    
    public boolean isRecaptchaEnabled() {
        return this.recaptchaEnabled;
    }

}
