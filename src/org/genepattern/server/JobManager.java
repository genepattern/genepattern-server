package org.genepattern.server;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.executor.JobSubmissionException;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterFormatConverter;
import org.genepattern.webservice.ParameterInfo;

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

}
