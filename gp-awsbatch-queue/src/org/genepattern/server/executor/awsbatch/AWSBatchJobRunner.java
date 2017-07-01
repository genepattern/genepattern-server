package org.genepattern.server.executor.awsbatch;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.genepattern.server.config.Value;
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.executor.CommandProperties;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamId;
import org.genepattern.server.job.input.ParamValue;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.json.JSONObject;


public class AWSBatchJobRunner implements JobRunner{
    private static final Logger log = Logger.getLogger(AWSBatchJobRunner.class);

    /** generic implementation of getOrDefault, for use in Java 1.7 */
    public static final <K,V> V getOrDefault(final Map<K,V> map, K key, V defaultValue) {
        V value=map.get(key);
        if (value != null) {
            return value;
        }
        return defaultValue;
    }

    private String defaultLogFile=null;
    
    private static  Map<String, DrmJobState> batchToGPStatusMap = new HashMap<String, DrmJobState>();
    
    private static String checkStatusScript;
    private static String submitJobScript;
    private static String synchWorkingDirScript;
    private static String cancelJobScript;
    
    public void setCommandProperties(CommandProperties properties) {
        log.debug("setCommandProperties");
        if (properties==null) {
            log.debug("commandProperties==null");
            return;
        } 
        submitJobScript =  properties.getProperty("submitJobScript");
        checkStatusScript = properties.getProperty("checkStatusScript");
        synchWorkingDirScript = properties.getProperty("synchWorkingDirScript");
        cancelJobScript = properties.getProperty("cancelJobScript");
        if ((submitJobScript == null) | (checkStatusScript == null) | (synchWorkingDirScript == null) | (cancelJobScript == null)) {
            throw new RuntimeException("Need the yaml config file to specify submitJobScript, checkStatusScript, synchWorkingDirScript, cancelJobScript in the AWSBatchJobRunner configuration.properties");
        } 
        defaultLogFile=properties.getProperty(JobRunner.PROP_LOGFILE);
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
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DefaultExecutor exec=new DefaultExecutor();
            exec.setStreamHandler( new PumpStreamHandler(outputStream));
            CommandLine cl= new CommandLine(AWSBatchJobRunner.checkStatusScript);
            // tasklib
            cl.addArgument(awsId);
            final Map<String,String> cmdEnv=null;
            try {
                exec.execute(cl, cmdEnv);
          
                String awsStatus =  outputStream.toString().trim();
                JSONObject jobJSON = new JSONObject(awsStatus);
                JSONObject awsJob =  ((JSONObject)jobJSON.getJSONArray("jobs").get(0));
                String awsStatusCode = awsJob.getString("status");
                
                DrmJobStatus.Builder b=new DrmJobStatus.Builder().extJobId(awsId);
                DrmJobState jobState=getOrDefault(batchToGPStatusMap, awsStatusCode, DrmJobState.UNDETERMINED);
                b.jobState(jobState);
                if (awsStatusCode.equalsIgnoreCase("SUCCEEDED")) {
                    // TODO get the CPU time etc from AWS.  Will need to change the check status to return the full
                    // JSON instead of just the ID and then read it into a JSON obj from which we pull the status
                    // and sometimes the other details
                    if (log.isDebugEnabled()) {
                        log.debug("Done");
                    }
                    try {
                        b.queueId(awsJob.getString("jobQueue"));
                        b.startTime(new Date(awsJob.getLong("startedAt")));
                        b.submitTime(new Date(awsJob.getLong("createdAt")));
                        b.endTime(new Date(awsJob.getLong("stoppedAt")));
                    } 
                    catch (Throwable t) {
                        log.error(t);
                    }
                    
                    refreshWorkingDirFromS3(jobRecord);
                    try {
                        File metaDataDir = new File( jobRecord.getWorkingDir(), ".gp_metadata");
                        File exitCodeFile = new File(metaDataDir, "exit_code.txt");
                        byte[] encoded = Files.readAllBytes(Paths.get(exitCodeFile.getAbsolutePath()));
                        String jsonText = new String(encoded);
                        JSONObject json = new JSONObject(jsonText);  
                        b.exitCode(json.getInt("exit_code"));
                    } 
                    catch (Exception e) {
                        log.error(e);
                    }
                } 
                else if (awsStatusCode.equalsIgnoreCase("FAILED")) {
                    if (log.isDebugEnabled()) {
                        log.debug("Failed.");
                    }
                    try {
                        b.startTime(new Date(awsJob.getLong("startedAt")));
                    } 
                    catch (Exception e) {
                        // its not always present depending non how it failed
                    }
                    try {
                        b.submitTime(new Date(awsJob.getLong("createdAt")));
                    } 
                    catch (Exception e) {
                        // its not always present depending non how it failed
                    }
                    try {
                        b.endTime(new Date(awsJob.getLong("stoppedAt")));
                    } 
                    catch (Exception e){
                        // its not always present depending non how it failed}
                        // but this one we have a decent idea about
                        b.endTime(new Date());
                    } 
                    refreshWorkingDirFromS3(jobRecord);
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

    private void refreshWorkingDirFromS3(DrmJobRecord jobRecord){
        // call out to a script to refresh the directory path to pull files from S3
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        //
        // ask AWS for status and update since this is async
        // we'll always just get the old value for now
        //
        DefaultExecutor exec=new DefaultExecutor();
        exec.setStreamHandler( new PumpStreamHandler(outputStream));
        CommandLine cl= new CommandLine(AWSBatchJobRunner.synchWorkingDirScript);
        // tasklib
        cl.addArgument(jobRecord.getWorkingDir().getAbsolutePath());
        final Map<String,String> cmdEnv=null;
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

        // Now we have synch'd set the jobs stderr and stdout to the ones we got back from AWS
        // since I can't change the DRMJobSubmission objects pointers we'll copy the contents over for now
        File workDir = jobRecord.getWorkingDir();
        File gpMeta = new File(workDir, ".gp_metadata");
        if (gpMeta.isDirectory()){
            File stdErr = new File(gpMeta, "stderr.txt");
            if (stdErr.exists()) copyFileContents(stdErr, jobRecord.getStderrFile());
            File stdOut = new File(gpMeta, "stdout.txt");
            if (stdOut.exists()) copyFileContents(stdOut, jobRecord.getStdoutFile());
            
        } 
        else {    
            throw new RuntimeException("Did not get the .gp_metadata directory.  Something seriously went wrong with the AWS batch run");
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

    @Override
    public boolean cancelJob(DrmJobRecord jobRecord) throws Exception {
        log.debug("cancelJob, gpJobNo="+jobRecord.getGpJobNo());
        final String awsId=jobRecord.getExtJobId();

        if (awsId != null){
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DefaultExecutor exec=new DefaultExecutor();
            exec.setStreamHandler( new PumpStreamHandler(outputStream));
            CommandLine cl= new CommandLine(AWSBatchJobRunner.cancelJobScript);
            // tasklib
            cl.addArgument(awsId);
            try {
                exec.execute(cl);
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
    
    protected static Set<File> getInputFiles(final DrmJobSubmission gpJob) {
        if (log.isDebugEnabled()) {
            log.debug("listing input files for gpJobNo="+gpJob.getGpJobNo()+" ...");
        }
        // linked hash set preserves insertion order
        final Set<File> inputFiles = new LinkedHashSet<File>();
        for(final String localFilePath : gpJob.getJobContext().getLocalFilePaths()) {
            log.debug("    localFilePath="+localFilePath);
            final File file=new File(localFilePath);
            if (file.exists()) {
                inputFiles.add(file);
            }
            else {
                log.error("file doesn't exist, for gpJobNo="+gpJob.getGpJobNo()+", localFilePath="+localFilePath);
            }
        }
        return inputFiles;
    }

    //TODO: code cleanup, the following commented out part was an initial attempt which will work without the new
    //    gpContext#getLocalFilePaths added in gp-3.9.11-prerelease
    protected Set<File> debug_getInputFiles(final DrmJobSubmission gpJob,  HashMap<String,String> urlToFileMap) {
        final File gpBaseDataDir=getBaseDataDir(gpJob);
        final File userUploadDir=gpJob.getGpConfig().getUserUploadDir(gpJob.getJobContext());
        final String gpBaseHref = gpJob.getJobContext().getJobInput().getBaseGpHref();

        if (log.isDebugEnabled()) {
            log.debug("    baseGpHref="+gpBaseHref);
            log.debug("    baseGpDataDir="+gpBaseDataDir);
            log.debug("    userUploadDir="+userUploadDir);
            log.debug("    input params (provisional) ...");
        }
        
        final TaskInfo taskInfo=gpJob.getJobContext().getTaskInfo();
        final Map<String,ParameterInfoRecord> paramInfoMap;
        if (taskInfo != null) {
            paramInfoMap=ParameterInfoRecord.initParamInfoMap(taskInfo);
        }
        else {
            paramInfoMap=Collections.emptyMap();
        }
        for(final Entry<ParamId,Param> entry : gpJob.getJobContext().getJobInput().getParams().entrySet()) {
            final ParamId id=entry.getKey();
            final Param param=entry.getValue();
            final ParameterInfoRecord record = paramInfoMap.get(id.getFqName());
            final Boolean isInputFile;
            if (record != null) {
                isInputFile=record.getFormal().isInputFile();
            }
            else {
                isInputFile=null;
            }
            if (log.isDebugEnabled()) {
                log.debug("        "+id.getFqName()+", isInputFile="+isInputFile);
            }
            for(ParamValue paramValue : param.getValues()) {
                if (log.isDebugEnabled()) {
                    log.debug("          "+paramValue.getValue());
                }
            }
        }
        
        if (log.isDebugEnabled()) {
            log.debug("    input files (actual) ...");
        }
        // linked hash set preserves insertion order
        final Set<File> inputFiles = new LinkedHashSet<File>();
        ParameterInfo[] params = gpJob.getJobContext().getTaskInfo().getParameterInfoArray();
        try {
        for (int i=0; i < params.length; i++) {
            if (log.isDebugEnabled()) {
                log.debug("    p["+i+"]: "+params[i].getName());
            }
            if (params[i].getName().toLowerCase().endsWith(".filename")){
                List<ParamValue> vals = gpJob.getJobContext().getJobInput().getParamValues(params[i].getName());
                if (vals != null){
                    for (ParamValue val: vals){
                        String value = val.getValue();
                        if (log.isDebugEnabled()) {
                            log.debug("    "+params[i].getName()+"="+value);
                            log.debug("    value.startsWith(gpBaseHref): "+value.startsWith(gpBaseHref));
                        }
                       
                        if (value.startsWith(gpBaseHref)) {
                            String path = value.substring(gpBaseHref.length());
                            log.debug("    path initial: "+path);
                            path=path.replace("/tmp/", "/uploads/tmp/");
                            log.debug("    path modified: "+path);
                            //File fileMaybe = new File(gpBaseDataDir, path.replace("/tmp/", "/uploads/tmp/"));
                            File fileMaybe = new File(gpBaseDataDir, path);
                            log.debug("    fileMaybe="+fileMaybe);
                            if (fileMaybe.exists()){
                                log.debug("    fileMaybe exists");
                                urlToFileMap.put(value, fileMaybe.getAbsolutePath());
                                inputFiles.add(fileMaybe);
                                if (log.isDebugEnabled()) {
                                    log.debug("    added inputFile="+fileMaybe);
                                }
                            }
                         } else {
                            try {
                                File f = new File(value);
                                if (f.exists()) {
                                    inputFiles.add(f);
                                    if (log.isDebugEnabled()) {
                                        log.debug("    added inputFile="+f);
                                    } 
                                }
                            } catch(Exception e){
                                // swallow and ignore
                            }
                            
                        }
                    }
                }
            }
        }
        } catch (Exception e){
            e.printStackTrace();
        }
        return inputFiles;
    }
    
    private static File getBaseDataDir(final DrmJobSubmission gpJob) {
        File dataDir=gpJob.getGpConfig().getGpHomeDir();
        if (dataDir==null) {
            dataDir=new File(gpJob.getWorkingDir().getParentFile().getParent());
        }
        return dataDir;
    }

    protected static CommandLine initAwsBatchScript( DrmJobSubmission gpJob) {
        if (gpJob == null) {
            throw new IllegalArgumentException("gpJob==null");
        }
        List<String> gpCommand = gpJob.getCommandLine();
        if (gpCommand == null) {
            throw new IllegalArgumentException("gpJob.commandLine==null");
        }
        if (gpCommand.size() == 0) {
            throw new IllegalArgumentException("gpJob.commandLine.size==0");
        }

        /*
         * First pass the arguments needed by the AWS launch script, then follow
         * with the normal GenePattern command line
         */
        final Value value = gpJob.getValue("aws_batch_script");
        final String awsBatchScript;
        if (value != null) {
            awsBatchScript = value.getValue();
        }
        else {
            // default
            awsBatchScript = submitJobScript;
        }
        
        final String awsBatchJobDefinition; // default
        Value jobDefValue = gpJob.getValue("aws_batch_job_definition_name");
        if (jobDefValue != null) {
            awsBatchJobDefinition = jobDefValue.getValue();
        }
        else {
            throw new IllegalArgumentException("module: " + gpJob.getJobInfo().getTaskName() + " needs a aws_batch_job_definition_name defined in the custom.yaml");
        }

        final File wrapperScript = new File(awsBatchScript);
        final CommandLine cl = new CommandLine(wrapperScript.getAbsolutePath());

        // tasklib
        cl.addArgument(gpJob.getTaskLibDir().getAbsolutePath());

        // workdir
        cl.addArgument(gpJob.getWorkingDir().getAbsolutePath());

        // aws batch job definition name
        cl.addArgument(awsBatchJobDefinition);

        // job name to have in AWS batch displays
        cl.addArgument("GP_Job_" + gpJob.getGpJobNo());

        final File inputDir = new File(gpJob.getWorkingDir() , ".inputs_for_" + gpJob.getGpJobNo() );
        final Set<File> inputFiles = getInputFiles(gpJob);
        final Map<String,String> urlToFileMap = new HashMap<String,String>();
        try {
            //inputDir = new File(gpJob.getWorkingDir() , ".inputs_for_" + gpJob.getGpJobNo() );
            inputDir.mkdir();
            for (File inputFile : inputFiles) {
                DefaultExecutor exec=new DefaultExecutor();
                exec.setWorkingDirectory(inputDir);
                CommandLine link = new CommandLine("ln");
                link.addArgument("-s");
                link.addArgument(inputFile.getAbsolutePath());
                link.addArgument(inputFile.getName());
                File linkedFile = new File(inputDir,inputFile.getName() );
                urlToFileMap.put(inputFile.getName(), linkedFile.getAbsolutePath());
                exec.execute(link);
            }
            
        } 
        catch (Exception e) {
            log.error("Error creating symlink for input file", e);
            e.printStackTrace();
            throw new IllegalArgumentException("could not collect input files: "); 
        }
        
        final boolean handleQuoting = false;
        cl.addArgument(inputDir.getAbsolutePath() );       
        for (int i = 0; i < gpCommand.size(); ++i) {       
            String commandPart = gpCommand.get(i);        
            for (File inputFile : inputFiles) {       
                String filename = inputFile.getName();        
                if (commandPart.endsWith(filename)){      
                    commandPart = urlToFileMap.get(filename);     
                }
            }         
            cl.addArgument(commandPart, handleQuoting);       
        }
        return cl;
    }

    protected static Executor initExecutorForJob(final DrmJobSubmission gpJob, ByteArrayOutputStream outputStream) throws ExecutionException, IOException {
       // File outfile = gpJob.getRelativeFile(gpJob.getStdoutFile());
        File errfile = gpJob.getRelativeFile(gpJob.getStderrFile());
        File infile = gpJob.getRelativeFile(gpJob.getStdinFile());
        final PumpStreamHandler pumpStreamHandler;
        if (infile != null) {
            pumpStreamHandler = new PumpStreamHandler( 
                    outputStream,
                new FileOutputStream(errfile),
                new FileInputStream(infile));
        }
        else {
            pumpStreamHandler = new PumpStreamHandler( 
                    outputStream,
                new FileOutputStream(errfile));
        }
        
        final ExecuteWatchdog watchDog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
        final ShutdownHookProcessDestroyer processDestroyer = new ShutdownHookProcessDestroyer();
        
        DefaultExecutor exec=new DefaultExecutor();
        exec.setWorkingDirectory(gpJob.getWorkingDir());
        exec.setStreamHandler( pumpStreamHandler );
        exec.setWatchdog(watchDog);
        exec.setProcessDestroyer(processDestroyer);
        return exec;
    }

    protected DrmJobStatus submitAwsBatchJob(final DrmJobSubmission gpJob) throws ExecutionException, IOException {
        CommandLine cl = initAwsBatchScript(gpJob);
        if (log.isDebugEnabled()) {
            log.debug("aws_batch_script='"+cl.getExecutable()+"'");
            for(final String arg : cl.getArguments()) {
                log.debug("     '"+arg+"'");
            }
        }

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final Executor exec=initExecutorForJob(gpJob, outputStream);
        final Map<String,String> cmdEnv=null;
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
     * 
     * @author pcarr
     */
    protected void logCommandLine(final DrmJobSubmission job) {
        final File jobLogFile=job.getLogFile(); 
        final File commandLogFile;
        if (jobLogFile==null && defaultLogFile==null) {
            // a null 'job.logFile' means "don't write the log file"
            return;
        }
        else if (jobLogFile==null) {
            commandLogFile=new File(job.getWorkingDir(), defaultLogFile);
        }
        else if (!jobLogFile.isAbsolute()) {
            commandLogFile=new File(job.getWorkingDir(), jobLogFile.getPath());
        }
        else {
            commandLogFile=jobLogFile;
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
