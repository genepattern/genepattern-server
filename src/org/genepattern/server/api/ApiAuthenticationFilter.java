package org.genepattern.server.api;

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
import org.genepattern.server.webapp.AuthenticationUtil;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;

/**
 * Default authorization and authentication filter for all GP API requests.
 * 
 * As currently implemented, uses HTTP Basic Authentication, requires an authenticated admin account.
 * 
 * @author pcarr
 */
public class ApiAuthenticationFilter implements Filter {
    private static Logger log = Logger.getLogger(ApiAuthenticationFilter.class);

    public void init(FilterConfig arg0) throws ServletException {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            log.error("Expecting HttpServletRequest");
            chain.doFilter(request, response);
        }
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        //1) authenticate
        String gpUserId = null;
        try {
            gpUserId = AuthenticationUtil.getAuthenticatedUserId(req, resp);
        }
        catch (AuthenticationException e) {
            AuthenticationUtil.requestBasicAuth(resp, e.getLocalizedMessage());
            return;
        } 
        if (gpUserId == null) {
            //don't expect to be here
            log.error("Expecting an AuthenticationException to be thrown");
            AuthenticationUtil.requestBasicAuth(req, resp);
            return;
        }
        //2) authorize
        final boolean isAdmin = AuthorizationHelper.adminServer(gpUserId);
        if (!isAdmin) {
            //error
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User '"+gpUserId+"' is not authorized1");
            return;
        }
        
        chain.doFilter(req, resp);
        return;
    }

    public void destroy() {
    }

}
