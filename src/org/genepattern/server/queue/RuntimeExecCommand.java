package org.genepattern.server.queue;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterInfo;

/**
 * Run a job using RuntimeExec. This implementation of the CommandExecutor interface
 * waits for the job to complete.
 * 
 * @author pcarr
 */
public class RuntimeExecCommand {
    private static Logger log = Logger.getLogger(RuntimeExecCommand.class);

    /**
     * hashtable of running jobs. key=jobID (as String), value=Process
     */
    private static Hashtable<String, Process> htRunningJobs = new Hashtable<String, Process>();

    /**
     * hashtable of running pipelines. key=jobID (as String), value=Process
     */
    private static Hashtable<String, Process> htRunningPipelines = new Hashtable<String, Process>();
    
    private int exitValue = 0;
    public int getExitValue() {
        return exitValue;
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
    public void runCommand(String commandLine[], Map<String, String> environmentVariables, File runDir, File stdoutFile, File stderrFile, JobInfo jobInfo, String stdin, StringBuffer stderrBuffer) {
        ProcessBuilder pb = null;
        Process process = null;
        String jobID = null;
        try {
            pb = new ProcessBuilder(commandLine);
            Map<String, String> env = pb.environment();
            env.putAll(environmentVariables);
            pb.directory(runDir);

            // spawn the command
            process = pb.start();
            // BUG: there is race condition during a tiny time window between the exec and the close
            // (the lines above and below this comment) during which it is possible for an application
            // to imagine that there might be useful input coming from stdin.
            // This seemed to be the case for Perl 5.0.1, and might be a problem in other applications as well.
            OutputStream standardInStream = process.getOutputStream();
            if (stdin == null) {
                standardInStream.close();
            } 
            else {
                byte[] b = new byte[2048];
                int bytesRead;
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(new File(runDir, stdin));
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
            jobID = "" + jobInfo.getJobNumber();
            htRunningJobs.put(jobID, process);

            // create threads to read from the command's stdout and stderr streams
            Thread outputReader = streamToFile(process.getInputStream(), stdoutFile);
            Thread errorReader = streamToFile(process.getErrorStream(), stderrFile);

            // drain the output and error streams
            outputReader.start();
            errorReader.start();

            // wait for all output before attempting to send it back to the client
            outputReader.join();
            errorReader.join();

            // the process will be dead by now
            process.waitFor();
            exitValue = process.exitValue();
        } 
        catch (Throwable t) {
            log.error("Error in runCommand, reporting to stderr.", t);
            stderrBuffer.append(t.getLocalizedMessage());
            exitValue = -1;
        } 
        finally {
            if (jobID != null) {
                htRunningJobs.remove(jobID);
            }
        }
    }

    /**
     * Creates a new Thread which blocks on reads to an InputStream, appends their output to the given file. 
     * The thread terminates upon EOF from the InputStream.
     * 
     * @param is, InputStream to read from
     * @param file, file to write to
     * @author Jim Lerner
     */
    private Thread streamToFile(final InputStream is, final File file) {
        // create thread to read from a process' output or error stream
        return new Thread() {
            public void run() {
                byte[] b = new byte[2048];
                int bytesRead;
                BufferedOutputStream fis = null;
                boolean wroteBytes = false;
                try {
                    fis = new BufferedOutputStream(new FileOutputStream(file));
                    while ((bytesRead = is.read(b)) >= 0) {
                        wroteBytes = true;
                        fis.write(b, 0, bytesRead);
                    }
                } 
                catch (IOException e) {
                    e.printStackTrace();
                    log.error(e);
                } 
                finally {
                    if (fis != null) {
                        try {
                            fis.flush();
                            fis.close();
                        } 
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (!wroteBytes) {
                        file.delete();
                    }
                }
            }
        };
    }

    /**
     * accepts a jobID and Process object, logging them in the htRunningPipelines Hashtable. When the pipeline
     * terminates, they will be removed from the Hashtable by terminateJob.
     * 
     * @param jobID
     *            job ID number
     * @param p
     *            Process object for running R pipeline
     * @author Jim Lerner
     * @see #terminateJob(String,Hashtable)
     * @see #terminatePipeline(String)
     */
    public static void storeProcessInHash(String jobID, Process p) {
        htRunningPipelines.put(jobID, p);
    }

    /**
     * takes a jobID and a Hashtable in which the jobID is putatively listed, and attempts to terminate the job. Note
     * that Process.destroy() is not always successful. If a process cannot be killed without a "kill -9", it seems not
     * to die from a Process.destroy() either.
     * 
     * @param jobID
     *            JobInfo jobID number
     * @param htWhere
     *            Hashtable in which the job was listed when it was invoked
     * @return true if the job was found, false if not listed (already deleted)
     * @author Jim Lerner
     */
    public static boolean terminateJob(String jobID, Hashtable htWhere) {
        Process p = (Process) htWhere.get(jobID);
        if (p != null) {
            p.destroy();
        }
        return (p != null);
    }

    /**
     * accepts a jobID and attempts to terminate the running pipeline process. Pipelines are notable only in that they
     * are sometimes run not as Omnigene tasks, but as R code that runs through each task serially. The running R
     * process itself is the "pipeline", although it isn't strictly speaking a task. When the pipeline is run as a task,
     * it is not treated as a pipeline in this code. The pipeline behavior only occurs when run via runPipeline.jsp,
     * allowing intermediate results of the task to appear, which would not happen if it were run as a task (all or none
     * for output).
     * 
     * @param jobID
     *            JobInfo jobNumber
     * @return Process of the pipeline if running, else null
     * @author Jim Lerner
     */
    public static Process terminatePipeline(String jobID) {
        Process p = (Process) htRunningPipelines.remove(jobID);
        if (p != null) {
            p.destroy();
        } 
        else {
            p = (Process) htRunningJobs.get(jobID);
            if (p != null) {
                p.destroy();
            }
        }
        return p;
    }

    public static void terminateAll(String message) {
        log.debug(message);
        String jobID;
        Enumeration eJobs;
        int numTerminated = 0;
        for (eJobs = htRunningPipelines.keys(); eJobs.hasMoreElements();) {
            jobID = (String) eJobs.nextElement();
            log.info("Terminating job " + jobID);
            Process p = terminatePipeline(jobID);
            if (p != null) {
                try {
                    updatePipelineStatus(Integer.parseInt(jobID), JobStatus.JOB_ERROR, null);
                } 
                catch (Exception e) { /* ignore */
                }
            }
            numTerminated++;
        }
        for (eJobs = htRunningJobs.keys(); eJobs.hasMoreElements();) {
            jobID = (String) eJobs.nextElement();
            log.info("Terminating job " + jobID);
            terminateJob(jobID, htRunningJobs);
            numTerminated++;
        }
        if (numTerminated > 0) {
            // let the processes terminate, clean up, and record their outputs in the database
            Thread.yield();
        }
    }
    
    /**
     * Changes the JobStatus of a pipeline job, and appends zero or more output parameters (output filenames) to the
     * JobInfo ParameterInfo array for eventual return to the invoker. The jobStatus constants are those defined in
     * edu.mit.wi.omnigene.framework.analysis.JobStatus
     * 
     * This method gets called from many threads which might or might not be wrapped in transactions. This needs to be
     * committed immediately since other threads or processes might be waiting on the update.
     * 
     * @param jobNumber
     *            jobID of the pipeline whose status is to be updated
     * @param jobStatus
     *            new status (eg. JobStatus.PROCESSING, JobStatus.DONE, etc.)
     * @param additionalParams
     *            array of ParameterInfo objects which represent additional output parameters from the pipeline job
     * @throws OmnigeneException
     *             if thrown by Omnigene
     * @throws RemoteException
     *             if thrown by Omnigene
     * @author Jim Lerner
     * @see org.genepattern.webservice.JobStatus
     */
    public static void updatePipelineStatus(int jobNumber, int jobStatus, ParameterInfo[] additionalParams)
    throws OmnigeneException, RemoteException {
        if (log.isDebugEnabled()) {
            log.debug("Updating pipeline status.  job# = " + jobNumber);
        }
        try {
            HibernateUtil.beginTransaction();
            JobInfo jobInfo = GenePatternAnalysisTask.getDS().getJobInfo(jobNumber);
            if (additionalParams != null) {
                for (int i = 0; i < additionalParams.length; i++) {
                    jobInfo.addParameterInfo(additionalParams[i]);
                }
            }
            if (jobStatus < JobStatus.JOB_PENDING) {
                jobStatus = ((Integer) JobStatus.STATUS_MAP.get(jobInfo.getStatus())).intValue();
            }
            GenePatternAnalysisTask.getDS().updateJob(jobNumber, jobInfo.getParameterInfo(), jobStatus);
            HibernateUtil.commitTransaction();
        } 
        catch (OmnigeneException ex) {
            log.error("Error updating pipeline status.  jobNumber=" + jobNumber);
            HibernateUtil.rollbackTransaction();
        }
    }

    /**
     * Changes the JobStatus of a pipeline job, and appends zero or one output parameters (output filenames) to the
     * jobs's JobInfo ParameterInfo array for eventual return to the invoker. This routine is actually invoked from
     * updatePipelineStatus.jsp. The jobStatus constants are those defined in
     * edu.mit.wi.omnigene.framework.analysis.JobStatus
     * 
     * @param jobNumber
     *            jobID of the pipeline whose status is to be updated
     * @param jobStatus
     *            new status (eg. JobStatus.PROCESSING, JobStatus.DONE, etc.)
     * @param name
     *            optional [short] name of filename parameter, ie. without directory information
     * @param additionalFilename
     *            optional filename of output file for this job
     * @throws OmnigeneException
     *             if thrown by Omnigene
     * @throws RemoteException
     *             if thrown by Omnigene
     * @author Jim Lerner
     * @see org.genepattern.webservice.JobStatus
     */
    private static void updatePipelineStatus(int jobNumber, int jobStatus, String name, String additionalFilename)
    throws OmnigeneException, RemoteException {
        if (name != null && additionalFilename != null) {
            ParameterInfo additionalParam = new ParameterInfo();
            additionalParam.setAsOutputFile();
            additionalParam.setName(name);
            additionalParam.setValue(additionalFilename);
            updatePipelineStatus(jobNumber, jobStatus, new ParameterInfo[] { additionalParam });
        } 
        else {
            updatePipelineStatus(jobNumber, jobStatus, null);
        }
    }
}
