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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.net.URLEncoder;
import java.net.URLDecoder;
import org.genepattern.util.IGPConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.*;

/**
 * @author Liefeld
 * 
 * checks permissions files to see if a given user is allowed to pereform a
 * given action defaults to allowing anything not specifically disallowed
 * 
 * basically answers the question, can userX do action Y and also can return a
 * string representing the link, or a failure string to put in place of the link
 */

public class AuthorizationManager implements IAuthorizationManager, IGPConstants {

    public static void main(String[] args) throws Exception {
        // test here
        if (System.getProperty("genepattern.properties") == null) {
            System.setProperty("genepattern.properties", "../resources/");
        }
        (new AuthorizationManager()).init();

    }

    public AuthorizationManager() {
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected HashMap userGroups = new HashMap();
    protected HashMap groupUsers = new HashMap();

    protected HashMap actionPermission = new HashMap();
    protected HashMap groupPermission = new HashMap();

    public String getCheckedLink(String link, String userID, String failureNote) {
        // to pass to isAllowed we want everything before the ?
        int idx = link.indexOf("?");
        String uri = link;
        if (idx >= 0) {
            uri = link.substring(0, idx);
        }

        if (isAllowed(uri, userID)) return link;
        else
            return failureNote;

    }

    public String getCheckedLink(String permName, String link, String userID, String failureNote) {
        // to pass to isAllowed we want everything before the ?
        int idx = link.indexOf("?");
        if (checkPermission(permName, userID)) return link;
        else
            return failureNote;

    }

    public boolean isAllowed(String urlOrSoapMethod, String userID) {
        boolean allow = _isAllowed(urlOrSoapMethod, userID);
        // System.out.println("AM IA: " + urlOrSoapMethod + " --> " + userID + "
        // == " + allow);
        return allow;
    }

    public boolean _isAllowed(String urlOrSoapMethod, String userID) {

        // convert link name to permission name and then check permission
        HashSet permNames = getPermissionNameForLink(urlOrSoapMethod);
        if (permNames == emptySet) return true;

        for (Iterator iter = permNames.iterator(); iter.hasNext();) {
            String permName = (String) iter.next();
            boolean allowed = checkPermission(permName, userID);
            if (allowed) return true;
        }
        return false;
    }

    public boolean checkPermission(String permissionName, String userID) {
        // System.out.println("AM CP: ");

        boolean allow = _checkPermission(permissionName, userID);
        // System.out.println("AM CP: " + permissionName+ " --> " + userID + "
        // == " + allow);
        return allow;

    }

    public boolean _checkPermission(String permissionName, String userID) {
        HashSet usersGroups = (HashSet) userGroups.get(userID);
        HashSet openGroups = (HashSet) userGroups.get("*");

        if (usersGroups == null) usersGroups = emptySet;
        if (openGroups == null) openGroups = emptySet;
        boolean allowed = false;

        HashSet allowedGroups = (HashSet) groupPermission.get(permissionName);

        // the file says anyone may connect if it has a group named '*'
        // or if no restriction is specified
        if (allowedGroups == null) return true;
        if (allowedGroups.contains("*")) return true;
        if (allowedGroups == emptySet) return true;

        // System.out.println("Allowed Groups for " + permissionName + " is " +
        // allowedGroups);
        for (Iterator iter = usersGroups.iterator(); iter.hasNext();) {
            String groupName = (String) iter.next();

            if (allowedGroups.contains(groupName)) return true;
        }
        for (Iterator iter = openGroups.iterator(); iter.hasNext();) {
            String groupName = (String) iter.next();

            if (allowedGroups.contains(groupName)) return true;
        }

        return false;
    }

    private final HashSet emptySet = new HashSet();

    protected HashSet getPermissionNameForLink(String link) {
        HashSet perms = (HashSet) actionPermission.get(link);
        if (perms == null) return emptySet;
        else
            return perms;
    }

    protected static String DBF = "javax.xml.parsers.DocumentBuilderFactory";

    public void init() throws IOException, IllegalArgumentException, IllegalAccessException, NoSuchMethodException,
            SecurityException, ParserConfigurationException, SAXException {
        String oldDocumentBuilderFactory = System.getProperty(DBF);
        try {
            //
            // read from the gp resources directory the following files
            // permissionMap.xml, userGroups.xml, actionPermissionMap.xml
            //
            initPermissionMap();
            initActionPermissionMap();
            initUserGroupMap();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw ioe;
        } finally {
            // if (oldDocumentBuilderFactory != null)
            // System.setProperty(DBF, oldDocumentBuilderFactory);
        }

    }

    public void initActionPermissionMap() throws IOException, ParserConfigurationException, SAXException {

        File actionPermissionMapFile = new File(System.getProperty("genepattern.properties"), "actionPermissionMap.xml");
        if (!actionPermissionMapFile.exists()) return;

        InputStream is = new FileInputStream(actionPermissionMapFile);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(is);
        Element root = document.getDocumentElement();

        NodeList urlNodes = root.getElementsByTagName("url");
        for (int i = 0; i < urlNodes.getLength(); i++) {
            Node controlledUrl = urlNodes.item(i);

            String link = controlledUrl.getAttributes().getNamedItem("link").getNodeValue();

            HashSet actionPerms = (HashSet) actionPermission.get(link);
            if (actionPerms == null) {
                actionPerms = new HashSet();
                actionPermission.put(link, actionPerms);
            }

            String perm = controlledUrl.getAttributes().getNamedItem("permission").getNodeValue();
            actionPerms.add(perm);

        }

        NodeList soapNodes = root.getElementsByTagName("SOAPmethod");
        for (int i = 0; i < soapNodes.getLength(); i++) {
            Node controlledUrl = soapNodes.item(i);

            String meth = controlledUrl.getAttributes().getNamedItem("name").getNodeValue();

            HashSet actionPerms = (HashSet) actionPermission.get(meth);
            if (actionPerms == null) {
                actionPerms = new HashSet();
                actionPermission.put(meth, actionPerms);
            }
            String perm = controlledUrl.getAttributes().getNamedItem("permission").getNodeValue();
            actionPerms.add(perm);
        }

        // loop over SOAP methods next
        is.close();
    }

    public void initUserGroupMap() throws IOException, ParserConfigurationException, SAXException {

        File userGroupMapFile = new File(System.getProperty("genepattern.properties"), "userGroups.xml");
        if (!userGroupMapFile.exists()) return;
        InputStream is = new FileInputStream(userGroupMapFile);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(is);
        Element root = document.getDocumentElement();

        NodeList groupNodes = root.getElementsByTagName("group");
        for (int i = 0; i < groupNodes.getLength(); i++) {
            Element group = (Element) groupNodes.item(i);

            String groupName = group.getAttributes().getNamedItem("name").getNodeValue();

            HashSet groupMembers = (HashSet) groupUsers.get(groupName);
            if (groupMembers == null) {
                groupMembers = new HashSet();
                groupUsers.put(groupName, groupMembers);
            }

            NodeList userNodes = group.getElementsByTagName("user");
            for (int j = 0; j < userNodes.getLength(); j++) {
                Node userNode = userNodes.item(j);
                String userName = userNode.getAttributes().getNamedItem("name").getNodeValue();
                HashSet usersGroups = (HashSet) userGroups.get(userName);
                if (usersGroups == null) {
                    usersGroups = new HashSet();
                    userGroups.put(userName, usersGroups);

                    usersGroups.add(groupName);
                    groupMembers.add(userName);
                }
            }
        }
        // loop over SOAP methods next
        is.close();

        // System.out.println("UG=" + userGroups);
    }

    public void initPermissionMap() throws IOException, ParserConfigurationException, SAXException {

        File permissionMapFile = new File(System.getProperty("genepattern.properties"), "permissionMap.xml");
        if (!permissionMapFile.exists()) return;
        InputStream is = new FileInputStream(permissionMapFile);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(is);
        Element root = document.getDocumentElement();

        NodeList permissionNodes = root.getElementsByTagName("permission");
        for (int i = 0; i < permissionNodes.getLength(); i++) {
            Element permNode = (Element) permissionNodes.item(i);
            Node nameNode = permNode.getAttributes().getNamedItem("name");
            String pName = nameNode.getNodeValue();
            HashSet perm = (HashSet) groupPermission.get(pName);
            if (perm == null) {
                perm = new HashSet();
                groupPermission.put(pName, perm);

            }

            NodeList children = permNode.getElementsByTagName("group");
            for (int j = 0; j < children.getLength(); j++) {
                Node groupNode = children.item(j);
                String groupName = groupNode.getAttributes().getNamedItem("name").getNodeValue();
                perm.add(groupName);

            }

        }
        // loop over SOAP methods next
        is.close();
    }

}