/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
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
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.Properties;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.webapp.jsf.LoginBean;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.util.GPConstants;

/**
 * Servlet filter that requires user to log in to access certain pages
 *
 * @author Liefeld
 */
public class AuthenticationFilter implements Filter {

    private static Logger log = Logger.getLogger(AuthenticationFilter.class);

    private String[] noAuthorizationRequiredPages;

    private String homePage;

    private boolean passwordRequired;

    private String loginPage;

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String fqHostName = getFQHostName();
        String requestedURI = req.getRequestURI();

        // allow jsp precompilation
        if (isJspPrecompile(req)) {
            chain.doFilter(request, response);
            return;
        }

        // always use the fqHostName so that only one cookie needs to be written
        String serverName = request.getServerName();
        if (!fqHostName.equalsIgnoreCase(serverName)) {
            redirectToFullyQualifiedHostName((HttpServletRequest) request, (HttpServletResponse) response);
            return;
        }

        if (isAuthenticated((HttpServletRequest) request, (HttpServletResponse) response)) {
            for (int i = 0, length = noAuthorizationRequiredPages.length; i < length; i++) {
                if (requestedURI.contains(noAuthorizationRequiredPages[i]) && req.getParameter("origin") == null) {
                    ((HttpServletResponse) response).sendRedirect(req.getContextPath() + homePage);
                    return;
                } else if (requestedURI.contains(noAuthorizationRequiredPages[i]) && req.getParameter("origin") != null) {
                    chain.doFilter(request, response);
                    return;
                }
            }
            chain.doFilter(request, response);
        } else {
            // escape valve for some pages that do not require authentication
            for (int i = 0, length = noAuthorizationRequiredPages.length; i < length; i++) {
                if (requestedURI.contains(noAuthorizationRequiredPages[i])) {
                    chain.doFilter(request, response);
                    return;
                }
            }
            redirectToLoginPage((HttpServletRequest) request, (HttpServletResponse) response);
        }
    }

    /**
     * Authenticate the user by checking to see if the cookie userID is set
     */
    protected boolean isAuthenticated(HttpServletRequest request, HttpServletResponse response) {
        String userId = getUserId(request);
        if (userId != null && isSignedIn(userId, request, response)) {
            request.setAttribute(GPConstants.USERID, userId);
            request.setAttribute("userID", userId); // old jsp pages use this
            // attribute for usernames
            return true;
        }
        return false;

    }

    protected String getUserId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (GPConstants.USERID.equals(c.getName())) {
                    String userId = c.getValue();
                    return userId;
                }
            }
        }
        return null;
    }

    /**
     * Check to see if user is logged in and has registerd in database.
     *
     */
    private boolean isSignedIn(String userId, HttpServletRequest request, HttpServletResponse response) {
        if (!passwordRequired) {
            // don't check for valid session if no password is
            // required-GPConstants.USERID cookie is sufficient for
            // authentication

            HibernateUtil.beginTransaction();
            User user = new UserDAO().findById(userId);
            if (user == null) {
                LoginBean.createNewUserNoPassword(userId);
                try {
                    UIBeanHelper.login(userId, false, false, request, response);
                } catch (UnsupportedEncodingException e) {
                    log.error(e);
                } catch (IOException e) {
                    log.error(e);
                }
            }

            return true;
        } else {
            if (request.isRequestedSessionIdFromURL()) { // disallow passing
                // the
                // session id from the
                // URL, allow cookie
                // based sessions only
                return false;
            }

            HttpSession session = request.getSession(false);
            if (session != null) {
                HibernateUtil.beginTransaction();
                return new UserDAO().findById(userId) != null;
            }
            return false;
        }
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

    public void redirectToLoginPage(HttpServletRequest request, HttpServletResponse response) {
        String currentURL = request.getRequestURI();
        // get everything after the context root
        int firstSlash = currentURL.indexOf("/", 1); // jump past the
        // starting slash
        String targetURL = null;
        if (firstSlash != -1) {
            targetURL = currentURL.substring(firstSlash + 1, currentURL.length());
        }

        // redirect to the fully-qualified host name to make sure that the
        // cookie that we are allowed to write is useful
        try {
            String fqHostName = System.getProperty("fqHostName");
            if (fqHostName == null) {
                fqHostName = InetAddress.getLocalHost().getCanonicalHostName();
                if (fqHostName.equals("localhost")) {
                    fqHostName = "127.0.0.1";
                }
            }

            String contextPath = request.getContextPath();
            String basePath = request.getScheme() + "://" + fqHostName + ":" + request.getServerPort() + contextPath;
            String fullQualifiedLoginPage = basePath + loginPage;
            targetURL = basePath + targetURL;

            if (targetURL != null && !targetURL.contains(loginPage)) { // don't
                // redirect
                // back to
                // login
                // page
                request.getSession().setAttribute("origin", targetURL);
            }
            response.sendRedirect(fullQualifiedLoginPage);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void redirectToFullyQualifiedHostName(HttpServletRequest request, HttpServletResponse response) {
        try {
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
            } else {
                queryString = "?" + queryString;
            }
            String fqAddress = request.getScheme() + "://" + fqHostName + ":" + request.getServerPort()
                    + request.getRequestURI() + queryString;
            response.sendRedirect(fqAddress);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void destroy() {
    }

    private void loadProperties(Properties props, File propFile) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(propFile);
            props.load(fis);

        } catch (IOException e) {
            log.error(e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {

                }
            }
        }

    }

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
        noAuthorizationRequiredPages = filterconfig.getInitParameter("no.login.required").split(",");
        for (int i = 0; i < noAuthorizationRequiredPages.length; i++) {
            noAuthorizationRequiredPages[i] = noAuthorizationRequiredPages[i].trim();
        }

        homePage = filterconfig.getInitParameter("home.page").trim();
        loginPage = filterconfig.getInitParameter("login.page").trim();

    }

}