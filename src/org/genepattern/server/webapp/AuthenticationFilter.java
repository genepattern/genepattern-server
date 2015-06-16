/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.util.GPConstants;

/**
 * Servlet filter that requires user to log in to access certain pages
 *
 * @author Liefeld
 */
public class AuthenticationFilter implements Filter {
    private static Logger log = Logger.getLogger(AuthenticationFilter.class);

    private String homePage = "/pages/index.jsf";

    /**
     * List of jsf pages that user can access if not logged in. If user requests
     * one of these pages while logged in, he goes to the requested page.
     */
    private String[] noAuthorizationRequiredPages;

    /**
     * List of jsf pages that user can access if not logged in. If user requests
     * one of these pages while logged in, he is forwarded to the home page.
     */
    private String[] noAuthorizationRequiredPagesRedirect;

    public void init(FilterConfig filterconfig) throws ServletException {
        String dir = filterconfig.getInitParameter("genepattern.properties");
        File propFile = new File(dir, "genepattern.properties");
        File customPropFile = new File(dir, "custom.properties");
        Properties props = new Properties();

        if (propFile.exists()) {
            loadProperties(props, propFile);
        }

        if (customPropFile.exists()) {
            loadProperties(props, customPropFile);
        }
        noAuthorizationRequiredPagesRedirect = csvToArray(filterconfig.getInitParameter("no.login.required.redirect.to.home"));
        noAuthorizationRequiredPages = csvToArray(filterconfig.getInitParameter("no.login.required"));
        homePage = filterconfig.getInitParameter("home.page").trim();
    }

    static void loadProperties(Properties props, File propFile) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(propFile);
            props.load(fis);
        } 
        catch (IOException e) {
            log.error(e);
        } 
        finally {
            if (fis != null) {
                try {
                    fis.close();
                } 
                catch (IOException e) {
                }
            }
        }
    }

    private static String[] csvToArray(String s) {
        if (s == null) {
            return new String[0];
        }
        String[] tokens = s.split(",");
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = tokens[i].trim();
        }
        return tokens;
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) 
    throws IOException, ServletException 
    {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        boolean redirectToOrigin = request.getParameter("origin") != null;

        if (log.isDebugEnabled()) {
            String method = request.getMethod();
            String requestURI = request.getRequestURI();
            log.debug(method + " " + requestURI);
        }
        
        if (isAuthenticated(request)) {
            if (isRedirectRequired(request)) {
                if (!redirectToOrigin) {
                    response.sendRedirect(request.getContextPath() + homePage);
                } 
                else {
                    chain.doFilter(servletRequest, servletResponse);
                }
                return;
            }
            chain.doFilter(servletRequest, servletResponse);
            return;
        }
        //else, not authenticated
        if (!isAuthenticationRequired(request)) {
            //no authentication required
            chain.doFilter(servletRequest, servletResponse);
            return;
        }
        //else, try to authenticate ...
        try {
            //The GenePattern user account is created in the login step if necessary
            LoginManager.instance().login(request, response, redirectToOrigin);
        }
        catch (AuthenticationException e) {
            //ignore
        }
        if (isAuthenticated(request)) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }
        else {
            //if we are here, request authentication

            //first, store the target URL as a session parameter
            String targetURL = request.getRequestURI();
            String queryString = request.getQueryString();
            if (queryString != null) {
                targetURL += "?" + queryString;
            }
            request.getSession().setAttribute("origin", targetURL);

            //second, request authentication, by default a redirect to the webapp's login page
            log.debug("requesting authentication for HTTP request");
            UserAccountManager.instance().getAuthentication().requestAuthentication(request, response);
        }
    }

    public void destroy() {
    }
    
    /**
     * Check for the 'userid' session variable.
     * 
     * @return true if the user has already been authenticated in this web session.
     */
    protected boolean isAuthenticated(HttpServletRequest request) {
        //Delegated to LoginManager because the same logic is applied from the LoginServlet.
        String userId = LoginManager.instance().getUserIdFromSession(request);

        if (userId != null) {
            //TODO: this is an artifact of gp-3.1.1 and earlier,
            //      which uses a request attribute to get the current user        
            request.setAttribute(GPConstants.USERID, userId);
            request.setAttribute("userID", userId); // old jsp pages use this
        }

        return userId != null;
    }

    /**
     * Does the requested resource require authentication?
     * 
     * @param request
     * @return
     */
    protected boolean isAuthenticationRequired(HttpServletRequest request) {
        String requestedURI = request.getRequestURI();
        for (int i = 0, length = noAuthorizationRequiredPages.length; i < length; i++) {
            if (requestedURI.contains(noAuthorizationRequiredPages[i])) {
                return false;
            }
        }
        for (int i = 0, length = noAuthorizationRequiredPagesRedirect.length; i < length; i++) {
            if (requestedURI.contains(noAuthorizationRequiredPagesRedirect[i])) {
                return false;
            }
        }
        // allow jsp precompilation
        if (isJspPrecompile(request)) {
            return false;
        }
        return true;
    }
    
    /**
     * Is a redirect required when the resource is requested by an authenticated user?
     */
    protected boolean isRedirectRequired(HttpServletRequest request) {
        String requestedURI = request.getRequestURI();
        for (int i = 0, length = noAuthorizationRequiredPagesRedirect.length; i < length; i++) {
            if (requestedURI.contains(noAuthorizationRequiredPagesRedirect[i])) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * check whether this is just the servlet engine precompiling jsp pages.
     * This must be a request coming from the localhost, with only the one
     * parameter set 'jsp_precompile'
     */
    protected boolean isJspPrecompile(HttpServletRequest request) {
        String rh = request.getRemoteHost();
        String p = request.getParameter("jsp_precompile");

        int numParams = request.getParameterMap().keySet().size();

        // allow jsp precompilation
        return ((p != null) && ("localhost".equals(rh)) && (numParams == 1));
    }
}
