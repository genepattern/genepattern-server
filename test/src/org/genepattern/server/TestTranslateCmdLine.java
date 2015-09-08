/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.genepattern.CommandLineParser;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * junit tests for the CommandLineParser#translateCmdLine class.
 * @author pcarr
 *
 */
public class TestTranslateCmdLine {
    private GpConfig gpConfig;
    private GpContext gpContext;
    private JobInfo jobInfo;
    private TaskInfo taskInfo;
    private Map<String,ParameterInfo> parameterInfoMap;
    
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();
    private File rootTasklibDir;
    private File libdir;
    
    @Before
    public void setUp() throws IOException {
        // use example config file from resources directory
        gpConfig=new GpConfig.Builder()
            .configFile(new File("resources/config_local_job_runner.yaml"))
        .build();
        gpContext=mock(GpContext.class);
        // need the next two lines to enable getting values for added properties
        when(gpContext.getCheckPropertiesFiles()).thenReturn(true);
        when(gpContext.getCheckSystemProperties()).thenReturn(true);
        jobInfo=mock(JobInfo.class);
        when(gpContext.getJobInfo()).thenReturn(jobInfo);
        taskInfo=mock(TaskInfo.class);
        when(gpContext.getTaskInfo()).thenReturn(taskInfo);
        parameterInfoMap=new HashMap<String,ParameterInfo>();
        
        rootTasklibDir=tmp.newFolder("taskLib");
        libdir=new File(rootTasklibDir, "ConvertLineEndings.1.1");
        boolean success=libdir.mkdirs();
        if (!success) {
            fail("failed to create tmp libdir: "+libdir);
        }
    }
    
    @Test
    public void substitute_name() {
        final String taskName="TestModule";
        when(taskInfo.getName()).thenReturn(taskName);
        assertEquals(
               // expected
               Arrays.asList("echo", "taskName="+taskName),
               // actual
               CommandLineParser.translateCmdLine(gpConfig, gpContext, "echo taskName=<name>"));
    }
    
    @Test
    public void substitute_job_id() {
        int jobId=101;
        when(gpContext.getJobNumber()).thenReturn(jobId);
        assertEquals(
               Arrays.asList("echo", "job_id="+jobId),
               CommandLineParser.translateCmdLine(gpConfig, gpContext, "echo job_id=<job_id>"));
    }
    
    @Test
    public void substitute_parent_job_id() {
        int parentJobId=100;
        when(jobInfo._getParentJobNumber()).thenReturn(parentJobId);
        assertEquals(
               Arrays.asList("echo", "parent_job_id="+parentJobId),
               CommandLineParser.translateCmdLine(gpConfig, gpContext, "echo parent_job_id=<parent_job_id>"));
    }
    
    @Test
    public void substitute_userid() {
        final String userId="testUser";
        when(jobInfo.getUserId()).thenReturn(userId);
        assertEquals(
               Arrays.asList("echo", "userid="+userId),
               CommandLineParser.translateCmdLine(gpConfig, gpContext, "echo userid=<userid>"));
    }
    
    @Test
    public void substitute_LSID() {
        final String LSID="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:1";
        when(gpContext.getLsid()).thenReturn(LSID);
        assertEquals(
               Arrays.asList("echo", "LSID="+LSID),
               CommandLineParser.translateCmdLine(gpConfig, gpContext, "echo LSID=<LSID>"));
    }
    
    @Test
    public void substitute_libdir() {
        final File libdir=new File("/Applications/GenePatternServer/taskLib/ConvertLineEndings.1.1/");
        when(gpContext.getTaskLibDir()).thenReturn(libdir);
        assertEquals(
               Arrays.asList("echo", "libdir="+libdir+File.separator),
               CommandLineParser.translateCmdLine(gpConfig, gpContext, "echo libdir=<libdir>"));
    }

    @Test
    public void substitute_libdir_as_file() {
        when(gpContext.getTaskLibDir()).thenReturn(libdir);
        assertEquals(
               Arrays.asList("echo", "libdir="+libdir+File.separator),
               CommandLineParser.translateCmdLine(gpConfig, gpContext, "echo libdir=<libdir>"));
    }
    
    @Test
    public void substitute_patches() {
        File patchesDir=new File("patches").getAbsoluteFile();
        gpConfig=new GpConfig.Builder()
            .addProperty(GpConfig.PROP_PLUGIN_DIR, patchesDir.getAbsolutePath())
        .build();
        assertEquals(
               Arrays.asList("echo", "patches="+patchesDir),
               CommandLineParser.translateCmdLine(gpConfig, gpContext, "echo patches=<patches>"));
    }
    
    @Test
    public void substitute_java_flags() {
        gpConfig=new GpConfig.Builder()
            .addProperty("java", "java")
            .addProperty("java_flags", "-Xmx512m -Dhttp.proxyHost=<http.proxyHost> -Dhttp.proxyPort=<http.proxyPort>")
             .addProperty("job.memory", "8 Gb")
        .build();
        assertEquals(
               Arrays.asList("java", "-Xmx512m", "-Dhttp.proxyHost=", "-Dhttp.proxyPort=", "Print", "Hello"),
               CommandLineParser.translateCmdLine(gpConfig, gpContext, "<java> <java_flags> Print Hello"));
    }

    //TODO: replace default -Xmx512m with custom job.memory value
    @Ignore @Test
    public void substitute_java_flags_customMemory() {
        gpConfig=new GpConfig.Builder()
            .addProperty("java", "java")
            .addProperty("java_flags", "-Xmx512m -Dhttp.proxyHost=<http.proxyHost> -Dhttp.proxyPort=<http.proxyPort>")
            .addProperty("job.memory", "8 Gb")
        .build();
        assertEquals(
               Arrays.asList("java", "-Xmx8g", "-Dhttp.proxyHost=", "-Dhttp.proxyPort=", "Print", "Hello"),
               CommandLineParser.translateCmdLine(gpConfig, gpContext, "<java> <java_flags> Print Hello"));
    }

    //TODO: develop proper test-cases for handling input file values
    @Test
    public void substituteInputParam_text() {
        JobInput jobInput=new JobInput();
        jobInput.addValue("arg1", "Value One");
        when(gpContext.getJobInput()).thenReturn(jobInput);
        assertEquals(
               Arrays.asList("echo", "Value One"),
               CommandLineParser.translateCmdLine(gpConfig, gpContext, "echo <arg1>", parameterInfoMap));
    }
    
    @Test
    public void substitute_GenePatternVersion() {
        gpConfig=new GpConfig.Builder()
            .webappDir(new File("website"))
        .build();
        assertEquals(
                Arrays.asList("echo", "genepattern.version=3.9.5", "GenePatternVersion=3.9.5"),
                CommandLineParser.translateCmdLine(gpConfig, gpContext, "echo genepattern.version=<genepattern.version> GenePatternVersion=<GenePatternVersion>", parameterInfoMap));
    }

}
