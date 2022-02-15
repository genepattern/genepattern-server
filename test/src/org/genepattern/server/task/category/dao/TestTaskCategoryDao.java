/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.task.category.dao;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.database.HibernateSessionManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for querying task_category mapping from the DB.
 * 
 * @author pcarr
 *
 */
public class TestTaskCategoryDao {
    private HibernateSessionManager mgr;
    
    @Before
    public void setUp() throws ExecutionException {
        mgr=DbUtil.getTestDbSession();
    }

    /**
     * Run through basic DAO operations as a single test.
     * When run individually, the results are non-deterministic
     * because they depend on the order in  which the tests are run.
     */
    @Test
    public void regressionTest() {
        final String baseLsid="urn:lsid:8080.jtriley.STARAPP-DEV.MIT.EDU:genepatternmodules:17";
        final String cleLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002";
        final String hiddenLsid="urn:lsid:broad.mit.edu:gptest:00001";

        TaskCategoryRecorder recorder=new TaskCategoryRecorder(mgr);
        recorder.save(baseLsid, "MIT_701X"); //custom category
        recorder.save(cleLsid, ""); //empty category
        recorder.save(hiddenLsid, ".hiddenCategory");

        // test 1, query all custom categories
        Assert.assertEquals("customCategories.size", 3, recorder.getAllCustomCategories().size());

        // test 2, query categories by lsid
        List<TaskCategory> records=recorder.query(baseLsid);
        Assert.assertNotNull("query MIT_701X", records);
        Assert.assertEquals("query MIT_701X, records.size", 1, records.size());
        Assert.assertEquals("records[0].category", "MIT_701X", records.get(0).getCategory());
    }

}
