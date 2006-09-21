package org.genepattern.server.user;

import java.util.Date;

import org.genepattern.webservice.JobInfo;

public class UsageLog {
    
    public static void logJobCompletion(JobInfo jobInfo, JobInfo parentJobInfo, Date completionDate, long elapsedTime) {
        System.out.println("Job completed in: " + elapsedTime );
        
    }

}
