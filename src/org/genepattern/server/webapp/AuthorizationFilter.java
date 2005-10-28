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

import java.lang.reflect.*;


import org.genepattern.util.IGPConstants;
import org.apache.jasper.servlet.JspServlet;
import org.genepattern.server.util.AuthorizationManagerFactoryImpl;
import org.genepattern.server.util.IAuthorizationManager;


/**
 * @author Liefeld
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class AuthorizationFilter implements Filter, IGPConstants {
	private FilterConfig filterConfig = null;
   	private IAuthorizationManager authManager = null;

	public void init(FilterConfig filterConfig) throws ServletException {
		try {
	      	this.filterConfig = filterConfig;

			String gpprops = filterConfig.getInitParameter("genepattern.properties");
			System.setProperty("genepattern.properties", gpprops);		

			authManager  = (new AuthorizationManagerFactoryImpl()).getAuthorizationManager();

//			String className = filterConfig.getInitParameter("org.genepattern.AuthorizationManagerFactory");
//			Class cl = Class.forName(className);
//			Constructor construct = cl.getConstructor(new Class[0]);
//		
//			IAuthorizationManagerFactory factory = (IAuthorizationManagerFactory)construct.newInstance(new Object[0]);
//
//			authManager = factory.getAuthorizationManager();

		} catch (Exception e){
			throw new ServletException(e);
		}
   	}

   	public void destroy() {
      	this.filterConfig = null;
		authManager = null;
   	}


	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      	//if (filterConfig == null)
      	//   return;
		HttpServletRequest req = (HttpServletRequest) request;
      	String requestedURI = req.getRequestURI();

		String rh= req.getRemoteHost();
		String p = req.getParameter("jsp_precompile");
		int numParams = req.getParameterMap().keySet().size();

		// allow jsp precompilation	
		if ((p != null) && ("localhost".equals(rh)) && (numParams == 1)){
			chain.doFilter(request, response);
			return;
		}

/*		System.out.println("AUTH FILTER=" + requestedURI);
		System.out.println("\tmeth=" + req.getMethod());
		System.out.println("\tQS=" + req.getQueryString());
		Enumeration enum = req.getHeaderNames();
		while (enum.hasMoreElements()) {
			String na = (String)enum.nextElement();
			String he = req.getHeader(na);
			System.out.println("\t\t" + na + "  =  " + he);
		}
*/

		String userId = (String)request.getAttribute("userID");
		String uri = req.getRequestURI();
		int idx = uri.lastIndexOf("/");
		uri = uri.substring(idx+1);

		// check permission
		boolean allowed = authManager.isAllowed(uri, userId);

		if (!allowed) { // not allowed to do this
			setNotPermittedPageRedirect((HttpServletRequest)request, (HttpServletResponse )response);
			return;
		} else { // looking for userID
			chain.doFilter(request, response);
			return;
		}
   }



	public void setNotPermittedPageRedirect(HttpServletRequest request, HttpServletResponse response){

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
			String fqAddress = "http://" + fqHostName + ":"	+ request.getServerPort() + "/gp/notpermitted.jsp?link=" + URLEncoder.encode(request.getRequestURI(), UTF8);


			response.sendRedirect(fqAddress);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}


}