package org.genepattern.drm;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.executor.CommandProperties.Value;
import org.genepattern.webservice.JobInfo;

import com.google.inject.internal.ImmutableMap;

/**
 * The details of a command line job to submit to the queuing system.
 * This includes the command line arguments as well as additional drm job specification parameters
 * which can be used by specific JobRunner implementations such as for LSF, SGE, or PBS/Torque.
 * See http://slurm.schedmd.com/rosetta.pdf, 'a Rosetta Stone of Workload Managers',
 * for a table of common job specification parameters.
 * 
 * To ensure that this class is immutable, use the DrmJobSubmission.Builder class to create a new instance.
 * 
 * The workerName and workerConfig options were developed for Wu, Le-Shin at Indiana Universiry for the
 * integration of GenePattern with their PBS/Torque queuing system. The workerName can be used to select 
 * 
 * @author pcarr
 *
 */
public class DrmJobSubmission { 
    private final Integer gpJobNo;
    private final JobInfo jobInfo;
    private final List<String> commandLine;
    private final Map<String, String> environmentVariables;

    private final File workingDir;
    private final File stdoutFile;
    private final File stderrFile;
    private final File stdinFile;
    private final File logFile;

    private final String queue; 
    private final Memory memory; //default is null
    private final Walltime walltime; //default is null
    private final Integer nodeCount; //default is null
    private final Integer cpuCount; //default is null
    private final List<String> extraArgs;
    private final String workerName; //default is null
    private final Map<?,?> workerConfig; //default is null
    private final Context jobContext;
    
    private DrmJobSubmission(Builder builder) {
        this.gpJobNo=builder.gpJobNo;
        this.jobInfo=builder.jobInfo;
        if (builder.commandLine == null || builder.commandLine.size()==0) {
            throw new IllegalArgumentException("commandLine not set");
        }
        this.commandLine=new ArrayList<String>(builder.commandLine);
        if (builder.environmentVariables==null) {
            this.environmentVariables=Collections.emptyMap();
        }
        else {
            this.environmentVariables=new HashMap<String,String>( builder.environmentVariables );
        }
        this.workingDir=builder.workingDir;
        this.stdoutFile=builder.stdoutFile;
        this.stderrFile=builder.stderrFile;
        this.stdinFile=builder.stdinFile;
        this.logFile=builder.logFile;
        
        this.queue=builder.queue;
        this.memory=builder.memory;
        this.walltime=builder.walltime;
        this.nodeCount=builder.nodeCount;
        this.cpuCount=builder.cpuCount;
        if (builder.extraArgs == null || builder.extraArgs.size()==0) {
            this.extraArgs=Collections.emptyList();
        }
        else {
            this.extraArgs=new ArrayList<String>(builder.extraArgs);
        }
        this.workerName=builder.workerName;
        this.workerConfig=builder.workerConfig; 
        this.jobContext=ServerConfiguration.Context.getContextForJob(jobInfo);
    }
    
    /**
     * The GenePattern job number.
     * @return
     */
    public Integer getGpJobNo() {
        return this.gpJobNo;
    }

    /**
     * The job details as a JobInfo instance.
     * @return
     */
    public JobInfo getJobInfo() {
        return jobInfo;
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
     * 
     * @return the optional walltime setting, default is null.
     */
    public Walltime getWalltime() {
        return walltime;
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
        return Collections.unmodifiableList(extraArgs);
    }
    
    /**
     * Get the runtime setting for the given configuration property. This is a helper method which 
     * uses the GenePattern configuration system to get the value for a property.
     * 
     * @param propName
     * @return
     */
    public String getProperty(final String propName) {
        if (workerConfig != null && workerConfig.containsKey(propName)) {
            return (String) workerConfig.get(propName);
        }
        Value value=ServerConfigurationFactory.instance().getValue(jobContext, propName);
        if (value==null) {
            return null;
        }
        return value.getValue();
    }
    
    public String getWorkerName() {
        return workerName;
    }
    
    public static final class Builder {
        private final Integer gpJobNo;
        private List<String> commandLine=null;
        private Map<String, String> environmentVariables=null;

        private final File workingDir;
        private File stdoutFile;
        private File stderrFile;
        private File stdinFile;
        private File logFile;
        private final JobInfo jobInfo;
        
        private String queue=null;
        private Memory memory=null;
        private Walltime walltime=null;
        private Integer nodeCount=null;
        private Integer cpuCount=null;
        private List<String> extraArgs=null;
        private String workerName;
        private Map<?,?> workerConfig=null;

        public Builder(final JobInfo jobInfo, final File workingDir) {
            this.jobInfo=jobInfo;
            this.gpJobNo=jobInfo.getJobNumber();
            this.workingDir=workingDir;
        }
        public Builder(final Integer gpJobNo, final File workingDir) {
            this.gpJobNo=gpJobNo;
            this.workingDir=workingDir;
            this.jobInfo=new JobInfo();
            this.jobInfo.setJobNumber(gpJobNo);
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
        
        public Builder gpUserId(final String gpUserId) {
            this.jobInfo.setUserId(gpUserId);
            return this;
        }
        
        public Builder lsid(final String lsid) {
            this.jobInfo.setTaskLSID(lsid);
            return this;
        }
        
        public Builder taskName(final String taskName) {
            this.jobInfo.setTaskName(taskName);
            return this;
        }
        
        public Builder queue(final String queue) {
            this.queue=queue;
            return this;
        }

        /**
         * Initialize a memory setting for the job, from a string, usually set in the config file. For example,
         *     'memory: 8 Gb'
         * Which means this job must run on a node with at least 8 Gb of available memory.
         * 
         * @param memorySpec
         * @return
         * @throws IllegalArgumentException
         * @throws NumberFormatException
         */
        public Builder memory(final String memorySpec) throws IllegalArgumentException, NumberFormatException {
            this.memory=Memory.fromString(memorySpec);
            return this;
        }
        
        /**
         * Initialize a wall clock limit for the job, from a string, usually set in the config file. For example,
         *     'drm.walltime: 01:00:00'
         * Which means terminate this job after one hour.
         * 
         * @param wallClockLimitSpec
         * @return
         * @throws Exception
         */
        public Builder walltime(final String wallClockLimitSpec) throws Exception {
            this.walltime=Walltime.fromString(wallClockLimitSpec);
            return this;
        }
        
        public Builder nodeCount(final Integer nodeCount) {
            this.nodeCount=nodeCount;
            return this;
        }
        
        public Builder cpuCount(final Integer cpuCount) {
            this.cpuCount=cpuCount;
            return this;
        }
        
        public Builder addExtraArg(final String extraArg) {
            if (this.extraArgs == null) {
                this.extraArgs = new ArrayList<String>();
            }
            this.extraArgs.add(extraArg);
            return this;
        }
        
        public Builder workerName(final String workerName) {
            this.workerName=workerName;
            return this;
        }
        
        public Builder workerConfig(final Map<?,?> workerConfig) {
            if (workerConfig==null) {
                this.workerConfig=null;
            }
            else {
                this.workerConfig=ImmutableMap.copyOf(workerConfig);
            }
            return this;
        }
        
        public DrmJobSubmission build() {
            return new DrmJobSubmission(this);
        }
    }

}
