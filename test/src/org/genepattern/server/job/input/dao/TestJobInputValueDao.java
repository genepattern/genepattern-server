/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.dao;

import static org.genepattern.junitutil.Demo.gpConfig;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.genepattern.junitutil.AnalysisJobUtil;
import org.genepattern.junitutil.DbUtil;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.job.input.GroupId;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.webservice.TaskInfo;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestJobInputValueDao {
    private static final String userId="test_user";
    private static final String baseGpHref="http://127.0.0.1:8080/gp";
    private static final String gpUrl=baseGpHref+"/";
    private static final String cleLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2";
    private static final String cleZip="modules/ConvertLineEndings_v2.zip";
    
    private static HibernateSessionManager mgr;
    private static GpConfig gpConfig;
    private static GpContext gpContext;
    
    @BeforeClass
    public static void beforeClass() throws ExecutionException {
        mgr=DbUtil.getTestDbSession();
        gpConfig=gpConfig();

        final File zipFile=FileUtil.getDataFile(cleZip);
        final TaskInfo taskInfo=TaskUtil.getTaskInfoFromZip(zipFile);
        gpContext=new GpContext.Builder()
            .userId(userId)
            .taskInfo(taskInfo)
            .build();
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

        // (1) db transaction
        final boolean initDefault=true;
        int jobNo=AnalysisJobUtil.addJobToDb(mgr, gpConfig, gpContext, jobInput, -1, initDefault);

        // (2) another db transaction
        new JobInputValueRecorder(mgr).saveJobInput(jobNo, jobInput);

        // (3) another db transaction
        JobInput jobInputOut = new JobInputValueRecorder(mgr).fetchJobInput(jobNo);

        assertEquals("numParams", 4, jobInputOut.getParams().size());
        assertEquals("input.filename[0]", gpUrl+"users/"+userId+"/all_aml_test_01.cls", jobInputOut.getParamValues("input.filename").get(0).getValue());
        assertEquals("input.filename[1]", gpUrl+"users/"+userId+"/all_aml_test_02.cls", jobInputOut.getParamValues("input.filename").get(1).getValue());
        assertEquals("input.filename[2]", gpUrl+"users/"+userId+"/all_aml_test_01.gct", jobInputOut.getParamValues("input.filename").get(2).getValue());
        assertEquals("input.filename[3]", gpUrl+"users/"+userId+"/all_aml_test_02.gct", jobInputOut.getParamValues("input.filename").get(3).getValue());
        assertEquals("input.filename[4]", gpUrl+"users/"+userId+"/all_aml_train_01.cls", jobInputOut.getParamValues("input.filename").get(4).getValue());
        assertEquals("input.filename[5]", gpUrl+"users/"+userId+"/all_aml_train_02.cls", jobInputOut.getParamValues("input.filename").get(5).getValue());
        assertEquals("input.filename[6]", gpUrl+"users/"+userId+"/all_aml_train_01.gct", jobInputOut.getParamValues("input.filename").get(6).getValue());
        assertEquals("input.filename[7]", gpUrl+"users/"+userId+"/all_aml_train_02.gct", jobInputOut.getParamValues("input.filename").get(7).getValue());
        
        assertEquals("output.file", "<input.filename_basename>.cvt.<input.filename_extension>", jobInputOut.getParam("output.file").getValues().get(0).getValue());
        
        assertEquals("job.queue", "broad", jobInputOut.getParam("job.queue").getValues().get(0).getValue());
        assertEquals("job.memory", "8gb", jobInputOut.getParam("job.memory").getValues().get(0).getValue());
        
        // verify cascade delete
        // (4) another db transaction
        AnalysisJobUtil.deleteJobFromDb(mgr, jobNo);
        
        // (5) another db transaction
        jobInputOut = new JobInputValueRecorder(mgr).fetchJobInput(jobNo);
        assertEquals("jobInput.params.size after delete from analysis_job table", 0, jobInputOut.getParams().size());
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
        int jobNo=AnalysisJobUtil.addJobToDb(mgr, gpConfig, gpContext, jobInput, -1, initDefault);
        new JobInputValueRecorder(mgr).saveJobInput(jobNo, jobInput);
        JobInput jobInputOut = new JobInputValueRecorder(mgr).fetchJobInput(jobNo);

        assertEquals("input.filename", gpUrl+"users/"+userId+"/all_aml_test_01.cls", jobInputOut.getParam("input.filename").getValues().get(0).getValue());
        assertEquals("job.queue", "broad", jobInputOut.getParam("job.queue").getValues().get(0).getValue());
        assertEquals("job.memory", "8gb", jobInputOut.getParam("job.memory").getValues().get(0).getValue());
        assertEquals("walltime (empty string)", "", jobInputOut.getParam("walltime").getValues().get(0).getValue());
        
        List<Integer> matchingJobs=new JobInputValueRecorder(mgr).fetchMatchingJobs(gpUrl+"users/"+userId+"/all_aml_test_01.cls");
        assertEquals("query jobIds by input param, num jobs", 1, matchingJobs.size());
        assertEquals("query jobIds by input param, job number", (Integer) jobNo, (Integer) matchingJobs.get(0));
        
        List<String> matchingGroups=new JobInputValueRecorder(mgr).fetchMatchingGroups(gpUrl+"users/"+userId+"/all_aml_test_01.cls");
        assertEquals("query groupIds by input param, num groups", 0, matchingGroups.size());
    }

    @Test
    public void updateValue_no_previous() throws Exception {
        JobInput jobInput=new JobInput();
        jobInput.addValue("input.filename", gpUrl+"users/"+userId+"/all_aml_test_01.cls");
        final boolean initDefault=true;
        int jobNo=AnalysisJobUtil.addJobToDb(mgr, gpConfig, gpContext, jobInput, -1, initDefault);
        new JobInputValueRecorder(mgr).saveJobInput(jobNo, jobInput);
        
        // validate initial input
        JobInput jobInputOut = new JobInputValueRecorder(mgr).fetchJobInput(jobNo);
        assertEquals("input.filename, initial input", gpUrl+"users/"+userId+"/all_aml_test_01.cls", jobInputOut.getParam("input.filename").getValues().get(0).getValue());
        
        // now update the initial input value
        JobInput updated=new JobInput();
        updated.addValue("input.filename", "ftp://ftp.broadinstitute.org/pub/genepattern/all_aml/all_aml_test.gct");
        new JobInputValueRecorder(mgr).saveJobInput(jobNo, updated);
        jobInputOut = new JobInputValueRecorder(mgr).fetchJobInput(jobNo);
        assertEquals("input.filename, updated value", 
                "ftp://ftp.broadinstitute.org/pub/genepattern/all_aml/all_aml_test.gct", 
                jobInputOut.getParam("input.filename").getValues().get(0).getValue());
        
        // now append values
        updated=new JobInput();
        // example job config params
        updated.addValue("job.queue", "broad");
        updated.addValue("job.memory", "8gb");
        updated.addValue("walltime", "");
        new JobInputValueRecorder(mgr).saveJobInput(jobNo, updated);
        
        jobInputOut = new JobInputValueRecorder(mgr).fetchJobInput(jobNo);
        //assertEquals("input.filename, updated input", gpUrl+"users/"+userId+"/all_aml_test_01.cls", jobInputOut.getParam("input.filename").getValues().get(0).getValue());
        assertEquals("job.queue", "broad", jobInputOut.getParam("job.queue").getValues().get(0).getValue());
        assertEquals("job.memory", "8gb", jobInputOut.getParam("job.memory").getValues().get(0).getValue());
        assertEquals("walltime (empty string)", "", jobInputOut.getParam("walltime").getValues().get(0).getValue());
    }
    
    @Test
    public void saveBaseGpHref() throws Exception {
        JobInput jobInput=new JobInput();
        jobInput.setLsid(cleLsid);
        jobInput.setBaseGpHref(baseGpHref);
        jobInput.addValue("input.filename", gpUrl+"users/"+userId+"/all_aml_test_03.cls");
        final boolean initDefault=true;
        int jobNo=AnalysisJobUtil.addJobToDb(mgr, gpConfig, gpContext, jobInput, -1, initDefault);
        new JobInputValueRecorder(mgr).saveJobInput(jobNo, jobInput);
        
        JobInput jobInputOut = new JobInputValueRecorder(mgr).fetchJobInput(jobNo);
        assertEquals("baseGpHref from DB", baseGpHref, jobInputOut.getBaseGpHref());
    }
    
}
