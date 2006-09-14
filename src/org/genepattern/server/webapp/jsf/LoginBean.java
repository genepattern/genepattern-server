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

    private boolean unknownUser = false;   
    private UIInput usernameComponent;
 
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

            UserPassword up = (new UserPasswordHome()).findByUsername(username);
            if (up == null) {
                unknownUser = true;
            }
            else {
                if (password.equals(up.getPassword())) {
                    setUserAndRedirect(request, getResponse(), username);
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

    public void validateLogin(FacesContext context, UIComponent component, Object value) {

        String pw = (String) value;
        String name = (String) usernameComponent.getValue();

        UserPassword up = (new UserPasswordHome()).findByUsername(name);
        if (up != null && pw != null && !pw.equals(up.getPassword())) {
            String message = "Invalid password";
            FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
            ((UIInput) component).setValid(false);
            throw new ValidatorException(facesMessage);
        }

    }


    public UIInput getUsernameComponent() {
        return usernameComponent;
    }

    public void setUsernameComponent(UIInput passwordComponent) {
        this.usernameComponent = passwordComponent;
    }

}
