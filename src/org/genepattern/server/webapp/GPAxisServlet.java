/*
 * Created on Feb 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.genepattern.server.webapp;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.genepattern.server.util.AccessManager;

import org.apache.axis.transport.http.AxisServlet;

/**
 * @author Liefeld
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class GPAxisServlet extends AxisServlet {

	/**
	 *  
	 */
	public GPAxisServlet() {
		super();
		// TODO Auto-generated constructor stub
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		boolean allowed = AccessManager.isAllowed(req.getRemoteHost(), req
				.getRemoteAddr());
		//	System.out.println("doGET allowed=" + allowed + " " +
		// req.getRemoteHost() + " " + req.getRemoteAddr()
		// +"\n=========================");

		if (allowed)
			super.doGet(req, resp);
		else
			resp.sendError(resp.SC_FORBIDDEN);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		boolean allowed = AccessManager.isAllowed(req.getRemoteHost(), req
				.getRemoteAddr());
		//	System.out.println("doPOST allowed=" + allowed + " " +
		// req.getRemoteHost() + " " + req.getRemoteAddr()
		// +"\n=========================");
		if (allowed)
			super.doPost(req, resp);
		else
			resp.sendError(resp.SC_FORBIDDEN);

	}
}