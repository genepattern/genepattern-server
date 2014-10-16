package org.genepattern.drm.impl.lsf.core;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.drm.Memory;
import org.genepattern.server.executor.lsf.LsfProperties;
import org.junit.Before;
import org.junit.Test;

/**
 * junit tests for the bsub command with configurable 'lsf.memoryScale'.
 * @author pcarr
 *
 */
public class TestGetMemFlags {
    private DrmJobSubmission jobSubmission;
    
    @Before
    public void setUp() {
        jobSubmission=mock(DrmJobSubmission.class);
    }
    
    @Test
    public void memFlags() {
        when(jobSubmission.getMemory()).thenReturn(Memory.fromString("8 Gb"));
        List<String> memFlags=CmdLineLsfRunner.getMemFlags(jobSubmission);
        assertEquals("default mem flags", Arrays.asList("-R", "rusage[mem=8]", "-M", "8"), memFlags);
    }
    
    @Test
    public void memFlags_customMemoryUnit() {
        when(jobSubmission.getMemory()).thenReturn(Memory.fromString("8 Gb"));
        List<String> memFlags=CmdLineLsfRunner.getMemFlags(jobSubmission, Memory.Unit.mb);
        assertEquals("default mem flags", Arrays.asList("-R", "rusage[mem=8192]", "-M", "8192"), memFlags);
    }

    @Test
    public void memFlags_nullMemoryUnit() {
        when(jobSubmission.getMemory()).thenReturn(Memory.fromString("8 Gb"));
        List<String> memFlags=CmdLineLsfRunner.getMemFlags(jobSubmission, null);
        assertEquals("default mem flags", Arrays.asList("-R", "rusage[mem=8]", "-M", "8"), memFlags);
    }

    @Test
    public void memFlags_jobSubmissionNotSet_customLsfMaxMemory() {
        when(jobSubmission.getProperty(LsfProperties.Key.MAX_MEMORY.getKey())).thenReturn("1");
        List<String> memFlags=CmdLineLsfRunner.getMemFlags(jobSubmission);
        assertEquals("default mem flags", Arrays.asList("-R", "rusage[mem=1]", "-M", "1"), memFlags);
    }

    @Test
    public void memFlags_jobSubmissionNotSet_customLsfMaxMemoryWithBogusValue() {
        // Hint: legacy code expects an Integer value, assuming Gb units
        when(jobSubmission.getProperty(LsfProperties.Key.MAX_MEMORY.getKey())).thenReturn("24 Gb");
        List<String> memFlags=CmdLineLsfRunner.getMemFlags(jobSubmission);
        assertEquals("default mem flags", Arrays.asList("-R", "rusage[mem=2]", "-M", "2"), memFlags);
    }

    @Test
    public void memFlags_jobSubmissionNotSet() {
        List<String> memFlags=CmdLineLsfRunner.getMemFlags(jobSubmission);
        assertEquals("default mem flags", Arrays.asList("-R", "rusage[mem=2]", "-M", "2"), memFlags);
    }
    
    @Test
    public void memFlags_jobSubmissionNotSet_customMemoryUnit() {
        List<String> memFlags=CmdLineLsfRunner.getMemFlags(jobSubmission, Memory.Unit.mb);
        assertEquals("custom lsf.memoryUnit=mb", Arrays.asList("-R", "rusage[mem=2048]", "-M", "2048"), memFlags);        
    }
    

}
