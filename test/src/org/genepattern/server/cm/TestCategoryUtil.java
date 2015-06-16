/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.cm;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.eula.TestEulaInfo;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.TaskInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * junit tests for the CategoryUtil class.
 * @author pcarr
 *
 */
public class TestCategoryUtil {
    GpConfig gpConfig;
    GpContext userContext;
    GpContext customUserContext;
    private CategoryUtil categoryUtil;

    @Before
    public void beforeTest() throws Exception {
        File configFile=FileUtil.getSourceFile(this.getClass(), "config.yaml");
        userContext=new GpContext.Builder()
            .userId("testUser")
        .build();
        customUserContext=new GpContext.Builder()
            .userId("customUser")
        .build();
        gpConfig=new GpConfig.Builder()
            .configFile(configFile)
        .build();
        categoryUtil=new CategoryUtil();
    }

    @Test
    public void testBaseLsid() {
        TaskInfo taskInfo=new TaskInfo();
        taskInfo.giveTaskInfoAttributes();
        taskInfo.getTaskInfoAttributes().put(GPConstants.LSID, "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2");
        Assert.assertEquals("urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002", CategoryUtil.getBaseLsid(taskInfo));
    }

    @Test
    public void testBaseLsid_minorVersion() {
        TaskInfo taskInfo=new TaskInfo();
        taskInfo.giveTaskInfoAttributes();
        taskInfo.getTaskInfoAttributes().put(GPConstants.LSID, "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:0.1");
        Assert.assertEquals("urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002", CategoryUtil.getBaseLsid(taskInfo));
    }

    @Test
    public void testBaseLsid_noVersion() {
        TaskInfo taskInfo=new TaskInfo();
        taskInfo.giveTaskInfoAttributes();
        taskInfo.getTaskInfoAttributes().put(GPConstants.LSID, "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002");
        Assert.assertEquals("urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002", CategoryUtil.getBaseLsid(taskInfo));
    }

    @Test
    public void testBaseLsid_null() {
        TaskInfo taskInfo=null;
        Assert.assertNull("taskInfo==null", CategoryUtil.getBaseLsid(taskInfo));
    }
    
    @Test
    public void testBaseLsid_emptyLsid() {
        TaskInfo taskInfo=new TaskInfo();
        Assert.assertNull("taskInfo==null", CategoryUtil.getBaseLsid(taskInfo));
    }
        
    /**
     * The legacy Golub pipeline should be in the 'pipeline' category.
     */
    @Test
    public void testGolubPipeline() {
        final TaskInfo taskInfo=TaskUtil.getTaskInfoFromZip(TestEulaInfo.class, "Golub.Slonim.1999.Nature.all.aml.pipeline_v2_modules_only.zip");
        final List<String> categories=categoryUtil.getCategoriesFromManifest(taskInfo);
        Assert.assertNotNull("Expecting non-null value from getCategoriesForTask", categories);
        Assert.assertEquals("num categories", 1, categories.size());
        Assert.assertEquals("category[0]", "pipeline", categories.get(0));
    }
    
    /**
     * Test a legacy module with 'taskType=Test' in the manifest file.
     */
    @Test
    public void testLegacyModule() {
        TaskInfo taskInfo=new TaskInfo();
        taskInfo.giveTaskInfoAttributes();
        taskInfo.getTaskInfoAttributes().put(GPConstants.TASK_TYPE, "Test");
        final List<String> categories=categoryUtil.getCategoriesFromManifest(taskInfo);
        Assert.assertNotNull("Expecting non-null value from getCategoriesForTask", categories);
        Assert.assertEquals("num categories", 1, categories.size());
        Assert.assertEquals("category[0]", "Test", categories.get(0));
    }

    @Test
    public void testCategorizedVisualizer() {
        final File zipFile=FileUtil.getDataFile("modules/MIT_701X_OriginalDataViewer_categorized.zip");
        final TaskInfo taskInfo=TaskUtil.getTaskInfoFromZip(zipFile);
        final List<String> categories=categoryUtil.getCategoriesFromManifest(taskInfo);
        Assert.assertNotNull("Expecting non-null value from getCategoriesForTask", categories);
        Assert.assertEquals("num categories", 2, categories.size());
        Assert.assertEquals("category[0]", "Visualizer", categories.get(0));
        Assert.assertEquals("category[1]", "MIT_701X", categories.get(1));
    }
    
    @Test
    public void testCategorizedPipeline() {
        final File zipFile=FileUtil.getDataFile("modules/MIT_701X_processClusteringData.pipeline_categorized.zip");
        final TaskInfo taskInfo=TaskUtil.getTaskInfoFromZip(zipFile);
        final List<String> categories=categoryUtil.getCategoriesFromManifest(taskInfo);
        Assert.assertNotNull("Expecting non-null value from getCategoriesForTask", categories);
        Assert.assertEquals("num categories", 2, categories.size());
        Assert.assertEquals("category[0]", "pipeline", categories.get(0));
        Assert.assertEquals("category[1]", "MIT_701X", categories.get(1));
    }

    /**
     * Move the module from the Visualizer category into a custom category.
     */
    @Test
    public void testMoveVisualizer() {
        TaskInfo taskInfo=new TaskInfo();
        taskInfo.giveTaskInfoAttributes();
        taskInfo.getTaskInfoAttributes().put(GPConstants.TASK_TYPE, GPConstants.TASK_TYPE_VISUALIZER);
        taskInfo.getTaskInfoAttributes().put(GPConstants.CATEGORIES, "MIT_701X");
        final List<String> categories=categoryUtil.getCategoriesFromManifest(taskInfo);
        Assert.assertNotNull("Expecting non-null value from getCategoriesForTask", categories);
        Assert.assertEquals("num categories", 1, categories.size());
        Assert.assertEquals("category[0]", "MIT_701X", categories.get(0));
    }

    /**
     * Test a module which has a matching taskType and categories property.
     */
    @Test
    public void testMatchingTaskTypeAndCategory() {
        TaskInfo taskInfo=new TaskInfo();
        taskInfo.giveTaskInfoAttributes();
        taskInfo.getTaskInfoAttributes().put(GPConstants.TASK_TYPE, "Test");
        taskInfo.getTaskInfoAttributes().put(GPConstants.CATEGORIES, "Test");
        final List<String> categories=categoryUtil.getCategoriesFromManifest(taskInfo);
        Assert.assertNotNull("Expecting non-null value from getCategoriesForTask", categories);
        Assert.assertEquals("num categories", 1, categories.size());
        Assert.assertEquals("category[0]", "Test", categories.get(0));
    }
    
    /**
     * Test a module which has a mis-matched taskType and categories property.
     */
    @Test
    public void testMismatchedTaskTypeAndCategory() {
        TaskInfo taskInfo=new TaskInfo();
        taskInfo.giveTaskInfoAttributes();
        taskInfo.getTaskInfoAttributes().put(GPConstants.TASK_TYPE, "Test");
        taskInfo.getTaskInfoAttributes().put(GPConstants.CATEGORIES, "CustomCategory");
        final List<String> categories=categoryUtil.getCategoriesFromManifest(taskInfo);
        Assert.assertNotNull("Expecting non-null value from getCategoriesForTask", categories);
        Assert.assertEquals("num categories", 1, categories.size());
        Assert.assertEquals("category[0]", "CustomCategory", categories.get(0));
    }
    
    /**
     * Test a module is more than one category.
     */
    @Test
    public void testMultipleCategories() {
        TaskInfo taskInfo=new TaskInfo();
        taskInfo.giveTaskInfoAttributes();
        taskInfo.getTaskInfoAttributes().put(GPConstants.TASK_TYPE, "Test");
        taskInfo.getTaskInfoAttributes().put(GPConstants.CATEGORIES, "Test;CustomA;CustomB");
        final List<String> categories=categoryUtil.getCategoriesFromManifest(taskInfo);
        Assert.assertNotNull("Expecting non-null value from getCategoriesForTask", categories);
        Assert.assertEquals("num categories", 3, categories.size());
        Assert.assertEquals("category[0]", "Test", categories.get(0));
        Assert.assertEquals("category[1]", "CustomA", categories.get(1));
        Assert.assertEquals("category[2]", "CustomB", categories.get(2));
    }

    /**
     * Test a module with whitespace characters in the categories spec.
     */
    @Test
    public void testTrimCategories() {
        TaskInfo taskInfo=new TaskInfo();
        taskInfo.giveTaskInfoAttributes();
        taskInfo.getTaskInfoAttributes().put(GPConstants.TASK_TYPE, "Test");
        taskInfo.getTaskInfoAttributes().put(GPConstants.CATEGORIES, " Test ; CustomA; CustomB");
        final List<String> categories=categoryUtil.getCategoriesFromManifest(taskInfo);
        Assert.assertNotNull("Expecting non-null value from getCategoriesForTask", categories);
        Assert.assertEquals("num categories", 3, categories.size());
        Assert.assertEquals("category[0]", "Test", categories.get(0));
        Assert.assertEquals("category[1]", "CustomA", categories.get(1));
        Assert.assertEquals("category[2]", "CustomB", categories.get(2));
    }

    /**
     * Test a module which has empty string categories, they should be ignored.
     */
    @Test
    public void testEmptyCategories() {
        TaskInfo taskInfo=new TaskInfo();
        taskInfo.giveTaskInfoAttributes();
        taskInfo.getTaskInfoAttributes().put(GPConstants.TASK_TYPE, "Test");
        taskInfo.getTaskInfoAttributes().put(GPConstants.CATEGORIES, " Test ; CustomA; ; ; CustomB ; ");
        final List<String> categories=categoryUtil.getCategoriesFromManifest(taskInfo);
        Assert.assertNotNull("Expecting non-null value from getCategoriesForTask", categories);
        Assert.assertEquals("num categories", 3, categories.size());
        Assert.assertEquals("category[0]", "Test", categories.get(0));
        Assert.assertEquals("category[1]", "CustomA", categories.get(1));
        Assert.assertEquals("category[2]", "CustomB", categories.get(2));
    }

    /**
     * Hide the module from all categories.
     */
    @Test
    public void testHiddenModule_emptyCategories() {
        TaskInfo taskInfo=new TaskInfo();
        taskInfo.giveTaskInfoAttributes();
        taskInfo.getTaskInfoAttributes().put(GPConstants.TASK_TYPE, "Preprocess & Utilities");
        taskInfo.getTaskInfoAttributes().put(GPConstants.CATEGORIES, "");
        final List<String> categories=categoryUtil.getCategoriesFromManifest(taskInfo);
        Assert.assertNotNull("Expecting non-null value from getCategoriesForTask", categories);
        Assert.assertEquals("num categories", 0, categories.size());
    }
    
    /**
     * Hide the visualizer from all categories.
     */
    @Test
    public void testHiddenVisualizer() {
        TaskInfo taskInfo=new TaskInfo();
        taskInfo.giveTaskInfoAttributes();
        taskInfo.getTaskInfoAttributes().put(GPConstants.TASK_TYPE, GPConstants.TASK_TYPE_VISUALIZER);
        taskInfo.getTaskInfoAttributes().put(GPConstants.CATEGORIES, "");
        final List<String> categories=categoryUtil.getCategoriesFromManifest(taskInfo);
        Assert.assertNotNull("Expecting non-null value from getCategoriesForTask", categories);
        Assert.assertEquals("num categories", 0, categories.size());
    }

    /**
     * Hide the pipeline from all categories.
     */
    @Test
    public void testHiddenPipeline() {
        TaskInfo taskInfo=new TaskInfo();
        taskInfo.giveTaskInfoAttributes();
        taskInfo.getTaskInfoAttributes().put(GPConstants.TASK_TYPE, GPConstants.TASK_TYPE_PIPELINE);
        taskInfo.getTaskInfoAttributes().put(GPConstants.CATEGORIES, "");
        final List<String> categories=categoryUtil.getCategoriesFromManifest(taskInfo);
        Assert.assertNotNull("Expecting non-null value from getCategoriesForTask", categories);
        Assert.assertEquals("num categories", 0, categories.size());
    }
    
    /**
     * Test case 1 for 'custom category', a custom category is one which has been over-ridden in the server.
     * Tests are done by mocking the DB query.
     * 
     * Test 1: No overide in the DB.
     */
    @Test
    public void testCustomCategories_noEntry() {
        TaskInfo taskInfo=new TaskInfo();   
        taskInfo.giveTaskInfoAttributes();
        //set the lsid to the same as ConvertLineEndings, v.2
        taskInfo.getTaskInfoAttributes().put(GPConstants.LSID,"urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2");
        taskInfo.getTaskInfoAttributes().put(GPConstants.TASK_TYPE, "Test");
        taskInfo.getTaskInfoAttributes().put(GPConstants.CATEGORIES, "Test; MIT_701X; ActualA; ActualB ");

        final List<String> customCategoriesFromDb=null;
        
        CategoryUtil spy = Mockito.spy(categoryUtil);
        // see: http://docs.mockito.googlecode.com/hg/1.9.5/org/mockito/Spy.html for an "Important gotcha when spying on real objects!"
        Mockito.doReturn(customCategoriesFromDb).when(spy).getCustomCategoriesFromDb(taskInfo);

        List<String> categories=spy.getCategoriesForTask(gpConfig, customUserContext, taskInfo);
        Assert.assertNotNull("Expecting non-null value from getCategoriesForTask", categories);
        Assert.assertEquals("num categories", 2, categories.size());
        Assert.assertEquals("category[0]", "ActualA", categories.get(0));
        Assert.assertEquals("category[1]", "ActualB", categories.get(1));
    }
    
    /**
     * Custom category test, custom category in DB.
     */
    @Test
    public void testCustomCategories_from_db() {
        TaskInfo taskInfo=new TaskInfo();   
        taskInfo.giveTaskInfoAttributes();
        //set the lsid to the same as ConvertLineEndings, v.2
        taskInfo.getTaskInfoAttributes().put(GPConstants.LSID,"urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2");
        taskInfo.getTaskInfoAttributes().put(GPConstants.TASK_TYPE, "Test");
        taskInfo.getTaskInfoAttributes().put(GPConstants.CATEGORIES, "Test; MIT_701X; ActualA; ActualB ");

        final List<String> customCategoriesFromDb=new ArrayList<String>();
        customCategoriesFromDb.add("CustomA");
        customCategoriesFromDb.add("CustomB");
        CategoryUtil spy = Mockito.spy(categoryUtil);
        Mockito.doReturn(customCategoriesFromDb).when(spy).getCustomCategoriesFromDb(taskInfo);

        List<String> categories=spy.getCategoriesForTask(gpConfig, customUserContext, taskInfo);
        Assert.assertNotNull("Expecting non-null value from getCategoriesForTask", categories);
        Assert.assertEquals("num categories", 2, categories.size());
        Assert.assertEquals("category[0]", "CustomA", categories.get(0));
        Assert.assertEquals("category[1]", "CustomB", categories.get(1));
    }

    /**
     * Custom category test, hidden in DB.
     */
    @Test
    public void testCustomCategories_hidden() {
        TaskInfo taskInfo=new TaskInfo();   
        taskInfo.giveTaskInfoAttributes();
        //set the lsid to the same as ConvertLineEndings, v.2
        taskInfo.getTaskInfoAttributes().put(GPConstants.LSID,"urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2");
        taskInfo.getTaskInfoAttributes().put(GPConstants.TASK_TYPE, "Test");
        taskInfo.getTaskInfoAttributes().put(GPConstants.CATEGORIES, "Test; MIT_701X; ActualA; ActualB ");

        // an empty list means, this module is in no categories
        final List<String> customCategoriesFromDb=new ArrayList<String>();
        
        CategoryUtil spy = Mockito.spy(categoryUtil);
        Mockito.doReturn(customCategoriesFromDb).when(spy).getCustomCategoriesFromDb(taskInfo);

        List<String> categories=spy.getCategoriesForTask(gpConfig, customUserContext, taskInfo);
        Assert.assertNotNull("Expecting non-null value from getCategoriesForTask", categories);
        Assert.assertEquals("num categories", 0, categories.size());
    }

}
