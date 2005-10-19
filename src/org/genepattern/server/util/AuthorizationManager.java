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
import java.io.*;
import java.util.*;

import org.jdom.*;
import org.jdom.input.SAXBuilder;



/**
 * @author Liefeld
 * 
 * checks permissions files to see if a given user is allowed to pereform a given action
 * defaults to allowing anything not specifically disallowed
 *
 * basically answers the question, can userX do action Y
 * and also can return a string representing the link, or a failure string to put in place of the link
 */

public class AuthorizationManager implements IAuthorizationManager, IGPConstants {
	  

	public static void main(String[] args) throws Exception{
		// test here
		if (System.getProperty("genepattern.properties") == null){
			System.setProperty("genepattern.properties", "c:/progra~1/genepatternserver/resources/");
		}
		(new AuthorizationManager()).init();



	}


	public AuthorizationManager(){
		try {
			init();
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	protected HashMap userGroups = new HashMap();
	protected HashMap groupUsers = new HashMap();

	protected HashMap actionPermission = new HashMap();
	protected HashMap groupPermission = new HashMap();
	


	public String getCheckedLink(String link, String userID, String failureNote){
		// to pass to isAllowed we want everything before the ?
		int idx = link.indexOf("?");
		String uri = link;
		if (idx >= 0) {
			uri = link.substring(0, idx);
		} 

		if (isAllowed(uri, userID)) return link;
		else return failureNote;

	}
	public String getCheckedLink(String permName, String link, String userID, String failureNote){
		// to pass to isAllowed we want everything before the ?
		int idx = link.indexOf("?");
		if (checkPermission(permName, userID)) return link;
		else return failureNote;

	}

	public boolean isAllowed(String urlOrSoapMethod, String userID){
		boolean allow = _isAllowed(urlOrSoapMethod,userID);
		System.out.println("AM IA: " + urlOrSoapMethod + " --> " + userID + "  == " + allow);
		return allow;		
	}

	public boolean _isAllowed(String urlOrSoapMethod, String userID){

		//convert link name to permission name and then check permission
		HashSet permNames = getPermissionNameForLink(urlOrSoapMethod);
		if (permNames == emptySet) return true;

		for (Iterator iter = permNames.iterator(); iter.hasNext(); ){
			String permName = (String)iter.next();
			boolean allowed = checkPermission(permName, userID); 
			if (allowed) return true;
		}
		return false;
	}

	public boolean checkPermission(String permissionName, String userID){
		System.out.println("AM CP: ");

		boolean allow = _checkPermission(permissionName,userID);
		System.out.println("AM CP: " + permissionName+ " --> " + userID + "  == " + allow);
		return allow;	

	}
	public boolean _checkPermission(String permissionName, String userID){
		HashSet usersGroups = (HashSet )userGroups.get(userID);
		HashSet openGroups = (HashSet )userGroups.get("*");
		
		if (usersGroups == null) usersGroups = emptySet;
		if (openGroups == null) openGroups = emptySet;
		boolean allowed = false;		

		HashSet allowedGroups = (HashSet)groupPermission.get(permissionName);

		// the file says anyone may connect if it has a group named '*'
		// or if no restriction is specified
		if (allowedGroups == null) return true;
		if (allowedGroups.contains("*")) return true;
		if (allowedGroups == emptySet) return true;

System.out.println("Allowed Groups for " + permissionName + " is " + allowedGroups); 
		for (Iterator iter = usersGroups.iterator(); iter.hasNext(); ){
			String groupName = (String)iter.next();

			if (allowedGroups.contains(groupName)) return true;
		}
		for (Iterator iter = openGroups.iterator(); iter.hasNext(); ){
			String groupName = (String)iter.next();

			if (allowedGroups.contains(groupName)) return true;
		}

		return false;
	}

	private final HashSet emptySet = new HashSet();

	protected HashSet getPermissionNameForLink(String link){
		HashSet perms =  (HashSet)actionPermission.get(link);
		if (perms == null) return emptySet;
		else return perms;
	}


	protected static String DBF = "javax.xml.parsers.DocumentBuilderFactory";

	public void init() throws IOException, IllegalArgumentException, IllegalAccessException,
			NoSuchMethodException, SecurityException {
		String oldDocumentBuilderFactory = System.getProperty(DBF);
		try {
			//
			// read from the gp resources directory the following files
			// permissionMap.xml, userGroups.xml, actionPermissionMap.xml
			//
			System.setProperty(DBF,	"org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
			initPermissionMap();
			initActionPermissionMap();
			initUserGroupMap();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw ioe;
		} catch (JDOMException ioe) {
			throw new IOException(ioe.getMessage() + " while reading authorization files");
		} finally {
			if (oldDocumentBuilderFactory != null)
				System.setProperty(DBF, oldDocumentBuilderFactory);
		}

	}
	
	public void initActionPermissionMap() throws IOException, JDOMException {
		InputStream is = null;
		org.jdom.Document document = null;

		File actionPermissionMapFile = new File(System.getProperty("genepattern.properties"),"actionPermissionMap.xml");
		if (!actionPermissionMapFile.exists()) return;

  		is = new FileInputStream(actionPermissionMapFile);

		SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        	// Parse the specified file and convert it to a JDOM document
        	document = builder.build(is);
		Element root = document.getRootElement();
		
		for(Iterator i = root.getChildren("url").iterator(); i.hasNext(); ) {
            	Element controlledUrl = (Element) i.next();
			String link = controlledUrl.getAttribute("link").getValue();

			HashSet actionPerms = (HashSet)actionPermission.get(link);
			if (actionPerms == null) {
				actionPerms = new HashSet();
				actionPermission.put(link, actionPerms);
			}
		      String perm = controlledUrl.getAttribute("permission").getValue();
      		actionPerms.add(perm);

		}
 		for(Iterator i = root.getChildren("SOAPmethod").iterator(); i.hasNext(); ) {
            	Element controlledUrl = (Element) i.next();
			// XXX
  			String meth = controlledUrl.getAttribute("name").getValue();
		      
			HashSet actionPerms = (HashSet)actionPermission.get(meth);
			if (actionPerms == null) {
				actionPerms = new HashSet();
				actionPermission.put(meth, actionPerms);
			}
			String perm = controlledUrl.getAttribute("permission").getValue();
      		actionPerms.add(perm);
		}

		// loop over SOAP methods next
		is.close();
	}

	public void initUserGroupMap() throws IOException, JDOMException {
		InputStream is = null;
		org.jdom.Document document = null;

		File userGroupMapFile = new File(System.getProperty("genepattern.properties"), "userGroups.xml");
		if (!userGroupMapFile.exists()) return; 
 		is = new FileInputStream(userGroupMapFile);

		SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        	// Parse the specified file and convert it to a JDOM document
        	document = builder.build(is);
		Element root = document.getRootElement();
		
 		for(Iterator i = root.getChildren("group").iterator(); i.hasNext(); ) {
            	Element group = (Element) i.next();

  			String groupName = group.getAttribute("name").getValue();

			HashSet groupMembers = (HashSet )groupUsers.get(groupName);
			if (groupMembers == null){
				groupMembers = new HashSet ();
				groupUsers.put(groupName, groupMembers);

			}
		 	for(Iterator i2 = group.getChildren("user").iterator(); i2.hasNext(); ) {
	            	Element user = (Element) i2.next();
	  			String userName = user.getAttribute("name").getValue();
				HashSet usersGroups = (HashSet )userGroups.get(userName);
				if (usersGroups== null){
					usersGroups= new HashSet ();
					userGroups.put(userName, usersGroups);
				}
	
				usersGroups.add(groupName);
				groupMembers.add(userName);
			}
		}
		// loop over SOAP methods next
		is.close();

System.out.println("UG=" + userGroups);
	}
	public void initPermissionMap() throws IOException, JDOMException {
		InputStream is = null;
		org.jdom.Document document = null;

		File permissionMapFile = new File(System.getProperty("genepattern.properties"), "permissionMap.xml");
  		if (!permissionMapFile.exists()) return;
		is = new FileInputStream(permissionMapFile);

		SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        	// Parse the specified file and convert it to a JDOM document
        	document = builder.build(is);
		Element root = document.getRootElement();
		
 		for(Iterator i = root.getChildren("permission").iterator(); i.hasNext(); ) {
            	Element permission = (Element) i.next();
			Attribute permissionNameText = (Attribute)permission.getAttribute("name");
			String pName = permissionNameText.getValue();
			HashSet perm = (HashSet)groupPermission.get(pName);
			if (perm == null){
				perm = new HashSet();
				groupPermission.put(pName, perm);

			}
			for(Iterator i2 = permission.getChildren("group").iterator(); i2.hasNext(); ) {
	  			Element group = (Element) i2.next();
				String groupName = group.getAttribute("name").getValue();
				perm.add(groupName);
			}

		}
		// loop over SOAP methods next
		is.close();
	}


}