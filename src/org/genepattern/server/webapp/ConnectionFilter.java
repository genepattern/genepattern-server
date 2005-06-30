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
public class ConnectionFilter implements Filter {
	private FilterConfig filterConfig = null;
   
	public void init(FilterConfig filterConfig) throws ServletException {
      	this.filterConfig = filterConfig;
   	}

   	public void destroy() {
      	this.filterConfig = null;
   	}


	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

      	boolean allowed = AccessManager.isAllowed(request.getRemoteHost(), request.getRemoteAddr());



		if (allowed){
			chain.doFilter(request, response);
			return;
		} else {
			if (isJspCall((HttpServletRequest)request)){
				((HttpServletResponse)response).sendRedirect("notallowed.html");
			} else {
				((HttpServletResponse)response).sendError(((HttpServletResponse)response).SC_FORBIDDEN);
			}

		}
   }

   	public boolean isJspCall(HttpServletRequest request){
		return (request.getRequestURI().indexOf(".jsp") >= 0);

	}


}