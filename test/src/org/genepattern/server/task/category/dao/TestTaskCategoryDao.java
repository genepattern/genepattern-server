package org.genepattern.server.task.category.dao;

import java.util.List;

import org.genepattern.junitutil.DbUtil;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test cases for querying task_category mapping from the DB.
 * 
 * @author pcarr
 *
 */
public class TestTaskCategoryDao {
    
    @BeforeClass
    static public void beforeClass() throws Exception {
        DbUtil.initDb();
    }

    @AfterClass
    static public void afterClass() throws Exception {
        DbUtil.shutdownDb();
    }
    
    @Test
    public void testSaveAndQueryTaskCategory() { 
        final String baseLsid="urn:lsid:8080.jtriley.STARAPP-DEV.MIT.EDU:genepatternmodules:17";
        TaskCategoryRecorder recorder=new TaskCategoryRecorder();
        recorder.save(baseLsid, "MIT_701X");
        
        final List<TaskCategory> records=recorder.query(baseLsid);
        Assert.assertNotNull("records", records);
        Assert.assertEquals("records.size", 1, records.size());
        Assert.assertEquals("records[0].category", "MIT_701X", records.get(0).getCategory());
    }
    
    @Test
    public void testGetAllRecords() {
        TaskCategoryRecorder recorder=new TaskCategoryRecorder();
        
        //add some more records ...
        final String cleLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002";
        recorder.save(cleLsid, ""); //hidden
        List<TaskCategory> records=recorder.getAllCustomCategories();
        Assert.assertNotNull("records", records);
        Assert.assertEquals("records.size", 2, records.size());
        Assert.assertEquals("records[0].category", "MIT_701X", records.get(0).getCategory());
    }

}
