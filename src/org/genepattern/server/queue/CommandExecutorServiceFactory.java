package org.genepattern.server.queue;

import org.genepattern.webservice.JobInfo;

/**
 * Factory class for generating instances of CommandExecutorService, based on a given JobInfo.
 * This is here to enable configurable routing of jobs to different types of command executors.
 * By default, jobs are run with calls to RuntimeExec.
 * This factory class can be updated to send jobs to LSF.
 * 
 * @author pcarr
 */
public class CommandExecutorServiceFactory {
    public static CommandExecutorServiceFactory instance() {
        return Singleton.analysisServiceFactory;
    }

    private static class Singleton {
        static CommandExecutorServiceFactory analysisServiceFactory = new CommandExecutorServiceFactory();
    }
    
    private CommandExecutorServiceFactory() {
    }

    private static CommandExecutorService runtimeExecSvc = new RuntimeExecCmdExecSvc();
    
    public CommandExecutorService getAnalysisService(JobInfo jobInfo) {
        //TODO: develop plugin architecture for CommandExecutorService, rather than hard-coding to RuntimeExec.
        return runtimeExecSvc;
    }
}
