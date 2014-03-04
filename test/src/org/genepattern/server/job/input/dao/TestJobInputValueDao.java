package org.genepattern.server.job.input.dao;

import java.io.File;

import org.genepattern.junitutil.AnalysisJobUtil;
import org.genepattern.junitutil.DbUtil;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.job.input.GroupId;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.webservice.TaskInfo;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestJobInputValueDao {
    final String userId="test_user";
    final String gpUrl="http://127.0.0.1:8080/gp/";
    final String cleLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2";
    final String cleZip="modules/ConvertLineEndings_v2.zip";
    
    @BeforeClass
    static public void beforeClass() throws Exception {
        DbUtil.initDb();
    }

    @AfterClass
    static public void afterClass() throws Exception {
        DbUtil.shutdownDb();
    } 
    
    @Test
    public void testAddJobInputValue() throws Exception { 
        JobInput jobInput=new JobInput();
        jobInput.setLsid(cleLsid);
        jobInput.addValue("input.filename", gpUrl+"users/"+userId+"/all_aml_test_01.cls", new GroupId("test"));
        jobInput.addValue("input.filename", gpUrl+"users/"+userId+"/all_aml_test_02.cls", new GroupId("test"));
        jobInput.addValue("input.filename", gpUrl+"users/"+userId+"/all_aml_test_01.gct", new GroupId("test"));
        jobInput.addValue("input.filename", gpUrl+"users/"+userId+"/all_aml_test_02.gct", new GroupId("test"));
        jobInput.addValue("input.filename", gpUrl+"users/"+userId+"/all_aml_train_01.cls", new GroupId("train"));
        jobInput.addValue("input.filename", gpUrl+"users/"+userId+"/all_aml_train_02.cls", new GroupId("train"));
        jobInput.addValue("input.filename", gpUrl+"users/"+userId+"/all_aml_train_01.gct", new GroupId("train"));
        jobInput.addValue("input.filename", gpUrl+"users/"+userId+"/all_aml_train_02.gct", new GroupId("train"));
        jobInput.addValue("output.file", "<input.filename_basename>.cvt.<input.filename_extension>");
        
        // example job config params
        jobInput.addValue("drm.queue", "broad");
        jobInput.addValue("drm.memory", "8gb");
        
        
        final File zipFile=FileUtil.getDataFile(cleZip);
        final TaskInfo taskInfo=TaskUtil.getTaskInfoFromZip(zipFile);
        final GpContext taskContext=GpContext.getContextForUser(userId);
        taskContext.setTaskInfo(taskInfo);
        AnalysisJobUtil jobUtil=new AnalysisJobUtil();
        final boolean initDefault=true;
        
        // (1) db transaction
        int jobNo=jobUtil.addJobToDb(taskContext, jobInput, initDefault);

        // (2) another db transaction
        new JobInputValueRecorder().saveJobInput(jobNo, jobInput);

        // (3) another db transaction
        JobInput jobInputOut = new JobInputValueRecorder().fetchJobInput(jobNo);

        Assert.assertEquals("numParams", 4, jobInputOut.getParams().size());
        Assert.assertEquals("input.filename[0]", gpUrl+"users/"+userId+"/all_aml_test_01.cls", jobInputOut.getParamValues("input.filename").get(0).getValue());
        Assert.assertEquals("input.filename[1]", gpUrl+"users/"+userId+"/all_aml_test_02.cls", jobInputOut.getParamValues("input.filename").get(1).getValue());
        Assert.assertEquals("input.filename[2]", gpUrl+"users/"+userId+"/all_aml_test_01.gct", jobInputOut.getParamValues("input.filename").get(2).getValue());
        Assert.assertEquals("input.filename[3]", gpUrl+"users/"+userId+"/all_aml_test_02.gct", jobInputOut.getParamValues("input.filename").get(3).getValue());
        Assert.assertEquals("input.filename[4]", gpUrl+"users/"+userId+"/all_aml_train_01.cls", jobInputOut.getParamValues("input.filename").get(4).getValue());
        Assert.assertEquals("input.filename[5]", gpUrl+"users/"+userId+"/all_aml_train_02.cls", jobInputOut.getParamValues("input.filename").get(5).getValue());
        Assert.assertEquals("input.filename[6]", gpUrl+"users/"+userId+"/all_aml_train_01.gct", jobInputOut.getParamValues("input.filename").get(6).getValue());
        Assert.assertEquals("input.filename[7]", gpUrl+"users/"+userId+"/all_aml_train_02.gct", jobInputOut.getParamValues("input.filename").get(7).getValue());
        
        Assert.assertEquals("output.file", "<input.filename_basename>.cvt.<input.filename_extension>", jobInputOut.getParam("output.file").getValues().get(0).getValue());
        
        Assert.assertEquals("drm.queue", "broad", jobInputOut.getParam("drm.queue").getValues().get(0).getValue());
        Assert.assertEquals("drm.memory", "8gb", jobInputOut.getParam("drm.memory").getValues().get(0).getValue());
        
        // verify cascade delete
        // (4) another db transaction
        jobUtil.deleteJobFromDb(jobNo);
        
        // (5) another db transaction
        jobInputOut = new JobInputValueRecorder().fetchJobInput(jobNo);
        Assert.assertEquals("jobInput.params.size after delete from analysis_job table", 0, jobInputOut.getParams().size());
    }

}
