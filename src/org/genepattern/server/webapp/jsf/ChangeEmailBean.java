/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webapp.jsf;

import java.security.NoSuchAlgorithmException;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.log4j.Logger;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;

public class ChangeEmailBean {
    private static Logger log = Logger.getLogger(ChangeEmailBean.class);

    private String currentEmail;

    private String newEmail;

    private String confirmNewEmail;

    private UIInput emailConfirmComponent;

    public ChangeEmailBean() {
	User user = (new UserDAO()).findById(UIBeanHelper.getUserId());
	assert user != null;
	// email will be null if server was initially configured not to
	// require login
	currentEmail = user.getEmail();
	if (currentEmail == null || "".equals(currentEmail)) {
	    currentEmail = "No email set";
	}

    }

    public void validateEmail(FacesContext context, UIComponent component, Object value) throws ValidatorException {
	if (!value.equals(emailConfirmComponent.getSubmittedValue())) {
	    String message = "Your new email entries did not match.";
	    FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
	    ((UIInput) component).setValid(false);
	    throw new ValidatorException(facesMessage);
	}
    }

    public String changeEmail() {
	User user = (new UserDAO()).findById(UIBeanHelper.getUserId());
	user.setEmail(newEmail);
	currentEmail = newEmail;
	String message = "Your new email has been saved";
	UIBeanHelper.setInfoMessage(message);
	return "my settings";
    }

    public UIInput getEmailConfirmComponent() {
	return emailConfirmComponent;
    }

    public void setEmailConfirmComponent(UIInput emailConfirmComponent) {
	this.emailConfirmComponent = emailConfirmComponent;
    }

    public String getConfirmNewEmail() {
	return confirmNewEmail;
    }

    public void setConfirmNewEmail(String confirmNewPassord) {
	this.confirmNewEmail = confirmNewPassord;
    }

    public String getCurrentEmail() {
	return currentEmail;
    }

    public void setCurrentEmail(String currentEmail) {
	this.currentEmail = currentEmail;
    }

    public String getNewEmail() {
	return newEmail;
    }

    public void setNewEmail(String newEmail) {
	this.newEmail = newEmail;
    }

}
