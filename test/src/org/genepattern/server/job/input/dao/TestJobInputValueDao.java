package org.genepattern.server.job.input.dao;

import java.io.File;
import java.util.List;

import org.genepattern.junitutil.AnalysisJobUtil;
import org.genepattern.junitutil.DbUtil;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.GpContextFactory;
import org.genepattern.server.job.input.GroupId;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.webservice.TaskInfo;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestJobInputValueDao {
    private static final String userId="test_user";
    private static final String gpUrl="http://127.0.0.1:8080/gp/";
    private static final String cleLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2";
    private static final String cleZip="modules/ConvertLineEndings_v2.zip";
    
    private GpContext gpContext;
    private AnalysisJobUtil jobUtil;
    
    @BeforeClass
    static public void beforeClass() throws Exception {
        DbUtil.initDb();
    }

    @AfterClass
    static public void afterClass() throws Exception {
        DbUtil.shutdownDb();
    } 
    
    @Before
    public void beforeTest() {
        final File zipFile=FileUtil.getDataFile(cleZip);
        final TaskInfo taskInfo=TaskUtil.getTaskInfoFromZip(zipFile);
        gpContext=new GpContextFactory.Builder()
            .userId(userId)
            .taskInfo(taskInfo)
            .build();
        jobUtil=new AnalysisJobUtil();
    }
    
    @Test
    public void testAddJobInputValue_fileGroup() throws Exception { 
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
        jobInput.addValue("job.queue", "broad");
        jobInput.addValue("job.memory", "8gb");
        
        
        final boolean initDefault=true;
        
        // (1) db transaction
        int jobNo=jobUtil.addJobToDb(gpContext, jobInput, initDefault);

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
        
        Assert.assertEquals("job.queue", "broad", jobInputOut.getParam("job.queue").getValues().get(0).getValue());
        Assert.assertEquals("job.memory", "8gb", jobInputOut.getParam("job.memory").getValues().get(0).getValue());
        
        // verify cascade delete
        // (4) another db transaction
        jobUtil.deleteJobFromDb(jobNo);
        
        // (5) another db transaction
        jobInputOut = new JobInputValueRecorder().fetchJobInput(jobNo);
        Assert.assertEquals("jobInput.params.size after delete from analysis_job table", 0, jobInputOut.getParams().size());
    }
    
    @Test
    public void testBlankValues() throws Exception {
        JobInput jobInput=new JobInput();
        jobInput.setLsid(cleLsid);
        jobInput.addValue("input.filename", gpUrl+"users/"+userId+"/all_aml_test_01.cls");
        // example job config params
        jobInput.addValue("job.queue", "broad");
        jobInput.addValue("job.memory", "8gb");
        jobInput.addValue("walltime", "");

        final boolean initDefault=true;
        int jobNo=jobUtil.addJobToDb(gpContext, jobInput, initDefault);
        new JobInputValueRecorder().saveJobInput(jobNo, jobInput);
        JobInput jobInputOut = new JobInputValueRecorder().fetchJobInput(jobNo);

        Assert.assertEquals("input.filename", gpUrl+"users/"+userId+"/all_aml_test_01.cls", jobInputOut.getParam("input.filename").getValues().get(0).getValue());
        Assert.assertEquals("job.queue", "broad", jobInputOut.getParam("job.queue").getValues().get(0).getValue());
        Assert.assertEquals("job.memory", "8gb", jobInputOut.getParam("job.memory").getValues().get(0).getValue());
        Assert.assertEquals("walltime (empty string)", "", jobInputOut.getParam("walltime").getValues().get(0).getValue());
        
        List<Integer> matchingJobs=new JobInputValueRecorder().fetchMatchingJobs(gpUrl+"users/"+userId+"/all_aml_test_01.cls");
        Assert.assertEquals("query jobIds by input param, num jobs", 1, matchingJobs.size());
        Assert.assertEquals("query jobIds by input param, job number", (Integer) jobNo, (Integer) matchingJobs.get(0));
        
        List<String> matchingGroups=new JobInputValueRecorder().fetchMatchingGroups(gpUrl+"users/"+userId+"/all_aml_test_01.cls");
        Assert.assertEquals("query groupIds by input param, num groups", 0, matchingGroups.size());
    }

}
