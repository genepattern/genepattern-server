package org.genepattern.server.auth;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

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
public class XmlGroupMembership extends DefaultGroupManager {
    
    /**
     * Initialize group membership from a file.
     * @param userGroupsXmlFile
     */
    public XmlGroupMembership(File userGroupsXmlFile) {
        try {
            initUserGroupMap(userGroupsXmlFile);
        }
        catch (IOException e) {
            //TODO: log exception
        }
        catch (JDOMException e) {
            //TODO: log exception
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
            //TODO: log exception
        }
        catch (JDOMException e) {
            //TODO: log exception
        }
    }

    private void initUserGroupMap(File userGroupsXmlFile) throws IOException, JDOMException {
        InputStream is = null;
        if (!userGroupsXmlFile.exists()) {
            //TODO: log error
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
