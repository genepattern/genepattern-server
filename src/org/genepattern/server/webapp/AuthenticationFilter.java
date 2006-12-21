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

import java.io.IOException;
import java.net.InetAddress;

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

import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.util.GPConstants;

/**
 * Servlet filter that requires user to log in to access certain pages
 * 
 * @author Liefeld
 */
public class AuthenticationFilter implements Filter {

    private static final String[] NO_AUTH_REQUIRED_PAGES = { "getPipelineModel.jsp", "retrieveResults.jsp",
            "getFile.jsp", "getInputFile.jsp", "login.jsp", "login.jsf", "registerUser.jsf", "forgotPassword.jsf" };

    /** Forward to home page if logged in user requests these pages */
    private static final String[] LOGIN_PAGES = { "login.jsf", "registerUser.jsf", "forgotPassword.jsf" };

    private static final String HOME_PAGE = "/pages/index.jsf";

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
            for (int i = 0, length = LOGIN_PAGES.length; i < length; i++) {
                if (requestedURI.contains(LOGIN_PAGES[i])) {
                    ((HttpServletResponse) response).sendRedirect(req.getContextPath() + HOME_PAGE);
                    return;
                }
            }
            chain.doFilter(request, response);
        } else {
            // escape valve for some pages that do not require authentication
            for (int i = 0, length = NO_AUTH_REQUIRED_PAGES.length; i < length; i++) {
                if (requestedURI.contains(NO_AUTH_REQUIRED_PAGES[i])) {
                    chain.doFilter(request, response);
                    return;
                }
            }
            setLoginPageRedirect((HttpServletRequest) request, (HttpServletResponse) response);
        }
    }

    /**
     * Authenticate the user by checking to see if the cookie userID is set
     */
    protected boolean isAuthenticated(HttpServletRequest request, HttpServletResponse response) {
        String userId = getUserId(request);
        if (userId != null && isSignedIn(userId, request)) {
            request.setAttribute(GPConstants.USERID, userId);
            request.setAttribute("userID", userId); // old jsp pages use this attribute for usernames
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
     * Check to see if user has been registerd in database.
     * 
     */
    private boolean isSignedIn(String userId, HttpServletRequest request) {
        if (request.isRequestedSessionIdFromURL()) { // disallow passing the
            // session id from the
            // URL, allow cookie
            // based sessions only
            return false;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        // This filter might run before the HibernateFilter. Open transaction
        // just in case. The transaction is closed automatically.
        HibernateUtil.beginTransaction();
        User user = new UserDAO().findById(userId);
        if (user != null && session.getId().equals(user.getSessionId())) {
            return true;
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

    /**
     * get fully qualified host name from the machine. If this was set in system
     * properties (from the genepattern.properties file) use that since some
     * machines have multiple aliases
     */
    protected String getFQHostName() throws IOException {
        String fqHostName = System.getProperty("fullyQualifiedHostName");
        if (fqHostName == null) {
            fqHostName = InetAddress.getLocalHost().getCanonicalHostName();
        }
        if (fqHostName.equals("localhost")) {
            fqHostName = "127.0.0.1";
        }
        return fqHostName;
    }

    public void setLoginPageRedirect(HttpServletRequest request, HttpServletResponse response) {
        String currentURL = request.getRequestURI();
        // get everything after the context root
        int firstSlash = currentURL.indexOf("/", 1); // jump past the
        // starting slash
        String targetURL = null;
        if (firstSlash != -1) {
            targetURL = currentURL.substring(firstSlash + 1, currentURL.length());
        }

        // if (targetURL != null && request.getQueryString() != null) {
        // targetURL = targetURL + ("?" + request.getQueryString());
        // }

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
            if (contextPath != null && contextPath.charAt(contextPath.length() - 1) != '/') {
                contextPath += "/";
            }
            String basePath = request.getScheme() + "://" + fqHostName + ":" + request.getServerPort() + contextPath;
            String fqAddress = basePath + "pages/login.jsf";
            targetURL = basePath + targetURL;

            if (targetURL != null && !targetURL.contains("login.jsf")) { // don't
                // redirect
                // back to
                // login
                // page
                request.getSession().setAttribute("origin", targetURL);
            }
            response.sendRedirect(fqAddress);
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

    public void init(FilterConfig filterconfig) throws ServletException {
    }
}