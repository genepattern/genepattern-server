/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.collection;

import java.io.File;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.junitutil.MockGpFilePath;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.input.GroupId;
import org.genepattern.server.job.input.GroupInfo;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamId;
import org.genepattern.server.job.input.TestJobInput;
import org.genepattern.webservice.JobInfo;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * jUnit tests for the ParamGroupHelper class
 * @author pcarr
 *
 */
public class TestParamGroupHelper {
    private static final String lsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.test.analysis:00006:0.7";
    
    @Rule
    public TemporaryFolder tmpDir=new TemporaryFolder();

    @Test
    public void testWriteGroupFile() throws Exception {
        final String userId="test";
        final JobInput jobInput = new JobInput();
        jobInput.setLsid(lsid);
        jobInput.addValue("inputList", TestJobInput.DATA_URL+"all_aml_train.res", new GroupId("train"));
        jobInput.addValue("inputList", TestJobInput.DATA_URL+"all_aml_test.res", new GroupId("test"));
        jobInput.addValue("inputList", TestJobInput.DATA_URL+"all_aml_test.cls", new GroupId("TEST"));
        jobInput.addValue("inputList", TestJobInput.DATA_URL+"all_aml_test.gct", new GroupId(" test "));
        jobInput.addValue("inputList", TestJobInput.DATA_URL+"all_aml_train.cls", new GroupId(" train "));
        jobInput.addValue("inputList", TestJobInput.DATA_URL+"all_aml_train.gct", new GroupId("Train"));
        final Param inputParam=jobInput.getParam("inputList");
        Assert.assertEquals("numGroups", 2, inputParam.getNumGroups());
        final GroupInfo groupInfo=new GroupInfo.Builder()
            .min(0)
            .max(null)
            .build();
        
        final File paramGroupFile=tmpDir.newFile("test_group.tsv");
        final GpFilePath toFile=new MockGpFilePath.Builder(paramGroupFile).build();
        final int jobNo=13;
        final JobInfo jobInfo=new JobInfo();
        jobInfo.setJobNumber(jobNo);
        jobInfo.setUserId(userId);
        
        final HibernateSessionManager mgr=DbUtil.getTestDbSession();
        final GpConfig gpConfig=new GpConfig.Builder().build();
        final GpContext jobContext=GpContext.getContextForJob(jobInfo);

        final ParamGroupHelper pgh=new ParamGroupHelper.Builder(jobInput.getParam(new ParamId("inputList")))
            .mgr(mgr)
            .gpConfig(gpConfig)
            .jobContext(jobContext)
            .groupInfo(groupInfo)
            .downloadExternalFiles(false)
            .toFile(toFile)
            .build();
        
        final GpFilePath gpFilePath=pgh.createFilelist();
        Assert.assertEquals(toFile.getServerFile(), gpFilePath.getServerFile());
    }
}
