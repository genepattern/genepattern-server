package org.genepattern.server;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.BatchJobDAO;
import org.genepattern.server.executor.AnalysisJobScheduler;
import org.genepattern.server.executor.JobDeletionException;
import org.genepattern.server.executor.JobSubmissionException;
import org.genepattern.server.executor.JobTerminationException;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
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
     * Create the job directory for a newly added job.
     * This method requires a valid jobId, but does not check if the jobId is valid.
     * 
     * @throws IllegalArgumentException, JobDispatchException
     */
    public static File createJobDirectory(JobInfo jobInfo) throws JobSubmissionException {
        if (jobInfo == null) {
            throw new IllegalArgumentException("Can't create job directory for jobInfo=null");
        }
        if (jobInfo.getJobNumber() < 0) {
            throw new IllegalArgumentException("Can't create job directory for jobInfo.jobNumber="+jobInfo.getJobNumber());
        }
        
        File jobDir = null;
        try {
            ServerConfiguration.Context jobContext = ServerConfiguration.Context.getContextForJob(jobInfo);
            File rootJobDir = ServerConfiguration.instance().getRootJobDir(jobContext);
            jobDir = new File(rootJobDir, ""+jobInfo.getJobNumber());
        }
        catch (ServerConfiguration.Exception e) {
            throw new JobSubmissionException(e.getLocalizedMessage());
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
    static public JobInfo addJobToQueue(final TaskInfo taskInfo, final String userId, final ParameterInfo[] parameterInfoArray, final Integer parentJobNumber, final Integer initialJobStatus) 
    throws JobSubmissionException
    {
        JobInfo jobInfo = null;
        try {
            AnalysisDAO ds = new AnalysisDAO();
            Integer jobNo = ds.addNewJob(userId, taskInfo, parameterInfoArray, parentJobNumber, initialJobStatus);
            if (jobNo != null) {
                jobInfo = ds.getJobInfo(jobNo);
            }
            if (jobInfo == null) {
                throw new JobSubmissionException(
                "addJobToQueue: Operation failed, null value returned for JobInfo");
            } 
            createJobDirectory(jobInfo);
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
        AnalysisJobScheduler.terminateJob(jobId);
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
            AnalysisJobScheduler.terminateJob(jobId);
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

    private static void deleteJobDir(int jobNumber) throws JobDeletionException {
        File jobDir = new File(GenePatternAnalysisTask.getJobDir(""+jobNumber));
        if (!jobDir.canWrite()) {
            throw new JobDeletionException("Error deleting job #"+jobNumber+": gp account does not have write permission on jobDir: "+jobDir.getPath());
        }
        boolean success = deleteDir(jobDir);
        if (!success) {
            throw new JobDeletionException("Error deleting job #"+jobNumber+": did not delete all files from jobDir: "+jobDir.getPath());
        }
    }
    private static boolean deleteDir(File dir) {
        boolean success = true;
        File[] files = dir.listFiles();
        for(File file : files) {
            if (file.isDirectory()) {
                boolean deleted = deleteDir(file);
                if (!deleted) {
                    success = false;
                }
            }
            else {
                boolean deleted = file.delete();
                if (!deleted) {
                    success = false;
                }
            }
        }
        if (success) {
            boolean deleted = dir.delete();
            if (!deleted) {
                success = false; 
            }
        }
        return success;
    }

}
