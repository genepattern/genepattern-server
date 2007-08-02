/**
 * 
 */
package org.genepattern.server.webapp.jsf;

import java.io.IOException;

import javax.faces.FacesException;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.validator.ValidatorException;

import org.apache.log4j.Logger;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;

/**
 * Backing bean for creating a new user.
 * 
 * @author jrobinso
 * 
 */
public class RegistrationBean {

    private static Logger log = Logger.getLogger(RegistrationBean.class);
    private String username;
    private String password;
    private String passwordConfirm;
    private String email;
    private String emailConfirm;
    private UIInput passwordConfirmComponent;
    private UIInput emailConfirmComponent;

    public RegistrationBean() {
	String createAccountAllowedProp = System.getProperty("create.account.allowed", "true").toLowerCase();
	boolean createAccountAllowed = (createAccountAllowedProp.equals("true") || createAccountAllowedProp.equals("y") || createAccountAllowedProp
		.equals("yes"));
	if (!createAccountAllowed) {
	    log.info("Unauthorized attempt to create new user by " + UIBeanHelper.getRequest().getRemoteAddr() + ".");
	    throw new SecurityException("Unauthorized attempt to create new user.");
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

    /**
     * Register a new user. For now this uses an action listener since we are redirecting to a page outside of the JSF
     * framework. This should be changed to an action to use jsf navigation in the future.
     * 
     * @param event --
     *                ignored
     */
    public void registerUser(ActionEvent event) {

	try {
	    assert username != null;
	    assert password != null;

	    User newUser = new User();
	    newUser.setUserId(username);
	    newUser.setEmail(email);
	    newUser.setPassword(EncryptionUtil.encrypt(password));
	    (new UserDAO()).save(newUser);
	    UIBeanHelper.login(username, true);
	} catch (Exception e) {
	    log.error(e);
	    throw new RuntimeException(e);
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
	    FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
	    ((UIInput) component).setValid(false);
	    throw new ValidatorException(facesMessage);
	}
    }

    public void validateNewUsername(FacesContext context, UIComponent component, Object value)
	    throws ValidatorException {
	User user = (new UserDAO()).findById(value.toString());
	if (user != null) {
	    String message = "An account with this username already exists.  Please choose another.";
	    FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
	    ((UIInput) component).setValid(false);
	    throw new ValidatorException(facesMessage);
	}
    }

    public void validatePassword(FacesContext context, UIComponent component, Object value) throws ValidatorException {
	if (!value.equals(passwordConfirmComponent.getSubmittedValue())) {
	    String message = "Password entries do not match.";
	    FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
	    ((UIInput) component).setValid(false);
	    throw new ValidatorException(facesMessage);
	}
    }
}
