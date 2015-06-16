/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.auth;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * Implement GenePattern group membership by parsing an xml file at server startup.
 * Based on AuthorizationManger code from GP 3.1.1 (and earlier).
 * 
 * @author pcarr
 */
public class XmlGroupMembership extends DefaultGroupMembership {
    private static Logger log = Logger.getLogger(XmlGroupMembership.class);
    
    /**
     * Create a new instance by reading the userGroups.xml file from the genepattern.properties directory.
     */
    public XmlGroupMembership() {
        this((File)null);
    }

    /**
     * Initialize group membership from a file.
     * @param userGroupsXmlFile - if this is null, use the default location for the userGroups.xml file.
     */
    public XmlGroupMembership(File userGroupsXmlFile) {
        super();
        if (userGroupsXmlFile == null) {
            userGroupsXmlFile = new File(ServerConfigurationFactory.instance().getResourcesDir(), "userGroups.xml");
        }
        try {
            initUserGroupMap(userGroupsXmlFile);
        }
        catch (IOException e) {
            log.error("Didn't initialize group access permissions: "+e.getLocalizedMessage(), e);
        }
        catch (JDOMException e) {
            log.error("Didn't initialize group access permissions: "+e.getLocalizedMessage(), e);
        }
    }

    /**
     * Initialize group membership from an InputStream.
     * @param in
     */
    public XmlGroupMembership(InputStream in) {
        try {
            initUserGroupMap(in);
        }
        catch (IOException e) {
            log.error("Didn't initialize group access permissions: "+e.getLocalizedMessage(), e);
        }
        catch (JDOMException e) {
            log.error("Didn't initialize group access permissions: "+e.getLocalizedMessage(), e);
        }
    }

    private void initUserGroupMap(File userGroupsXmlFile) throws IOException, JDOMException {
        InputStream is = null;
        if (!userGroupsXmlFile.exists()) {
            log.error("Didn't initialize group access permissions! File doesn't exist: "+userGroupsXmlFile.getAbsolutePath());
            return;
        }
        is = new FileInputStream(userGroupsXmlFile);
        initUserGroupMap(is);
    }
    
    private void initUserGroupMap(InputStream is)  throws IOException, JDOMException {
        // Parse the input into a JDOM document
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(is);
        Element root = document.getRootElement();

        for (Iterator i = root.getChildren("group").iterator(); i.hasNext();) {
            Element groupElem = (Element) i.next();
            String groupName = groupElem.getAttribute("name").getValue();
            addGroup(groupName);
            for (Iterator i2 = groupElem.getChildren("user").iterator(); i2.hasNext();) {
                Element user = (Element) i2.next();
                String userName = user.getAttribute("name").getValue();
                addUserToGroup(userName, groupName);
            }
        }
        is.close();
    }
}
