package org.genepattern.server.executor.lsf;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.executor.CommandExecutor;
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.executor.CommandManagerFactory;
import org.genepattern.server.executor.CommandProperties;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.webservice.JobInfo;
import org.hibernate.Query;
import org.hibernate.Session;

import edu.mit.broad.core.Main;
import edu.mit.broad.core.lsf.LocalLsfJob;
import edu.mit.broad.core.lsf.LsfJob;
import edu.mit.broad.core.lsf.LsfJob.JobCompletionListener;

public class LsfCommandExecutor implements CommandExecutor {
    private static Logger log = Logger.getLogger(LsfCommandExecutor.class);

    //for submitting jobs to the LSF queue
    private Main broadCore = null;
    private static volatile ExecutorService jobSubmissionService = null;
    private static volatile ExecutorService jobCompletionService = null;
    private static int numJobSubmissionThreads = 3;
    private static int numJobCompletionThreads = 3;
    
    private final CommandProperties configurationProperties = new CommandProperties();
    
    public void setConfigurationFilename(final String filename) {
        log.error("method not implemented, setConfigurationFilename( "+filename+" )");
    }

    public void setConfigurationProperties(final CommandProperties properties) {
        this.configurationProperties.putAll(properties);
        
        //WARNING: setting a static variable because the getJobCompletionService must be static
        String key = "lsf.num.job.submission.threads";
        String prop = this.configurationProperties.getProperty(key);
        if (prop != null) {
            try {
                numJobSubmissionThreads = Integer.parseInt( prop );
            }
            catch (final Exception e) {
                log.error("Error in configuration file: "+key+"="+prop);
            }
        }
        key = "lsf.num.job.completion.threads";
        prop = this.configurationProperties.getProperty(key);
        if (prop != null) {
            try {
                numJobCompletionThreads = Integer.parseInt( prop );
            }
            catch (final Exception e) {
                log.error("Error in configuration file: "+key+"="+prop);
            }
        }
    }
    
    private static ExecutorService getJobSubmissionService() { 
        if (jobSubmissionService == null) {
            log.info("Initializing jobSubmissionService with a fixed thread pool of size "+numJobSubmissionThreads);
            jobSubmissionService = Executors.newFixedThreadPool(numJobSubmissionThreads);
        }
        return jobSubmissionService;
    }

    public static ExecutorService getJobCompletionService() { 
        if (jobCompletionService == null) {
            log.info("Initializing jobCompletionService with a fixed thread pool of size "+numJobCompletionThreads);
            jobCompletionService = Executors.newFixedThreadPool(numJobCompletionThreads);
        }
        if (jobCompletionService != null) {
            return jobCompletionService;
        }
        else {
            log.error("Invalid call to getJobCompletionService before call to start");
            return jobCompletionService;
        }
    }
    
    public void start() {
        log.info("Initializing LsfCommandExecSvc ...");
        
        getJobSubmissionService();
        getJobCompletionService();

        try {
            //initialize the GAP_SERVER_ID column of the LSF_JOB table
            final String broadCoreEnv = this.configurationProperties.getProperty("broadcore.env", "prod");
            String broadCoreServerName = this.configurationProperties.getProperty("broadcore.server.name");
            if (broadCoreServerName == null) {
                broadCoreServerName = System.getProperty("jboss.server.name");
            }
            if (broadCoreServerName == null) {
                broadCoreServerName = System.getProperty("fqHostName", "localhost");
            }
            System.setProperty("jboss.server.name", broadCoreServerName);
            this.broadCore = Main.getInstance();
            this.broadCore.setEnvironment(broadCoreEnv); 
            
            final String dataSourceName = this.configurationProperties.getProperty("hibernate.connection.datasource", "java:comp/env/jdbc/gpdb");
            log.info("using hibernate.connection.datasource="+dataSourceName);
            this.broadCore.setDataSourceName(dataSourceName);
            log.info("setting hibernate options...");
            //TODO: tidy this up ...
            Properties configurationPropertiesProperties = this.configurationProperties.toProperties();
            for(final Entry<?,?> entry : configurationPropertiesProperties.entrySet()) {
                log.info(""+entry.getKey()+": "+entry.getValue());
            }
            this.broadCore.setHibernateOptions(configurationPropertiesProperties);

            int lsfCheckFrequency = 60;
            final String lsfCheckFrequencyProp = this.configurationProperties.getProperty("lsf.check.frequency");
            if (lsfCheckFrequencyProp != null) {
                try {
                    lsfCheckFrequency = Integer.parseInt(lsfCheckFrequencyProp);
                }
                catch (final NumberFormatException e) {
                    log.error("Invalid value, lsf.check.frequency="+lsfCheckFrequencyProp,e);
                    log.error("Using lsf.check.frequency="+lsfCheckFrequency+" instead");
                }
            }
            log.info("broadCore.setLsfCheckFrequency="+lsfCheckFrequency);
            this.broadCore.setLsfCheckFrequency(lsfCheckFrequency);

            this.broadCore.start();
        }
        catch (final Throwable t) {
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
        catch (final Throwable t) {
            log.error("Error shutting down BroadCore: "+t.getLocalizedMessage(), t);
        }
        shutdownService("jobSubmissionService", jobSubmissionService);
        shutdownService("jobCompletionService", jobCompletionService);
        log.info("done!");
    }
    
    private static void shutdownService(final String serviceName, final ExecutorService executorService) {
        if (executorService != null) {
            log.debug("stopping "+serviceName+" executor...");
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.error(serviceName+" executor shutdown timed out after 30 seconds.");
                    executorService.shutdownNow();
                }
            }
            catch (final InterruptedException e) {
                log.error(serviceName+" executor.shutdown was interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    public void runCommand(final String[] commandLine, final Map<String, String> environmentVariables, final File runDir, final File stdoutFile, final File stderrFile, final JobInfo jobInfo, final File stdinFile) 
    throws CommandExecutorException
    {
        runCommand(commandLine, environmentVariables, runDir, stdoutFile, stderrFile, jobInfo, stdinFile, LsfJobCompletionListener.class);
    }

    public void runCommand(final String[] commandLine,
			final Map<String, String> environmentVariables, final File runDir,
			final File stdoutFile, final File stderrFile,
			final JobInfo jobInfo, final File stdinFile,
			final Class<? extends JobCompletionListener> completionListenerClass)
			throws CommandExecutorException {
		log.debug("Running command for job "+jobInfo.getJobNumber()+". "+jobInfo.getTaskName());
        final LsfCommand cmd = new LsfCommand();
        
        final CommandProperties lsfProperties = CommandManagerFactory.getCommandManager().getCommandProperties(jobInfo);
        cmd.setLsfProperties(lsfProperties);
        cmd.runCommand(commandLine, environmentVariables, runDir, stdoutFile, stderrFile, jobInfo, stdinFile, completionListenerClass);
        LsfJob lsfJob = cmd.getLsfJob();
        lsfJob = submitJob(""+jobInfo.getJobNumber(), lsfJob);
        log.debug(jobInfo.getJobNumber()+". "+jobInfo.getTaskName()+" is dispatched.");
	}

    /**
     * Uses the BroadCore library to submit the job to LSF, must handle database transactions in this method.
     * @param lsfJob
     * @return the value returned from LsfWrapper#dispatchLsfJob
     */
    private LsfJob submitJob(final String gpJobId, final LsfJob lsfJob) throws CommandExecutorException { 
        if (jobSubmissionService == null) {
            log.error("service not started ... ignoring submitJob("+lsfJob.getName()+")");
            return lsfJob;
        }
        final Callable<LsfJob> c = new LsfTransactedCallable(lsfJob);
        final FutureTask<LsfJob> future = new FutureTask<LsfJob>(c);
        jobSubmissionService.execute(future);
        try {
            final LsfJob lsfJobOut = future.get();
            log.debug("submitted job to LSF queue: internalJobId="+lsfJobOut.getInternalJobId()
                    +", lsfId= "+lsfJobOut.getLsfJobId());
        }
        catch (final Exception e) {
            throw new CommandExecutorException("Error submitting job to LSF, job #"+gpJobId, e);
        }
        return lsfJob;
    }
    
    public void terminateJob(final JobInfo jobInfo) {
        if (jobInfo == null) {
            log.error("Terminating job with null jobInfo!");
            return;
        }
        final String gpJobId = ""+jobInfo.getJobNumber();
        final List<LsfJob> lsfJobs = getLsfJobByGpJobId(gpJobId);
        if (lsfJobs.isEmpty()) {
            //handle special-case, terminating a job which has not yet been added to the LSF queue
            log.error("Didn't find lsf job for gp job: "+gpJobId);
            try {
                GenePatternAnalysisTask.handleJobCompletion(jobInfo.getJobNumber(), -1, "User terminated job #"+jobInfo.getJobNumber()+" before it was added to the LSF queue");
            } 
            catch (final Exception e) {
                log.error("Error terminating job "+jobInfo.getJobNumber(), e);
            }
        } 
        else {
            terminateJobs(gpJobId, lsfJobs);
        }
    }

	public static void terminateJobs(final String gpJobId, final Collection<LsfJob> lsfJobs) {
		final Collection<LocalLsfJob> localLsfJobs = new ArrayList<LocalLsfJob>(lsfJobs.size());
        for (final LsfJob lsfJob : lsfJobs) {
        	localLsfJobs.add(convert(lsfJob));
        }
    	LocalLsfJob.cancel(localLsfJobs);
	}
    
    //NOTE: based on assumption that the GenePattern ANALYSIS_JOB.JOB_NO is stored in the LSF_JOB.JOB_NAME column
    /**
     * gets the list of all lsf jobs related to this gp job id in the order in which they were submitted.
     */
    private List<LsfJob> getLsfJobByGpJobId(final String gpJobId) {
        final Session session= this.broadCore.getHibernateSession();
        if (session == null) {
            log.error("broadCore.hibernateSession is null");
            return Collections.emptyList();
        }
        try {
            session.beginTransaction();
            final Query query = session.createQuery("from LsfJob as lsfJob where lsfJob.name = ? order by lsfJob.internalJobId");
            query.setString(0, gpJobId);
            return query.list();
        }
        finally {
            session.close();
        }
    }
    
    //copied private code from BroadCore LsfWrapper.convert
    //    TODO: should update BroadCore
    /**
     * Creates an LocalLsfJob instance from an LsfJob instance. Checks to see what
     * fields are null on the incoming LsfJob, and only sets non-null properties
     * on the LocalLsfJob.
     */
    private static LocalLsfJob convert(final LsfJob job) {
        final LocalLsfJob localJob = new LocalLsfJob();
        localJob.setCommand(job.getCommand());
        localJob.setName(job.getName());
        localJob.setQueue(job.getQueue());
        localJob.setProject(job.getProject());
        localJob.setExtraBsubArgs(job.getExtraBsubArgs());

        if (job.getWorkingDirectory() != null) {
            localJob.setWorkingDir(new File(job.getWorkingDirectory()));
        }

        if (job.getInputFilename() != null) {
            localJob.setInputFile(getAbsoluteFile(job.getWorkingDirectory(),
                    job.getInputFilename()));
        }

        if (job.getOutputFilename() != null) {
            localJob.setOutputFile(getAbsoluteFile(job.getWorkingDirectory(),
                    job.getOutputFilename()));
        }

        if (job.getErrorFileName() != null) {
            localJob.setErrFile(getAbsoluteFile(job.getWorkingDirectory(), job
                    .getErrorFileName()));
        }

        if (job.getLsfJobId() != null) {
            localJob.setBsubJobId(job.getLsfJobId());
        }

        if (job.getStatus() != null) {
            localJob.setLsfStatusCode(job.getStatus());
        }

        return localJob;
    }

    private static File getAbsoluteFile(final String dir, final String filename) {
        if (filename.startsWith(File.separator)) {
            return new File(filename);
        } else {
            return new File(dir, filename);
        }
    }
    
    public int handleRunningJob(final JobInfo jobInfo) throws Exception {
        log.debug("handle running job #"+jobInfo.getJobNumber()+" on startup");
        return JobStatus.JOB_PROCESSING;
    }
}
