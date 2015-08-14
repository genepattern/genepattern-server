/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.jsf;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.DbException;
import org.genepattern.server.database.HibernateSessionManager;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * junit tests for the RegisterServerBean.
 * 
 * @author pcarr
 *
 */
public class TestRegisterServerBean {
    private HibernateSessionManager mgr;
    
    @Before
    public void setUp() throws ExecutionException {
        mgr=DbUtil.getTestDbSession();
    }

    //@Ignore
    @Test
    public void databaseOperationsHsqlDb() throws Exception {
        //DbUtil.initDb();
        String dbRegisteredVersion=RegisterServerBean.getDbRegisteredVersion(mgr, "3.9.1");
        assertEquals("getDbRegisteredVersion, before save", "", dbRegisteredVersion);
        
        RegisterServerBean.saveIsRegistered(mgr, "3.9.0");
        RegisterServerBean.saveIsRegistered(mgr, "3.9.1");
        assertEquals("getDbRegisteredVersion, after save", "3.9.1", RegisterServerBean.getDbRegisteredVersion(mgr, "3.9.1"));

        List<String> actual=RegisterServerBean.getDbRegisteredVersions(mgr);
        assertEquals("num dbRegisteredVersions", 2, actual.size());
        assertTrue("startsWith('registeredVersion')", actual.get(0).startsWith("registeredVersion"));
        
        DbUtil.shutdownDb();
    }

    // only works when manually configured to connect to a MySQL server
    @Ignore 
    @Test
    public void getDbRegisteredVersion_mysql() throws Exception {
        System.setProperty("hibernate.configuration.file", "hibernate.mysql.cfg.xml");
        //DbUtil.initDb();
        String dbRegisteredVersion=RegisterServerBean.getDbRegisteredVersion(mgr, "3.9.1");
        assertEquals("", "", dbRegisteredVersion);
        
        RegisterServerBean.saveIsRegistered(mgr, "3.9.1");
        assertEquals("", "3.9.1", RegisterServerBean.getDbRegisteredVersion(mgr, "3.9.1"));
    }
    
    // only works when manually configured to connect to a MySQL server
    @Ignore 
    @Test
    public void getDbRegisteredVersions_mysql() {
        System.setProperty("hibernate.configuration.file", "hibernate.mysql.cfg.xml");
        List<String> actual=RegisterServerBean.getDbRegisteredVersions(mgr);
        assertEquals("numResults", 25, actual.size());
        assertTrue("startsWith('registeredVersion')", actual.get(0).startsWith("registeredVersion"));
    }
    
    // only works when manually configured to connect to a MySQL server
    @Ignore 
    @Test
    public void saveIsRegistered_hql_mysql() throws DbException {
        System.setProperty("hibernate.configuration.file", "hibernate.mysql.cfg.xml");
        RegisterServerBean.saveIsRegistered(mgr, "3.9.2");
    }
    
}
