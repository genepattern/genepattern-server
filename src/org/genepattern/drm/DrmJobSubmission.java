package org.genepattern.drm;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.config.Value;
import org.genepattern.webservice.JobInfo;

import com.google.common.collect.ImmutableList;
import com.google.inject.internal.ImmutableMap;

/**
 * The details of a command line job to submit to the queuing system.
 * 
 * Custom job specification parameters are accessible from this class via some pre-set getters
 * such as getMemory. They are set in the GenePattern Server configuration system
 * (accessed through the GpConfig object). Specific JobRunner implementations such as for LSF, SGE, or PBS/Torque can use the settings
 * when submitting jobs to the queue. See http://slurm.schedmd.com/rosetta.pdf, 'a Rosetta Stone of Workload Managers',
 * for a table of common job specification parameters.
 * 
 * To ensure that this class is immutable, use the DrmJobSubmission.Builder class to create a new instance. 
 * 
 * @see JobRunner JobRunner class for a list of common properties.
 * 
 * 
 * @author pcarr
 *
 */
public class DrmJobSubmission { 
    private static final Logger log = Logger.getLogger(DrmJobSubmission.class);

    private final GpConfig _gpConfig;
    private final GpContext jobContext;
    private final List<String> commandLine;
    private final Map<String, String> environmentVariables;

    private final File workingDir;
    private final File stdoutFile;
    private final File stderrFile;
    private final File stdinFile;
    private final File logFile;
    
    private final String queue;
    private final Memory memory;
    private final String walltimeStr;
    private final Integer nodeCount;
    private final Integer cpuCount;
    private final Value extraArgs;
    
    private DrmJobSubmission(Builder builder) {
        if (builder.gpConfig!=null) {
            this._gpConfig=builder.gpConfig;
        }
        else {
            this._gpConfig=ServerConfigurationFactory.instance();
        }
        if (builder.jobContext!=null) {
            this.jobContext=builder.jobContext;
        }
        else {
            throw new IllegalArgumentException("jobContext==null");
        }
        if (jobContext.getJobInfo()==null) {
            throw new IllegalArgumentException("jobContext.jobInfo==null");
        }
        
        if (builder.commandLine == null || builder.commandLine.size()==0) {
            this.commandLine=Collections.emptyList();
            log.warn("commandLine not set");
        }
        else {
            this.commandLine=ImmutableList.copyOf(builder.commandLine);
        }
        if (builder.environmentVariables==null) {
            this.environmentVariables=Collections.emptyMap();
        }
        else {
            this.environmentVariables=ImmutableMap.copyOf( builder.environmentVariables );
        }
        this.workingDir=builder.workingDir;
        this.stdoutFile=builder.stdoutFile;
        this.stderrFile=builder.stderrFile;
        this.stdinFile=builder.stdinFile;
        this.logFile=builder.logFile;
        
        this.queue=this._gpConfig.getGPProperty(jobContext, JobRunner.PROP_QUEUE);
        this.memory=this._gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY);
        this.walltimeStr=this._gpConfig.getGPProperty(jobContext, JobRunner.PROP_WALLTIME);
        this.nodeCount=this._gpConfig.getGPIntegerProperty(jobContext, JobRunner.PROP_NODE_COUNT);
        this.cpuCount=this._gpConfig.getGPIntegerProperty(jobContext, JobRunner.PROP_CPU_COUNT);
        this.extraArgs=this._gpConfig.getValue(jobContext, JobRunner.PROP_EXTRA_ARGS);
    }
    
    /**
     * The GenePattern job number.
     * @return
     */
    public Integer getGpJobNo() {
        return this.jobContext.getJobNumber();
    }

    /**
     * The job details as a JobInfo instance.
     * @return
     */
    public JobInfo getJobInfo() {
        return this.jobContext.getJobInfo();
    }
    
    /**
     * The command line as a list of args.
     * @return
     */
    public List<String> getCommandLine() {
        return Collections.unmodifiableList(commandLine);
    }

    /**
     * Runtime environment variables.
     * @return
     */
    public Map<String,String> getEnvironmentVariables() {
        return Collections.unmodifiableMap(environmentVariables);
    }

    /**
     * The working directory for the job.
     * @return
     */
    public File getWorkingDir() {
        return workingDir;
    }

    /**
     * Stream stdout into this file.
     * @return
     */
    public File getStdoutFile() {
        return stdoutFile;
    }

    /**
     * Stream stderr into this file.
     * @return
     */
    public File getStderrFile() {
        return stderrFile;
    }

    /**
     * When set, stream stdin from this file.
     * @return
     */
    public File getStdinFile() {
        return stdinFile;
    }

    /**
     * The location of queuing system specific log information, such as the lsf meta data.
     * @return
     */
    public File getLogFile() {
        return logFile;
    }
    
    /**
     * The name of the queue.
     * The default value is null.
     * @return 
     */
    public String getQueue() {
        return queue;
    }

    /**
     * The name of the queue, with default value specified.
     *
     * @param defaultValue
     * @return
     */
    public String getQueue(String defaultValue) {
        if (queue==null) {
            return defaultValue;
        }
        return queue;
    }
    
    /**
     * The memory size required for the given job. For example for an LSF job it means, request a node with at least this much
     * available memory.
     * 
     * Initialized from a string, e.g. '8 gb' or '8gb',
     * units must be one of 'b', 'kb', 'mb', 'gb' or 'tb', case insensitive.
     * 
     * @return the optional Memory setting, default is null.
     */
    public Memory getMemory() {
        return memory;
    }

    /**
     * Get the maximum amount of time a job is allowed to run
     *
     * @return the optional walltime setting, default is null.
     */
    public Walltime getWalltime() {
        return getWalltime(null);
    }

    /**
     * Get the maximum amount of time a job is allowed to run, default is specified
     *
     * @param defaultValue
     * @return
     */
    public Walltime getWalltime(String defaultValue) {
        try {
            if (walltimeStr!=null) {
                return Walltime.fromString(walltimeStr);
            }
            else if (defaultValue!=null) {
                return Walltime.fromString(defaultValue);
            }
        }
        catch (Throwable t) {
            log.error(t);
        }
        return null;
    }

    /**
     * 
     * @return the optional nodeCount setting, default is null.
     */
    public Integer getNodeCount() {
        return nodeCount;
    }

    /**
     * 
     * @return the optional cpuCount setting, default is null.
     */
    public Integer getCpuCount() {
        return cpuCount;
    }

    /**
     * 
     * @return the list of additional JobRunner command line args, default is an empty list.
     */
    public List<String> getExtraArgs() {
        if (extraArgs != null) {
            return extraArgs.getValues();
        }
        return Collections.emptyList();
    }
    
    /**
     * Helper method for working with files relative to the working directory.
     * 
     * @param file, the file (e.g. for stdout redirect)
     * @return the the file is an absolute path, return it, otherwise return a new file 
     *     with the workingDir for the job as the parent directory.
     */
    public File getRelativeFile(final File file) {
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

    //
    // Helper methods for working with the GenePattern Server configuration system
    //
    
    
    public GpConfig getGpConfig() {
        return _gpConfig;
    }
    
    public GpContext getJobContext() {
        return jobContext;
    }
    
    public File getTaskLibDir() {
        return jobContext.getTaskLibDir();
    }
    
    /**
     * Get the runtime setting for the given configuration property. This is a helper method which 
     * uses the GenePattern configuration system to get the value for a property.
     * 
     * @param key
     * @return
     */
    public String getProperty(final String key) {
        return _gpConfig.getGPProperty(jobContext, key);
    }
    
    public Value getValue(final String key) {
        return _gpConfig.getValue(jobContext, key);
    }
    
    public static final class Builder {
        private GpConfig gpConfig=null;
        private GpContext jobContext=null;
        private List<String> commandLine=null;
        private Map<String, String> environmentVariables=null;

        private final File workingDir;
        private File stdoutFile;
        private File stderrFile;
        private File stdinFile;
        private File logFile;

        public Builder(final File workingDir) {
            this.workingDir=workingDir;
        }

        public Builder gpConfig(final GpConfig gpConfig) {
            this.gpConfig=gpConfig;
            return this;
        }

        public Builder jobContext(final GpContext jobContext) {
            this.jobContext=jobContext;
            return this;
        }
        public Builder commandLine(final String[] commandLine) {
            if (this.commandLine != null) {
                throw new IllegalArgumentException("commandLine already set, should only call this method once!");
            }
            this.commandLine=Arrays.asList(commandLine);
            return this;
        }
        
        public Builder addArg(final String commandLineArg) {
            if (this.commandLine == null) {
                this.commandLine = new ArrayList<String>();
            }
            this.commandLine.add(commandLineArg);
            return this;
        }
        
        public Builder environmentVariables(final Map<String,String> environmentVariables) {
            if (this.environmentVariables != null) {
                throw new IllegalArgumentException("environmentVariables already set, should only call this method once!");
            }
            this.environmentVariables = new HashMap<String,String>( environmentVariables );
            return this;
        }
        
        public Builder addEnvVar(final String key, final String value) {
            if (this.environmentVariables==null) {
                this.environmentVariables=new HashMap<String,String>();
            }
            this.environmentVariables.put(key, value);
            return this;
        }
        
        public Builder stdoutFile(final File stdoutFile) {
            this.stdoutFile=stdoutFile;
            return this;
        }
        
        public Builder stderrFile(final File stderrFile) {
            this.stderrFile=stderrFile;
            return this;
        }
        
        public Builder stdinFile(final File stdinFile) {
            this.stdinFile=stdinFile;
            return this;
        }
        
        public Builder logFilename(final String logFilename) {
            if (logFilename==null || logFilename.length()==0) {
                return this;
            }
            return logFile(new File(logFilename));
        }
        public Builder logFile(final File logFile) {
            this.logFile=logFile;
            return this;
        }
        
        public DrmJobSubmission build() {
            return new DrmJobSubmission(this);
        }
    }

}
