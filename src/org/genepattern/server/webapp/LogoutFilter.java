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
import javax.servlet.http.HttpSession;

import org.genepattern.util.GPConstants;

/**
 * Helper class for logging a user out of genepattern.
 * 
 * @author pcarr
 */
public class LogoutFilter implements Filter {

    public void init(FilterConfig arg0) throws ServletException {
    }
    
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
    throws IOException, ServletException 
    {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpRes = (HttpServletResponse) response;

        //logout operation
        HttpSession session = httpReq.getSession();
        session.removeAttribute(GPConstants.USERID);
        session.invalidate();

        //redirect to main page
        String contextPath = httpReq.getContextPath();
        httpRes.sendRedirect( contextPath );
    }

    public void destroy() {
    }
}
