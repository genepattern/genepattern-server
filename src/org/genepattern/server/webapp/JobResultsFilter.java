/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/


/*
 * Created on Feb 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.genepattern.server.webapp;

import java.io.IOException;
import java.util.StringTokenizer;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.genepattern.server.util.AccessManager;
import org.genepattern.server.util.AuthorizationManagerFactoryImpl;
import org.genepattern.server.util.IAuthorizationManager;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;

/**
 * @author Liefeld
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class JobResultsFilter implements Filter {
	private FilterConfig filterConfig = null;
   
	public void init(FilterConfig filterConfig) throws ServletException {
      	this.filterConfig = filterConfig;
   	}

   	public void destroy() {
      	this.filterConfig = null;
   	}

   	/**
   	 * security filter on job results pages which are now accessible via normal
   	 * http without using the retrieveResults.jsp page
   	 * 
   	 * In 3.0, you are allowed access to the directory of your own jobs only
   	 * but not the parent dir of your jobs or anyone elses jobs
   	 * 
   	 * This is determined by looking at the URL, taking the job # as whatever follows
   	 * the "jobResults" part in the url
   	 * 
   	 * 
	 *	reqURL: http://gp21e-789.broad.mit.edu:8080/gp/jobResults/92/foo.out
   	 *  contextPath: 	/gp
	 *	reqURI: 		/gp/jobResults/92/foo.out
	 *	ServletPath: 	/jobResults/92/foo.out
	 *
	 * and in genepattern.properties we typically have
	 *  	jobs=./webapps/gp/jobResults
	 * which defines the directory the files are in
	 * 
	 *  In this first implementation, we'll assume the dir name 'jobResults'
	 *  cannot be changed in any GP install
   	 */

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		boolean allowed = true;
		
		IAuthorizationManager authManager  = (new AuthorizationManagerFactoryImpl()).getAuthorizationManager();
		
		String userid = (String)request.getAttribute(GPConstants.USERID);
		String jobDirName = System.getProperty("jobs"); // from genepattern.properties
		boolean isAdmin = authManager.checkPermission("administrateServer", userid);
		
			
		// since this is a dir name with a path, we want to get the path in the application context
		// ie "gp" so...
		// given http://aserver:aport/gp/jobResults/92/foo.txt
		// we want to get the "92" as the job #
		
		HttpServletRequest hsr = (HttpServletRequest)request;
		String servletPath = hsr.getServletPath();
		int idx = servletPath.indexOf("jobResults");
		if (idx == -1)((HttpServletResponse)response).sendError(((HttpServletResponse)response).SC_FORBIDDEN);
		
		String resultsPath = servletPath.substring(idx + 1 + ("jobResults".length()));
		
		
		StringTokenizer strtok = new StringTokenizer(resultsPath,"/");
		String job = null;
		String file = null;
		if (strtok.hasMoreTokens())
			job = strtok.nextToken();
		if (strtok.hasMoreTokens())
			file = strtok.nextToken();

		if (isAdmin){
			allowed = true;
		} else if (job == null){
			// should admin be allowed here? or anyone if no passwords?
			allowed = false;
		} else if (isJobOwner(userid, job)){
			allowed=true;
		}
		
		if (allowed){
			chain.doFilter(request, response);
			return;
		} else {
			((HttpServletResponse)response).sendError(((HttpServletResponse)response).SC_FORBIDDEN);
		}
   }

	  private boolean isJobOwner(String user, String jobId){
		  try {
			  int jobID = Integer.parseInt(jobId);
	
			  AnalysisDAO ds = new AnalysisDAO();
			  JobInfo jobInfo = ds.getJobInfo(jobID);
			  return user.equals(jobInfo.getUserId());
		  } catch (NumberFormatException nfe) {
			  return false;
		  }
		 
	  }
	    


}