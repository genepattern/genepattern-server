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
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class AccessManager {
	protected static Vector allowedClients = null;
	/**
	 * 
	 */
	public static boolean isAllowed(String host, String address){
		
		Vector okClients = getAllowedClients();
		String clientList = System.getProperty("gp.allowed.clients");
		
		if (okClients != null){
			for (int i=0;i < okClients.size(); i++){
				String validClient = (String)okClients.get(i);
				if (host.indexOf(validClient)>= 0) return true;
				if (address.indexOf(validClient)>= 0) return true;
			}
			return false;
		}
		return true;
	}

	protected static Vector getAllowedClients(){
		if (allowedClients == null){
			String clientList = System.getProperty("gp.allowed.clients");
			System.out.println("ClientsList=" + clientList);
			if (clientList != null){
				allowedClients = new Vector();				
				StringTokenizer strtok = new StringTokenizer(clientList, ",");
				while (strtok.hasMoreTokens()){
					String tok = strtok.nextToken();
					allowedClients.add(tok);
				}
				try{
				InetAddress addr = InetAddress.getLocalHost();
					String host_address = addr.getCanonicalHostName();
					String host_address2 = addr.getHostAddress();
					allowedClients.add(host_address);
					allowedClients.add(host_address2);
					
				} catch (UnknownHostException uke){
					// do nothing
				}
				
			} else {// null indicates allow anyone
				allowedClients = null;
			}
		}
		return allowedClients;
	}
	
	
}
