/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.domain;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.database.HibernateSessionManager;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPropsTable {
    private static HibernateSessionManager mgr;
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        mgr=DbUtil.getTestDbSession();
    }
    
    /**
     * CRUD tests for the 'PROPS' table.
     * @throws Exception
     */
    @Test
    public void createUpdateDeleteProp() throws Exception {
        List<PropsTable> allProps=PropsTable.selectAllProps(mgr);
        assertEquals("selectAllProps.size, before save", 1, allProps.size());
        
        String key="NEW_KEY";
        assertEquals("selectValue, before save", "",  PropsTable.selectValue(mgr, key));
        assertEquals("selectKeys, before save", Arrays.asList(), PropsTable.selectKeys(mgr, "NEW_KEY"));
        PropsTable.saveProp(mgr, key, "FIRST_VALUE");
        assertEquals("selectValue, after save", "FIRST_VALUE",  PropsTable.selectValue(mgr, key));
        assertEquals("selectKeys, after save", Arrays.asList(key), PropsTable.selectKeys(mgr, "NEW_KEY"));
        assertEquals("selectAllProps.size, after save", 2, PropsTable.selectAllProps(mgr).size());
        PropsTable.saveProp(mgr, key, "UPDATED_VALUE");
        assertEquals("after update", "UPDATED_VALUE",  PropsTable.selectValue(mgr, key));
        assertEquals("selectAllProps.size, after update", 2, PropsTable.selectAllProps(mgr).size());
        PropsTable.removeProp(mgr, key);
        assertEquals("after delete", "",  PropsTable.selectValue(mgr, key));
        assertEquals("selectAllProps.size, after delete", 1, PropsTable.selectAllProps(mgr).size());
        PropsTable.removeProp(mgr, key);
        assertEquals("after 2nd delete", "",  PropsTable.selectValue(mgr, key));
        assertEquals("selectAllProps.size, after 2nd delete", 1, PropsTable.selectAllProps(mgr).size());
        
        // null insert means null value ....
        PropsTable.saveProp(mgr, key, "FIRST_VALUE");
        PropsTable.saveProp(mgr, key, "");
        assertEquals("after setting value to empty string", "", PropsTable.selectRow(mgr, key).getValue());
        PropsTable.saveProp(mgr, key, null);
        assertEquals("after setting value to null", null, PropsTable.selectRow(mgr, key).getValue());
        
        // must remove to delete the row
        PropsTable.removeProp(mgr, key);
        assertEquals("after removing ", null, PropsTable.selectRow(mgr, key));
    }

}
