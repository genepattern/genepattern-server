/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.webservice.JobInfo;

/**
 * Run a job using RuntimeExec. This implementation of the CommandExecutor interface
 * waits for the job to complete.
 * 
 * @author pcarr
 */
public class RuntimeExecCommand {
    private static Logger log = Logger.getLogger(RuntimeExecCommand.class);
    
    enum Status { 
        PENDING,
        RUNNING,
        TERMINATED, //terminated by user request
        SERVER_SHUTDOWN //terminated on server shutdown
    }

    private Process process = null;
    private CopyStreamThread outputReader = null;
    private CopyStreamThread errorReader = null;

    private Status internalJobStatus = Status.PENDING;
    public Status getInternalJobStatus() {
        return internalJobStatus;
    }

    private int exitValue = 0;
    public int getExitValue() {
        return exitValue;
    }
    
    private StringBuffer stderrBuffer = new StringBuffer();
    public String getStderr() {
        return stderrBuffer.toString();
    }

    /**
     * Spawns a separate process to execute the requested analysis task. It copies the stdout and stderr output streams
     * to StringBuffers so that they can be returned to the invoker. The stdin input stream is closed immediately after
     * execution in order to ensure that the running task has no misconceptions about being able to read anything from
     * it. runCommand maintains entries in the htRunningJobs Hashtable whose keys are jobIDs and whose values are
     * running Process objects. This allows Processes to be stopped by jobID.
     * <p/>
     * <p/>
     * Please read about the BUG in the runCommand comments related to a race condition in the closure of the stdin
     * stream after forking the process.
     * 
     * @param commandLine
     *            String representation of the command line to run with all substitutions for parameters made.
     * @param env
     *            Hashtable of environment name/value pairs. Used to provide the environment to the exec method,
     *            including the modified PATH value.
     * @param runDir
     *            The directory in which to start the process running (it will be a temporary directory with only input
     *            files in it).
     * @param stdoutFile
     *            file to capture stdout output from the running process
     * @param stderrFile
     *            file to capture stderr output from the running process
     * @param jobInfo
     *            JobInfo object for this instance
     * @param stdin
     *            file path that is set to standard input of the running process or <tt>null</tt>
     * @param stderrBuffer
     *            buffer to append GenePattern errors to
     * @author Jim Lerner
     */
    public void runCommand(String commandLine[], Map<String, String> environmentVariables, File runDir, File stdoutFile, File stderrFile, JobInfo jobInfo, File stdinFile) {
        ProcessBuilder pb = null;
        try {
            pb = new ProcessBuilder(commandLine);
            Map<String, String> env = pb.environment();
            env.putAll(environmentVariables);
            pb.directory(runDir);

            // spawn the command
            log.debug("starting job: "+jobInfo.getJobNumber());
            long start_time = System.currentTimeMillis();

            process = pb.start();
            // BUG: there is race condition during a tiny time window between the exec and the close
            // (the lines above and below this comment) during which it is possible for an application
            // to imagine that there might be useful input coming from stdin.
            // This seemed to be the case for Perl 5.0.1, and might be a problem in other applications as well.
            OutputStream standardInStream = process.getOutputStream();
            
            if (stdinFile == null) {
                standardInStream.close();
            } 
            else {
                byte[] b = new byte[2048];
                int bytesRead;
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(stdinFile);
                    while ((bytesRead = fis.read(b)) >= 0) {
                        standardInStream.write(b, 0, bytesRead);
                    }
                } 
                finally {
                    if (fis != null) {
                        fis.close();
                    }
                    standardInStream.close();
                }
            }

            // create threads to read from the command's stdout and stderr streams
            outputReader = new CopyStreamThread(process.getInputStream(), new FileOutputStream(stdoutFile));
            errorReader = new CopyStreamThread(process.getErrorStream(), new FileOutputStream(stderrFile));

            // drain the output and error streams
            outputReader.start();
            errorReader.start();

            String jobID = ""+jobInfo.getJobNumber();
            log.debug(jobID+": process.waitFor()...");
            long t0 = System.currentTimeMillis();

            process.waitFor();

            long t1 = System.currentTimeMillis();
            log.debug(jobID+": process.waitFor()...done! took "+(Math.round(0.001*(t1 - t0)))+" s");

            // wait for all output before attempting to send it back to the client
            long waitTime = 60*1000L; //don't wait more than a minute during normal job execution
            if (internalJobStatus == Status.TERMINATED) {
                stderrBuffer.append("Job was terminated by user");
                waitTime = 10*1000L;
            }

            log.debug(jobID+": outputReader.join()...");
            t0 = System.currentTimeMillis();

            outputReader.join(waitTime);

            t1 = System.currentTimeMillis();
            log.debug(jobID+": outputReader.join()...done! took "+(t1 - t0)+" ms");

            log.debug(jobID+": errorReader.join()...");
            t0 = System.currentTimeMillis();

            errorReader.join(waitTime);

            t1 = System.currentTimeMillis();
            log.debug(jobID+": errorReader.join()...done! took "+(t1 - t0)+" ms");
            
            if (!outputReader.wroteBytes()) {
                boolean success = stdoutFile.delete();
                if (!success) {
                    log.error("Error deleting empty stdout file: "+stdoutFile.getAbsolutePath());
                }
            }
            if (!errorReader.wroteBytes()) {
                boolean success = stderrFile.delete();
                if (!success) {
                    log.error("Error deleting empty stderr file: "+stderrFile.getAbsolutePath());
                }
            }
            
            long d = Math.round(0.001*(System.currentTimeMillis()-start_time));
            log.debug(jobID+": job completed in "+d+" s");

            exitValue = process.exitValue();
            if (exitValue != 0) {
                if (stderrBuffer.length() == 0) {
                    stderrBuffer.append("Process exited with status code: "+exitValue);
                }
            }
        } 
        catch (Throwable t) {
            log.error("Error in runCommand, reporting to stderr.", t);
            stderrBuffer.append(t.getLocalizedMessage());
            exitValue = -1;
        } 
    }

    public void terminateProcess(Status status) {
        this.internalJobStatus = status;
        if (outputReader != null) {
            outputReader.interrupt();
        }
        if (errorReader != null) {
            errorReader.interrupt();
        }
        if (process != null) {
            process.destroy();
        }
    }
}
