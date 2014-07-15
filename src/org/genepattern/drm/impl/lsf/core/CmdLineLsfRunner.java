package org.genepattern.drm.impl.lsf.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.drm.JobRunner;
import org.genepattern.drm.Memory;
import org.genepattern.drm.Walltime;
import org.genepattern.server.config.Value;
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.executor.lsf.LsfProperties;
import org.genepattern.webservice.ParameterInfo;

import edu.mit.broad.core.lsf.LocalLsfJob;
import edu.mit.broad.core.lsf.LsfJob;

/**
 * LSF integration via the JobRunner API, the implementation uses external command line wrappers
 * for running the LSF commands.
 *  
 * @author pcarr
 *
 */
public class CmdLineLsfRunner implements JobRunner {
    private static final Logger log = Logger.getLogger(CmdLineLsfRunner.class);

    @Override
    public void stop() {
        log.debug("stopped LsfJobRunner.");
    }

    @Override
    public String startJob(final DrmJobSubmission drmJobSubmission) throws CommandExecutorException {
        if (log.isDebugEnabled()) {
            log.debug("starting job #"+drmJobSubmission.getGpJobNo());
        }
        try {
            LsfJob lsfJob=initLsfJob(drmJobSubmission);
            dispatchUsingExec(lsfJob);
            if (log.isDebugEnabled()) {
                log.debug("added job to queue, lsfJobId="+ (lsfJob==null ? "" : lsfJob.getLsfJobId()));
            }
            return lsfJob.getLsfJobId();
        }
        catch (Throwable t) {
            log.error("Error adding job to LSF queue, gpJobNo="+drmJobSubmission.getGpJobNo(), t);
            throw new CommandExecutorException("Error adding job to LSF queue: "+t.getLocalizedMessage());
        }
    }

    @Override
    public DrmJobStatus getStatus(final DrmJobRecord jobRecord) {
        LsfStatusChecker statusChecker=new LsfStatusChecker(jobRecord);
        try {
            statusChecker.checkStatus();
            DrmJobStatus jobStatus=statusChecker.getStatus();
            return jobStatus;
        }
        catch (CmdException e) {
            log.error("Unexpected error checking status for job="+jobRecord.getGpJobNo(), e);
        }
        catch (InterruptedException e) {
            log.debug("Interrupted");
            Thread.currentThread().interrupt();
            return null;
        }
        return new DrmJobStatus.Builder()
            .jobState(DrmJobState.UNDETERMINED)
            .jobStatusMessage("Error getting status for job")
        .build();
    }

    @Override
    public boolean cancelJob(final DrmJobRecord drmJobRecord) throws Exception {
        log.debug("cancelJob, gpJobNo="+drmJobRecord.getGpJobNo()+", lsfJobNo="+drmJobRecord.getExtJobId());
        final LocalLsfJob lsfJob = new LocalLsfJob();
        lsfJob.setBsubJobId(drmJobRecord.getExtJobId());
        lsfJob.setLsfStatusCode("RUN");

        List<LocalLsfJob> jobs=new ArrayList<LocalLsfJob>();
        jobs.add(lsfJob);
        LocalLsfJob.cancel(jobs);
        return true;
    }
    
    private File getLogFile(DrmJobSubmission drmJobSubmission) {
        return getRelativeFile(drmJobSubmission.getWorkingDir(), drmJobSubmission.getLogFile());
    }

    private File getRelativeFile(final File workingDir, final File file) {
        if (file == null) {
            return null;
        }
        else if (file.isAbsolute()) {
            return file;
        }
        if (workingDir != null) {
            return new File(workingDir, file.getPath());
        }
        return file;
    }
    
    /**
     * Get the command line flags to set the max memory. 
     * This is for the LSF instance used by the GenePattern Server at the Broad Institute,
     * which defines memory flags as integer values corresponding to Gigabytes of RAM.
     * 
     * If the newer (more general) 'job.memory' flag is set, use that, otherwise,
     * use the 'lsf.max.memory' flag.
     * 
     * These flags use two different formats, job.memory expects units to be declared, while
     * the lsf.max.memory must be an integer value. For example,
     * <pre>
       job.memory: 8gb
       lsf.max.memory: 8
     * </pre>
     * 
     * @return
     */
    protected static List<String> getMemFlags(final DrmJobSubmission gpJob) {
        final Memory drmMemory=gpJob.getMemory();
        Integer numGb;
        if (drmMemory==null) {
            try {
                numGb=Integer.parseInt( gpJob.getProperty(LsfProperties.Key.MAX_MEMORY.getKey()) );
            }
            catch (Throwable t) {
                 numGb=LsfProperties.MAX_MEMORY_DEFAULT;
            }
        }
        else {
            numGb = (int) Math.ceil(drmMemory.numGb());
            if (numGb > drmMemory.numGb()) {
                log.debug("Rounded up to nearest int, LSF executor expects an integer value, was "+drmMemory.numGb());
            }
        }

        final List<String> memFlags = new ArrayList<String>();
        memFlags.add("-R");
        memFlags.add("rusage[mem="+numGb+"]");
        memFlags.add("-M");
        memFlags.add(""+numGb);
        
        return memFlags;
    }

    /**
     * Get the pre_exec_command arguments, including the '-E'.
     * For example,
     *     { "-E",  "cd /xchip/gpint/d1 && cd /xchip/gpint/d2" }
     * @param commandLine
     * 
     * @return a List of extra args to include with the bsub command, an empty list if no pre_exec_command is required.
     */
    private List<String> getPreExecCommand(final DrmJobSubmission gpJob) { 
        final String prop=gpJob.getProperty(LsfProperties.Key.USE_PRE_EXEC_COMMAND.getKey());
        if (!Boolean.parseBoolean(prop)) {
            return Collections.emptyList();
        }
        final List<String> rval = new ArrayList<String>();
        final Set<String> filePaths = new HashSet<String>();
        
        final String standardDirectories = gpJob.getProperty(LsfProperties.Key.PRE_EXEC_STANDARD_DIRECTORIES.getKey());
        if (standardDirectories != null) {
            filePaths.addAll(Arrays.asList(standardDirectories.split(";")));
        }
        
        // add libdir
        if (gpJob.getTaskLibDir() != null) {
            filePaths.add( gpJob.getTaskLibDir().getAbsolutePath() );
        }
        //add the working directory for the job
        if (gpJob.getWorkingDir() != null && gpJob.getWorkingDir().exists()) {
            final String path = gpJob.getWorkingDir().getAbsolutePath();
            filePaths.add(path);
        }

        //for each input parameter, if it is a file which exists, add its parent to the list
        for(final ParameterInfo param : gpJob.getJobInfo().getParameterInfoArray()) {
            final String val = param.getValue();
            final File file = new File(val);
            final File parentFile = file.getParentFile();
            if (parentFile != null && parentFile.exists() && parentFile.isDirectory()) {
                final String path = parentFile.getAbsolutePath();
                filePaths.add(path);
            }
        }
        
        if (filePaths.isEmpty()) {
            return rval;
        }
        
        String preExecCommand="";
        boolean first = true;
        for(final String path : filePaths) {
            if (!first) {
                preExecCommand += " && ";
            }
            else {
                first = false;
            }
            preExecCommand += "cd \""+path+"\"";
        }

        log.debug("setting pre_exec_command to: -E \""+preExecCommand+"\"");
        rval.add("-E");
        rval.add(preExecCommand);
        return rval;
    }

    /**
     * Construct a command line string from the list of args.
     * Wrap each arg in single quote characters, make sure to escape any single quote characters in the args.
     * 
     * @param commandLine
     * @return
     */
    private String wrapCommandLineArgsInSingleQuotes(List<String> commandLine) {
        String rval = "";
        boolean first = true;
        for(String arg : commandLine) {
            arg = wrapInSingleQuotes(arg);
            if (first) {
                first = false;
            }
            else {
                rval += " ";
            }
            rval += arg;
        }
        return rval;
    }

    private String wrapInSingleQuotes(String arg) {
        if (arg.contains("'")) {
            // replace each ' with '\''
            arg = arg.replace("'", "'\\''");
        }
        arg = "'"+arg+"'";
        return arg;
    }
    
    protected LsfJob initLsfJob(final DrmJobSubmission gpJob) {
        final Integer jobId=gpJob.getGpJobNo();
        final File runDir=gpJob.getWorkingDir();
        final File stdinFile=gpJob.getStdinFile();
        final File stdoutFile=gpJob.getStdoutFile();
        final File stderrFile=gpJob.getStderrFile();

        final LsfJob lsfJob = new LsfJob();
        lsfJob.setName(""+jobId);
        
        lsfJob.setWorkingDirectory(runDir.getAbsolutePath());
        if (stdinFile != null) {
            lsfJob.setInputFilename(stdinFile.getAbsolutePath());
        }

        String stdoutFilename=null;
        if (stdoutFile != null) {
            stdoutFilename = stdoutFile.getName();
        }
        
        //Note: BroadCore does not handle the %J idiom for the output file
        final String jobReportFilename;
        final File logFile = getLogFile(gpJob);
        if (logFile != null) {
            jobReportFilename=logFile.getPath();
            lsfJob.setOutputFilename(jobReportFilename);
        }
        else {
            jobReportFilename=null;
            lsfJob.setOutputFilename(stdoutFilename);
        }
        
        if (stderrFile != null) {
            lsfJob.setErrorFileName(stderrFile.getName());
        }
        else {
            log.error("Missing required parameter, stderrFile, using 'stderr.txt'");
            lsfJob.setErrorFileName("stderr.txt");
        }
        
        final String lsfProject=gpJob.getProperty(LsfProperties.Key.PROJECT.getKey());
        final String lsfQueue=gpJob.getQueue();
        Value extraBsubArgsFromConfigFile = gpJob.getValue(JobRunner.PROP_EXTRA_ARGS);
        if (extraBsubArgsFromConfigFile == null) {
            extraBsubArgsFromConfigFile = gpJob.getValue(LsfProperties.Key.EXTRA_BSUB_ARGS.getKey());
        }
        
        final String lsfPriority = gpJob.getProperty(LsfProperties.Key.PRIORITY.getKey());
        final String lsfHostOs = gpJob.getProperty(LsfProperties.Key.HOST_OS.getKey());

        lsfJob.setProject(lsfProject);
        lsfJob.setQueue(lsfQueue);
        
        final List<String> extraBsubArgs = new ArrayList<String>();
        final List<String> memFlags=getMemFlags(gpJob);
        if (memFlags!=null) {
            extraBsubArgs.addAll( memFlags );
        }
        
        final Walltime walltime=gpJob.getWalltime();
        if (walltime != null) {
            extraBsubArgs.add("-W");
            extraBsubArgs.add(walltime.formatHoursAndMinutes());
        }

        //special-case, enable per-user job groups
        final Value jobGroup = gpJob.getValue(LsfProperties.Key.JOB_GROUP.getKey());
        if (jobGroup != null) {
            extraBsubArgs.add("-g");
            extraBsubArgs.add(jobGroup.join(" "));
        }
        
        //special-case, enable custom hostname
        Value hostnames=gpJob.getValue(LsfProperties.Key.HOSTNAME.getKey());
        if (hostnames != null && hostnames.getNumValues() > 0) {
            extraBsubArgs.add("-m");
            extraBsubArgs.add(hostnames.join(" "));
        }

        if (gpJob.getCpuCount()!=null) {
            extraBsubArgs.add("-n");
            extraBsubArgs.add(""+gpJob.getCpuCount());
        }
        else if (gpJob.getNodeCount()!=null) {
            extraBsubArgs.add("-n");
            extraBsubArgs.add(""+gpJob.getNodeCount());
        }
        else {
            Integer lsfCpuSlots=gpJob.getGpConfig().getGPIntegerProperty(gpJob.getJobContext(), LsfProperties.Key.CPU_SLOTS.getKey());
            if (lsfCpuSlots != null) {
                log.warn("Using deprecated key '"+LsfProperties.Key.CPU_SLOTS.getKey()+"' Use '"+JobRunner.PROP_CPU_COUNT+"' instead.");
                extraBsubArgs.add("-n");
                extraBsubArgs.add(""+lsfCpuSlots);
            }
        }

        if (extraBsubArgsFromConfigFile != null) {
            if (extraBsubArgsFromConfigFile.getNumValues() > 1) {
                //it's a list
                extraBsubArgs.addAll( extraBsubArgsFromConfigFile.getValues() );
            }
            else {
                String arg = extraBsubArgsFromConfigFile.getValue();
                //ignore null value
                if (arg != null) {
                    extraBsubArgs.add(arg);
                }
            }
        }

        if (lsfPriority != null) {
            extraBsubArgs.add("-sp");
            extraBsubArgs.add(lsfPriority);
        }

        if (lsfHostOs != null) {
            extraBsubArgs.add("-R");
            extraBsubArgs.add("select["+lsfHostOs+"]");
        }
        
        final List<String> preExecArgs=getPreExecCommand(gpJob);
        extraBsubArgs.addAll(preExecArgs);
        lsfJob.setExtraBsubArgs(extraBsubArgs);
        
        if (log.isDebugEnabled()) {
            log.debug("lsf extraBsubArgs: "+extraBsubArgs);
        }

        String commandLineStr = wrapCommandLineArgsInSingleQuotes(gpJob.getCommandLine());

        if (jobReportFilename != null && jobReportFilename.length() > 0) {
            commandLineStr += " >> " + wrapInSingleQuotes(stdoutFilename);
        }
        log.debug("lsf job commandLine: "+commandLineStr);
        lsfJob.setCommand(commandLineStr);
        return lsfJob;
    }
    
    /**
     * Dispatch a job using the utilities which assume that LSF is available as
     * a set of command line tools on the local machine. After invocation the
     * job passed in will have both an LSF job ID and a status set on it unless
     * an exception is thrown.
     *
     * @param job
     */
    private void dispatchUsingExec(final LsfJob job) {
        LocalLsfJob localJob = convert(job);
        localJob.start();
        job.setLsfJobId(localJob.getBsubJobId());
        job.setStatus(localJob.getLsfStatusCode());
        job.setUpdatedDate(new Date()); // set modification timestamp
    }

    /**
     * Creates an LocalLsfJob instance from an LsfJob instance. Checks to see what
     * fields are null on the incoming LsfJob, and only sets non-null properties
     * on the LocalLsfJob.
     */
    private LocalLsfJob convert(LsfJob job) {
        LocalLsfJob localJob = new LocalLsfJob();
        localJob.setCommand(job.getCommand());
        localJob.setName(job.getName());
        localJob.setQueue(job.getQueue());
        localJob.setProject(job.getProject());
        localJob.setExtraBsubArgs(job.getExtraBsubArgs());

        if (job.getWorkingDirectory() != null) {
            localJob.setWorkingDir(new File(job.getWorkingDirectory()));
        }

        if (job.getInputFilename() != null) {
            localJob.setInputFile(getAbsoluteFile(job.getWorkingDirectory(),
                    job.getInputFilename()));
        }

        if (job.getOutputFilename() != null) {
            localJob.setOutputFile(getAbsoluteFile(job.getWorkingDirectory(),
                    job.getOutputFilename()));
        }

        if (job.getErrorFileName() != null) {
            localJob.setErrFile(getAbsoluteFile(job.getWorkingDirectory(), job
                    .getErrorFileName()));
        }

        if (job.getLsfJobId() != null) {
            localJob.setBsubJobId(job.getLsfJobId());
        }

        if (job.getStatus() != null) {
            localJob.setLsfStatusCode(job.getStatus());
        }

        return localJob;
    }

    private File getAbsoluteFile(String dir, String filename) {
        if (filename.startsWith(File.separator)) {
            return new File(filename);
        } else {
            return new File(dir, filename);
        }
    }

}
