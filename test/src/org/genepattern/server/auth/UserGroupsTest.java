/*******************************************************************************
 * Copyright (c) 2003-2021 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.auth;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.genepattern.junitutil.FileUtil;
import org.jdom.JDOMException;
import org.junit.Test;

/**
 * Unit test the UserGroups class.
 * 
 * @author pcarr
 */
public class UserGroupsTest {

    @Test
    public void testGetUsersInGroup() throws JDOMException, IOException {
        final File userGroupsXmlFile=FileUtil.getSourceFile(this.getClass(), "userGroups_test_02.xml");
        final UserGroups userGroups=UserGroups.initFromXml(userGroupsXmlFile);

        // test getUsersInGroup, user membership for the 'ignore' group
        final String groupId="gp-team";
        final Set<String> users=userGroups.getUsers(groupId);
        assertEquals("users.size", 7, users.size());
        assertEquals(groupId+".contains[gp-admin-01]", true, users.contains("gp-admin-01"));
        // ...
        assertEquals(groupId+".contains[gp-common-01]", true, users.contains("gp-common-01"));
        
        // test getGroupsForUser
        final String userId="gp-common-01";
        final Set<String> groups=userGroups.getGroups(userId);
        assertEquals("groups.size", 6, groups.size());
        assertEquals(userId+"is in '*'", true, groups.contains("*"));
        assertEquals(userId+"is in public", true, groups.contains("public"));
        assertEquals(userId+"is in administrators", true, groups.contains("administrators"));
        assertEquals(userId+"is in dev-users", true, groups.contains("dev-users"));
        assertEquals(userId+"is in test-users", true, groups.contains("test-users"));
        assertEquals(userId+"is in gp-team", true, groups.contains("gp-team"));
    }

    @Test
    public void testDefault() {
        final UserGroups userGroups=UserGroups.initDefault();
        final Set<String> groups=userGroups.getGroups("test_user");
        assertEquals("groups.size", 1, groups.size());
        assertTrue("groups.contains('*')", groups.contains("*"));
        
        assertTrue("'test_user' is member of '*' group", userGroups.isMember("test_user", "*"));
        assertFalse("'test_user' is member of 'admin' group", userGroups.isMember("test_user", "admin"));
    }

}
