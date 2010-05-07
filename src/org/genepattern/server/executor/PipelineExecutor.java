package org.genepattern.server.executor;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.genepattern.webservice.JobInfo;

/**
 * Run all GP pipelines via with this executor service.
 * 
 * @author pcarr
 */
public class PipelineExecutor implements CommandExecutor {
    
    //initial implementation uses a single thread for each pipeline ... 
    //    ... using code ported from when pipelines were exectuing in separate JVM processes
    private Map<String,PipelineCommand> runningPipelines = new HashMap<String,PipelineCommand>();

    public void runCommand(String[] commandLine,
            Map<String, String> environmentVariables, File runDir,
            File stdoutFile, File stderrFile, JobInfo jobInfo, String stdin,
            StringBuffer stderrBuffer) 
    throws Exception {
        PipelineCommand cmd = new PipelineCommand();
        cmd.setCommandTokens(commandLine);
        cmd.setStdoutFile(stdoutFile);
        cmd.setStderrFile(stderrFile);
        cmd.setJobInfo(jobInfo);
        cmd.setStderrBuffer(stderrBuffer);
        runningPipelines.put(""+jobInfo.getJobNumber(), cmd);
        cmd.runPipeline(); 
        runningPipelines.remove(cmd);
    }

    public void setConfigurationFilename(String filename) {
        // TODO Auto-generated method stub
        
    }

    public void setConfigurationProperties(Properties properties) {
        // TODO Auto-generated method stub
        
    }

    public void start() {
        // TODO Auto-generated method stub
        
    }

    public void stop() {
        // TODO Auto-generated method stub
        
    }

    public void terminateJob(JobInfo jobInfo) throws Exception {
        // TODO Auto-generated method stub
        PipelineCommand cmd = runningPipelines.get(""+jobInfo.getJobNumber());
        cmd.terminate();
        runningPipelines.remove(cmd);
    }

}
