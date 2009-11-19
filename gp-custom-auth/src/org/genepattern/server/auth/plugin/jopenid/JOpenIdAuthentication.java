package org.genepattern.server.auth.plugin.jopenid;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.auth.IAuthenticationPlugin;

/**
 * Example implementation of the IAuthenticationPlugin interface which adds OpenID (see http://openid.net/) support to GenePattern.
 * 
 * <b>Important note</b>: This is demo code. You must make some edits (e.g. some URLs are hard coded) and do thorough testing
 * before deploying in a production environment.
 *
 * This class uses JOpenID (http://code.google.com/p/jopenid/).
 * It requires you to install the JOpenIdAuthenticationServlet into the GenePattern web application.
 * Here are the example additions to the web.xml file:
 * <pre>
 <!--  add support for jopenid -->
 <servlet>
  <display-name>JOpenIdServlet</display-name>
  <servlet-name>JOpenIdServlet</servlet-name>
  <servlet-class>org.genepattern.server.auth.plugin.jopenid.JOpenIdAuthenticationServlet</servlet-class>
 </servlet>
 <servlet-mapping>
  <servlet-name>JOpenIdServlet</servlet-name>
  <url-pattern>/openId/*</url-pattern>
 </servlet-mapping>
 * </pre>
 * @author pcarr
 */
public class JOpenIdAuthentication implements IAuthenticationPlugin { 
    /**
     * Handle call from the genepattern runtime and indicate whether or not the request is from a validated user.
     * The JOpenIdAuthenticationServlet is responsible for adding 'jopenid.userid' to the session so all we have
     * to do here is check for the existence of the session attribute.
     * 
     * @return the userid for an authenticated session or null if the session is not authenticated.
     */
    public String authenticate(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        String jopenidUserid = (String)request.getSession().getAttribute("jopenid.userid");
        return jopenidUserid;
    }

    public boolean authenticate(String user, byte[] credentials) throws AuthenticationException {
        // TODO implement openid authentication via SOAP interface!
        //      without this your server can't authenticate users who connect to GenePattern from the SOAP interface
        return false;
    }

    public void logout(String userid, HttpServletRequest request, HttpServletResponse response) {
        request.getSession().removeAttribute("jopenid.userid");
        
        //TODO: figure out how to redirect back to GenePattern after logging out via redirect to google
        //    google accepts a 'continue' request parameter, but I am not sure what it does, 
        //        e.g.
        //        "http://www.google.com/accounts/Logout?continue=http%3A%2F%2Fwww.google.com%2F";
        //        "http://www.google.com/accounts/Logout?continue=http%3A/127.0.0.1%3A8080%2Fgp%2F";
        // replace ':' with '%3A'
        // replace '/' with '%2F'
        
        request.setAttribute("redirectTo", "http://www.google.com/accounts/Logout");
    }

    public void requestAuthentication(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect("/gp/openId?op=Google");
    }

}