package org.genepattern.server.executor.awsbatch;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.drm.JobRunner;
import org.genepattern.drm.Memory;
import org.genepattern.drm.Walltime;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.config.Value;
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.executor.CommandProperties;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

/**
 * Implementation of the JobRunner API for AWS Batch integration.
 * Overview:
 *   Each job is run in a docker container.
 *   Support files and data files are copied into the container before running the command.
 *   Output files are copied from the container into the GP Server head node after the command
 *     exits. 
 *   AWS S3 is used as an intermediary data store.
 * 
 * Example config_custom.yaml configuration:
<pre>
    default.properties:
        executor: AWSBatch
    ...
    executors:
        AWSBatch:
            classname: org.genepattern.server.executor.drm.JobExecutor
            configuration.properties:
                jobRunnerClassname: org.genepattern.server.executor.awsbatch.AWSBatchJobRunner
                jobRunnerName: AWSBatchJobRunner
            default.properties:
                # optional AWS Profile, 
                #   see: http://docs.aws.amazon.com/cli/latest/userguide/cli-multiple-profiles.html
                aws-profile: "genepattern"
                aws-s3-root: "s3://gp-example"
                job.queue: "GenePatternAWS"

                # location for aws_batch scripts
                #   'aws-batch-script-dir' path is relative to '<wrapper-scripts>'
                #   'aws-batch-script' path is relative to 'aws-batch-script-dir'
                aws-batch-script-dir: "aws_batch"
                aws-batch-script: "runOnBatch-v0.2.sh" 
</pre>
 *
 * Links:
 * <ul>
 *   <li><a href="https://aws.amazon.com/batch/">aws.amazon.com/batch</a>
 *   <li><a href="https://aws.amazon.com/s3/">aws.amazon.com/s3</a>
 * </ul>
 * 
 */
public class AWSBatchJobRunner implements JobRunner {
    private static final Logger log = Logger.getLogger(AWSBatchJobRunner.class);

    public static final String PROP_AWS_PROFILE="aws-profile";
    
    public static final String PROP_AWS_CLI="aws-cli";
    public static final Value DEFAULT_AWS_CLI=new Value("aws-cli.sh");
    
    // 's3-root' aka S3_PREFIX
    public static final String PROP_AWS_S3_ROOT="aws-s3-root";

    /**
     * Set the 'job.awsbatch.job-definition-name' property to link a particular
     * version of a module to a particular version of an aws batch job definition.
     * 
     * Example 1, AWSBatch default job definition
     * <pre>
     * executors:
     *     AWSBatch:
     *         ...
     *         default.properties:
     *             job.awsbatch.job-definition-name: "Java17_Oracle_Generic"
     * </pre>
     * 
     * Example 2, as part of an executor.properties definition
     * <pre>
     * executor.properties: {
     *     ...
     *     "java/1.7": {
     *         job.awsbatch.job-definition-name: "Java17_Oracle_Generic",
     *     }
     *     ...
     * }
     * </pre>
     * 
     */
    public static final String PROP_JOB_AWSBATCH_JOB_DEF="job.awsbatch.job-definition-name";
    
    public static final String PROP_AWS_BATCH_SCRIPT_DIR="aws-batch-script-dir";
    public static final Value DEFAULT_AWS_BATCH_SCRIPT_DIR=new Value("aws_batch");
    public static final String PROP_AWS_BATCH_SCRIPT="aws-batch-script";
    public static final Value DEFAULT_AWS_BATCH_SCRIPT=new Value("runOnBatch-v0.2.sh");
    public static final String PROP_STATUS_SCRIPT="aws-batch-check-status-script";
    public static final Value DEFAULT_STATUS_SCRIPT=new Value("awsCheckStatus.sh");
    public static final String PROP_SYNCH_SCRIPT="aws-batch-synch-script";
    public static final Value DEFAULT_SYNCH_SCRIPT=new Value("awsSyncDirectory.sh");
    public static final String PROP_CANCEL_SCRIPT="aws-batch-cancel-job-script";
    public static final Value DEFAULT_CANCEL_SCRIPT=new Value("awsCancelJob.sh");

    /** generic implementation of getOrDefault, for use in Java 1.7 */
    public static final <K,V> V getOrDefault(final Map<K,V> map, K key, V defaultValue) {
        V value=map.get(key);
        if (value != null) {
            return value;
        }
        return defaultValue;
    }
    
    private static  Map<String, DrmJobState> batchToGPStatusMap = new HashMap<String, DrmJobState>();

    // commons exec, always set handleQuoting to false when adding command line args
    protected static final boolean handleQuoting = false;
    
    public void setCommandProperties(CommandProperties properties) {
        log.debug("setCommandProperties");
        if (properties==null) {
            log.debug("commandProperties==null");
            return;
        } 
    }

    public void start() {
        log.debug("started JobRunner, classname="+this.getClass());
        
        //AWS Batch Status: SUBMITTED PENDING RUNNABLE STARTING RUNNING FAILED SUCCEEDED       
        batchToGPStatusMap.put("SUBMITTED", DrmJobState.QUEUED);
        batchToGPStatusMap.put("PENDING", DrmJobState.QUEUED);
        batchToGPStatusMap.put("RUNNABLE", DrmJobState.QUEUED);
        batchToGPStatusMap.put("STARTING", DrmJobState.RUNNING);
        batchToGPStatusMap.put("RUNNING", DrmJobState.RUNNING);
        batchToGPStatusMap.put("FAILED", DrmJobState.FAILED);
        batchToGPStatusMap.put("SUCCEEDED", DrmJobState.DONE);
    }

    protected DrmJobStatus initStatus(DrmJobSubmission gpJob) {
        DrmJobStatus status = new DrmJobStatus.Builder(""+gpJob.getGpJobNo(), DrmJobState.QUEUED)
            .submitTime(new Date())
        .build();
        return status;
    }

    protected DrmJobStatus updateStatus_cancel(final String awsId) {
        return new DrmJobStatus.Builder()
            .extJobId(awsId)
            .jobState(DrmJobState.UNDETERMINED)
            .jobState( DrmJobState.CANCELLED ) 
            .exitCode(-1) // hard-code exitCode for user-cancelled task
            .endTime(new Date())
            .jobStatusMessage("Job cancelled by user")
        .build();
    }

    boolean shuttingDown=false;
    @Override
    public void stop() {
        log.debug("shutting down ...");
        shuttingDown=true;
     }

    @Override
    public String startJob(DrmJobSubmission gpJob) throws CommandExecutorException {
        try {
            if (log.isDebugEnabled()) {
                log.debug("startJob, gp_job_id="+gpJob.getGpJobNo());
                AwsBatchUtil.logInputFiles(log, gpJob);
            }
            logCommandLine(gpJob);
            DrmJobStatus jobStatus = submitAwsBatchJob(gpJob);
            if (jobStatus==null) {
                throw new CommandExecutorException("Error starting job: submitAwsBatchJob returned null jobStatus");
            }
            if (log.isDebugEnabled()) {
                log.debug("submitted aws batch job, gp_job_id='"+gpJob.getGpJobNo()+"', aws_job_id="+jobStatus.getDrmJobId());
            }
            return jobStatus.getDrmJobId();
        }
        catch (Throwable t) {
            throw new CommandExecutorException("Error starting job: "+gpJob.getGpJobNo(), t);
        }
    }

    protected String getCheckStatusScript(final DrmJobRecord jobRecord) {
        final File checkStatusScript=getAwsBatchScriptFile(jobRecord, PROP_STATUS_SCRIPT, DEFAULT_STATUS_SCRIPT);
        return ""+checkStatusScript;
    }
    
    protected static Date getOrDefaultDate(final JSONObject awsJob, final String prop, final Date default_value) {
        try {
            if (awsJob.has(prop)) {
                return new Date(awsJob.getLong(prop));
            }
        }
        catch (JSONException e) {
            if (log.isDebugEnabled()) {
                log.debug("Error getting Date from JSON object, name='"+prop+"'", e);
            }
        }
        catch (Throwable t) {
            // its not always present depending non how it failed
            log.error("Unexpected error getting Date from JSON object, name='"+prop+"'", t);
        }  
        return default_value;
    }
    
    protected Integer getExitCodeFromMetadataDir(final File metadataDir) { 
        final File exitCodeFile = new File(metadataDir, "exit_code.txt");
        return parseExitCodeFromFile(exitCodeFile);
    }

    protected Integer parseExitCodeFromFile(final File exitCodeFile) { 
        if (!exitCodeFile.canRead()) {
            log.debug("Can't read exitCodeFile="+exitCodeFile);
            return null;
        }
        try {
            final byte[] encoded = Files.readAllBytes(Paths.get(exitCodeFile.getAbsolutePath()));
            final String jsonText = new String(encoded);
            final JSONObject json = new JSONObject(jsonText);
            return json.getInt("exit_code");
        }
        catch (IOException e) {
            log.error("Error parsing exitCodeFile="+exitCodeFile, e);
        }
        catch (JSONException e) {
            log.error("Error parsing exitCodeFile="+exitCodeFile, e);
        }
        catch (Throwable t) {
            log.error("Unexpected error parsing exitCodeFile="+exitCodeFile, t);
        }
        return null;
    }

    @Override
    public DrmJobStatus getStatus(final DrmJobRecord jobRecord) {
        final String awsId=jobRecord.getExtJobId();
        if (awsId == null) {
            // special-case: job was terminated in a previous GP instance
            return new DrmJobStatus.Builder()
                .extJobId(jobRecord.getExtJobId())
                .jobState(DrmJobState.UNDETERMINED)
                .jobStatusMessage("No record for job, assuming it was terminated at shutdown of GP server")
            .build();
        }
        else {
            // cmd: aws batch describe-jobs --jobs {awsbatch.jobId}
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DefaultExecutor exec=new DefaultExecutor();
            exec.setStreamHandler(new PumpStreamHandler(outputStream));
            final String checkStatusScript=getCheckStatusScript(jobRecord);
            final CommandLine cl=new CommandLine(checkStatusScript);
            cl.addArgument(awsId);
            try {
                final Map<String,String> cmdEnv=initAwsCmdEnv(jobRecord);
                exec.execute(cl, cmdEnv);
                final JSONObject jobJSON = new JSONObject(outputStream.toString().trim());
                final JSONArray jobsArr=jobJSON.optJSONArray("jobs");
                if (jobsArr==null || jobsArr.length()==0) {
                    final String message="Error getting status for job: expecting 'jobs' key in JSON response";
                    log.error(message+", gp_job_id='"+jobRecord.getGpJobNo()+"', aws_job_id="+jobRecord.getExtJobId());
                    return new DrmJobStatus.Builder()
                        .extJobId(jobRecord.getExtJobId())
                        .jobState(DrmJobState.UNDETERMINED)
                        .jobStatusMessage(message)
                    .build();
                }
                final JSONObject awsJob = jobsArr.optJSONObject(0);
                // jobs[0].jobDefinition
                final String jobDefinition = awsJob.optString("jobDefinition");
                // jobs[0].container.image
                final String containerImage;
                final JSONObject container=awsJob.optJSONObject("container");
                if (container != null) {
                    containerImage=container.optString("image");
                }
                else {
                    containerImage=null;
                }
                // jobs[0].container.exitCode, (exitCode=-1 treated as not set)
                final int containerExitCode;
                if (container != null) {
                    containerExitCode=container.optInt("exitCode", -1);
                    if (log.isDebugEnabled()) {
                        log.debug("jobs[0].container.exitCode: "+containerExitCode);
                    }
                }
                else {
                    containerExitCode=-1;
                }

                // jobs[0].jobQueue
                final String jobQueue=awsJob.optString("jobQueue");
                // jobs[0].status
                final String awsStatusCode = awsJob.optString("status");
                // jobs[0].statusReason
                final String awsStatusReason = awsJob.optString("statusReason");
                if (log.isDebugEnabled()) {
                    log.debug("jobs[0].jobDefinition: "+jobDefinition);
                    if (containerImage!=null) {
                        log.debug("jobs[0].container.image: "+containerImage);
                    }
                    log.debug("jobs[0].jobQueue: "+jobQueue);
                    log.debug("jobs[0].status: "+awsStatusCode);
                    log.debug("jobs[0].statusReason: "+awsStatusReason);
                } 
                
                final DrmJobStatus.Builder b=new DrmJobStatus.Builder().extJobId(awsId);
                final DrmJobState jobState=getOrDefault(batchToGPStatusMap, awsStatusCode, DrmJobState.UNDETERMINED);
                b.jobState(jobState);
                if (awsJob.has("jobQueue")) {
                    b.queueId(awsJob.getString("jobQueue"));
                }
                
                if (jobState.is(DrmJobState.TERMINATED)) {
                    // job cleanup, TERMINATED covers SUCCEEDED and FAILED
                    if (log.isDebugEnabled()) {
                        log.debug("job finished with awsStatusCode="+awsStatusCode+", drmJobState="+jobState);
                        log.debug("    jobs[0].statusReason: "+awsStatusReason);
                    }
                    b.startTime(  getOrDefaultDate(awsJob, "startedAt", null) );
                    b.submitTime( getOrDefaultDate(awsJob, "createdAt", null) );
                    b.endTime(    getOrDefaultDate(awsJob, "stoppedAt", new Date()) );
                    final File metadataDir=getMetadataDir(jobRecord);
                    try {
                        refreshWorkingDirFromS3(jobRecord, metadataDir, cmdEnv);
                    } 
                    catch (Throwable t) {
                        log.error("Error copying output files from s3 for job="+jobRecord.getGpJobNo(), t);
                    }
                    final Integer exitCode=getExitCodeFromMetadataDir(metadataDir);
                    if (exitCode != null) {
                        b.exitCode(exitCode);
                        // special-case: custom timeout
                        if (exitCode==142) {
                            b.jobState(DrmJobState.TERM_RUNLIMIT);
                        }
                    }
                    else if (containerExitCode >= 0) {
                        // special-case: aws batch timeout
                        //   awsStatusCode: FAILED
                        //   awsStatusReason: Job attempt duration exceeded timeout
                        //   containerExitCode: 137
                        b.exitCode(containerExitCode);
                        if (awsStatusReason != null) {
                            b.jobStatusMessage(awsStatusReason);
                        }
                    }
                    else {
                        log.error("Error getting exitCode for job="+jobRecord.getGpJobNo());
                        if (awsStatusReason != null) {
                            b.jobStatusMessage(awsStatusReason);
                        }
                    }
                }
                return b.build();
            } 
            catch (Throwable t) {
                log.error(t);
            }
            // status unknown
            return null;
        }
    }

    protected String getSynchWorkingDirScript(final DrmJobRecord jobRecord) {
        final File synchDirScript=getAwsBatchScriptFile(jobRecord, PROP_SYNCH_SCRIPT, DEFAULT_SYNCH_SCRIPT);
        return ""+synchDirScript;
    }

    protected static File getMetadataDir(final DrmJobSubmission gpJob) {
        return getMetadataDir(gpJob.getWorkingDir());
    }

    protected static File getMetadataDir(final DrmJobRecord jobRecord) {
        return getMetadataDir(jobRecord.getWorkingDir());
    }

    protected static File getMetadataDir(final File jobWorkingDir) {
        //// original: job.workingDir/.gp_metadata
        //final File metadir=new File(jobWorkingDir, ".gp_metadata");
        ////return metadir;

        // default: <jobs>/<jobId>.meta
        final File metadir_default=new File(
                jobWorkingDir.getParentFile(),
                jobWorkingDir.getName() + ".meta");
        return metadir_default;
    }

    /**
     * Initialize aws cli environment variables
     * @return a new map of aws cli environment variables
     */
    protected final Map<String,String> initAwsCliEnv(final GpConfig gpConfig, final GpContext jobContext) {
        final Map<String,String> cmdEnv=new HashMap<String,String>();
        return initAwsCliEnv(cmdEnv, gpConfig, jobContext);
    }

    protected final Map<String,String> initAwsCliEnv(final Map<String,String> cmdEnv, final GpConfig gpConfig, final GpContext jobContext) {
        @SuppressWarnings("deprecation")
        final String aws_profile=gpConfig.getGPProperty(jobContext, PROP_AWS_PROFILE);
        if (aws_profile != null) {
            cmdEnv.put("AWS_PROFILE", aws_profile);
        }
        @SuppressWarnings("deprecation")
        final String s3_root=gpConfig.getGPProperty(jobContext, PROP_AWS_S3_ROOT);
        if (s3_root != null) {
            cmdEnv.put("S3_ROOT", s3_root);
            cmdEnv.put("AWS_S3_PREFIX", s3_root);
        }
        return cmdEnv; 
    }

    /**
     * Get environment variables for the aws batch submit-job command line.
     * @return a Map<String,String> of environment variables
     */
    protected final Map<String,String> initAwsCmdEnv(final DrmJobSubmission gpJob) {
        final Map<String,String> cmdEnv=initAwsCliEnv(gpJob.getGpConfig(), gpJob.getJobContext());

        cmdEnv.put("GP_JOB_ID", ""+gpJob.getGpJobNo());
        cmdEnv.put("GP_JOB_WORKING_DIR", gpJob.getWorkingDir().getAbsolutePath());
        final File metadataDir=getMetadataDir(gpJob.getWorkingDir());
        if (metadataDir != null) {
            cmdEnv.put("GP_JOB_METADATA_DIR", metadataDir.getAbsolutePath());
        }
        cmdEnv.put("GP_MODULE_NAME", gpJob.getJobContext().getTaskName());
        cmdEnv.put("GP_MODULE_LSID", gpJob.getJobContext().getLsid());
        cmdEnv.put("GP_MODULE_DIR", gpJob.getTaskLibDir().getAbsolutePath());
        
        final Value bindMounts=gpJob.getValue("job.docker.bind_mounts");
        if (bindMounts != null) {
            cmdEnv.put(
                "GP_JOB_DOCKER_BIND_MOUNTS",
                Joiner.on(":").skipNulls().join(bindMounts.getValues())
            );
        }

        final String dockerImage=gpJob.getProperty("job.docker.image");
        if (Strings.isNullOrEmpty(dockerImage)) {
            log.warn("job.docker.image not set");
        }
        else {
            cmdEnv.put("GP_JOB_DOCKER_IMAGE", dockerImage);
        }

        final String jobQueue=gpJob.getQueue();
        if (Strings.isNullOrEmpty(jobQueue)) {
            log.warn("job.queue not set");
        }
        else {
            cmdEnv.put("JOB_QUEUE", jobQueue);
        }
        final Integer cpuCount=gpJob.getCpuCount();
        if (cpuCount != null) {
            cmdEnv.put("GP_JOB_CPU_COUNT", ""+cpuCount);
        }
        final Memory jobMemory=gpJob.getMemory();
        if (jobMemory != null) {
            final String mib=""+AwsBatchUtil.numMiB(jobMemory);
            cmdEnv.put("GP_JOB_MEMORY_MB", mib);
        } 
        final Walltime walltime=gpJob.getWalltime();
        if (walltime != null) {
            final String walltimeSec=""+walltime.getTimeUnit().toSeconds(walltime.getDuration());
            cmdEnv.put("GP_JOB_WALLTIME_SEC", walltimeSec);
        }
        return cmdEnv;
    }

    protected final Map<String,String> initAwsCmdEnv(final DrmJobRecord jobRecord) {
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        final GpContext jobContext=AwsBatchUtil.initJobContext(jobRecord);
        final Map<String,String> cmdEnv=initAwsCliEnv(gpConfig, jobContext);
        
        final File metadataDir=getMetadataDir(jobRecord.getWorkingDir());
        if (metadataDir != null) {
            cmdEnv.put("GP_JOB_METADATA_DIR", metadataDir.getAbsolutePath());
        }
        
         return cmdEnv;
    }

    /**
     * Pull data files from aws s3 into the local file system.
     * Template:
     *   ./<aws-batch-script-dir>/awsSyncDirectory.sh filepath
     *   aws s3 sync ${S3_ROOT}${1} ${1}
     * Example:
     *   # hard-coded in script
     *   S3_ROOT=s3://moduleiotest
     *   # 1st arg
     *   filepath=/jobResults/1
     *   # optionally set AWS_PROFILE in init-aws-cli-env.sh script
     * Command:
     *   AWS_PROFILE=genepattern; aws s3 sync s3://moduleiotest/jobResults/1 /jobResults/1
     *   aws s3 sync s3://moduleiotest/jobResults/1 /jobResults/1 --profile genepattern
     */
    protected void awsSyncDirectory(final DrmJobRecord jobRecord, final File filepath, final Map<String,String> cmdEnv) {
        // call out to a script to refresh the directory path to pull files from S3
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        //
        // ask AWS for status and update since this is async
        // we'll always just get the old value for now
        //
        DefaultExecutor exec=new DefaultExecutor();
        exec.setStreamHandler(new PumpStreamHandler(outputStream));
        final String synchWorkingDirScript=getSynchWorkingDirScript(jobRecord);
        CommandLine cl= new CommandLine(synchWorkingDirScript);
        // tasklib
        cl.addArgument(filepath.getAbsolutePath());
        try {
            exec.execute(cl, cmdEnv);
            if (log.isDebugEnabled()) {
                String output = outputStream.toString().trim();
                log.debug("sync command output: "+output);
            }            
        } 
        catch (Exception e) {
            log.error(e);
        }
    }

    private void refreshWorkingDirFromS3(final DrmJobRecord jobRecord, final File metadataDir, final Map<String, String> cmdEnv) {
        // pull the job.metaDir from s3
        awsSyncDirectory(jobRecord, metadataDir, cmdEnv);
        // pull the job.workingDir from s3
        awsSyncDirectory(jobRecord, jobRecord.getWorkingDir(), cmdEnv);

        // Now we have synch'd set the jobs stderr and stdout to the ones we got back from AWS
        // since I can't change the DRMJobSubmission objects pointers we'll copy the contents over for now
        if (metadataDir.isDirectory()){
            File stdErr = new File(metadataDir, "stderr.txt");
            if (stdErr.exists()) copyFileContents(stdErr, jobRecord.getStderrFile());
            File stdOut = new File(metadataDir, "stdout.txt");
            if (stdOut.exists()) copyFileContents(stdOut, jobRecord.getStdoutFile());
            
        } 
        else {    
            throw new RuntimeException("Did not get the job.metadata directory. Something seriously went wrong with the AWS batch run");
        }
    }

    protected void copyFileContents(File source, File destination){
        FileReader fr = null;
        FileWriter fw = null;
        try {
            fr = new FileReader(source);
            fw = new FileWriter(destination);
            int c = fr.read();
            while(c!=-1) {
                fw.write(c);
                c = fr.read();
            }
        } 
        catch(IOException e) {
            e.printStackTrace();
        } 
        finally {
            try {
                if (fr != null) {
                    fr.close();
                }
            } 
            catch(IOException e) {
            }
            try {
                if (fw != null) {
                    fw.close();
                }
            } 
            catch(IOException e) {
            }
        }
    }

    protected String getCancelJobScript(final DrmJobRecord jobRecord) {
        final File cancelScript=getAwsBatchScriptFile(jobRecord, PROP_CANCEL_SCRIPT, DEFAULT_CANCEL_SCRIPT);
        return ""+cancelScript;
    }

    @Override
    public boolean cancelJob(DrmJobRecord jobRecord) throws Exception {
        log.debug("cancelJob, gpJobNo="+jobRecord.getGpJobNo());
        final String awsId=jobRecord.getExtJobId();

        if (awsId != null){
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DefaultExecutor exec=new DefaultExecutor();
            exec.setStreamHandler( new PumpStreamHandler(outputStream));
            final String cancelJobScript=getCancelJobScript(jobRecord);
            CommandLine cl= new CommandLine(cancelJobScript);
            // tasklib
            cl.addArgument(awsId);
            try {
                final Map<String,String> cmdEnv=initAwsCmdEnv(jobRecord);
                exec.execute(cl, cmdEnv);
                if (log.isDebugEnabled()) { 
                    String output =  outputStream.toString().trim();
                    log.debug("output: "+output);
                }
            } 
            catch (Exception e){
                log.error(e);
            }
        }
        updateStatus_cancel(jobRecord.getExtJobId());
        return true;
    }
    
    /**
     * Resolve the path to the named aws batch script. Paths are resolved
     * relative to the 'aws-batch-script-dir'. 
     * Default: ('aws-batch-script-dir' not set)
     *   <wrapper-scripts>/aws_batch/{scriptName}
     * Custom ('aws-batch-script-dir' is a fully qualified path)
     *   <aws-batch-script-dir>/{scriptName}
     * Custom ('aws-batch-script-dir' is a relative path)
     *   <wrapper-scripts>/<aws-batch-script-dir>/{scriptName}
     */
    protected static File getAwsBatchScriptFile(final GpConfig gpConfig, final GpContext jobContext, final String scriptName) {
        File file = new File(scriptName);
        if (file.isAbsolute()) {
            return file;
        }
        
        // special-case: when 'scriptName' is a relative path
        final Value dirname = gpConfig.getValue(jobContext, PROP_AWS_BATCH_SCRIPT_DIR, DEFAULT_AWS_BATCH_SCRIPT_DIR);
        file = new File(dirname.getValue(), scriptName);
        if (file.isAbsolute()) {
            return file;
        }
        
        // special-case: when 'aws-batch-script-dir' is a relative path
        final File wrapperScripts=gpConfig.getGPFileProperty(jobContext, GpConfig.PROP_WRAPPER_SCRIPTS_DIR);
        file = new File(wrapperScripts, file.getPath());
        return file;
    }

    protected static File getAwsBatchScriptFile(final GpConfig gpConfig, final GpContext jobContext, final String key, final Value defaultValue) {
        final Value filename = gpConfig.getValue(jobContext, key, defaultValue);
        return getAwsBatchScriptFile(gpConfig, jobContext, filename.getValue());
    }
    
    protected static File getAwsBatchScriptFile(final DrmJobSubmission gpJob, final String key, final Value defaultValue) {
        if (gpJob != null) {
            return getAwsBatchScriptFile(gpJob.getGpConfig(), gpJob.getJobContext(), key, defaultValue);
        }
        return getAwsBatchScriptFile(ServerConfigurationFactory.instance(), (GpContext) null, key, defaultValue);
    }


    protected static File getAwsBatchScriptFile(final DrmJobRecord jobRecord, final String key, final Value defaultValue) {
        final GpContext jobContext=AwsBatchUtil.initJobContext(jobRecord);
        return getAwsBatchScriptFile(ServerConfigurationFactory.instance(), jobContext, key, defaultValue);
    }
    
    protected static String getAwsJobName(final DrmJobSubmission gpJob) {
        final String prefix=AwsBatchUtil.getProperty(gpJob, "aws-job-name-prefix", "GP_Job_");
        return prefix + gpJob.getGpJobNo();
    }

    /**
     * First pass the arguments needed by the AWS launch script, then follow
     * with the normal GenePattern command line
     */
    protected static CommandLine initAwsBatchScript(final DrmJobSubmission gpJob, final File inputDir, final Set<File> inputFiles, final Map<String, String> inputFileMap) throws CommandExecutorException {
        if (gpJob == null) {
            throw new IllegalArgumentException("gpJob==null");
        }
        if (gpJob.getCommandLine() == null) {
            throw new IllegalArgumentException("gpJob.commandLine==null");
        }
        if (gpJob.getCommandLine().size() == 0) {
            throw new IllegalArgumentException("gpJob.commandLine.size==0");
        }
        
        final File awsBatchScript=getAwsBatchScriptFile(gpJob, PROP_AWS_BATCH_SCRIPT, DEFAULT_AWS_BATCH_SCRIPT);
        if (log.isDebugEnabled()) {
            log.debug("job "+gpJob.getGpJobNo());
            log.debug("           aws-batch-script='"+awsBatchScript+"'");
            log.debug("    aws-batch-script.fqPath='"+awsBatchScript.getAbsolutePath()+"'");
        }
        
        final String awsBatchJobDefinition; // default
        final Value jobDefValue = gpJob.getValue(PROP_JOB_AWSBATCH_JOB_DEF);
        if (jobDefValue != null) {
            awsBatchJobDefinition = jobDefValue.getValue();
        }
        else {
            throw new IllegalArgumentException("module: " + gpJob.getJobInfo().getTaskName() + " needs a "+PROP_JOB_AWSBATCH_JOB_DEF+" defined in the custom.yaml");
        }
        
        final CommandLine cl = new CommandLine(awsBatchScript.getAbsolutePath());

        // tasklib
        cl.addArgument(gpJob.getTaskLibDir().getAbsolutePath(), handleQuoting);

        // workdir
        cl.addArgument(gpJob.getWorkingDir().getAbsolutePath(), handleQuoting);

        // aws batch job definition name
        cl.addArgument(awsBatchJobDefinition, handleQuoting);

        // job name to have in AWS batch displays
        final String awsJobName=getAwsJobName(gpJob);
        cl.addArgument(awsJobName, handleQuoting);

        // handle input files
        cl.addArgument(inputDir.getAbsolutePath(), handleQuoting); 
        // substitute input file paths, replace original with linked file paths
        //final List<String> cmdLine=substituteInputFilePaths(gpJob.getCommandLine(), inputFileMap, inputFiles);
        
        final List<String> cmdLine=gpJob.getCommandLine();
        for(final String arg : cmdLine) {
            cl.addArgument(arg, handleQuoting);
        }
        return cl;
    }

    protected void copyInputFiles(final String awsCmd, final Map<String,String> cmdEnv, final Set<File> inputFiles, final String s3_root, final AwsS3Filter awsS3Filter) {
        final AwsS3Cmd s3Cmd=new AwsS3Cmd.Builder()
            .awsCmd(awsCmd)
            .awsCliEnv(cmdEnv)
            .s3_bucket(s3_root)
        .build();
        
        // optionally skip the push to s3 step
        for(final File inputFile : inputFiles) {
            if (awsS3Filter.skipS3Upload(inputFile)) {
                log.debug("skipping s3 push: "+inputFile);
            }
            else {
                s3Cmd.syncToS3(inputFile);
            }
        }
        
        // create 'aws-sync-from-s3.sh' script in the metadataDir
        final String script_name="aws-sync-from-s3.sh";
        final File script_dir=s3Cmd.getMetadataDir();
        if (!script_dir.exists()) {
            if (log.isDebugEnabled()) {
                log.debug("mkdirs script_dir="+script_dir);
            }
            boolean success=script_dir.mkdirs();
            if (!success) {
                log.error("failed to mkdirs for script_dir="+script_dir);
            }
        }

        final File script=new File(script_dir, script_name);
        try {
            boolean success=script.createNewFile();
            if (!success) {
                log.error("createNewFile failed, script="+script);
                return;
            }
        }
        catch (IOException e) {
            log.error("Error in createNewFile, script="+script, e);
            return;
        }
        
        try (final BufferedWriter bw = new BufferedWriter(new FileWriter(script))) {
            bw.write("#!/usr/bin/env bash"); bw.newLine();
            for(final File inputFile : inputFiles) {
                // Template
                //   aws s3 sync {s3_bucket}{localDir} {localDir} --exclude "*" --include "{filename}"  
                // Example
                //   aws s3 sync s3://gpbeta/temp /temp --exclude "*" --include "test.txt"
                final List<String> args=s3Cmd.getSyncFromS3Args(inputFile);
                bw.write("aws \\"); bw.newLine();
                for(int i=0; i<args.size(); ++i) {
                    bw.write("    \""+args.get(i)+"\" ");
                    bw.write(" \\");
                    bw.newLine();
                }
                bw.write(" >> "+script_dir+"/s3_downloads.log");
                bw.newLine();
                bw.newLine();
                
                if (inputFile.isFile() && inputFile.canExecute()) {
                    bw.write("chmod u+x \""+inputFile.getPath()+"\"");
                    bw.newLine();
                    bw.newLine();
                }
            }
        }
        catch (IOException e) {
            log.error("Error writing to script="+script, e);
        }
        boolean success=script.setExecutable(true);
        if (!success) {
            log.error("failed to set exec flag for script="+script);
        }
    }

    private static List<String> _substituteInputFilePaths(final List<String> cmdLineIn, final Map<String,String> inputFileMap, final Set<File> inputFiles) {
        final List<String> cmdLineOut=new ArrayList<String>(cmdLineIn.size());
        for (int i = 0; i < cmdLineIn.size(); ++i) { 
            final String arg = _substituteCmdLineArg(cmdLineIn.get(i), inputFileMap, inputFiles);
            cmdLineOut.add(arg);
        } 
        return cmdLineOut;
    }

    /**
     * Substitute gp server local file path with docker container file path for the given command line arg
     * 
     * @param arg the command line arg
     * @param inputFileMap a lookup table mapping the original fq file to the linked fq file
     * @param inputFiles the list of all gp server local file paths to module input file
     * 
     * @return the command line arg to use in the container
     */
    private static String _substituteCmdLineArg(String arg, final Map<String, String> inputFileMap, final Set<File> inputFiles) {
        for (final File inputFile : inputFiles) { 
            final String inputFilepath = inputFile.getPath();
            final String linkedFilepath = inputFileMap.get(inputFilepath);
            if (Strings.isNullOrEmpty(linkedFilepath)) {
                log.error("Missing linkedFilepath in map, inputFilepath="+inputFilepath);
            }
            else {
                arg=AwsBatchUtil.replaceAll_quoted(arg, inputFilepath, linkedFilepath);
            }
        }
        return arg;
    }

    protected static String getLinkedStdinFile(final DrmJobSubmission gpJob, final Map<String, String> inputFileMap) {
        final File stdinLocal=gpJob.getRelativeFile(gpJob.getStdinFile());
        if (stdinLocal == null) {
            return null;
        }
        final String localFilepath = stdinLocal.getPath();
        final String linkedFilepath = inputFileMap.get(localFilepath);
        if (!Strings.isNullOrEmpty(linkedFilepath)) {
            return linkedFilepath;
        }
        else {
            return localFilepath;
        }
    }

    protected static Executor initExecutorForJob(final DrmJobSubmission gpJob, ByteArrayOutputStream outputStream) throws ExecutionException, IOException {
        File errfile = gpJob.getRelativeFile(gpJob.getStderrFile());
        final PumpStreamHandler pumpStreamHandler = new PumpStreamHandler( 
                    outputStream,
                new FileOutputStream(errfile));
        
        final ExecuteWatchdog watchDog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
        final ShutdownHookProcessDestroyer processDestroyer = new ShutdownHookProcessDestroyer();
        
        DefaultExecutor exec=new DefaultExecutor();
        exec.setWorkingDirectory(gpJob.getWorkingDir());
        exec.setStreamHandler( pumpStreamHandler );
        exec.setWatchdog(watchDog);
        exec.setProcessDestroyer(processDestroyer);
        return exec;
    }

    protected DrmJobStatus submitAwsBatchJob(final DrmJobSubmission gpJob) throws CommandExecutorException, ExecutionException, IOException {
        // Handle job input files before AWS Batch submission
        //   -- make symbolic links in the .inputs_for_{job_id} directory
        final File inputDir = new File(gpJob.getWorkingDir(), ".inputs_for_" + gpJob.getGpJobNo());
        inputDir.mkdir();
        
        // this is a test
        final File awsCli=getAwsBatchScriptFile(gpJob, PROP_AWS_CLI, DEFAULT_AWS_CLI);
        final String s3_root=gpJob.getProperty(PROP_AWS_S3_ROOT);
        final Set<File> inputFiles = AwsBatchUtil.getInputFiles(gpJob);
        final Map<String,String> cmdEnv=initAwsCmdEnv(gpJob);
        final AwsS3Filter awsS3Filter=AwsS3Filter.initAwsS3Filter(gpJob.getGpConfig(), gpJob.getJobContext());
        copyInputFiles(awsCli.getPath(), cmdEnv, inputFiles, s3_root, awsS3Filter);

        //final Map<String, String> inputFileMap=makeSymLinks(inputDir, inputFiles);
        final Map<String,String> inputFileMap=Collections.emptyMap();

        final CommandLine cl = initAwsBatchScript(gpJob, inputDir, inputFiles, inputFileMap);
        if (log.isDebugEnabled()) {
            log.debug("aws-batch-script-cmd='"+cl.getExecutable()+"'");
            for(final String arg : cl.getArguments()) {
                log.debug("     '"+arg+"'");
            }
        }

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final Executor exec=initExecutorForJob(gpJob, outputStream); 
        // set GP_STDIN_FILE
        final String linkedStdIn=getLinkedStdinFile(gpJob, inputFileMap);
        if (!Strings.isNullOrEmpty(linkedStdIn)) {
            cmdEnv.put("GP_STDIN_FILE", linkedStdIn);
        }
        exec.execute(cl, cmdEnv);
        String awsJobId =  outputStream.toString();
        
        return new DrmJobStatus.Builder(""+gpJob.getGpJobNo(), DrmJobState.QUEUED)
            .extJobId(awsJobId)
            .submitTime(new Date())
        .build();
    }
    
    /**
     * When 'job.logFile' is set, write the command line into a log file in the working directory for the job.
     * <pre>
           # the name of the log file, relative to the working directory for the job
           job.logfile: .rte.out
     * </pre>
     */
    protected void logCommandLine(final DrmJobSubmission job) {
        // a null 'job.logFile' means "don't write the log file"
        if (job==null || job.getLogFile()==null) {
            return;
        }
        File commandLogFile=job.getLogFile();
        if (!commandLogFile.isAbsolute()) {
            commandLogFile=new File(job.getWorkingDir(), commandLogFile.getPath());
        }
        final List<String> args=job.getCommandLine();
        log.debug("saving command line to log file ...");
        String commandLineStr = "";
        boolean first = true;
        for(final String arg : args) {
            if (first) {
                commandLineStr = arg;
                first = false;
            }
            else {
                commandLineStr += (" "+arg);
            }
        }

        if (commandLogFile.exists()) {
            log.error("log file already exists: "+commandLogFile.getAbsolutePath());
            return;
        }

        final boolean append=true;
        BufferedWriter bw = null;
        try {
            FileWriter fw = new FileWriter(commandLogFile, append);
            bw = new BufferedWriter(fw);
            bw.write("job command line: ");
            bw.write(commandLineStr);
            bw.newLine();
            int i=0;
            for(final String arg : args) {
                bw.write("    arg["+i+"]: '"+arg+"'");
                bw.newLine();
                ++i;
            }
            bw.close();
        }
        catch (IOException e) {
            log.error("error writing log file: "+commandLogFile.getAbsolutePath(), e);
            return;
        }
        catch (Throwable t) {
            log.error("error writing log file: "+commandLogFile.getAbsolutePath(), t);
            log.error(t);
        }
        finally {
            if (bw != null) {
                try {
                    bw.close();
                }
                catch (IOException e) {
                    log.error(e);
                }
            }
        }
    }

}
