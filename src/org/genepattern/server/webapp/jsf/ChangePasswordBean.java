/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webapp.jsf;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.log4j.Logger;
import org.genepattern.server.EncryptionUtil;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;

public class ChangePasswordBean {
    private static Logger log = Logger.getLogger(ChangePasswordBean.class);

    private String currentPassword;

    private String newPassword;

    private String confirmNewPassword;

    private UIInput passwordConfirmComponent;

    public ChangePasswordBean() {

    }

    public void validatePassword(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (!value.equals(passwordConfirmComponent.getSubmittedValue())) {
            String message = "Your new password entries did not match.";
            FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
            ((UIInput) component).setValid(false);
            throw new ValidatorException(facesMessage);
        }
    }

    public void validateCurrentPassword(FacesContext context, UIComponent component, Object value)
            throws ValidatorException {
        User user = (new UserDAO()).findById(UIBeanHelper.getUserId());

        boolean correctPassword = false;
        try {
            correctPassword = Arrays.equals(EncryptionUtil.encrypt(value.toString()), (user.getPassword()));
        } catch (NoSuchAlgorithmException e) {
            log.error(e);
        }
        if (!correctPassword) {
            String message = "Please specify the correct current password.";
            FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
            ((UIInput) component).setValid(false);
            throw new ValidatorException(facesMessage);
        }
    }

    public String changePassword() {
        User user = (new UserDAO()).findById(UIBeanHelper.getUserId());
        try {
            user.setPassword(EncryptionUtil.encrypt(newPassword));
        } catch (NoSuchAlgorithmException e) {
            log.error(e);
            UIBeanHelper.setErrorMessage("An error occurred while saving your password");
            return "error";
        }
        String message = "Your new password has been saved";
        UIBeanHelper.setInfoMessage(message);
        return "my settings";
    }

    public UIInput getPasswordConfirmComponent() {
        return passwordConfirmComponent;
    }

    public void setPasswordConfirmComponent(UIInput passwordConfirmComponent) {
        this.passwordConfirmComponent = passwordConfirmComponent;
    }

    public String getConfirmNewPassword() {
        return confirmNewPassword;
    }

    public void setConfirmNewPassword(String confirmNewPassord) {
        this.confirmNewPassword = confirmNewPassord;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
