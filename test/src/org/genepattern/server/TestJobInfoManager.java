/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * junit tests for the JobInfoManager class.
 * @author pcarr
 *
 */
public class TestJobInfoManager {
    // use non-default ('/gp') servlet context path
    final String gpContextPath="/gp-custom-context-path";
    // default GenePatternURL set in 'genepattern.properties'
    final String GenePatternURL="http://127.0.0.1:8080/gp/"; 

    private GpConfig gpConfig;
    private GpContext gpContext;
    
    @Rule
    public TemporaryFolder temp= new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        File rootJobDir=FileUtil.getDataFile("jobResults");
        gpConfig=new GpConfig.Builder()
            .webappDir(new File("website"))
            .gpServletContext(gpContextPath)
            .genePatternURL(new URL(GenePatternURL))
            .addProperty(GpConfig.PROP_JOBS, rootJobDir.getAbsolutePath())
        .build();
        gpContext=new GpContext.Builder().build();
    }
    
    @Test(expected=FileNotFoundException.class)
    public void getLaunchUrl_rootJobDirNotExists() throws Exception {
        File rootJobDir=temp.newFolder("_"+Math.random());
        rootJobDir.delete();
        gpConfig=new GpConfig.Builder()
            .webappDir(new File("website"))
            .genePatternURL(new URL(GenePatternURL))
            .addProperty(GpConfig.PROP_JOBS, rootJobDir.getAbsolutePath())
        .build();
        JobInfoManager.getLaunchUrl(gpConfig, gpContext, 695);
    }

    @Test(expected=FileNotFoundException.class)
    public void getLaunchUrl_jobDirNotExists() throws Exception {
        assertEquals(
                // expected
                "",
                // actual
                JobInfoManager.getLaunchUrl(gpConfig, gpContext, 1)
                );
    }
    
    @Test(expected=FileNotFoundException.class)
    public void getLaunchUrl_launchUrlFileNotExists() throws IOException {
        final int jobId=0;
        JobInfoManager.getLaunchUrl(gpConfig, gpContext, jobId);
    }

    @Test
    public void getLaunchUrl_from_file_in_job_dir() throws IOException {
        final String expected=
                gpContextPath+"/tasklib/urn:lsid:broad.mit.edu:cancer.software.genepattern.module.visualizer:00261:3.3/clsfilecreator.html?input.file=http%3A%2F%2Fwm97e-54f.broadinstitute.org%3A8080%2Fgp%2Fusers%2Fadmin%2Fall_aml_test.gct";
        
        assertEquals(
                expected,
                JobInfoManager.getLaunchUrl(gpConfig, gpContext, 695)
        );
    }

}
