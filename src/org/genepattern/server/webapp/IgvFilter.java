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
import org.genepattern.server.UserAccountManager;
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
        
        boolean authenticated = basicAuth(req,resp);
        if (authenticated) {
            chain.doFilter(req, resp);
        }
        return;
    }

    public void destroy() {
    }
    
    /**
     * Authenticate the username:password pair from the request header. 
     * If the client is not authorized, this method prompts the web client to authenticate.
     * If the client is authorized, it is up to the calling method to handle the client response.
     * 
     * @param request
     * @return true iff the client is authorized.
     */
    private boolean basicAuth(HttpServletRequest req, HttpServletResponse resp) throws IOException { 
        //bypass basicauth if the current session is already authorized
        String userId = LoginManager.instance().getUserIdFromSession(req);
        if (userId != null) {
            return true;
        }

        boolean authenticated = false;
        String auth = req.getHeader("Authorization");
        if (auth != null) {
            String[] up = getUsernamePassword(auth);
            userId = up[0];
            String passwordStr = up[1];
            byte[] password = passwordStr != null ? passwordStr.getBytes() : null;
            try {
                authenticated = UserAccountManager.instance().authenticateUser(userId, password);
            }
            catch (AuthenticationException e) {
            }
        }
        
        if (authenticated) { 
            //must add the userId to the session before calling chain.doFilter,
            //otherwise we will simply be redirected to the login page
            LoginManager.instance().addUserIdToSession(req, userId);
            return true;
        }
        
        //not authorized, send 401
        final String realm = "GenePattern Server";
        resp.setHeader("WWW-Authenticate", "BASIC realm=\""+realm+"\"");
        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }

    /**
     * Parse out the username:password pair from the authorization header.
     * 
     * @param auth
     * @return <pre>new String[] {<username>, <password>};</pre>
     */
    private String[] getUsernamePassword(String auth) {
        String[] up = new String[2];
        up[0] = null;
        up[1] = null;
        if (auth == null) {
            return up;
        }

        if (!auth.toUpperCase().startsWith("BASIC "))  {
            return up;
        }

        // Get encoded user and password, comes after "BASIC "
        String userpassEncoded = auth.substring(6);

        // Decode it, using any base 64 decoder
        sun.misc.BASE64Decoder dec = new sun.misc.BASE64Decoder();
        String userpassDecoded = null;
        try {
            userpassDecoded = new String(dec.decodeBuffer(userpassEncoded));
        }
        catch (IOException e) {
            log.error("Error decoding username and password from HTTP request header", e);
            return up;
        }
        String username = "";
        String passwordStr = null;
        int idx = userpassDecoded.indexOf(":");
        if (idx >= 0) {
            username = userpassDecoded.substring(0, idx);
            passwordStr = userpassDecoded.substring(idx+1);
        }
        up[0] = username;
        up[1] = passwordStr;
        return up;
    }

}
