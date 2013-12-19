package org.genepattern.server.job.input.collection;

import java.io.File;
import java.util.List;

import org.genepattern.junitutil.MockGpFilePath;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.input.GroupId;
import org.genepattern.server.job.input.GroupInfo;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.Param;
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
        final GroupInfo groupInfo=new GroupInfo.Builder()
            .min(0)
            .max(null)
            .build();
        
        File paramGroupFile=tmpDir.newFile("test_group.tsv");
        final GpFilePath toFile=new MockGpFilePath.Builder(paramGroupFile).build();
        final int jobNo=13;
        JobInfo jobInfo=new JobInfo();
        jobInfo.setJobNumber(jobNo);
        jobInfo.setUserId(userId);
        Context jobContext=ServerConfiguration.Context.getContextForJob(jobInfo);
        
        Assert.fail("test not implemented!");
        //ParamGroupHelper pgh=new ParamGroupHelper(toFile, inputParam, groupInfo);
        //List<GpFilePath> gpFilePaths=pgh.downloadExternalUrl(jobContext);
        //pgh.writeGroupFile(gpFilePaths);

    }
}
