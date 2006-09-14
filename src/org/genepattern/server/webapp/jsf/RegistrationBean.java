/**
 * 
 */
package org.genepattern.server.webapp.jsf;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.validator.ValidatorException;
import javax.servlet.http.*;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.genepattern.server.UserPassword;
import org.genepattern.server.UserPasswordHome;
import org.genepattern.util.GPConstants;

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
    private String email;
    private String emailConfirm;

//    private UIInput passwordComponent;
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

            HttpServletRequest request = getRequest();
            
            Base64 encoder = new Base64();
            String encodedPassword = new String( encoder.encode(password.getBytes()));
            
            
            UserPassword newUser = new UserPassword();
            newUser.setUsername(username);
            newUser.setPassword(encodedPassword);
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

/*    public UIInput getPasswordComponent() {
        return passwordComponent;
    }

    public void setPasswordComponent(UIInput passwordComponent) {
        this.passwordComponent = passwordComponent;
    }
*/
    
    protected String getReferrer(HttpServletRequest request) {
        String referrer = request.getParameter("referrer");
        if (referrer == null || referrer.length() == 0) {
            referrer = request.getContextPath() + "/index.jsp";
        }
        return referrer;
    }

    protected void setUserAndRedirect(HttpServletRequest request, HttpServletResponse response, String username) throws UnsupportedEncodingException, IOException {
        request.setAttribute("userID", username);
    
        String userID = "\"" + URLEncoder.encode(username.replaceAll("\"", "\\\""), "utf-8") + "\"";
        Cookie cookie4 = new Cookie(GPConstants.USERID, userID);
        cookie4.setPath(getRequest().getContextPath());
        cookie4.setMaxAge(Integer.MAX_VALUE);
        getResponse().addCookie(cookie4);
    
        String referrer = getReferrer(request);
        referrer += (referrer.indexOf('?') > 0 ? "&" : "?");
        referrer += username;
        getResponse().sendRedirect(referrer);
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
}
