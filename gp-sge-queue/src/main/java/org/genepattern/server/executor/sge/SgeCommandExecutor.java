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

import scala.Option;
import scala.Some;

/**
 * CommandExecutor for SGE with DRMAA / Java, based on Zamboni implementation, via src code provided circa July 2011.
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
    }

    public void setConfigurationProperties(CommandProperties properties) {
        //set properties from config.file, section for this executor
        this.configurationProperties.putAll( properties );
    }
    
    public void start() { 
        log.info("starting SGE CommandExecutor...");
        
        // listing system properties
        log.info("SGE_ROOT="+System.getProperty("SGE_ROOT"));
        log.info("SGE_CELL="+System.getProperty("SGE_CELL")); 
        
        String sgeRoot = configurationProperties.getProperty(Prop.SGE_ROOT.name(), System.getProperty(Prop.SGE_ROOT.name()));
        String sgeCell = configurationProperties.getProperty(Prop.SGE_CELL.name(), System.getProperty(Prop.SGE_CELL.name()));
        String sgeProject = configurationProperties.getProperty(Prop.SGE_PROJECT.name());
        //if the session_file is relative, assume it is relative to the resources directory (rather than the working directory)
        String sgeSessionFile = configurationProperties.getProperty(Prop.SGE_SESSION_FILE.name());
        if (sgeSessionFile == null) {
            sgeSessionFile = System.getProperty("resources", ".") + "/conf/sge_contact.txt";
        }
        String sgeBatchSystemName = configurationProperties.getProperty(Prop.SGE_BATCH_SYSTEM_NAME.name(), "gp_server");
        String sgeLogFilename = configurationProperties.getProperty(Prop.SGE_LOG_FILENAME.name(), ",sge.out");
        
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
        if (sgeProject != null) {
            System.setProperty(Prop.SGE_SESSION_FILE.name(), sgeSessionFile);
        }
        if (sgeLogFilename != null) {
            System.setProperty(Prop.SGE_LOG_FILENAME.name(), sgeLogFilename);
        }
        //initialize Zamboni's SGE service
        sgeBatchSystem = new SgeBatchSystem(sgeBatchSystemName);
        startJobMonitor();
    }

    public void stop() {
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

    public int handleRunningJob(JobInfo jobInfo) throws Exception {
        if (jobInfo == null) {
            throw new IllegalArgumentException("jobInfo is null");
        }
        if (sgeBatchSystem == null) {
            throw new Exception("sgeBatchSystem not initialized; handleRunningJob(jobId="+jobInfo.getJobNumber()+")");
        }
        
        String sgeJobId = new JobRecorder().getSgeJobId(jobInfo);
        BatchJob sgeJob = this.getBatchJobFromGpJobInfo(jobInfo);
        sgeJob.setJobId(new scala.Some<String>(sgeJobId)); 
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
        
        try {
            BatchJob sgeJob = getBatchJobFromGpJobInfo( jobInfo );
            //TODO: need to optionally set the sgeJob.jobId by looking it up in the GP DB table
            sgeJob.setCommand(new scala.Some<String>(commandLine[0])); 
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
            sgeJob.setArgs( new scala.Some<String[]>(args) );
            sgeJob.setWorkingDirectory( new scala.Some<String>(runDir.getPath()) );
            sgeJob.setOutputPath( new scala.Some<String>(stdoutFile.getPath()) );
            sgeJob.setErrorPath( new scala.Some<String>(stderrFile.getPath()) );
            sgeJob.setJobName( new scala.Some<String>("GP_"+jobInfo.getJobNumber()) );
            if (stdinFile != null) {
                //@see org.ggf.drmaa.JobTemplate#setInputPath for details 
                String inputPath = ":"+stdinFile.getAbsolutePath();
                sgeJob.setInputPath(inputPath);
            }
            
            sgeJob = sgeBatchSystem.submit(sgeJob);
            
            //TODO: think about error handling, the job is presumably running on SGE, however if we have DB errors in the 
            //    following lines of code, the GP server will assume the job is not running
            log.debug("submitted job to SGE, gp_job_id="+jobInfo.getJobNumber()+", sge_job_id="+sgeJob.getJobId());
            new JobRecorder().createSgeJobRecord(jobInfo, sgeJob);
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
        BatchJob sgeJob = getBatchJobFromGpJobInfo( jobInfo );
        //TODO: need to optionally set the sgeJob.jobId by looking it up in the GP DB table 
        sgeBatchSystem.kill( sgeJob );
    }
    
    //helper methods
    /**
     * Get the working directory for the given job.
     * 
     * @param jobInfo
     * @return
     * @throws Exception
     */
    private File getWorkingDir(JobInfo jobInfo) throws Exception {
        boolean gp_3_3_3 = false;
        if (gp_3_3_3) {
            //TODO: if you are working with GP 3.3.3 or later, use JobManager#getWorkingDirectory
            //return JobManager.getWorkingDirectory(jobInfo);
        }
        
        //3.3.2 based 
        if (jobInfo == null) {
            throw new IllegalArgumentException("Can't get working directory for jobInfo=null");
        }
        if (jobInfo.getJobNumber() < 0) {
            throw new IllegalArgumentException("Can't get working directory for jobInfo.jobNumber="+jobInfo.getJobNumber());
        }

        File jobDir = null;
        try {
            ServerConfiguration.Context jobContext = ServerConfiguration.Context.getContextForJob(jobInfo);
            File rootJobDir = ServerConfiguration.instance().getRootJobDir(jobContext);
            jobDir = new File(rootJobDir, ""+jobInfo.getJobNumber());
        }
        catch (ServerConfiguration.Exception e) {
            throw new Exception(e.getLocalizedMessage());
        }
        return jobDir;
    }
    
    /**
     * Create a new BatchJob instance, based on the given JobInfo. If we already have a BatchJob.id for the job set it.
     * @param jobInfo
     * @return
     */
    private BatchJob getBatchJobFromGpJobInfo(JobInfo jobInfo) throws Exception {
        
        //init all variables to None
        Option<String> workingDirectory = scala.Option.apply(null);
        Option<String> command = scala.Option.apply(null);
        String[] args = new String[0];
        Option<String> outputPath = scala.Option.apply(null);
        Option<String>  errorPath = scala.Option.apply(null);
        Option<String[]> emailAddresses = scala.Option.apply(null);
        Option<Integer> priority = scala.Option.apply(null);
        Option<String> jobName = scala.Option.apply(null);
        Option<String> queueName = scala.Option.apply(null);
        Option<Boolean> exclusive = scala.Option.apply(null);
        Option<Integer> maxRunningTime = scala.Option.apply(null);
        Option<Integer> memoryReservation = scala.Option.apply(null);
        Option<Integer> maxMemory = scala.Option.apply(null);
        Option<Integer> slotReservation = scala.Option.apply(null);
        Option<Integer> maxSlots = scala.Option.apply(null);
        Option<Boolean> restartable = scala.Option.apply(null);


        //set workingDirectory from jobInfo
        File runDir = getWorkingDir(jobInfo);
        workingDirectory = new Some<String>(runDir.getPath());
        //set jobName from jobInfo, must not start with a digit
        jobName = new Some<String>( "GP_"+jobInfo.getJobNumber() );
        //set restartable to false (None causes an error in SgeBatchSystem.scala)
        restartable = scala.Option.apply(false);

        BatchJob sgeJob = sgeBatchSystem.newBatchJob(
                workingDirectory,
                command,
                args,
                outputPath,
                errorPath,
                emailAddresses,
                priority,
                jobName,
                queueName,
                exclusive,
                maxRunningTime,
                memoryReservation,
                maxMemory,
                slotReservation,
                maxSlots,
                restartable );
       
        return sgeJob;
    }
}
