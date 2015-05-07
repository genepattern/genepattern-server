/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.domain;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class SuiteDAOTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        System.setProperty("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
        System.setProperty("hibernate.connection.shutdown", "true");
        System.setProperty("hibernate.connection.url", "jdbc:hsqldb:hsql://localhost/xdb");
        System.setProperty("hibernate.connection.username", "sa");
        System.setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
        
        String port = "9001";
        String dbFile = "../resources/GenePatternDB";
        String dbUrl = "file:" + dbFile;
        String dbName = "xdb";
        String[] args = new String[] { "-port", port, "-database.0", dbUrl, "-dbname.0", dbName };
        org.hsqldb.Server.main(args);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public final void testFindAll() {
        SuiteDAO dao = new SuiteDAO();
        List<Suite> suites = dao.findAll();
        for(Suite s : suites) {
            System.out.println(s.getUserId());
        }
        assertTrue(suites.size() > 0);
    }

    @Test
    public final void testFindByOwner() {
        SuiteDAO dao = new SuiteDAO();
        String userId = "jrobinso@broad.mit.edu";
        List suites = dao.findByOwner(userId);
        assertTrue(suites.size() > 0);
    }

    @Test
    public final void testFindByOwnerOrPublic() {
        SuiteDAO dao = new SuiteDAO();
        String userId = "jrobinso@broad.mit.edu";
        List suites = dao.findByOwnerOrPublic(userId);
        assertTrue(suites.size() > 0);
    }
}
