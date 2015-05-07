/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp;

import java.util.HashMap;
import java.util.Map;

import org.genepattern.server.EncryptionUtil;
import org.genepattern.server.executor.JobSubmissionException;
import org.genepattern.server.handler.AddNewJobHandler;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * Helper class for submitting a job from the jsp page.
 * 
 * @author pcarr
 * @deprecated - replaced this functionality when we switched the the jQuery based job input form.
 */
class RunJobFromJsp {
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
    
    public String submitJob() throws JobSubmissionException {
        if (isWingsJob()) {
            return submitWingsJob();
        }
        else {
            return submitDefaultJob();
        }
    }

    private JobInfo submitJob(int taskID, ParameterInfo[] parameters) throws JobSubmissionException {
        AddNewJobHandler req = new AddNewJobHandler(taskID, userId, parameters);
        JobInfo jobInfo = req.executeRequest();
        return jobInfo;
    }

    private String submitDefaultJob() throws JobSubmissionException {
        ParameterInfo[] paramInfos = task == null ? null : task.getParameterInfoArray();
        paramInfos = paramInfos == null ? paramInfos = new ParameterInfo[0] : paramInfos;
        JobInfo job = submitJob(task.getID(), paramInfos);
        String jobId = "" + job.getJobNumber();
        return jobId;
    }
    
    private boolean isWingsJob() {
        return 
            task != null &&
            task.getName() != null &&
            task.getName().toLowerCase().startsWith("abstract");
    }

    private String submitWingsJob() throws JobSubmissionException {
        // step 1: get the task id for the wings module, by taskName
        final String wingsTaskName = "wings";
        final String pipelineLSID = task.getLsid();
        TaskInfo wingsTaskInfo = new AdminDAO().getTask(wingsTaskName, userId);

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

        JobInfo job = submitJob(task.getID(), paramInfos);
        String jobId = "" + job.getJobNumber();
        return jobId;
    }

}
