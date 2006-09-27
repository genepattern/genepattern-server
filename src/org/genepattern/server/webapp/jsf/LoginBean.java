package org.genepattern.server.webapp.jsf;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.faces.event.ActionEvent;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserHome;
import org.genepattern.util.GPConstants;

/**
 * Backing bean for pages/login.
 * 
 * @author jrobinso
 * 
 */
public class LoginBean  {

    private static Logger log = Logger.getLogger(LoginBean.class);
    private String username;
    private String password;
    private boolean passwordRequired;

    private boolean unknownUser = false;
    private boolean invalidPassword = false;

    public LoginBean() {
        String prop = System.getProperty("require.password", "false").toLowerCase();
        passwordRequired = (prop.equals("true") || prop.equals("y") || prop.equals("yes"));
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
            HttpServletRequest request = UIBeanHelper.getRequest();

            User up = (new UserHome()).findById(username);
            if (up == null) {
                if (passwordRequired) {
                    unknownUser = true;
                }
                else {
                    User newUser = new User();
                    newUser.setUserId(username);
                    newUser.setPassword(null);

                    (new UserHome()).merge(newUser);
                    UIBeanHelper.setUserAndRedirect(request, UIBeanHelper.getResponse(), username);

                }
            }
            else if (passwordRequired) {
                Base64 decoder = new Base64();
                String actualPassword = new String(decoder.decode(up.getPassword().getBytes()));

                if (!actualPassword.equals(password)) {
                    invalidPassword = true;
                }

                else {
                    UIBeanHelper.setUserAndRedirect(request, UIBeanHelper.getResponse(), username);
                }
            }
            else {
                UIBeanHelper.setUserAndRedirect(request, UIBeanHelper.getResponse(), username);
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



}
