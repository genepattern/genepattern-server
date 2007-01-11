package org.genepattern.server.webapp.jsf;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import javax.faces.event.ActionEvent;
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
public class LoginBean {

    private static Logger log = Logger.getLogger(LoginBean.class);

    private String username;

    private String password;

    private boolean passwordRequired;

    private boolean unknownUser = false;

    private boolean invalidPassword = false;

    public LoginBean() {
        String prop = System.getProperty("require.password", "false").toLowerCase();
        passwordRequired = (prop.equals("true") || prop.equals("y") || prop.equals("yes"));
        String usernameInRequest = UIBeanHelper.getRequest().getParameter("username");

        if (usernameInRequest != null) {
            username = usernameInRequest;
            password = UIBeanHelper.getRequest().getParameter("password");
            submitLogin(null);
        }
    }

    public boolean isPasswordRequired() {
        return passwordRequired;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return this.password;
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

    public boolean isInvalidPassword() {
        return invalidPassword;
    }

    public static void createNewUserNoPassword(String username) {
        User newUser = new User();
        newUser.setUserId(username);
        try {
            newUser.setPassword(EncryptionUtil.encrypt(""));
        } catch (NoSuchAlgorithmException e) {
            log.error(e);
        }
        (new UserDAO()).save(newUser);
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
            User up = (new UserDAO()).findById(username);
            if (up == null) {
                if (passwordRequired) {
                    unknownUser = true;
                } else {
                    createNewUserNoPassword(username);
                    try {
                        UIBeanHelper.login(username, false);
                    } catch (UnsupportedEncodingException e) {
                        log.error(e);
                    } catch (IOException e) {
                        log.error(e);
                    }
                }
            } else if (passwordRequired) {
                if (!java.util.Arrays.equals(EncryptionUtil.encrypt(password), up.getPassword())) {
                    invalidPassword = true;
                } else {
                    UIBeanHelper.login(username, passwordRequired);
                }
            } else {
                UIBeanHelper.login(username, passwordRequired);
            }
        } catch (UnsupportedEncodingException e) {
            log.error(e);
            throw new RuntimeException(e); // @TODO -- wrap in gp system
            // exeception.
        } catch (IOException e) {
            log.error(e);
            throw new RuntimeException(e); // @TODO -- wrap in gp system
            // exeception.
        } catch (NoSuchAlgorithmException e) {
            log.error(e);
            throw new RuntimeException(e); // @TODO -- wrap in gp system
            // exeception.

        }
    }

    public String logout() {
        UIBeanHelper.logout();
        return "logout";
    }

}
