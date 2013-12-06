package org.genepattern.drm;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.genepattern.webservice.JobInfo;

/**
 * The description of a command line job to submit to the queuing system.
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
    }
    
    public Integer getGpJobNo() {
        return this.gpJobNo;
    }
    
    public JobInfo getJobInfo() {
        return jobInfo;
    }
    
    public List<String> getCommandLine() {
        return Collections.unmodifiableList(commandLine);
    }
    
    public Map<String,String> getEnvironmentVariables() {
        return Collections.unmodifiableMap(environmentVariables);
    }
    
    public File getWorkingDir() {
        return workingDir;
    }
    
    public File getStdoutFile() {
        return stdoutFile;
    }
    
    public File getStderrFile() {
        return stderrFile;
    }
    
    public File getStdinFile() {
        return stdinFile;
    }
    
    public File getLogFile() {
        return logFile;
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
                throw new IllegalArgumentException("environmentVariables alread set, should only call this method once!");
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
        
        public DrmJobSubmission build() {
            return new DrmJobSubmission(this);
        }
    }

}
