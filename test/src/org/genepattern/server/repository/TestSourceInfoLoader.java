/*******************************************************************************
 * Copyright (c) 2003-2018 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.repository;

import org.genepattern.junitutil.TaskLoader;
import org.genepattern.server.job.input.TestJobInputHelper;
import org.genepattern.server.taskinstall.InstallInfo;
import org.genepattern.webservice.TaskInfo;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * jUnit tests for the SourceInfoLoader class.
 * 
 * @author pcarr
 *
 */
public class TestSourceInfoLoader {
    private static TaskLoader taskLoader;
    private static SourceInfoLoader sourceInfoLoader;

    //ConvertLineEndings v1
    final String cleLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:1";
    //ComparativeMarkerSelection v9
    final String cmsLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:9";
    //PreprocessDataset v4
    final String pdLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00020:4";
    //ListFiles v0.7
    final String listFilesLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00275:0.7";
    
    @BeforeClass
    static public void init() {
        sourceInfoLoader=new StubSourceInfoLoader();

        taskLoader=new TaskLoader();
        taskLoader.addTask(TestJobInputHelper.class, "ConvertLineEndings_v1.zip");
        taskLoader.addTask(TestJobInputHelper.class, "ComparativeMarkerSelection_v9.zip");
        taskLoader.addTask(TestJobInputHelper.class, "PreprocessDataset_v4.zip");
        taskLoader.addTask(TestJobInputHelper.class, "ListFiles_v0.7.zip");
    }
    
    @Test
    public void testFromGPProd() {
        final TaskInfo cleTaskInfo=taskLoader.getTaskInfo(cleLsid);
        final SourceInfo fromGPProd=sourceInfoLoader.getSourceInfo(cleTaskInfo);
        Assert.assertNotNull("expecting non-null sourceInfo", fromGPProd);
        Assert.assertEquals("sourceInfo.type", InstallInfo.Type.REPOSITORY, fromGPProd.getType());
        Assert.assertEquals("sourceInfo.label", "GenePattern production (new)", fromGPProd.getLabel());
        Assert.assertNotNull("sourceInfo.iconImgSrc, unexpected null value", fromGPProd.getIconImgSrc());
        Assert.assertNotNull("sourceInfo.briefDescription, unexpected null value", fromGPProd.getBriefDescription());
        Assert.assertNotNull("sourceInfo.fullDescription, unexpected null value", fromGPProd.getFullDescription());
    }
    
    @Test
    public void testFromGparc() {
        final TaskInfo cmsTaskInfo=taskLoader.getTaskInfo(cmsLsid);
        final SourceInfo fromGparc=sourceInfoLoader.getSourceInfo(cmsTaskInfo);
        Assert.assertNotNull("expecting non-null sourceInfo", fromGparc);
        Assert.assertEquals("sourceInfo.type", InstallInfo.Type.REPOSITORY, fromGparc.getType());
        Assert.assertEquals("sourceInfo.label", "GParc (GenePattern Archive)", fromGparc.getLabel());
        Assert.assertNotNull("sourceInfo.iconImgSrc, unexpected null value", fromGparc.getIconImgSrc());
        Assert.assertNotNull("sourceInfo.briefDescription, unexpected null value", fromGparc.getBriefDescription());
        Assert.assertNotNull("sourceInfo.fullDescription, unexpected null value", fromGparc.getFullDescription());
    }
    
    @Test
    public void testFromGPBeta() {
        final TaskInfo pdTaskInfo=taskLoader.getTaskInfo(pdLsid);
        final SourceInfo fromGPBeta=sourceInfoLoader.getSourceInfo(pdTaskInfo);
        Assert.assertNotNull("expecting non-null sourceInfo", fromGPBeta);
        Assert.assertEquals("sourceInfo.type", InstallInfo.Type.REPOSITORY, fromGPBeta.getType());
        Assert.assertEquals("sourceInfo.label", "GP beta (new)", fromGPBeta.getLabel());
        Assert.assertNotNull("sourceInfo.iconImgSrc, unexpected null value", fromGPBeta.getIconImgSrc());
        Assert.assertNotNull("sourceInfo.briefDescription, unexpected null value", fromGPBeta.getBriefDescription());
        Assert.assertNotNull("sourceInfo.fullDescription, unexpected null value", fromGPBeta.getFullDescription());
    }
    
    @Test
    public void testFromUnknown() {
        final TaskInfo listFilesTaskInfo=taskLoader.getTaskInfo(listFilesLsid);
        final SourceInfo fromUnknown=sourceInfoLoader.getSourceInfo(listFilesTaskInfo);
        Assert.assertNotNull("expecting non-null sourceInfo", fromUnknown);
        Assert.assertEquals("sourceInfo.type", InstallInfo.Type.UNKNOWN, fromUnknown.getType());
        Assert.assertEquals("sourceInfo.label", "N/A", fromUnknown.getLabel());
        Assert.assertNotNull("sourceInfo.iconImgSrc, expecting non-null value", fromUnknown.getIconImgSrc());
        Assert.assertEquals("sourceInfo.briefDescription", "Installation source not known, module was installed before the GP 3.6.1 update", fromUnknown.getBriefDescription());
        Assert.assertNull("sourceInfo.fullDescription, expecting null value", fromUnknown.getFullDescription());
    }

}
