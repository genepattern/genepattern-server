package org.genepattern.drm.impl.lsf.core;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.genepattern.drm.DrmJobRecord;
import org.genepattern.server.config.Value;
import org.genepattern.server.executor.CommandProperties;
import org.junit.Before;
import org.junit.Test;

public class TestCmdLineRunner {
    private CmdLineLsfRunner cmd;
    private CommandProperties cmdProps;
    private DrmJobRecord jobRecord;
    private String extJobId="ext_job_23";
    
    @Before
    public void setUp() {
        jobRecord=new DrmJobRecord.Builder().extJobId(extJobId).build();
        cmd=new CmdLineLsfRunner();
        cmdProps=new CommandProperties();
    }
    
    @Test
    public void defaultLsfStatusCmd() {
        cmd.setCommandProperties(cmdProps);
        assertEquals("lsf.statusCmd", 
                Arrays.asList("bjobs", "-W", extJobId), 
                cmd.initLsfStatusChecker().initStatusCmd(jobRecord));
    }
    
    @Test
    public void defaultLsfStatusRegex() {
        assertEquals("lsf.statusRegex", 
                "(?<JOBID>\\d+)\\s+(?<USER>\\S+)\\s+(?<STATUS>\\S+)\\s+(?<QUEUE>\\S+)\\s+(?<FROMHOST>\\S+)\\s+(?<EXECHOST>\\S+)\\s+(?<JOBNAME>.*\\S)\\s+(?<SUBMITTIME>\\d\\d\\/\\d\\d-\\d\\d:\\d\\d:\\d\\d)\\s+(?<PROJNAME>\\S+)\\s+(?<CPUhours>\\d\\d\\d):(?<CPUmins>\\d\\d):(?<CPUsecs>\\d\\d\\.\\d\\d)\\s+(?<MEM>\\d+)\\s+(?<SWAP>\\d+)\\s+(?<PIDS>-|(?:(?:\\d+,)*\\d+))\\s+(?<STARTTIME>-|\\d\\d\\/\\d\\d-\\d\\d:\\d\\d:\\d\\d)\\s+(?<FINISHTIME>-|\\d\\d\\/\\d\\d-\\d\\d:\\d\\d:\\d\\d)(?<SLOT>\\s*(-|\\d*))\\s*", 
                cmd.initLsfStatusChecker().getLsfStatusPattern().pattern());
    }
    
    @Test
    public void customLsfStatusCmd() {
        cmdProps.put(CmdLineLsfRunner.PROP_STATUS_CMD, new Value(Arrays.asList("bjobs", "-a", "-W")));
        cmd.setCommandProperties(cmdProps);
        assertEquals("lsf.statusCmd", 
                Arrays.asList("bjobs", "-a", "-W", extJobId), 
                cmd.initLsfStatusChecker().initStatusCmd(jobRecord));
    }
    
    @Test
    public void customLsfStatusRegex() {
        // valid regex, just for testing
        String validRegex="\\s";
        cmdProps.put(CmdLineLsfRunner.PROP_STATUS_REGEX, validRegex);
        cmd.setCommandProperties(cmdProps);
        assertEquals("lsf.statusRegex", 
                validRegex, 
                cmd.initLsfStatusChecker().getLsfStatusPattern().pattern());
    }
    
    @Test
    public void bogusLsfStatusRegex() {
        cmdProps.put(CmdLineLsfRunner.PROP_STATUS_REGEX, "\\");
        cmd.setCommandProperties(cmdProps);
        assertEquals("lsf.statusRegex, when error, revert to default", 
                LsfBjobsParser.LINE_PATTERN_DEFAULT.pattern(), 
                cmd.initLsfStatusChecker().getLsfStatusPattern().pattern());
    }
}
