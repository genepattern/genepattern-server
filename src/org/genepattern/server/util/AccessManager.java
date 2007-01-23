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
package org.genepattern.server.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;
import java.util.Vector;
/**
 * @author Liefeld
 * 
 */
public class AccessManager  {
	protected static String clientList = "";
	protected static Vector allowedClients = null;

	/**
	 *  
	 */
	public static boolean isAllowed(String host, String address) {

		Vector okClients = getAllowedClients();
//		String clientList = System.getProperty("gp.allowed.clients");

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
		String allowedClientList = System.getProperty("gp.allowed.clients");
			

		// refresh on first time through or if something has changed since last time
		//
		boolean refresh = (allowedClients == null);
		if ((clientList == null) && (allowedClientList != null )) refresh=true;
		else if ((clientList != null) && (allowedClientList == null )) refresh = true;
		else {

			if ((clientList == null) && (allowedClientList == null)) refresh = true;
			else if (!(clientList.trim().equals(allowedClientList.trim()))) refresh = true;
		}
		if (refresh ) {
			clientList = System.getProperty("gp.allowed.clients");
			if ((clientList != null)&& (!(clientList.trim().equals("Any")))) {
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

	
}