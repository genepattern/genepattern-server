package org.genepattern.server.executor.lsf;

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.genepattern.server.executor.CommandExecutor;
import org.genepattern.server.executor.CommandManagerFactory;
import org.genepattern.webservice.JobInfo;

import edu.mit.broad.core.Main;
import edu.mit.broad.core.lsf.LsfJob;

public class LsfCommandExecutor implements CommandExecutor {
    private static Logger log = Logger.getLogger(LsfCommandExecutor.class);

    //for submitting jobs to the LSF queue
    private static ExecutorService executor = null;
    private Properties configurationProperties = new Properties();
    
    public void setConfigurationFilename(String filename) {
        log.error("method not implemented, setConfigurationFilename( "+filename+" )");
    }
    
    public void setConfigurationProperties(Properties properties) {
        this.configurationProperties.putAll(properties);
    }
    
    public void start() {
        log.info("Initializing LsfCommandExecSvc ...");
        executor = Executors.newFixedThreadPool(3);
        try {
            
            //load custom properties
            System.setProperty("jboss.server.name", System.getProperty("fqHostName", "localhost"));
            Main broadCore = Main.getInstance();
            broadCore.setEnvironment("prod"); 
            
            String dataSourceName = this.configurationProperties.getProperty("hibernate.connection.datasource", "java:comp/env/jdbc/cmap_dev/genepattern_dev_01");
            log.info("using hibernate.connection.datasource="+dataSourceName);
            broadCore.setDataSourceName(dataSourceName);
            log.info("setting hibernate options...");
            for(Entry<?,?> entry : configurationProperties.entrySet()) {
                log.info(""+entry.getKey()+": "+entry.getValue());
            }
            broadCore.setHibernateOptions(configurationProperties);

            int lsfCheckFrequency = 60;
            String lsfCheckFrequencyProp = configurationProperties.getProperty("lsf.check.frequency");
            if (lsfCheckFrequencyProp != null) {
                try {
                    lsfCheckFrequency = Integer.parseInt(lsfCheckFrequencyProp);
                }
                catch (NumberFormatException e) {
                    log.error("Invalid value, lsf.check.frequency="+lsfCheckFrequencyProp,e);
                    log.error("Using lsf.check.frequency="+lsfCheckFrequency+" instead");
                }
            }
            log.info("broadCore.setLsfCheckFrequency="+lsfCheckFrequency);
            broadCore.setLsfCheckFrequency(lsfCheckFrequency);

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
            log.debug("stopping BroadCore...");
            Main.getInstance().stop();
        }
        catch (Throwable t) {
            log.error("Error shutting down BroadCore: "+t.getLocalizedMessage(), t);
        }
        if (executor != null) {
            log.debug("stopping executor...");
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.error("executor shutdown timed out after 30 seconds.");
                    executor.shutdownNow();
                }
            }
            catch (InterruptedException e) {
                log.error("executor.shutdown was interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
        log.info("done!");
    }

    public void runCommand(String[] commandLine, Map<String, String> environmentVariables, File runDir, File stdoutFile, File stderrFile, JobInfo jobInfo, String stdin, StringBuffer stderrBuffer) 
    throws Exception
    {
        log.debug("Running command for job "+jobInfo.getJobNumber()+". "+jobInfo.getTaskName());
        LsfCommand cmd = new LsfCommand();
        
        Properties lsfProperties = CommandManagerFactory.getCommandManager().getCommandProperties(jobInfo);
        cmd.setLsfProperties(lsfProperties);
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
        if (executor == null) {
            log.error("service not started ... ignoring submitJob("+lsfJob.getName()+")");
            return lsfJob;
        }
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
        //TODO: implement terminate job using the BroadCore library.
        String jobId = jobInfo != null ? ""+jobInfo.getJobNumber() : "null";
        log.error("Terminating job "+jobId+": terminateJob not implemented");
    }

}


