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
import javax.servlet.http.*;

import org.apache.log4j.Logger;
import org.genepattern.server.UserPassword;
import org.genepattern.server.UserPasswordHome;

/**
 * Backing bean for pages/login.
 * 
 * @author jrobinso
 * 
 */
public class RegistrationBean extends AbstractUIBean {

    private static Logger log = Logger.getLogger(RegistrationBean.class);
    private String username;
    private String password;
    private String passwordConfirm;

    private UIInput passwordComponent;
    private UIInput passwordConfirmComponent;

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

    public void validateNewUsername(FacesContext context, UIComponent component, Object value)
            throws ValidatorException {
        UserPassword up = (new UserPasswordHome()).findByUsername(value.toString());
        if (up != null) {
            String message = "An account with this username already exist.  Please choose another.";
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

            HttpServletRequest request = getRequest();

            UserPassword newUser = new UserPassword();
            newUser.setUsername(username);
            newUser.setPassword(password);
            (new UserPasswordHome()).persist(newUser);
            setUserAndRedirect(getRequest(), getResponse(), username);

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

    public UIInput getPasswordComponent() {
        return passwordComponent;
    }

    public void setPasswordComponent(UIInput passwordComponent) {
        this.passwordComponent = passwordComponent;
    }

}
