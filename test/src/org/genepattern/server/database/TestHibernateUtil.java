package org.genepattern.server.database;

import static org.junit.Assert.*;

import org.genepattern.junitutil.DbUtil;
import org.junit.Before;
import org.junit.Test;

public class TestHibernateUtil {
    @Before
    public void setUp() throws Exception {
        DbUtil.initDb();
    }
    
    @Test
    public void isInTransaction() {
        boolean isInTransaction=HibernateUtil.isInTransaction();
        assertFalse(isInTransaction);
    }
    
    @Test
    public void mappingFromHbmXmlFile_User() {
        DbUtil.addUserToDb("test_user");
        
    }
}
