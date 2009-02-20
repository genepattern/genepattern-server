/*******************************************************************************
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright (2003-2008) by the
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
import org.genepattern.server.PermissionsHelper;
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

    public void doGet(HttpServletRequest request, HttpServletResponse response) 
    throws IOException, ServletException
    {
        String useridFromSession = null;
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object obj = session.getAttribute(GPConstants.USERID);
            if (obj instanceof String) {
                useridFromSession = (String) obj;
            }
        }

        String servletPath = request.getServletPath();
        if (!"/jobResults".equals(servletPath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
       
        //valid servlet paths:
        // /jobResults
        // /jobResults/
        // /jobResults/<job>
        // /jobResults/<job>/
        // /jobResults/<job>?returnType=JSON
        // /jobResults/<job>/?returnType=JSON
        // /jobResults/<job>/<file>

        String resultsPath = request.getPathInfo();
        if (resultsPath != null && resultsPath.length() > 0) {
            if ('/' != resultsPath.charAt(0)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            resultsPath = resultsPath.substring(1);
        }
        
        //parse the job number and file
        String jobNumber = null;
        String file = null;
        if (resultsPath != null && resultsPath.length() > 0) {
            int idx = resultsPath.indexOf('/');
            if (idx < 0) {
                jobNumber = resultsPath;
            }
            else {
                jobNumber = resultsPath.substring(0, idx);
                //remove '/'
                file = resultsPath.substring(idx+1);
                if (file.length() == 0) {
                    file = null;
                }
            }
        }
        
        //special case: list all job results
        if (jobNumber == null) {
            RequestDispatcher rd = this.getServletContext().getRequestDispatcher("/pages/jobResults.jsf");
            rd.forward(request, response);
            return;
        }

        boolean allowed = false;
        int jobID = -1;
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
                //return json formatted version of job num
                JobInfoManager m = new JobInfoManager();
                JobInfoManager.MyJobInfo myJobInfo = m.getJobInfo(useridFromSession, jobID);

                try {
                    response.setContentType("text/html");
                    response.setHeader("Cache-Control", "no-cache");

                    m.writeJobInfo(response.getWriter(), myJobInfo);
                    
                    response.getWriter().flush();
                    response.getWriter().close();
                    
                    return;
                }
                catch (JSONException e) {
                    log.error("Error in ajax request for job info: "+e.getMessage(), e);
                }
            }
            RequestDispatcher rd = this.getServletContext().getRequestDispatcher("/pages/jobResult.jsf?jobNumber="+jobNumber);
            rd.forward(request, response);
            return;
        }

        File fileObj = new File(jobsDirectory + File.separator + jobNumber + File.separator + file);
        serveFile(fileObj, response);
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

}
