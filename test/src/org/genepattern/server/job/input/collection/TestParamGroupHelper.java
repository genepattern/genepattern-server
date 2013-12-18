package org.genepattern.server.job.input.collection;

import java.io.File;
import java.util.List;

import org.genepattern.junitutil.TempFileUtil;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.serverfile.ServerFilePath;
import org.genepattern.server.job.input.GroupId;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.TestJobInput;
import org.genepattern.server.job.input.collection.ParamGroupHelper;
import org.genepattern.webservice.JobInfo;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * jUnit tests for the ParamGroupHelper class
 * @author pcarr
 *
 */
public class TestParamGroupHelper {
    private static final String lsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.test.analysis:00006:0.7";
    private static TempFileUtil tmpFileUtil;
    
    @BeforeClass
    public static void beforeClass() {
        tmpFileUtil=new TempFileUtil();
    }
    
    @AfterClass
    public static void afterClass() throws Exception {
        tmpFileUtil.cleanup();
    }

    @Test
    public void testWriteGroupFile() throws Exception {
        final String userId="test";
        final JobInput jobInput = new JobInput();
        jobInput.setLsid(lsid);
        jobInput.addValue(new GroupId("train"), "inputList", TestJobInput.DATA_URL+"all_aml_train.res");
        jobInput.addValue(new GroupId("test"), "inputList", TestJobInput.DATA_URL+"all_aml_test.res");
        jobInput.addValue(new GroupId("TEST"), "inputList", TestJobInput.DATA_URL+"all_aml_test.cls");
        jobInput.addValue(new GroupId(" test "), "inputList", TestJobInput.DATA_URL+"all_aml_test.gct");
        jobInput.addValue(new GroupId(" train "), "inputList", TestJobInput.DATA_URL+"all_aml_train.cls");
        jobInput.addValue(new GroupId("Train"), "inputList", TestJobInput.DATA_URL+"all_aml_train.gct");
        final Param inputParam=jobInput.getParam("inputList");

        
        final File tmpDir=tmpFileUtil.newTmpDir();
        final GpFilePath toFile=new ServerFilePath( new File(tmpDir,"test.group.csv") );
        final int jobNo=13;
        JobInfo jobInfo=new JobInfo();
        jobInfo.setJobNumber(jobNo);
        jobInfo.setUserId(userId);
        Context jobContext=ServerConfiguration.Context.getContextForJob(jobInfo);
        //ParamGroupHelper pgh=ParamGroupHelper.create(jobContext, inputParam);
        ParamGroupHelper pgh=new ParamGroupHelper(toFile, inputParam);
        List<GpFilePath> gpFilePaths=pgh.downloadExternalUrl(jobContext);
        pgh.writeGroupFile(gpFilePaths);

    }
}
