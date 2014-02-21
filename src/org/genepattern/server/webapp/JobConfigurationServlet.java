package org.genepattern.server.webapp;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.executor.CommandManagerFactory;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.util.GPConstants;

/**
 * Restful interface to the Job configuration page. 
 * 
 * Requires a session with a logged in admin account.
 * Implemented methods:
 * <code>
       POST /gp/rest/JobConfiguration?method=reloadConfigFile
 * </code>
 * 
 * Here is an example 'curl' session which reloads the job configuration:
 * <code>
   curl --dump-header session.txt -d username=admin -d password=**** http://127.0.0.1:8080/gp/login
   curl --cookie session.txt -d method=reloadConfigFile http://127.0.0.1:8080/gp/rest/JobConfiguration
 * </code>
 * 
 * @author pcarr
 */
public class JobConfigurationServlet extends HttpServlet {
    //TODO: duplicate of JobResultsServlet
    private String getUserIdFromSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object obj = session.getAttribute(GPConstants.USERID);
            if (obj instanceof String) {
                return (String) obj;
            }
        }
        return null;        
    }

    private boolean checkPermissions(HttpServletRequest req, HttpServletResponse resp) throws IOException {        
        final String userIdFromSession = getUserIdFromSession(req);
        final boolean isAdmin = AuthorizationHelper.adminJobs(userIdFromSession);
        if (!isAdmin) {
            handleError(resp, HttpServletResponse.SC_FORBIDDEN, "Failure, Must be logged into an admin account!");
        }
        return isAdmin;
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    public void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
    
    public void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
    
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        //require login
        if (!checkPermissions(req, resp)) {
            return;
        }
        String method = req.getParameter("method");
        if (method != null && "reloadConfigFile".equalsIgnoreCase(method)) {
            ServerConfigurationFactory.reloadConfiguration();
            handleSuccess(resp);
            return;
        }
        handleError(resp);
    }
    
    private void handleSuccess(HttpServletResponse resp) throws IOException {
        handleSucess(resp, "Success!");
    }

    private void handleSucess(HttpServletResponse resp, String message) throws IOException {
        resp.getWriter().println(message);
    }

    private void handleError(HttpServletResponse resp) throws IOException {
        handleError(resp, "Failure!");
    }

    private void handleError(HttpServletResponse resp, String errorMessage) throws IOException {
        handleError(resp, HttpServletResponse.SC_BAD_REQUEST, errorMessage);
    }
    private void handleError(HttpServletResponse resp, int httpServletResponseCode, String errorMessage) throws IOException {
        resp.setStatus(httpServletResponseCode);
        resp.getWriter().println(errorMessage);
    }
}
