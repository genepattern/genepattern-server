package org.genepattern.server;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.BatchJobDAO;
import org.genepattern.server.executor.CommandManagerFactory;
import org.genepattern.server.executor.JobDeletionException;
import org.genepattern.server.executor.JobSubmissionException;
import org.genepattern.server.executor.JobTerminationException;
import org.genepattern.server.jobqueue.JobQueue;
import org.genepattern.server.jobqueue.JobQueueUtil;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * Submit jobs for execution. Consolidates duplicate code invoked via the web client and the soap client.
 * 
 * @author pcarr
 */
public class JobManager {
    private static Logger log = Logger.getLogger(JobManager.class);
    
    /**
     * Get the working directory for the given job.
     * In GP 3.3.2 and earlier, this is hard-coded based on a configured property.
     * In future releases, the working directory is configurable, and must be stored in the DB.
     * @param jobInfo
     * @return
     */
    public static File getWorkingDirectory(JobInfo jobInfo) throws Exception {
        if (jobInfo == null) {
            throw new IllegalArgumentException("Can't get working directory for jobInfo=null");
        }
        if (jobInfo.getJobNumber() < 0) {
            throw new IllegalArgumentException("Can't get working directory for jobInfo.jobNumber="+jobInfo.getJobNumber());
        }

        File jobDir = null;
        try {
            ServerConfiguration.Context jobContext = ServerConfiguration.Context.getContextForJob(jobInfo);
            File rootJobDir = ServerConfigurationFactory.instance().getRootJobDir(jobContext);
            jobDir = new File(rootJobDir, ""+jobInfo.getJobNumber());
        }
        catch (ServerConfiguration.Exception e) {
            throw new Exception(e.getLocalizedMessage());
        }
        return jobDir;
    }

    /**
     * Create the job directory for a newly added job.
     * This method requires a valid jobId, but does not check if the jobId is valid.
     * 
     * @throws IllegalArgumentException, JobDispatchException
     */
    public static File createJobDirectory(JobInfo jobInfo) throws JobSubmissionException { 
        File jobDir = null;
        try {
            jobDir = getWorkingDirectory(jobInfo);
        }
        catch (Throwable t) {
            throw new JobSubmissionException(t.getLocalizedMessage());
        }
        
        //TODO: record the working dir with the jobInfo and save to DB
        //jobInfo.setWorkingDir(jobDir.getPath());
        // make directory to hold input and output files
        if (!jobDir.exists()) {
            boolean success = jobDir.mkdirs();
            if (!success) {
                throw new JobSubmissionException("Error creating working directory for job #" + jobInfo.getJobNumber() +", jobDir=" + jobDir.getPath());
            }
        } 
        else {
            // clean out existing directory
            if (log.isDebugEnabled()) {
                log.debug("clean out existing directory");
            }
            File[] old = jobDir.listFiles();
            for (int i = 0; old != null && i < old.length; i++) {
                old[i].delete();
            }
        }
        return jobDir;
    }

    /**
     * Adds a new job entry to the ANALYSIS_JOB table, with initial status either PENDING or WAITING.
     * 
     * @param taskID
     * @param userID
     * @param parameterInfoArray
     * @param parentJobID
     * @param jobStatusId
     * @return
     * @throws JobSubmissionException
     */
    static public JobInfo addJobToQueue(final TaskInfo taskInfo, final String userId, final ParameterInfo[] parameterInfoArray, final Integer parentJobNumber, final JobQueue.Status initialJobStatus) 
    throws JobSubmissionException
    {
        JobInfo jobInfo = null;
        try {
            AnalysisDAO ds = new AnalysisDAO();
            Integer jobNo = ds.addNewJob(userId, taskInfo, parameterInfoArray, parentJobNumber);
            if (jobNo != null) {
                jobInfo = ds.getJobInfo(jobNo);
            }
            if (jobInfo == null) {
                throw new JobSubmissionException(
                "addJobToQueue: Operation failed, null value returned for JobInfo");
            } 
            createJobDirectory(jobInfo);
            
            //add record to the internal job queue, for dispatching ...
            JobQueueUtil.addJobToQueue(jobInfo, initialJobStatus);
            
            return jobInfo;
        }
        catch (JobSubmissionException e) {
            throw e;
        }
        catch (Throwable t) {
            throw new JobSubmissionException(t);
        }
    }
    
    static public void terminateJob(boolean isAdmin, String currentUser, int jobId) throws JobTerminationException {
        try {
            boolean canWriteJob = canWriteJob(isAdmin, currentUser, jobId);
            if (!canWriteJob) {
                throw new JobTerminationException("'"+currentUser+"' does not have permission to terminate job #"+jobId);
            }
        }
        catch (WebServiceException e) {
            log.error(e);
            throw new JobTerminationException(e.getLocalizedMessage());
        }
        CommandManagerFactory.getCommandManager().terminateJob(jobId);
    }
    
    static private boolean canWriteJob(boolean isAdmin, String userId, int jobId) throws WebServiceException {
        PermissionsHelper ph = null;
        try {
            ph = new PermissionsHelper(isAdmin, userId, jobId);
            return ph.canWriteJob();
        }
        catch (Throwable t) {
            throw new WebServiceException("server error, unable to check permissions for job #"+jobId, t);
        }
    }

    /**
     * Delete the given job by first terminating it if it is running, deleting its files, and then removing its entry from the database.
     * If necessary, validate that the current user has permission to delete the job.
     * 
     * @param isAdmin
     * @param currentUser
     * @param jobId
     * 
     * @throws WebServiceException
     * @return the list of jobNumbers that were deleted
     */
    static public List<Integer> deleteJob(boolean isAdmin, String currentUser, int jobId) throws WebServiceException {
        canDeleteJob(isAdmin, currentUser, jobId);
        try {
            //first terminate the job including child jobs
            CommandManagerFactory.getCommandManager().terminateJob(jobId);
            //then delete the job including child jobs
            return deleteJobNoCheck(jobId);
        }
        catch (JobTerminationException e) {
            throw new WebServiceException("Error terminating job #"+jobId, e);
        }
        catch (JobDeletionException e) {
            throw new WebServiceException("Error deleting job #"+jobId, e);            
        }
    }
    
    static private void canDeleteJob(boolean isAdmin, String userId, int jobId) throws WebServiceException {
        if (isAdmin) {
            //all admin users have full permissions
            return;
        }
        PermissionsHelper ph = new PermissionsHelper(isAdmin, userId, jobId);
        if (!ph.canWriteJob()) {
            throw new WebServiceException("You do not have permission to edit the job: "+jobId);
        }
    }

    /**
     * Delete the given job and any child jobs if the job is a pipeline This method deletes all input and output files as well as removes the record from the database.
     * Assume permission check has passed and that the job is terminated.
     * 
     * TODO: this method does not clean up uploaded input files (either uploaded via soap or web interface).
     * 
     * @param jobNumber
     * @return the list of jobNumbers that were deleted
     */
    static private List<Integer> deleteJobNoCheck(int jobNumber) throws JobDeletionException {
        List<Integer> deletedJobIds = new ArrayList<Integer>();
        try {
            HibernateUtil.beginTransaction();
            AnalysisDAO dao = new AnalysisDAO();
            deleteJobNoCheck(deletedJobIds, dao, jobNumber);
            HibernateUtil.commitTransaction();
        }
        catch (Exception e) {
            HibernateUtil.rollbackTransaction();
            log.error("Error deleting job #"+jobNumber, e);
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }

        return deletedJobIds;
    }

    /**
     * 
     * @param dao
     * @param jobNumber
     * @return the list of deleted jobs, including this job
     */
    static private void deleteJobNoCheck(List<Integer> deletedJobIds, AnalysisDAO dao, int jobNumber) {
        JobInfo[] children = dao.getChildren(jobNumber);
        for(JobInfo child : children) {
            deleteJobNoCheck(deletedJobIds, dao, child.getJobNumber());
        }
        dao.deleteJob(jobNumber);
        deletedJobIds.add(jobNumber);
        
        BatchJobDAO batchJob = new BatchJobDAO();
        batchJob.markDeletedIfLastJobDeleted(jobNumber);
    }
}
