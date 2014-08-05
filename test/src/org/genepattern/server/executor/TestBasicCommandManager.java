package org.genepattern.server.executor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import org.genepattern.drm.impl.local.commons_exec.LocalCommonsExecJobRunner;
import org.genepattern.drm.impl.lsf.core.CmdLineLsfRunner;
import org.genepattern.server.config.ConfigurationException;
import org.genepattern.server.executor.drm.JobExecutor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestBasicCommandManager {
    BasicCommandManager cmdMgr;
    JobExecutor localExec;
    JobExecutor lsfExec;
    JobExecutor sgeExec;
    
    CommandExecutor runtimeExec;
    
    @Before
    public void setUp() throws ConfigurationException {
        cmdMgr=new BasicCommandManager();
        lsfExec=mock(JobExecutor.class);
        when(lsfExec.getJobRunnerClassname()).thenReturn(CmdLineLsfRunner.class.getName());
        when(lsfExec.getJobRunnerName()).thenReturn(CmdLineLsfRunner.class.getSimpleName());
        
        localExec=mock(JobExecutor.class);
        when(localExec.getJobRunnerClassname()).thenReturn(LocalCommonsExecJobRunner.class.getName());
        when(localExec.getJobRunnerName()).thenReturn(LocalCommonsExecJobRunner.class.getSimpleName());
        
        runtimeExec=mock(RuntimeCommandExecutor.class);
        
        cmdMgr.addCommandExecutor("RuntimeExec", runtimeExec);
        cmdMgr.addCommandExecutor("LSF", lsfExec);
        cmdMgr.addCommandExecutor("LocalJobRunner", localExec);
    }
    
    /**
     * From an entry in the 'job_runner_job' table, get the JobExecutor.
     */
    @Test
    public void getCmdLineLsfRunner() {
        assertEquals("cmdMgr.jobExecutorById", lsfExec, 
                cmdMgr.lookupJobExecutorByJobRunnerName("CmdLineLsfRunner"));
        assertEquals("expected jobRunnerClassname", CmdLineLsfRunner.class.getName(),
                cmdMgr.lookupJobExecutorByJobRunnerName("CmdLineLsfRunner").getJobRunnerClassname());
    }
    
    @Test
    public void getLocalCommonsExecJobRunner() {
        assertEquals("cmdMgr.jobExecutorById", localExec, 
                cmdMgr.lookupJobExecutorByJobRunnerName("LocalCommonsExecJobRunner"));
        assertEquals("expected jobRunnerClassname", LocalCommonsExecJobRunner.class.getName(),
                cmdMgr.lookupJobExecutorByJobRunnerName("LocalCommonsExecJobRunner").getJobRunnerClassname());
    }
    
    @Test
    public void validIdNotAJobExecutorInstance() {
        Assert.assertNull(cmdMgr.lookupJobExecutorByJobRunnerName("RuntimeExec"));
    }
    
    @Test
    public void noExecutorWithId() {
        cmdMgr=new BasicCommandManager();
        Assert.assertNull(cmdMgr.lookupJobExecutorByJobRunnerName("RuntimeExec"));
    }
    
    /** Expecting an exception */
    @Test(expected=ConfigurationException.class)
    public void duplicateJobRunnerName() throws ConfigurationException {
        JobExecutor lsfLong=mock(JobExecutor.class);
        when(lsfLong.getJobRunnerName()).thenReturn(CmdLineLsfRunner.class.getSimpleName());
        cmdMgr.addCommandExecutor("LSF_Long", lsfLong);
    }
}
