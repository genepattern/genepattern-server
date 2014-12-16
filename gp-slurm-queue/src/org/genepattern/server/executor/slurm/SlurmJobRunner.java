package org.genepattern.server.executor.slurm;

import java.io.*;
import java.util.*;

import com.google.common.base.Joiner;
import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.JobRunner;
import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.drm.impl.lsf.core.CmdException;
import org.genepattern.drm.impl.lsf.core.CommonsExecCmdRunner;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.executor.CommandExecutorException;

/**
 * Implementation of the JobRunner interface for SLURM
 *
 * @author Thorin Tabor
 */
public class SlurmJobRunner implements JobRunner {
    private static final Logger log = Logger.getLogger(SlurmJobRunner.class);

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
                commandLineStr += (" "+arg);
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
            int i=0;
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
    String buildSubmissionScript(String gpJobId, String workDirPath, String commandLine, String queue, String account, String maxTime) throws CommandExecutorException {
        File workingDirectory = new File(workDirPath);
        File jobScript = new File(workingDirectory, "launchJob.sh");
        String scriptText = "#!/bin/bash -l\n" +
                            "#\n" +
                            "#SBATCH -J " + gpJobId + "\n" +
                            "#SBATCH -o stdout.txt\n" +
                            "#SBATCH -e stderr.txt\n" +

                            "#SBATCH -t " + maxTime + "\n" +
                            "#SBATCH -A " + account + "\n" +
                            "#\n" +
                            "#SBATCH -p " + queue + " -n 1\n" +
                            " \n" +
                            "ibrun " + commandLine + "\n";

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
            throw new CommandExecutorException("Error writing launchJob.sh script for job: " + gpJobId);
        }
        finally {
            try {
                if (writeScript != null) writeScript.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            return jobScript.getCanonicalPath();
        } catch (IOException e) {
            log.error("Error getting Canonical Path, falling back to Absolute Path: " + jobScript.getAbsolutePath());
            return jobScript.getAbsolutePath();
        }
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
        String commandLine = Joiner.on(" ").join(commandLineList);

        // Get configuration and context objects
        GpConfig config = drmJobSubmission.getGpConfig();
        GpContext context = drmJobSubmission.getJobContext();

        // Get necessary configuration variables
        String queue = drmJobSubmission.getQueue("development");
        String account =  config.getGPProperty(context, "tacc.account", "TACC-GenePattern");
        String maxTime = drmJobSubmission.getWalltime("00:01:00").toString();

        // Build the shell script for submitting the slurm job
        String scriptPath = buildSubmissionScript(gpJobId, workDir, commandLine, queue, account, maxTime);

        // Run the command line through the Slurm shell script
        CommonsExecCmdRunner commandRunner = new CommonsExecCmdRunner();
        List<String> output = null;
        try {
            output = commandRunner.runCmd(Arrays.asList("sbatch", scriptPath));
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
            extJobId = extractExternalID(output);
        }
        catch (Exception e) {
            log.error("Error obtaining slurm job ID: " + e.getMessage());
            throw new CommandExecutorException(e.getMessage());
        }

        // Return the external job ID
        return extJobId;
    }

    /**
     * Extract the Slurm status from the squeue output text
     *
     * @param extJobId - The SLurm job ID
     * @param stderr - Standard error
     * @param output - Standard out
     * @return - The status of the Slurm job
     * @throws Exception
     */
    private DrmJobStatus extractSlurmStatus(String extJobId, File stderr, List<String> output) throws Exception {
        // Extract the status string from the output
        String slurmStatusString = null;
        if (output.size() > 3) log.warn("Extra lines found in Slurm status output");
        if (output.size() > 2) {
            StringTokenizer tokenizer = new StringTokenizer(output.get(2));
            if (tokenizer.countTokens() < 5) {
                log.warn("Missing tokens from Slurm status output");
            }
            else {
                int count = 1;
                while (count != 5) {
                    tokenizer.nextToken();
                    count++;
                }
                slurmStatusString = tokenizer.nextToken();
            }
        }

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
        else if (slurmStatusString.compareToIgnoreCase("CANCELLI") == 0 || slurmStatusString.compareToIgnoreCase("CANCELLED") == 0) {
            return new DrmJobStatus.Builder(extJobId, DrmJobState.CANCELLED).build();
        }
        else if (slurmStatusString.compareToIgnoreCase("COMPLETI") == 0 || slurmStatusString.compareToIgnoreCase("COMPLETING") == 0) {
            return new DrmJobStatus.Builder(extJobId, DrmJobState.RUNNING).build();
        }
        else if (slurmStatusString.compareToIgnoreCase("COMPLETE") == 0 || slurmStatusString.compareToIgnoreCase("COMPLETED") == 0) {
            if (stderr != null && stderr.exists()) {
                return new DrmJobStatus.Builder(extJobId, DrmJobState.FAILED).exitCode(-1).build();
            }
            else {
                return new DrmJobStatus.Builder(extJobId, DrmJobState.DONE).exitCode(0).endTime(new Date()).build();
            }
        }
        else if (slurmStatusString.compareToIgnoreCase("CONFIGUR") == 0 || slurmStatusString.compareToIgnoreCase("CONFIGURING") == 0) {
            return new DrmJobStatus.Builder(extJobId, DrmJobState.QUEUED).build();
        }
        else if (slurmStatusString.compareToIgnoreCase("FAILED") == 0) {
            return new DrmJobStatus.Builder(extJobId, DrmJobState.FAILED).exitCode(-1).build();
        }
        else if (slurmStatusString.compareToIgnoreCase("TIMEOUT") == 0) {
            return new DrmJobStatus.Builder(extJobId, DrmJobState.FAILED).exitCode(-1).build();
        }
        else if (slurmStatusString.compareToIgnoreCase("PREEMPT") == 0 || slurmStatusString.compareToIgnoreCase("PREEMPTED") == 0) {
            return new DrmJobStatus.Builder(extJobId, DrmJobState.FAILED).exitCode(-1).build();
        }
        else if (slurmStatusString.compareToIgnoreCase("NODE_FAI") == 0 || slurmStatusString.compareToIgnoreCase("NODE_FAIL") == 0) {
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
        String extJobId = drmJobRecord.getExtJobId();
        File stderr = drmJobRecord.getStderrFile();

        // Run the command line to get status
        CommonsExecCmdRunner commandRunner = new CommonsExecCmdRunner();
        List<String> output = null;
        try {
            output = commandRunner.runCmd(Arrays.asList("squeue",
                                                        "-l",
                                                        "-t 'PENDING,RUNNING,SUSPENDED,CANCELLED,COMPLETING,COMPLETED,CONFIGURING,FAILED,TIMEOUT,PREEMPTED,NODE_FAIL'",
                                                        "-j " + extJobId));
        }
        catch (Throwable e) {
            log.error("Error getting status for slurm job: " + e.getMessage());
        }

        try {
            return extractSlurmStatus(extJobId, stderr, output);
        }
        catch (Exception e) {
            // It is likely that this job finished a long time ago, mark as failed
            log.error("Exception checking job status: " + e);
            Thread.currentThread().interrupt();
            return new DrmJobStatus.Builder(extJobId, DrmJobState.FAILED).exitCode(-1).build();
        }
    }
}