/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.slurm;


import java.io.*;
import java.util.*;

import java.lang.StringBuffer;
import com.google.common.base.Joiner;
import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.JobRunner;
import org.genepattern.drm.Memory;
import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.drm.impl.lsf.core.CmdException;
import org.genepattern.drm.impl.lsf.core.CommonsExecCmdRunner;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.executor.CommandExecutorException;

/**
 * Implementation of the JobRunner interface for Slurm
 *
 * @author Thorin Tabor, modified by Ted Liefeld
 */
public class SlurmJobRunner implements JobRunner {
    private static final Logger log = Logger.getLogger(SlurmJobRunner.class);

    private HashMap<Integer, DrmJobStatus> statusMap = new HashMap<Integer, DrmJobStatus>();
    
    
    public String remotePrefix ;
    public boolean failIfStderr = false;
    
    public SlurmJobRunner(){
        super();
        System.out.println("Initializing SlurmJobRunner");
    }
    /**
     * If configured by the server admin, write the command line into a log file
     * in the working directory for the job.
     * <pre>
     * # flag, if true save the command line into a log file in the working directory for each job
     * rte.save.logfile: false
     * # the name of the command line log file
     * rte.logfile: .rte.out
     * </pre>
     *
     */
    private void logCommandLine(DrmJobSubmission drmJobSubmission) {
        if (drmJobSubmission.getLogFile() == null) {
            // a null logfile means "don't write the log file"
            return;
        }
        final File commandLogFile;
        if (!drmJobSubmission.getLogFile().isAbsolute()) {
            //relative path is relative to the working directory for the job
            commandLogFile = new File(drmJobSubmission.getWorkingDir(), drmJobSubmission.getLogFile().getPath());
        }
        else {
            commandLogFile = drmJobSubmission.getLogFile();
        }
        log.debug("saving command line to log file ...");
        String commandLineStr = "";
        boolean first = true;
        for(final String arg : drmJobSubmission.getCommandLine()) {
            if (first) {
                commandLineStr = arg;
                first = false;
            }
            else {
                commandLineStr += (" " + arg);
            }
        }
        if (commandLogFile.exists()) {
            log.error("log file already exists: " + commandLogFile.getAbsolutePath());
            return;
        }
        BufferedWriter bw = null;
        try {
            FileWriter fw = new FileWriter(commandLogFile);
            bw = new BufferedWriter(fw);
            bw.write(commandLineStr);
            bw.newLine();
            int i = 0;
            for(final String arg : drmJobSubmission.getCommandLine()) {
                bw.write(" arg[" + i + "]: '" + arg + "'");
                bw.newLine();
                ++i;
            }
            bw.close();
        }
        catch (IOException e) {
            log.error("error writing log file: " + commandLogFile.getAbsolutePath(), e);
        }
        catch (Throwable t) {
            log.error("error writing log file: " + commandLogFile.getAbsolutePath(), t);
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

    /**
     * Stop the job runner
     */
    @Override
    public void stop() {
        log.info("Stopping SlurmJobRunner");
    }

    /**
     * Cancel the Slurm job
     *
     * @param drmJobRecord, contains a record of the job
     * @return - Return whether the job was successfully cancelled or not
     * @throws Exception
     */
    @Override
    public boolean cancelJob(DrmJobRecord drmJobRecord) throws Exception {
        final String drmJobId = drmJobRecord.getExtJobId();
        CommonsExecCmdRunner commandRunner = new CommonsExecCmdRunner();
        try {
            commandRunner.runCmd(Arrays.asList("scancel", drmJobId));
        }
        catch (CmdException e) {
            log.error("Error canceling slurm job: " + e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Extract the Slurm ID from the job submission output
     *
     * @param output - The output from the Slurm terminal command
     * @return - Return the job ID in Slurm
     */
    private String extractExternalID(List<String> output) throws Exception {
        int lines = output.size();
        if (lines > 0) {
            String lastLine = output.get(lines - 1);

            // Check for error messages in the output
            for (String line : output) {
                if (line.toLowerCase().contains("error")) {
                    throw new CommandExecutorException("Error message found in output: " + line);
                }
            }

            // Find the correct line with the job number
            for (String line : output) {
                if (line.contains("Submitted batch job")) {
                    // Extract the job number
                    String[] parts = lastLine.split(" ");
                    // Return the job number
                    return parts[parts.length - 1];
                }
            }
        }

        // If we weren't able to get the job number or had no lines of output, throw an error
        throw new CommandExecutorException("Cannot extract Slurm ID");
    }
    public static String memFormatG(final Memory m) {
        
        long mib = (long) Math.ceil(
            (double) m.getNumBytes() / (double) Memory.Unit.g.getMultiplier()
        );
        return ""+mib+"G";
    }
    
    /**
     * Build the shell script necessary to initiate the Slurm job
     *
     * @param gpJobId - GenePattern job ID
     * @param workDirPath - Path to the working directory
     * @param commandLine - Command line string
     * @param queue - Name of the queue to execute under
     * @param account - The account to charge processing to
     * @param maxTime - The maximum compute time (see doc for max allowable times on compute system)
     * @return - Return the path to the submission script
     * @throws CommandExecutorException
     */
    String buildSubmissionScript(DrmJobSubmission drmJobSubmission, String gpJobId, String workDirPath, String commandLine) throws CommandExecutorException {
        GpConfig config = drmJobSubmission.getGpConfig();
        GpContext jobContext = drmJobSubmission.getJobContext();
        
        // Get necessary configuration variables
        String sbatchPrefix = config.getGPProperty(jobContext, "slurm.sbatch.prefix", "");
        String sbatchExtra = config.getGPProperty(jobContext, "slurm.additional.command", "");
     
        String partition = drmJobSubmission.getQueue("shared");
        String account =  config.getGPProperty(jobContext, "slurm.account", "WHO_PAYS_FOR_THIS");
        String maxTime = drmJobSubmission.getWalltime("02:00:00").toString();
        
        Integer cpusPerTaskGPUOveride =  config.getGPIntegerProperty(jobContext, "slurm.cpus.per.task", null); // GPU uses 10
        String gpuMemoryOveride =  config.getGPProperty(jobContext, "slurm.gpu.memory", null); // GPU uses 186 GB
        String nGPU = config.getGPProperty(jobContext, "slurm.ngpus", null);
        String nNodes = config.getGPProperty(jobContext, "slurm.nnodes", null);
         
        
        File workingDirectory = new File(workDirPath);
        File jobScript = new File(workingDirectory, ".launchJob.sb");
        
        Memory memRequested = drmJobSubmission.getMemory();
        if (memRequested == null) memRequested =  Memory.fromString("2 Gb");
        
        // the value in the config file is a default and also a floor for gpu memory  
        if (gpuMemoryOveride != null){
            Memory gpuMinMem = Memory.fromString(gpuMemoryOveride);
            if (memRequested.getNumBytes() < gpuMinMem.getNumBytes()) memRequested = gpuMinMem;
        }
        
     
        
        String nCPUStr = drmJobSubmission.getProperty("job.cpuCount.per.task");
        Integer nCPU = null;
        if (nCPUStr != null){
            nCPU = new Integer(nCPUStr);
        } else {
            nCPU = drmJobSubmission.getCpuCount();
        }
        if (nCPU == null) nCPU = 1;
        if (cpusPerTaskGPUOveride != null) {
            
            if (cpusPerTaskGPUOveride > nCPU) nCPU=cpusPerTaskGPUOveride;
        }
        
        // the value in the config file is a default and also a floor for nGPU
        String nGPU_job = drmJobSubmission.getProperty("job.gpuCount");
        
        if (nGPU != null){
            Integer n_gpu = new Integer(nGPU);
            if (nGPU_job != null){
                Integer n_gpu_job = new Integer(nGPU_job);
                if (n_gpu_job > n_gpu) nGPU = nGPU_job;
            }
            if (nCPU < 10) nCPU = 10; // for GPU
        }
        String ntasksPerNodeDefault =  config.getGPProperty(jobContext, "slurm.ntasks.per.node", "1"); // GPU uses 2
        String ntasksPerNode = drmJobSubmission.getProperty("job.numTasksPerNode");
        if (ntasksPerNode == null) ntasksPerNode =  ntasksPerNodeDefault;
        
        
        // the partition can override the default IFF nGPU >= 4
        // this is for the SDSC expanse system where you cannot use the gpu-shared
        // queue for 4 or more GPU but the GPU-shared is cheaper and we want to use that when we can
        //
        // to use in config specify a drmSubmission variable name and a value and an alt queue name
        //     job.alt.queue: "gpu"
        //     job.alt.queue.switchover.var: "job.gpuCount"
        //     job.alt.queue.switchover.val: "4"
        //
        // with this it will switch to the queue/partition named 'gpu' if job.gpuCount >= 4
        
        String altPartition = config.getGPProperty(jobContext, "job.alt.queue", null);
        String altSwitchover = config.getGPProperty(jobContext, "job.alt.queue.switchover.val", null);
        String altSwitchoverVar = config.getGPProperty(jobContext, "job.alt.queue.switchover.var", null);

//        System.out.println("---- altPartition "+ altPartition);
//        System.out.println("---- altSwitchover "+ altSwitchover);
//        System.out.println("---- altSwitchoverVar "+ altSwitchoverVar);
        
        if ((altPartition != null) & (altSwitchover != null) && (altSwitchoverVar != null)){
            try {
                Integer valOfVar = new Integer(drmJobSubmission.getProperty(altSwitchoverVar));
                //System.out.println("---- valOfVar "+ valOfVar);
                
                
                Integer altSwitchoverPoint = new Integer(altSwitchover);
               
                if (valOfVar >= altSwitchoverPoint){
                    partition = altPartition;
                }
            } catch (Exception e){
                // swallow
                log.error(e);
            }
        }
        
        
        String scriptText = "#!/bin/bash -l\n" +
                            "#\n" +
                            "#SBATCH --no-requeue \n" +
                            "#SBATCH --job-name="+gpJobId+"_gp\n" +
                            "#SBATCH -D " + workDirPath  + "\n" +
                            "#SBATCH --output="+workDirPath+"/stdout.txt\n" +
                            "#SBATCH --error="+workDirPath+"/stderr.txt\n" +
                            "#SBATCH --nodes=1\n" +     // no parallel jobs here
                            "#SBATCH --ntasks-per-node="+ntasksPerNode+"\n" +
                            "#SBATCH --mem="+ memFormatG(memRequested) + "\n" +
                            "#SBATCH --cpus-per-task="+ nCPU + "\n" +
                            "#SBATCH --time=" + maxTime + "\n" +
                            "#SBATCH --account=" + account + "\n" +
                            "#\n" +
                            "#SBATCH --partition=" + partition + " \n";
        
        if (nGPU != null) scriptText += "#SBATCH --gpus="+nGPU + "\n";
        if (nNodes != null) scriptText += "#SBATCH --nodes="+nNodes + "\n";
        
        scriptText +=       " \n" +
                            sbatchExtra + 
                            "\n\n" +
                            sbatchPrefix + " " + commandLine + "\n";

        // Test working directory before writing
        if (!workingDirectory.exists()) throw new CommandExecutorException("Working directory does not exist!");
        if (!workingDirectory.canWrite()) throw new CommandExecutorException("No write access to working directory!");
        if (!workingDirectory.isDirectory()) throw new CommandExecutorException("Working directory is not a directory!");
        if (jobScript.exists()) log.warn("script.sh already exists in working directory, overwriting!");

        // Write script to working directory
        FileOutputStream writeScript = null;
        try {
            if (!jobScript.exists()) {
                //noinspection ResultOfMethodCallIgnored
                jobScript.createNewFile();
            }

            writeScript = new FileOutputStream(jobScript);
            writeScript.write(scriptText.getBytes());
        }
        catch (Exception e) {
            log.error("Error writing launchJob.sh script for job: " + gpJobId);
            throw new CommandExecutorException("Error writing .launchJob.sb script for job: " + gpJobId);
        }
        
        // DEBUGGING
        File jobScript2 = new File("/home/genepattern/launchJob.sb."+drmJobSubmission.getGpJobNo());
        try {
            writeScript = new FileOutputStream(jobScript2);
            writeScript.write(scriptText.getBytes());
        } catch (Exception e){
            e.printStackTrace();
        }
        
        finally {
            try {
                if (writeScript != null) writeScript.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        return jobScript.getAbsolutePath();
    }

    
    protected DrmJobStatus initStatusSubmittedToSlurm(DrmJobSubmission gpJob) {
        DrmJobStatus status = new DrmJobStatus.Builder(""+gpJob.getGpJobNo(), DrmJobState.QUEUED)
            .submitTime(new Date())
        .build();
        return status;
    }
    
  
    
    /**
     * Submit a Slurm job
     *
     * @param drmJobSubmission - DrmJobSubmission object
     * @return - Return the Slurm job ID
     * @throws CommandExecutorException
     */
    @Override
    public String startJob(DrmJobSubmission drmJobSubmission) throws CommandExecutorException {
        String gpJobId = drmJobSubmission.getGpJobNo().toString();
        String workDir = drmJobSubmission.getWorkingDir().getAbsolutePath();
        List<String> commandLineList = drmJobSubmission.getCommandLine();
        GpConfig config = drmJobSubmission.getGpConfig();
        GpContext context = drmJobSubmission.getJobContext();
        
        String replacePath =  config.getGPProperty(context, "path.to.replace", null);
        
        StringBuffer cmdBuffer = new StringBuffer();
        // String commandLine = Joiner.on(" ").join(commandLineList);
        // try to put quotes around args with spaces
        for (String s: commandLineList){
            boolean needsQuotes = false;
            
            if (s.contains(" ")){
                needsQuotes = true;
            }
            cmdBuffer.append(" ");
            if (needsQuotes) cmdBuffer.append("\"");
            cmdBuffer.append(s);
            if (needsQuotes) cmdBuffer.append("\"");
        }
        String commandLine = cmdBuffer.toString();
        
        log.error("SBatch working dir " + workDir);
        
        // Get configuration and context objects
        config = drmJobSubmission.getGpConfig();
        context = drmJobSubmission.getJobContext();

        
        String replaceWithPath =  config.getGPProperty(context, "path.replaced.with", null);
          
        
        if ((replacePath != null )&&(replaceWithPath != null )){
            commandLine = commandLine.replaceAll(replacePath, replaceWithPath);
        }
        // Build the shell script for submitting the slurm job
        String scriptPath = buildSubmissionScript(drmJobSubmission, gpJobId, workDir, commandLine);

        // Substitute file path prefixes with the correct prefixes for the execution server
        scriptPath = scriptPath.replaceAll(replacePath, replaceWithPath);

        // add prefix to ssh to a submit node if needed
        // XXX need to cache this because we cannot get the config to retrieve it 
        // later in get status calls
        if (remotePrefix == null) {
            remotePrefix = config.getGPProperty(context, "remote.exec.prefix", "");
            failIfStderr = config.getGPBooleanProperty(context, "job.error_status.stderr", false);
        }
   
        
        
        String[] remotePrefixArray = remotePrefix.split("\\s+");
        List<String> prefixArray = Arrays.asList(remotePrefixArray);
        ArrayList<String> commandArray = new ArrayList<String>();
        commandArray.addAll(prefixArray);
        commandArray.add("sbatch");
        commandArray.add(scriptPath);
        
        // Run the command line through the Slurm shell script
        CommonsExecCmdRunner commandRunner = new CommonsExecCmdRunner();
        List<String> output = null;
        
        StringBuffer buff = new StringBuffer();
        for (String s: commandArray){
            buff.append(s);
            buff.append(" ");
        }
        log.debug("slurm job command: " + buff.toString());
        
        try {
            output = commandRunner.runCmd(commandArray);
            initStatusSubmittedToSlurm(drmJobSubmission);
        }
        catch (Throwable e) {
            log.error("Error submitting slurm job: " + e.getMessage());
            throw new CommandExecutorException(e.getMessage());
        }

        // Write the output to a log file
        logCommandLine(drmJobSubmission);

        // Extract the external job ID from the output
        String extJobId = null;
        try {
            for (String s: output){
                log.debug("slurm job output: " + s);
            }
            extJobId = extractExternalID(output);
            log.debug("slurm job id: " + extJobId);
        }
        catch (Exception e) {
            
            
            log.error("Error obtaining slurm job ID: " + e.getMessage()+ "\n -- " + buff.toString());
            
            throw new CommandExecutorException(e.getMessage());
        }

        // Return the external job ID
        return extJobId;
    }

    /**
     * Extract the Slurm status from the squeue output text
     *
     * @param extJobId - The Slurm job ID
     * @param stderr - Standard error
     * @param output - Standard out
     * @return - The status of the Slurm job
     * @throws Exception
     */
    private DrmJobStatus extractSlurmStatus(String extJobId, File stderr, List<String> output) throws Exception {
        // Extract the status string from the output
        String slurmStatusString = null;
        if (output.size() > 3) log.warn("Extra lines found in Slurm status output " + output.size());
        if (output.size() > 2) {
            // nominally there should be 3 lines unless SDSC adds warning messages like they did on 2/23/22
            // so if there are >3 lines we will try for the last one
            int idx = output.size()-1;
            StringTokenizer tokenizer = new StringTokenizer(output.get(idx));
            log.debug("--Parsing slurm status from: " + output.get(idx));
            
            if (tokenizer.countTokens() < 5) {
                log.warn("Missing tokens from Slurm status output");
            }
            else {
                
                int count = 1;
                while (count != 5) {
                    String tok = (String)tokenizer.nextToken();
                    log.debug(tok);
                    count++;
                }
                slurmStatusString = tokenizer.nextToken();
                log.debug("     slurm status: " + slurmStatusString);
            }
        }

        return slurmStatusToDrmStatus(extJobId, stderr, slurmStatusString);
    }
    private DrmJobStatus slurmStatusToDrmStatus(String extJobId, File stderr, String slurmStatusString) throws CommandExecutorException {
        // just in case its ever not all caps already
        slurmStatusString = slurmStatusString.toUpperCase();
        
        // Build the correct status from the string, and return
        if (slurmStatusString == null) {
            log.error("Cannot retrieve Slurm status string: null");
            throw new CommandExecutorException("Cannot retrieve Slurm status string: null");
        }
        else if (slurmStatusString.compareToIgnoreCase("PENDING") == 0) {
            return new DrmJobStatus.Builder(extJobId, DrmJobState.QUEUED).build();
        }
        else if (slurmStatusString.compareToIgnoreCase("RUNNING") == 0) {
            return new DrmJobStatus.Builder(extJobId, DrmJobState.RUNNING).build();
        }
        else if (slurmStatusString.compareToIgnoreCase("SUSPEND") == 0 || slurmStatusString.compareToIgnoreCase("SUSPENDED") == 0) {
            return new DrmJobStatus.Builder(extJobId, DrmJobState.SUSPENDED).build();
        }
        else if (slurmStatusString.startsWith("CANCEL")  ) {
            return new DrmJobStatus.Builder(extJobId, DrmJobState.CANCELLED).build();
        }
        else if ( slurmStatusString.compareToIgnoreCase("CANCELLED") == 0) {
            return new DrmJobStatus.Builder(extJobId, DrmJobState.CANCELLED).build();
        }
        else if (slurmStatusString.indexOf("COMPLET") >= 0 ) {
            return new DrmJobStatus.Builder(extJobId, DrmJobState.RUNNING).build();
        }
        else if (slurmStatusString.compareToIgnoreCase("COMPLETE") == 0 || slurmStatusString.compareToIgnoreCase("COMPLETED") == 0) {
            if (failIfStderr && (stderr != null && stderr.exists() && stderr.length() != 0) ) {
                Thread.currentThread().interrupt();
                return new DrmJobStatus.Builder(extJobId, DrmJobState.FAILED).exitCode(-1).build();
            } else {
                return new DrmJobStatus.Builder(extJobId, DrmJobState.DONE).exitCode(0).endTime(new Date()).build();
            }
        }
        else if (slurmStatusString.compareToIgnoreCase("CONFIGUR") == 0 || slurmStatusString.compareToIgnoreCase("CONFIGURING") == 0) {
            return new DrmJobStatus.Builder(extJobId, DrmJobState.QUEUED).build();
        }
        else if (slurmStatusString.compareToIgnoreCase("FAILED") == 0) {
            Thread.currentThread().interrupt();
            return new DrmJobStatus.Builder(extJobId, DrmJobState.FAILED).exitCode(-1).build();
        }
        else if (slurmStatusString.compareToIgnoreCase("TIMEOUT") == 0) {
            Thread.currentThread().interrupt();
            return new DrmJobStatus.Builder(extJobId, DrmJobState.FAILED).exitCode(-1).build();
        }
        else if (slurmStatusString.compareToIgnoreCase("PREEMPT") == 0 || slurmStatusString.compareToIgnoreCase("PREEMPTED") == 0) {
            Thread.currentThread().interrupt();
            return new DrmJobStatus.Builder(extJobId, DrmJobState.FAILED).exitCode(-1).build();
        }
        else if (slurmStatusString.compareToIgnoreCase("NODE_FAI") == 0 || slurmStatusString.compareToIgnoreCase("NODE_FAIL") == 0) {
            Thread.currentThread().interrupt();
            return new DrmJobStatus.Builder(extJobId, DrmJobState.FAILED).exitCode(-1).build();
        } else if (slurmStatusString.indexOf("Invalid job id specif") >= 0){
            Thread.currentThread().interrupt();
            return new DrmJobStatus.Builder(extJobId, DrmJobState.FAILED).exitCode(-1).build();      
        }
        else {
            log.error("Unknown Slurm status string: " + slurmStatusString);
            throw new CommandExecutorException("Unknown Slurm status string: " + slurmStatusString);
        }
    }

    /**
     * Check the status of a Slurm job
     *
     * @param drmJobRecord - DrmJobRecord object
     * @return - Return the status of the Slurm job
     */
    @Override
    public DrmJobStatus getStatus(DrmJobRecord drmJobRecord) {
        DrmJobStatus status = _getStatus(drmJobRecord);
        
        status = updateJobRunnerJobDetails(drmJobRecord, status);
        
        
        return status;
        
    }
    
    public DrmJobStatus updateJobRunnerJobDetails(DrmJobRecord drmJobRecord, DrmJobStatus status){
        DrmJobStatus finalStatus = status;
        try {
            int gpJobNo = drmJobRecord.getGpJobNo();
            DrmJobStatus oldStatus = statusMap.get(gpJobNo);
            
            // first time here
            if ((oldStatus == null) && (!status.getJobState().equals(DrmJobState.TERMINATED))) {
                statusMap.put(gpJobNo, status);
            }
            // nothing has changed so nothing to do
            if (oldStatus.getJobState().equals(status.getJobState())){
                return finalStatus;
            }
            
            if (oldStatus.getJobState().equals(DrmJobState.GP_PENDING) 
                                        && status.getJobState().equals(DrmJobState.GP_PROCESSING)){
                    finalStatus = initStatusStartedOnSlurm(drmJobRecord, status.getJobState());
                    statusMap.put(gpJobNo, status);
            }

            
            // job is done.  Make sure to remove it 
            if (status.getJobState().equals(DrmJobState.TERMINATED)) {
                statusMap.remove(gpJobNo);
                status = initStatusFinishedOnSlurm(drmJobRecord, status.getJobState());
            }
            
            // look for when it changes from pending to started so that we can record the 
            // actual time spent running (roughly)
            if (oldStatus.getJobState().equals(DrmJobState.QUEUED) 
                    && status.getJobState().equals(DrmJobState.RUNNING)){
                finalStatus = initStatusStartedOnSlurm(drmJobRecord, status.getJobState());
                statusMap.put(gpJobNo, status);
            }
            
        } catch (Exception e){
            e.printStackTrace();
            
            log.error(e);
        } finally {
            return finalStatus;
        }
        
    }
    protected DrmJobStatus initStatusStartedOnSlurm(DrmJobRecord gpJob, DrmJobState state) {
        DrmJobStatus status = new DrmJobStatus.Builder(""+gpJob.getGpJobNo(), state)
            .startTime(new Date())
        .build();
        return status;
    }
    protected DrmJobStatus initStatusFinishedOnSlurm(DrmJobRecord gpJob, DrmJobState state) {
        DrmJobStatus status = new DrmJobStatus.Builder(""+gpJob.getGpJobNo(), state)
            .endTime(new Date())
        .build();
        
        
        return status;
    }
    
    
    public DrmJobStatus _getStatus(DrmJobRecord drmJobRecord) {
            
        String extJobId = drmJobRecord.getExtJobId();
        File stderr = drmJobRecord.getStderrFile();

        // Run the command line to get status
        CommonsExecCmdRunner commandRunner = new CommonsExecCmdRunner();
        List<String> output = null;
        
        // this can get called before the job runner is initialized.  This prevents it from bailing on a running job
        if (remotePrefix == null) return null;
        
        
        String[] remotePrefixArray = remotePrefix.split("\\s+");
        List<String> prefixArray = Arrays.asList(remotePrefixArray);
        ArrayList<String> commandArray = new ArrayList<String>();
        commandArray.addAll(prefixArray);
        commandArray.add("squeue");
        commandArray.add(        "-l");
        commandArray.add("-t 'PENDING,RUNNING,SUSPENDED,CANCELLED,COMPLETING,COMPLETED,CONFIGURING,FAILED,TIMEOUT,PREEMPTED,NODE_FAIL'");
        commandArray.add("-j " + extJobId);
        
        try {
            StringBuffer buff = new StringBuffer();
            for (String s: commandArray){
                buff.append(s);
                buff.append(" ");
            }
            log.error("slurm getStatus command: " + buff.toString());
            
            
            output = commandRunner.runCmd(commandArray);
            log.error("slurm getStatus result: \n" + output.get(0)+"\n"+output.get(1)+"\n"+output.get(2));
            
            
        }
        catch (Throwable e) {
            log.error("Error getting status for slurm job with squeue: " + extJobId + "  " +  e.getMessage());
            return this.altGetStatus(drmJobRecord);
        }

        try {
           
            return extractSlurmStatus(extJobId, stderr, output);
          
        }
        catch (Exception e) {
            // It is likely that this job finished a long time ago, mark as failed
            log.error("Exception checking job status with squeue: " + e);
            Thread.currentThread().interrupt();
            
            return null;
        }
    }
    
    public DrmJobStatus altGetStatus(DrmJobRecord drmJobRecord) {
        String extJobId = drmJobRecord.getExtJobId();
        File stderr = drmJobRecord.getStderrFile();

        // Run the command line to get status
        CommonsExecCmdRunner commandRunner = new CommonsExecCmdRunner();
        List<String> output = null;
        
        String[] remotePrefixArray = remotePrefix.split("\\s+");
        List<String> prefixArray = Arrays.asList(remotePrefixArray);
        ArrayList<String> commandArray = new ArrayList<String>();
        commandArray.addAll(prefixArray);
        commandArray.add("sacct");
        commandArray.add("-n");
        commandArray.add("--format STATE");
        commandArray.add("-j " + extJobId+".batch"); // only the batch step, must be submitted using sbatch
        
        try {
            StringBuffer buff = new StringBuffer();
            for (String s: commandArray){
                buff.append(s);
                buff.append(" ");
            }
            log.error("slurm getStatus command: " + buff.toString());
            
            
            output = commandRunner.runCmd(commandArray);
        
            String slurmStatusString = null;
            if (output.size() > 1) log.warn("Extra lines found in Slurm sacct status output");
         
            for (int i=0; i< output.size(); i++){
                log.error(output.get(i));
            }
            
            if (output.size() > 0) {
                // grab the last line
                slurmStatusString = output.get(output.size()-1);
            }
            return slurmStatusToDrmStatus(extJobId, stderr, slurmStatusString);
        }
        catch (Exception e) {
            // It is likely that this job finished a long time ago, mark as failed
            log.error("Exception checking job status with sacct: " + e);
            
            Thread.currentThread().interrupt();
            return null;
        }
    }
    
    
}
