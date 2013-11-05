package org.genepattern.server.job.input.choice;

import java.io.File;

import org.genepattern.junitutil.FileUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * junit tests for the LocalDirFilter class, also tests most of the functionality in the parent class.
 * 
 * @author pcarr
 *
 */
public class TestLocalDirFilter {
    private static File parentDir;
    
    @BeforeClass
    public static void beforeClass() {
        parentDir=new File(FileUtil.getDataDir(), "all_aml");
    }

    /**
     * By default, accept any file which is a file (File#isFile == true).
     */
    @Test
    public void testDefaultFilter() {
        final LocalDirFilter filter=new LocalDirFilter(null);
        final File inputFile=new File(parentDir, "all_aml_test.gct");
        Assert.assertTrue("accept(inputFile)", filter.accept(inputFile));
        Assert.assertFalse("accept(parentDir)", filter.accept(parentDir));
    }

    /**
     * Test 'choiceDirFilter=type=any'.
     */
    @Test
    public void testTypeIsAny() {
        final LocalDirFilter filter=new LocalDirFilter("type=any");
        final File inputFile=new File(parentDir, "all_aml_test.gct");
        Assert.assertTrue("accept(inputFile)", filter.accept(inputFile));
        Assert.assertTrue("accept(parentDir)", filter.accept(parentDir));
    }
    
    /**
     * Test 'choiceDirFilter=type=file'.
     */
    @Test
    public void testTypeIsFile() {
        final LocalDirFilter filter=new LocalDirFilter("type=file");
        final File inputFile=new File(parentDir, "all_aml_test.gct");
        Assert.assertTrue("accept(inputFile)", filter.accept(inputFile));
        Assert.assertFalse("accept(parentDir)", filter.accept(parentDir));
    }
    
    /**
     * Test 'choiceDirFilter=type=dir'.
     */
    @Test
    public void testTypeIsDir() {
        final LocalDirFilter filter=new LocalDirFilter("type=dir");
        Assert.assertEquals("accept(parentDir)", true, filter.accept(parentDir));        
        Assert.assertEquals("accept(parentDir)", false, filter.accept(new File(parentDir, "all_aml_test.gct")));        
    }
    
    /**
     * Test 'choiceDirFilter=*.gct'.
     */
    @Test
    public void testFilterFileByGct() {
        final LocalDirFilter filter=new LocalDirFilter("*.gct");
        final File inputFile=new File(parentDir, "all_aml_test.gct");
        Assert.assertTrue("accept(all_aml_test.gct)", filter.accept(inputFile));
        Assert.assertFalse("accept(all_aml_test.cls)", filter.accept(new File(parentDir, "all_aml_test.cls")));
    }

    /**
     * Test 'choiceDirFilter=!*.gct'.
     */
    @Test
    public void testNotGct() {
        final LocalDirFilter filter=new LocalDirFilter("!*.gct");
        final File inputFile=new File(parentDir, "all_aml_test.gct");
        Assert.assertFalse("accept(all_aml_test.gct)", filter.accept(inputFile));
        Assert.assertTrue("accept(all_aml_test.cls)", filter.accept(new File(parentDir, "all_aml_test.cls")));
    }

    /**
     * Test 'choiceDirFilter=*.gct;*.cls'.
     */
    @Test
    public void testMulti() {
        final LocalDirFilter filter=new LocalDirFilter("*.gct;*.cls");
        final File inputFile=new File(parentDir, "all_aml_test.gct");
        Assert.assertTrue("accept(all_aml_test.gct)", filter.accept(inputFile));
        Assert.assertTrue("accept(all_aml_test.cls)", filter.accept(new File(parentDir, "all_aml_test.cls")));
        Assert.assertFalse("accept(all_aml_test.res)", filter.accept(new File(parentDir, "all_aml_test.res")));
        Assert.assertFalse("accept(Golub_et_al_1999.R)", filter.accept(new File(parentDir, "Golub_et_al_1999.R")));
    }

    /**
     * Test 'choiceDirFilter=type=dir;*all_aml*'.
     */
    @Test
    public void testMultiWithDir() {
        final LocalDirFilter filter=new LocalDirFilter("type=dir;*all_aml*");
        Assert.assertTrue("accept(all_aml/)", filter.accept(parentDir));
        Assert.assertFalse("accept(sub/)", filter.accept(new File(parentDir,"sub")));
        Assert.assertFalse("accept(all_aml_test.gct)", filter.accept(new File(parentDir, "all_aml_test.gct")));
        Assert.assertFalse("accept(all_aml_test.cls)", filter.accept(new File(parentDir, "all_aml_test.cls")));
        Assert.assertFalse("accept(all_aml_test.res)", filter.accept(new File(parentDir, "all_aml_test.res")));
        Assert.assertFalse("accept(Golub_et_al_1999.R)", filter.accept(new File(parentDir, "Golub_et_al_1999.R")));
    }

    /**
     * Test 'choiceDirFilter=!*.gct;!*.cls'.
     */
    @Test
    public void testMultiAnti() {
        final LocalDirFilter filter=new LocalDirFilter("!*.gct;!*.cls");
        final File inputFile=new File(parentDir, "all_aml_test.gct");
        Assert.assertFalse("accept(all_aml_test.gct)", filter.accept(inputFile));
        Assert.assertFalse("accept(all_aml_test.cls)", filter.accept(new File(parentDir, "all_aml_test.cls")));
        Assert.assertTrue("accept(all_aml_test.res)", filter.accept(new File(parentDir, "all_aml_test.res")));
        Assert.assertTrue("accept(Golub_et_al_1999.R)", filter.accept(new File(parentDir, "Golub_et_al_1999.R")));
    }
    
    @Test
    public void testTrimOuterWhitespace() {
        final LocalDirFilter filter=new LocalDirFilter(" *.gct ");
        final File inputFile=new File(parentDir, "all_aml_test.gct");
        Assert.assertTrue("accept(all_aml_test.gct)", filter.accept(inputFile));
        Assert.assertFalse("accept(all_aml_test.cls)", filter.accept(new File(parentDir, "all_aml_test.cls")));
    }
    
    @Test
    public void testTrimInnerWhitespace() {
        final LocalDirFilter filter=new LocalDirFilter(" type=file ; *.gct ; *.cls ");
        final File inputFile=new File(parentDir, "all_aml_test.gct");
        Assert.assertTrue("accept(all_aml_test.gct)", filter.accept(inputFile));
        Assert.assertTrue("accept(all_aml_test.cls)", filter.accept(new File(parentDir, "all_aml_test.cls")));
        Assert.assertFalse("accept(all_aml_test.res)", filter.accept(new File(parentDir, "all_aml_test.res")));
        Assert.assertFalse("accept(Golub_et_al_1999.R)", filter.accept(new File(parentDir, "Golub_et_al_1999.R")));
    }
}
