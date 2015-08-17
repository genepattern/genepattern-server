/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.ExecutionException;

import org.genepattern.drm.impl.local.commons_exec.LocalCommonsExecJobRunner;
import org.genepattern.drm.impl.lsf.core.CmdLineLsfRunner;
import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.config.ConfigurationException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.executor.drm.JobExecutor;
import org.junit.Before;
import org.junit.Test;

public class TestBasicCommandManager {
    HibernateSessionManager mgr;
    GpConfig gpConfig;
    BasicCommandManager cmdMgr;
    JobExecutor localExec;
    JobExecutor lsfExec;
    JobExecutor sgeExec;
    
    CommandExecutor runtimeExec;
    
    @Before
    public void setUp() throws ConfigurationException, ExecutionException {
        mgr=DbUtil.getTestDbSession();
        gpConfig=new GpConfig.Builder().build();
        cmdMgr=new BasicCommandManager(mgr, gpConfig);
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
        assertNull(cmdMgr.lookupJobExecutorByJobRunnerName("RuntimeExec"));
    }
    
    @Test
    public void noExecutorWithId() {
        cmdMgr=new BasicCommandManager(mgr, gpConfig);
        assertNull(cmdMgr.lookupJobExecutorByJobRunnerName("RuntimeExec"));
    }
    
    /** Expecting an exception */
    @Test(expected=ConfigurationException.class)
    public void duplicateJobRunnerName() throws ConfigurationException {
        JobExecutor lsfLong=mock(JobExecutor.class);
        when(lsfLong.getJobRunnerName()).thenReturn(CmdLineLsfRunner.class.getSimpleName());
        cmdMgr.addCommandExecutor("LSF_Long", lsfLong);
    }
}
