/*******************************************************************************
 * Copyright (c) 2003-2021 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.genepattern.junitutil.FileUtil;
import org.junit.Before;
import org.junit.Test;

/**
 * jUnit tests for the StartupServlet.
 * 
 * Some of these tests use the System Rules package 
 *   see: http://stefanbirkner.github.io/system-rules/
 * These rules may result in unexpected behaviors when run in parallel within the same JVM.
 * 
 * @author pcarr
 *
 */
public class TestStartupServlet {

    private File expectedGpHomeDir;
    private ServletContext servletContext;
    private ServletConfig servletConfig;
    
    @Before
    public void setUp() throws IOException {
        expectedGpHomeDir=new File(FileUtil.getDataDir(),"gp_home");
        servletContext=mock(ServletContext.class);
        servletConfig=mock(StartupServlet.class);
        when(servletConfig.getInitParameterNames()).thenReturn(new Vector<String>().elements());
    }
    
    @Test
    public void initGpWorkingDir() throws IOException {
        final File tomcatDir=new File("installer-2014-sp1/gpdist/Tomcat").getAbsoluteFile();
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getRealPath("../../")).thenReturn(tomcatDir.getAbsolutePath());        
        File gpWorkingDir=StartupServlet.initGpWorkingDir(servletConfig, (File)null);
        assertEquals(tomcatDir, gpWorkingDir);
    }
    
    @Test
    public void initGpWorkingDir_fromSystemProp() throws IOException {
        final File customGpWorkingDir=new File(FileUtil.getDataDir(),"startup_dir");
        File gpWorkingDir=StartupServlet.initGpWorkingDir(customGpWorkingDir.getAbsolutePath(), servletConfig, (File)null);
        assertEquals(customGpWorkingDir, gpWorkingDir);
    }
    
    @Test
    public void initGpWorkingDir_gpHomeIsSet() {
        //assume GENEPATTERN_HOME is set
        File gpWorkingDir=StartupServlet.initGpWorkingDir(servletConfig, expectedGpHomeDir);
        assertNull("Expecting null gpWorkingDir when gpHomeDir is set", gpWorkingDir);
    }
    
    @Test
    public void initResourcesDir_from_gpHomeDir() {
        //assume GENEPATTERN_HOME is set
        final File workingDir=new File(System.getProperty("user.dir"));
        final File resourcesDir=StartupServlet.initResourcesDir(expectedGpHomeDir, workingDir);
        File expectedResourcesDir=new File(expectedGpHomeDir, "resources");
        assertEquals(expectedResourcesDir, resourcesDir);
    }

    @Test
    public void initResourcesDir_gpHome_notSet() {
        //when GENEPATTERN_HOME is not set, fall back to ../resources
        final File workingDir=new File(System.getProperty("user.dir"));
        final File resourcesDir=StartupServlet.initResourcesDir((File)null, workingDir);
        final File expectedResourcesDir=new File(workingDir.getParentFile(), "resources").getAbsoluteFile();
        assertEquals(expectedResourcesDir, resourcesDir);
    }

    @Test
    public void loadProperties_gpHomeDir_isNull() throws ServletException {
        StartupServlet startupServlet=new StartupServlet();
        startupServlet.loadProperties(servletConfig);
    }

    @Test
    public void initWebappDir() throws ServletException {
        File webappDir=new File("website").getAbsoluteFile();
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getRealPath("")).thenReturn(webappDir.getAbsolutePath());
        assertEquals("webappDir", webappDir, StartupServlet.initWebappDir(servletConfig));
    }
    
}
