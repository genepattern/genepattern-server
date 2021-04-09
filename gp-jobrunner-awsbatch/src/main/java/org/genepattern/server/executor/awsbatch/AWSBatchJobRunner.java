package org.genepattern.server.executor.awsbatch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletResponse;

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
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.dm.ExternalFileManager;
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
                aws-batch-script: "runOnBatch-v0.4.sh" 
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
    public static final Value DEFAULT_AWS_BATCH_SCRIPT=new Value("runOnBatch-v0.4.sh");
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
        if (log.isTraceEnabled()) {
            log.trace("getStatus for jobRecord.gpJobNo="+jobRecord.getGpJobNo());
            log.trace("    jobRecord.extJobId="+jobRecord.getExtJobId());
        }
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
                String output = outputStream.toString().trim();
                
                if (log.isDebugEnabled()) {
                    log.debug("getStatus call is: " + checkStatusScript + " " + awsId);
                    log.debug("getStatus response: "+output);
                }
                
                
                final JSONObject jobJSON = new JSONObject(output);
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

                final Date startTime=getOrDefaultDate(awsJob, "startedAt", null);
                final Date submitTime=getOrDefaultDate(awsJob, "createdAt", null);
                
                final DrmJobStatus.Builder b=new DrmJobStatus.Builder().extJobId(awsId);
                final DrmJobState jobState=getOrDefault(batchToGPStatusMap, awsStatusCode, DrmJobState.UNDETERMINED);
                log.debug("DRM status: "+jobState.toString());
                log.debug("state map: " + batchToGPStatusMap);
                b.jobState(jobState);
                if (awsJob.has("jobQueue")) {
                    b.queueId(awsJob.getString("jobQueue"));
                }
                b.startTime( startTime  );
                b.submitTime( submitTime );
                if (jobState.is(DrmJobState.TERMINATED)) {
                    // job cleanup, TERMINATED covers SUCCEEDED and FAILED
                    if (log.isDebugEnabled()) {
                        log.debug("job finished with awsStatusCode="+awsStatusCode+", drmJobState="+jobState);
                        log.debug("    jobs[0].statusReason: "+awsStatusReason);
                    }
                    final Date endTime=getOrDefaultDate(awsJob, "stoppedAt", new Date());
                    b.endTime( endTime );
                    final File metadataDir=getMetadataDir(jobRecord);
                    try {
                        refreshWorkingDirFromS3(jobRecord, metadataDir, cmdEnv);
                        log.debug("A");
                    } 
                    catch (Throwable t) {
                        log.debug("ERROR A");
                        log.error("Error copying output files from s3 for job="+jobRecord.getGpJobNo(), t);
                    }
                    final Integer exitCode=getExitCodeFromMetadataDir(metadataDir);
                    if (exitCode != null) {
                        log.debug("B");
                        b.exitCode(exitCode);
                        // special-case: custom timeout
                        if (exitCode==142) {
                            b.jobState(DrmJobState.TERM_RUNLIMIT);
                        }
                    }
                    else if (containerExitCode >= 0) {
                        log.debug("C");
                        // special-case: aws batch timeout
                        //   awsStatusCode: FAILED
                        //   awsStatusReason: Job attempt duration exceeded timeout
                        //   containerExitCode: 137
                        b.exitCode(containerExitCode);
                        if (awsStatusReason != null) {
                            b.jobStatusMessage(awsStatusReason);
                        }
                        getAdditionalErrorLogs(jobRecord, metadataDir);
                    }
                    else {
                        log.debug("D");
                        log.error("Error getting exitCode for job="+jobRecord.getGpJobNo());
                        if (awsStatusReason != null) {
                            b.jobStatusMessage(awsStatusReason);
                            getAdditionalErrorLogs(jobRecord, metadataDir);
                        }
                    }
                }
                log.debug("E");
                return b.build();
            } 
            catch (Throwable t) {
                log.debug("F");
                log.error(t);
            }
            log.debug("G");
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
        final String aws_profile=gpConfig.getGPProperty(jobContext, PROP_AWS_PROFILE);
        if (aws_profile != null) {
            cmdEnv.put("AWS_PROFILE", aws_profile);
        }
        final String s3_root=gpConfig.getGPProperty(jobContext, PROP_AWS_S3_ROOT);
        if (s3_root != null) {
            cmdEnv.put("S3_ROOT", s3_root);
            cmdEnv.put("AWS_S3_PREFIX", s3_root);
        } 
        final String dest_prefix=gpConfig.getGPProperty(jobContext, "job.awsbatch.dest-prefix", "");
        if (dest_prefix != null) {
            cmdEnv.put("GP_LOCAL_PREFIX", dest_prefix);
        }
        return cmdEnv; 
    }

    /**
     * Get environment variables for the aws batch submit-job command line.
     * @return a Map<String,String> of environment variables
     */
    protected final Map<String,String> initAwsCmdEnv(final DrmJobSubmission gpJob, final Set<File> inputParentDirs) {
        final Map<String,String> cmdEnv=initAwsCliEnv(gpJob.getGpConfig(), gpJob.getJobContext());

        cmdEnv.put("GP_JOB_ID", ""+gpJob.getGpJobNo());
        cmdEnv.put("GP_USER_ID", ""+ gpJob.getJobContext().getUserId());
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
        } else if (inputParentDirs != null ){
            // ALTERNATE BIND - bind to parent dirs of any inputs only and not higher level root dirs
            // to make it harder to have a malicious container see things it ought not to
            // but exempt the jobdir itself which causes a error for binding the same mount twice
            List<String> inputParentDirList = new ArrayList<String>();
            String parent = gpJob.getWorkingDir().getAbsolutePath();
            for (File f: inputParentDirs){
                String dirPath = f.getAbsolutePath();
                if (!parent.equals(dirPath)) inputParentDirList.add(dirPath);
            }
            inputParentDirList.add(gpJob.getWorkingDir().getAbsolutePath());
            cmdEnv.put(
                    "GP_JOB_DOCKER_BIND_MOUNTS",
                    Joiner.on(":").skipNulls().join(inputParentDirList)
                );
        }

        final String dockerImage=gpJob.getGpConfig().getJobDockerImage(gpJob.getJobContext());
        if (log.isDebugEnabled()) {
            log.debug("+"+PROP_DOCKER_IMAGE+"'="+dockerImage);
        }
        if (Strings.isNullOrEmpty(dockerImage)) {
            log.warn("'"+PROP_DOCKER_IMAGE+"' not set");
        }
        else {
            cmdEnv.put("GP_JOB_DOCKER_IMAGE", dockerImage);
        }
        
        // switch to turn off execution of module containers that run as root within the container
        final boolean rootContainerNotAllowed=gpJob.getGpConfig().getGPBooleanProperty(gpJob.getJobContext(), "dockerCantRunContainersAsRoot", false);
        cmdEnv.put("GP_CONTAINER_CANT_RUN_AS_ROOT", ""+rootContainerNotAllowed);
        
        final boolean readonlyBindMounts=gpJob.getGpConfig().getGPBooleanProperty(gpJob.getJobContext(), "dockerReadOnlyBindMounts", false);
        cmdEnv.put("GP_READONLY_BIND_MOUNTS", ""+readonlyBindMounts);
 
        
        // set GP_AWS_SYNC_SCRIPT_NAME, default=aws-sync-from-s3.sh
        final String syncFromScriptName=gpJob.getGpConfig().
            getGPProperty(gpJob.getJobContext(), "job.awsbatch.sync-from-s3-script", "aws-sync-from-s3.sh");
        if (Strings.isNullOrEmpty(syncFromScriptName)) {
            log.warn("job.awsbatch.sync-from-s3-script not set");
        }
        cmdEnv.put("GP_AWS_SYNC_SCRIPT_NAME", syncFromScriptName);

        final String jobQueue=gpJob.getQueue();
        if (Strings.isNullOrEmpty(jobQueue)) {
            log.warn("job.queue not set");
        }
        else {
            cmdEnv.put("GP_JOB_QUEUE", jobQueue);
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
    
    
    private String getBucketName(final GpConfig gpConfig, GpContext userContext) {
        String aws_s3_root = gpConfig.getGPProperty(userContext, "aws-s3-root");
        // pull the bucket name out of something like "s3://moduleiotest/gp-dev-ami"
        int endIdx = aws_s3_root.indexOf("/", 5);
        return aws_s3_root.substring(5,endIdx);
    }
    private String getBucketRoot(final GpConfig gpConfig, GpContext userContext) {
        String aws_s3_root = gpConfig.getGPProperty(userContext, "aws-s3-root");
        // pull the bucket root path out of something like "s3://moduleiotest/gp-dev-ami"
        int endIdx = aws_s3_root.indexOf("/", 5);
        return aws_s3_root.substring(endIdx+1);
    }
    
    
   
    
    /**
     * When we want to leave the files on S3 instead of copying them to local, we still need to fake out
     * GenePattern into seeing the files.  So first attempt we will create 0-length files 
     * @param jobRecord
     * @param filepath
     * @param cmdEnv
     */
    protected void awsFakeSyncDirectory(final DrmJobRecord jobRecord, final File filepath, final Map<String,String> cmdEnv) {
        // call out to a script to refresh the directory path to pull files from S3
        final GpContext jobContext=AwsBatchUtil.initJobContext(jobRecord);
        GpConfig gpConfig =  ServerConfigurationFactory.instance();
        
        String awsfilepath = gpConfig.getGPProperty(jobContext,"aws-batch-script-dir");
        String awsfilename = gpConfig.getGPProperty(jobContext, AWSBatchJobRunner.PROP_AWS_CLI, "aws-cli.sh");
        String bucket = getBucketName(gpConfig, jobContext);
        String bucketRoot = getBucketRoot(gpConfig, jobContext);
        
        String execArgs[];
        String s3filepath = filepath.getAbsolutePath();
        // make sure it ends with a slash to avoid collecting the meta dir as well
        if (!s3filepath.endsWith("/")) s3filepath += "/";
        
        execArgs = new String[] {awsfilepath+awsfilename, "s3", "ls", bucket+ "/"+bucketRoot+s3filepath, "--recursive"};
        
        // check for URLs that were fetched on the compute node but are actually input files
        File downloadListingFile = new File(jobRecord.getWorkingDir(),ExternalFileManager.downloadListingFileName);
        HashSet<String> inputUrlDownloadsToIgnore = new HashSet<String>();
        if (downloadListingFile.exists()){
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(downloadListingFile));
                String line = null;
                while (( line = reader.readLine())!= null){
                    // expect uri \t filenameandpath
                    String[] lineParts = line.split("\t");
                    inputUrlDownloadsToIgnore.add(lineParts[1]);
                }                     
            } catch (Exception ioe){
                log.error(ioe);
            } finally {
                try {
                    if (reader != null) reader.close(); 
                } catch (Exception ee){}
            }
        }
        try {
            String envp[] = new String[cmdEnv.size()];
            int i=0;
            for (String key: cmdEnv.keySet()){
                String val = cmdEnv.get(key);
                envp[i++] = key + "="+val;
            }
            Process proc = Runtime.getRuntime().exec(execArgs, envp);
            
            proc.waitFor();
        
            BufferedReader stdInput = new BufferedReader(new     InputStreamReader(proc.getInputStream()));
            BufferedReader stdError = new BufferedReader(new  InputStreamReader(proc.getErrorStream()));
    
           // Read the output from the command
           String s = null;
           
           // each line will look like 
           //         2020-12-01 13:19:01    4931101 tedslaptop/Users/liefeld/gp/users/739701.jpg
           // we want to get the filenames, sizes dates and times and stick them into a json file
           // that GenePatternAnalysisTask will find and then use to register the (non-local) output files
           //  .non.retrieved.output.files.json
           JSONArray outFilesJSON = new JSONArray();
           while ((s = stdInput.readLine()) != null) {
               System.out.println(s);
               String[] lineParts = s.split("\\s+",4); // only grab the first 4 bits, any other spaces are part of the filename
               // strip out the bucketroot which is not wanted for the local file
               try {
                   String outFilePath = lineParts[3].substring(bucketRoot.length());
                   // date time size <filepath>/filename
                   //File f = new File(outFilePath);
                   //f.createNewFile();
                   
                   // skip files that were downloaded on the compute node as inputs
                   if (inputUrlDownloadsToIgnore.contains(outFilePath)) continue;
                   JSONObject oneFileJSON = new JSONObject();
                   oneFileJSON.put("filename", outFilePath);
                   oneFileJSON.put("size", lineParts[2]);
                   oneFileJSON.put("date", lineParts[0]);
                   oneFileJSON.put("time", lineParts[1]);
                   
                   outFilesJSON.put(oneFileJSON);
                   
               } catch (Exception e){
                   // ignore
               }
           }
           File outListFile = new File(s3filepath+ExternalFileManager.nonRetrievedFilesFileName);
           BufferedWriter writer = new BufferedWriter(new FileWriter (outListFile));
           writer.append(outFilesJSON.toString());
           writer.close();
           
           // Read any errors from the attempted command
           if (log.isDebugEnabled()){
               log.debug("Here is the standard error of retrieving details of files left on S3 (if any):\n");
               while ((s = stdError.readLine()) != null) {
                   log.debug(s);
               }     
           }
        } catch (Exception e){
            e.printStackTrace();
            
        }
    }
    

    private void refreshWorkingDirFromS3(final DrmJobRecord jobRecord, final File metadataDir, final Map<String, String> cmdEnv) {
        // pull the job.metaDir from s3
        awsSyncDirectory(jobRecord, metadataDir, cmdEnv);
        
        // pull the job.workingDir from s3
        // unless an external downloader has been configured in which case we can skip this and let
        // them be directly downloaded from S3
        if (isSkipWorkingDirDownload(jobRecord)) {
            awsFakeSyncDirectory(jobRecord, jobRecord.getWorkingDir(), cmdEnv);
        } else {
            awsSyncDirectory(jobRecord, jobRecord.getWorkingDir(), cmdEnv);
        }
        
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

    // in the case of a AWS error, there may also be a dockererr.log file in the metadata dir
    // we will copy it to the working dir so that it can be seen
    private void  getAdditionalErrorLogs(final DrmJobRecord jobRecord, final File  metadataDir){
        File dockerErr = new File(metadataDir, "dockererr.log");
        System.out.println("Looking for dockererr.log =============");
        if (dockerErr.exists()){
            System.out.println("moved dockererr.log to =============" + jobRecord.getWorkingDir().getAbsolutePath());
            File visibleDockerErr = new File(jobRecord.getWorkingDir(), "dockererr.log");
            boolean moved = dockerErr.renameTo(visibleDockerErr);
            System.out.println("Moved");
        } else {
            System.out.println("Did NOT find dockererr.log =============" + dockerErr.getAbsolutePath());  
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
    
    protected static boolean isSkipWorkingDirDownload(final DrmJobRecord jobRecord) {
        final GpContext jobContext=AwsBatchUtil.initJobContext(jobRecord);
        GpConfig jobConfig = ServerConfigurationFactory.instance();
        final boolean directDownloadEnabled_obsolete = (jobConfig.getGPProperty(jobContext, "download.aws.s3.downloader.class", null) != null);
        final boolean directDownloadEnabled = (jobConfig.getGPProperty(jobContext, ExternalFileManager.classPropertyKey, null) != null);
      
        return (directDownloadEnabled_obsolete || directDownloadEnabled);
    }
    
    protected static boolean isSkipWorkingDirDownload(final DrmJobSubmission jobSubmission) {
        final GpContext jobContext=AwsBatchUtil.initJobContext(jobSubmission);
        GpConfig jobConfig = ServerConfigurationFactory.instance();
        String downloaderClass_obsolete = jobConfig.getGPProperty(jobContext, "download.aws.s3.downloader.class", null);
        String downloaderClass = jobConfig.getGPProperty(jobContext, ExternalFileManager.classPropertyKey, null);
        
        return ((downloaderClass != null) || (downloaderClass_obsolete != null));
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
        final HibernateSessionManager mgr = org.genepattern.server.database.HibernateUtil.instance();
        
        final File awsBatchScript=getAwsBatchScriptFile(gpJob, PROP_AWS_BATCH_SCRIPT, DEFAULT_AWS_BATCH_SCRIPT);
        //if (log.isDebugEnabled()) {  // JTL XXX 02/102020
        {
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

    protected void copyInputFiles(final String awsCmd, final Map<String,String> cmdEnv, final Set<File> inputFiles, final String s3_root, final AwsS3Filter awsS3Filter, final File jobDir, String userId) {
        final AwsS3Cmd s3Cmd=new AwsS3Cmd.Builder()
            .awsCmd(awsCmd)
            .awsCliEnv(cmdEnv)
            .s3_bucket(s3_root)
        .build();
        
        // optionally skip the push to s3 step
        for(final File inputFile : inputFiles) {
            if (awsS3Filter.skipS3Upload(inputFile, s3Cmd)) {
                log.debug("skipping s3 push: "+inputFile);
            }
            else {
                s3Cmd.syncToS3(inputFile);
            }
        }
        
        // create 'aws-sync-from-s3.sh' script in the metadataDir
        final String script_name=getOrDefault(cmdEnv,"GP_AWS_SYNC_SCRIPT_NAME", "aws-sync-from-s3.sh");
        final String dest_prefix=getOrDefault(cmdEnv, "GP_LOCAL_PREFIX", "");
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
                log.error("              failed, exits ="+script.exists());
                return;
            }
        }
        catch (IOException e) {
            log.error("Error in createNewFile, script="+script, e);
            return;
        }
        
        ArrayList<String[]> urlsToFetch = new ArrayList<String[]>();
        File downloadListingFile = new File(jobDir,ExternalFileManager.downloadListingFileName);
        if (downloadListingFile.exists()){
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(downloadListingFile));
                String line = null;
                while (( line = reader.readLine())!= null){
                    // expect uri \t filenameandpath
                    String[] lineParts = line.split("\t");
                    urlsToFetch.add(lineParts);
                }
                
                
            } catch (Exception ioe){
                log.error(ioe);
            } finally {
                try {
                    if (reader != null) reader.close(); 
                } catch (Exception ee){}
            }
        }
        
        
        
        try (final BufferedWriter bw = new BufferedWriter(new FileWriter(script))) {
            bw.write("#!/usr/bin/env bash"); bw.newLine();
            for(final File inputFile : inputFiles) {
                final List<String> args=s3Cmd.getSyncFromS3Args(inputFile, dest_prefix, userId);
                bw.write("aws \\"); bw.newLine();
                for(int i=0; i<args.size(); ++i) {
                    bw.write("    \""+args.get(i)+"\" ");
                    bw.write(" \\");
                    bw.newLine();
                }
                bw.write(" >> "+dest_prefix+""+script_dir+"/s3_downloads.log");
                bw.newLine();
                bw.newLine();
                
                if (inputFile.isFile() && inputFile.canExecute()) {
                    bw.write("chmod u+x \""+Strings.nullToEmpty(dest_prefix)+""+inputFile.getPath()+"\"");
                    bw.newLine();
                    bw.newLine();
                }
            }
            for (String[] urlfile: urlsToFetch){
                bw.write("wget -O ");
                bw.write(dest_prefix);
                bw.write(urlfile[1]);
                bw.write("  ");
                bw.write(urlfile[0]);
                bw.write(" >> "+dest_prefix+""+script_dir+"/http_downloads.log");
                bw.newLine();
                bw.newLine();
                // better safe than sorry
                bw.write("chmod u+rw \""+Strings.nullToEmpty(dest_prefix)+""+urlfile[1]+"\"");
                bw.newLine();
                bw.newLine();
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
        
        final Set<File> inputDirectories = AwsBatchUtil.getInputFileParentDirectories(gpJob);
        
        final Map<String,String> cmdEnv=initAwsCmdEnv(gpJob, inputDirectories);
        String userId = gpJob.getJobContext().getUserId();
        final AwsS3Filter awsS3Filter=AwsS3Filter.initAwsS3Filter(gpJob.getGpConfig(), gpJob.getJobContext());
        copyInputFiles(awsCli.getPath(), cmdEnv, inputFiles, s3_root, awsS3Filter, gpJob.getWorkingDir(), userId);

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
