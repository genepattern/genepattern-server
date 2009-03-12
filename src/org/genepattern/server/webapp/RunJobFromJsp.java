package org.genepattern.server.webapp;

import java.util.HashMap;
import java.util.Map;

import org.genepattern.server.EncryptionUtil;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
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
        if (isWingsJob()) {
            return submitWingsJob();
        }
        else {
            return submitDefaultJob();
        }
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
    
    private boolean isWingsJob() {
        return 
            task != null &&
            task.getName() != null &&
            task.getName().toLowerCase().startsWith("abstract");
    }

    private String submitWingsJob() throws WebServiceException {
        LocalAdminClient adminClient = new LocalAdminClient(userId);
        LocalAnalysisClient analysisClient = new LocalAnalysisClient(userId);

        // step 1: get the task id for the wings module
        final String wingsTaskName = "wings";
        final String pipelineLSID = task.getLsid();
        TaskInfo wingsTaskInfo = adminClient.getTask(wingsTaskName);

        // step 2: build input parameters for the wings module
        String passwordKey = null;
        if (userId != null) {
            User user = new UserDAO().findById(userId);
            if (user != null) {
                passwordKey = EncryptionUtil.getInstance().pushPipelineUserKey(user);
            }
        }

        Map<String, String> wingsInputs = new HashMap<String, String>();
        wingsInputs.put("pipelineLsid", pipelineLSID);
        wingsInputs.put("gp_user_id", userId);
        wingsInputs.put("gp_password", passwordKey != null ? passwordKey : "");

        for (ParameterInfo pinfo : wingsTaskInfo.getParameterInfoArray()) {
            if (wingsInputs.containsKey(pinfo.getName())) {
                pinfo.setValue(wingsInputs.get(pinfo.getName()));
            }
        }

        ParameterInfo[] paramInfos = 
            wingsTaskInfo == null ? null
                : wingsTaskInfo.getParameterInfoArray();
        paramInfos = 
            paramInfos == null ? paramInfos = new ParameterInfo[0]
                : paramInfos;

        JobInfo job = analysisClient.submitJob(task.getID(), paramInfos);
        String jobId = "" + job.getJobNumber();
        return jobId;
    }

}
