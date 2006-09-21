/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp.jsf;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.servlet.http.Cookie;

import org.apache.log4j.Logger;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserHome;

public class ChangePasswordBean  {
    private static Logger log = Logger.getLogger(ChangePasswordBean.class);
    private String currentPassword;
    private String newPassword;
    private String confirmNewPassword;
    private UIInput passwordConfirmComponent;
    private boolean passwordSet;

    public ChangePasswordBean() {
        User user = (new UserHome()).findById(UIBeanHelper.getUserId());

        // password will be null if server was initially configured not to
        // require password
        if (user != null) { // FIXME
            passwordSet = user.getPassword() != null;
        }
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
        User user = (new UserHome()).findById(UIBeanHelper.getUserId());

       
        if (!value.toString().equals(user.getPassword())) {
            String message = "Please specify the correct current password.";
            FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
            ((UIInput) component).setValid(false);
            throw new ValidatorException(facesMessage);
        }
    }

    public String changePassword() {
        User user = (new UserHome()).findById(UIBeanHelper.getUserId());
        user.setPassword(newPassword);
        return "success";
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

    public boolean isPasswordSet() {
        return passwordSet;
    }

    public void setPasswordSet(boolean passwordSet) {
        this.passwordSet = passwordSet;
    }
}
