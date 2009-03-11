/*******************************************************************************
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright (2003-2009) by the
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

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
import org.genepattern.server.PermissionsHelper;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.user.UserProp;
import org.genepattern.server.util.EmailNotificationManager;
import org.genepattern.util.GPConstants;
import org.json.JSONException;

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
 * reqURL: http://gp21e-789.broad.mit.edu:8080/gp/jobResults/92/foo.out
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

    private String jobsDirectory;
    
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        String dir = config.getInitParameter("genepattern.properties");
        File propFile = new File(dir, "genepattern.properties");
        File customPropFile = new File(dir, "custom.properties");
        Properties props = new Properties();

        if (propFile.exists()) {
            loadProperties(props, propFile);
        }

        if (customPropFile.exists()) {
            loadProperties(props, customPropFile);
        }
        jobsDirectory = props.getProperty("jobs");
    }

    private void loadProperties(Properties props, File propFile) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(propFile);
            props.load(fis);

        } 
        catch (IOException e) {
            log.error(e);
        } 
        finally {
            if (fis != null) {
                try {
                    fis.close();
                } 
                catch (IOException e) {
                }
            }
        }
    }

    /**
     * Handle requests on job results.
     * <pre>
       GET /jobResults
       GET /jobResults/
       GET /jobResults/<job>
       GET /jobResults/<job>/
       GET /jobResults/<job>?returnType=JSON
       GET /jobResults/<job>/?returnType=JSON
       GET /jobResults/<job>/<file>
     * </pre>
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) 
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
                PermissionsHelper ph = new PermissionsHelper(useridFromSession, jobID);
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
                //return json formatted version of job 
                JobInfoManager m = new JobInfoManager();
                
                String contextPath = request.getContextPath();
                String cookie = request.getHeader("Cookie");

                JobInfoWrapper jobInfoWrapper = m.getJobInfo(cookie, contextPath, useridFromSession, jobID);

                try {
                    response.setContentType("application/json");
                    response.setHeader("Cache-Control", "no-cache");

                    m.writeJobInfo(response.getWriter(), jobInfoWrapper);
                    
                    response.getWriter().flush();
                    response.getWriter().close();
                    
                    return;
                }
                catch (JSONException e) {
                    log.error("Error in ajax request for job info: "+e.getMessage(), e);
                }
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

        File fileObj = new File(jobsDirectory + File.separator + jobNumber + File.separator + file);
        serveFile(fileObj, response);
    }
    
    /**
     * Handle actions on job existing jobs results:
     * <pre>
       POST /jobResults/<job>/showExecutionLogs
       POST /jobResults/<job>/hideExecutionLogs
       POST /jobResults/<job>/requestEmailNotification
       POST /jobResults/<job>/cancelEmailNotification
     * </pre>
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) 
    throws IOException, ServletException
    {
        if (!checkServletPath(request, response)) {
            return;
        }  
        //parse the job number and file
        StringBuffer resultsPath = initParsePathInfo(request);
        String jobNumber = parseJobNumber(resultsPath);
        try {
            Integer.parseInt(jobNumber);
        }
        catch (NumberFormatException e) {
            response.setHeader("X-genepattern-JobResultsServletException", "Invalid jobNumber: "+jobNumber);
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
        else {
            response.setHeader("X-genepattern-JobResultsServletException", "Action not available: "+action);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String currentUserId = this.getUserIdFromSession(request);
        
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
            int jobId = -1;
            try {
                jobId = Integer.parseInt(jobNumber);
            }
            catch (NumberFormatException e) {
                response.setHeader("X-genepattern-EmailNotificationException", "Invalid jobNumber: "+jobNumber);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            try {
                sendEmailNotification(request, sendNotification, currentUserId, jobId);
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

    private void serveFile(File fileObj, HttpServletResponse httpServletResponse) 
    throws IOException
    {
        String lcFileName = fileObj.getName().toLowerCase();

        httpServletResponse.setHeader("Content-disposition", "inline; filename=\"" + fileObj.getName() + "\"");
        httpServletResponse.setHeader("Cache-Control", "no-store");
        httpServletResponse.setHeader("Pragma", "no-cache");
        httpServletResponse.setDateHeader("Expires", 0);
        httpServletResponse.setDateHeader("Last-Modified", fileObj.lastModified());
        httpServletResponse.setHeader("Content-Length", "" + fileObj.length());

        if (lcFileName.endsWith(".html") || lcFileName.endsWith(".htm")){
            httpServletResponse.setHeader("Content-Type", "text/html"); 
        }
    
        BufferedInputStream is = null;
        try {
            OutputStream os = httpServletResponse.getOutputStream();
            is = new BufferedInputStream(new FileInputStream(fileObj));
            byte[] b = new byte[10000];
            int bytesRead;
            while ((bytesRead = is.read(b)) != -1) {
                os.write(b, 0, bytesRead);
            }
        }
        catch (FileNotFoundException e) {
            httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
        } 
        finally {
            if (is != null) {
                try {
                    is.close();
                } 
                catch (IOException x) {
                }
            }
        }
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


}



