/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

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
