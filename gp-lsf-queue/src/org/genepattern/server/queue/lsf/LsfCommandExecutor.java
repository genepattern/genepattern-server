package org.genepattern.server.queue.lsf;

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.apache.log4j.Logger;
import org.genepattern.server.queue.CommandExecutor;
import org.genepattern.webservice.JobInfo;
import org.hibernate.cfg.Environment;
import org.hibernate.transaction.JDBCTransactionFactory;

import edu.mit.broad.core.Main;
import edu.mit.broad.core.lsf.LsfJob;

public class LsfCommandExecutor implements CommandExecutor {
    private static Logger log = Logger.getLogger(LsfCommandExecutor.class);
    //for submitting jobs to the LSF queue
    private static ExecutorService executor = Executors.newFixedThreadPool(3);
    
    public void start() {
        log.info("Initializing LsfCommandExecSvc ...");
        try {
            System.setProperty("jboss.server.name", System.getProperty("fqHostName", "localhost"));
            
            Main broadCore = Main.getInstance();
            broadCore.setEnvironment("prod");

            String dataSourceName = System.getProperty("jndi.datasource.name", "java:comp/env/jdbc/db1");
            log.info("using jndi.datasource.name="+dataSourceName);
            broadCore.setDataSourceName(dataSourceName);

            Properties props = new Properties();
            props.put(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");
            props.put(Environment.TRANSACTION_STRATEGY, JDBCTransactionFactory.class.getName());
            props.put(Environment.DEFAULT_SCHEMA, "GENEPATTERN_DEV_01");
            props.put(Environment.DIALECT, "org.genepattern.server.database.PlatformOracle9Dialect");
            broadCore.setHibernateOptions(props);

            //TODO: modify the check frequency, current we only check every 60 seconds
            broadCore.setLsfCheckFrequency(60);
            broadCore.start();
        }
        catch (Throwable t) {
            log.error("Error starting BroadCore: "+t.getLocalizedMessage(), t);
        }
        log.info("done!");
    }

    public void stop() {
        log.info("Stopping LsfCommandExecSvc ...");
        try {
            Main.getInstance().stop();
        }
        catch (Throwable t) {
            log.error("Error shutting down BroadCore: "+t.getLocalizedMessage(), t);
        }
        log.info("done!");
    }

    public void runCommand(String[] commandLine, Map<String, String> environmentVariables, File runDir, File stdoutFile, File stderrFile, JobInfo jobInfo, String stdin, StringBuffer stderrBuffer) 
    throws Exception
    {
        log.debug("Running command for job "+jobInfo.getJobNumber()+". "+jobInfo.getTaskName());
        LsfCommand cmd = new LsfCommand();
        cmd.runCommand(commandLine, environmentVariables, runDir, stdoutFile, stderrFile, jobInfo, stdin, stderrBuffer);
        LsfJob lsfJob = cmd.getLsfJob();
        lsfJob = submitJob(lsfJob);
        log.debug(jobInfo.getJobNumber()+". "+jobInfo.getTaskName()+" is dispatched.");
    }        

    /**
     * Uses the BroadCore library to submit the job to LSF, must handle database transactions in this method.
     * @param lsfJob
     * @return the value returned from LsfWrapper#dispatchLsfJob
     */
    private LsfJob submitJob(final LsfJob lsfJob) throws Exception { 
        Callable<LsfJob> c = new LsfTransactedCallable(lsfJob);
        FutureTask<LsfJob> future = new FutureTask<LsfJob>(c);
        executor.execute(future);
        try {
            LsfJob lsfJobOut = future.get();
            log.debug("submitted job to LSF queue: internalJobId="+lsfJobOut.getInternalJobId()
                    +", lsfId= "+lsfJobOut.getLsfJobId());
        }
        catch (Exception e) {
            log.error("Error submitting job to LSF, job #"+lsfJob.getInternalJobId());
            throw e;
        }
        return lsfJob;
    }
    
    public void terminateJob(JobInfo jobInfo) {
        log.error("Terminate job not enabled");
        //TODO: implement terminate job in BroadCore library. It currently is not part of the library, pjc.
    }
    
}


