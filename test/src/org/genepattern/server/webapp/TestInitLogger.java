/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import javax.servlet.ServletConfig;

import org.genepattern.junitutil.FileUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.rules.TemporaryFolder;

/**
 * jUnit tests for the StartupServlet#initLogger helper methods
 * 
 * @author pcarr
 */
public class TestInitLogger {

    @Rule
    public TemporaryFolder temp=new TemporaryFolder();

    // this rule undoes changes of all system properties when the test finishes (whether it passes or fails).
    @Rule
    public final RestoreSystemProperties restore_sys_props = new RestoreSystemProperties();

    // this rule sets environment variables and reverts changes after the test
    @Rule
    public final EnvironmentVariables env = new EnvironmentVariables();

    private ServletConfig servletConfig;
    private File gpHomeDir;
    private File gpWorkingDir;
    private File gpResourcesDir;

    @Before
    public void setUp() throws IOException {
        servletConfig=mock(StartupServlet.class);
        when(servletConfig.getInitParameterNames()).thenReturn(new Vector<String>().elements());
        gpWorkingDir=new File(System.getProperty("user.dir"));
        gpHomeDir=new File(FileUtil.getDataDir(),"gp_home");
        gpResourcesDir=new File("resources");
    }
    
    @Test
    public void initLogDir() {
        assertEquals("default: gp.home/logs", 
            new File(gpHomeDir, "logs"), 
            StartupServlet.initLogDirPath(servletConfig, gpHomeDir, gpWorkingDir));
    }

    @Test
    public void initLogDir_gpHome_notSet() throws IOException {
        gpHomeDir=null;
        gpWorkingDir=new File(temp.newFolder(), "working_dir");
        assertEquals("default: ../logs", 
            new File(gpWorkingDir.getParentFile(), "logs").getAbsoluteFile(), 
            StartupServlet.initLogDirPath(servletConfig, (File)null, gpWorkingDir));
    }

    @Test
    public void initLogDir_gpHome_notSet_gpWorkingDir_notSet() throws IOException {
        gpHomeDir=null;
        gpWorkingDir=new File(temp.newFolder(), "working_dir");
        assertEquals("default: ./logs", 
            new File("logs").getAbsoluteFile(), 
            StartupServlet.initLogDirPath(servletConfig, (File)null, (File)null));
    }

    @Test
    public void initLogDir_custom_servletConfig() throws IOException {
        final File customLogDir=new File(temp.newFolder(), "logs");
        when(servletConfig.getInitParameter("gp.log")).thenReturn(customLogDir.getAbsolutePath());
        assertEquals("custom log dir",
            customLogDir,
            StartupServlet.initLogDirPath(servletConfig, gpHomeDir, gpWorkingDir));
    }

    @Test
    public void initLogDir_custom_servletConfig_GP_LOG() throws IOException {
        final File customLogDir=new File(temp.newFolder(), "logs");
        when(servletConfig.getInitParameter("GP_LOG")).thenReturn(customLogDir.getAbsolutePath());
        assertEquals("custom log dir",
            customLogDir,
            StartupServlet.initLogDirPath(servletConfig, gpHomeDir, gpWorkingDir));
    }
    @Test
    public void initLogDir_custom_sysProp() throws IOException {
        final File customLogDir=new File(temp.newFolder(), "logs");
        System.setProperty("gp.log", customLogDir.getAbsolutePath());
        assertEquals("custom log dir", 
            customLogDir, 
            StartupServlet.initLogDirPath(servletConfig, gpHomeDir, gpWorkingDir));
    }

    @Test
    public void initLogDir_custom_sysProp_GP_LOG() throws IOException {
        final File customLogDir=new File(temp.newFolder(), "logs");
        System.setProperty("GP_LOG", customLogDir.getAbsolutePath());
        assertEquals("custom log dir", 
            customLogDir, 
            StartupServlet.initLogDirPath(servletConfig, gpHomeDir, gpWorkingDir));
    }

    @Test
    public void initLogDir_custom_env() throws IOException {
        final File customLogDir=new File(temp.newFolder(), "logs");
        env.set("gp.log", customLogDir.getAbsolutePath());
        assertEquals("custom log dir",
            customLogDir, 
            StartupServlet.initLogDirPath(servletConfig, gpHomeDir, gpWorkingDir));
    }

    @Test
    public void initLogDir_custom_env_GP_LOG() throws IOException {
        final File customLogDir=new File(temp.newFolder(), "logs");
        env.set("GP_LOG", customLogDir.getAbsolutePath());
        assertEquals("custom log dir",
            customLogDir, 
            StartupServlet.initLogDirPath(servletConfig, gpHomeDir, gpWorkingDir));
    }
    
}
