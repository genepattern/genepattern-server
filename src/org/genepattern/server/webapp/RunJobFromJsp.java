package org.genepattern.server.webapp;

import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * Helper class for submitting a job from the jsp page.
 * 
 * @author pcarr
 */
public class RunJobFromJsp {
    private String userId = null;
    private TaskInfo task = null;
    
    public RunJobFromJsp() {    
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public void setTaskInfo(TaskInfo t) {
        this.task = t;
    }

    public String submitJob() throws WebServiceException {
        return submitDefaultJob();
    }

    private String submitDefaultJob() 
        throws WebServiceException
    {
        ParameterInfo[] paramInfos = task == null ? null : task.getParameterInfoArray();
        paramInfos = paramInfos == null ? paramInfos = new ParameterInfo[0] : paramInfos;
        LocalAnalysisClient analysisClient = new LocalAnalysisClient(userId);        
        JobInfo job = analysisClient.submitJob(task.getID(), paramInfos);
        String jobId = "" + job.getJobNumber();
        return jobId;
    }
}
