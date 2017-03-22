package org.genepattern.server.executor.awsbatch;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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

import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.executor.CommandProperties;


public class AWSBatchJobRunner implements JobRunner{
    private static final Logger log = Logger.getLogger(AWSBatchJobRunner.class);

    private String defaultLogFile=null;
    
    private static  HashMap<String, DrmJobState> batchToGPStatusMap = new HashMap<String, DrmJobState>();
    
    
    
    public void setCommandProperties(CommandProperties properties) {
        log.debug("setCommandProperties");
        if (properties==null) {
            log.debug("commandProperties==null");
            return;
        }
        defaultLogFile=properties.getProperty(JobRunner.PROP_LOGFILE);
    }

    public void start() {
        log.debug("started JobRunner, classname="+this.getClass());
        
        //AWS Batch Status: SUBMITTED PENDING RUNNABLE STARTING RUNNING FAILED SUCCEEDED       
        batchToGPStatusMap.put("SUBMITTED", DrmJobState.GP_PROCESSING);
        batchToGPStatusMap.put("PENDING", DrmJobState.GP_PROCESSING);
        batchToGPStatusMap.put("RUNNABLE", DrmJobState.QUEUED);
        batchToGPStatusMap.put("STARTING", DrmJobState.RUNNING);
        batchToGPStatusMap.put("RUNNING", DrmJobState.RUNNING);
        batchToGPStatusMap.put("FAILED", DrmJobState.FAILED);
        batchToGPStatusMap.put("SUCCEEDED", DrmJobState.DONE);
        
    }

    protected ConcurrentMap<Integer,String> gpToAwsMap=new ConcurrentHashMap<Integer, String>();
    
 
    protected DrmJobStatus initStatus(DrmJobSubmission gpJob) {
        DrmJobStatus status = new DrmJobStatus.Builder(""+gpJob.getGpJobNo(), DrmJobState.QUEUED)
            .submitTime(new Date())
        .build();
        return status;
    }
    
   
    
    
   
    
    
    
    
    protected DrmJobStatus updateStatus_cancel(int gpJobNo, boolean isPending) {
        
        DrmJobStatus status;
        DrmJobStatus.Builder b;
    
        b = new DrmJobStatus.Builder().extJobId(""+gpJobNo);
        b.jobState(DrmJobState.UNDETERMINED);
        String awsId = this.gpToAwsMap.get(gpJobNo) ;
        b.extJobId(awsId);
        b.jobState( DrmJobState.CANCELLED );
         
        b.exitCode(-1); // hard-code exitCode for user-cancelled task
        b.endTime(new Date());
        b.jobStatusMessage("Job cancelled by user");
       
        return b.build();
    }

    boolean shuttingDown=false;
    @Override
    public void stop() {
        log.debug("shutting down ...");
        shuttingDown=true;
     }

    @Override
    public String startJob(DrmJobSubmission gpJob) throws CommandExecutorException {
        
      
        gpJob.getCommandLine();
        
        try {
       
            logCommandLine(gpJob);
            initStatus(gpJob);
            DrmJobStatus jobStatus =  runJobNoWait(gpJob);
         }
        catch (Throwable t) {
            throw new CommandExecutorException("Error starting job: "+gpJob.getGpJobNo(), t);
        }
        return ""+gpJob.getGpJobNo();
    }
    
    

    @Override
    public DrmJobStatus getStatus(DrmJobRecord jobRecord) {
        DrmJobStatus jobStatus = null;
        
        String awsId = this.gpToAwsMap.get(jobRecord.getGpJobNo());
        if (awsId == null){
            if (!(jobRecord.getExtJobId().equalsIgnoreCase(""+jobRecord.getGpJobNo())))
                awsId = jobRecord.getExtJobId();
        }
        
        
        if (awsId != null){
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            //
            // ask AWS for status and update since this is async
            // we'll always just get the old value for now
            //
            DefaultExecutor exec=new DefaultExecutor();
            exec.setStreamHandler( new PumpStreamHandler(outputStream));
            CommandLine cl= new CommandLine("/Users/liefeld/GenePattern/gp_dev/tedsandbox/awsCheckStatus.sh");
            // tasklib
            cl.addArgument(awsId);
            final Map<String,String> cmdEnv=null;
            try {
                exec.execute(cl, cmdEnv);
          
                String awsStatus =  outputStream.toString().trim();
                
                DrmJobStatus.Builder b;
                b = new DrmJobStatus.Builder().extJobId(this.gpToAwsMap.get(""+jobRecord.getGpJobNo()));
                b.jobState(batchToGPStatusMap.getOrDefault(awsStatus, DrmJobState.UNDETERMINED));
                System.out.println("-- get status called on job "+ jobRecord.getGpJobNo() + "  found " + awsStatus);
                if (awsStatus.equalsIgnoreCase("SUCCEEDED")){
                    // TODO get the CPU time etc from AWS.  Will need to change the check status to return the full
                    // JSON instead of just the ID and then read it into a JSON obj from which we pull the status
                    // and sometimes the other details
                    b.endTime(new Date());
                    refreshWorkingDirFromS3(jobRecord);
                } else if (awsStatus.equalsIgnoreCase("FAILED")) {
                    b.endTime(new Date());
                    refreshWorkingDirFromS3(jobRecord);
                }
                
                
                jobStatus = b.build();
                 
                
            } catch (Exception e){
                e.printStackTrace();
            }
            return jobStatus;
        }
        
        
       
        // special-case: job was terminated in a previous GP instance
        return new DrmJobStatus.Builder()
                .extJobId(jobRecord.getExtJobId())
                .jobState(DrmJobState.UNDETERMINED)
                .jobStatusMessage("No record for job, assuming it was terminated at shutdown of GP server")
            .build();
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
        CommandLine cl= new CommandLine("/Users/liefeld/GenePattern/gp_dev/tedsandbox/awsSyncDirectory.sh");
        // tasklib
        cl.addArgument(jobRecord.getWorkingDir().getAbsolutePath());
        final Map<String,String> cmdEnv=null;
        try {
            exec.execute(cl, cmdEnv);
      
            String output =  outputStream.toString().trim();
            System.out.println(output);
        } catch (Exception e){
            e.printStackTrace();
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
            
        } else {
            
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
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fr != null) {
                    fr.close();
                }
            } catch(IOException e) {
            }
            try {
                if (fw != null) {
                    fw.close();
                }
            } catch(IOException e) {
            }
        }
    }
    
    
    
    @Override
    public boolean cancelJob(DrmJobRecord jobRecord) throws Exception {
        log.debug("cancelJob, gpJobNo="+jobRecord.getGpJobNo());
        boolean isPending=false;
        
        
        String awsId = this.gpToAwsMap.get(jobRecord.getGpJobNo());
        if (awsId == null){
            if (!(jobRecord.getExtJobId().equalsIgnoreCase(""+jobRecord.getGpJobNo())))
                awsId = jobRecord.getExtJobId();
        }
        if (awsId != null){
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            //
            // ask AWS for status and update since this is async
            // we'll always just get the old value for now
            //
            DefaultExecutor exec=new DefaultExecutor();
            exec.setStreamHandler( new PumpStreamHandler(outputStream));
            CommandLine cl= new CommandLine("/Users/liefeld/GenePattern/gp_dev/tedsandbox/awsCancelJob.sh");
            // tasklib
            cl.addArgument(awsId);
            final Map<String,String> cmdEnv=null;
            try {
                exec.execute(cl, cmdEnv);
          
                String output =  outputStream.toString().trim();
                System.out.println(output);
            } catch (Exception e){
                e.printStackTrace();
            }
        
        }
        updateStatus_cancel(jobRecord.getGpJobNo(), isPending);
        return true;
    }
    
    
    
   


    protected static CommandLine initCommand(final DrmJobSubmission gpJob) {
        if (gpJob==null) {
            throw new IllegalArgumentException("gpJob==null");
        }
        List<String> gpCommand = gpJob.getCommandLine();
        if (gpCommand==null) {
            throw new IllegalArgumentException("gpJob.commandLine==null");
        }
        if (gpCommand.size()==0) {
            throw new IllegalArgumentException("gpJob.commandLine.size==0");
        }
        boolean handleQuoting=false;
        
        
        /** 
         * First pass the arguments needed by the AWS launch script, then follow with the normal GenePattern command line
         */
        // MyScript - should get this from some per-module configuration
        CommandLine cl= new CommandLine("/Users/liefeld/GenePattern/gp_dev/tedsandbox/runJava17OnBatch.sh");
        
        // tasklib
        cl.addArgument(gpJob.getTaskLibDir().getAbsolutePath());
       
        
        // workdir
        cl.addArgument(gpJob.getWorkingDir().getAbsolutePath());
        
        // input file directory
        cl.addArgument("/Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/Java17_oracle_jdk/tests/selectfeaturescolumns/data");
        
     
        
        for(int i=0; i<gpCommand.size(); ++i) {
            cl.addArgument(gpCommand.get(i), handleQuoting);
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

    protected DrmJobStatus runJobNoWait(final DrmJobSubmission gpJob) throws ExecutionException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Executor exec=initExecutorForJob(gpJob, outputStream);
        CommandLine cl = initCommand(gpJob);
        logCommandLine( gpJob ) ;
        
        final Map<String,String> cmdEnv=null;
        exec.execute(cl, cmdEnv);
        String awsJobId =  outputStream.toString();
        gpToAwsMap.put( gpJob.getGpJobNo(), awsJobId);
        
        DrmJobStatus status = new DrmJobStatus.Builder(""+gpJob.getGpJobNo(), DrmJobState.QUEUED)
                .submitTime(new Date())
            .build();
        return status;
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
        
        logCommandLine(commandLogFile, job);
    }
    
    protected void logCommandLine(final File logFile, final DrmJobSubmission job) { 
        logCommandLine(logFile, job.getCommandLine());
    }

    protected void logCommandLine(final File logFile, final List<String> args) { 
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

        if (logFile.exists()) {
            log.error("log file already exists: "+logFile.getAbsolutePath());
            return;
        }

        final boolean append=true;
        BufferedWriter bw = null;
        try {
            FileWriter fw = new FileWriter(logFile, append);
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
            log.error("error writing log file: "+logFile.getAbsolutePath(), e);
            return;
        }
        catch (Throwable t) {
            log.error("error writing log file: "+logFile.getAbsolutePath(), t);
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
    
    
   
    protected static void log(String s){
        System.out.println(s);
    }
    
   

}
