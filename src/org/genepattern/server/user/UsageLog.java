package org.genepattern.server.user;

import java.util.Date;

import org.genepattern.server.database.HibernateUtil;
import org.genepattern.webservice.JobInfo;

public class UsageLog {

    public static void logJobCompletion(JobInfo jobInfo, JobInfo parentJobInfo, Date completionDate, long elapsedTime) {

        JobCompletionEvent event = new JobCompletionEvent();
        event.setCompletionDate(completionDate);
        event.setElapsedTime(elapsedTime);
        event.setJobNumber(jobInfo.getJobNumber());
        event.setUserId(jobInfo.getUserId());
        event.setTaskLsid(jobInfo.getTaskLSID());
        event.setTaskName(jobInfo.getTaskName());
        event.setCompletionStatus(jobInfo.getStatus());
        if (parentJobInfo == null) {
            ;
            event.setType("TASK");
        }
        else {
            event.setType("PIPELINE");
            event.setParentJobNumber(parentJobInfo.getJobNumber());
        }

        HibernateUtil.getSession().save(event);

    }
}
