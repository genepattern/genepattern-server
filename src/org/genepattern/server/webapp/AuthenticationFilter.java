/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2008) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.genepattern.server.EncryptionUtil;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.auth.DefaultGenePatternAuthentication;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.util.GPConstants;

/**
 * Servlet filter that requires user to log in to access certain pages
 *
 * @author Liefeld
 */
public class AuthenticationFilter implements Filter {
    private static Logger log = Logger.getLogger(AuthenticationFilter.class);

    private boolean redirectToFqHostName = false;
    private boolean passwordRequired;
    private String homePage;
    private String loginPage;

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
        String prop = props.getProperty("require.password", "false").toLowerCase();
        passwordRequired = (prop.equals("true") || prop.equals("y") || prop.equals("yes"));
        noAuthorizationRequiredPagesRedirect = csvToArray(filterconfig
                .getInitParameter("no.login.required.redirect.to.home"));
        noAuthorizationRequiredPages = csvToArray(filterconfig.getInitParameter("no.login.required"));
        homePage = filterconfig.getInitParameter("home.page").trim();
        loginPage = filterconfig.getInitParameter("login.page").trim();
        redirectToFqHostName = Boolean.valueOf(props.getProperty("redirect.to.fq.host", "true"));
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
        
        //if necessary, redirect to fully qualified host name so that only one cookie needs to be written
        if (redirectToFqHostName) { 
            String serverName = servletRequest.getServerName();
            if (!getFQHostName().equalsIgnoreCase(serverName)) {
                redirectToFullyQualifiedHostName(request, response);
                return;
            }
        }
        
        if (isAuthenticated(request)) {
            if (isRedirectRequired(request)) {
                //do redirect
                boolean origin = request.getParameter("origin") != null;
                if (!origin) {
                    response.sendRedirect(request.getContextPath() + homePage);
                } 
                else {
                    chain.doFilter(servletRequest, servletResponse);
                }
                return;
            }
            if (isChangePasswordRequired(request, response)) {
                response.sendRedirect(request.getContextPath() + "/pages/requireChangePassword.jsf");
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
            LoginManager.instance().login(request, response, false);
        }
        catch (AuthenticationException e) {
            //ignore
        }
        if (isAuthenticated(request)) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }
        else {
            //if authentication requires another step, redirect to a login page
            UserAccountManager.instance().getAuthentication().requestAuthentication(request, response);
            return;
        }
    }

    
    public void destroy() {
    }
    
    public void redirectToFullyQualifiedHostName(HttpServletRequest request, HttpServletResponse response) 
    throws IOException
    {
        String fqHostName = System.getProperty("fullyQualifiedHostName");
        if (fqHostName == null) {
            fqHostName = InetAddress.getLocalHost().getCanonicalHostName();
        }
        if (fqHostName.equals("localhost")) {
            fqHostName = "127.0.0.1";
        }
        String queryString = request.getQueryString();
        if (queryString == null) {
            queryString = "";
        } 
        else {
            queryString = "?" + queryString;
        }
        String portStr = "";
        int port = request.getServerPort();
        if (port > 0) {
            portStr = ":"+port;
        }
        String fqAddress = request.getScheme() + "://" + fqHostName + portStr + request.getRequestURI() + queryString;
        response.sendRedirect(fqAddress);
    }

    /**
     * get fully qualified host name from the machine. If this was set in system
     * properties (from the genepattern.properties file) use that since some
     * machines have multiple aliases
     */
    protected String getFQHostName() throws IOException {
        String fqHostName = System.getProperty("fqHostName");
        if (fqHostName == null) {
            fqHostName = InetAddress.getLocalHost().getCanonicalHostName();
        }
        if (fqHostName.equals("localhost")) {
            fqHostName = "127.0.0.1";
        }
        return fqHostName;
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
            //TODO: this is an artifact of gp-3.1 and earlier,
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
     * Special case when using default GenePattern authentication and the server configuration has changed from not requiring passwords to requiring passwords.
     * Any user account created before passwords were required will be automatically redirected to the change password page the next time they log in.
     * 
     * @param request
     * @param response
     * @return
     */
    protected boolean isChangePasswordRequired(HttpServletRequest request, HttpServletResponse response) {
        if (!(UserAccountManager.instance().getAuthentication() instanceof DefaultGenePatternAuthentication)) {
            //hack alert: special case should only apply for default genepattern authentication
            return false;
        }
        if (!passwordRequired) {
            return false;
        }
        
        if (request.getRequestURI().contains("requireChangePassword")) {
            return false;
        }
        
        String userId = LoginManager.instance().getUserIdFromSession(request);
        HttpSession session = request.getSession(false);
        if (session != null) {
            HibernateUtil.beginTransaction();
            User user = new UserDAO().findById(userId);
            HibernateUtil.commitTransaction();

            //check for users with empty passwords
            if (user != null && EncryptionUtil.isEmpty(user.getPassword())) {
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
