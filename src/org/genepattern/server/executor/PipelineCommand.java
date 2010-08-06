package org.genepattern.server.executor;

import static org.genepattern.util.GPConstants.STDERR;
import static org.genepattern.util.GPConstants.STDOUT;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.JobInfoManager;
import org.genepattern.server.TaskIDNotFoundException;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.webapp.RunPipelineInThread;
import org.genepattern.server.webapp.RunPipelineInThread.MissingTasksException;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.WebServiceException;

public class PipelineCommand implements Callable<PipelineCommand> {
    private static Logger log = Logger.getLogger(PipelineCommand.class);

    private int jobNumber;
    private RunPipelineInThread rp = new RunPipelineInThread();

    private String[] commandTokens = null;
    private int jobStatus = JobStatus.JOB_PROCESSING;
    private int exitCode = -1;
    private String stdoutFilename = STDOUT;
    private String stderrFilename = STDERR;
    private StringBuffer stderrBuffer = new StringBuffer();
    
    public void setStdoutFile(File stdoutFile) {
        if (stdoutFile != null) {
            stdoutFilename = stdoutFile.getName();
        }
        else {
            stdoutFilename = STDOUT;
        }
    }
    
    public void setStderrFile(File stderrFile) {
        if (stderrFile != null) {
            stderrFilename = stderrFile.getName();
        }
        else {
            stderrFilename = STDERR;
        }
    }

    public void setJobInfo(JobInfo jobInfo) {
        this.jobNumber = jobInfo.getJobNumber();
        // 1) set user id
        rp.setUserId(jobInfo.getUserId());
        // 2) set job id
        rp.setJobId(jobInfo.getJobNumber());
        // 3) set the lsid of the pipeline
        rp.setPipelineTaskId(jobInfo.getTaskID());
        rp.setPipelineTaskLsid(jobInfo.getTaskLSID());
    }
    
    public void setCommandTokens(String[] cmdTokensIn) {
        //arraycopy
        commandTokens = new String[cmdTokensIn.length];
        for(int i=0; i<cmdTokensIn.length; ++i) {
            commandTokens[i] = cmdTokensIn[i];
        }
    }
    
    public int getJobStatus() {
        return jobStatus;
    }
    
    public int getJobNumber() {
        return jobNumber;
    }

    //override equals and hashCode based on the jobNumber
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PipelineCommand) {
            PipelineCommand cmdObj = (PipelineCommand) obj;
            return this.jobNumber == cmdObj.jobNumber;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.jobNumber;
    }

    public PipelineCommand call() throws Exception {
        try {
            runPipeline();
        } 
        catch (Throwable t) {
            //TODO: handle exception
            log.error("Unhandled error thrown in runPipeline", t);
        }
        return this;
    }
    
    public void runPipeline() {
        // 4) set pipeline model
        int taskId = rp.getPipelineTaskId();
        TaskInfo taskInfo = null;
        try {
            taskInfo = JobInfoManager.getTaskInfo(taskId);
        }
        catch (TaskIDNotFoundException e) {
            stderrBuffer.append(e.getLocalizedMessage()+" for pipeline job #"+jobNumber);
            jobStatus = JobStatus.JOB_ERROR;
            exitCode = -1;
            return;
        }
        PipelineModel model = null;
        TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
        if (tia != null) {
            String serializedModel = (String) tia.get(GPConstants.SERIALIZED_MODEL);
            if (serializedModel != null && serializedModel.length() > 0) {
                try {
                    model = PipelineModel.toPipelineModel(serializedModel);
                } 
                catch (Throwable x) {
                    stderrBuffer.append(x.getLocalizedMessage());
                    jobStatus = JobStatus.JOB_ERROR;
                    return;
                }
            }
        }
        rp.setPipelineModel(model);

        // 5) set additional arguments
        Properties additionalArguments = new Properties();
        //HACK: remove all args up to org.genepattern.server.webapp.RunPipelineSoap
        List<String> modifiedCommandTokens = new ArrayList<String>();
        int startIdx = 0;
        for(int i=0; i<commandTokens.length; ++i) {
            if ("org.genepattern.server.webapp.RunPipelineSoap".equals(commandTokens[i])) {
                startIdx = i+1;
                break;
            }
        }
        for(int i=startIdx; i<commandTokens.length; ++i) {
            modifiedCommandTokens.add(commandTokens[i]);
        }
        String[] args = new String[modifiedCommandTokens.size()];
        args = modifiedCommandTokens.toArray(args);
        if (args.length > 2) {
            for (int i = 2; i < args.length; i++) {
                // assume args are in the form name=value
                String arg = args[i];
                StringTokenizer strtok = new StringTokenizer(arg, "=");
                String key = strtok.nextToken();
                StringBuffer valbuff = new StringBuffer("");
                int count = 0;
                while (strtok.hasMoreTokens()) {
                    valbuff.append(strtok.nextToken());
                    if ((strtok.hasMoreTokens()))
                        valbuff.append("=");
                    count++;
                }
                additionalArguments.put(key, valbuff.toString());
            }
        }
        rp.setAdditionalArgs(additionalArguments);
        try {
            rp.runPipeline();
            jobStatus = rp.getJobStatus();
            exitCode = rp.getExitCode();
            stderrBuffer.append(rp.getErrorMessage());
        }
        catch (InterruptedException e) {
            stderrBuffer.append("pipeline interrupted: "+e.getLocalizedMessage());
            jobStatus = JobStatus.JOB_ERROR;
            exitCode = -1;
        }
        catch (MissingTasksException e) {
            stderrBuffer.append(e.getLocalizedMessage());
            jobStatus = JobStatus.JOB_ERROR;
            exitCode = -1;
        }
        catch (WebServiceException e) {
            stderrBuffer.append(e.getLocalizedMessage());
            jobStatus = JobStatus.JOB_ERROR;
            exitCode = -1;
        }
        
        handleJobCompletion();
    }
    
    public void handleJobCompletion() {
        //output stderrBuffer to STDERR file
        if (stderrBuffer != null && stderrBuffer.length() > 0) {
            jobStatus = JobStatus.JOB_ERROR;
            if (exitCode == 0) {
                exitCode = -1;
            }
            String outDirName = GenePatternAnalysisTask.getJobDir(Integer.toString(jobNumber));
            GenePatternAnalysisTask.writeStringToFile(outDirName, STDERR, stderrBuffer.toString());
        }

        try {
            GenePatternAnalysisTask.handleJobCompletion(jobNumber, stdoutFilename, stderrFilename, exitCode, jobStatus, GenePatternAnalysisTask.JOB_TYPE.PIPELINE);
        }
        catch (Exception e) {
            log.error("Error handling job completion for pipeline: "+jobNumber, e);
        }
    }
    
    public void terminatePipelineBeforeStart() {
        stderrBuffer.append("Pipeline cancelled before starting");
        jobStatus = JobStatus.JOB_ERROR;
        exitCode = -1;
        handleJobCompletion();
    }

}
