package org.genepattern.server.config;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;

import org.genepattern.server.genepattern.CommandLineParser;
import org.genepattern.webservice.ParameterInfo;
import org.junit.Test;

public class TestGpConfig {
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
        final File webappDir=new File("website").getAbsoluteFile();
        final File expected_ant_home=new File(webappDir, "WEB-INF/tools/ant/apache-ant-1.8.4").getAbsoluteFile();
        String expected_ant_cmd="<java> -jar <ant-1.8_HOME><file.separator>lib<file.separator>ant-launcher.jar -Dant.home=<ant-1.8_HOME>";

        GpConfig gpConfig=new GpConfig.Builder()
            .addProperty("java", "java")
            .webappDir(webappDir)
        .build();
        GpContext gpContext=GpContext.getServerContext();

        assertEquals("antHomeDir", expected_ant_home, gpConfig.getAntHomeDir());
        assertEquals("<ant> command", expected_ant_cmd, gpConfig.getValue(gpContext, "ant").getValue());
        assertEquals("<ant-1.8> command", expected_ant_cmd, gpConfig.getValue(gpContext, "ant-1.8").getValue());
        assertEquals("<ant-1.8_HOME>", expected_ant_home, gpConfig.getGPFileProperty(gpContext, "ant-1.8_HOME"));
        
        assertEquals("parse <ant-1.8> command", 
                Arrays.asList("java", "-jar",
                        new File(expected_ant_home,"lib/ant-launcher.jar").getAbsolutePath(),
                        "-Dant.home="+expected_ant_home, "-version"),
                CommandLineParser.createCmdLine(gpConfig, gpContext, "<ant-1.8> -version", new Properties(), new ParameterInfo[0]));
    }

}
