package org.genepattern.server.webapp.rest;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.webapp.BasicAuthUtil;

/**
 * Custom filter to use HTTP Basic Authentication for the GenePattern REST API.
 * 
 * References:
 *     http://www.oracle.com/technetwork/java/filters-137243.html
 *     http://seamframework.org/Community/LargeFileDownload
 * 
 * @author pcarr
 */
public class RestApiFilter implements Filter {
    private static Logger log = Logger.getLogger(RestApiFilter.class);

    @Override
    public void init(FilterConfig arg0) throws ServletException {
    }
    
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            log.error("Expecting HttpServletRequest");
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        //announce support for partial get
        resp.setHeader("Accept-Ranges", "bytes");

        String gpUserId = null;
        try {
            gpUserId = BasicAuthUtil.getAuthenticatedUserId(req, resp);
        }
        catch (AuthenticationException e) {
            BasicAuthUtil.requestAuthentication(resp, e.getLocalizedMessage());
            return;
        }
        
        if (gpUserId == null) {
            log.error("Expecting an AuthenticationException to be thrown");
            BasicAuthUtil.requestAuthentication(req, resp);
            return;
        }
        chain.doFilter(req, resp);
        return;
    }

    public void destroy() {
    }


}
