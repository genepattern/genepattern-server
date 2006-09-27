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
            HibernateUtil.getSessionFactory().getCurrentSession().beginTransaction();

            // Call the next filter (continue request processing)
            chain.doFilter(request, response);

            // Commit and cleanup
            log.debug("Committing the database transaction");
            if (HibernateUtil.getSession().getTransaction().isActive()) {
                HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
            }

        }
        catch (StaleObjectStateException staleEx) {
            log.error("This interceptor does not implement optimistic concurrency control!");
            log.error("Your application will not work until you add compensation actions!");
            // Rollback, close everything, possibly compensate for any permanent
            // changes
            // during the conversation, and finally restart business
            // conversation. Maybe
            // give the user of the application a chance to merge some of his
            // work with
            // fresh data... what you do here depends on your applications
            // design.
            throw staleEx;
        }
        catch (Throwable ex) {
            // Rollback only
            ex.printStackTrace();
            try {
                if (HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().isActive()) {
                    log.debug("Trying to rollback database transaction after exception");
                    HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().rollback();
                }
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