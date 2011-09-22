package org.genepattern.server.webapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

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

/**
 * Custom filter to use HTTP Basic Authentication for all requests coming from the configured set of 
 * User-Agent prefixes.
 * 
 * This filter was added to allow IGV web client to directly request files in the jobResults directory.
 * Without this filter, the login page was being loaded into IGV for visualization.
 * 
 * Edit the web.xml file to configure the set of User-Agents (by prefix) to which this filter applies.
 * The following configures this filter to apply for all user agents prefixed with 'IGV'.
 * <pre>
   <init-param><param-name>IGV</param-name><param-value></param-value></init-param>
 * </pre>
 * 
 * References:
 *     http://www.oracle.com/technetwork/java/filters-137243.html
 *     http://seamframework.org/Community/LargeFileDownload
 * 
 * @author pcarr
 */
public class IgvFilter implements Filter {
    private static Logger log = Logger.getLogger(IgvFilter.class);
    
    private List<String> userAgentPrefixes;

    public void init(FilterConfig filterConfig) throws ServletException {
        userAgentPrefixes = new ArrayList<String>();
        Enumeration<?> e = filterConfig.getInitParameterNames();
        while(e.hasMoreElements()) {
            String prefix = (String) e.nextElement();
            userAgentPrefixes.add(prefix);
        }
    }
    
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            log.error("Expecting HttpServletRequest");
            chain.doFilter(request, response);
        }
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        
        boolean applyFilter = false;
        String userAgent = req.getHeader("User-Agent");
        if (userAgent != null) {
            for(String prefix : userAgentPrefixes) {
                if (userAgent.startsWith(prefix)) {
                    applyFilter = true;
                    break;
                }
            }
        }
        if (!applyFilter) {
            chain.doFilter(request, response);
            return;
        }

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
