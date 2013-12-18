package org.genepattern.server.job.input.collection;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.genepattern.junitutil.MockGpFilePath;
import org.genepattern.junitutil.TempFileUtil;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.input.GroupId;
import org.genepattern.server.job.input.GroupInfo;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamId;
import org.genepattern.server.job.input.ParamValue;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * junit test for writing the paramgroup file.
 * @author pcarr
 *
 */
public class TestDefaultParamGroupWriter {
    private static TempFileUtil tempFileUtil;
    private static File tmpDir;

    private static void appendValue(final Param inputParam, final List<GpFilePath> gpFilePaths, final GroupId groupId, final String filepath) {
        final File localFile = new File(tmpDir, filepath);
        GpFilePath gpFilePath=new MockGpFilePath.Builder(localFile).build();
        inputParam.addValue(new GroupId(groupId), new ParamValue(gpFilePath.getParamInfoValue())); 
        gpFilePaths.add(gpFilePath);
    }
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        tempFileUtil=new TempFileUtil();
        tmpDir=tempFileUtil.newTmpDir();
    }
    
    @AfterClass
    public static void afterClass() throws Exception {
        tempFileUtil.cleanup();
    }

    @Test
    public void testWriteParamGroup() throws Exception {
        final GroupInfo groupInfo=null;

        final boolean isBatch=false;
        final List<GpFilePath> gpFilePaths=new ArrayList<GpFilePath>();
        final Param inputParam=new Param(new ParamId("inputFiles"), isBatch);
        
        appendValue(inputParam, gpFilePaths, new GroupId("test"), "all_aml_test.cls");
        appendValue(inputParam, gpFilePaths, new GroupId("test"), "all_aml_test.gct");
        appendValue(inputParam, gpFilePaths, new GroupId("test"), "all_aml_test.res");
        appendValue(inputParam, gpFilePaths, new GroupId("train"), "all_aml_train.cls");
        appendValue(inputParam, gpFilePaths, new GroupId("train"), "all_aml_train.gct");
        appendValue(inputParam, gpFilePaths, new GroupId("train"), "all_aml_train.res");
        
        final File toFile=new File(tmpDir, "exampleParamGroup_01.tsv");
        final DefaultParamGroupWriter writer=new DefaultParamGroupWriter.Builder(toFile).build();
        writer.writeParamGroup(groupInfo, inputParam, gpFilePaths);
        
        Assert.assertEquals("toFile.exists", true, toFile.exists());
    }

    /**
     * Verify that the custom headers are written from the groupInfo
     */
    @Test
    public void testCustomHeaders() throws Exception {
        final GroupInfo groupInfo=new GroupInfo.Builder()
            .min(0)
            .max(null)
            .fileColumnLabel("replicate")
            .groupColumnLabel("sample type")
            .build();

        final boolean isBatch=false;
        final List<GpFilePath> gpFilePaths=new ArrayList<GpFilePath>();
        final Param inputParam=new Param(new ParamId("inputFiles"), isBatch);
        
        appendValue(inputParam, gpFilePaths, new GroupId("test"), "all_aml_test.cls");
        appendValue(inputParam, gpFilePaths, new GroupId("test"), "all_aml_test.gct");
        appendValue(inputParam, gpFilePaths, new GroupId("test"), "all_aml_test.res");
        appendValue(inputParam, gpFilePaths, new GroupId("train"), "all_aml_train.cls");
        appendValue(inputParam, gpFilePaths, new GroupId("train"), "all_aml_train.gct");
        appendValue(inputParam, gpFilePaths, new GroupId("train"), "all_aml_train.res");
        
        final File toFile=new File(tmpDir, "exampleParamGroup_02.tsv");
        final DefaultParamGroupWriter writer=new DefaultParamGroupWriter.Builder(toFile).build();
        writer.writeParamGroup(groupInfo, inputParam, gpFilePaths);
        Assert.assertEquals("toFile.exists", true, toFile.exists());
    }

}
