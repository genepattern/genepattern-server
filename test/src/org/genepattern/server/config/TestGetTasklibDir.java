/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.config;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test cases for getting the path to the 'tasklib' directory.
 * @author pcarr
 *
 */
public class TestGetTasklibDir {
    private GpContext serverContext;
    private File userDir;
    private File userInstallDir;
    private File gpHomeDir;
    private File tasklibDir;

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();
    
    @Before
    public void setUp() throws IOException {
        serverContext=GpContext.getServerContext();
        userDir=new File(System.getProperty("user.dir"));
        userInstallDir=tmp.newFolder("GenePatternServer");
        gpHomeDir=tmp.newFolder(".gp_home");        
        tasklibDir=new File(gpHomeDir, "taskLib");
    }
    
    @Test
    public void getRootTasklibDir() {
        GpConfig gpConfig=new GpConfig.Builder()
            .gpHomeDir(gpHomeDir)
        .build();
        assertEquals(
                "Expecting GP_HOME/taskLib",
                tasklibDir,
                gpConfig.getRootTasklibDir(serverContext)); 
    } 
    
    @Test
    public void getRootTasklibDirCustomAbsolutePath() throws IOException {
        File customTasklibDir=tmp.newFolder("customTaskLib");
        GpConfig gpConfig=new GpConfig.Builder()
            .gpHomeDir(gpHomeDir)
            .addProperty(GpConfig.PROP_TASKLIB_DIR, customTasklibDir.getAbsolutePath())
        .build();
        assertEquals(
                "Expecting custom tasklib",
                customTasklibDir,
                gpConfig.getRootTasklibDir(serverContext));
    }
    
    @Test
    public void getRootTasklibDirCustomRelativePath() {
        File customTasklibDir=new File(gpHomeDir, "customTasklib");
        GpConfig gpConfig=new GpConfig.Builder()
            .gpHomeDir(gpHomeDir)
            .addProperty(GpConfig.PROP_TASKLIB_DIR, "customTasklib")
        .build();
        assertEquals(
                "Expecting custom tasklib",
                customTasklibDir,
                gpConfig.getRootTasklibDir(serverContext));
    }

    @Test
    public void getRootTasklibDir_legacy() {
        File expectedTasklibDir=new File(userDir.getParentFile(), "taskLib");
        GpConfig gpConfig=new GpConfig.Builder()
        .build();
        assertEquals(
                "when gpHome, gpWorking, and tasklib are not set, expect '../taskLib'",
                expectedTasklibDir, 
                gpConfig.getRootTasklibDir(serverContext));        
    }

}
