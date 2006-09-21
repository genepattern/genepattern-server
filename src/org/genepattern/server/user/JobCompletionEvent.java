package org.genepattern.server.user;

import java.util.Date;

import org.genepattern.webservice.JobInfo;

public class JobCompletionEvent {

    Integer id;
    String userID;
    String type;
    int jobNumber;
    int parentJobNumber;
    String taskLSID;
    String taskName;
    Date completionDate;
    long elapsedTime;

    public JobCompletionEvent() {
        
    }
    
    public JobCompletionEvent(JobInfo jobInfo, JobInfo parentJobInfo, 
            Date completionDate, long elapsedTime) {
         userID = jobInfo.getUserId();
         //type
         
         jobNumber = jobInfo.getJobNumber();
         if(parentJobInfo != null) {
             parentJobNumber = parentJobInfo.getJobNumber();
         }
         taskLSID = jobInfo.getTaskLSID();
         taskName = jobInfo.getTaskName();
         this.completionDate = completionDate;
         this.elapsedTime = elapsedTime;

    }

}
