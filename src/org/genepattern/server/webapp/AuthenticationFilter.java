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

import org.genepattern.util.IGPConstants;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Servlet filter that requires user to log in to access certain pages
 *
 * @author Liefeld
 */
public class AuthenticationFilter implements Filter, IGPConstants {
    private FilterConfig filterConfig;

    private static final String[] NO_LOGIN_REQUIRED_PAGES = {"retrieveResults.jsp", "getFile.jsp", "getInputFile.jsp"};

    private static final String LOGIN_PAGE = "login.jsp";

    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
    }

    public void destroy() {
        this.filterConfig = null;
    }


    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String requestedURI = req.getRequestURI();
        String rh = req.getRemoteHost();
        String p = req.getParameter("jsp_precompile");
        int numParams = req.getParameterMap().keySet().size();

        // allow jsp precompilation
        if ((p != null) && ("localhost".equals(rh)) && (numParams == 1)) {
            chain.doFilter(request, response);
            return;
        }
        String fqHostName = System.getProperty("fullyQualifiedHostName");
        if (fqHostName == null) {
            fqHostName = InetAddress.getLocalHost().getCanonicalHostName();
        }
        if (fqHostName.equals("localhost")) {
            fqHostName = "127.0.0.1";
        }
        String serverName = request.getServerName();
        if (!fqHostName.equalsIgnoreCase(serverName)) {
            redirectToFullyQualifiedHostName((HttpServletRequest) request, (HttpServletResponse) response);
            return;
        }
        boolean isLogin = requestedURI.indexOf(LOGIN_PAGE) >= 0;
        boolean isResultFetch = false;
        for (int i = 0, length = NO_LOGIN_REQUIRED_PAGES.length; i < length; i++) {
            if (requestedURI.indexOf(NO_LOGIN_REQUIRED_PAGES[i]) >= 0) {
                isResultFetch = true;
                break;
            }
        }
        if (!(isLogin || isResultFetch)) {
            String userId = _getUserID((HttpServletRequest) request);
            if (userId == null) {
                setLoginPageRedirect((HttpServletRequest) request, (HttpServletResponse) response);
                return;
            }
            request.setAttribute("userID", userId);
            chain.doFilter(request, response);
        } else { // looking for userID
            chain.doFilter(request, response);
            return;
        }
    }


    public String _getUserID(HttpServletRequest request) {
        String userID = null;
        if (request.getAttribute(USER_LOGGED_OFF) != null) {
            return userID;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals(USERID)) {
                    userID = cookies[i].getValue();
                    if (userID.length() > 0) {
                        break;
                    }
                }
            }
        }
        if ((userID == null || userID.length() == 0) && request.getParameter(USERID) != null) {
            userID = request.getParameter(USERID);
        }
        if (userID != null) {
            // strip surrounding quotes, if they exist
            if (userID.startsWith("\"")) {
                userID = userID.substring(1, userID.length() - 1);
                try {
                    userID = URLDecoder.decode(userID, UTF8);
                } catch (UnsupportedEncodingException uee) { /* ignore */
                }
            }
            if (userID.length() == 0) {
                userID = null;
            }
        }
        return userID;
    }


    public void setLoginPageRedirect(HttpServletRequest request, HttpServletResponse response) {
        String URL = request.getRequestURI();
        if (response == null) {
            return;
        }

        // redirect to the fully-qualified host name to make sure that the
        // one cookie that we are allowed to write is useful
        try {
            String fqHostName = System.getProperty("fqHostName");
            if (fqHostName == null) {
                fqHostName = InetAddress.getLocalHost().getCanonicalHostName();
                if (fqHostName.equals("localhost")) {
                    fqHostName = "127.0.0.1";
                }
            }
            String serverName = request.getServerName();
            if (request.getQueryString() != null) {
                URL = URL + ("?" + request.getQueryString());
            }
            String fqAddress = request.getScheme() + "://" + fqHostName + ":" + request.getServerPort() + "/" +
                    request.getContextPath() + "/login.jsp?origin=" + URLEncoder.encode(URL, UTF8);
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
            String fqAddress = request.getScheme() + "://" + fqHostName + ":" + request.getServerPort() +
                    request.getRequestURI() + queryString;
            response.sendRedirect(fqAddress);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}