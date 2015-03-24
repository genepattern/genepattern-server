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
    
    
    @SuppressWarnings("deprecation")
    @Before
    public void setUp() {
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
    
}
