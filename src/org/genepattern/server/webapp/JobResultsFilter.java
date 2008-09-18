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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.server.JobIDNotFoundException;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;

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
 * contextPath: /gp reqURI: /gp/jobResults/92/foo.out ServletPath:
 * /jobResults/92/foo.out
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
public class JobResultsFilter implements Filter {

    private String jobsDirectory;

    private static Logger log = Logger.getLogger(JobResultsFilter.class);

    private void loadProperties(Properties props, File propFile) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(propFile);
            props.load(fis);

        } catch (IOException e) {
            log.error(e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {

                }
            }
        }

    }

    public void init(FilterConfig filterconfig) throws ServletException {
        String dir = filterconfig.getInitParameter("genepattern.properties");
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

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
    throws IOException, ServletException 
    {
        String userid = (String) request.getAttribute(GPConstants.USERID);
        String servletPath = ((HttpServletRequest) request).getServletPath();
       
        //valid servlet paths:
        // <contextpath>/jobResults
        // <contextpath>/jobResults/
        // <contextpath>/jobResults/<job>
        // <contextpath>/jobResults/<job>/
        // <contextpath>/jobResults/<job>/<file>

        if (!servletPath.startsWith("/jobResults")) {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String resultsPath = servletPath.substring("/jobResults".length());
        if (resultsPath.length() > 0) {
            if ('/' != resultsPath.charAt(0)) {
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            resultsPath = resultsPath.substring(1);
        }
        
        //parse the job number and file
        String job = null;
        String file = null;
        if (resultsPath != null && resultsPath.length() > 0) {
            int idx = resultsPath.indexOf('/');
            if (idx < 0) {
                job = resultsPath;
            }
            else {
                job = resultsPath.substring(0, idx);
                //remove '/'
                file = resultsPath.substring(idx+1);
                if (file.length() == 0) {
                    file = null;
                }
            }
        }
        
        //special case: list all job results
        if (job == null) {
            //((HttpServletResponse) response).sendRedirect( contextPath + "/pages/jobResults.jsf" );
            //return;
            //i'd like to use this to eliminate the .jfs from the url but it causes problems on Sign out
            //    for reasons unknown to me, if I use the request dispatcher to include a jsf page,
            //    when the user 'Signs out' a JobInfoBean is instantiated.
            RequestDispatcher rd = request.getRequestDispatcher("/pages/jobResults.jsf");
            rd.include(request, response);
            chain.doFilter(request, response);
            return;
        }

        boolean allowed = false;
        try {
            int jobID = Integer.parseInt(job);
            AnalysisDAO ds = new AnalysisDAO();
            JobInfo jobInfo = ds.getJobInfo(jobID);
            allowed = userid != null && userid.equals(jobInfo.getUserId());
        }
        catch (NumberFormatException e) {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid jobid: "+job);
            return;
        }
        catch (JobIDNotFoundException e) {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, "Job not found: "+job);
            return;
        }

        if (allowed && file == null) {
            String jsfPage = "/pages/jobResult.jsf?jobNumber="+job;
            //((HttpServletResponse) response).sendRedirect( contextPath + jsfPage );
            RequestDispatcher rd = request.getRequestDispatcher(jsfPage);
            rd.include(request, response);
            chain.doFilter(request, response);
            return;
        }

        if (allowed) {
            File fileObj = new File(jobsDirectory + File.separator + job + File.separator + file);
            String lcFileName = fileObj.getName().toLowerCase();
            
            HttpServletResponse httpServletResponse = (HttpServletResponse) response;
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
                OutputStream os = response.getOutputStream();
                is = new BufferedInputStream(new FileInputStream(fileObj));
                byte[] b = new byte[10000];
                int bytesRead;
                while ((bytesRead = is.read(b)) != -1) {
                    os.write(b, 0, bytesRead);
                }
            } catch (FileNotFoundException e) {
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException x) {
                    }
                }
            }
        } else {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    public void destroy() {
    }
}
