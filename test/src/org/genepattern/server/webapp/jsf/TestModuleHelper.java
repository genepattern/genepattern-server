package org.genepattern.server.webapp.jsf;

import java.io.File;
import java.util.List;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.eula.TestEulaInfo;
import org.genepattern.webservice.TaskInfo;
import org.junit.Assert;
import org.junit.Test;

/**
 * junit tests for the legacy (<= 3.7.0) ModuleHelper class for the JSF based Modules & Pipelines panel.
 * 
 * @author pcarr
 *
 */
public class TestModuleHelper {
    /*
     * Test-cases
     * 1) get category for legacy pipeline
     * 2) get category for legacy module
     * 3) get custom category for pipeline from manifest ...
     * 3a) standard mode, show in 'pipeline' and '<custom>'
     * 3b) hidden mode, should not have any category
     * 3c) don't include in 'pipeline' category
     */
    
    /**
     * The legacy Golub pipeline should be in the 'pipeline' category.
     */
    @Test
    public void testGolubPipeline() {
        final TaskInfo taskInfo=TaskUtil.getTaskInfoFromZip(TestEulaInfo.class, "Golub.Slonim.1999.Nature.all.aml.pipeline_v2_modules_only.zip");
        final List<String> categories=ModuleHelper.getCategoriesForTask(taskInfo, true);
        Assert.assertNotNull("Expecting non-null value from getCategoriesForTask", categories);
        Assert.assertEquals("num categories", 1, categories.size());
        Assert.assertEquals("category[0]", "pipeline", categories.get(0));
    }
    
    @Test
    public void testCategorizedVisualizer() {
        final File zipFile=FileUtil.getDataFile("modules/MIT_701X_OriginalDataViewer_categorized.zip");
        final TaskInfo taskInfo=TaskUtil.getTaskInfoFromZip(zipFile);
        final List<String> categories=ModuleHelper.getCategoriesForTask(taskInfo, true);
        Assert.assertNotNull("Expecting non-null value from getCategoriesForTask", categories);
        Assert.assertEquals("num categories", 2, categories.size());
        Assert.assertEquals("category[0]", "Visualizer", categories.get(0));
        Assert.assertEquals("category[1]", "MIT_701X", categories.get(1));
    }
    
    @Test
    public void testCategorizedPipeline() {
        final File zipFile=FileUtil.getDataFile("modules/MIT_701X_processClusteringData.pipeline_categorized.zip");
        final TaskInfo taskInfo=TaskUtil.getTaskInfoFromZip(zipFile);
        final List<String> categories=ModuleHelper.getCategoriesForTask(taskInfo, true);
        Assert.assertNotNull("Expecting non-null value from getCategoriesForTask", categories);
        Assert.assertEquals("num categories", 2, categories.size());
        Assert.assertEquals("category[0]", "pipeline", categories.get(0));
        Assert.assertEquals("category[1]", "MIT_701X", categories.get(1));
    }

}
