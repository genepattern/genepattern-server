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
import java.io.IOException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.util.Properties;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.genepattern.server.util.AuthorizationManagerFactory;
import org.genepattern.server.util.IAuthorizationManager;
import org.genepattern.util.GPConstants;

/**
 * @author Liefeld
 *
 *
 */
public class AuthorizationFilter implements Filter {

    private IAuthorizationManager authManager = null;

    private boolean redirectToFqHostName;

    public void destroy() {
        authManager = null;
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        // String requestedURI = req.getRequestURI();

        String rh = req.getRemoteHost();
        String p = req.getParameter("jsp_precompile");
        int numParams = req.getParameterMap().keySet().size();

        // allow jsp precompilation
        if ((p != null) && ("localhost".equals(rh)) && (numParams == 1)) {
            chain.doFilter(request, response);
            return;
        }

        String userId = (String) request.getAttribute("userID");
        String uri = req.getRequestURI();
        int idx = uri.lastIndexOf("/");
        uri = uri.substring(idx + 1);

        // check permission
        boolean allowed = authManager.isAllowed(uri, userId);

        if (!allowed) { // not allowed to do this
            setNotPermittedPageRedirect((HttpServletRequest) request, (HttpServletResponse) response);
            return;
        } else { // looking for userID
            chain.doFilter(request, response);
            return;
        }
    }

    public void init(FilterConfig filterconfig) throws ServletException {
        try {
            String dir = filterconfig.getInitParameter("genepattern.properties");
            System.setProperty("genepattern.properties", dir);

            File propFile = new File(dir, "genepattern.properties");
            File customPropFile = new File(dir, "custom.properties");
            Properties props = new Properties();

            if (propFile.exists()) {
                AuthenticationFilter.loadProperties(props, propFile);
            }

            if (customPropFile.exists()) {
                AuthenticationFilter.loadProperties(props, customPropFile);
            }

            authManager = AuthorizationManagerFactory.getAuthorizationManager();
            redirectToFqHostName = Boolean.valueOf(props.getProperty("redirect.to.fq.host", "true"));

        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    public void setNotPermittedPageRedirect(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (response == null) {
            return;
        }

        String fqHostName = request.getServerName();
        if (redirectToFqHostName) {
            // redirect to the fully-qualified host name
            fqHostName = System.getProperty("fqHostName");
            if (fqHostName == null) {
                fqHostName = InetAddress.getLocalHost().getCanonicalHostName();
                if (fqHostName.equals("localhost")) {
                    fqHostName = "127.0.0.1";
                }
            }
        }

        String notPermittedUrl = "http://" + fqHostName + ":" + request.getServerPort() + request.getContextPath()
                + "/pages/notPermitted.jsf?link=" + URLEncoder.encode(request.getRequestURI(), GPConstants.UTF8);
        response.sendRedirect(notPermittedUrl);
    }

}