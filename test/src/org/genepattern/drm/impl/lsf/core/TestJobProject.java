package org.genepattern.drm.impl.lsf.core;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.drm.JobRunner;
import org.genepattern.server.executor.lsf.LsfProperties;
import org.junit.Before;
import org.junit.Test;

import edu.mit.broad.core.lsf.LsfJob;

/**
 * junit tests for the 'job.project' and legacy 'lsf.project' configuration parameters of the Lsf Runner.
 * @author pcarr
 *
 */
public class TestJobProject {
    private CmdLineLsfRunner lsfRunner = new CmdLineLsfRunner();
    private DrmJobSubmission jobSubmission=mock(DrmJobSubmission.class);
    private File workingDir=new File("jobResults/1");

    @Before
    public void setUp() {
        when(jobSubmission.getWorkingDir()).thenReturn(workingDir);
    } 
    
    @Test
    public void defaultProjectIsNull() {
        LsfJob lsfJob=lsfRunner.initLsfJob(jobSubmission);
        assertEquals("By default, lsfJob.project is null", null,  lsfJob.getProject());
    }
    
    @Test
    public void lsfProjectFromConfig() {
        when(jobSubmission.getProperty(LsfProperties.Key.PROJECT.getKey())).thenReturn("customLsfProject");
        LsfJob lsfJob=lsfRunner.initLsfJob(jobSubmission);
        assertEquals("When 'lsf.project' is set", "customLsfProject",  lsfJob.getProject());
    }
    
    @Test
    public void jobProjectFromConfig() {
        when(jobSubmission.getProperty(JobRunner.PROP_PROJECT)).thenReturn("customJobProject");
        LsfJob lsfJob=lsfRunner.initLsfJob(jobSubmission);
        assertEquals("When 'job.project' is set", "customJobProject",  lsfJob.getProject());
    }
    
    @Test
    public void projectPrecedenceFromConfig() {
        when(jobSubmission.getProperty(LsfProperties.Key.PROJECT.getKey())).thenReturn("customLsfProject");
        when(jobSubmission.getProperty(JobRunner.PROP_PROJECT)).thenReturn("customJobProject");
        LsfJob lsfJob=lsfRunner.initLsfJob(jobSubmission);
        assertEquals("Use 'job.project' when both 'job.project' and 'lsf.project' are set", "customJobProject",  lsfJob.getProject());
    }

    @Test(expected=IllegalArgumentException.class)
    public void failOnNullWorkingDir() {
        DrmJobSubmission jobSubmission=mock(DrmJobSubmission.class);
        when(jobSubmission.getWorkingDir()).thenReturn(null);
        lsfRunner.initLsfJob(jobSubmission);
        
    }

}
