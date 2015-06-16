/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
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

/**
 * Servlet filter for logging a user out of genepattern.
 * 
 * @author pcarr
 */
public class LogoutFilter implements Filter {

    public void init(FilterConfig arg0) throws ServletException {
    }
    
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
    throws IOException, ServletException 
    {
        boolean redirect = true;
        String redirectParam = request.getParameter("redirect");
        if (redirectParam != null) {
            redirect = Boolean.valueOf(redirectParam);
        }
        LoginManager.instance().logout((HttpServletRequest) request, (HttpServletResponse) response, redirect);
    }

    public void destroy() {
    }
}
