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

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.genepattern.server.util.AccessManager;

/**
 * @author Liefeld
 * 
 */
public class ConnectionFilter implements Filter {

    public void init(FilterConfig filterConfig) throws ServletException {

    }

    public void destroy() {

    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {

        boolean allowed = AccessManager.isAllowed(request.getRemoteHost(), request.getRemoteAddr());

        if (allowed) {
            chain.doFilter(request, response);
        } else {
            if (isJspCall((HttpServletRequest) request)) {
                ((HttpServletResponse) response).sendRedirect("/pages/notallowed.jsf");
            } else {
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
            }

        }
    }

    public boolean isJspCall(HttpServletRequest request) {
        return (request.getRequestURI().indexOf(".jsp") >= 0);

    }

}
