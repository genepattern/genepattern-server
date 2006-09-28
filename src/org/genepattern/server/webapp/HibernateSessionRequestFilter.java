package org.genepattern.server.webapp;

import java.io.IOException;

import javax.servlet.*;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.SessionFactory;
import org.hibernate.StaleObjectStateException;

public class HibernateSessionRequestFilter implements Filter {

    private static Logger log = Logger.getLogger(HibernateSessionRequestFilter.class);

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {

        try {
            log.debug("Starting a database transaction");
            HibernateUtil.beginTransaction();

            // Call the next filter (continue request processing)
            chain.doFilter(request, response);

            // Commit and cleanup
            log.debug("Committing the database transaction");
            HibernateUtil.commitTransaction();
            

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
            finally {
                HibernateUtil.closeCurrentSession();
            }

            // Let others handle it... maybe another interceptor for exceptions?
            throw new ServletException(ex);
        }
    }

    public void init(FilterConfig filterConfig) throws ServletException {

    }

    public void destroy() {
    }

}