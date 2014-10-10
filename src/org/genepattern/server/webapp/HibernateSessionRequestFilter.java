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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.StaleObjectStateException;

public class HibernateSessionRequestFilter implements Filter {

    private static Logger log = Logger.getLogger(HibernateSessionRequestFilter.class);

    /**
     * Hand-coded filter, return false if we should not begin a db connection.
     * For GP-5200
     * @param request
     */
    protected boolean shouldBeginDbTransaction(final ServletRequest request) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String contextPath=httpRequest.getContextPath();
        String uri=httpRequest.getRequestURI();
        if (uri.startsWith(contextPath+"/rest/RunTask/upload")) {
            log.debug("ignoring uri="+uri);
            return false;
        }
        else if (uri.startsWith(contextPath+"/rest/v1/upload/multipart/assemble/")) {
            log.debug("ignoring url="+uri);
            return false;
        }
        // filter out image files, for example, /gp/images/GP-logo.gif
        else if (uri.startsWith(contextPath+"/images/")) {
            return false;
        }
        // filter out css files, for example, /gp/css/frozen/menu.css
        else if (uri.startsWith(contextPath+"/css/")) {
            return false;
        }
        // filter out js files, for example, /gp/js/jquery/jquery-1.8.3.js
        else if (uri.startsWith(contextPath+"/js/")) {
            return false;
        }
        // filter out the download files, for example, /gp/downloads/GenePattern.zip
        else if (uri.startsWith(contextPath+"/downloads/")) {
            return false;
        }
        return true;
    }
    
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException { 
        try {
            if (log.isDebugEnabled()) {
                final boolean alreadyInTransaction=HibernateUtil.isInTransaction();
                log.debug("about to begin transaction, alreadyInTransaction="+alreadyInTransaction);
            }
            if (shouldBeginDbTransaction(request)) {
                HibernateUtil.beginTransaction();
            }
            
            // Call the next filter (continue request processing)
            chain.doFilter(request, response);

            // Commit and cleanup
            final boolean stillInTransaction=HibernateUtil.isInTransaction();
            if (log.isDebugEnabled()) {
                log.debug("end of filter, stillInTransaction="+stillInTransaction);
            }
            if (stillInTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        catch (StaleObjectStateException staleEx) {
            log.error("This interceptor does not implement optimistic concurrency control!");
            log.error("Your application will not work until you add compensation actions!");
            HibernateUtil.rollbackTransaction();              

            throw staleEx;
        }
        catch (Throwable ex) {
            // Rollback only
            log.error(ex);
            try {
                HibernateUtil.rollbackTransaction();              
            }
            catch (Throwable rbEx) {
                log.error("Could not rollback transaction after exception!", rbEx);
            }
            // Let others handle it... maybe another interceptor for exceptions?
            throw new ServletException(ex);
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }

    public void init(FilterConfig filterConfig) throws ServletException {

    }

    public void destroy() {
    }

}
