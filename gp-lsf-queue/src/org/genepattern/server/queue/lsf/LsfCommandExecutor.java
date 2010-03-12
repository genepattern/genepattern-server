package org.genepattern.server.queue.lsf;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.queue.CommandExecutor;
import org.genepattern.webservice.JobInfo;
import org.hibernate.cfg.Environment;
import org.hibernate.transaction.JDBCTransactionFactory;

import edu.mit.broad.core.Main;
import edu.mit.broad.core.lsf.LsfJob;
import edu.mit.broad.core.lsf.LsfWrapper;

public class LsfCommandExecutor implements CommandExecutor {
    private static Logger log = Logger.getLogger(LsfCommandExecutor.class);
    
    public void start() {
        log.info("Initializing LsfCommandExecSvc ...");
        try {
            Main broadCore = Main.getInstance();

            //String dataSourceName = System.getProperty("jndi.datasource.name", "java:comp/env/jdbc/myoracle");
            //log.info("using jndi.datasource.name="+dataSourceName);
            //broadCore.setDataSourceName("jndi:/jdbc/our_pool");
            //broadCore.setDataSourceName(dataSourceName);
            broadCore.setEnvironment("prod");
            Properties props = new Properties();
            props.put(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");
            props.put(Environment.TRANSACTION_STRATEGY, JDBCTransactionFactory.class.getName());
            broadCore.setHibernateOptions(props);
            broadCore.setLsfCheckFrequency(10);
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
        try {
            log.debug("Running command for job "+jobInfo.getJobNumber()+". "+jobInfo.getTaskName());
            HibernateUtil.beginTransaction();        
            LsfCommand cmd = new LsfCommand();
            cmd.runCommand(commandLine, environmentVariables, runDir, stdoutFile, stderrFile, jobInfo, stdin, stderrBuffer);
        
            LsfJob lsfJob = cmd.getLsfJob();
            lsfJob = new LsfWrapper().dispatchLsfJob(lsfJob);
            HibernateUtil.commitTransaction();
            log.debug(jobInfo.getJobNumber()+". "+jobInfo.getTaskName()+" is dispatched.");
        }
        catch (Throwable t) {
            try {
                HibernateUtil.rollbackTransaction();
            }
            catch (Throwable t1) {
                log.error("Error in HibernateUtil.rollbackTransaction", t1);
            }
            throw new Exception(t);
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }
    
    public void terminateJob(JobInfo jobInfo) {
        log.error("Terminate job not enabled");
        //TODO: implement terminate job in BroadCore library. It currently is not part of the library, pjc.
    }
}
