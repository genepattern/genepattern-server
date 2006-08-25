package org.genepattern.server.webapp.jsf;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.util.GPConstants;

/**
 * Backing bean for pages/login.
 * 
 * @author jrobinso
 * 
 */
public class LoginBean extends BackingBeanBase {

    private static Logger log = Logger.getLogger(LoginBean.class);
    private String username;
    private String password;
    private String referrer;

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
            HttpServletRequest request = getRequest();
            HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext()
                    .getResponse();

            if (referrer == null || referrer.length() == 0) referrer = request.getContextPath() + "/index.jsp";

            if (username != null && username.length() > 0) {

                request.setAttribute("userID", username);

                String userID = "\"" + URLEncoder.encode(username.replaceAll("\"", "\\\""), "utf-8") + "\"";
                addUserIDCookies(response, request, userID);
                referrer += (referrer.indexOf('?') > 0 ? "&" : "?");
                referrer += username;
                response.sendRedirect(referrer);

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

    /**
     * Validate the username and password.
     * 
     * @TODO -- implementation, for now accepts anything
     * @return
     */
    private boolean validateUser() {
        return (username != null && username.length() > 0 && password != null && password.length() > 0);
    }

    private void addUserIDCookies(HttpServletResponse response, HttpServletRequest request, String userID) {
        // no explicit domain
        Cookie cookie4 = new Cookie(GPConstants.USERID, userID);
        cookie4.setPath(request.getContextPath());
        cookie4.setMaxAge(Integer.MAX_VALUE);
        response.addCookie(cookie4);
    }

}
