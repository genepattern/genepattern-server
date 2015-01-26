/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
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
import java.util.Set;

import org.genepattern.server.UserAccountManager;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * @author Liefeld
 * 
 * checks permissions files to see if a given user is allowed to perform a
 * given action defaults to allowing anything not specifically disallowed
 * 
 * basically answers the question, can userX do action Y and also can return a
 * string representing the link, or a failure string to put in place of the link
 */

public class AuthorizationManager implements IAuthorizationManager {

    public AuthorizationManager(final File resourcesDir) {
        try {
            init(resourcesDir);
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final HashSet<String> emptySet = new HashSet<String>();
    private HashMap<String, HashSet<String>> actionPermission = new HashMap<String, HashSet<String>>();
    private HashMap<String, HashSet<String>> groupPermission = new HashMap<String, HashSet<String>>();

    public String getCheckedLink(String link, String userID, String failureNote) {
        // to pass to isAllowed we want everything before the ?
        int idx = link.indexOf("?");
        String uri = link;
        if (idx >= 0) {
            uri = link.substring(0, idx);
        }

        if (isAllowed(uri, userID)) {
            return link;
        }
        else {
            return failureNote;
        }
    }

    public String getCheckedLink(String permName, String link, String userID, String failureNote) {
        if (checkPermission(permName, userID)) {
            return link;
        }
        else {
            return failureNote;
        }
    }

    public boolean isAllowed(String urlOrSoapMethod, String userID) {
        // convert link name to permission name and then check permission
        HashSet<String> permNames = getPermissionNameForLink(urlOrSoapMethod);
        if (permNames.size() == 0) {
            return true;
        }
        for (Iterator<String> iter = permNames.iterator(); iter.hasNext();) {
            String permName = iter.next();
            boolean allowed = checkPermission(permName, userID);
            if (allowed) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check permission map.
     * @return true if user has specified privilege (permissionName), false otherwise.
     */
    public boolean checkPermission(String permissionName, String userID) {
        HashSet<String> allowedGroups = groupPermission.get(permissionName);

        // the file says anyone may connect if it has a group named '*'
        // or if no restriction is specified
        if (allowedGroups == null || allowedGroups.size() == 0) {
            return true;
        }
        if (allowedGroups.contains("*")) {
            return true;
        }

        Set<String> usersGroups = UserAccountManager.instance().getGroupMembership().getGroups(userID);

        if (usersGroups == null) {
            return false;
        }
        for(String groupName : usersGroups) {
            if (allowedGroups.contains(groupName)) {
                return true;
            }            
        }
        return false;
    }


    protected HashSet<String> getPermissionNameForLink(String link) {
        HashSet<String> perms = actionPermission.get(link);
        if (perms == null) {
            return emptySet;
        }
        else {
            return perms;
        }
    }

    public void init(final File resourcesDir) throws IOException {
        try {
            //
            // read from the gp resources directory the following files
            // permissionMap.xml, actionPermissionMap.xml
            //
            initPermissionMap(resourcesDir);
            initActionPermissionMap(resourcesDir);
        } 
        catch (IOException ioe) {
            ioe.printStackTrace();
            throw ioe;
        } 
        catch (JDOMException ioe) {
            throw new IOException(ioe.getMessage() + " while reading authorization files");
        }
    }

    public void initActionPermissionMap(final File resourcesDir) throws IOException, JDOMException {
        InputStream is = null;
        org.jdom.Document document = null;

        File actionPermissionMapFile = new File(resourcesDir, "actionPermissionMap.xml");
        
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

    public void initPermissionMap(final File resourcesDir) throws IOException, JDOMException {
        InputStream is = null;
        org.jdom.Document document = null;

        File permissionMapFile = new File(resourcesDir, "permissionMap.xml");
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
