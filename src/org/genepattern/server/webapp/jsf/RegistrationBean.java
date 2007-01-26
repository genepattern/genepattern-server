/**
 * 
 */
package org.genepattern.server.webapp.jsf;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.validator.ValidatorException;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;

/**
 * Backing bean for pages/login.
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

    // private UIInput passwordComponent;
    private UIInput passwordConfirmComponent;
    private UIInput emailConfirmComponent;

    public UIInput getPasswordConfirmComponent() {
        return passwordConfirmComponent;
    }

    public void setPasswordConfirmComponent(UIInput passwordConfirmComponent) {
        this.passwordConfirmComponent = passwordConfirmComponent;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmailConfirm() {
        return emailConfirm;
    }

    public void setEmailConfirm(String emailConfirm) {
        this.emailConfirm = emailConfirm;
    }

    public UIInput getEmailConfirmComponent() {
        return emailConfirmComponent;
    }

    public void setEmailConfirmComponent(UIInput emailConfirmComponent) {
        this.emailConfirmComponent = emailConfirmComponent;
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

    public void validateEmail(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (!value.equals(emailConfirmComponent.getSubmittedValue())) {
            String message = "Email entries do not match.";
            FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
            ((UIInput) component).setValid(false);
            throw new ValidatorException(facesMessage);
        }
    }

    /**
     * Register a new user. For now this uses an action listener since we are
     * redirecting to a page outside of the JSF framework. This should be
     * changed to an action to use jsf navigation in the future.
     * 
     * @param event --
     *            ignored
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
        }
        catch (Exception e) {
            log.error(e);
            throw new RuntimeException(e);
        }

    }

    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }
}
