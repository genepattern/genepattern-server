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
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jasper.servlet.JspServlet;
import org.genepattern.server.util.AccessManager;

/**
 * @author Liefeld
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class JSPServlet extends JspServlet {

	/**
	 *  
	 */
	public JSPServlet() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Override the default JSP servlet to allow us to insert some control here
	 * over who gets to see what
	 *  
	 */
	public void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		boolean allowed = AccessManager.isAllowed(req.getRemoteHost(), req
				.getRemoteAddr());

		//System.out.println("allowed=" + allowed + " " + req.getRemoteHost() +
		// " " + req.getRemoteAddr() +"\n=========================");
		if (allowed)
			super.service(req, resp);
		else
			resp.sendRedirect("notallowed.html");

	}

}