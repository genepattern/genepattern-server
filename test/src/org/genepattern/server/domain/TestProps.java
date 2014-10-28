package org.genepattern.server.domain;

import static org.junit.Assert.assertEquals;

import org.genepattern.junitutil.DbUtil;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestProps {
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        DbUtil.initDb();
    }
    
    /**
     * CRUD tests for the 'PROPS' table.
     * @throws Exception
     */
    @Test
    public void createUpdateDeleteProp() throws Exception {
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

}
