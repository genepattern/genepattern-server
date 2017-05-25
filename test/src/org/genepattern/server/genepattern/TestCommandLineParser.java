/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.genepattern;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.genepattern.junitutil.Demo;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestCommandLineParser {
    private Map<String,ParameterInfoRecord> parameterInfoMap;

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();
    private File rootTasklibDir;

    @Before
    public void setUp() throws IOException { 
        parameterInfoMap=new HashMap<String,ParameterInfoRecord>();
        rootTasklibDir=new File(FileUtil.getDataDir(), "taskLib");
    }
    
    @Test
    public void resolveValue_antCmd_legacy() {
        //TODO: improve substitution for <java>, should not have to set this with addProperty
        final String java_val="java";
        final String tomcatCommonLib=new File("installer-2014/gpdist/Tomcat/common/lib").getAbsoluteFile().toString();
        final GpConfig gpConfig=new GpConfig.Builder()
            .resourcesDir(FileUtil.getResourcesDir())
            .webappDir(FileUtil.getWebappDir())
            .addProperty("java", java_val)
            .addProperty("tomcatCommonLib", tomcatCommonLib)
            .addProperty("ant", "<java> -cp <tomcatCommonLib>/tools.jar -jar <tomcatCommonLib>/ant-launcher.jar -Dant.home=<tomcatCommonLib> -lib <tomcatCommonLib>")
        .build();

        final GpContext gpContext=null;
        assertThat(
            // actual
            CommandLineParser.resolveValue(gpConfig, gpContext, "<ant>", parameterInfoMap, 0),
            // is(expected)
            is(Arrays.asList( java_val, "-cp", tomcatCommonLib+"/tools.jar", "-jar", tomcatCommonLib+"/ant-launcher.jar", "-Dant.home="+tomcatCommonLib, "-lib", tomcatCommonLib )));
    }
    
    //TODO: implement support for _basename substitution in the resolveValue method
    @Test
    public void basenameSub() {
        final String userId="test_user";
        final String gpUrl="http://127.0.0.1:8080/gp/";
        // set up job context
        final JobInput jobInput=new JobInput();
        jobInput.addValue("input.filename", gpUrl+"users/"+userId+"/all_aml_test.cls");
        final GpContext gpContext=new GpContext.Builder()
            .jobInput(jobInput)
        .build();
        assertEquals(
                "all_aml_test", 
                CommandLineParser.getBasenameSubstitution((GpConfig) null, gpContext, "input.filename_basename", parameterInfoMap));
    }
    
    @Ignore @Test
    public void resolveValue_basename() {
        String userId="test_user";
        String gpUrl="http://127.0.0.1:8080/gp/";
        // set up job context
        JobInput jobInput=new JobInput();
        jobInput.addValue("input.filename", gpUrl+"users/"+userId+"/all_aml_test.cls");
        GpContext gpContext=new GpContext.Builder()
            .jobInput(jobInput)
        .build();
        assertEquals(
                Arrays.asList("all_aml_test"),
                CommandLineParser.resolveValue((GpConfig) null, gpContext, "<input.filename_basename>", parameterInfoMap, 0));
    }

    @Test
    public void checkFileSubstitution_nullArg() {
        assertArrayEquals("null arg", (String[])null, 
                CommandLineParser.checkFileSubstitution((String)null));
    }

    @Test
    public void checkFileSubstitution_emptyArg() {
        assertArrayEquals("empty arg", (String[])null, 
                CommandLineParser.checkFileSubstitution(""));
    }

    @Test
    public void checkFileSubstitution_noMatch() {
        final String arg="input.filename";
        assertArrayEquals("checkFileSubstitution('"+arg+"')", (String[])null, 
                CommandLineParser.checkFileSubstitution(arg));
    }

    @Test
    public void checkFileSubstitution_notAnExtension() {
        final String arg="input.filename_notAMatch";
        assertArrayEquals("checkFileSubstitution('"+arg+"')", (String[])null, 
                CommandLineParser.checkFileSubstitution(arg));
    }

    @Test
    public void checkFileSubstitution_basename() {
        assertArrayEquals("input.filename_basename",  new String[]{"input.filename", "_basename"}, 
                CommandLineParser.checkFileSubstitution("input.filename_basename"));
    }

    @Test
    public void checkFileSubstitution_extension() {
        assertArrayEquals("input.filename_extension", new String[]{"input.filename", "_extension"}, 
                CommandLineParser.checkFileSubstitution("input.filename_extension"));
    }

    @Test
    public void checkFileSubstitution_file() {
        assertArrayEquals("input.filename_file",      new String[]{"input.filename", "_file"}, 
                CommandLineParser.checkFileSubstitution("input.filename_file"));
    }

    @Test
    public void substituteValue_libdir() { 
        final File cleLibdir=new File(rootTasklibDir, "ConvertLineEndings.1.1");
        GpContext gpContext=mock(GpContext.class);
        when(gpContext.getTaskLibDir()).thenReturn(cleLibdir);
        assertEquals(Arrays.asList(cleLibdir+File.separator), 
                CommandLineParser.substituteValue(Demo.gpConfig(), gpContext, "<libdir>", parameterInfoMap));
    }
    
    @Test
    public void substituteValue_libdir_arg() { 
        final File cleLibdir=new File(rootTasklibDir, "ConvertLineEndings.1.1");
        GpContext gpContext=mock(GpContext.class);
        when(gpContext.getTaskLibDir()).thenReturn(cleLibdir);
        assertEquals(Arrays.asList(cleLibdir+File.separator+"test.txt"),
                CommandLineParser.substituteValue(Demo.gpConfig(), gpContext, "<libdir>test.txt", parameterInfoMap));
    }

    @Test
    public void substituteValue_notSet() {
        GpContext gpContext=null;
        String arg="<not_set>";
        List<String> expected=new ArrayList<String>();
        List<String> actualValue=CommandLineParser.substituteValue(Demo.gpConfig(), gpContext, arg, parameterInfoMap);
        assertEquals(expected, actualValue);
    }

    @Test
    public void subsituteValue_nullParameterInfoMap() {
        GpContext gpContext=null;
        parameterInfoMap=null;
        assertEquals(
                Arrays.asList("literal"),
                CommandLineParser.substituteValue(Demo.gpConfig(), gpContext, "literal", parameterInfoMap));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void nullGpConfig() {
        CommandLineParser.substituteValue( (GpConfig) null, (GpContext) null, "literal", parameterInfoMap );
    }
    
    @Test
    public void gpatCreateCmdLine_fromPropsAndConfig() {
        final String R2_15_HOME="/Library/Frameworks/R.framework/Versions/2.15/Resources";
        GpConfig gpConfig=new GpConfig.Builder()
            .addProperty("R2.15_HOME", R2_15_HOME)
        .build();

        // <R2.15_HOME>/bin/Rscript --no-save --quiet --slave --no-restore <libdir>run_rank_normalize.R <libdir> <user.dir> <patches> --input.file=<input.file> --output.file.name=<output.file.name> <scale.to.value> <threshold> <ceiling> <shift>
        final String cmdLine="<R2.15_HOME>/bin/Rscript --input.file=<input.file>";
        final Map<String,String> setupProps=new HashMap<String,String>();
        //Demo.localDataDir
        final File inputFile=FileUtil.getDataFile("all_aml_test.gct");
        //File inputFile=new File(uploadsDir, "all_aml_test.gct");
        setupProps.put("input.file", inputFile.getAbsolutePath());
        final Map<String,ParameterInfoRecord> paramInfoMap=Collections.emptyMap();
        final List<String> actual=ValueResolver.resolveValue(gpConfig, (GpContext) null, cmdLine, setupProps, paramInfoMap);
        assertEquals(
                Arrays.asList(R2_15_HOME+"/bin/Rscript", "--input.file="+inputFile.getAbsolutePath()),
                actual );
    }
    
    @Test
    public void gpatCreateCmdLine_MacApp_R2_5() throws IOException {
        // simulate genepattern.properties entries for 3.9.2 Mac app 
        final File svmLibdir=new File(rootTasklibDir, "SVM.4.100");
        final GpConfig gpConfig=new GpConfig.Builder()
            .webappDir(FileUtil.getWebappDir())
            .resourcesDir(FileUtil.getResourcesDir())
            .addProperty("java", "/usr/bin/java")
            .addProperty("R2.5", "<java> -DR_suppress=<R.suppress.messages.file> -DR_HOME=<R2.5_HOME> -Dr_flags=<r_flags> -cp <run_r_path> RunR")
            .addProperty("R2.5_HOME", "/Library/Frameworks/R.framework/Versions/2.5/Resources")
            .addProperty("r_flags", "--no-save --quiet --slave --no-restore")
        .build();
        
        // simulate the Properties object passed in from GPAT.java onJob (circa GP <= 3.9.2)
        Map<String,String> gpatRuntimeProps=new HashMap<String,String>();
        //TODO: improve substitution for <libdir>, should not depend on this being passed via the Properties arg
        gpatRuntimeProps.put("libdir", svmLibdir+"/");
        gpatRuntimeProps.put("train.data.filename", "/path/to/all_aml_train.gct");
        
        //String cmdLine="<R2.5> <libdir>svm.R mysvm -rf<train.data.filename> -rc<train.cls.filename> -ef<test.data.filename> -ec<test.cls.filename> -pr<pred.results.output.file> -mf<model.output.file> -li<libdir> -sm<saved.model.filename>";
        String cmdLine="<R2.5> <libdir>svm.R mysvm -rf<train.data.filename>";
        
        final Map<String,ParameterInfoRecord> paramInfoMap=Collections.emptyMap();
        final List<String> expected=Arrays.asList(
                "/usr/bin/java", 
                "-DR_suppress="+FileUtil.getResourcesDir().getAbsolutePath()+"/R_suppress.txt",
                "-DR_HOME=/Library/Frameworks/R.framework/Versions/2.5/Resources", 
                "-Dr_flags=--no-save --quiet --slave --no-restore",
                "-cp",
                new File(FileUtil.getWebappDir(), "WEB-INF/classes").getAbsolutePath(),
                "RunR",
                svmLibdir+"/svm.R", "mysvm",
                "-rf/path/to/all_aml_train.gct"
                );
        final List<String> actual=ValueResolver.resolveValue(gpConfig, (GpContext) null, cmdLine, gpatRuntimeProps, paramInfoMap);
        assertThat(actual, CoreMatchers.is(expected));
    }
    
}
