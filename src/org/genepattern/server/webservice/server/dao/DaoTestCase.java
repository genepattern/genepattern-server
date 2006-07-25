package org.genepattern.server.webservice.server.dao;

import junit.framework.TestCase;


public class DaoTestCase extends TestCase {
 
    private static boolean driverIsInitialized = false;

    protected void setUp() throws Exception {
        if (!driverIsInitialized) {
            intitializeDriver();
            driverIsInitialized = true;
        }
 
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
     }

    private static void intitializeDriver() {
        try {
            Class.forName("org.hsqldb.jdbcDriver");
        }
        catch (Exception e) {
            System.out.println("ERROR: failed to load HSQLDB JDBC driver.");
            e.printStackTrace();
        }
    }
}
