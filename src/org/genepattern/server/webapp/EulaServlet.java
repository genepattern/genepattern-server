package org.genepattern.server.webapp;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.server.eula.EulaManager;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.TaskInfo;

/**
 * Servlet for handling the 'OK' button from the eulaTaskForm. 
 *     Hint: the existing JSF for the core of GP is a bit convoluted and cumbersome to do things in the 'normal' way.
 *     Instead it's easier to hand-code a servlet for the 'Ok' button.
 *     
 *     /gp/ModuleUserAgreement?accept=<yes | no>&lsid=<lsid>&redirect=<link>
 * 
 * <pre>
   Example HTTP requests:
       GET /gp/eula?lsid=<lsid>
       GET /gp/eula?lsid=<lsid>&initalQueryString=<query>
       GET /gp/eula?lsid=<lsid>&reloadJob=<jobno>
 * </pre>
 *     
 * @author pcarr
 *
 */
public class EulaServlet  extends HttpServlet implements Servlet {
    private static Logger log = Logger.getLogger(EulaServlet.class);
    
    public static String getServletPath(final HttpServletRequest request) {
        String rootPath=UrlUtil.getGpUrl(request);
        if (!rootPath.endsWith("/")) {
            rootPath += "/";
        }
        rootPath += "eula";
        return rootPath;
    }


    public void init(ServletConfig config) 
    throws ServletException
    {
        super.init(config);
    }
    
    public void doGet(HttpServletRequest req, HttpServletResponse resp) 
    throws IOException, ServletException
    {
        process(req, resp, true);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) 
    throws IOException, ServletException
    {
        process(req, resp, false);
    }

    private void process(HttpServletRequest request, HttpServletResponse resp, boolean redirect) 
    throws IOException
    {
        String currentUser=null;
        String lsid=null;
        try {
            currentUser = getUserIdFromSession(request);
            lsid=request.getParameter("lsid");
            recordEULA(currentUser,lsid);
        }
        catch (Throwable t) {
            String message="Error accepting EULA agreement in servlet: "+t.getLocalizedMessage();
            log.error(message, t);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
            return;
        }
        
        //success, how to handle redirect?
        String redirectTo=request.getContextPath() + "/pages/index.jsf";
        String initialQueryString=request.getParameter("initialQueryString");
        if (initialQueryString != null && initialQueryString.length() != 0) {
            redirectTo += "?"+initialQueryString;
        }
        else {
            redirectTo += "?lsid=" + UIBeanHelper.encode(lsid);
            String reloadJob=request.getParameter("reloadJob");
            if (reloadJob != null && reloadJob.length() != 0) {
                redirectTo += "&reloadJob="+reloadJob;
            }
        }
        resp.sendRedirect(redirectTo);
        return;
    }

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

    private static TaskInfo initTaskInfo(final String currentUser, final String lsid) {
        //TODO: this code is duplicated in RunTaskBean, 
        //      should find a way to share the same instance of the TaskInfo per page request
        TaskInfo taskInfo = null;
        if (lsid != null && lsid.length()>0) {
            try {
                final LocalAdminClient lac = new LocalAdminClient(currentUser);
                taskInfo = lac.getTask(lsid);
            }
            catch (Throwable t) {
                log.error("Error initializing taskInfo for lsid=" + lsid, t);
            }
        } 
        return taskInfo;
    }

    private static void recordEULA(final String currentUser, final String lsid) {
        Context taskContext=Context.getContextForUser(currentUser);
        TaskInfo taskInfo = null;
        taskInfo = initTaskInfo(currentUser, lsid);
        taskContext.setTaskInfo(taskInfo);
        EulaManager.instance(taskContext).recordEula(taskContext);
    }

}

