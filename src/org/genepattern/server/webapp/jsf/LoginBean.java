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
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.server.UserPassword;
import org.genepattern.server.UserPasswordHome;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.util.GPConstants;

/**
 * Backing bean for pages/login.
 * 
 * @author jrobinso
 * 
 */
public class LoginBean extends AbstractUIBean {

    private static Logger log = Logger.getLogger(LoginBean.class);
    private String username;
    private String password;
    private String passwordConfirm;
    private String referrer;
    private boolean unknownUser = false;
    private boolean invalidPassword = false;
    private boolean usernameTaken = false;
    private boolean passwordMismatch = false;
    private UIInput passwordConfirmComponent;

    public UIInput getPasswordConfirmComponent() {
        return passwordConfirmComponent;
    }

    public void setPasswordConfirmComponent(UIInput passwordConfirmComponent) {
        this.passwordConfirmComponent = passwordConfirmComponent;
    }

    public boolean isPasswordMismatch() {
        return passwordMismatch;
    }

    public void setPasswordMismatch(boolean passwordMismatch) {
        this.passwordMismatch = passwordMismatch;
    }

    public boolean isUsernameTaken() {
        return usernameTaken;
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

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    public boolean isInvalidPassword() {
        return invalidPassword;
    }

    public boolean isUnknownUser() {
        return unknownUser;
    }

    /**
     * Submit the user / password. For now this uses an action listener since we
     * are redirecting to a page outside of the JSF framework. This should be
     * changed to an action to use jsf navigation in the future.
     * 
     * @param event --
     *            ignored
     */
    public void submitLogin(ActionEvent event) {

        try {
            assert username != null;
            assert password != null;
            HttpServletRequest request = getRequest();

            if (referrer == null || referrer.length() == 0) referrer = request.getContextPath() + "/index.jsp";

            UserPassword up = (new UserPasswordHome()).findByUsername(username);
            if (up == null) {
                unknownUser = true;
            }
            else {
                if (password.equals(up.getPassword())) {
                    setUserAndRedirect(request, getResponse());
                }
                else {
                    System.out.println("Invalid password");
                }

            }
        }
        catch (UnsupportedEncodingException e) {
            log.error(e);
            throw new RuntimeException(e); // @TODO -- wrap in gp system
            // exeception.
        }
        catch (IOException e) {
            log.error(e);
            throw new RuntimeException(e); // @TODO -- wrap in gp system
            // exeception.
        }

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

    public void validatePassword(FacesContext context, UIComponent component, Object value)
            throws ValidatorException {
        if (! value.equals(passwordConfirmComponent.getSubmittedValue())) {
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
            if (referrer == null || referrer.length() == 0) referrer = request.getContextPath() + "/index.jsp";

            UserPassword up = (new UserPasswordHome()).findByUsername(username);
            if (up != null) {
                usernameTaken = true;
            }
            else if (!password.equals(passwordConfirm)) {
                passwordMismatch = true;
            }
            else {
                UserPassword newUser = new UserPassword();
                newUser.setUsername(username);
                newUser.setPassword(password);
                (new UserPasswordHome()).persist(newUser);
                setUserAndRedirect(getRequest(), getResponse());
            }
        }
        catch (Exception e) {
            log.error(e);
            throw new RuntimeException(e);
        }

    }

    private void setUserAndRedirect(HttpServletRequest request, HttpServletResponse response)
            throws UnsupportedEncodingException, IOException {
        request.setAttribute("userID", username);

        String userID = "\"" + URLEncoder.encode(username.replaceAll("\"", "\\\""), "utf-8") + "\"";
        Cookie cookie4 = new Cookie(GPConstants.USERID, userID);
        cookie4.setPath(getRequest().getContextPath());
        cookie4.setMaxAge(Integer.MAX_VALUE);
        getResponse().addCookie(cookie4);

        referrer += (referrer.indexOf('?') > 0 ? "&" : "?");
        referrer += username;
        getResponse().sendRedirect(referrer);
    }

    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }

}
