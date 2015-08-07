/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.user;

import java.util.Date;

import org.genepattern.server.database.HibernateUtil;
import org.genepattern.webservice.JobInfo;

public class UsageLog {

    public static void logJobCompletion(JobInfo jobInfo, Date completionDate, long elapsedTime) {

        JobCompletionEvent event = new JobCompletionEvent();
        event.setCompletionDate(completionDate);
        event.setElapsedTime(elapsedTime);
        event.setJobNumber(jobInfo.getJobNumber());
        event.setUserId(jobInfo.getUserId());
        event.setTaskLsid(jobInfo.getTaskLSID());
        event.setTaskName(jobInfo.getTaskName());
        event.setCompletionStatus(jobInfo.getStatus());
        final int parentJobNo=jobInfo._getParentJobNumber();
        if (parentJobNo < 0) {
            event.setType("TASK");
        }
        else {
            event.setType("PIPELINE");
            event.setParentJobNumber(parentJobNo);
        }

        HibernateUtil.getSession().save(event);

    }
}
