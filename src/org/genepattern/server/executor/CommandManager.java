package org.genepattern.server.executor;

import java.util.Map;

/**
 * This is the core service for managing GenePattern job execution. It is responsible for polling the internal database for
 * new user job submissions and executing each job on the appropriate queue. 
 * 
 * For historical reasons (this is a refactoring of code in the StartupServlet and GenePatternAnalysisTask) job submission is handled by two related processes:
 * 1) an internal job queue, and
 * 2) a list of one or more CommandExecutors.
 * 
 * The list of CommandExecutors are configurable so that different job execution systems (such as LSF or SGE) can be integrated into GenePattern.
 * 
 * The internal job queue polls the database for new jobs (status of PENDING in the ANALYSIS_JOB table). 
 * When new jobs are found, it calls GenePatternAnalysisTask#onJob.
 * The onJob method prepares the job for execution, then submits the job to the appropriate CommandExecutor instance.
 * The CommandManager is responsible for providing the CommandExecutor for each job, this is configurable my implementing the CommandExecutorMapper interface.
 * 
 * The default (circa GP 3.2.2) CommandExecutor runs each job in a new process. A new thread is created for each job, which waits for the job
 * to complete before its results are processed.
 * 
 * @author pcarr
 */
public interface CommandManager extends CommandExecutorMapper {  
    //internal job queue
    /**
     * Call this at system startup, e.g. from javax.servlet.Servlet#init.
     */
    void startAnalysisService();
    /**
     * Call this at system shutdown, e.g. from javax.servlet.Servlet#destroy.
     */
    void shutdownAnalysisService();
    
    //job execution 
    void startCommandExecutors();
    void stopCommandExecutors();
    Map<String,CommandExecutor> getCommandExecutorsMap();

    void wakeupJobQueue();
}
