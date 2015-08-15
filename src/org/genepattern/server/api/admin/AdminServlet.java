/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.api.admin;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.genepattern.server.DataManager;
import org.genepattern.server.api.ApiUtil;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.util.GPConstants;

/**
 * Hand coded servlet, for the GP 3.4+ rest api to administer to the server.
 * 
 * Implements the following methods,
 *     POST /api/admin/syncUploads  (requires form-field, userid=<userid>)
 *     
 * Example usage,
 *     # tell the gp server to sync the user uploads tab for the 'test' account
 *     curl --user admin:****** -X POST -d "userid=test" http://127.0.0.1:8080/gp/api/admin/syncUploads
 * 
 * @author pcarr
 */
public class AdminServlet extends HttpServlet {
    
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
            ApiUtil.handleError(resp, HttpServletResponse.SC_FORBIDDEN, "Failure, Must be logged into an admin account!");
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
        //String method = req.getParameter("method");
        //if (method != null && "reloadConfigFile".equalsIgnoreCase(method)) {
        //    ServerConfiguration.instance().reloadConfiguration();
        //    handleSuccess(resp);
        //    return;
        //}
        
        final String method = getApiMethod(req);
        if ("reloadConfigFile".equalsIgnoreCase(method)) {
            ApiUtil.handleError(resp, HttpServletResponse.SC_NOT_IMPLEMENTED , "Method: "+method+" not implemented!");
            return;
        }
        if ("syncUploads".equalsIgnoreCase(method)) {
            syncUploads(req, resp);
            return;
        }
        ApiUtil.handleError(resp);
    }
    
    /**
     * Parse the request for the internal method name.
     * 
     * @param req
     * @return null if no method specified
     */
    private String getApiMethod(HttpServletRequest req) {
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();
        if (servletPath.startsWith("/api/")) {
            servletPath = servletPath.substring("/api/".length());
        }
        if (pathInfo == null) {
            return "";
        }
        if (pathInfo.startsWith("/")) {
            pathInfo = pathInfo.substring(1);
        }
        return pathInfo;
    }
    
    //methods
    private void syncUploads(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String userid = req.getParameter("userid");
        if (userid == null) {
            ApiUtil.handleError(resp, HttpServletResponse.SC_NOT_FOUND, "Missing required parameter, 'userid'");
            return;
        }
        try {
            DataManager.syncUploadFiles(HibernateUtil.instance(), userid);
            ApiUtil.handleSuccess(resp);
        }
        catch (Throwable t) {
            ApiUtil.handleError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: "+ t.getLocalizedMessage());
        }
    }
    
}
