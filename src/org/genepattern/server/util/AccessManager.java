/*
 * Created on Feb 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.genepattern.server.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import java.net.URLEncoder;
import java.net.URLDecoder;
import org.genepattern.util.IGPConstants;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
/**
 * @author Liefeld
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class AccessManager implements IGPConstants {
	protected static Vector allowedClients = null;

	/**
	 *  
	 */
	public static boolean isAllowed(String host, String address) {

		Vector okClients = getAllowedClients();
		String clientList = System.getProperty("gp.allowed.clients");

		if (okClients != null) {
			for (int i = 0; i < okClients.size(); i++) {
				String validClient = (String) okClients.get(i);
				if (host.indexOf(validClient) >= 0)
					return true;
				if (address.indexOf(validClient) >= 0)
					return true;
			}
			return false;
		}
		return true;
	}

	protected static Vector getAllowedClients() {
		if (allowedClients == null) {
			String clientList = System.getProperty("gp.allowed.clients");
			if (clientList != null) {
				allowedClients = new Vector();
				StringTokenizer strtok = new StringTokenizer(clientList, ",");
				while (strtok.hasMoreTokens()) {
					String tok = strtok.nextToken();
					allowedClients.add(tok);
				}
				allowedClients.add("127.0.0.1");// so that you can always get in
												// locally
				allowedClients.add("localhost");// so that you can always get in
												// locally

				try {
					InetAddress addr = InetAddress.getLocalHost();
					String host_address = addr.getCanonicalHostName();
					String host_address2 = addr.getHostAddress();
					allowedClients.add(host_address);// so that you can always
													 // get in locally
					allowedClients.add(host_address2);// so that you can always
													  // get in locally

				} catch (UnknownHostException uke) {
					// do nothing
				}

			} else {// null indicates allow anyone
				allowedClients = null;
			}
		}
		return allowedClients;
	}

	/**
	 * returns the userID value extracted from an HTTP cookie. If the user is
	 * unidentified, this method sends a redirect to the browser to request the
	 * user to login, and the login page will then redirect the user back to the
	 * original page with a valid userID now known.
	 * 
	 * @param request
	 *            HttpServletRequest containing a cookie with userID (if they
	 *            are logged in)
	 * @param response
	 *            HttpServletResponse which is used to redirect the browser if
	 *            the user is not logged in yet
	 * @return String userID after login
	 * @author Jim Lerner
	 *  
	 */
	public static String getUserID(HttpServletRequest request, HttpServletResponse response) {
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

		if ((userID == null || userID.length() == 0) && response != null) {
			// redirect to the fully-qualified host name to make sure that the
			// one cookie that we are allowed to write is useful
			try {
				String fqHostName = System.getProperty("fqHostName");
				if (fqHostName == null) {
					fqHostName = InetAddress.getLocalHost()
							.getCanonicalHostName();
					if (fqHostName.equals("localhost"))
						fqHostName = "127.0.0.1";
				}
				String serverName = request.getServerName();
				if (!fqHostName.equalsIgnoreCase(serverName)) {
					String URL = request.getRequestURI();
					if (request.getQueryString() != null)
						URL = URL + ("?" + request.getQueryString());
					String fqAddress = "http://" + fqHostName + ":"
							+ request.getServerPort() + "/gp/login.jsp?origin="
							+ URLEncoder.encode(URL, UTF8);
					response.sendRedirect(fqAddress);
					return null;
				}
				response.sendRedirect("login.jsp");
				return null;
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
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
		}
		return userID;
	}


}