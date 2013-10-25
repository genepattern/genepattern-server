package org.genepattern.server.cm;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.genepattern.junitutil.ConfigUtil;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.eula.TestEulaInfo;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.TaskInfo;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * junit tests for the CategoryManager class.
 * 
 * @author pcarr
 *
 */
public class TestCategoryManager {
    Context userContext=ServerConfiguration.Context.getContextForUser("testUser");

    /*
     * Test-cases
     * 1) get category for legacy pipeline
     * 2) get category for legacy module
     * 3) get custom category for pipeline from manifest ...
     * 3a) standard mode, show in 'pipeline' and '<custom>'
     * 3b) hidden mode, should not have any category
     * 3c) don't include in 'pipeline' category
     */

    @BeforeClass
    public static void beforeClass() {
        ConfigUtil.loadConfigFile(TestCategoryManager.class, "config.yaml");
    }
    
    /**
     * The legacy Golub pipeline should be in the 'pipeline' category.
     */
    @Test
    public void testGolubPipeline() {
        final TaskInfo taskInfo=TaskUtil.getTaskInfoFromZip(TestEulaInfo.class, "Golub.Slonim.1999.Nature.all.aml.pipeline_v2_modules_only.zip");
        final List<String> categories=CategoryManager.getCategoriesForTask(userContext, taskInfo);
        Assert.assertNotNull("Expecting non-null value from getCategoriesForTask", categories);
        Assert.assertEquals("num categories", 1, categories.size());
        Assert.assertEquals("category[0]", "pipeline", categories.get(0));
    }
    
    /**
     * Test a module which is in more than one category.
     */
    @Test
    public void testLegacyModule() {
        TaskInfo taskInfo=new TaskInfo();
        taskInfo.giveTaskInfoAttributes();
        taskInfo.getTaskInfoAttributes().put(GPConstants.TASK_TYPE, "Test");
        final List<String> categories=CategoryManager.getCategoriesForTask(userContext, taskInfo);
        Assert.assertNotNull("Expecting non-null value from getCategoriesForTask", categories);
        Assert.assertEquals("num categories", 1, categories.size());
        Assert.assertEquals("category[0]", "Test", categories.get(0));
    }

    @Test
    public void testCategorizedVisualizer() {
        final File zipFile=FileUtil.getDataFile("modules/MIT_701X_OriginalDataViewer_categorized.zip");
        final TaskInfo taskInfo=TaskUtil.getTaskInfoFromZip(zipFile);
        final List<String> categories=CategoryManager.getCategoriesForTask(userContext, taskInfo);
        Assert.assertNotNull("Expecting non-null value from getCategoriesForTask", categories);
        Assert.assertEquals("num categories", 2, categories.size());
        Assert.assertEquals("category[0]", "Visualizer", categories.get(0));
        Assert.assertEquals("category[1]", "MIT_701X", categories.get(1));
    }
    
    @Test
    public void testCategorizedPipeline() {
        final File zipFile=FileUtil.getDataFile("modules/MIT_701X_processClusteringData.pipeline_categorized.zip");
        final TaskInfo taskInfo=TaskUtil.getTaskInfoFromZip(zipFile);
        final List<String> categories=CategoryManager.getCategoriesForTask(userContext, taskInfo);
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
        final List<String> categories=CategoryManager.getCategoriesForTask(userContext, taskInfo);
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
        final List<String> categories=CategoryManager.getCategoriesForTask(userContext, taskInfo);
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
        final List<String> categories=CategoryManager.getCategoriesForTask(userContext, taskInfo);
        Assert.assertNotNull("Expecting non-null value from getCategoriesForTask", categories);
        Assert.assertEquals("num categories", 1, categories.size());
        Assert.assertEquals("category[0]", "CustomCategory", categories.get(0));
    }
    
    /**
     * Test a module which has a mis-matched taskType and categories property.
     */
    @Test
    public void testMultipleCategories() {
        TaskInfo taskInfo=new TaskInfo();
        taskInfo.giveTaskInfoAttributes();
        taskInfo.getTaskInfoAttributes().put(GPConstants.TASK_TYPE, "Test");
        taskInfo.getTaskInfoAttributes().put(GPConstants.CATEGORIES, "Test;CustomA;CustomB");
        final List<String> categories=CategoryManager.getCategoriesForTask(userContext, taskInfo);
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
        final List<String> categories=CategoryManager.getCategoriesForTask(userContext, taskInfo);
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
        final List<String> categories=CategoryManager.getCategoriesForTask(userContext, taskInfo);
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
    public void testHiddenModule() {
        TaskInfo taskInfo=new TaskInfo();
        taskInfo.giveTaskInfoAttributes();
        taskInfo.getTaskInfoAttributes().put(GPConstants.TASK_TYPE, "Preprocess & Utilities");
        taskInfo.getTaskInfoAttributes().put(GPConstants.CATEGORIES, "");
        final List<String> categories=CategoryManager.getCategoriesForTask(userContext, taskInfo);
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
        final List<String> categories=CategoryManager.getCategoriesForTask(userContext, taskInfo);
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
        final List<String> categories=CategoryManager.getCategoriesForTask(userContext, taskInfo);
        Assert.assertNotNull("Expecting non-null value from getCategoriesForTask", categories);
        Assert.assertEquals("num categories", 0, categories.size());
    }
    
    /**
     * Test cases for hidden categories, as configured in the config.yaml file.
     */
    @Test
    public void testHiddenCategories() {
        // see 'customUser' settings in the config.yaml file
        userContext=ServerConfiguration.Context.getContextForUser("customUser");
        TaskInfo taskInfo=new TaskInfo();
        taskInfo.giveTaskInfoAttributes();
        
        //1st test, module with taskType matching a hidden category
        taskInfo.getTaskInfoAttributes().put(GPConstants.TASK_TYPE, "Test");
        List<String> categories=CategoryManager.getCategoriesForTask(userContext, taskInfo);
        Assert.assertNotNull("Expecting non-null value from getCategoriesForTask", categories);
        Assert.assertEquals("num categories", 0, categories.size());
        
        //2nd test, module with categories matching a hidden category
        taskInfo=new TaskInfo();
        taskInfo.giveTaskInfoAttributes();
        taskInfo.getTaskInfoAttributes().put(GPConstants.TASK_TYPE, "Test");
        taskInfo.getTaskInfoAttributes().put(GPConstants.CATEGORIES, "Test;MIT_701X;Not Hidden");
        categories=CategoryManager.getCategoriesForTask(userContext, taskInfo);
        Assert.assertNotNull("Expecting non-null value from getCategoriesForTask", categories);
        Assert.assertEquals("num categories", 1, categories.size());
        Assert.assertEquals("category[0]", "Not Hidden", categories.get(0));
    }
    
    /**
     * Test case 1 for 'custom category', a custom category is one which has been over-ridden in the server.
     * Tests are done by mocking the DB query.
     * 
     * Test 1: No overide in the DB.
     */
    @Test
    public void testCustomCategories_noEntry() {
        userContext=ServerConfiguration.Context.getContextForUser("customUser");
        TaskInfo taskInfo=new TaskInfo();   
        taskInfo.giveTaskInfoAttributes();
        //set the lsid to the same as ConvertLineEndings, v.2
        taskInfo.getTaskInfoAttributes().put(GPConstants.LSID,"urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2");
        taskInfo.getTaskInfoAttributes().put(GPConstants.TASK_TYPE, "Test");
        taskInfo.getTaskInfoAttributes().put(GPConstants.CATEGORIES, "Test; MIT_701X; ActualA; ActualB ");

        //final List<String> customCategoriesFromDb=new ArrayList<String>();
        final List<String> customCategoriesFromDb=null;
        
        CategoryManagerImpl real = new CategoryManagerImpl();
        CategoryManagerImpl spy = Mockito.spy(real);
        Mockito.when( spy.getCustomCategoriesFromDb(taskInfo) ).thenReturn(customCategoriesFromDb);

        List<String> categories=spy.getCategoriesForTask(userContext, taskInfo);
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
        userContext=ServerConfiguration.Context.getContextForUser("customUser");
        TaskInfo taskInfo=new TaskInfo();   
        taskInfo.giveTaskInfoAttributes();
        //set the lsid to the same as ConvertLineEndings, v.2
        taskInfo.getTaskInfoAttributes().put(GPConstants.LSID,"urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2");
        taskInfo.getTaskInfoAttributes().put(GPConstants.TASK_TYPE, "Test");
        taskInfo.getTaskInfoAttributes().put(GPConstants.CATEGORIES, "Test; MIT_701X; ActualA; ActualB ");

        // an empty list means, this module is in no categories
        final List<String> customCategoriesFromDb=new ArrayList<String>();
        customCategoriesFromDb.add("CustomA");
        customCategoriesFromDb.add("CustomB");
        
        CategoryManagerImpl real = new CategoryManagerImpl();
        CategoryManagerImpl spy = Mockito.spy(real);
        Mockito.when( spy.getCustomCategoriesFromDb(taskInfo) ).thenReturn(customCategoriesFromDb);

        List<String> categories=spy.getCategoriesForTask(userContext, taskInfo);
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
        userContext=ServerConfiguration.Context.getContextForUser("customUser");
        TaskInfo taskInfo=new TaskInfo();   
        taskInfo.giveTaskInfoAttributes();
        //set the lsid to the same as ConvertLineEndings, v.2
        taskInfo.getTaskInfoAttributes().put(GPConstants.LSID,"urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2");
        taskInfo.getTaskInfoAttributes().put(GPConstants.TASK_TYPE, "Test");
        taskInfo.getTaskInfoAttributes().put(GPConstants.CATEGORIES, "Test; MIT_701X; ActualA; ActualB ");

        // an empty list means, this module is in no categories
        final List<String> customCategoriesFromDb=new ArrayList<String>();
        
        CategoryManagerImpl real = new CategoryManagerImpl();
        CategoryManagerImpl spy = Mockito.spy(real);
        Mockito.when( spy.getCustomCategoriesFromDb(taskInfo) ).thenReturn(customCategoriesFromDb);

        List<String> categories=spy.getCategoriesForTask(userContext, taskInfo);
        Assert.assertNotNull("Expecting non-null value from getCategoriesForTask", categories);
        Assert.assertEquals("num categories", 0, categories.size());
    }

}
