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
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.genepattern.JavascriptHandler;
import org.genepattern.util.GPConstants;
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
    // use non-default ('/gp') servlet context path
    final String gpContextPath="/gp-custom-context-path";
    // default GenePatternURL set in 'genepattern.properties'
    final String GenePatternURL="http://127.0.0.1:8080/gp/"; 
    // client GpUrl as set in location bar in web browser
    final String clientGpUrl="http://wm97e-54f.broadinstitute.org:8080"+gpContextPath;

    private GpConfig gpConfig;
    private GpContext gpContext;
    private Map<String, List<String>> substitutedValues=new LinkedHashMap<String,List<String>>();

    private static final String lsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.visualizer:00261:999999999";
    private TaskInfo taskInfo;
    private final int jobNo=41;
    
    @Rule
    public TemporaryFolder temp= new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        File rootJobDir=FileUtil.getDataFile("jobResults");
        gpConfig=new GpConfig.Builder()
            .gpServletContext(gpContextPath)
            .genePatternURL(new URL(GenePatternURL))
            .addProperty(GpConfig.PROP_JOBS, rootJobDir.getAbsolutePath())
        .build();
        gpContext=new GpContext.Builder().build();

        substitutedValues.put("job.number", Arrays.asList(""+jobNo));

        final TaskInfoAttributes tia = new TaskInfoAttributes();
        tia.put(GPConstants.TASK_TYPE, GPConstants.TASK_TYPE_JAVASCRIPT);
        tia.put("commandLine", "clsfilecreator.html ? <input.file>");
        taskInfo=mock(TaskInfo.class);
        when(taskInfo.getLsid()).thenReturn(lsid);
        when(taskInfo.getTaskInfoAttributes()).thenReturn(tia);
        when(taskInfo.getAttributes()).thenReturn(tia);
    }
    
    @Test(expected=FileNotFoundException.class)
    public void getLaunchUrl_rootJobDirNotExists() throws Exception {
        File rootJobDir=temp.newFolder("_"+Math.random());
        gpConfig=new GpConfig.Builder()
            .genePatternURL(new URL(GenePatternURL))
            .addProperty(GpConfig.PROP_JOBS, rootJobDir.getAbsolutePath())
        .build();
        JobInfoManager.getLaunchUrl(gpConfig, gpContext, 695);
    }

    @Test(expected=FileNotFoundException.class)
    public void getLaunchUrl_jobDirNotExists() throws Exception {
        assertEquals(
                // expected
                "",
                // actual
                JobInfoManager.getLaunchUrl(gpConfig, gpContext, 1)
                );
    }
    
    @Test(expected=FileNotFoundException.class)
    public void getLaunchUrl_launchUrlFileNotExists() throws IOException {
        final int jobId=0;
        JobInfoManager.getLaunchUrl(gpConfig, gpContext, jobId);
    }

    @Test
    public void getLaunchUrl_from_file_in_job_dir() throws IOException {
        final String expected=
                gpContextPath+"/tasklib/urn:lsid:broad.mit.edu:cancer.software.genepattern.module.visualizer:00261:3.3/clsfilecreator.html?input.file=http%3A%2F%2Fwm97e-54f.broadinstitute.org%3A8080%2Fgp%2Fusers%2Fadmin%2Fall_aml_test.gct";
        
        assertEquals(
                expected,
                JobInfoManager.getLaunchUrl(gpConfig, gpContext, 695)
        );
    }

    @Test
    public void generateLaunchUrl_userUpload() throws Exception {
        final String fileUrl=clientGpUrl+"/users/admin/all_aml_test.gct";
        substitutedValues.put("input.file", Arrays.asList(clientGpUrl+"/users/admin/all_aml_test.gct")); 
        assertEquals(
                // expected
                //clientGpUrl+
                gpContextPath+"/tasklib/"+lsid+"/clsfilecreator.html?job.number="+jobNo+"&input.file="+ URLEncoder.encode(fileUrl, "UTF-8"),
                // actual
                JavascriptHandler.generateLaunchUrl(gpConfig, taskInfo, substitutedValues)
                );
    }

    @Test
    public void generateLaunchUrl_jobUpload() throws Exception {
        final String fileUrl=clientGpUrl+"users/test/tmp/run8743873107529768988.tmp/input.file/1/all_aml_train.gct";
        substitutedValues.put("input.file", Arrays.asList(fileUrl));
        assertEquals(
                // expected
                gpContextPath+"/tasklib/"+lsid+"/clsfilecreator.html?job.number="+jobNo+"&input.file="+ URLEncoder.encode(fileUrl, "UTF-8"),
                // actual
                JavascriptHandler.generateLaunchUrl(gpConfig, taskInfo, substitutedValues)
                );
    }

    @Test
    public void generateLaunchUrl_externalUrl() throws Exception  {
        final String fileUrl="http://www.broadinstitute.org/cancer/software/genepattern/data/all_aml/all_aml_train.gct";
        substitutedValues.put("input.file", Arrays.asList(fileUrl));
        assertEquals(
                // expected
                gpContextPath+"/tasklib/"+lsid+"/clsfilecreator.html?job.number="+jobNo+"&input.file="+URLEncoder.encode(fileUrl, "UTF-8"),
                // actual
                JavascriptHandler.generateLaunchUrl(gpConfig, taskInfo, substitutedValues)
                );
    }

}
