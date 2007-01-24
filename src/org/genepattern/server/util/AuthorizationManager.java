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

package org.genepattern.server.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * @author Liefeld
 * 
 * checks permissions files to see if a given user is allowed to pereform a
 * given action defaults to allowing anything not specifically disallowed
 * 
 * basically answers the question, can userX do action Y and also can return a
 * string representing the link, or a failure string to put in place of the link
 */

public class AuthorizationManager implements IAuthorizationManager {

    public AuthorizationManager() {
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected HashMap<String, HashSet<String>> userGroups = new HashMap<String, HashSet<String>>();

    protected HashMap<String, HashSet<String>> groupUsers = new HashMap<String, HashSet<String>>();

    protected HashMap<String, HashSet<String>> actionPermission = new HashMap<String, HashSet<String>>();

    protected HashMap<String, HashSet<String>> groupPermission = new HashMap<String, HashSet<String>>();

    public String getCheckedLink(String link, String userID, String failureNote) {
        // to pass to isAllowed we want everything before the ?
        int idx = link.indexOf("?");
        String uri = link;
        if (idx >= 0) {
            uri = link.substring(0, idx);
        }

        if (isAllowed(uri, userID))
            return link;
        else
            return failureNote;

    }

    public String getCheckedLink(String permName, String link, String userID, String failureNote) {
        // to pass to isAllowed we want everything before the ?
        // int idx = link.indexOf("?");
        if (checkPermission(permName, userID))
            return link;
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
        HashSet<String> permNames = getPermissionNameForLink(urlOrSoapMethod);
        if (permNames == emptySet)
            return true;

        for (Iterator<String> iter = permNames.iterator(); iter.hasNext();) {
            String permName = iter.next();
            boolean allowed = checkPermission(permName, userID);
            if (allowed)
                return true;
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
        HashSet<String> usersGroups = userGroups.get(userID);
        HashSet<String> openGroups = userGroups.get("*");

        if (usersGroups == null)
            usersGroups = emptySet;
        if (openGroups == null)
            openGroups = emptySet;

        HashSet<String> allowedGroups = groupPermission.get(permissionName);

        // the file says anyone may connect if it has a group named '*'
        // or if no restriction is specified
        if (allowedGroups == null)
            return true;
        if (allowedGroups.contains("*"))
            return true;
        if (allowedGroups == emptySet)
            return true;

        for (Iterator<String> iter = usersGroups.iterator(); iter.hasNext();) {
            String groupName = iter.next();

            if (allowedGroups.contains(groupName))
                return true;
        }
        for (Iterator<String> iter = openGroups.iterator(); iter.hasNext();) {
            String groupName = iter.next();

            if (allowedGroups.contains(groupName))
                return true;
        }

        return false;
    }

    private final HashSet<String> emptySet = new HashSet<String>();

    protected HashSet<String> getPermissionNameForLink(String link) {
        HashSet<String> perms = actionPermission.get(link);
        if (perms == null)
            return emptySet;
        else
            return perms;
    }

    public void init() throws IOException, IllegalArgumentException, IllegalAccessException, NoSuchMethodException,
            SecurityException {
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
        } catch (JDOMException ioe) {
            throw new IOException(ioe.getMessage() + " while reading authorization files");
        }

    }

    public void initActionPermissionMap() throws IOException, JDOMException {
        InputStream is = null;
        org.jdom.Document document = null;

        File actionPermissionMapFile = new File(System.getProperty("genepattern.properties"), "actionPermissionMap.xml");
        if (!actionPermissionMapFile.exists())
            return;

        is = new FileInputStream(actionPermissionMapFile);

        SAXBuilder builder = new SAXBuilder();
        // Parse the specified file and convert it to a JDOM document
        document = builder.build(is);
        Element root = document.getRootElement();

        for (Iterator i = root.getChildren("url").iterator(); i.hasNext();) {
            Element controlledUrl = (Element) i.next();
            String link = controlledUrl.getAttribute("link").getValue();

            HashSet<String> actionPerms = actionPermission.get(link);
            if (actionPerms == null) {
                actionPerms = new HashSet<String>();
                actionPermission.put(link, actionPerms);
            }
            String perm = controlledUrl.getAttribute("permission").getValue();
            actionPerms.add(perm);

        }
        for (Iterator i = root.getChildren("SOAPmethod").iterator(); i.hasNext();) {
            Element controlledUrl = (Element) i.next();
            // XXX
            String meth = controlledUrl.getAttribute("name").getValue();

            HashSet<String> actionPerms = actionPermission.get(meth);
            if (actionPerms == null) {
                actionPerms = new HashSet<String>();
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
        if (!userGroupMapFile.exists())
            return;
        is = new FileInputStream(userGroupMapFile);

        SAXBuilder builder = new SAXBuilder();
        // Parse the specified file and convert it to a JDOM document
        document = builder.build(is);
        Element root = document.getRootElement();

        for (Iterator i = root.getChildren("group").iterator(); i.hasNext();) {
            Element group = (Element) i.next();

            String groupName = group.getAttribute("name").getValue();

            HashSet<String> groupMembers = groupUsers.get(groupName);
            if (groupMembers == null) {
                groupMembers = new HashSet<String>();
                groupUsers.put(groupName, groupMembers);

            }
            for (Iterator i2 = group.getChildren("user").iterator(); i2.hasNext();) {
                Element user = (Element) i2.next();
                String userName = user.getAttribute("name").getValue();
                HashSet<String> usersGroups = userGroups.get(userName);
                if (usersGroups == null) {
                    usersGroups = new HashSet<String>();
                    userGroups.put(userName, usersGroups);
                }

                usersGroups.add(groupName);
                groupMembers.add(userName);
            }
        }
        // loop over SOAP methods next
        is.close();

    }

    public void initPermissionMap() throws IOException, JDOMException {
        InputStream is = null;
        org.jdom.Document document = null;

        File permissionMapFile = new File(System.getProperty("genepattern.properties"), "permissionMap.xml");
        if (!permissionMapFile.exists())
            return;
        is = new FileInputStream(permissionMapFile);

        SAXBuilder builder = new SAXBuilder();
        // Parse the specified file and convert it to a JDOM document
        document = builder.build(is);
        Element root = document.getRootElement();

        for (Iterator i = root.getChildren("permission").iterator(); i.hasNext();) {
            Element permission = (Element) i.next();
            Attribute permissionNameText = (Attribute) permission.getAttribute("name");
            String pName = permissionNameText.getValue();
            HashSet<String> perm = groupPermission.get(pName);
            if (perm == null) {
                perm = new HashSet<String>();
                groupPermission.put(pName, perm);

            }
            for (Iterator i2 = permission.getChildren("group").iterator(); i2.hasNext();) {
                Element group = (Element) i2.next();
                String groupName = group.getAttribute("name").getValue();
                perm.add(groupName);
            }

        }
        // loop over SOAP methods next
        is.close();
    }

}