/*
 * Created on Feb 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.genepattern.server.webapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeMap;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.FilterChain;
import javax.servlet.http.Cookie;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URLEncoder;
import java.net.URLDecoder;

import org.genepattern.util.IGPConstants;
import org.apache.jasper.servlet.JspServlet;
import org.genepattern.server.util.AccessManager;

/**
 * @author Liefeld
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class AuthenticationFilter implements Filter, IGPConstants {
	private FilterConfig filterConfig = null;
   
	public void init(FilterConfig filterConfig) throws ServletException {
      	this.filterConfig = filterConfig;
   	}

   	public void destroy() {
      	this.filterConfig = null;
   	}


	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      	//if (filterConfig == null)
      	//   return;
		HttpServletRequest req = (HttpServletRequest) request;
      	String requestedURI = req.getRequestURI();
		String stuff = req.getRequestURI();
	//	System.out.println("FILTER=" + requestedURI);
		

		String rh= req.getRemoteHost();
		String p = req.getParameter("jsp_precompile");
		int numParams = req.getParameterMap().keySet().size();

		// allow jsp precompilation	
		if ((p != null) && ("localhost".equals(rh)) && (numParams == 1)){
			chain.doFilter(request, response);
			return;
		}

		String fqHostName = System.getProperty("fullyQualifiedHostName");
		if (fqHostName == null) fqHostName = InetAddress.getLocalHost().getCanonicalHostName();
		if (fqHostName.equals("localhost")) fqHostName = "127.0.0.1";
		String serverName = request.getServerName();
		if (!fqHostName.equalsIgnoreCase(serverName)) {
			redirectToFullyQualifiedHostName((HttpServletRequest)request, (HttpServletResponse )response);
			return;
		}

		String userId = null;
		boolean isLogin = requestedURI.indexOf("login.jsp") >= 0;
		boolean isResultFetch = (requestedURI.indexOf("retrieveResults.jsp") >= 0) || (requestedURI.indexOf("getFile.jsp") >= 0);

		if (!(isLogin || isResultFetch )) {

			userId = _getUserID((HttpServletRequest) request);	  
			//System.out.println("UserID=" + userId);    
			if (userId == null){	
				setLoginPageRedirect((HttpServletRequest)request, (HttpServletResponse )response);
				return ;
			}
			request.setAttribute("userID", userId);
			chain.doFilter(request, response);
		} else { // looking for userID
			//System.out.println("Filter: Redirecting to login");
			chain.doFilter(request, response);
			return;
		}
   }


	public String _getUserID(HttpServletRequest request) {
		String userID = null;
		if (request.getAttribute(USER_LOGGED_OFF) != null) {
			return userID;
		}
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				if (cookies[i].getName().equals(USERID)) {
					userID = cookies[i].getValue();
					if (userID.length() > 0) {
						break;
					}
				}
			}
		}

		if ((userID == null || userID.length() == 0)
				&& request.getParameter(USERID) != null) {
			userID = request.getParameter(USERID);
		}

		if (userID != null) {
			// strip surrounding quotes, if they exist
			if (userID.startsWith("\"")) {
				userID = userID.substring(1, userID.length() - 1);
				try {
					userID = URLDecoder.decode(userID, UTF8);
				} catch (UnsupportedEncodingException uee) { /* ignore */
				}
			}
			if (userID.length() == 0) userID = null;
		} 		
		return userID;
	}



	public void setLoginPageRedirect(HttpServletRequest request, HttpServletResponse response){

		String URL = request.getRequestURI();

		if (response == null) return;


		// redirect to the fully-qualified host name to make sure that the
		// one cookie that we are allowed to write is useful
		try {
			String fqHostName = System.getProperty("fqHostName");
			if (fqHostName == null) {
				fqHostName = InetAddress.getLocalHost().getCanonicalHostName();
				if (fqHostName.equals("localhost"))	fqHostName = "127.0.0.1";
			}
			String serverName = request.getServerName();
			
			if (request.getQueryString() != null) URL = URL + ("?" + request.getQueryString());
			String fqAddress = request.getScheme() +"://" + fqHostName + ":"	+ request.getServerPort() + "/gp/login.jsp?origin="+ URLEncoder.encode(URL, UTF8);


			response.sendRedirect(fqAddress);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

public void redirectToFullyQualifiedHostName(HttpServletRequest request, HttpServletResponse response){
	try {
		String fqHostName = System.getProperty("fullyQualifiedHostName");
		if (fqHostName == null) fqHostName = InetAddress.getLocalHost().getCanonicalHostName();
		if (fqHostName.equals("localhost")) fqHostName = "127.0.0.1";


		String queryString = request.getQueryString();
		if (queryString == null) {
			queryString = "";
		} else {
			queryString = "?" + queryString;
		}
		String fqAddress = request.getScheme() +"://" + fqHostName + ":" + request.getServerPort() + request.getRequestURI() + queryString;
		response.sendRedirect(fqAddress);
		return;
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

	}


}