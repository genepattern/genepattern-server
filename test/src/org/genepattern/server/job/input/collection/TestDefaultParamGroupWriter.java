/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.collection;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

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
        final GpFilePath gpFilePath=new MockGpFilePath.Builder(localFile).build();
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
        
        assertEquals("toFile.exists", true, toFile.exists());
        
        //parse the results
        LineNumberReader reader=null;
        try {
            reader=new LineNumberReader(new FileReader(toFile));
            checkLine(0, reader.readLine(), gpFilePaths, "test");
            checkLine(1, reader.readLine(), gpFilePaths, "test");
            checkLine(2, reader.readLine(), gpFilePaths, "test");
            checkLine(3, reader.readLine(), gpFilePaths, "train");
            checkLine(4, reader.readLine(), gpFilePaths, "train");
            checkLine(5, reader.readLine(), gpFilePaths, "train");
            assertNull( "", reader.readLine() );
        }
        finally {
            if (reader != null) {
                reader.close();
            }
        }
    }
    
    private void checkLine(final int i, final String line, final List<GpFilePath> gpFilePaths, final String groupId) {
        final String[] items=line.split("\t");
        assertEquals("filename["+i+"]", gpFilePaths.get(i).getServerFile().toString(), items[0]);
        assertEquals("group["+i+"]", groupId, items[1]);
        try {
            assertEquals("url["+i+"]", gpFilePaths.get(i).getUrl().toString(), items[2]);
        }
        catch (Exception e) {
            fail("Error parsing url["+i+"]: "+e.getLocalizedMessage());
        }
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
        assertEquals("toFile.exists", true, toFile.exists());
    }

}
