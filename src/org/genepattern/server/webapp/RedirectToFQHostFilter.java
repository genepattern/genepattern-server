package org.genepattern.server.webapp;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * This filter optionally redirects all requests to the fully qualified hostname.
 * It is configured in 'web.xml', but configuration can be overridden in 'genepattern.properties'.
 * 
 * @author pcarr
 */
public class RedirectToFQHostFilter implements Filter {
    private static Logger log = Logger.getLogger(RedirectToFQHostFilter.class);

    private boolean redirectToFqHostName = false;
    private String fqHostName = "";

    private Properties loadGenePatternProperties(String resourceDir) {
        File genepatternProperties = new File(resourceDir, "genepattern.properties");
        File customProperties = new File(resourceDir, "custom.properties");
        Properties props = new Properties();

        if (genepatternProperties.exists()) {
            AuthenticationFilter.loadProperties(props, genepatternProperties);
        }

        if (customProperties.exists()) {
            AuthenticationFilter.loadProperties(props, customProperties);
        }
        return props;
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        //Initialize settings from web.xml file, but the settings can be overridden in genepattern.properties
        String resourceDir = filterConfig.getInitParameter("genepattern.properties");
        Properties props = loadGenePatternProperties(resourceDir);

        //if not set in genepattern.properties, use value from web.xml
        String redirectProp = filterConfig.getInitParameter("redirect.to.fq.host").trim();
        redirectProp = props.getProperty("redirect.to.fq.host", redirectProp);
        redirectToFqHostName = Boolean.valueOf(redirectProp);

        fqHostName = filterConfig.getInitParameter("fqHostName");
        fqHostName = props.getProperty("fqHostName", fqHostName);
        fqHostName = fqHostName.trim();
        if ("".equals(fqHostName)) {
            try {
                InetAddress localHost = InetAddress.getLocalHost();
                fqHostName = localHost.getCanonicalHostName();
            }
            catch (UnknownHostException e) {
                log.error("Error initializing RedirectToFQHostFilter: "+e.getLocalizedMessage(), e);
                fqHostName = "127.0.0.1";
            }
        }
        if (fqHostName.equals("localhost")) {
           fqHostName = "127.0.0.1";
        }
    }

    public void destroy() {
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        String serverName = servletRequest.getServerName();
        //always redirect from 'localhost' to '127.0.0.1'
        if ("localhost".equals(serverName)) {
            redirectToHostName( "127.0.0.1", (HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, filterChain);
            return;
        }
        //if necessary, redirect to fully qualified host name so that only one cookie needs to be written
        if (redirectToFqHostName) { 
            serverName = servletRequest.getServerName();
            if (!fqHostName.equalsIgnoreCase(serverName)) {
                redirectToFullyQualifiedHostName( (HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, filterChain);
                return;
            }
        }
        
        filterChain.doFilter(servletRequest, servletResponse);
    }
    
    /**
     * Replace the server name in the originating HTTP request with the fqHostName, then send a redirect response.
     * 
     * @param request
     * @param response
     * @throws IOException
     */
    private void redirectToFullyQualifiedHostName(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
    throws IOException, ServletException
    {
        redirectToHostName(this.fqHostName, request, response, filterChain);
    }
    
    private void redirectToHostName(String hostName, HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
    throws IOException, ServletException
    {
        StringBuffer requestUrl = request.getRequestURL();
        //modify the requestUrl by replacing the request.serverName with the fqHostName
        String serverName = request.getServerName();
        int idx = requestUrl.indexOf(serverName);
        if (idx >= 0) {
            String start = requestUrl.substring(0, idx);
            String end = requestUrl.substring(idx+serverName.length());
            String queryString = request.getQueryString();
            if (queryString == null) {
                queryString = "";
            } 
            else {
                queryString = "?" + queryString;
            }
            String fqAddress = start + hostName + end + queryString;
            response.sendRedirect(fqAddress);
            return;
        }
        else {
            //error
            log.error("Unable to redirectToHostName, requestURL=" + request.getRequestURL()+", hostName="+hostName);
            filterChain.doFilter(request, response);
        }
    }
}
