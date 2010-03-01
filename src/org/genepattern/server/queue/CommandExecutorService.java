package org.genepattern.server.queue;


import org.genepattern.webservice.JobInfo;

/**
 * @author pcarr
 */
public interface CommandExecutorService extends CommandExecutor {
    public void start();
    public void stop();
    public void terminateJob(JobInfo jobInfo);
}


