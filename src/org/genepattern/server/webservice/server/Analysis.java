/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webservice.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.xml.soap.SOAPException;

import org.apache.axis.MessageContext;
import org.apache.axis.attachments.AttachmentPart;
import org.apache.log4j.Logger;
import org.genepattern.server.AnalysisTask;
import org.genepattern.server.domain.AnalysisJobDAO;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.handler.AddNewJobHandler;
import org.genepattern.server.handler.GetJobStatusHandler;
import org.genepattern.server.util.AuthorizationManagerFactoryImpl;
import org.genepattern.server.util.IAuthorizationManager;
import org.genepattern.server.webservice.GenericWebService;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
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

    private MessageContext context;

    private static Logger log = Logger.getLogger(Analysis.class);

    private IAuthorizationManager authManager = (new AuthorizationManagerFactoryImpl()).getAuthorizationManager();

    /**
     * Default constructor. Constructs a <code>Analysis</code> web service
     * object.
     */
    public Analysis() {
        Thread.yield(); // JL: fixes BUG in which responses from AxisServlet are
        // sometimes empty

    }

    /**
     * Gets the latest versions of all tasks
     * 
     * @return The latest tasks
     * @exception WebServiceException
     *                If an error occurs
     */
    public TaskInfo[] getTasks() throws WebServiceException {
        isAuthorized(getUsernameFromContext(), "Analysis.getTasks");
        Thread.yield(); // JL: fixes BUG in which responses from AxisServlet are
        // sometimes empty
        return new AdminService() {

            protected String getUserName() {
                return getUsernameFromContext();
            }
        }.getLatestTasks();
    }

    /**
     * Saves a record of a job that was executed on the client into the database
     * 
     * @param taskID
     *            the ID of the task to run.
     * @param parameters
     *            the parameters
     * @exception WebServiceException
     *                thrown if problems are encountered
     * @return the job information
     */

    public JobInfo recordClientJob(int taskID, ParameterInfo[] parameters) throws WebServiceException {
        isAuthorized(getUsernameFromContext(), "Analysis.recordClientJob");

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
     *            the ID of the task to run.
     * @param parameters
     *            the parameters
     * @exception WebServiceException
     *                thrown if problems are encountered
     * @return the job information
     */

    public JobInfo recordClientJob(int taskID, ParameterInfo[] parameters, int parentJobNumber)
            throws WebServiceException {
        isAuthorized(getUsernameFromContext(), "Analysis.recordClientJob");
        try {
            AnalysisDAO dao = new AnalysisDAO();
            int jobNo = dao.recordClientJob(taskID, getUsernameFromContext(),
                    org.genepattern.webservice.ParameterFormatConverter.getJaxbString(parameters), parentJobNumber);
            return dao.getJobInfo(jobNo);

        } catch (Exception e) {
            throw new WebServiceException(e);
        }
    }

    public int[] getChildren(int jobId) throws WebServiceException {
        isJobOwnerOrAuthorized(getUsernameFromContext(), jobId, "Analysis.getChildren");

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

    /**
     * Submits an analysis job to be processed. The job is a child job of the
     * supplied parent job.
     * 
     * @param taskID
     *            the ID of the task to run.
     * @param parameters
     *            the parameters to process
     * @param files
     *            a HashMap of input files sent as attachments
     * @param parentJobId
     *            the parent job number
     * @return the job information for this process
     * @exception WebServiceException
     *                if problems are encountered
     */

    public JobInfo submitJob(int taskID, ParameterInfo[] parameters, Map files, int parentJobId)
            throws WebServiceException {
        isAuthorized(getUsernameFromContext(), "Analysis.submitJob");

        try {
            renameInputFiles(parameters, files);
            AddNewJobHandler req = new AddNewJobHandler(taskID, getUsernameFromContext(), parameters, parentJobId);
            return req.executeRequest();
        } catch (Exception e) {
            throw new WebServiceException(e);
        }
    }

    public void wakeupQueue() {
        AnalysisTask.getInstance().wakeupJobQueue();
    }

    private static void logAndThrow(Throwable t) throws WebServiceException {

        if (log != null)
            log.error(t.getMessage());
        t.printStackTrace();
        throw new WebServiceException(t);
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

                    String newFilename = dataHandler.getName() + "_" + orgFilename;
                    File f = new File(dataHandler.getName());
                    File newFile = new File(newFilename);
                    boolean renamed = f.renameTo(newFile);
                    // reset parameter's value with new filename
                    if (renamed) {
                        parameters[x].setValue(newFilename);
                    } else {
                        try {
                            parameters[x].setValue(f.getCanonicalPath());
                        } catch (IOException ioe) {
                            throw new WebServiceException(ioe);
                        }
                    }
                }
            }
    }

    /**
     * Submits an analysis job to be processed.
     * 
     * @param taskID
     *            the ID of the task to run.
     * @param parameters
     *            the parameters to process
     * @param files
     *            a HashMap of input files sent as attachments
     * @return the job information for this process
     * @exception WebServiceException
     *                thrown if problems are encountered
     */
    public JobInfo submitJob(int taskID, ParameterInfo[] parameters, Map files) throws WebServiceException {
        isAuthorized(getUsernameFromContext(), "Analysis.submitJob");

        Thread.yield(); // JL: fixes BUG in which responses from AxisServlet are
        // sometimes empty

        // get the username
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
     * Checks the status of a particular job.
     * 
     * @param jobID
     *            the ID of the task to check
     * @return the job information
     * @exception WebServiceException
     *                thrown if problems are encountered
     */
    public JobInfo checkStatus(int jobID) throws WebServiceException {
        Thread.yield(); // JL: fixes BUG in which responses from AxisServlet are
        // sometimes empty

        isJobOwnerOrAuthorized(getUsernameFromContext(), jobID, "Analysis.checkStatus");

        JobInfo jobInfo = null;

        try {
            GetJobStatusHandler req = new GetJobStatusHandler(jobID);
            jobInfo = req.executeRequest();
        } catch (Throwable t) {
            logAndThrow(t);
        }

        return jobInfo;
    }

    /**
     * Returns the result files of a completed job.
     * 
     * @param jobID
     *            the ID of the job that completed.
     * @return the List of FileWrappers containing the results for this process
     * @exception WebServiceException
     *                thrown if problems are encountered
     */
    public List getResultFiles(int jobID) throws WebServiceException {
        Thread.yield(); // JL: fixes BUG in which responses from AxisServlet are
        // sometimes empty
        isJobOwnerOrAuthorized(getUsernameFromContext(), jobID, "Analysis.getResultFiles");// checks
        // permissions

        JobInfo jobInfo = null;
        ArrayList<String> filenames = new ArrayList<String>();

        try {
            GetJobStatusHandler req = new GetJobStatusHandler(jobID);
            jobInfo = req.executeRequest();
        } catch (Throwable t) {
            logAndThrow(t);
        }

        if (jobInfo != null) {
            ParameterInfo[] parameters = jobInfo.getParameterInfoArray();
            if (parameters != null) {
                for (int x = 0; x < parameters.length; x++) {
                    if (parameters[x].isOutputFile()) {
                        filenames.add(System.getProperty("jobs") + "/" + parameters[x].getValue());
                    }
                }
            }
        }

        ArrayList<FileWrapper> list = new ArrayList<FileWrapper>(filenames.size());

        for (Iterator iterator = filenames.iterator(); iterator.hasNext();) {
            String fn = (String) iterator.next();
            File f = new File(fn);
            DataHandler dataHandler = new DataHandler(new FileDataSource(fn));
            list.add(new FileWrapper(dataHandler.getName(), dataHandler, f.length(), f.lastModified()));
        }

        return list;
    }

    private boolean isJobOwner(String user, int jobId) {
        AnalysisDAO ds = new AnalysisDAO();
        JobInfo jobInfo = ds.getJobInfo(jobId);

        return user.equals(jobInfo.getUserId());
    }

    private void isSameUserOrAuthorized(String user, String requestedUser, String method) throws WebServiceException {
        if (!user.equals(requestedUser)) {
            if (!authManager.checkPermission("administrateServer", user)) {
                throw new WebServiceException("You do not have permission for jobs started by other users.");
            }
        }
    }

    private void isJobOwnerOrAuthorized(String user, int jobId, String method) throws WebServiceException {
        if (!isJobOwner(user, jobId)) {
            isAuthorized(user, method);
        }
    }

    private void isAuthorized(String user, String method) throws WebServiceException {
        if (!authManager.isAllowed(method, user)) {
            throw new WebServiceException("You do not have permission for items owned by other users.");
        }
    }

    /**
     * Terminates the execution of the given job
     * 
     * 
     * 
     * @param jobId
     *            the job id
     */
    public void terminateJob(int jobId) throws WebServiceException {
        try {

            isJobOwnerOrAuthorized(getUsernameFromContext(), jobId, "Analysis.terminateJob");
            AnalysisDAO ds = new AnalysisDAO();

            Process p = org.genepattern.server.genepattern.GenePatternAnalysisTask.terminatePipeline("" + jobId);
            if (p != null) {
                setJobStatus(jobId, JobStatus.ERROR);
            }
            JobInfo[] children = ds.getChildren(jobId);
            for (int i = 0; i < children.length; i++) { // terminate all child
                // jobs
                terminateJob(children[i].getJobNumber());
            }
        } catch (WebServiceException wse) {
            throw wse;
        } catch (Exception e) {
            throw new WebServiceException(e);
        }
    }

    /**
     * Purges the all the input and output files for the given job and expunges
     * the job from the stored history. If the job is running if will be
     * terminated.
     * 
     * user identity is checked by deleteJob to ensure it is the job owner
     * 
     * @param jobId
     *            the job id
     */
    public void purgeJob(int jobId) throws WebServiceException {
        isJobOwnerOrAuthorized(getUsernameFromContext(), jobId, "Analysis.purgeJob");

        try {
            deleteJob(jobId);
            AnalysisDAO ds = new AnalysisDAO();
            JobInfo[] children = ds.getChildren(jobId);
            ds.deleteJob(jobId);

            for (int i = 0; i < children.length; i++) {
                purgeJob(children[i].getJobNumber());
            }
        } catch (WebServiceException wse) {
            throw wse;
        } catch (Exception e) {
            throw new WebServiceException(e);
        }
    }

    /**
     * Deletes the all the input and output files for the given job and removes
     * the job from the stored history. If the job is running if will be
     * terminated.
     * 
     * user identity is checked by terminateJob to ensure it is the job owner or
     * someone with admin provileges
     * 
     * @param jobId
     *            the job id
     */
    public void deleteJob(int jobId) throws WebServiceException {
        isJobOwnerOrAuthorized(getUsernameFromContext(), jobId, "Analysis.deleteJob");
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

    public JobInfo getJob(int jobId) throws WebServiceException {
        isJobOwnerOrAuthorized(getUsernameFromContext(), jobId, "Analysis.getJob");// checks
        // permissions

        try {
            AnalysisDAO ds = new AnalysisDAO();
            return ds.getJobInfo(jobId);
        } catch (org.genepattern.webservice.OmnigeneException oe) {
            throw new WebServiceException(oe);
        }

    }

    /**
     * 
     * Deletes the given output file for the given job and removes the output
     * file from the parameter info array for the job. If <tt>jobId</tt> is a
     * parent job and value was created by it's child, the child will be updated
     * as well. Additionall, if <tt>jobId</tt> is a child job, the parent will
     * be updated too.
     * 
     * @param jobId
     *            the job id
     * @param value
     *            the value of the parameter info object for the output file to
     *            delete
     */
    public void deleteJobResultFile(int jobId, String value) throws WebServiceException {

        isJobOwnerOrAuthorized(getUsernameFromContext(), jobId, "Analysis.deleteJobResultFile");

        AnalysisDAO ds = new AnalysisDAO();
        JobInfo jobInfo = ds.getJobInfo(jobId);
        int beforeDeletionLength = 0;
        ParameterInfo[] params = jobInfo.getParameterInfoArray();
        if (params != null) {
            beforeDeletionLength = params.length;
        }

        jobInfo.setParameterInfoArray(removeOutputFileParameters(jobInfo, value));

        if (jobInfo.getParameterInfoArray().length == beforeDeletionLength) {
            throw new WebServiceException(new java.io.FileNotFoundException());
        }

        int fileCreationJobNumber = jobInfo.getJobNumber();

        String fileName = value;
        int index1 = fileName.lastIndexOf('/');
        int index2 = fileName.lastIndexOf('\\');
        int index = (index1 > index2 ? index1 : index2);
        if (index != -1) {
            fileCreationJobNumber = Integer.parseInt(fileName.substring(0, index));
            fileName = fileName.substring(index + 1, fileName.length());

        }
        String jobDir = org.genepattern.server.genepattern.GenePatternAnalysisTask.getJobDir(String
                .valueOf(fileCreationJobNumber));
        File file = new File(jobDir, fileName);
        if (file.exists()) {
            file.delete();
        }

        ds.updateJob(jobInfo.getJobNumber(), jobInfo.getParameterInfo(), ((Integer) JobStatus.STATUS_MAP.get(jobInfo
                .getStatus())).intValue());

        if (fileCreationJobNumber != jobId) { // jobId is a parent job
            JobInfo childJob = ds.getJobInfo(fileCreationJobNumber);
            childJob.setParameterInfoArray(removeOutputFileParameters(childJob, value));
            ds.updateJob(childJob.getJobNumber(), childJob.getParameterInfo(), ((Integer) JobStatus.STATUS_MAP
                    .get(childJob.getStatus())).intValue());
        } else {
            JobInfo parent = ds.getParent(jobId);
            if (parent != null) { // jobId is a child job
                parent.setParameterInfoArray(removeOutputFileParameters(parent, value));
            }
        }

    }

    /**
     * Sets the status of the given job
     * 
     * @param jobId
     *            the job id
     * @param status
     *            the job status. One of "Pending", "Processing", "Finished, or
     *            "Error"
     */
    public void setJobStatus(int jobId, String status) throws WebServiceException {
        try {
            isJobOwnerOrAuthorized(getUsernameFromContext(), jobId, "Analysis.setJobStatus");

            AnalysisDAO ds = new AnalysisDAO();

            Integer intStatus = (Integer) JobStatus.STATUS_MAP.get(status);
            if (intStatus == null) {
                throw new WebServiceException("Unknown status: " + status);
            }
            ds.updateJobStatus(jobId, intStatus.intValue());
        } catch (Exception e) {
            throw new WebServiceException(e);
        }
    }

    /**
     * 
     * Gets the jobs for the current user
     * 
     * @param username
     *            the username to retrieve jobs for. If <tt>null</tt> all
     *            available jobs are returned.
     * @param maxJobNumber
     *            the maximum job number to include in the returned jobs or -1,
     *            to start at the current maximum job number in the database
     * @param maxEntries
     *            the maximum number of jobs to return
     * @param allJobs
     *            if <tt>true</tt> return all jobs that the given user has
     *            run, otherwise return jobs that have not been deleted
     * 
     * @return the jobs
     */
    public JobInfo[] getJobs(String username, int maxJobNumber, int maxEntries, boolean allJobs)
            throws WebServiceException {
        isSameUserOrAuthorized(getUsernameFromContext(), username, "Analysis.getJobs");

        try {
            AnalysisDAO ds = new AnalysisDAO();
            return ds.getJobs(username, maxJobNumber, maxEntries, allJobs);
        } catch (Exception e) {
            throw new WebServiceException(e);
        }
    }

    /**
     * Returns the username trying to access this service. The username is
     * retrieved from the incoming soap header.
     * 
     * @return a String containing the username or an empty string if one not
     *         found.
     */
    protected String getUsernameFromContext() {
        // get the context then the username from the soap header
        context = MessageContext.getCurrentContext();
        String username = context.getUsername();
        if (username == null)
            username = "";
        return username;
    }

    public String createProvenancePipeline(JobInfo[] jobs, String pipelineName) throws WebServiceException {
        for (int i = 0; i < jobs.length; i++) {
            isJobOwnerOrAuthorized(getUsernameFromContext(), jobs[i].getJobNumber(),
                    "Analysis.createProvenancePipeline");
        }

        String userID = getUsernameFromContext();
        ProvenanceFinder pf = new ProvenanceFinder(userID);
        TreeSet<JobInfo> jobSet = new TreeSet<JobInfo>();
        for (int i = 0; i < jobs.length; i++) {
            jobSet.add(jobs[i]);
        }
        return pf.createProvenancePipeline(jobSet, pipelineName);
    }

    public String createProvenancePipeline(String fileUrlOrJobNumber, String pipelineName) throws WebServiceException {
        String userID = getUsernameFromContext();
        ProvenanceFinder pf = new ProvenanceFinder(userID);

        // check permisison
        JobInfo job = pf.findJobThatCreatedFile(fileUrlOrJobNumber);
        isJobOwnerOrAuthorized(getUsernameFromContext(), job.getJobNumber(), "Analysis.createProvenancePipeline");

        String lsid = pf.createProvenancePipeline(fileUrlOrJobNumber, pipelineName);
        return lsid;
    }

    public JobInfo[] findJobsThatCreatedFile(String fileURLOrJobNumber) throws WebServiceException {
        String userID = getUsernameFromContext();
        ProvenanceFinder pf = new ProvenanceFinder(userID);

        // check permisison
        JobInfo job = pf.findJobThatCreatedFile(fileURLOrJobNumber);
        isJobOwnerOrAuthorized(getUsernameFromContext(), job.getJobNumber(), "Analysis.findJobsThatCreatedFile");

        TreeSet jobSet = (TreeSet) pf.findJobsThatCreatedFile(fileURLOrJobNumber);
        JobInfo[] jobs = new JobInfo[jobSet.size()];
        int i = 0;
        for (Iterator iter = jobSet.iterator(); iter.hasNext(); i++) {
            JobInfo aJob = (JobInfo) iter.next();
            jobs[i] = aJob;
        }
        return jobs;
    }

}
