package org.genepattern.server.domain;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPropsTable {
    private static HibernateSessionManager mgr;
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        DbUtil.initDb();
        mgr=HibernateUtil.instance();
    }
    
    /**
     * CRUD tests for the 'PROPS' table.
     * @throws Exception
     */
    @Test
    public void createUpdateDeleteProp() throws Exception {
        String key="NEW_KEY";
        assertEquals("selectValue, before save", "",  PropsTable.selectValue(mgr, key));
        assertEquals("selectKeys, before save", Arrays.asList(), PropsTable.selectKeys("NEW_KEY"));
        PropsTable.saveProp(mgr, key, "FIRST_VALUE");
        assertEquals("selectValue, after save", "FIRST_VALUE",  PropsTable.selectValue(mgr, key));
        assertEquals("selectKeys, after save", Arrays.asList(key), PropsTable.selectKeys("NEW_KEY"));
        PropsTable.saveProp(mgr, key, "UPDATED_VALUE");
        assertEquals("after update", "UPDATED_VALUE",  PropsTable.selectValue(mgr, key));
        PropsTable.removeProp(mgr, key);
        assertEquals("after delete", "",  PropsTable.selectValue(mgr, key));
        PropsTable.removeProp(mgr, key);
        assertEquals("after 2nd delete", "",  PropsTable.selectValue(mgr, key));
    }

}
