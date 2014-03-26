package org.genepattern.webservice;

/**
 * Utility methods for working with JobInfo instances.
 * We don't want to make modifications to the JobInfo class because it can break compatibility with the SOAP API.
 * 
 * @author pcarr
 *
 */
public class JobInfoUtil {
    /**
     * helper method which indicates if the job has completed processing.
     */
    public static boolean isFinished(final JobInfo jobInfo) {
        if ( org.genepattern.server.domain.JobStatus.FINISHED.equals(jobInfo.getStatus()) ||
                org.genepattern.server.domain.JobStatus.ERROR.equals(jobInfo.getStatus()) ) {
            return true;
        }
        return false;        
    }
    
    /**
     * helper method which indicates if the job has an error status.
     * @param jobInfo
     * @return
     */
    public static boolean hasError(final JobInfo jobInfo) {
        return org.genepattern.server.domain.JobStatus.ERROR.equals(jobInfo.getStatus());
    }
    
    /**
     * helper method which indicates if the job is pending.
     */
    public static boolean isPending(final JobInfo jobInfo) {
        return org.genepattern.server.domain.JobStatus.PENDING.equals(jobInfo.getStatus());
    }

}
