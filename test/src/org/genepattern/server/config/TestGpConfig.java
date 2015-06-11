/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.config;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.genepattern.server.genepattern.CommandLineParser;
import org.genepattern.webservice.ParameterInfo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.io.Files;

public class TestGpConfig {
    private File webappDir;
    private GpConfig gpConfig;
    private GpContext gpContext;

    @Rule
    public TemporaryFolder temp= new TemporaryFolder();
    
    @Before
    public void setUp() {
        webappDir=new File("website").getAbsoluteFile();
        gpConfig=new GpConfig.Builder()
            .addProperty("java", "java")
            .webappDir(webappDir)
        .build();
        gpContext=GpContext.getServerContext();
    }
    
    @Test
    public void getDbSchemaPrefix_HSQL() {
        GpConfig gpConfig=new GpConfig.Builder()
            .addProperty(GpConfig.PROP_DATABASE_VENDOR, "HSQL")
        .build();
        
        assertEquals("analysis_hypersonic-", gpConfig.getDbSchemaPrefix());
    }
    
    @Test
    public void getDbSchemaPrefix_hsql() {
        GpConfig gpConfig=new GpConfig.Builder()
            .addProperty(GpConfig.PROP_DATABASE_VENDOR, "hsql")
        .build();
        
        assertEquals("analysis_hypersonic-", gpConfig.getDbSchemaPrefix());
    }

    @Test
    public void getDbSchemaPrefix_MySql() {
        GpConfig gpConfig=new GpConfig.Builder()
            .addProperty(GpConfig.PROP_DATABASE_VENDOR, "MySQL")
        .build();
        assertEquals("analysis_mysql-", gpConfig.getDbSchemaPrefix());
    }

    @Test
    public void getDbSchemaPrefix_Oracle() {
        GpConfig gpConfig=new GpConfig.Builder()
            .addProperty(GpConfig.PROP_DATABASE_VENDOR, "Oracle")
        .build();
        assertEquals("analysis_oracle-", gpConfig.getDbSchemaPrefix());
    }
    
    @Test
    public void getAnt() {
        final File expected_ant_home=new File(webappDir, "WEB-INF/tools/ant/apache-ant-1.8.4").getAbsoluteFile();
        String expected_ant_cmd="<ant-1.8_HOME>/bin/ant";


        assertEquals("antHomeDir", expected_ant_home, gpConfig.getAntHomeDir());
        assertEquals("<ant> command", expected_ant_cmd, gpConfig.getValue(gpContext, "ant").getValue());
        assertEquals("<ant-1.8> command", expected_ant_cmd, gpConfig.getValue(gpContext, "ant-1.8").getValue());
        assertEquals("<ant-1.8_HOME>", expected_ant_home, gpConfig.getGPFileProperty(gpContext, "ant-1.8_HOME"));
        
        assertEquals("parse <ant-1.8> command", 
                Arrays.asList( new File(expected_ant_home,"bin/ant").getAbsolutePath(), "-version"),
                CommandLineParser.createCmdLine(gpConfig, gpContext, "<ant-1.8> -version", new Properties(), new ParameterInfo[0]));
    }
    
    @Test
    public void setAntExecFlag() throws IOException {
        final File origAnt=new File(webappDir, "WEB-INF/tools/ant/apache-ant-1.8.4/bin/ant").getAbsoluteFile();
        
        
        File webappDirTemp=temp.newFolder("gp_webapp");
        File tmpAnt=new File(webappDirTemp, "WEB-INF/tools/ant/apache-ant-1.8.4/bin/ant").getAbsoluteFile();
        tmpAnt.getParentFile().mkdirs();
        
        Files.copy(origAnt, tmpAnt);
        
        tmpAnt.setExecutable(false);
        assertEquals("before init, exec flag should be false", false, tmpAnt.canExecute());
        
        GpConfig gpConfig=new GpConfig.Builder()
            .webappDir(webappDirTemp)
        .build();
        List<String> args=CommandLineParser.createCmdLine(gpConfig, gpContext, "<ant-1.8> -version", new Properties(), new ParameterInfo[0]);        
        assertEquals("after init, exec flag should be true", true, new File(args.get(0)).canExecute());
    }
    
    @Test
    public void getRun_R_Path() {
        // $USER_INSTALL_DIR$/Tomcat/webapps/gp/WEB-INF/classes/
        File expected=new File(webappDir,"WEB-INF/classes/");
        File actual=gpConfig.getGPFileProperty(gpContext, "run_r_path");
        assertEquals("getGPFileProperty('run_r_path')", expected, actual);
    }

}
