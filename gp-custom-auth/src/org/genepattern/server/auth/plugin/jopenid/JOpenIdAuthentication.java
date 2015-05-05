package org.genepattern.server.auth.plugin.jopenid;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.auth.DefaultGenePatternAuthentication;

/**
 * Example implementation of the IAuthenticationPlugin interface which adds OpenID (see http://openid.net/) support to GenePattern.
 * This example extends <code>org.genepattern.server.auth.DefaultGenePatternAuthentication</code>.
 * 
 * <b>Important note</b>: This is demo code which has only been tested and verified to work with Google accounts. 
 * You must make some edits (e.g. some URLs are hard coded) and do thorough testing before deploying in a production environment.
 * Additional coding and testing is required to integrate with other OpenID providers.
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
 * 
 * You also must make some edits to the default login form, login.xhtml, or create a new login form (e.g. loginOpenID.xhtml).
 * Add the following link to the input form.
 * <pre>
   OpenID
   <br /> 
   <h:outputLink
        value="#{facesContext.externalContext.requestContextPath}/openId?op=Google">Sign on using your Google account</h:outputLink>
   <br />
 * </pre>
 * 
 * @see JOpenIDAuthenticationServlet for details of OpenID integration

 * @author pcarr
 */
public class JOpenIdAuthentication extends DefaultGenePatternAuthentication { 
    /**
     * Handle call from the genepattern runtime and indicate whether or not the request is from a validated user.
     * The JOpenIdAuthenticationServlet is responsible for adding 'jopenid.userid' to the session so all we have
     * to do here is check for the existence of the session attribute.
     * 
     * @return the userid for an authenticated session or null if the session is not authenticated.
     */
    public String authenticate(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        String jopenidEmail = (String)request.getSession().getAttribute("jopenid.email");
        String jopenidIdentity = (String)request.getSession().getAttribute("jopenid.identity");
        if (jopenidEmail != null) {
            // The GenePattern login manager uses the 'email' and 'password' request attributes 
            // when creating new user accounts
            request.setAttribute("email", jopenidEmail);
            request.setAttribute("password", jopenidIdentity);
            return jopenidEmail;
        }
        
        // [optionally] use default authentication
        return super.authenticate(request, response);
    }

    /**
     * TODO implement OpenID authentication via SOAP interface!
     * Without this your server can't authenticate users who connect to GenePattern from the SOAP interface.
     */
    public boolean authenticate(String user, byte[] credentials) throws AuthenticationException {
        return super.authenticate(user, credentials);
    }

    public void logout(String userid, HttpServletRequest request, HttpServletResponse response) {
        request.getSession().removeAttribute("jopenid.userid");
        super.logout(userid, request, response);
    }

    /**
     * Link to "/gp/openId?op=Google" from the login page. For example, for JSF
     * <pre>
       <h:outputLink value="#{facesContext.externalContext.requestContextPath}/openId?op=Google">Sign on using your Google account</h:outputLink>
       </pre>
     *  
     * Option 1: use default login page, and edit 'login.jsf', you must edit login.xhtml
       <code>super.requestBasicAuth(request, response);</code>
     * 
     * Option 2: redirect to a custom page; make sure to add this page to the web application
       <code>
        response.sendRedirect("/gp/pages/loginOpenID.jsf");
       </code>
     */
    public void requestAuthentication(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // option 1: use default login page, and edit 'login.jsf', you must edit login.xhtml
        //super.requestBasicAuth(request, response);
        // option 2: redirect to a custom page; make sure to add this page to the web application
        response.sendRedirect("/gp/pages/loginOpenID.jsf");
    }
}