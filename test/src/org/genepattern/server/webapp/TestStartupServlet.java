/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * jUnit tests for the StartupServlet.
 * @author pcarr
 *
 */
public class TestStartupServlet {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private StartupServlet startupServlet;
    private ServletContext servletContext;
    private ServletConfig servletConfig;

    private File workingDir;
    private File expectedGpHomeDir;
    
    @Before
    public void setUp() throws IOException {
        workingDir=new File(System.getProperty("user.dir"));
        expectedGpHomeDir=tmp.newFolder("gp_home");
        servletContext=mock(ServletContext.class);
        servletConfig=mock(StartupServlet.class);
        when(servletConfig.getInitParameterNames()).thenReturn(new Vector<String>().elements());

        startupServlet=new StartupServlet();
    }
    
    @Test
    public void initGpWorkingDir() throws IOException {
        File gpInstallDir=tmp.newFolder("GenePatternServer");
        File tomcatDir=new File(gpInstallDir, "Tomcat"); // mock location for <GenePatternServer>/Tomcat directory
        tomcatDir.mkdirs();
        File webappsDir=new File(tomcatDir, "webapps");
        webappsDir.mkdirs();
        File webappDir=new File(webappsDir, "gp");
        webappDir.mkdirs();
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getRealPath("../../")).thenReturn(tomcatDir.getAbsolutePath());
        
        File gpWorkingDir=startupServlet.initGpWorkingDir(servletConfig);
        assertEquals(tomcatDir, gpWorkingDir);
    }
    
    @Test
    public void initGpWorkingDir_fromSystemProp() throws IOException {
        File customGpWorkingDir=tmp.newFolder("customWorkingDir");
        File gpWorkingDir=startupServlet.initGpWorkingDir(customGpWorkingDir.getAbsolutePath(), servletConfig);
        assertEquals(customGpWorkingDir, gpWorkingDir);
    }
    
    @Test
    public void initGpWorkingDir_gpHomeIsSet() {
        //assume GENEPATTERN_HOME is set
        startupServlet.setGpHomeDir(expectedGpHomeDir);
        File gpWorkingDir=startupServlet.initGpWorkingDir(servletConfig);
        assertNull("Expecting null gpWorkingDir when gpHomeDir is set", gpWorkingDir);
    }
    
    
    @Test
    public void initGpHomeDir_fromServletConfig() {
        when(servletConfig.getInitParameter("GENEPATTERN_HOME")).thenReturn(expectedGpHomeDir.getAbsolutePath());
        File gpHomeDir=startupServlet.initGpHomeDir(servletConfig);
        startupServlet.setGpHomeDir(gpHomeDir);
        assertEquals(expectedGpHomeDir, gpHomeDir);
        
        assertEquals(expectedGpHomeDir, startupServlet.getGpHomeDir());
    }
    
    @Test
    public void initGpHomeDir_fromServletConfig_altSpelling() {
        when(servletConfig.getInitParameter("gp.home")).thenReturn(expectedGpHomeDir.getAbsolutePath());
        File gpHomeDir=startupServlet.initGpHomeDir(servletConfig);
        assertEquals(expectedGpHomeDir, gpHomeDir);
    }
    
    @Test
    public void initGpHomeDir_notSet() {
        File gpHomeDir=startupServlet.initGpHomeDir(servletConfig);
        assertNull("by default GENEPATTERN_HOME is not set", gpHomeDir);
    }

    @Test
    public void initGpHomeDir_fromSystemProp() {
        File gpHomeDir=startupServlet.initGpHomeDir(expectedGpHomeDir.getAbsolutePath(), servletConfig);
        assertEquals(expectedGpHomeDir.getAbsolutePath(), gpHomeDir.getAbsolutePath());
    }

    @Test
    public void initGpHomeDir_fromSystemProp_emptyString() {
        String gpHomeProp="";
        File gpHomeDir=startupServlet.initGpHomeDir(gpHomeProp, servletConfig);
        assertNull("by default GENEPATTERN_HOME is not set", gpHomeDir);
    }
    
    @Test
    public void initGpHomeDir_fromSystemProp_relativePath() {
        String gpHomeProp="gp_home";
        File gpHomeDir=startupServlet.initGpHomeDir(gpHomeProp, servletConfig);
        File expected=new File(System.getProperty("user.dir"), gpHomeProp);
        assertEquals(expected, gpHomeDir);
    }
    
    @Test
    public void initResourcesDir_from_gpHomeDir() {
        //assume GENEPATTERN_HOME is set
        startupServlet.setGpHomeDir(expectedGpHomeDir);
        File resourcesDir=startupServlet.initResourcesDir(workingDir);
        startupServlet.setGpResourcesDir(resourcesDir);
        File expectedResourcesDir=new File(expectedGpHomeDir, "resources");
        assertEquals(expectedResourcesDir, resourcesDir);
        assertEquals(expectedResourcesDir, startupServlet.getGpResourcesDir());
    }
    
    @Test
    public void loadProperties_gpHomeDir_isNull() throws ServletException {
        startupServlet.loadProperties(servletConfig);
    }

    @Test
    public void initWebappDir() throws ServletException {
        File webappDir=new File("website").getAbsoluteFile();
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getRealPath("")).thenReturn(webappDir.getAbsolutePath());
        assertEquals("webappDir", webappDir, startupServlet.initWebappDir(servletConfig));
    }
}
