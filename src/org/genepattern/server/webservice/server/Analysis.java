/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2009) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webservice.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.xml.soap.SOAPException;

import org.apache.axis.MessageContext;
import org.apache.axis.attachments.AttachmentPart;
import org.apache.log4j.Logger;
import org.genepattern.server.PermissionsHelper;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.IGroupMembershipPlugin;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.AnalysisJobDAO;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.executor.CommandExecutor;
import org.genepattern.server.executor.CommandManagerFactory;
import org.genepattern.server.handler.AddNewJobHandler;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.server.webservice.GenericWebService;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.util.StringUtils;
import org.genepattern.webservice.FileWrapper;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * Analysis Web Service.
 * 
 */
public class Analysis extends GenericWebService {

    public enum JobSortOrder {
	JOB_NUMBER, JOB_STATUS, SUBMITTED_DATE, COMPLETED_DATE, USER, MODULE_NAME
    }

    private static Logger log = Logger.getLogger(Analysis.class);

    /**
     * Default constructor. Constructs a <code>Analysis</code> web service object.
     */
    public Analysis() {
    }

    /**
     * Returns the JobInfo object with the given ID, presumably to check status.
     * 
     * @param jobID
     *                the Id of the task to check
     * @return the job information
     * @exception WebServiceException
     *                    thrown if problems are encountered
     */
    public JobInfo checkStatus(int jobID) throws WebServiceException {
        JobInfo jobInfo = null;
        try {
            final String userId = getUsernameFromContext();
            final Boolean isAdmin = AuthorizationHelper.adminJobs(userId);
            jobInfo = (new AnalysisDAO()).getJobInfo(jobID);
            this.canReadJob(isAdmin, userId, jobID);
        } 
        catch (Throwable t) {
            logAndThrow(t);
        }
        return jobInfo;
    }

    /**
     * Create a "provenance" pipeline from the job array. A provenance pipeline reconstructs the sequence of steps
     * performed to generate the output files in the specified job array.
     * 
     * @param jobs
     * @param pipelineName
     * @return The pipeline LSID.
     * @throws WebServiceException
     */
    public String createProvenancePipeline(JobInfo[] jobs, String pipelineName) throws WebServiceException {
	if (!AuthorizationHelper.createPipeline(getUsernameFromContext())) {
	    throw new WebServiceException("You are not authorized to perform this action.");
	}

	String userID = getUsernameFromContext();
	ProvenanceFinder pf = new ProvenanceFinder(userID);
	TreeSet<JobInfo> jobSet = new TreeSet<JobInfo>();
	for (int i = 0; i < jobs.length; i++) {
	    jobSet.add(jobs[i]);
	}
	return pf.createProvenancePipeline(jobSet, pipelineName);
    }

    /**
     * Create a "provenance" pipeline [DESCRIBE PLEASE] from the file url or job number.
     * 
     * @param fileUrlOrJobNumber
     * @param pipelineName
     * @return The pipeline LSID.
     * @throws WebServiceException
     */
    public String createProvenancePipeline(String fileUrlOrJobNumber, String pipelineName) throws WebServiceException {
	if (!AuthorizationHelper.createPipeline(getUsernameFromContext())) {
	    throw new WebServiceException("You are not authorized to perform this action.");
	}
	String userID = getUsernameFromContext();

	ProvenanceFinder pf = new ProvenanceFinder(userID);
	JobInfo job = pf.findJobThatCreatedFile(fileUrlOrJobNumber);
	String lsid = pf.createProvenancePipeline(fileUrlOrJobNumber, pipelineName);
	return lsid;
    }

    /**
     * Deletes the input and output files for the given job and removes the job from the stored history. If the job is
     * running if will be terminated.
     * 
     * user identity is checked by terminateJob to ensure it is the job owner or someone with admin provileges
     * 
     * @param jobId
     *                the job id
     */
    public void deleteJob(int jobId) throws WebServiceException {

	JobInfo jobInfo = getJob(jobId);
	try {
	    terminateJob(jobId);
	    File jobDir = new File(org.genepattern.server.genepattern.GenePatternAnalysisTask.getJobDir(String
		    .valueOf(jobId)));
	    File[] files = jobDir.listFiles();
	    if (files != null) {
		for (int i = 0; i < files.length; i++) {
		    files[i].delete();
		}
	    }
	    jobDir.delete();

	    AnalysisJobDAO aHome = new AnalysisJobDAO();
	    org.genepattern.server.domain.AnalysisJob aJob = aHome.findById(jobId);
	    aJob.setDeleted(true);

	    AnalysisDAO ds = new AnalysisDAO();
	    JobInfo[] children = ds.getChildren(jobId);
	    for (int i = 0; i < children.length; i++) {
		deleteJob(children[i].getJobNumber());
	    }
	} catch (Exception e) {
	    throw new WebServiceException(e);
	}
    }

    /**
     * Deletes the given output file for the given job and removes the output file from the parameter info array for the
     * job. If <tt>jobId</tt> is a parent job and value was created by it's child, the child will be updated as well.
     * Additionally, if <tt>jobId</tt> is a child job, the parent will be updated too.
     * 
     * @param jobId, the job id
     * @param value, the value of the parameter info object for the output file to delete
     */
    public void deleteJobResultFile(int jobId, String value) throws WebServiceException {
        String userId = getUsernameFromContext();
        boolean isAdmin = AuthorizationHelper.adminJobs(userId);
        canWriteJob(isAdmin, userId, jobId);

        AnalysisDAO ds = new AnalysisDAO();
        JobInfo jobInfo = ds.getJobInfo(jobId);
        if (jobInfo == null) {
            throw new WebServiceException("Unable to get jobInfo for jobId="+jobId);
        }

        int beforeDeletionLength = 0;
        ParameterInfo[] params = jobInfo.getParameterInfoArray();
        if (params != null) {
            beforeDeletionLength = params.length;
        }
        jobInfo.setParameterInfoArray(removeOutputFileParameters(jobInfo, value));
        if (jobInfo.getParameterInfoArray().length == beforeDeletionLength) {
            throw new WebServiceException(new FileNotFoundException());
        }

        int fileCreationJobNumber = jobInfo.getJobNumber();

        String fileName = value;
        int index = StringUtils.lastIndexOfFileSeparator(fileName);
        if (index != -1) {
            fileCreationJobNumber = Integer.parseInt(fileName.substring(0, index));
            fileName = fileName.substring(index + 1, fileName.length());
        }
        String jobDir = org.genepattern.server.genepattern.GenePatternAnalysisTask.getJobDir(String.valueOf(fileCreationJobNumber));
        File file = new File(jobDir, fileName);
        if (file.exists()) {
            file.delete();
        }

        ds.updateJob(jobInfo.getJobNumber(), jobInfo.getParameterInfo(), ((Integer) JobStatus.STATUS_MAP.get(jobInfo.getStatus())).intValue());

        if (fileCreationJobNumber != jobId) { // jobId is a parent job
            JobInfo childJob = ds.getJobInfo(fileCreationJobNumber);
            childJob.setParameterInfoArray(removeOutputFileParameters(childJob, value));
            ds.updateJob(childJob.getJobNumber(), childJob.getParameterInfo(), ((Integer) JobStatus.STATUS_MAP.get(childJob.getStatus())).intValue());
        } 
        else {
            JobInfo parent = ds.getParent(jobId);
            if (parent != null) { // jobId is a child job
                parent.setParameterInfoArray(removeOutputFileParameters(parent, value));
            }
        }
    }

    public JobInfo[] findJobsThatCreatedFile(String fileURLOrJobNumber) throws WebServiceException {
	String userID = getUsernameFromContext();
	ProvenanceFinder pf = new ProvenanceFinder(userID);

	JobInfo job = pf.findJobThatCreatedFile(fileURLOrJobNumber);

	Set<JobInfo> jobSet = pf.findJobsThatCreatedFile(fileURLOrJobNumber);
	JobInfo[] jobs = new JobInfo[jobSet.size()];
	int i = 0;
	for (Iterator<JobInfo> iter = jobSet.iterator(); iter.hasNext(); i++) {
	    JobInfo aJob = iter.next();
	    jobs[i] = aJob;
	}
	return jobs;
    }

    public int[] getChildren(int jobId) throws WebServiceException {

	try {
	    AnalysisDAO ds = new AnalysisDAO();
	    JobInfo[] children = ds.getChildren(jobId);

	    int[] jobs = new int[children.length];
	    for (int i = 0; i < children.length; i++) {
		jobs[i] = children[i].getJobNumber();
	    }
	    return jobs;
	} catch (Exception e) {
	    throw new WebServiceException(e);
	}
    }
    
    public JobInfo[] getChildJobInfos(int parentJobId) throws WebServiceException {
        String userId = getUsernameFromContext();
        boolean isAdmin = AuthorizationHelper.adminJobs(userId);
        this.canReadJob(isAdmin, userId, parentJobId);
        AnalysisDAO ds = new AnalysisDAO();
        return ds.getChildren(parentJobId);
    }

    public JobInfo getJob(int jobId) throws WebServiceException {
        boolean checkPermission = true;
        return getJob(jobId, checkPermission);
    }
    
    private JobInfo getJob(int jobId, boolean checkPermission) throws WebServiceException {
        if (checkPermission) {
            String userId = getUsernameFromContext();
            boolean isAdmin = AuthorizationHelper.adminJobs(userId);
            this.canReadJob(isAdmin, userId, jobId);
        }
        AnalysisDAO ds = new AnalysisDAO();
        JobInfo job = ds.getJobInfo(jobId);
        return job;
    }

    /**
     * 
     * Gets the jobs for the specifier user.
     * 
     * @param username
     *                the username to retrieve jobs for. If <tt>null</tt> all available jobs are returned.
     * @param maxJobNumber
     *                the maximum job number to include in the returned jobs or -1, to start at the current maximum job
     *                number in the database
     * @param maxEntries
     *                the maximum number of jobs to return
     * @param includeDeletedJobs
     *                if <tt>true</tt> return all jobs that the given user has run, otherwise return jobs that have
     *                not been deleted
     * 
     * @return the jobs
     */
    public JobInfo[] getJobs(String username, int maxJobNumber, int maxEntries, boolean includeDeletedJobs)
	    throws WebServiceException {
	return getJobs(username, maxJobNumber, maxEntries, includeDeletedJobs, JobSortOrder.JOB_NUMBER, false);
    }

    /**
     * 
     * Gets the jobs for the specified user.
     * 
     * @param username
     *                the username to retrieve jobs for. If <tt>null</tt> all available jobs are returned.
     * @param maxJobNumber
     *                the maximum job number to include in the returned jobs or -1, to start at the current maximum job
     *                number in the database
     * @param maxEntries
     *                the maximum number of jobs to return
     * @param includeDeletedJobs
     *                if <tt>true</tt> return all jobs that the given user has run, otherwise return jobs that have
     *                not been deleted
     * 
     * @return the jobs
     */
    public JobInfo[] getJobs(String username, int maxJobNumber, int maxEntries, boolean includeDeletedJobs, JobSortOrder jobSortOrder, boolean asc) 
    throws WebServiceException 
    {
        //three options:
        if (username != null) {
            // (1) get jobs owned by user
           try {
                AnalysisDAO ds = new AnalysisDAO();
                return ds.getJobs(username, maxJobNumber, maxEntries, includeDeletedJobs, jobSortOrder, asc);
            } 
            catch (Exception e) {
                throw new WebServiceException(e);
            }
        }
        Set<String> groups = null;
        if (!AuthorizationHelper.adminServer()) {
            // (2) get jobs for current user including jobs visible to that user by group permissions
            IGroupMembershipPlugin groupMembership = UserAccountManager.instance().getGroupMembership();
            username = getUsernameFromContext();
            groups = groupMembership.getGroups(username);
        }
        else {
            // (3) current user is an admin, get all jobs            
        }
        try {
            AnalysisDAO ds = new AnalysisDAO();
            return ds.getJobs(username, groups, maxJobNumber, maxEntries, includeDeletedJobs, jobSortOrder, asc);
        } 
        catch (Exception e) {
            throw new WebServiceException(e);
        }
    }

    public JobInfo[] getJobsInGroup(Set<String> groups, int maxJobNumber, int maxEntries, boolean includeDeletedJobs, JobSortOrder jobSortOrder, boolean asc) 
    throws WebServiceException
    {
        try {
            AnalysisDAO ds = new AnalysisDAO();
            return ds.getJobsInGroup(groups, maxJobNumber, maxEntries, includeDeletedJobs, jobSortOrder, asc);
        } 
        catch (Exception e) {
            throw new WebServiceException(e);
        }

    }

    /**
     * Get all of the jobs for the given user, filtered by option list of groups.
     * 
     * @param username
     * @param groups - if groups is empty, only get jobs owned by the user
     * @param maxJobNumber
     * @param maxEntries
     * @param includeDeletedJobs
     * @param jobSortOrder
     * @param asc
     * @return
     */
    public JobInfo[] getJobs(String username, Set<String> groups, int maxJobNumber, int maxEntries, boolean includeDeletedJobs, JobSortOrder jobSortOrder, boolean asc) 
    throws WebServiceException
    {
        try {
            AnalysisDAO ds = new AnalysisDAO();
            return ds.getJobs(username, groups, maxJobNumber, maxEntries, includeDeletedJobs, jobSortOrder, asc);
        } 
        catch (Exception e) {
            throw new WebServiceException(e);
        }
        
    }

    public FileWrapper[] getResultFiles(int jobId, String[] filenames) throws WebServiceException {
        String userId = getUsernameFromContext();
        boolean isAdmin = AuthorizationHelper.adminJobs(userId);
        canReadJob(isAdmin, userId, jobId);
        FileWrapper[] result = new FileWrapper[filenames.length];
        for (int i = 0; i < filenames.length; i++) {
            String path = System.getProperty("jobs") + File.separator + jobId + File.separator + filenames[i];
            File file = new File(path);
            if (file.exists()) {
                result[i] = new FileWrapper(file.getName(), new DataHandler(new FileDataSource(path)), file.length(), file.lastModified());
            } 
            else {
                result[i] = null;
            }
        }
        return result;
    }

    /**
     * Returns the result files of a completed job.
     * 
     * @param jobId
     *                the id of the job that completed.
     * @return the array of FileWrapper objects containing the results for the specified job.
     * @exception WebServiceException
     *                    thrown if problems are encountered
     */
    public FileWrapper[] getResultFiles(int jobId) throws WebServiceException {
        String userId = getUsernameFromContext();
        boolean isAdmin = AuthorizationHelper.adminJobs(userId);
        this.canReadJob(isAdmin, userId, jobId);

        ArrayList<String> filenames = new ArrayList<String>();
        JobInfo jobInfo = getJob(jobId);
        if (jobInfo != null) {
            ParameterInfo[] parameters = jobInfo.getParameterInfoArray();
            if (parameters != null) {
                for (ParameterInfo p : parameters) {
                    if (p.isOutputFile()) {
                        String path = System.getProperty("jobs") + File.separator + p.getValue();
                        File f = new File(path);
                        if (f.exists()) {
                            filenames.add(path);
                        }
                    }
                }
            }
        }

        FileWrapper[] files = new FileWrapper[filenames.size()];
        int i = 0;
        for (Iterator<String> iterator = filenames.iterator(); iterator.hasNext(); i++) {
            String path = iterator.next();
            File file = new File(path);
            files[i] = new FileWrapper(file.getName(), new DataHandler(new FileDataSource(path)), file.length(), file.lastModified());
        }
        
        return files;
    }

    /**
     * Gets the latest versions of all tasks
     * 
     * @return The latest tasks
     * @exception WebServiceException
     *                    If an error occurs
     */
    public TaskInfo[] getTasks() throws WebServiceException {
	return new AdminService() {
	    protected String getUserName() {
		return getUsernameFromContext();
	    }
	}.getLatestTasks();
    }

    /**
     * Purges the all the input and output files for the given job and expunges the job from the stored history. If the
     * job is running if will be terminated.
     * 
     * user identity is checked by deleteJob to ensure it is the job owner
     * 
     * @param jobId
     *                the job id
     */
    public void purgeJob(int jobId) throws WebServiceException {
        String userId = getUsernameFromContext();
        boolean isAdmin = AuthorizationHelper.adminJobs(userId);
        canWriteJob(isAdmin, userId, jobId);

        try {
            deleteJob(jobId);
            AnalysisDAO ds = new AnalysisDAO();
            JobInfo[] children = ds.getChildren(jobId);
            ds.deleteJob(jobId);

            for (int i = 0; i < children.length; i++) {
                purgeJob(children[i].getJobNumber());
            }
        } 
        catch (WebServiceException wse) {
            throw wse;
        } 
        catch (Exception e) {
            throw new WebServiceException(e);
        }
    }

    /**
     * Saves a record of a job that was executed on the client into the database
     * 
     * @param taskID
     *                the ID of the task to run.
     * @param parameters
     *                the parameters
     * @exception WebServiceException
     *                    thrown if problems are encountered
     * @return the job information
     */

    public JobInfo recordClientJob(int taskID, ParameterInfo[] parameters) throws WebServiceException {

	try {
	    AnalysisDAO dao = new AnalysisDAO();
	    int jobNo = dao.recordClientJob(taskID, getUsernameFromContext(),
		    org.genepattern.webservice.ParameterFormatConverter.getJaxbString(parameters), -1);
	    return dao.getJobInfo(jobNo);
	} catch (Exception e) {
	    throw new WebServiceException(e);
	}
    }

    /**
     * Saves a record of a job that was executed on the client into the database
     * 
     * @param taskID
     *                the ID of the task to run.
     * @param parameters
     *                the parameters
     * @exception WebServiceException
     *                    thrown if problems are encountered
     * @return the job information
     */

    public JobInfo recordClientJob(int taskID, ParameterInfo[] parameters, int parentJobNumber)
	    throws WebServiceException {
	try {
	    AnalysisDAO dao = new AnalysisDAO();
	    int jobNo = dao.recordClientJob(taskID, getUsernameFromContext(),
		    org.genepattern.webservice.ParameterFormatConverter.getJaxbString(parameters), parentJobNumber);
	    return dao.getJobInfo(jobNo);

	} catch (Exception e) {
	    throw new WebServiceException(e);
	}
    }

    /**
     * Sets the status of the given job
     * 
     * @param jobId
     *                the job id
     * @param status
     *                the job status. One of "Pending", "Processing", "Finished, or "Error"
     */
    public void setJobStatus(int jobId, String status) throws WebServiceException {
        try {
            log.debug("Analysis.SetStatus " + jobId);
            String userId = getUsernameFromContext();
            boolean isAdmin = AuthorizationHelper.adminJobs(userId);
            canWriteJob(isAdmin, userId, jobId);

            AnalysisDAO ds = new AnalysisDAO();
            Integer intStatus = JobStatus.STATUS_MAP.get(status);
            if (intStatus == null) {
                throw new WebServiceException("Unknown status: " + status);
            }
            ds.updateJobStatus(jobId, intStatus.intValue());
        } 
        catch (Exception e) {
            throw new WebServiceException(e);
        }
    }

    /**
     * Submits an analysis job to be processed.
     * 
     * @param taskID
     *                the ID of the task to run.
     * @param parameters
     *                the parameters to process
     * @param files
     *                a HashMap of input files sent as attachments
     * @return the job information for this process
     * @exception WebServiceException
     *                    thrown if problems are encountered
     */
    public JobInfo submitJob(int taskID, ParameterInfo[] parameters, Map files) throws WebServiceException {
	log.debug("submitJob: " + taskID);
	String username = getUsernameFromContext();

	JobInfo jobInfo = null;

	renameInputFiles(parameters, files);

	try {
	    AddNewJobHandler req = new AddNewJobHandler(taskID, username, parameters);
	    jobInfo = req.executeRequest();
	} catch (Throwable t) {
	    logAndThrow(t);
	}

	return jobInfo;
    }

    /**
     * Submits an analysis job to be processed. The job is a child job of the supplied parent job.
     * 
     * @param taskID
     *                the ID of the task to run.
     * @param parameters
     *                the parameters to process
     * @param files
     *                a HashMap of input files sent as attachments
     * @param parentJobId
     *                the parent job number
     * @return the job information for this process
     * @exception WebServiceException
     *                    if problems are encountered
     */

    public JobInfo submitJob(int taskID, ParameterInfo[] parameters, Map files, int parentJobId)
	    throws WebServiceException {
	try {
	    log.debug("submitJob parentJobId=" + parentJobId);
	    renameInputFiles(parameters, files);
	    log.debug("new AddNewJobHander...");
	    AddNewJobHandler req = new AddNewJobHandler(taskID, getUsernameFromContext(), parameters, parentJobId);
	    log.debug("executeRequest...");

	    JobInfo retVal = req.executeRequest();
	    log.debug("returning job " + retVal.getJobNumber());
	    return retVal;
	} catch (Exception e) {
	    throw new WebServiceException(e);
	}
    }

    /**
     * Terminates the execution of the given job
     *
     * @param jobId
     */
    public void terminateJob(int jobId) throws WebServiceException {
        JobInfo jobInfo = null;
        try {
            String userId = getUsernameFromContext();
            boolean isAdmin = AuthorizationHelper.adminJobs(userId);
            canWriteJob(isAdmin, userId, jobId);
            jobInfo = getJob(jobId);
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
        terminateJob(jobInfo);
    }
    
    private void terminateJob(JobInfo jobInfo) throws WebServiceException {
        if (jobInfo == null) {
            log.error("invalid null arg to terminateJob");
            return;
        }

        //terminate pending jobs immediately
        boolean isPending = isPending(jobInfo);
        if (isPending) {
            log.debug("Terminating PENDING job #"+jobInfo.getJobNumber());
            
            try { 
                AnalysisDAO ds = new AnalysisDAO();
                ds.updateJobStatus(jobInfo.getJobNumber(), JobStatus.JOB_ERROR);
                HibernateUtil.commitTransaction();
            }
            catch (Throwable t) {
                HibernateUtil.rollbackTransaction();
            }
            return;
        } 
        
        //note: don't terminate completed jobs
        boolean isFinished = isFinished(jobInfo); 
        if (isFinished) {
            log.debug("job "+jobInfo.getJobNumber()+"is already finished");
            return;
        }

        try {
            CommandExecutor cmdExec = CommandManagerFactory.getCommandManager().getCommandExecutor(jobInfo);
            cmdExec.terminateJob(jobInfo);
        }
        catch (Throwable t) {
            throw new WebServiceException(t);
        }
    }
    
    private static boolean isPending(JobInfo jobInfo) {
        return isPending(jobInfo.getStatus());
    }

    private static boolean isPending(String jobStatus) {
        return JobStatus.PENDING.equals(jobStatus);
    }

    private static boolean isFinished(JobInfo jobInfo) {
        return isFinished(jobInfo.getStatus());
    }
    
    private static boolean isFinished(String jobStatus) {
        if ( JobStatus.FINISHED.equals(jobStatus) ||
                JobStatus.ERROR.equals(jobStatus) ) {
            return true;
        }
        return false;        
    }

    /**
     * Returns the username trying to access this service. The username is retrieved from the incoming soap header.
     * 
     * @return a String containing the username or an empty string if one not found.
     */
    protected String getUsernameFromContext() {
        // get the context then the username from the soap header
        MessageContext context = MessageContext.getCurrentContext();
        String username = context.getUsername();
        if (username == null) {
            username = "";
        }
        return username;
    }

    private synchronized PermissionsHelper getPermissionsHelper(boolean isAdmin, String userId, int jobId) {
        return new PermissionsHelper(isAdmin, userId, jobId);
    }
    private void canReadJob(boolean isAdmin, String userId, int jobId) throws WebServiceException {
        PermissionsHelper ph = getPermissionsHelper(isAdmin, userId, jobId);
        if (!ph.canReadJob()) {
            throw new WebServiceException("You do not have permission to read the job: "+jobId);
        }
    }
    private void canWriteJob(boolean isAdmin, String userId, int jobId) throws WebServiceException {
        PermissionsHelper ph = getPermissionsHelper(isAdmin, userId, jobId);
        if (!ph.canWriteJob()) {
            throw new WebServiceException("You do not have permission to edit the job: "+jobId);
        }
    }

    private ParameterInfo[] removeOutputFileParameters(JobInfo jobInfo, String value) {
	ParameterInfo[] params = jobInfo.getParameterInfoArray();
	if (params == null) {
	    return new ParameterInfo[0];
	}
	List<ParameterInfo> newParams = new ArrayList<ParameterInfo>();
	for (int i = 0; i < params.length; i++) {
	    if (!(params[i].isOutputFile())) {
		newParams.add(params[i]); // not an output file
	    } else if (!(params[i].getValue().equals(value))) {
		newParams.add(params[i]); // is a different op file

	    }
	}
	return newParams.toArray(new ParameterInfo[newParams.size()]);
    }

    // find any input files and concat axis name with original file name.
    private void renameInputFiles(ParameterInfo[] parameters, Map files) throws WebServiceException {
	if (parameters != null)
	    for (int x = 0; x < parameters.length; x++) {
		if (parameters[x].isInputFile()) {
		    String orgFilename = parameters[x].getValue();
		    Object obj = files.get(orgFilename);
		    DataHandler dataHandler = null;
		    if (obj instanceof AttachmentPart) {
			AttachmentPart ap = (AttachmentPart) obj;
			try {
			    dataHandler = ap.getDataHandler();
			} catch (SOAPException se) {
			    throw new WebServiceException("Error while processing files");
			}
		    } else {
			dataHandler = (DataHandler) obj;
		    }

		    String newFileName = new File(dataHandler.getName() + "_" + orgFilename).getName();
		    File uploadedFile = new File(dataHandler.getName());
		    File uploadedDir = uploadedFile.getParentFile();
		    File newDir = new File(uploadedDir, getUsernameFromContext());

		    if (!newDir.exists()) {
			newDir.mkdir();
		    }
		    File newFile = new File(newDir, newFileName);

		    if (uploadedFile.renameTo(newFile)) {
			try { // set parameter's value with new filename
			    parameters[x].setValue(newFile.getCanonicalPath());
			} catch (IOException e) {
			    parameters[x].setValue(newFile.getPath());
			}
		    } else {
			uploadedFile.delete();
			throw new WebServiceException("Unable to save file " + newFileName + ".");
		    }
		}
	    }
    }

    private static void logAndThrow(Throwable t) throws WebServiceException {
	log.error(t.getMessage(), t);
	throw new WebServiceException(t);
    }

}
