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
		AuthorizationManager.init();



	}


	protected HashMap userGroups = new HashMap();
	protected HashMap actionPermission = new HashMap();
	protected HashMap userPermission = new HashMap();
	



	public String getCheckedLink(String link, String userID, String failureNote){
		String permission = getPermissionNameForLink(link);
		if (permission == null) return link;
		else return getCheckedLink(permission, link, userID, failureNote);
	}


	public String getCheckedLink(String permission, String link, String userID, String failureNote){
		if (isAllowed(permission, userID)) return link;
		else return failureNote;
	}

	public boolean isAllowed(String urlOrSoapMethod, String userID){
		//convert link name to permission name and then check permission

		return false; //XXX
	}

	public boolean checkPermission(String permissionName, String userID){
		return false; //XXX
	}


	protected String getPermissionNameForLink(String link){
		return null; // XXX
	}







	protected static String DBF = "javax.xml.parsers.DocumentBuilderFactory";

	public static void init() throws IOException, IllegalArgumentException, IllegalAccessException,
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
			throw new IOException(ioe.getMessage() + " while reading authorization files");
		} catch (JDOMException ioe) {
			throw new IOException(ioe.getMessage() + " while reading authorization files");
		} finally {
			if (oldDocumentBuilderFactory != null)
				System.setProperty(DBF, oldDocumentBuilderFactory);
		}

	}
	
	public static void initActionPermissionMap() throws IOException, JDOMException {
		InputStream is = null;
		org.jdom.Document document = null;

		File actionPermissionMapFile = new File(System.getProperty("genepattern.properties"),"actionPermissionMap.xml");
  		is = new FileInputStream(actionPermissionMapFile);

		SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        	// Parse the specified file and convert it to a JDOM document
        	document = builder.build(is);
		Element root = document.getRootElement();
		
 		for(Iterator i = root.getChildren("url").iterator(); i.hasNext(); ) {
            	Element controlledUrl = (Element) i.next();
			// XXX
  			Text link = (Text)root.getChild("link").getContent().get(0);
		      Text perm = (Text)root.getChild("permission").getContent().get(0);
      

		}
 		for(Iterator i = root.getChildren("SOAPmethod").iterator(); i.hasNext(); ) {
            	Element controlledUrl = (Element) i.next();
			// XXX
  			Text meth = (Text)controlledUrl.getChild("name").getContent().get(0);
		      Text perm = (Text)controlledUrl.getChild("permission").getContent().get(0);
      

		}

		// loop over SOAP methods next
		is.close();
	}

	public static void initUserGroupMap() throws IOException, JDOMException {
		InputStream is = null;
		org.jdom.Document document = null;

		File userGroupMapFile = new File(System.getProperty("genepattern.properties"), "userGroups.xml");
  		is = new FileInputStream(userGroupMapFile);

		SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        	// Parse the specified file and convert it to a JDOM document
        	document = builder.build(is);
		Element root = document.getRootElement();
		
 		for(Iterator i = root.getChildren("group").iterator(); i.hasNext(); ) {
            	Element group = (Element) i.next();
			// XXX
  			Text name = (Text)group.getChild("name").getContent().get(0);
		 	for(Iterator i2 = group.getChildren("user").iterator(); i2.hasNext(); ) {
	            	Element user = (Element) i2.next();

	  			Text userName = (Text)user.getChild("name").getContent().get(0);
	
			
			}
		}
		// loop over SOAP methods next
		is.close();
	}
	public static void initPermissionMap() throws IOException, JDOMException {
		InputStream is = null;
		org.jdom.Document document = null;

		File permissionMapFile = new File(System.getProperty("genepattern.properties"), "permissionMap.xml");
  		is = new FileInputStream(permissionMapFile);

		SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        	// Parse the specified file and convert it to a JDOM document
        	document = builder.build(is);
		Element root = document.getRootElement();
		
 		for(Iterator i = root.getChildren("permission").iterator(); i.hasNext(); ) {
            	Element permission = (Element) i.next();
			Text permissionName = (Text)permission.getChild("name").getContent().get(0);
		 	for(Iterator i2 = permission.getChildren("user").iterator(); i2.hasNext(); ) {
	  			Element user = (Element) i2.next();
				Text userName = (Text)user.getChild("name").getContent().get(0);
	
			
			}			
			for(Iterator i2 = permission.getChildren("group").iterator(); i2.hasNext(); ) {
	  			Element group = (Element) i2.next();
				Text groupName = (Text)group.getChild("name").getContent().get(0);
	
			
			}

		}
		// loop over SOAP methods next
		is.close();
	}


}