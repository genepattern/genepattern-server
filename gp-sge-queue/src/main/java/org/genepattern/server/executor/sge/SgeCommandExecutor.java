package org.genepattern.server.executor.sge;

import java.io.File;
import java.util.Map;

import org.apache.log4j.Logger;
import org.broadinstitute.zamboni.server.batchsystem.BatchJob;
import org.broadinstitute.zamboni.server.batchsystem.sge.SgeBatchSystem;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.executor.CommandExecutor;
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.executor.CommandProperties;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.JobStatus;

/**
 * CommandExecutor for SGE with DRMAA / Java, based on Zamboni implementation, via src code provided circa July 2011.
 * 
 * Example config.file settings
 * <pre>
executors:
    # [Experimental] SGE executor
    SGE:
        classname: org.genepattern.server.executor.sge.SgeCommandExecutor
        configuration.properties:
            #SGE_PROJECT: gpdev_server
            SGE_ROOT: /broad/gridengine
            SGE_CELL: broad
            SGE_SESSION_FILE: ./sge/sge_contact.txt
            SGE_BATCH_SYSTEM_NAME: gpdev_server
 * <pre>
 * 
 * Developer notes:
 *     @see: http://stackoverflow.com/questions/1997433/how-to-use-scala-none-from-java-code
 * 
 * @author pcarr
 *
 */
public class SgeCommandExecutor implements CommandExecutor {
    public static Logger log = Logger.getLogger(SgeCommandExecutor.class);
    
    //configuration property names; loaded from the config.file at runtime
    public enum Prop {
        SGE_PROJECT,
        SGE_ROOT,
        SGE_CELL,
        SGE_SESSION_FILE,
        SGE_BATCH_SYSTEM_NAME,
        SGE_LOG_FILENAME;
    }
    
    private SgeBatchSystem sgeBatchSystem = null;
    private final CommandProperties configurationProperties = new CommandProperties();
    //this is a monitor which monitors the SgeBatchSystem for completed jobs
    private JobMonitor jobMonitor = null;

    public void setConfigurationFilename(String filename) {
        log.error("Ignoring setConfigurationFilename( "+filename+" ): use setConfigurationProperties instead.");
    }

    public void setConfigurationProperties(CommandProperties properties) {
        //set properties from config.file, section for this executor
        this.configurationProperties.putAll( properties );
    }
    
    public void start() { 
        log.info("starting SGE CommandExecutor...");
        
        // listing system properties
        log.debug("SGE_ROOT="+System.getProperty("SGE_ROOT"));
        log.debug("SGE_CELL="+System.getProperty("SGE_CELL")); 
        
        String sgeRoot = configurationProperties.getProperty(Prop.SGE_ROOT.name(), System.getProperty(Prop.SGE_ROOT.name()));
        String sgeCell = configurationProperties.getProperty(Prop.SGE_CELL.name(), System.getProperty(Prop.SGE_CELL.name()));
        String sgeProject = configurationProperties.getProperty(Prop.SGE_PROJECT.name());
        //if the session_file is relative, assume it is relative to the resources directory (rather than the working directory)
        String sgeSessionFile = configurationProperties.getProperty(Prop.SGE_SESSION_FILE.name());
        if (sgeSessionFile == null) {
            sgeSessionFile = System.getProperty("resources", ".") + "/conf/sge_contact.txt";
        }
        String sgeBatchSystemName = configurationProperties.getProperty(Prop.SGE_BATCH_SYSTEM_NAME.name(), "gp_server");
        String sgeLogFilename = configurationProperties.getProperty(Prop.SGE_LOG_FILENAME.name(), ".sge.out");
        
        log.info(Prop.SGE_ROOT.name()+"="+sgeRoot);
        log.info(Prop.SGE_CELL.name()+"="+sgeCell);
        log.info(Prop.SGE_PROJECT.name()+"="+sgeProject);
        log.info(Prop.SGE_SESSION_FILE.name()+"="+sgeSessionFile);
        log.info(Prop.SGE_BATCH_SYSTEM_NAME.name()+"="+sgeBatchSystemName);
        log.info(Prop.SGE_LOG_FILENAME.name()+"="+sgeLogFilename);

        if (sgeRoot != null) {
            System.setProperty(Prop.SGE_ROOT.name(), sgeRoot);
        }
        if (sgeCell != null) {
            System.setProperty(Prop.SGE_CELL.name(), sgeCell);
        }
        if (sgeProject != null) {
            System.setProperty(Prop.SGE_PROJECT.name(), sgeProject);
        }
        if (sgeSessionFile != null) {
            System.setProperty(Prop.SGE_SESSION_FILE.name(), sgeSessionFile);
        }
        if (sgeLogFilename != null) {
            System.setProperty(Prop.SGE_LOG_FILENAME.name(), sgeLogFilename);
        }
        //initialize Zamboni's SGE service
        log.debug("initializing Zamboni's SGE service ...");
        sgeBatchSystem = new SgeBatchSystem(sgeBatchSystemName);
        log.debug("starting job monitor ...");
        startJobMonitor();
        log.info("... started SGE CommandExecutor.");
    }

    public void stop() {
        log.info("stopping SGE CommandExecutor ...");
        if (sgeBatchSystem != null) {
            try {
                log.info("Shutting down SGE Batch System ...");
                sgeBatchSystem.shutDown();
                log.info("Done.");
            }
            catch (Throwable t) {
                log.error("Error shutting down SGE Batch System: "+t.getLocalizedMessage(), t);
            }
        }
        stopJobMonitor();
        log.info("... stopped SGE CommandExecutor.");
    }
    
    private void startJobMonitor() {
        if (jobMonitor == null) {
            jobMonitor = new JobMonitor( sgeBatchSystem );
        }
        jobMonitor.start();
    }
    
    private void stopJobMonitor() {
        if (jobMonitor != null) {
            jobMonitor.stop();
        }
    }

    /**
     * Called on startup of the GP server, for all GP jobs on this queue which have not been
     * flagged as completed.
     */
    public int handleRunningJob(JobInfo jobInfo) throws Exception {
        if (jobInfo == null) {
            throw new IllegalArgumentException("jobInfo is null");
        }
        if (sgeBatchSystem == null) {
            throw new Exception("sgeBatchSystem not initialized; handleRunningJob(jobId="+jobInfo.getJobNumber()+")");
        }
        
        log.debug("handleRunningJob( jobId="+jobInfo.getJobNumber()+" )");
        BatchJob sgeJob = BatchJobUtil.findBatchJob(sgeBatchSystem, jobInfo);
        sgeBatchSystem.restoreOne( sgeJob );
        
        // don't change the status
        int currentStatus = JobStatus.JOB_PROCESSING;
        if (JobStatus.STATUS_MAP.get( jobInfo.getStatus() ) instanceof Integer) {
            currentStatus = (Integer) JobStatus.STATUS_MAP.get( jobInfo.getStatus() );
        }
        return currentStatus;
    }

    /**
     * Submit a GenePattern job to SGE.
     */
    public void runCommand(String[] commandLine, Map<String, String> environmentVariables, File runDir, File stdoutFile, File stderrFile, JobInfo jobInfo, File stdinFile) throws CommandExecutorException {
        //TODO: handle environmentVariables
        //TODO: handle stdinFile
        
        if (jobInfo == null) {
            throw new CommandExecutorException("Error: jobInfo == null");
        }
        if (commandLine == null) {
            throw new CommandExecutorException("commandLine == null");
        }
        if (log.isDebugEnabled()) {
            log.debug("commandLine: ");
            int i=0;
            for(String arg : commandLine) {
                log.debug("    arg["+i+"]='"+arg+"'");
                ++i;
            }
            log.debug("runDir: "+runDir);
            log.debug("stdoutFile: "+stdoutFile);
            log.debug("stderrFile: "+stderrFile);
            log.debug("stdinFile: "+stdinFile);
        }
        
        if (sgeBatchSystem == null) {
            throw new CommandExecutorException("Server Error: sgeBatchSystem == null");
        }
        
        try {
            log.debug("creating batch job ...");
            BatchJob sgeJob = BatchJobUtil.createBatchJob(sgeBatchSystem, jobInfo); 
            log.debug("sgeJob.setCommand( "+commandLine[0]+" )");
            sgeJob.setCommand( scala.Option.apply( commandLine[0] ) );
            String[] args = null;
            if (commandLine.length <= 1) {
                args = new String[0];
            }
            else {
                args = new String[ commandLine.length -1 ];
            }
            for(int i=1; i<commandLine.length; ++i) {
                args[i-1] = commandLine[i];
            } 
            log.debug("sgeJob.setArgs...");
            sgeJob.setArgs( new scala.Some<String[]>(args) );
            log.debug("sgeJob.setWorkingDirectory...");
            sgeJob.setWorkingDirectory( new scala.Some<String>(runDir.getPath()) );
            log.debug("sgeJob.setOutputPath...");
            sgeJob.setOutputPath( new scala.Some<String>(stdoutFile.getPath()) );
            log.debug("sgeJob.setErrorPath...");
            sgeJob.setErrorPath( new scala.Some<String>(stderrFile.getPath()) );
            log.debug("sgeJob.setJobName...");
            sgeJob.setJobName( new scala.Some<String>("GP_"+jobInfo.getJobNumber()) );
            if (stdinFile != null) {
                log.debug("sgeJob.setInputPath...");
                //@see org.ggf.drmaa.JobTemplate#setInputPath for details 
                String inputPath = ":"+stdinFile.getAbsolutePath();
                sgeJob.setInputPath(inputPath);
            }

            log.debug("setting custom properties for sgeJob ...");
            //load custom properties for job
            /*
             * "sge.priority"
             * "sge.queueName"
             * "sge.exclusive"
             * "sge.maxRunningTime"
             * "sge.memoryReservation"
             * "sge.maxMemory"
             * "sge.slotReservation"
             * "sge.restartable");
             */
            ServerConfiguration.Context jobContext = ServerConfiguration.Context.getContextForJob(jobInfo); 
            Integer priority = ServerConfiguration.instance().getGPIntegerProperty(jobContext, "sge.priority", null);
            if (priority != null) {
                log.debug("sgeJob.setPriority( "+priority+" )");
                sgeJob.setPriority( scala.Option.apply( priority ) );
            }
            String queueName = ServerConfiguration.instance().getGPProperty(jobContext, "sge.queueName", null);
            if (queueName != null) {
                log.debug("sgeJob.setQueueName( "+queueName+" )");
                sgeJob.setQueueName( scala.Option.apply( queueName ) );
            }
            Object exclusiveObj = ServerConfiguration.instance().getValue(jobContext, "sge.exclusive");
            if (exclusiveObj != null) {
                Boolean exclusive = ServerConfiguration.instance().getGPBooleanProperty(jobContext, "sge.exclusive");
                log.debug("sgeJob.setExclusive( "+exclusive+" )");
                sgeJob.setExclusive( scala.Option.apply( exclusive ) );
            }
            Integer maxRunningTime = ServerConfiguration.instance().getGPIntegerProperty(jobContext, "sge.maxRunningTime", null);
            if (maxRunningTime != null) {
                log.debug("sgeJob.setMaxRunningTime( "+maxRunningTime+" )");
                sgeJob.setMaxRunningTime( scala.Option.apply( maxRunningTime ) );
            }
            Integer memoryReservation = ServerConfiguration.instance().getGPIntegerProperty(jobContext, "sge.memoryReservation", null);
            if (memoryReservation != null) {
                log.debug("sgeJob.setMemoryReservation( "+memoryReservation+" )");
                sgeJob.setMemoryReservation( scala.Option.apply( memoryReservation ) );
            }
            Integer maxMemory = ServerConfiguration.instance().getGPIntegerProperty(jobContext, "sge.maxMemory", null);
            if (maxMemory != null) {
                log.debug("sgeJob.setMaxMemory( "+maxMemory+" )");
                sgeJob.setMaxMemory( scala.Option.apply( maxMemory ) );
            }
            Integer slotReservation = ServerConfiguration.instance().getGPIntegerProperty(jobContext, "sge.slotReservation", null);
            if (slotReservation != null) {
                log.debug("sgeJob.setSlotReservation( "+slotReservation+" )");
                sgeJob.setSlotReservation( scala.Option.apply( slotReservation ) );
            }
            Object restartableObj = ServerConfiguration.instance().getValue(jobContext, "sge.restartable");
            if (restartableObj != null) {
                Boolean restartable = ServerConfiguration.instance().getGPBooleanProperty(jobContext, "sge.restartable");
                log.debug("sgeJob.setRestartable( "+restartable+" )");
                sgeJob.setRestartable( scala.Option.apply( restartable ) );
            }
            log.debug("submitting job...");
            sgeJob = sgeBatchSystem.submit(sgeJob);
            //TODO: think about error handling, the job is presumably running on SGE, however if we have DB errors in the 
            //    following lines of code, the GP server will assume the job is not running
            log.debug("submitted job to SGE, gp_job_id="+jobInfo.getJobNumber()+", sge_job_id="+sgeJob.getJobId());
            log.debug("creating job record ...");
            BatchJobUtil.createJobRecord(jobInfo, sgeJob);
            log.debug("... Done!");
        }
        catch (Throwable t) {
            throw new CommandExecutorException("Error submitting job "+jobInfo.getJobNumber()+" to SGE: "+t.getLocalizedMessage(), t);
        } 
    }

    /**
     * Cancel an SGE job submitted from GenePattern.
     */
    public void terminateJob(JobInfo jobInfo) throws Exception {
        log.info("terminating SGE job, gp_job_id="+jobInfo.getJobNumber());
        BatchJob sgeJob = BatchJobUtil.findBatchJob(sgeBatchSystem, jobInfo);
        sgeBatchSystem.kill( sgeJob );
    }
    
}
