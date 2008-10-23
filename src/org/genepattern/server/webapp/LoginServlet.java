package org.genepattern.server.webapp;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.genepattern.server.auth.AuthenticationException;

/**
 * Helper servlet for programmatically logging into a GenePattern server from an HTTP client.
 * This servlet validates the 'username' and 'password' request parameters and adds the userid to the current session.
 * 
 * @see org.genepattern.server.webapp.jsf.UIBeanHelper#login(String, boolean, boolean, HttpServletRequest, HttpServletResponse)
 * 
 * @author pcarr
 */
public class LoginServlet extends HttpServlet implements Servlet {
    private LoginManager loginManager = null;
    
    public void init(ServletConfig config) 
    throws ServletException
    {
        super.init(config);
        
        loginManager = new LoginManager();
    }
    
    public void doGet(HttpServletRequest req, HttpServletResponse resp) 
    throws IOException, ServletException
    {
        process(req, resp);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) 
    throws IOException, ServletException
    {
        process(req, resp);
    }
    
    private void process(HttpServletRequest request, HttpServletResponse resp) 
    throws IOException
    {
        try {
            loginManager.loginFromWebClient(request);
        }
        catch (AuthenticationException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, e.getLocalizedMessage());
        }
    }
}
