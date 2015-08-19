/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.auth;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit test the XmlGroupManager class.
 * 
 * @author pcarr
 */
public class XmlGroupManagerTest {
    private XmlGroupMembership groupManager = null;

    private InputStream input = null;

    private String admin = "admin";
    private String gp_admin = "gp_admin";
    private String jgould = "jgould";
    private String nazaire = "nazaire";
    private String pcarr = "pcarr";
    private String test = "test";
    private String notInDb = "notInDb";
    
    private String adminGroup = "administrators";
    private String publicGroup = "public";
    private String devGroup = "gp-dev";
    private String moduleGroup = "module-dev";

    @Before
    public void setUp() throws Exception {
        input = this.getClass().getResourceAsStream("userGroups.xml");
        groupManager = new XmlGroupMembership(input);
    }
    
    @Test
    public void testWildcardGroups() {
        assertTrue("'test' user is in wildcard (*) group", groupManager.getGroups(test).contains("public"));
        assertTrue("'notInDb' user is in wildcard (*) group", groupManager.getGroups(notInDb).contains("public"));
    }
    
    @Test
    public void testAdministratorsGroup() {
        assertTrue("isMember(admin, admin)", groupManager.isMember(admin, adminGroup));
        assertTrue("isMember(gp_admin, admin)", groupManager.isMember(gp_admin, adminGroup));
        assertFalse("isMember(jgould, admin)", groupManager.isMember(jgould, adminGroup));
        assertFalse("isMember(nazaire, admin)", groupManager.isMember(nazaire, adminGroup));
        assertFalse("isMember(pcarr, admin)", groupManager.isMember(pcarr, adminGroup));
        assertFalse("isMember(test, admin)", groupManager.isMember(test, adminGroup));
        assertFalse("isMember(notInDb, admin)", groupManager.isMember(notInDb, adminGroup));
    }
    
    @Test
    public void testNonExistentGroup() {
        String n = "not_in_file";
        assertFalse("isMember(admin, n)", groupManager.isMember(admin, n));
        assertFalse("isMember(jgould, n)", groupManager.isMember(jgould, n));
        assertFalse("isMember(nazaire, n)", groupManager.isMember(nazaire, n));
        assertFalse("isMember(pcarr, n)", groupManager.isMember(pcarr, n));
        assertFalse("isMember(test, n)", groupManager.isMember(test, n));
        assertFalse("isMember(notInDb, n)", groupManager.isMember(notInDb, n));
    }
    
    @Test
    public void testGetGroups() {
        Set<String> groups = groupManager.getGroups(jgould);
        assertEquals("jgould is in 3 groups", groups.size(), 3);
        assertTrue("jgould is in the 'public' group", groups.contains(publicGroup));
        assertTrue("jgould is in the 'gp-dev' group", groups.contains(devGroup));
        assertTrue("jgould is in the 'gp-module'", groups.contains(moduleGroup));
    }

}
