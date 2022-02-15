/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.domain;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.DbException;
import org.genepattern.server.database.HibernateSessionManager;
import org.junit.Test;

public class TestPropsTable {
    /**
     * CRUD tests for the 'PROPS' table.
     */
    public static void test_PROPS_table(final HibernateSessionManager mgr) 
    throws DbException 
    {
        
        List<PropsTable> allProps=PropsTable.selectAllProps(mgr);
        assertNotNull("selectAllProps != null", allProps);
        
        String key="NEW_KEY";
        assertEquals("selectValue, before save", "",  PropsTable.selectValue(mgr, key));
        assertEquals("selectKeys, before save", Arrays.asList(), PropsTable.selectKeys(mgr, "NEW_KEY"));
        PropsTable.saveProp(mgr, key, "FIRST_VALUE"); 
        assertEquals("selectAllProps.size>0, after save", true, PropsTable.selectAllProps(mgr).size()>0); 
        assertEquals("selectValue, after save", "FIRST_VALUE",  PropsTable.selectValue(mgr, key));
        assertEquals("selectKeys, after save", Arrays.asList(key), PropsTable.selectKeys(mgr, "NEW_KEY"));
        PropsTable.saveProp(mgr, key, "UPDATED_VALUE");
        assertEquals("after update", "UPDATED_VALUE",  PropsTable.selectValue(mgr, key));
        PropsTable.removeProp(mgr, key);
        assertEquals("after delete", "",  PropsTable.selectValue(mgr, key));
        PropsTable.removeProp(mgr, key);
        assertEquals("after 2nd delete", "",  PropsTable.selectValue(mgr, key));
        
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
    
    /**
     * CRUD tests for the 'PROPS' table.
     * @throws ExecutionException 
     * @throws DbException
     */
    @Test
    public void createUpdateDeleteProp() throws DbException, ExecutionException {
        HibernateSessionManager mgr=DbUtil.getTestDbSession();
        test_PROPS_table(mgr);
    }

}
