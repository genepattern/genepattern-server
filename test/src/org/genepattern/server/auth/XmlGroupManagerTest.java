/*******************************************************************************
 * Copyright (c) 2003-2018 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.auth;

import java.io.File;

/**
 * Unit test the XmlGroupManager class.
 * 
 * @author pcarr
 */
public class XmlGroupManagerTest extends IGroupMembershipPluginTest {
    @Override
    protected IGroupMembershipPlugin createInstance(final File userGroupsXml) {
        return new XmlGroupMembership(userGroupsXml);
    }
}
