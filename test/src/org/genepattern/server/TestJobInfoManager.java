/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.executor.drm.dao.JobRunnerJob;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


/**
 * junit tests for the JobInfoManager class.
 * @author pcarr
 *
 */
public class TestJobInfoManager {
    private JobRunnerJob job;

    private static final String lsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.visualizer:00261:999999999";
    private TaskInfoAttributes tia;
    private TaskInfo taskInfo;
    private HashMap<String, String> attrs;
    private ParameterInfo[] parameterInfos;
    private ParameterInfo pinfo;
    private JobInfo jobInfo;
    private final int jobNo=41;
    //private final String userId="admin";
    
    @Rule
    public TemporaryFolder temp= new TemporaryFolder();

    @Before
    public void setUp() throws MalformedURLException {
        job=mock(JobRunnerJob.class);
        final URL gpUrl=new URL("http://127.0.0.1:8080/gp/");
        GpConfig gpConfig=new GpConfig.Builder().genePatternURL(gpUrl).build();

        tia = new TaskInfoAttributes();
        tia.put(GPConstants.TASK_TYPE, GPConstants.TASK_TYPE_JAVASCRIPT);
        tia.put("commandLine", "clsfilecreator.html ? <input.file>");
        taskInfo=mock(TaskInfo.class);
        when(taskInfo.getLsid()).thenReturn(lsid);
        when(taskInfo.getTaskInfoAttributes()).thenReturn(tia);
        when(taskInfo.getAttributes()).thenReturn(tia);

        attrs=new HashMap<String, String>();
        attrs.put("default_value", "");
        attrs.put("optional", "");
        attrs.put("prefix_when_specified", "");
        attrs.put("MODE", "URL_IN");
        attrs.put("type", "java.io.File");
        attrs.put("fileFormat", "gct;res");
        
        pinfo=new ParameterInfo();
        pinfo.setName("input.file");
        pinfo.setAttributes(attrs);

        parameterInfos = new ParameterInfo[1];
        parameterInfos[0]=pinfo;
        
        jobInfo=mock(JobInfo.class);
        when(jobInfo.getJobNumber()).thenReturn(jobNo);
        when(jobInfo.getParameterInfoArray()).thenReturn(parameterInfos);
    }
    
    @Test
    public void getLaunchUrl_nullJobRunnerJob() throws Exception {
        job=null;
        assertEquals(
                // expected
                "",
                // actual
                JobInfoManager.getLaunchUrl(job)
                );
    }
    
    @Test
    public void getLaunchUrl_workingDirNull() throws Exception {
        when(job.getWorkingDir()).thenReturn(null);
        assertEquals(
                // expected
                "",
                // actual
                JobInfoManager.getLaunchUrl(job)
                );
    }

    @Test(expected=FileNotFoundException.class)
    public void getLaunchUrl_workingDirNotExists() throws Exception {
        String workingDir=new File("_"+Math.random()).getAbsolutePath();
        when(job.getWorkingDir()).thenReturn(workingDir);
        assertEquals(
                // expected
                "",
                // actual
                JobInfoManager.getLaunchUrl(job)
                );
    }
    
    @Test(expected=FileNotFoundException.class)
    public void getLaunchUrl_hiddenFileNotExists() throws IOException {
        final int jobId=1;
        final File workingDir=temp.newFolder(""+jobId);
        when(job.getWorkingDir()).thenReturn(workingDir.getAbsolutePath());
        JobInfoManager.getLaunchUrl(job);
    }

    @Test
    public void getLaunchUrl() throws IOException {
        File workingDir = FileUtil.getDataFile("jobResults/695");
        when(job.getWorkingDir()).thenReturn(workingDir.getAbsolutePath());
        assertEquals(
                "http://127.0.0.1:8080/gp/tasklib/urn:lsid:broad.mit.edu:cancer.software.genepattern.module.visualizer:00261:3.3/clsfilecreator.html?input.file=http://127.0.0.1:8080/gp/users/admin/all_aml_test.gct&",
                JobInfoManager.getLaunchUrl(job)
        );
    }

//    @Test
//    public void generateLaunchUrl_userUpload() throws Exception {
//        pinfo.setValue("<GenePatternURL>users/admin/all_aml_test.gct");
//        final String fileUrl="http://127.0.0.1:8080/gp/users/admin/all_aml_test.gct";
//        assertEquals(
//                // expected
//                "http://127.0.0.1:8080/gp/tasklib/"+lsid+"/clsfilecreator.html?job.number="+jobNo+"&input.file="+fileUrl,
//                // actual
//                JobInfoManager.generateLaunchURL(gpConfig,taskInfo, jobInfo.getJobNumber())
//                );
//    }
//    
//
//    @Test
//    public void generateLaunchUrl_jobUpload() throws Exception {
//        pinfo.setValue("<GenePatternURL>users/"+userId+"/tmp/run8743873107529768988.tmp/input.file/1/all_aml_train.gct");
//        final String fileUrl="http://127.0.0.1:8080/gp/users/"+userId+"/tmp/run8743873107529768988.tmp/input.file/1/all_aml_train.gct";
//        assertEquals(
//                // expected
//                "http://127.0.0.1:8080/gp/tasklib/"+lsid+"/clsfilecreator.html?job.number="+jobNo+"&input.file="+fileUrl,
//                // actual
//                JobInfoManager.generateLaunchURL(gpConfig, taskInfo, jobInfo.getJobNumber())
//                );
//    }
//    
//    @Test
//    public void generateLaunchUrl_externalUrl() throws Exception  {
//        pinfo.setValue("http://www.broadinstitute.org/cancer/software/genepattern/data/all_aml/all_aml_train.gct");
//        final String fileUrl="http://www.broadinstitute.org/cancer/software/genepattern/data/all_aml/all_aml_train.gct";
//        assertEquals(
//                // expected
//                "http://127.0.0.1:8080/gp/tasklib/"+lsid+"/clsfilecreator.html?job.number="+jobNo+"&input.file="+fileUrl,
//                // actual
//                JobInfoManager.generateLaunchURL(gpConfig, taskInfo, jobInfo.getJobNumber())
//                );
//    }

}
