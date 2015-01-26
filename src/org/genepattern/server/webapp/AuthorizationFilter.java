/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp;

import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.genepattern.server.util.AuthorizationManagerFactory;
import org.genepattern.util.GPConstants;

/**
 * @author Liefeld
 *
 *
 */
public class AuthorizationFilter implements Filter {
    public void destroy() {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
    throws IOException, ServletException 
    {
        HttpServletRequest req = (HttpServletRequest) request;

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
        boolean allowed = AuthorizationManagerFactory.getAuthorizationManager().isAllowed(uri, userId);

        if (!allowed) { 
            redirectToNotPermittedPage((HttpServletRequest) request, (HttpServletResponse) response);
            return;
        } 
        else { 
            chain.doFilter(request, response);
            return;
        }
    }

    public void init(FilterConfig filterconfig) throws ServletException {
    }

    public void redirectToNotPermittedPage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (response == null) {
            return;
        }
        
        String notPermittedUrl = 
            request.getContextPath() 
            + 
            "/pages/notPermitted.jsf?link=" 
            + URLEncoder.encode(request.getRequestURI(), GPConstants.UTF8);
        
        response.sendRedirect(notPermittedUrl);
    }

}
