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
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.executor.CommandProperties;
import org.genepattern.server.config.Value;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.webservice.server.DirectoryManager;
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
class LsfCommand {
    private static Logger log = Logger.getLogger(LsfCommand.class);
    
    private CommandProperties lsfProperties = null;
    private LsfJob lsfJob = null;
    
    public void setLsfProperties(final CommandProperties p) {
        this.lsfProperties = p;
    }
    
    //example LSF command from the GP production server,
    //bsub -P $project -q "$queue" -R "rusage[mem=$max_memory]" -M $max_memory -m "$hosts" -K -o .lsf_%J.out -e $lsf_err $"$@" \>\> $cmd_out
    
    public void runCommand(final String[] commandLine, final Map<String, String> environmentVariables, final File runDir, final File stdoutFile, final File stderrFile, final JobInfo jobInfo, final File stdinFile, final Class<? extends JobCompletionListener> completionListenerClass) { 
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
        final String jobReportFilename=this.lsfProperties.getProperty(LsfProperties.Key.JOB_REPORT_FILE.getKey());
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
        this.lsfJob.setProject(this.lsfProperties.getProperty(LsfProperties.Key.PROJECT.getKey()));
        this.lsfJob.setQueue(this.lsfProperties.getProperty(LsfProperties.Key.QUEUE.getKey()));
        
        final List<String> extraBsubArgs = new ArrayList<String>();
        final String maxMemory = this.lsfProperties.getProperty(LsfProperties.Key.MAX_MEMORY.getKey(), "2");
        extraBsubArgs.add("-R");
        extraBsubArgs.add("rusage[mem="+maxMemory+"]");
        extraBsubArgs.add("-M");
        extraBsubArgs.add(maxMemory);

        final String cpuSlots=lsfProperties.getProperty(LsfProperties.Key.CPU_SLOTS.getKey());
        if (cpuSlots != null) {
            //expecting an integer
            try {
                //int numCpuSlots=
                        Integer.parseInt(cpuSlots);
            }
            catch (Throwable t) {
                //ignoring exception, let the bsub command deal with it
                log.error("Unexpcted value for "+LsfProperties.Key.CPU_SLOTS.getKey()+"="+cpuSlots);
            }
            extraBsubArgs.add("-n");
            extraBsubArgs.add(cpuSlots);
        }
        
        Value extraBsubArgsFromConfigFile = lsfProperties.get(LsfProperties.Key.EXTRA_BSUB_ARGS.getKey());
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

        final String priority = this.lsfProperties.getProperty(LsfProperties.Key.PRIORITY.getKey());
        if (priority != null) {
            extraBsubArgs.add("-sp");
            extraBsubArgs.add(priority);
        }

        final String host = this.lsfProperties.getProperty(LsfProperties.Key.HOST_OS.getKey());
        if (host != null) {
            extraBsubArgs.add("-R");
            extraBsubArgs.add("select["+host+"]");
        }
        
        final List<String> preExecArgs=getPreExecCommand(jobInfo);
        extraBsubArgs.addAll(preExecArgs);
        this.lsfJob.setExtraBsubArgs(extraBsubArgs);

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
    private List<String> getPreExecCommand(final JobInfo jobInfo) { 
        if (!Boolean.valueOf(this.lsfProperties.getProperty(LsfProperties.Key.USE_PRE_EXEC_COMMAND.getKey()))) {
            return Collections.emptyList();
        }
        final List<String> rval = new ArrayList<String>();

        final Set<String> filePaths = new HashSet<String>();
        
        final String standardDirectories = this.lsfProperties.getProperty(LsfProperties.Key.PRE_EXEC_STANDARD_DIRECTORIES.getKey());
        if (standardDirectories != null) {
        	filePaths.addAll(Arrays.asList(standardDirectories.split(";")));
        }
        
        // add libdir
        final String libDir=getLibDir(jobInfo);
		filePaths.add(libDir);

        //add the working directory for the job
        final String jobDirName = GenePatternAnalysisTask.getJobDir(""+jobInfo.getJobNumber());
        final File jobDir = new File(jobDirName);
        if (jobDir.exists()) {
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

    private String getLibDir(final JobInfo jobInfo) {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        if (log.isDebugEnabled()) {
            log.debug("getting libDir for job #"+jobInfo.getJobNumber()+", isInTransaction="+isInTransaction);
        }
        try {
            final String lsid=jobInfo.getTaskLSID();
            final String libDir=DirectoryManager.getLibDir(lsid);
            File file = new File(libDir);
            return file.getAbsolutePath() + File.separator;
        } 
        catch (Throwable t) {
            log.error("Error getting libDir for job #"+jobInfo.getJobNumber(), t);
            throw new RuntimeException("Exception getting libdir", t); 
        }
        finally {
            if (!isInTransaction) {
                if (log.isDebugEnabled()) {
                    log.debug("job #"+jobInfo.getJobNumber()+", isInTransaction was "+isInTransaction+", closing DB session");
                }
                HibernateUtil.closeCurrentSession();
            }
        }
    }
}
