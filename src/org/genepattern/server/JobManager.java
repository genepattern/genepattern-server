package org.genepattern.server;

import java.io.File;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.executor.AnalysisJobScheduler;
import org.genepattern.server.executor.JobDeletionException;
import org.genepattern.server.executor.JobSubmissionException;
import org.genepattern.server.executor.JobTerminationException;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterFormatConverter;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * Submit jobs for execution. Consolidates duplicate code invoked via the web client and the soap client.
 * 
 * @author pcarr
 */
public class JobManager {
    private static Logger log = Logger.getLogger(JobManager.class);
    
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
    static public JobInfo addJobToQueue(final int taskID, final String userID, final ParameterInfo[] parameterInfoArray, final Integer parentJobID, final Integer jobStatusId) 
    throws JobSubmissionException
    {
        JobInfo jobInfo = null;
        try {
            jobInfo = executeRequest(taskID, userID, parameterInfoArray, parentJobID, jobStatusId);
            return jobInfo;
        }
        catch (Throwable t) {
            throw new JobSubmissionException(t);
        }
    }

    /**
     * Creates job. Call this fun. if you need JobInfo object
     *
     * @throws TaskIDNotFoundException
     *             TaskIDNotFoundException
     * @throws OmnigeneException
     * @return <CODE>JobIndo</CODE>
     */
    static private JobInfo executeRequest(int taskID, String userID, ParameterInfo[] parameterInfoArray, Integer parentJobID, Integer jobStatusId) throws TaskIDNotFoundException {
        JobInfo ji = null;
        String parameter_info = ParameterFormatConverter.getJaxbString(parameterInfoArray);
        AnalysisDAO ds = new AnalysisDAO();
        if (parentJobID == null) {
            parentJobID = -1;
        }
        if (jobStatusId == null) {
            jobStatusId = JobStatus.JOB_PENDING;
        }
        
        Integer jobNo = ds.addNewJob(taskID, userID, parameter_info, null, parentJobID, null, jobStatusId);
        ji = ds.getJobInfo(jobNo);

        // Checking for null
        if (ji == null) {
            throw new OmnigeneException(
            "AddNewJobRequest:executeRequest Operation failed, null value returned for JobInfo");
        }

        // Reparse parameter_info before sending to client
        ji.setParameterInfoArray(ParameterFormatConverter.getParameterInfoArray(parameter_info));
        return ji;
    }

    /**
     * Delete the given job by first terminating it if it is running, deleting its files, and the removing its entry from the database.
     * If necessary, validate that the current user has permission to delete the job.
     * 
     * @param isAdmin
     * @param currentUser
     * @param jobId
     * 
     * @throws WebServiceException
     */
    static public void deleteJob(boolean isAdmin, String currentUser, int jobId) throws WebServiceException {
        canDeleteJob(isAdmin, currentUser, jobId);
        try {
            //first terminate the job including child jobs
            AnalysisJobScheduler.terminateJob(jobId);
            //then delete the job including child jobs
            deleteJobNoCheck(jobId);
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
     */
    static private void deleteJobNoCheck(int jobNumber) throws JobDeletionException {
        try {
            HibernateUtil.beginTransaction();
            AnalysisDAO dao = new AnalysisDAO();
            deleteJobNoCheck(dao, jobNumber);
            HibernateUtil.commitTransaction();
        }
        catch (Exception e) {
            HibernateUtil.rollbackTransaction();
            log.error("Error deleting job #"+jobNumber, e);
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }

    static private void deleteJobNoCheck(AnalysisDAO dao, int jobNumber) {
        JobInfo[] children = dao.getChildren(jobNumber);
        for(JobInfo child : children) {
            deleteJobNoCheck(dao, child.getJobNumber());
        }
        dao.deleteJob(jobNumber);
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
