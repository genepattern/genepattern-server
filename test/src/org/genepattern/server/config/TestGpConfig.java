/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.config;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.genepattern.ValueResolver;
import org.genepattern.server.webapp.jsf.AboutBean;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.io.Files;

public class TestGpConfig {
    private static final GpContext gpContext=GpContext.getServerContext();

    @Rule
    public TemporaryFolder temp= new TemporaryFolder();
    
    @Test
    public void isPasswordRequired() {
        GpConfig gpConfig=new GpConfig.Builder().build();
        // by default, no
        assertEquals("isPasswordRequired, default config",
            false,
            gpConfig.isPasswordRequired(gpContext));
        
        assertEquals("null arg", false, GpConfig.isPasswordRequired((String)null));
        assertEquals("isPasswordRequired('FALSE')", false, GpConfig.isPasswordRequired("FALSE"));
        assertEquals("isPasswordRequired('false')", false, GpConfig.isPasswordRequired("false"));
        assertEquals("isPasswordRequired('n')", false, GpConfig.isPasswordRequired("n"));
        assertEquals("isPasswordRequired('no')", false, GpConfig.isPasswordRequired("no"));
        assertEquals("isPasswordRequired('NO')", false, GpConfig.isPasswordRequired("NO"));
        assertEquals("isPasswordRequired('TRUE')", true, GpConfig.isPasswordRequired("TRUE"));
    }

    @Test
    public void getDbSchemaPrefix_HSQL() {
        assertEquals("analysis_hypersonic-", GpConfig.getDbSchemaPrefix("HSQL"));
    }
    
    @Test
    public void getDbSchemaPrefix_hsql() {
        assertEquals("analysis_hypersonic-", GpConfig.getDbSchemaPrefix("hsql"));
    }

    @Test
    public void getDbSchemaPrefix_MySql() {
        assertEquals("analysis_mysql-", GpConfig.getDbSchemaPrefix("MySQL"));
    }

    @Test
    public void getDbSchemaPrefix_Oracle() {
        assertEquals("analysis_oracle-", GpConfig.getDbSchemaPrefix("Oracle"));
    }
    
    @Test
    public void getAnt() {
        final File webappDir=FileUtil.getWebappDir();
        final GpConfig gpConfig=new GpConfig.Builder()
            .resourcesDir(new File("resources"))
            .addProperty("java", "java")
            .webappDir(webappDir)
        .build();
        final File expectedAntHome=new File(webappDir, "WEB-INF/tools/ant/apache-ant-1.8.4").getAbsoluteFile();
        final String expectedAntScriptCmd="<ant-1.8_HOME>/bin/ant --noconfig";
        final String expectedAntJavaCmd="<java> -Dant.home=<ant-1.8_HOME> -cp <ant-1.8_HOME>/lib/ant-launcher.jar org.apache.tools.ant.launch.Launcher";
        final String expectedAntCmd=expectedAntJavaCmd;
        
        assertEquals("<ant> command", expectedAntCmd, gpConfig.getValue(gpContext, "ant").getValue());
        assertEquals("<ant-1.8> command", expectedAntCmd, gpConfig.getValue(gpContext, "ant-1.8").getValue());
        assertEquals("<ant-1.8_HOME>", expectedAntHome, gpConfig.getGPFileProperty(gpContext, "ant-1.8_HOME"));
        assertEquals("<ant-java>", expectedAntJavaCmd, gpConfig.getValue(gpContext, "ant-java").getValue());
        assertEquals("<ant-script>", expectedAntScriptCmd, gpConfig.getValue(gpContext, "ant-script").getValue());
        
        final List<String> expectedAntScriptArgs=Arrays.asList( new File(expectedAntHome,"bin/ant").getAbsolutePath(), "--noconfig", "-version");
        assertEquals("parse <ant-script> command", 
                expectedAntScriptArgs,
                ValueResolver.resolveValue(gpConfig, gpContext, "<ant-script> -version"));

        final List<String> expectedAntJavaArgs=Arrays.asList("java", "-Dant.home="+expectedAntHome, "-cp", expectedAntHome+"/lib/ant-launcher.jar", "org.apache.tools.ant.launch.Launcher", "-version");
        assertEquals("parse <ant-java> command", 
                expectedAntJavaArgs,
                ValueResolver.resolveValue(gpConfig, gpContext, "<ant-java> -version"));

        assertEquals("parse <ant-1.8> command", 
                expectedAntJavaArgs,
                ValueResolver.resolveValue(gpConfig, gpContext, "<ant-1.8> -version"));

        assertEquals("parse <ant> command", 
                expectedAntJavaArgs,
                ValueResolver.resolveValue(gpConfig, gpContext, "<ant> -version"));
    }
    
    @Test
    public void getAnt_spaces_in_dir() throws IOException {
        File webappDirTemp=temp.newFolder("gp webapp"); // Note: space character in directory name
        File tmpAnt=new File(webappDirTemp, "WEB-INF/tools/ant/apache-ant-1.8.4/bin/ant").getAbsoluteFile();
        
        GpConfig gpConfig=new GpConfig.Builder()
            .webappDir(webappDirTemp)
        .build();
        
        assertEquals("parse <ant-script> command", 
                Arrays.asList( tmpAnt.getAbsolutePath(), "--noconfig", "-version"),
                ValueResolver.resolveValue(gpConfig, gpContext, "<ant-script> -version"));
    }

    @Test
    public void setAntScriptExecFlag() throws IOException {
        final File origAnt=new File(FileUtil.getWebappDir(), "WEB-INF/tools/ant/apache-ant-1.8.4/bin/ant").getAbsoluteFile();
        
        final File tmp_webappDir=new File(temp.newFolder(), "Tomcat/webapps/gp").getAbsoluteFile();
        final File tmpAnt=new File(tmp_webappDir, "WEB-INF/tools/ant/apache-ant-1.8.4/bin/ant").getAbsoluteFile();
        tmpAnt.getParentFile().mkdirs();
        Files.copy(origAnt, tmpAnt); 
        tmpAnt.setExecutable(false);
        assertEquals("before init, exec flag should be false", false, tmpAnt.canExecute());
        
        GpConfig gpConfig=new GpConfig.Builder()
            .webappDir(tmp_webappDir)
        .build();
        List<String> args=ValueResolver.resolveValue(gpConfig, gpContext, "<ant-script> -version");        
        assertEquals("after init, exec flag should be true", true, new File(args.get(0)).canExecute());
    }
    
    @Test
    public void getRun_R_Path() {
        // $USER_INSTALL_DIR$/Tomcat/webapps/gp/WEB-INF/classes/
        final GpConfig gpConfig=new GpConfig.Builder()
            .webappDir(FileUtil.getWebappDir())
        .build();
        
        assertEquals("getGPFileProperty('run_r_path')", 
            //expected
            new File(FileUtil.getWebappDir(), "WEB-INF/classes"),
            //actual
            gpConfig.getGPFileProperty(gpContext, "run_r_path")
        );
    }
    
    @Test
    public void getRSuppressMessages() {
        // <resources>/R_suppress.txt
        final GpConfig gpConfig=new GpConfig.Builder()
            .webappDir(FileUtil.getWebappDir())
            .resourcesDir(FileUtil.getResourcesDir())
        .build();
        
        assertEquals("getGPFileProperty('R.suppress.messages.file')", 
            //expected
            new File(FileUtil.getResourcesDir(), "R_suppress.txt"),
            //actual
            gpConfig.getGPFileProperty(gpContext, "R.suppress.messages.file")
        );
    }
    
    /**
     * test initialization of GenePatternVersion from ./WEB-INF/build.properties file.
     * @throws FileNotFoundException
     * @throws IOException
     */
    @Test
    public void buildProperties() throws FileNotFoundException, IOException {
        final String mockGpVersion="3.9.5";
        // setup ...
        // 1) create test webappDir with a build.properties file
        final File webappDirTmp=new File(temp.newFolder(), "Tomcat/webapps/gp");
        final File mockBuildPropFile=new File(webappDirTmp, "WEB-INF/build.properties");
        Properties mockBuildProps=new Properties();
        mockBuildProps.put("genepattern.version", mockGpVersion);
        mockBuildProps.put("version.revision.id", "90");
        mockBuildProps.put("version.label", "JUNIT-TEST");
        mockBuildProps.put("version.build.date", "2015-06-19 17:00"); 
        mockBuildPropFile.getParentFile().mkdirs();
        GpServerProperties.writeProperties(mockBuildProps, mockBuildPropFile, 
                "Creating mock build.properties file for junit test");
        // 2) create GpConfig for test
        final GpConfig gpConfig=new GpConfig.Builder()
            .webappDir(webappDirTmp)
        .build();
        
        // tests ...
        assertEquals("gpConfig.buildPropertiesFile", 
            mockBuildPropFile,  
            gpConfig.getBuildPropertiesFile(webappDirTmp));
        assertEquals("buildProperties[genepattern.version]", 
            mockGpVersion, 
            gpConfig.getBuildProperties().get("genepattern.version"));

        //final String actualGpVersion=gpConfig.initGenePatternVersion(gpContext);
        assertEquals("initGenePatternVersion", 
            // expected
            mockGpVersion,
            // actual
            gpConfig.initGenePatternVersion(gpContext));
        assertEquals("gpConfig.genePatternVersion", 
            // expected 
            mockGpVersion, 
            // actual
            gpConfig.getGenePatternVersion());

        // use same setup to ...
        // ... test initialization of the AboutBean from the GpConfig.
        AboutBean about=new AboutBean(gpConfig, gpContext);
        //final String expectedGpVersion="3.9.5";
        assertEquals("about.genePatternVersion", mockGpVersion, about.getGenePatternVersion());
        assertEquals("about.versionLabel", "JUNIT-TEST", about.getVersionLabel());
        assertEquals("about.buildTag", "90", about.getBuildTag());
        assertEquals("about.full", "3.9.5 JUNIT-TEST", about.getFull());
        assertEquals("about.date", "2015-06-19 17:00", about.getDate());
    }

    @Test(expected=IllegalArgumentException.class)
    public void aboutBean_null_gpConfig() {
        new AboutBean((GpConfig)null, gpContext);
    }

    @Test
    public void toolsDir() {
        GpConfig gpConfig=new GpConfig.Builder()
            .webappDir(FileUtil.getWebappDir())
        .build();
        assertEquals("<gp.tools.dir>",
            // expected
            new File(FileUtil.getWebappDir(), "WEB-INF/tools").getAbsolutePath(),
            // actual
            gpConfig.getGPProperty(gpContext, "gp.tools.dir")
        );
    }

}
