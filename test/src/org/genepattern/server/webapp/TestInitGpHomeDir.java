/*******************************************************************************
 * Copyright (c) 2003-2018 Regents of the University of California and Broad Institute. All rights reserved.
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

/**
 * jUnit tests for the StartupServlet#initGpHomeDir static helper method.
 * 
 * Some of these tests use the System Rules package 
 *   see: http://stefanbirkner.github.io/system-rules/
 * These rules may result in unexpected behaviors when run in parallel within the same JVM.
 * 
 * @author pcarr
 */
public class TestInitGpHomeDir {

    // this rule undoes changes of all system properties when the test finishes (whether it passes or fails).
    @Rule
    public final RestoreSystemProperties restore_sys_props = new RestoreSystemProperties();

    // this rule sets environment variables and reverts changes after the test
    @Rule
    public final EnvironmentVariables env = new EnvironmentVariables();

    private ServletConfig servletConfig;
    private File expectedGpHomeDir;
    
    @Before
    public void setUp() throws IOException {
        servletConfig=mock(StartupServlet.class);
        when(servletConfig.getInitParameterNames()).thenReturn(new Vector<String>().elements());
        expectedGpHomeDir=new File(FileUtil.getDataDir(),"gp_home");
    }

    @Test
    public void initGpHomeDir_fromServletConfig() {
        when(servletConfig.getInitParameter("GENEPATTERN_HOME")).thenReturn(expectedGpHomeDir.getAbsolutePath());
        assertEquals("initialized from 'GENEPATTERN_HOME' servlet parameter",
            expectedGpHomeDir,
            StartupServlet.initGpHomeDir(servletConfig));
    }
    
    @Test
    public void initGpHomeDir_fromServletConfig_altSpelling() {
        when(servletConfig.getInitParameter("gp.home")).thenReturn(expectedGpHomeDir.getAbsolutePath());
        assertEquals("initialized from 'gp.home' servlet parameter",
            expectedGpHomeDir,
            StartupServlet.initGpHomeDir(servletConfig));
    }
    
    @Test
    public void initGpHomeDir_notSet() {
        assertNull("by default GENEPATTERN_HOME is not set", 
            StartupServlet.initGpHomeDir(servletConfig));
    }

    @Test
    public void initGpHomeDir_fromSystemProp() {
        System.setProperty("GENEPATTERN_HOME", expectedGpHomeDir.getAbsolutePath());
        final File gpHomeDir=StartupServlet.initGpHomeDir(servletConfig);
        assertEquals(expectedGpHomeDir.getAbsolutePath(), gpHomeDir.getAbsolutePath());
    }

    @Test
    public void initGpHomeDir_fromSystemProp_emptyString() {
        System.setProperty("GENEPATTERN_HOME", "");
        final File gpHomeDir=StartupServlet.initGpHomeDir(servletConfig);
        assertNull("by default GENEPATTERN_HOME is not set", gpHomeDir);
    }
    
    @Test
    public void initGpHomeDir_fromSystemProp_relativePath() {
        System.setProperty("GENEPATTERN_HOME", "gp_home");
        final File gpHomeDir=StartupServlet.initGpHomeDir(servletConfig);
        final File expected=new File(System.getProperty("user.dir"), "gp_home");
        assertEquals(expected, gpHomeDir);
    }

    @Test
    public void initGpHomeDir_fromSystemProp_relativeParentPath() {        
        System.setProperty("GENEPATTERN_HOME", "../gp_home");
        final File gpHomeDir=StartupServlet.initGpHomeDir(servletConfig);
        final File expected=new File(new File(System.getProperty("user.dir")).getParentFile(), "gp_home");
        assertEquals(expected, gpHomeDir);
    }

    @Test
    public void initGpHomeDir_fromEnvironment() {
        env.set("GENEPATTERN_HOME", expectedGpHomeDir.getAbsolutePath());
        assertEquals("sanity check", 
            expectedGpHomeDir.getAbsolutePath(), 
            System.getenv("GENEPATTERN_HOME")
        );
        
        final File gpHomeDir=StartupServlet.initGpHomeDir(servletConfig);
        assertEquals("initGpHomeDir from GENEPATTERN_HOME", 
            expectedGpHomeDir.getAbsolutePath(), 
            gpHomeDir.getAbsolutePath()
        );
    }
    
}
