package org.genepattern.server.executor.lsf;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.drm.JobRunner;
import org.genepattern.drm.Memory;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.Value;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;

import edu.mit.broad.core.lsf.LsfJob;
import edu.mit.broad.core.lsf.LsfJob.JobCompletionListener;

/**
 * Run the given command line on the LSF queue. This class depends on another thread which monitors the LSF queue for completed jobs.
 * 
 * @author pcarr
 */
class LsfCommand2 {
    private static Logger log = Logger.getLogger(LsfCommand2.class);

    //input params
    private final GpConfig gpConfig;
    private final GpContext gpContext;
    //output values
    private LsfJob lsfJob = null;

    public LsfCommand2(final GpConfig gpConfig, final GpContext gpContext) {
        this.gpConfig=gpConfig;
        this.gpContext=gpContext;
    }
    
    /**
     * Get the command line flags to set the max memory. 
     * This is for the LSF instance used by the GenePattern Server at the Broad Institute,
     * which defines memory flags as integer values corresponding to Gigabytes of RAM.
     * 
     * IF the newer (more general) 'drm.memory' flag is set, use that, otherwise,
     * use the 'lsf.max.memory' flag.
     * 
     * These flags use two different formats, drm.memory expects units to be declared, while
     * the lsf.max.memory must be an integer value. For example,
     * <pre>
       drm.memory: 8gb
       lsf.max.memory: 8
     * </pre>
     * 
     * @return
     */
    protected static List<String> getMemFlags(final GpConfig gpConfig, final GpContext gpContext) {
        final Memory drmMemory=gpConfig.getGPMemoryProperty(gpContext, JobRunner.PROP_MEMORY);
        final Integer numGb;
        if (drmMemory==null) {
             numGb = gpConfig.getGPIntegerProperty(gpContext, LsfProperties.Key.MAX_MEMORY.getKey(), LsfProperties.MAX_MEMORY_DEFAULT);
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

    //example LSF command from the GP production server,
    //bsub -P $project -q "$queue" -R "rusage[mem=$max_memory]" -M $max_memory -m "$hosts" -K -o .lsf_%J.out -e $lsf_err $"$@" \>\> $cmd_out
    
    public void runCommand(final String[] commandLine, 
            final Map<String, String> environmentVariables, 
            final File runDir, 
            final File stdoutFile, 
            final File stderrFile, 
            final File stdinFile, 
            final Class<? extends JobCompletionListener> completionListenerClass) 
    { 
        final JobInfo jobInfo=gpContext.getJobInfo();
        final long jobId = jobInfo != null ? jobInfo.getJobNumber() : -1L;

        this.lsfJob = new LsfJob();
        this.lsfJob.setName(""+jobId);
        this.lsfJob.setWorkingDirectory(runDir.getAbsolutePath());
        if (stdinFile != null) {
            this.lsfJob.setInputFilename(stdinFile.getAbsolutePath());
        }

        String stdoutFilename=null;
        if (stdoutFile != null) {
            stdoutFilename = stdoutFile.getName();
        }
        if (!GPConstants.STDOUT.equals(stdoutFilename)) {
            log.debug("custom stdout file="+stdoutFilename);
        }
        
        //Note: BroadCore does not handle the %J idiom for the output file
        final String jobReportFilename=gpConfig.getGPProperty(gpContext, LsfProperties.Key.JOB_REPORT_FILE.getKey());
        if (jobReportFilename != null && jobReportFilename.length() > 0 ) {
            this.lsfJob.setOutputFilename(jobReportFilename);
        }
        else {
            this.lsfJob.setOutputFilename(stdoutFilename);
        }
        
        if (stderrFile != null) {
            this.lsfJob.setErrorFileName(stderrFile.getName());
        }
        else {
            log.error("Missing required parameter, stderrFile, using 'stderr.txt'");
            this.lsfJob.setErrorFileName("stderr.txt");
        }
        final String lsfProject=gpConfig.getGPProperty(gpContext, LsfProperties.Key.PROJECT.getKey());
        final String lsfQueue=gpConfig.getGPProperty(gpContext, LsfProperties.Key.QUEUE.getKey());
        final String lsfCpuSlots=gpConfig.getGPProperty(gpContext, LsfProperties.Key.CPU_SLOTS.getKey());
        final Value extraBsubArgsFromConfigFile = gpConfig.getValue(gpContext, LsfProperties.Key.EXTRA_BSUB_ARGS.getKey());
        final String lsfPriority = gpConfig.getGPProperty(gpContext, LsfProperties.Key.PRIORITY.getKey());
        final String lsfHostOs = gpConfig.getGPProperty(gpContext, LsfProperties.Key.HOST_OS.getKey());

        this.lsfJob.setProject(lsfProject);
        this.lsfJob.setQueue(lsfQueue);
        
        final List<String> extraBsubArgs = new ArrayList<String>();
        List<String> memFlags=LsfCommand2.getMemFlags(gpConfig, gpContext);
        if (memFlags!=null) {
            for(final String memFlag : memFlags) {
                extraBsubArgs.add( memFlag );
            }
        }

        if (lsfCpuSlots != null) {
            //expecting an integer
            try {
                Integer.parseInt(lsfCpuSlots);
            }
            catch (Throwable t) {
                //ignoring exception, let the bsub command deal with it
                log.error("Unexpcted value for "+LsfProperties.Key.CPU_SLOTS.getKey()+"="+lsfCpuSlots);
            }
            extraBsubArgs.add("-n");
            extraBsubArgs.add(lsfCpuSlots);
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
        
        final List<String> preExecArgs=getPreExecCommand(jobInfo, runDir);
        extraBsubArgs.addAll(preExecArgs);
        this.lsfJob.setExtraBsubArgs(extraBsubArgs);
        
        if (log.isDebugEnabled()) {
            log.debug("lsf extraBsubArgs: "+extraBsubArgs);
        }

        String commandLineStr = wrapCommandLineArgsInSingleQuotes(commandLine);

        if (jobReportFilename != null && jobReportFilename.length() > 0) {
            commandLineStr += " >> " + wrapInSingleQuotes(stdoutFilename);
        }
        log.debug("lsf job commandLine: "+commandLineStr);
        this.lsfJob.setCommand(commandLineStr);
        this.lsfJob.setCompletionListenerName(completionListenerClass.getName());
    }
    
    public void prepareToTerminate(final JobInfo jobInfo) {
        final int jobId = jobInfo != null ? jobInfo.getJobNumber() : -1;
        this.lsfJob = new LsfJob();
        //note: use the name of the job (the bsub -J arg) to map the GP JOB ID to the JOB_LSF table
        //    the internalJobId is (by default) configured as a primary key with a sequence
        this.lsfJob.setName(""+jobId);
    }
    
    public LsfJob getLsfJob() {
        return this.lsfJob;
    }
    
    /**
     * Construct a command line string from the list of args.
     * Wrap each arg in single quote characters, make sure to escape any single quote characters in the args.
     * 
     * @param commandLine
     * @return
     */
    private String wrapCommandLineArgsInSingleQuotes(String[] commandLine) {
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
    
    /**
     * Get the pre_exec_command arguments, including the '-E'.
     * For example,
     *     { "-E",  "cd /xchip/gpint/d1 && cd /xchip/gpint/d2" }
     * @param commandLine
     * 
     * @return a List of extra args to include with the bsub command, an empty list if no pre_exec_command is required.
     */
    private List<String> getPreExecCommand(final JobInfo jobInfo, final File jobDir) { 
        if (! gpConfig.getGPBooleanProperty(gpContext, LsfProperties.Key.USE_PRE_EXEC_COMMAND.getKey())) {
            return Collections.emptyList();
        }
        final List<String> rval = new ArrayList<String>();
        final Set<String> filePaths = new HashSet<String>();
        
        final String standardDirectories = gpConfig.getGPProperty(gpContext, LsfProperties.Key.PRE_EXEC_STANDARD_DIRECTORIES.getKey());
        if (standardDirectories != null) {
        	filePaths.addAll(Arrays.asList(standardDirectories.split(";")));
        }
        
        // add libdir
        if (gpContext.getTaskLibDir() != null) {
            filePaths.add( gpContext.getTaskLibDir().getAbsolutePath() );
        }
        //add the working directory for the job
        if (jobDir != null && jobDir.exists()) {
            final String path = jobDir.getAbsolutePath();
            filePaths.add(path);
        }

        //for each input parameter, if it is a file which exists, add its parent to the list
        for(final ParameterInfo param : jobInfo.getParameterInfoArray()) {
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

}
