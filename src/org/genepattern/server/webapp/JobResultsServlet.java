/*******************************************************************************
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright (2003-2011) by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are
 * reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 *
 *******************************************************************************/

package org.genepattern.server.webapp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.NavigationHandler;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextFactory;
import javax.faces.lifecycle.Lifecycle;
import javax.faces.lifecycle.LifecycleFactory;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.genepattern.server.JobIDNotFoundException;
import org.genepattern.server.JobInfoManager;
import org.genepattern.server.JobInfoWrapper;
import org.genepattern.server.JobManager;
import org.genepattern.server.PermissionsHelper;
import org.genepattern.server.auth.GroupPermission;
import org.genepattern.server.auth.GroupPermission.Permission;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.user.UserProp;
import org.genepattern.server.util.EmailNotificationManager;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.server.webapp.jsf.RunTaskBean;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.server.webservice.server.ProvenanceFinder.ProvenancePipelineResult;
import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.WebServiceException;
import org.json.JSONObject;

/**
 *
 * Security filter on job results pages which are now accessible via normal http
 * without using the retrieveResults.jsp page
 *
 * In 3.0, you are allowed access to the directory of your own jobs only but not
 * the parent dir of your jobs or anyone elses jobs
 *
 * This is determined by looking at the URL, taking the job # as whatever
 * follows the "jobResults" part in the url
 *
 *
 * reqURL: http://gp21e-789.broadinstitute.org:8080/gp/jobResults/92/foo.out
 * contextPath: /gp 
 * reqURI: /gp/jobResults/92/foo.out 
 * ServletPath: /jobResults/92/foo.out
 *
 * and in genepattern.properties we typically have jobs=./webapps/gp/jobResults
 * which defines the directory the files are in
 *
 * In this first implementation, we'll assume the dir name 'jobResults' cannot
 * be changed in any GP install
 *
 * @author Ted Liefeld
 * @author Joshua Gould
 *
 */
public class JobResultsServlet extends HttpServlet implements Servlet {
    private static Logger log = Logger.getLogger(JobResultsServlet.class);
    
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    public void doHead(HttpServletRequest req, HttpServletResponse resp) 
    throws IOException, ServletException 
    {
        boolean serveContent = false;
        processRequest(req, resp, serveContent);
    }
    
    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
    {
        boolean serveContent = true;
        processRequest(request, response, serveContent);
    }

    /**
     * Handle requests on job results.
     * <pre>
       GET /jobResults
       GET /jobResults/
       GET /jobResults/<job>
       GET /jobResults/<job>/
       GET /jobResults/<job>/<file>
       GET /jobResults/<job>.zip
     * </pre>
     */
    public void processRequest(HttpServletRequest request, HttpServletResponse response, boolean serveContent) 
    throws IOException, ServletException
    { 
        if (!checkServletPath(request, response)) {
            return;
        }
        
        //parse the job number and file
        StringBuffer resultsPath = initParsePathInfo(request);
        String jobNumber = parseJobNumber(resultsPath);
        String file = null;
        if (jobNumber != null) {
            file = parseFilename(resultsPath);
            //Note: special case for /jobResults/<job>.zip, file will equal '.zip'
        }
        
        //special case: list all job results
        if (jobNumber == null) {
            RequestDispatcher rd = this.getServletContext().getRequestDispatcher("/pages/jobResults.jsf");
            rd.forward(request, response);
            return;
        }

        boolean allowed = false;
        int jobID = -1;
        String useridFromSession = getUserIdFromSession(request);
        if (useridFromSession != null) {
            try {
                jobID = Integer.parseInt(jobNumber);
                final boolean isAdmin = AuthorizationHelper.adminJobs(useridFromSession);
                PermissionsHelper ph = new PermissionsHelper(isAdmin, useridFromSession, jobID);
                allowed = ph.canReadJob();            
            }
            catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid jobid: "+jobNumber);
                return;
            }
            catch (JobIDNotFoundException e) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Job not found: "+jobNumber);
                return;
            }
        }
        if (!allowed) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (file == null) {
            String returnTypeParam = request.getParameter("returnType");
            if ("JSON".equalsIgnoreCase(returnTypeParam)) {
                log.error("returnType=JSON not supported");
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "returnType=JSON not supported");
                return;
            }
            String openVisualizers = request.getParameter("openVisualizers") == null ? "" : "&openVisualizers=true";
            
            RequestDispatcher rd = this.getServletContext().getRequestDispatcher("/pages/jobResult.jsf?jobNumber="+jobNumber+openVisualizers);
            response.addHeader("Pragma", "no-cache");
            response.addHeader("Cache-Control", "no-cache");
            response.addHeader("Cache-Control", "no-store");
            response.addHeader("Cache-Control", "must-revalidate");
            response.addHeader("Expires", "Mon, 1 Jan 2006 05:00:00 GMT");//in the past
            rd.forward(request, response);
            return;
        }
        
        if (file.equals(".zip")) {
            downloadZip(useridFromSession, jobID, request, response);
            return;
        }

        ServerConfiguration.Context context = ServerConfiguration.Context.getContextForUser(useridFromSession);
        File rootJobDir = null;
        try {
            rootJobDir = ServerConfigurationFactory.instance().getRootJobDir(context);
        }
        catch (Throwable t) {
            log.error(t);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t.getLocalizedMessage());
            return;
        }
        File fileObj = new File(rootJobDir, jobNumber + File.separator + file);
        FileDownloader.serveFile(this.getServletContext(), request, response, serveContent, fileObj);
    }
    
    /**
     * Handle actions on job existing jobs results:
     * <pre>
       POST /jobResults/<job>/showExecutionLogs
       POST /jobResults/<job>/hideExecutionLogs
       POST /jobResults/<job>/requestEmailNotification
       POST /jobResults/<job>/cancelEmailNotification
       POST /jobResults/<job>/setPermissions
       POST /jobResults/<job>/deleteJob
       POST /jobResults/<job>/deleteFile
       POST /jobResults/<job>/saveFile
       POST /jobResults/<job>/createPipeline
       POST /jobResults/<job>/loadTask
     * </pre>
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) 
    throws IOException, ServletException
    {
        if (!checkServletPath(request, response)) {
            return;
        }  
        //get the current user
        String currentUserId = this.getUserIdFromSession(request);
        //parse the job number and file
        StringBuffer resultsPath = initParsePathInfo(request);
        String jobId = parseJobNumber(resultsPath);
        int jobNumber = -1;
        try {
            jobNumber = Integer.parseInt(jobId);
        }
        catch (NumberFormatException e) {
            response.setHeader("X-genepattern-JobResultsServletException", "Invalid jobNumber: "+jobId);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        String action = null;
        if (resultsPath != null) {
            action = resultsPath.toString();
        }
        
        Boolean showExecutionLogs = null;
        Boolean sendNotification = null;
        if ("showExecutionLogs".equals(action)) {
            showExecutionLogs = true;
        }
        else if ("hideExecutionLogs".equals(action)) {
            showExecutionLogs = false;
        }
        else if ("requestEmailNotification".equals(action)) {
            sendNotification = true;
        }
        else if ("cancelEmailNotification".equals(action)) {
            sendNotification = false;
        }
        else if ("setPermissions".equals(action)) {
            setPermissions(currentUserId, jobNumber, request, response);
            return;
        }
        else if ("deleteJob".equals(action)) {
            deleteJob(currentUserId, jobNumber, request, response);
        }
        else if ("deleteFile".equals(action)) {
            deleteFile(currentUserId, jobNumber, request, response);            
        }
        else if ("saveFile".equals(action)) {
            saveFile(currentUserId, jobNumber, request, response);
        }
        else if ("createPipeline".equals(action)) {
            createPipeline(currentUserId, jobNumber, request, response);
        }
        else if ("loadTask".equals(action)) {
            loadTask(currentUserId, jobNumber, request, response);
        }
        else {
            response.setHeader("X-genepattern-JobResultsServletException", "Action not available: "+action);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        if (showExecutionLogs != null) {
            try {
                setShowExecutionLogs(currentUserId, showExecutionLogs);
                response.setStatus(HttpServletResponse.SC_OK);
                return;
            }
            catch (Exception e) {
                response.setHeader("X-genepattern-JobResultsServletException", action + ": " + e.getLocalizedMessage());
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
        }
        
        if (sendNotification != null) {
            try {
                sendEmailNotification(request, sendNotification, currentUserId, jobNumber);
                response.setStatus(HttpServletResponse.SC_OK); 
                return;
            }
            catch (EmailNotificationException e) {
                response.setHeader("X-genepattern-EmailNotificationException", e.getErrorMessage());
                response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getErrorMessage());
                return;
            }
        } 
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
    
    /**
     * Double check that the servlet path is properly configured.
     * For Tomcat this is specified in the 'web.xml' file.
     * @return true if the servlet path is valid
     */
    private boolean checkServletPath(HttpServletRequest request, HttpServletResponse response) 
    throws IOException
    {
        String servletPath = request.getServletPath();
        if (!"/jobResults".equals(servletPath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid servletPath: "+servletPath);
            return false;
        }
        return true;
    }
    
    private StringBuffer initParsePathInfo(HttpServletRequest request) {
        StringBuffer resultsPath = null;
        String requestPathInfo = request.getPathInfo();
        if (requestPathInfo != null) {
            resultsPath = new StringBuffer(request.getPathInfo());
            //remove leading '/'
            if (resultsPath != null && resultsPath.length() > 0) {
                if ('/' != resultsPath.charAt(0)) {
                    log.error("Expecting leading '/' character in request.pathInfo: "+resultsPath);
                    return resultsPath;
                }
                resultsPath.delete(0, 1);
            }
        }
        return resultsPath;        
    }

    private String parseJobNumber(StringBuffer resultsPath) {
        if (resultsPath == null) {
            return null;
        }
        //parse the job number and file
        String jobNumber = null;
        if (resultsPath.length() > 0) {
            int idx = resultsPath.indexOf("/");
            if (idx < 0) {
                //check for special case: <job>.zip
                int idx2 = resultsPath.indexOf(".zip");
                if (idx2 >= 0) {
                    jobNumber = resultsPath.substring(0, idx2);
                    resultsPath.delete(0, idx2);
                    return jobNumber;
                }
                jobNumber = resultsPath.toString();
                resultsPath.delete(0, resultsPath.length());
            }
            else {
                jobNumber = resultsPath.substring(0, idx);
                //remove '/'
                resultsPath.delete(0, idx+1);
            }
        }
        return jobNumber; 
    }
    
    private String parseFilename(StringBuffer resultsPath) {
        if (resultsPath != null && resultsPath.length() > 0) {
            return resultsPath.toString();
        }
        return null;
    }
    
    private void setShowExecutionLogs(String userId, boolean showExecutionLogs) {
        new UserDAO().setProperty(userId, "showExecutionLogs", String.valueOf(showExecutionLogs));
    }

    private final static class EmailNotificationException extends Exception {
        private String errorMessage = "";
        
        public EmailNotificationException(String errorMessage) {
            this.errorMessage = errorMessage;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }

    private void sendEmailNotification(HttpServletRequest request, boolean sendNotification, String currentUserId, int jobNumber) 
    throws EmailNotificationException
    {
        String currentUserEmail = null;
        try {
            HibernateUtil.beginTransaction();
            UserDAO userDao = new UserDAO();
            User user = userDao.findById(currentUserId);
            if (user != null) {
                currentUserEmail = user.getEmail();
            }
            
            //special case: current user has not yet set their email
            if (currentUserEmail == null || "".equals(currentUserEmail)) {
                currentUserEmail = request.getParameter("userEmail");
            }

            String key = null;
            if (jobNumber >= 0 && currentUserId != null && currentUserEmail != null) {
                key = UserProp.getEmailNotificationPropKey(jobNumber);
            }
            if (key == null) {
                throw new  EmailNotificationException("Can't send email notification: jobNumber="+jobNumber+", user="+currentUserId+", email="+currentUserEmail);
            }
            //save state
            String value = String.valueOf(sendNotification);
            userDao.setProperty(currentUserId, key, value);
            //send notification
            if (sendNotification) {
                EmailNotificationManager.getInstance().addWaitingUser(currentUserEmail, currentUserId, ""+jobNumber);
            } 
            else {
                EmailNotificationManager.getInstance().removeWaitingUser(currentUserEmail, currentUserId, ""+jobNumber);
            }
        }
        catch (Exception e) {
            String errorMessage = "Unable to initialize email notification for user: '"+currentUserId+"': "+e.getLocalizedMessage();
            log.error(errorMessage, e);
            throw new EmailNotificationException(errorMessage);
        }
        finally {
            HibernateUtil.commitTransaction();
        }
    }

    /**
     * Process HTTP POST to set access permissions for a job.
     * <pre>
     * HTTP POST /gp/jobResults/<job>/setPermissions
     * 
     * Request parameters:
     *     jobAccessPerm:<group>=[NONE|READ|READ_WRITE]
     *     
     * For example,
     *     HTTP POST /gp/jobResults/2464/setPermissions
     *         jobAccessPerm:*=READ&jobAccessPerm:administrators=READ_WRITE
     * This request will set the access permission for job #2464 to READ for everyone (note the '*' character),
     * and to READ_WRITE for all members of the administrators group.
     * 
     * By default all other access permissions are set to NONE, so there is really no need to explicitly include,
     *     jobAccessPerm:<group>=NONE
     * in the request.
     *     
     * Response codes
     * 1. 200 OK  , or 204 No content
     *    Indicates successful update of job permissions.
     *  
     * 2. 3xx
     *    Indicates successful update when redirect=true.
     *    
     * 3. 403
     *    Indicates an error in setting the permissions.
     *    Response headers, when an error occurs, a response header gives the details:
     *    X-genepattern-setPermissionsException
     * </pre>
     * 
     * @see PermissionHelper, which has the code which updates the permissions.
     * 
     * @auther pcarr
     */
    private void setPermissions(String currentUserId, int jobNumber, HttpServletRequest request, HttpServletResponse response) 
    throws IOException
    {
        Map<String,String[]> requestParameters = request.getParameterMap();
        Set<GroupPermission> updatedPermissions = new HashSet<GroupPermission>();
        for(String name : requestParameters.keySet()) {
            int idx = name.indexOf("jobAccessPerm:");
            if (idx >= 0) {
            	
                idx += "jobAccessPerm:".length();
                String groupId = name.substring(idx);
                Permission groupAccessPermission = Permission.NONE;
          
                String permFlagStr = null;
                String[] values = requestParameters.get(name);
                if (values != null && values.length == 1) {
                    permFlagStr = values[0];
                }
                else {
                	log.error("Unexpected value for request parameter, "+name+"="+values);
                }
                try {
                    groupAccessPermission = Permission.valueOf(permFlagStr);
                }
                catch (IllegalArgumentException e) {
                    handleSetPermissionsException(response, "Ignoring permissions flag: "+permFlagStr, e);
                    return;
                }
                catch (NullPointerException e) {
                    handleSetPermissionsException(response, "Ignoring permissions flag: "+permFlagStr, e);
                    return;
                }
                if (groupAccessPermission != Permission.NONE) {
                    //ignore NONE
                    updatedPermissions.add(new GroupPermission(groupId, groupAccessPermission));
                }
            }
        }
        
        try {
            final boolean isAdmin = AuthorizationHelper.adminJobs(currentUserId);
            PermissionsHelper permissionsHelper = new PermissionsHelper(isAdmin, currentUserId, jobNumber);
            permissionsHelper.setPermissions(updatedPermissions);

            response.setContentType("text/javascript");
            JSONObject json = new JSONObject();
            json.put("success", true);
            json.put("isShared", permissionsHelper.isShared());
            // let's wait half a second, we dont' want to return too quick or user won't think anything happened.
            Thread.sleep(500);
            
            response.getWriter().write(json.toString());
            return;
        }
        catch (Exception e) {
            handleSetPermissionsException(response, "You are not authorized to change the permissions for this job", e);
        }
    }
    
    private void handleSetPermissionsException(HttpServletResponse response, String message, Exception e) 
    throws IOException
    {
    	response.setContentType("text/javascript");
    	try {
        JSONObject json = new JSONObject();
        json.put("success", false);
        // let's wait half a second, we dont' want to return too quick or user won't think anything happened.
        Thread.sleep(500);
        response.getWriter().write(json.toString());
    	} catch (Exception e2) {}
        
        return;
    }
    
    private void deleteJob(String currentUserId, int jobNumber, HttpServletRequest request, HttpServletResponse response) 
    throws IOException
    {
        final boolean isAdmin = AuthorizationHelper.adminJobs(currentUserId);
        PermissionsHelper perm = new PermissionsHelper(isAdmin, currentUserId, jobNumber);
        if (!perm.canWriteJob()) {
            response.setHeader("X-genepattern-deleteJobException", "User "+currentUserId+" does not have permission to delete job "+jobNumber);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            //Note: the deleteJob method also checks permissions before proceeding
            JobManager.deleteJob(isAdmin, currentUserId, jobNumber);
        } 
        catch (WebServiceException e) {
            log.error("Error deleting job " + jobNumber, e);
            response.setHeader("X-genepattern-deleteJobException", "Server error while deleting job: "+jobNumber);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        } 
        
        boolean redirect = false;
        String redirectParam = request.getParameter("redirect");
        if (redirectParam != null) {
            redirect = Boolean.valueOf(redirectParam);
        }
        if (redirect) {
            String redirectTo = request.getHeader("Referer");
            if (redirectTo == null || "".equals(redirectTo)) {
                redirectTo = request.getContextPath() + "/";
            }
            response.sendRedirect(redirectTo);
            return;
        }
        else {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
    }
    
    private void deleteFile(String currentUserId, int jobNumber, HttpServletRequest request, HttpServletResponse response) 
    throws IOException
    {
        String jobFileName = request.getParameter("jobFile");
        final boolean isAdmin = AuthorizationHelper.adminJobs(currentUserId);
        PermissionsHelper perm = new PermissionsHelper(isAdmin, currentUserId, jobNumber);
        if (!perm.canWriteJob()) {
            response.setHeader("X-genepattern-deleteFileException", "User "+currentUserId+" does not have permission to delete file "+jobNumber+" : "+jobFileName);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        
        //parse encodedJobFileName for <jobNumber> and <filename>, add support for directories
        //from Job Summary page jobFileName="1/all_aml_test.preprocessed.gct"
        //from Job Status page jobFileName="/gp/jobResults/1/all_aml_test.preprocessed.gct"
        String contextPath = request.getContextPath();
        String pathToJobResults = contextPath + "/jobResults/";
        if (jobFileName.startsWith(pathToJobResults)) {
            jobFileName = jobFileName.substring(pathToJobResults.length());
        }
        int idx = jobFileName.indexOf('/');
        if (idx <= 0) {
            response.setHeader("X-genepattern-deleteFileException", "Error deleting file: "+jobFileName);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            LocalAnalysisClient analysisClient = new LocalAnalysisClient(currentUserId);
            analysisClient.deleteJobResultFile(jobNumber, jobFileName);
        } 
        catch (WebServiceException e) {
            response.setHeader("X-genepattern-deleteFileException", "Error deleting file: "+jobFileName+", "+e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            UIBeanHelper.setErrorMessage("Error deleting file: "+jobFileName+", "+e.getMessage());
            return;
        }
        
        String redirect = request.getParameter("redirect");
        if (redirect != null) {
            response.sendRedirect(redirect);
        }
        else {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
    }
    
    //<form name="#{param.jobNumber}_saveFile_#{outputFile.value}" method="post" action="/gp/jobResults/#{param.jobNumber}/saveFile">
    //<input type="hidden" name="jobFileName" value="#{outputFile.value}" />
    private void saveFile(String currentUserId, int jobNumber, HttpServletRequest request, HttpServletResponse response) {
        String jobFileName = request.getParameter("jobFileName");
        jobFileName = UIBeanHelper.decode(jobFileName);
        if (jobFileName == null || "".equals(jobFileName.trim())) {
            response.setHeader("X-genepattern-saveFileException", "Error saving file, missing required parameter, 'jobFileName'.");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        
        //parse jobFileName for <jobNumber> and <filename>, add support for directories
        //from Job Summary page jobFileName="1/all_aml_test.preprocessed.gct"
        //from Job Status page jobFileName="/gp/jobResults/1/all_aml_test.preprocessed.gct"
        String contextPath = request.getContextPath();
        String pathToJobResults = contextPath + "/jobResults/";
        if (jobFileName.startsWith(pathToJobResults)) {
            jobFileName = jobFileName.substring(pathToJobResults.length());
        }

        int idx = jobFileName.indexOf('/');
        if (idx <= 0) {
            response.setHeader("X-genepattern-saveFileException", "Error saving file, invalid parameter, jobFileName="+jobFileName);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        String jobNumberStr = jobFileName.substring(0, idx);
        String filename = jobFileName.substring(idx+1);
        File in = new File(GenePatternAnalysisTask.getJobDir(jobNumberStr), filename);
        if (!in.exists()) {
            response.setHeader("X-genepattern-saveFileException", "File " + filename + " does not exist.");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        InputStream is = null;
        try {
            response.setHeader("Content-Disposition", "attachment; filename=" + in.getName() + ";");
            response.setHeader("Content-Type", "application/octet-stream");
            response.setHeader("Cache-Control", "no-store"); // HTTP 1.1
            response.setHeader("Pragma", "no-cache"); // HTTP 1.0 cache
            response.setDateHeader("Expires", 0);

            OutputStream os = response.getOutputStream();
            is = new BufferedInputStream(new FileInputStream(in));
            byte[] b = new byte[10000];
            int bytesRead;
            while ((bytesRead = is.read(b)) != -1) {
                os.write(b, 0, bytesRead);
            }
            os.flush();
            os.close();
        } 
        catch (IOException e) {
            log.error("Error saving file.", e);
        } 
        finally {
            if (is != null) {
                try {
                    is.close();
                } 
                catch (IOException e) {
                }
            }
        }
    }
    
    private void downloadZip(String currentUserId, int jobNumber, HttpServletRequest request, HttpServletResponse response) throws IOException {
        JobInfoManager m = new JobInfoManager();
        String contextPath = request.getContextPath();
        String cookie = request.getHeader("Cookie");
        JobInfoWrapper jobInfoWrapper = m.getJobInfo(cookie, contextPath, currentUserId, jobNumber);
        
        response.setHeader("Content-Disposition", "attachment; filename=" + jobNumber + ".zip" + ";");
        response.setHeader("Content-Type", "application/octet-stream");
        response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0 cache control
        response.setDateHeader("Expires", 0);
        OutputStream os = response.getOutputStream();
        
        JobInfoManager.writeOutputFilesToZipStream(os, jobInfoWrapper);
        os.flush();
        os.close();
    }

    private void createPipeline(String currentUserId, int jobNumber, HttpServletRequest request, HttpServletResponse response) 
    throws IOException
    {
        try {
            // TODO prompt user for name
            String pipelineName = "job" + jobNumber; 
            ProvenancePipelineResult pipelineResult = new LocalAnalysisClient(currentUserId).createProvenancePipeline(""+jobNumber, pipelineName);
            String lsid = pipelineResult.getLsid();
            if (lsid == null) {
                UIBeanHelper.setErrorMessage("Unable to create pipeline.");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/pipeline/index.jsf?lsid=" + UIBeanHelper.encode(lsid));
        } 
        catch (WebServiceException wse) {
            log.error("Error creating pipeline.", wse);
        } 
    }
    
    private void loadTask(String currentUserId, int jobNumber, HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
    {
        String lsid = UIBeanHelper.decode(request.getParameter("module"));
        String matchJob = ""+jobNumber;
        String outputFileName = UIBeanHelper.decode(request.getParameter("name"));
        
        LifecycleFactory lFactory = (LifecycleFactory) FactoryFinder.getFactory(FactoryFinder.LIFECYCLE_FACTORY);
        Lifecycle lifecycle = lFactory.getLifecycle(LifecycleFactory.DEFAULT_LIFECYCLE);
        FacesContextFactory fcFactory = (FacesContextFactory) FactoryFinder.getFactory(FactoryFinder.FACES_CONTEXT_FACTORY);
        FacesContext facesContext = fcFactory.getFacesContext(getServletContext(), request, response, lifecycle);
        Application application = facesContext.getApplication();
        ViewHandler viewHandler = application.getViewHandler();

        String viewId = "/jobResults/"+matchJob+"/loadTask";
        UIViewRoot view = viewHandler.createView(facesContext, viewId);
        facesContext.setViewRoot(view);
        
        ExternalContext externalContext = facesContext.getExternalContext();
        externalContext.getRequestMap().put("matchJob", matchJob);
        externalContext.getRequestMap().put("outputFileName", outputFileName);
        RunTaskBean runTaskBean = (RunTaskBean) application.createValueBinding("#{runTaskBean}").getValue(facesContext);
        runTaskBean.setTask(lsid);
        
        // JSF navigation ... this is JSF for a redirect
        String outcome="run task";
        String fromAction="run task";
        NavigationHandler navigationHandler = application.getNavigationHandler();
        navigationHandler.handleNavigation(facesContext, fromAction, outcome);
       
        lifecycle.render(facesContext);
        return;
    }
}



