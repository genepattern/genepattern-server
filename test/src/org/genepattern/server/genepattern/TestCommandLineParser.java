/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.genepattern;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.webservice.ParameterInfo;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestCommandLineParser {
    private GpConfig gpConfig;
    private GpContext gpContext;
    private Map<String,ParameterInfo> parameterInfoMap;
    
    private String java_val="java";
    private String tomcatCommonLib_val=".";
    
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();
    private File rootTasklibDir;
    private File libdir;
    private File webappDir;
    private File uploadsDir;
    private File resourcesDir;
    private File gpAppDir;
    
    
    @SuppressWarnings("deprecation")
    @Before
    public void setUp() {
        gpAppDir=tmp.newFolder("GenePattern.app").getAbsoluteFile();
        resourcesDir=tmp.newFolder("resources").getAbsoluteFile();
        uploadsDir=tmp.newFolder("users/test_user/uploads");
        webappDir=tmp.newFolder("Tomcat/webapps/gp").getAbsoluteFile();
        File tomcatCommonLib=new File(webappDir.getParentFile().getParentFile(), "common/lib").getAbsoluteFile();
        tomcatCommonLib_val=tomcatCommonLib.toString();
        
        gpConfig=new GpConfig.Builder()
            .webappDir(webappDir)
            .addProperty("java", java_val)
            .addProperty("tomcatCommonLib", tomcatCommonLib_val)
            .addProperty("ant", "<java> -cp <tomcatCommonLib>/tools.jar -jar <tomcatCommonLib>/ant-launcher.jar -Dant.home=<tomcatCommonLib> -lib <tomcatCommonLib>")
        .build();
        gpContext=new GpContext();
        parameterInfoMap=new HashMap<String,ParameterInfo>();

        rootTasklibDir=tmp.newFolder("taskLib");
        libdir=new File(rootTasklibDir, "ConvertLineEndings.1.1");
        boolean success=libdir.mkdirs();
        if (!success) {
            fail("failed to create tmp libdir: "+libdir);
        }
    }
    
    @Test
    public void resolveValue_antCmd() {
        assertEquals(
                Arrays.asList( java_val, "-cp", tomcatCommonLib_val+"/tools.jar", "-jar", tomcatCommonLib_val+"/ant-launcher.jar", "-Dant.home="+tomcatCommonLib_val, "-lib", tomcatCommonLib_val ),
                CommandLineParser.resolveValue(gpConfig, gpContext, "<ant>", parameterInfoMap, 0));
    }
    
    //TODO: implement support for _basename substitution in the resolveValue method
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
                CommandLineParser.resolveValue(gpConfig, gpContext, "<input.filename_basename>", parameterInfoMap, 0));
    }

    @Test
    public void substituteValue_libdir() { 
        gpContext.setTaskLibDir(libdir);
        assertEquals(Arrays.asList(libdir+File.separator), 
                CommandLineParser.substituteValue(gpConfig, gpContext, "<libdir>", parameterInfoMap));
    }
    
    @Test
    public void substituteValue_libdir_arg() { 
        gpContext.setTaskLibDir(libdir);
        assertEquals(Arrays.asList(libdir+File.separator+"test.txt"),
                CommandLineParser.substituteValue(gpConfig, gpContext, "<libdir>test.txt", parameterInfoMap));
    }

    @Test
    public void substituteValue_notSet() {
        String arg="<not_set>";
        List<String> expected=new ArrayList<String>();
        List<String> actualValue=CommandLineParser.substituteValue(gpConfig, gpContext, arg, parameterInfoMap);
        assertEquals(expected, actualValue);
    }

    @Test
    public void subsituteValue_nullParameterInfoMap() {
        parameterInfoMap=null;
        assertEquals(
                Arrays.asList("literal"),
                CommandLineParser.substituteValue(gpConfig, gpContext, "literal", parameterInfoMap));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void nullGpConfig() {
        CommandLineParser.substituteValue( (GpConfig) null, gpContext, "literal", parameterInfoMap );
    }
    
    @Test
    public void gpatCreateCmdLine_fromPropsAndConfig() {
        final String R2_15_HOME="/Library/Frameworks/R.framework/Versions/2.15/Resources";
        GpConfig gpConfig=new GpConfig.Builder()
            .addProperty("R2.15_HOME", R2_15_HOME)
        .build();

        // <R2.15_HOME>/bin/Rscript --no-save --quiet --slave --no-restore <libdir>run_rank_normalize.R <libdir> <user.dir> <patches> --input.file=<input.file> --output.file.name=<output.file.name> <scale.to.value> <threshold> <ceiling> <shift>
        final String cmdLine="<R2.15_HOME>/bin/Rscript --input.file=<input.file>";
        final Properties setupProps=new Properties();
        File inputFile=new File(uploadsDir, "all_aml_test.gct");
        setupProps.setProperty("input.file", inputFile.getAbsolutePath());
        final ParameterInfo[] formalParameters=new ParameterInfo[0];
        final List<String> actual=CommandLineParser.createCmdLine(gpConfig, gpContext, cmdLine, setupProps, formalParameters);
        assertEquals(
                Arrays.asList(R2_15_HOME+"/bin/Rscript", "--input.file="+inputFile.getAbsolutePath()),
                actual );
    }
    
    @Test
    public void gpatCreateCmdLine_MacApp_R2_5() {
        // simulate genepattern.properties entries for 3.9.2 Mac app 
        final File svmLibdir=new File(rootTasklibDir, "SVM.4.100");

        final File tomcatDir=new File(gpAppDir, "Contents/Resources/GenePatternServer/Tomcat");
        GpConfig gpConfig=new GpConfig.Builder()
            .addProperty("GENEPATTERN_APP_DIR", gpAppDir.toString())
            .addProperty("java", "/usr/bin/java")
            .addProperty("R2.5", "<java> -DR_suppress=<R.suppress.messages.file> -DR_HOME=<R2.5_HOME> -Dr_flags=<r_flags> -cp <run_r_path> RunR")
            .addProperty("R2.5_HOME", "/Library/Frameworks/R.framework/Versions/2.5/Resources")
            .addProperty("R.suppress.messages.file", "<resources>/R_suppress.txt")
            .addProperty("run_r_path", tomcatDir+"/webapps/gp/WEB-INF/classes/")
            .addProperty("r_flags", "--no-save --quiet --slave --no-restore")
        .build();
        
        // simulate the Properties object passed in from GPAT.java onJob (circa GP <= 3.9.2)
        Properties gpatRuntimeProps=new Properties();
        //TODO: improve substitution for <resources>, should not depend on this being passed via the Properties arg
        gpatRuntimeProps.setProperty("resources", resourcesDir.toString());
        //TODO: improve substitution for <libdir>, should not depend on this being passed via the Properties arg
        gpatRuntimeProps.setProperty("libdir", svmLibdir+"/");
        gpatRuntimeProps.setProperty("train.data.filename", "/path/to/all_aml_train.gct");
        
        //String cmdLine="<R2.5> <libdir>svm.R mysvm -rf<train.data.filename> -rc<train.cls.filename> -ef<test.data.filename> -ec<test.cls.filename> -pr<pred.results.output.file> -mf<model.output.file> -li<libdir> -sm<saved.model.filename>";
        String cmdLine="<R2.5> <libdir>svm.R mysvm -rf<train.data.filename>";
        
        final ParameterInfo[] formalParameters=new ParameterInfo[0];
        final List<String> expected=Arrays.asList(
                "/usr/bin/java", 
                "-DR_suppress="+resourcesDir.getAbsolutePath()+"/R_suppress.txt",
                "-DR_HOME=/Library/Frameworks/R.framework/Versions/2.5/Resources", 
                "-Dr_flags=--no-save --quiet --slave --no-restore",
                "-cp",
                gpAppDir+"/Contents/Resources/GenePatternServer/Tomcat/webapps/gp/WEB-INF/classes/",
                "RunR",
                svmLibdir+"/svm.R", "mysvm",
                "-rf/path/to/all_aml_train.gct"
                );
        final List<String> actual=CommandLineParser.createCmdLine(gpConfig, gpContext, cmdLine, gpatRuntimeProps, formalParameters);
        assertThat(actual, Matchers.is(expected));
        
    }
    
}
