package org.genepattern.server.webapp.jsf;

import static org.junit.Assert.*;

import java.util.List;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.domain.Props;
import org.junit.Ignore;
import org.junit.Test;

/**
 * junit tests for the RegisterServerBean.
 * 
 * @author pcarr
 *
 */
public class TestRegisterServerBean {
    
    //@Ignore
    @Test
    public void createUpdateDeleteProp() throws Exception {
        DbUtil.initDb();
        String key="NEW_KEY";
        assertEquals("before save", "",  Props.selectValue(key));
        Props.saveProp(key, "FIRST_VALUE");
        assertEquals("after save", "FIRST_VALUE",  Props.selectValue(key));
        Props.saveProp(key, "UPDATED_VALUE");
        assertEquals("after update", "UPDATED_VALUE",  Props.selectValue(key));
        Props.removeProp(key);
        assertEquals("after delete", "",  Props.selectValue(key));
        Props.removeProp(key);
        assertEquals("after 2nd delete", "",  Props.selectValue(key));
    }

    //@Ignore
    @Test
    public void databaseOperationsHsqlDb() throws Exception {
        DbUtil.initDb();
        String dbRegisteredVersion=RegisterServerBean.getDbRegisteredVersion("3.9.1");
        assertEquals("getDbRegisteredVersion, before save", "", dbRegisteredVersion);
        
        RegisterServerBean.saveIsRegistered("3.9.0");
        RegisterServerBean.saveIsRegistered("3.9.1");
        assertEquals("getDbRegisteredVersion, after save", "3.9.1", RegisterServerBean.getDbRegisteredVersion("3.9.1"));

        List<String> actual=RegisterServerBean.getDbRegisteredVersions();
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
        String dbRegisteredVersion=RegisterServerBean.getDbRegisteredVersion("3.9.1");
        assertEquals("", "", dbRegisteredVersion);
        
        RegisterServerBean.saveIsRegistered("3.9.1");
        
        //RegisterServerBean.saveIsRegistered("3.9.1");
        assertEquals("", "3.9.1", RegisterServerBean.getDbRegisteredVersion("3.9.1"));
    }
    
    // only works when manually configured to connect to a MySQL server
    @Ignore 
    @Test
    public void getDbRegisteredVersions_mysql() {
        System.setProperty("hibernate.configuration.file", "hibernate.mysql.cfg.xml");
        List<String> actual=RegisterServerBean.getDbRegisteredVersions();
        assertEquals("numResults", 25, actual.size());
        assertTrue("startsWith('registeredVersion')", actual.get(0).startsWith("registeredVersion"));
    }
    
    
    // only works when manually configured to connect to a MySQL server
    @Ignore 
    @Test
    public void saveIsRegistered_hql_mysql() {
        System.setProperty("hibernate.configuration.file", "hibernate.mysql.cfg.xml");
        RegisterServerBean.saveIsRegistered("3.9.2");
    }
    
}
