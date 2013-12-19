package org.genepattern.server.job.input.collection;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.genepattern.junitutil.MockGpFilePath;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.input.GroupId;
import org.genepattern.server.job.input.GroupInfo;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamId;
import org.genepattern.server.job.input.ParamValue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * junit test for writing the paramgroup file.
 * @author pcarr
 *
 */
public class TestDefaultParamGroupWriter {

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();
    
    private void appendValue(final Param inputParam, final List<GpFilePath> gpFilePaths, final GroupId groupId, final String filepath) 
    throws IOException
    {
        final File localFile = tmpDir.newFile(filepath);
        //final File localFile = new File(tmpDir, filepath);
        GpFilePath gpFilePath=new MockGpFilePath.Builder(localFile).build();
        inputParam.addValue(new GroupId(groupId), new ParamValue(gpFilePath.getParamInfoValue())); 
        gpFilePaths.add(gpFilePath);
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
        
        final File toFile=tmpDir.newFile("exampleParamGroup_01.tsv");
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
        
        final File toFile=tmpDir.newFile("exampleParamGroup_02.tsv");
        final DefaultParamGroupWriter writer=new DefaultParamGroupWriter.Builder(toFile).build();
        writer.writeParamGroup(groupInfo, inputParam, gpFilePaths);
        Assert.assertEquals("toFile.exists", true, toFile.exists());
    }

}
