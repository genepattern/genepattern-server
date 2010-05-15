package org.genepattern.server.executor;

import static org.genepattern.util.GPConstants.STDERR;
import static org.genepattern.util.GPConstants.STDOUT;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.JobInfoManager;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.webapp.RunPipelineInThread;
import org.genepattern.server.webapp.RunPipelineInThread.MissingTasksException;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.WebServiceException;

public class PipelineCommand {
    private static Logger log = Logger.getLogger(PipelineCommand.class);
    
    private RunPipelineInThread rp = null;

    private String[] commandTokens = null;
    private JobInfo jobInfo = null;
    private int jobStatus = JobStatus.JOB_PROCESSING;
    private int exitCode = -1;
    private File stdoutFile = null;
    private File stderrFile = null;
    private StringBuffer stderrBuffer = new StringBuffer();
    
    public void setStdoutFile(File stdoutFile) {
        this.stdoutFile = stdoutFile;
    }
    public void setStderrFile(File stderrFile) {
        this.stderrFile = stderrFile;
    }

    public void setJobInfo(JobInfo jobInfo) {
        this.jobInfo = jobInfo;
    }
    
    public void setCommandTokens(String[] cmdTokensIn) {
        //arraycopy
        commandTokens = new String[cmdTokensIn.length];
        for(int i=0; i<cmdTokensIn.length; ++i) {
            commandTokens[i] = cmdTokensIn[i];
        }
    }
    
    public void setStderrBuffer(StringBuffer stderrBuffer) {
        this.stderrBuffer = stderrBuffer;
    }

    public StringBuffer getStderrBuffer() {
        return stderrBuffer;
    }
    
    public int getJobStatus() {
        return jobStatus;
    }
    
    public void runPipeline() {
        rp = new RunPipelineInThread();
        // 1) set user id
        rp.setUserId(jobInfo.getUserId());

        // 2) set job id
        rp.setJobId(jobInfo.getJobNumber());

        // 3) set the lsid of the pipeline
        rp.setPipelineTaskLsid(jobInfo.getTaskLSID());

        // 4) set pipeline model
        PipelineModel model = null;
        TaskInfo taskInfo = null;
        try {
            taskInfo = JobInfoManager.getTaskInfo(jobInfo);
        }
        catch (Exception e) {
            stderrBuffer.append(e.getLocalizedMessage());
            jobStatus = JobStatus.JOB_ERROR;
            return;
        }
        if (taskInfo != null) {
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
            jobStatus = JobStatus.JOB_FINISHED;
            exitCode = 0;
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
        
        String stdoutFilename = STDOUT;
        if (stdoutFile != null) {
            stdoutFilename = stdoutFile.getAbsolutePath();
        }
        String stderrFilename = STDERR;
        if (stderrFile != null) {
            stderrFilename = stderrFile.getAbsolutePath();
        }
        
        //output stderrBuffer to STDERR file
        if (stderrBuffer.length() > 0) {
            jobStatus = JobStatus.JOB_ERROR;
            if (exitCode == 0) {
                exitCode = -1;
            }
            String outDirName = GenePatternAnalysisTask.getJobDir(Integer.toString(jobInfo.getJobNumber()));
            GenePatternAnalysisTask.writeStringToFile(outDirName, STDERR, stderrBuffer.toString());
        }
        
        try {
            GenePatternAnalysisTask.handleJobCompletion(jobInfo.getJobNumber(), stdoutFilename, stderrFilename, exitCode, jobStatus, GenePatternAnalysisTask.JOB_TYPE.PIPELINE);
        }
        catch (Exception e) {
            log.error("Error handling job completion for pipeline: "+jobInfo.getJobNumber(), e);
        }
    }
    
    public void terminate() {
        //special case when pipeline thread has not yet started
        if (rp == null) {
            try {
                GenePatternAnalysisTask.handleJobCompletion(jobInfo.getJobNumber(), stdoutFile.getAbsolutePath(), stderrFile.getAbsolutePath(), exitCode, JobStatus.JOB_ERROR, GenePatternAnalysisTask.JOB_TYPE.PIPELINE);
            }
            catch (Exception e) {
                log.error("Error terminating pipeline: "+jobInfo.getJobNumber(), e);
            }
            return;
        }
        if (rp != null) {
            rp.terminate();
        }
    }

}
