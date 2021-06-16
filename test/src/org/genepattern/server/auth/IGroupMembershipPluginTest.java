/*******************************************************************************
 * Copyright (c) 2003-2021 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.auth;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Set;

import org.genepattern.junitutil.FileUtil;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Generic test of the IGroupMembershipPlugin interface which can be re-used to test
 * multiple implementations of this interface.
 * 
 * To test a concrete class, extend this test class and implement the abstract method.
 *     createInstance(File xmlFile) throws Exception;
 * 
 * I used this when I replaced the XmlGroupMembership class with the UserGroups class. 
 * Both implementations passed these tests.
 */

@Ignore
public abstract class IGroupMembershipPluginTest {
    
    protected abstract IGroupMembershipPlugin createInstance(final File xmlFile) throws Exception;

    private IGroupMembershipPlugin groupManager;
    
    private String adminGroup  = "administrators";
    private String publicGroup = "public";
    private String devGroup    = "gp-dev";
    private String moduleGroup = "module-dev";

    @Before
    public void setUp() throws Exception {
        final File userGroupsXml=FileUtil.getSourceFile(IGroupMembershipPluginTest.class, "userGroups.xml");
        groupManager = createInstance(userGroupsXml);
    }

    protected void assertIsMember(final String userId, final String groupId) {
        assertTrue("isMember("+userId+","+groupId+")", groupManager.isMember(userId, groupId));
    }
    
    protected void assertIsNotMember(final String userId, final String groupId) {
        assertFalse("isMember("+userId+","+groupId+")", groupManager.isMember(userId, groupId));
    }

    @Test
    public void testWildcardGroups() {
        assertTrue("'test' user is in 'public' group", groupManager.getGroups("test").contains("public"));
        assertTrue("'notInDb' user is in 'public' group", groupManager.getGroups("notInDb").contains("public"));
        assertTrue("'test' user is in '*' group", groupManager.getGroups("test").contains("*"));
        assertTrue("'notInDb' user is in '*' group", groupManager.getGroups("notInDb").contains("*"));
    }
    
    @Test
    public void testAdministratorsGroup() {
        assertIsMember("admin", adminGroup);
        assertIsMember("gp_admin", adminGroup);
        assertIsNotMember("dev_user_02", adminGroup);
        assertIsNotMember("user_03", adminGroup);
        assertIsNotMember("dev_user_01", adminGroup);
        assertIsNotMember("test", adminGroup);
        assertIsNotMember("notInDb", adminGroup);
    }
    
    @Test
    public void testNonExistentGroup() {
        final String n = "not_in_file";
        assertIsNotMember("admin", n);
        assertIsNotMember("dev_user_02", n);
        assertIsNotMember("user_03", n);
        assertIsNotMember("dev_user_01", n);
        assertIsNotMember("test", n);
        assertIsNotMember("notInDb", n);
    }
    
    @Test
    public void testGetGroups() {
        Set<String> groups = groupManager.getGroups("gp_dev_user_02");
        assertEquals("'gp_dev_user_02' is in 4 groups", 4, groups.size());
        assertTrue("'gp_dev_user_02' is in the hidden wildcard ('*') group", groups.contains("*"));
        assertTrue("'gp_dev_user_02' is in the 'public' group", groups.contains(publicGroup));
        assertTrue("'gp_dev_user_02' is in the 'gp-dev' group", groups.contains(devGroup));
        assertTrue("'gp_dev_user_02' is in the 'gp-module'", groups.contains(moduleGroup));
    }

}
