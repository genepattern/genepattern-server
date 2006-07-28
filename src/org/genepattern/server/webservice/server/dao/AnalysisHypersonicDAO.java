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

package org.genepattern.server.webservice.server.dao;

import java.io.File;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.sql.*;
import java.util.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.genepattern.server.*;
import org.genepattern.server.webservice.server.AnalysisJobDataSource;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.*;
import org.genepattern.webservice.JobStatus;
import org.hibernate.*;

import com.sun.rowset.CachedRowSetImpl;

/**
 * AnalysisHypersonicDAO.java
 * 
 * @author rajesh kuttan, Hui Gong
 * @version
 */
public class

AnalysisHypersonicDAO extends BaseDAO  {

    private static Logger log = Logger.getLogger(AnalysisHypersonicDAO.class);

    private AdminDAO adminDAO = new AdminHSQLDAO();

    /** Creates new AnalysisHypersonicAccess */
    public AnalysisHypersonicDAO() {
        log.setLevel((Level) Level.FATAL);
    }

    /**
     * 
     * @param maxJobCount
     *            max. job count
     * @throws OmnigeneException
     * @throws RemoteException
     * @return JobInfo Vector
     */
    public Vector getWaitingJob(int maxJobCount) throws OmnigeneException {
        Vector jobVector = new Vector();
        PreparedStatement stat = null;
        ResultSet resultSet = null;

        // initializing maxJobCount, if it has invalid value
        if (maxJobCount <= 0) {
            maxJobCount = 1;
        }

        try {

            // Validating taskID is not done here bcos.
            // assuming once job is submitted, it should be executed even if
            // taskid is removed from task master

            // Query job table for waiting job
            stat = getSession().connection().prepareStatement(
                    "SELECT job_no,analysis_job.task_id,analysis_job.parameter_info,analysis_job.user_id, "
                            + " analysis_job.task_lsid, analysis_job.task_name FROM analysis_job, task_master "
                            + " where analysis_job.task_id=task_master.task_id and  status_id = ? "
                            + " order by date_submitted");
            stat.setInt(1, JOB_WAITING_STATUS);
            resultSet = stat.executeQuery();

            int jobNo = 0, taskID = 0;
            String parameter_info = "";
            String lsid = null;
            boolean recordFoundFlag = false;

            ParameterFormatConverter parameterFormatConverter = new ParameterFormatConverter();
            int i = 1;
            // Moves to the next record until no more records
            while (resultSet.next() && i++ <= maxJobCount) {
                recordFoundFlag = true;
                jobNo = resultSet.getInt(1);
                taskID = resultSet.getInt(2);
                parameter_info = resultSet.getString(3);
                lsid = resultSet.getString("task_lsid");
                String taskName = resultSet.getString("task_name");

                updateJobStatus(jobNo, PROCESSING_STATUS);

                // Add waiting job info to vector, for AnalysisTask
                ParameterInfo[] params = parameterFormatConverter.getParameterInfoArray(parameter_info);
                JobInfo singleJobInfo = new JobInfo(jobNo, taskID, null, null, null, params, resultSet.getString(4),
                        lsid, taskName);
                jobVector.add(singleJobInfo);
            }

        }
        catch (Exception e) {
            log.error("AnalysisHypersonicDAO: getWaitingJob failed", e);
            throw new OmnigeneException(e.getMessage());
        }

        finally {
            cleanupJDBC(resultSet, stat);
        }

        return jobVector;

    }

    /**
     * 
     */
    public String getTemporaryPipelineName(int jobNumber) throws OmnigeneException {

        String hql = "select taskName from org.genepattern.server.webservice.server.dao.AnalysisJob where jobNo = :jobNumber";
        Query q = getSession().createQuery(hql);
        q.setInteger("jobNumber", jobNumber);
        return (String) q.uniqueResult();
    }

    /**
     * 
     */
    public JobInfo createTemporaryPipeline(String user_id, String parameter_info, String pipelineName, String lsid)
            throws OmnigeneException {
        Integer jobNo = addNewJob(UNPROCESSABLE_TASKID, user_id, parameter_info, pipelineName, null, lsid);
        return this.getJobInfo(jobNo);
    }

    /**
     * 
     */
    public JobInfo recordClientJob(int taskID, String user_id, String parameter_info) throws OmnigeneException {
        return recordClientJob(taskID, user_id, parameter_info, -1);
    }

    /**
     * 
     */
    public JobInfo recordClientJob(int taskID, String user_id, String parameter_info, int parentJobNumber)
            throws OmnigeneException {
        Integer jobNo = null;
        try {

            Integer parent = null;
            if (parentJobNumber != -1) {
                parent = new Integer(parentJobNumber);
            }
            jobNo = addNewJob(taskID, user_id, parameter_info, null, parent, null);
            updateJobStatus(jobNo, JobStatus.JOB_FINISHED);
            setJobDeleted(jobNo, true);
            return getJobInfo(jobNo);
        }
        catch (OmnigeneException e) {
            if (jobNo != null) {
                deleteJob(jobNo);
            }
            throw e;
        }
    }

    /**
     * 
     */
    public JobInfo addNewJob(int taskID, String user_id, String parameter_info, int parentJobNumber)
            throws OmnigeneException {
        Integer jobNo = addNewJob(taskID, user_id, parameter_info, null, new Integer(parentJobNumber), null);
        return this.getJobInfo(jobNo);
    }

    /**
     * Submit a new job
     * 
     * @param taskID
     *            the task id or UNPROCESSABLE_TASKID if the task is a temporary
     *            pipeline
     * @param user_id
     *            the user id
     * @param parameter_info
     *            the parameter info
     * @param taskName
     *            the task name if the task is a temporary pipeline
     * @param parentJobNumber
     *            the parent job number of <tt>null</tt> if the job has no
     *            parent
     * @throws OmnigeneException
     * @throws RemoteException
     * @return Job ID
     */
    public Integer addNewJob(int taskID, String user_id, String parameter_info, String taskName,
            Integer parentJobNumber, String task_lsid) {
        int updatedRecord = 0;

        String lsid = null;

        // Check taskID is valid
        if (taskID != UNPROCESSABLE_TASKID) {
            String hqlString = "select taskName, lsid from org.genepattern.server.webservice.server.dao.TaskMaster where taskId = :taskId";
            Query query = getSession().createQuery(hqlString);
            query.setInteger("taskId", taskID);
            Object[] results = (Object[]) query.uniqueResult();
            taskName = (String) results[0];
            lsid = (String) results[1];
        }
        else {
            if (task_lsid != null)
                lsid = task_lsid;
        }

        AnalysisJob aJob = new AnalysisJob();
        aJob.setTaskId(taskID);
        org.genepattern.server.webservice.server.dao.JobStatus js = new org.genepattern.server.webservice.server.dao.JobStatus();
        js.setStatusId(JOB_WAITING_STATUS);
        aJob.setJobStatus(js);
        aJob.setSubmittedDate(Calendar.getInstance().getTime());
        aJob.setParameterInfo(parameter_info);
        aJob.setUserId(user_id);
        aJob.setTaskName(taskName);
        aJob.setParent(parentJobNumber);
        aJob.setTaskLsid(task_lsid);

        return (Integer) getSession().save(aJob);

    }

    /**
     * Update job information like status and resultfilename
     * 
     * @param jobNo
     * @param jobStatusID
     * @param outputFilename
     * @throws OmnigeneException
     * @throws RemoteException
     * @return record count of updated records
     */
    public int updateJobStatus(int jobNo, int jobStatusID) throws OmnigeneException {

        Query query = getSession().createQuery(
                "update org.genepattern.server.webservice.server.dao.AnalysisJob "
                        + " set jobStatus.statusId = :statusId, completedDate = :completedDate "
                        + " where jobNo = :jobNo");
        query.setInteger("statusId", jobStatusID);
        query.setDate("completedDate", now());
        query.setInteger("jobNo", jobNo);
        int count = query.executeUpdate();
        getSession().flush();
        getSession().clear();
        return count;

    }

    /**
     * Update job info with paramter infos and status
     * 
     * @param jobNo
     * @param parameters
     * @param jobStatusID
     * @return number of record updated
     * @throws OmnigeneException
     * @throws RemoteException
     */
    public int updateJob(int jobNo, String parameters, int jobStatusID) throws OmnigeneException {
        Query query = getSession()
                .createQuery(
                        "update org.genepattern.server.webservice.server.dao.AnalysisJob "
                                + " set jobStatus.statusId = :statusId, parameterInfo = :parameterInfo, completedDate = :completedDate "
                                + " where jobNo = :jobNo");
        query.setInteger("statusId", jobStatusID);
        query.setString("parameterInfo", parameters);
        query.setDate("completedDate", now());
        query.setInteger("jobNo", jobNo);
        int count = query.executeUpdate();
        getSession().flush();
        getSession().clear();
        return count;

    }

    /**
     * Fetches JobInformation
     * 
     * @param jobNo
     * @throws OmnigeneException
     * @throws RemoteException
     * @return <CODE>JobInfo</CODE>
     * @throws OmnigeneException
     */
    public JobInfo getJobInfo(int jobNo) {

        String hql = " from org.genepattern.server.webservice.server.dao.AnalysisJob where jobNo = :jobNo";
        Query query = getSession().createQuery(hql);
        query.setInteger("jobNo", jobNo);
        AnalysisJob aJob = (AnalysisJob) query.uniqueResult();
        // If jobNo not found
        if (aJob == null)
            throw new JobIDNotFoundException("AnalysisHypersonicDAO:getJobInfo JobID " + jobNo + " not found");

        return jobInfoFromAnalysisJob(aJob);
    }

    /**
     * Fetches list of JobInformation by userId
     * 
     * @param user_id
     * @throws OmnigeneException
     * @throws RemoteException
     * @return <CODE>JobInfo[]</CODE>
     */
    public JobInfo[] getJobInfo(String user_id) throws OmnigeneException {
        JobInfo ji = null;
        Vector jobVector = new Vector();
        Session session = null;
        try {
            session = getSession();

            // Fetch from database
            String hql = " from org.genepattern.server.webservice.server.dao.AnalysisJob where userId = :userId";
            Query query = session.createQuery(hql);
            query.setString("userId", user_id);
            query.setFetchSize(100);
            List results = query.list();

            boolean recordFound = false;
            for (Object aJob : results) {
                recordFound = true;
                ji = jobInfoFromAnalysisJob((AnalysisJob) aJob);
                jobVector.add(ji);
                log.debug("submit: " + ji.getDateSubmitted() + "complete: " + ji.getDateCompleted());
            }

            return (JobInfo[]) jobVector.toArray(new JobInfo[] {});

        }
        catch (Exception e) {
            e.printStackTrace();
            log.error("AnalysisHypersonicDAO:getJobInfo failed " + e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            if (session != null)
                session.close();
        }
    }

    public JobInfo getParent(int jobId) throws OmnigeneException {

        String hql = " select parent from org.genepattern.server.webservice.server.dao.AnalysisJob as parent, "
                + " org.genepattern.server.webservice.server.dao.AnalysisJob as child "
                + " where child.jobNo = :jobNo and parent.jobNo = child.parent ";
        Query query = getSession().createQuery(hql);
        query.setInteger("jobNo", jobId);
        AnalysisJob parent = (AnalysisJob) query.uniqueResult();
        if (parent != null) {
            return jobInfoFromAnalysisJob(parent);
        }
        return null;

    }

    /**
     * 
     */
    public JobInfo[] getChildren(int jobId) throws OmnigeneException {

        java.util.List results = new java.util.ArrayList();

        String hql = " from org.genepattern.server.webservice.server.dao.AnalysisJob  where parent = :jobNo ";
        Query query = getSession().createQuery(hql);
        query.setInteger("jobNo", jobId);
        query.setFetchSize(50);
        List<AnalysisJob> aJobs = query.list();
        for (AnalysisJob aJob : aJobs) {
            JobInfo ji = jobInfoFromAnalysisJob(aJob);
            results.add(ji);
        }
        return (JobInfo[]) results.toArray(new JobInfo[0]);
    }

    /**
     * 
     */
    public void setJobDeleted(int jobNumber, boolean deleted) throws OmnigeneException {
        String hql = "update org.genepattern.server.webservice.server.dao.AnalysisJob "
                + " set deleted = :deleted where jobNo = :jobNo";
        Query query = getSession().createQuery(hql);
        query.setBoolean("deleted", deleted);
        query.setInteger("jobNo", jobNumber);
        query.executeUpdate();
        getSession().flush();
        getSession().clear();

    }

    /**
     * 
     */
    public JobInfo[] getJobs(String username, int maxJobNumber, int maxEntries, boolean allJobs)
            throws OmnigeneException {

        String hql = " from org.genepattern.server.webservice.server.dao.AnalysisJob where parent = null ";
        if (username != null) {
            hql += " AND userId = :username ";
        }
        if (maxJobNumber != -1) {
            hql += " AND jobNo <= :maxJobNumber ";
        }
        if (!allJobs) {
            hql += " AND deleted = :deleted ";
        }
        hql += " ORDER BY jobNo DESC";
        Query query = getSession().createQuery(hql);
        query.setFetchSize(50);
        query.setMaxResults(maxEntries);

        if (username != null) {
            query.setString("username", username);
        }
        if (maxJobNumber != -1) {
            query.setInteger("maxJobNumber", maxJobNumber);
        }
        if (!allJobs) {
            query.setBoolean("deleted", false);
        }

        java.util.List results = new java.util.ArrayList();
        List<AnalysisJob> aJobs = query.list();
        for (AnalysisJob aJob : aJobs) {
            JobInfo ji = this.jobInfoFromAnalysisJob(aJob);
            results.add(ji);
        }
        return (JobInfo[]) results.toArray(new JobInfo[] {});

    }

    /**
     * Fetches list of JobInfo based on completion date on or before a specified
     * date
     * 
     * @param date
     * @throws OmnigeneException
     * @throws RemoteException
     * @return <CODE>JobInfo[]</CODE>
     */
    public JobInfo[] getJobInfo(java.util.Date date) throws OmnigeneException {

        Vector jobVector = new Vector();

        String hql = " from org.genepattern.server.webservice.server.dao.AnalysisJob where completedDate < :completedDate ";
        Query query = getSession().createQuery(hql);
        query.setDate("completedDate", date);
        query.setFetchSize(50);

        List<AnalysisJob> aJobs = query.list();
        for (AnalysisJob aJob : aJobs) {
            JobInfo ji = this.jobInfoFromAnalysisJob(aJob);
            jobVector.add(ji);
        }
        return (JobInfo[]) jobVector.toArray(new JobInfo[] {});

    }

    /*
     * int jobNo, int taskID, String status, Date submittedDate, Date
     * completedDate, ParameterInfo[] parameters, String userId, String lsid,
     * String taskName) {
     * 
     */

    protected JobInfo jobInfoFromAnalysisJob(org.genepattern.server.webservice.server.dao.AnalysisJob aJob)
            throws OmnigeneException {
        ParameterFormatConverter parameterFormatConverter = new ParameterFormatConverter();

        JobInfo ji = new JobInfo(aJob.getJobNo().intValue(), aJob.getTaskId(), aJob.getJobStatus().getStatusName(),
                aJob.getSubmittedDate(), aJob.getCompletedDate(), parameterFormatConverter.getParameterInfoArray(aJob
                        .getParameterInfo()), aJob.getUserId(), aJob.getTaskLsid(), aJob.getTaskName());

        return ji;
    }

    /**
     * Removes a job and all it's input and output files based on jobID
     * 
     * @param taskID
     * @throws OmnigeneException
     * @throws RemoteException
     */
    public void deleteJob(int jobID) throws OmnigeneException {
        java.sql.Connection conn = null;
        PreparedStatement stat = null;
        ResultSet resultSet = null;
        boolean DEBUG = false;
        JobInfo jobInfo = getJobInfo(jobID);

        ParameterInfo[] pia = jobInfo.getParameterInfoArray();
        if (pia != null) {
            for (int i = 0; i < pia.length; i++) {
                if (pia[i].isOutputFile() || pia[i].isInputFile()) {
                    if (DEBUG)
                        System.out.println("deleting " + pia[i].getValue());
                    new File(pia[i].getValue()).delete();
                }
            }
        }

        Session session = getSession();
        String hqlDelete = "delete org.genepattern.server.webservice.server.dao.AnalysisJob  where jobNo = :jobNo";
        Query query = session.createQuery(hqlDelete);
        query.setInteger("jobNo", jobID);
        int updatedRecord = query.executeUpdate();
        getSession().flush();
        getSession().clear();

        // If no record updated
        if (updatedRecord == 0) {
            log.error("deleteTask Could not delete task, taskID not found");
            throw new JobIDNotFoundException("AnalysisHypersonicDAO:deleteJob JobID " + jobID + " not a valid jobID ");
        }
    }

    /**
     * To create a new regular task
     * 
     * @param taskName
     * @param user_id
     * @param access_id
     * @param description
     * @param parameter_info
     * @throws OmnigeneException
     * @return task ID
     */
    public int addNewTask(String taskName, String user_id, int access_id, String description, String parameter_info,
            String taskInfoAttributes) throws OmnigeneException {

        try {
            TaskInfoAttributes tia = TaskInfoAttributes.decode(taskInfoAttributes);
            String sLSID = null;
            if (tia != null) {
                sLSID = tia.get(GPConstants.LSID);
            }

            TaskMaster tm = new TaskMaster();
            tm.setTaskName(taskName);
            tm.setDescription(description);
            tm.setParameterInfo(parameter_info);
            tm.setTaskinfoattributes(taskInfoAttributes);
            tm.setUserId(user_id);
            tm.setAccessId(access_id);
            tm.setLsid(sLSID.toString());
            int taskID = (Integer) getSession().save(tm);

            if (sLSID != null && !sLSID.equals("")) {
                LSID tmp = new LSID(sLSID);
                Lsid lsid = new Lsid();
                lsid.setLsid(tmp.toString());
                lsid.setLsidNoVersion(tmp.toStringNoVersion());
                lsid.setVersion(tmp.getVersion());
                getSession().save(lsid);
            }

            return taskID;
        }
        catch (Exception e) {
            log.error(e);
            throw new OmnigeneException(e);
        }
    }

    /**
     * Updates task description and parameters
     * 
     * @param taskID
     *            task ID
     * @param description
     *            task description
     * @param parameter_info
     *            parameters as a xml string
     * @return No. of updated records
     * @throws OmnigeneException
     * @throws RemoteException
     */
    public int updateTask(int taskId, String taskDescription, String parameter_info, String taskInfoAttributes,
            String user_id, int access_id) throws OmnigeneException {

        try {
            Query query = getSession().createQuery(
                    "select lsid from org.genepattern.server.webservice.server.dao.TaskMaster "
                            + " where taskId = :taskId");
            query.setInteger("taskId", taskId);
            String oldLSID = (String) query.uniqueResult();

            String updateHql = "update org.genepattern.server.webservice.server.dao.TaskMaster "
                    + " set parameterInfo = :parameterInfo, description = :description, "
                    + " taskInfoAttributes = :taskInfoAttributes, userId = :userId, "
                    + " accessId = :accessId, lsid = :lsid WHERE taskId = :taskId ";
            Query updateQuery = getSession().createQuery(updateHql);
            updateQuery.setString("parameterInfo", parameter_info);
            updateQuery.setString("description", taskDescription);
            updateQuery.setString("taskInfoAttributes", taskInfoAttributes);
            updateQuery.setString("userId", user_id);
            updateQuery.setInteger("accessId", access_id);

            TaskInfoAttributes tia = TaskInfoAttributes.decode(taskInfoAttributes);
            String sLSID = null;
            LSID lsid = null;
            if (tia != null) {
                sLSID = tia.get(GPConstants.LSID);
            }
            if (sLSID != null && !sLSID.equals("")) {
                lsid = new LSID(sLSID);
                updateQuery.setString("lsid", sLSID);
            }
            else {
                updateQuery.setString("lsid", null);
            }
            updateQuery.setInteger("taskId", taskId);
            int updatedRecord = updateQuery.executeUpdate();

            if (oldLSID != null) {
                // delete the old LSID record
                String deleteHql = "delete from org.genepattern.server.webservice.server.dao.Lsid where lsid = :lsid";
                Query deleteQuery = getSession().createQuery(deleteHql);
                deleteQuery.setString("lsid", oldLSID);
                deleteQuery.executeUpdate();

            }

            if (sLSID != null) {
                Lsid lsidHibernate = new Lsid();
                lsidHibernate.setLsid(lsid.toString());
                lsidHibernate.setLsidNoVersion(lsid.toStringNoVersion());
                lsidHibernate.setVersion(lsid.getVersion());
                getSession().save(lsidHibernate);

            }
            getSession().flush();
            getSession().clear();

            return updatedRecord;
        }
        catch (Exception e) {
            log.error(e);
            throw new OmnigeneException(e);
        }
    }

    /**
     * Updates task parameters
     * 
     * @param taskID
     *            task ID
     * @param parameter_info
     *            parameters as a xml string
     * @throws OmnigeneException
     * @throws RemoteException
     * @return No. of updated records
     */
    public int updateTask(int taskId, String parameter_info, String taskInfoAttributes, String user_id, int access_id)
            throws OmnigeneException {

        try {
            Query query = getSession().createQuery(
                    "select lsid from org.genepattern.server.webservice.server.dao.TaskMaster "
                            + " where taskId = :taskId");
            query.setInteger("taskId", taskId);
            String oldLSID = (String) query.uniqueResult();

            // update task
            String updateHql = "update org.genepattern.server.webservice.server.dao.TaskMaster "
                    + " set parameterInfo = :parameterInfo,  "
                    + " taskInfoAttributes = :taskInfoAttributes, userId = :userId, "
                    + " accessId = :accessId, lsid = :lsid WHERE taskId = :taskId ";
            Query updateQuery = getSession().createQuery(updateHql);
            updateQuery.setString("parameterInfo", parameter_info);
            updateQuery.setString("taskInfoAttributes", taskInfoAttributes);
            updateQuery.setString("userId", user_id);
            updateQuery.setInteger("accessId", access_id);
            TaskInfoAttributes tia = TaskInfoAttributes.decode(taskInfoAttributes);
            String sLSID = null;
            LSID lsid = null;
            if (tia != null) {
                sLSID = tia.get(GPConstants.LSID);
            }
            if (sLSID != null && !sLSID.equals("")) {
                lsid = new LSID(sLSID);
                updateQuery.setString("lsid", sLSID);
            }
            else {
                updateQuery.setString("lsid", null);
            }
            updateQuery.setInteger("taskId", taskId);
            int updatedRecord = updateQuery.executeUpdate();

            if (oldLSID != null) {
                // delete the old LSID record
                String deleteHql = "delete from org.genepattern.server.webservice.server.dao.Lsid where lsid = :lsid";
                Query deleteQuery = getSession().createQuery(deleteHql);
                deleteQuery.setString("lsid", oldLSID);
                deleteQuery.executeUpdate();

            }

            if (sLSID != null) {
                Lsid lsidHibernate = new Lsid();
                lsidHibernate.setLsid(lsid.toString());
                lsidHibernate.setLsidNoVersion(lsid.toStringNoVersion());
                lsidHibernate.setVersion(lsid.getVersion());
                getSession().save(lsidHibernate);

            }
            getSession().flush();
            getSession().clear();

            return updatedRecord;
        }
        catch (Exception e) {
            log.error(e);
            throw new OmnigeneException(e);
        }
    }

    /**
     * Updates user_id and access_id
     * 
     * @param taskID
     *            task ID
     * @param user_id
     * @param access_id
     * @return No. of updated records
     * @throws OmnigeneException
     * @throws RemoteException
     */
    public int updateTask(int taskId, String user_id, int access_id) throws OmnigeneException {
        try {
            String updateHql = "update org.genepattern.server.webservice.server.dao.TaskMaster "
                    + " set userId = :userId, accessId = :accessId where taskId = :taskId ";
            Query updateQuery = getSession().createQuery(updateHql);
            updateQuery.setString("userId", user_id);
            updateQuery.setInteger("accessId", access_id);
            updateQuery.setInteger("taskId", taskId);
            int updatedRecord = updateQuery.executeUpdate();
            getSession().flush();
            getSession().clear();
            return updatedRecord;

        }
        catch (Exception e) {
            log.error("AnalysisHypersonicDAO: updateTask failed " + e);
            throw new OmnigeneException(e.getMessage());
        }
    }

    /**
     * To remove registered task based on task ID
     * 
     * @param taskID
     * @throws OmnigeneException
     * @throws RemoteException
     * @return No. of updated records
     */
    public int deleteTask(int taskID) throws OmnigeneException {

        try {

            String hql = "delete from org.genepattern.server.webservice.server.dao.TaskMaster  where taskId = :taskId ";
            Query query = getSession().createQuery(hql);
            query.setInteger("taskId", taskID);
            int updatedRecord = query.executeUpdate();
            getSession().flush();
            getSession().clear();

            // If no record updated
            if (updatedRecord == 0) {
                log.error("deleteTask Could not delete task, taskID not found");
                throw new TaskIDNotFoundException("AnalysisHypersonicDAO:deleteTask TaskID " + taskID
                        + " not a valid TaskID ");
            }

            return updatedRecord;

        }
        catch (Exception e) {
            log.error("deleteTask failed " + e);
            throw new OmnigeneException(e.getMessage());
        }
    }

    /**
     * reset any previous running (but incomplete) jobs to waiting status, clear
     * their output files
     * 
     * @return true if there were running jobs
     * @author Jim Lerner
     * 
     */
    public boolean resetPreviouslyRunningJobs() {

        String hql = "update org.genepattern.server.webservice.server.dao.AnalysisJob set "
                + " jobStatus.statusId = :waitStatus where jobStatus.statusId = :processingStatus ";
        Query query = getSession().createQuery(hql);
        query.setInteger("waitStatus", JOB_WAITING_STATUS);
        query.setInteger("processingStatus", PROCESSING_STATUS);
        boolean exist = (query.executeUpdate() > 0);
        getSession().flush();
        getSession().clear();
        return exist;
    }

    /**
     * execute arbitrary SQL on database, returning ResultSet
     * 
     * @param sql
     * @throws OmnigeneException
     * @throws RemoteException
     * @return ResultSet
     */
    public ResultSet executeSQL(String sql) throws OmnigeneException {

        Statement stat = null;
        ResultSet resultSet = null;

        try {
            Connection conn = getSession().connection();
            stat = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            resultSet = stat.executeQuery(sql);
            CachedRowSetImpl crs = new CachedRowSetImpl();
            crs.populate(resultSet);
            return crs;
        }
        catch (Exception e) {
            log.error("AnalysisHypersonicDAO: executeSQL for " + sql + " failed " + e);
            throw new OmnigeneException(e.getMessage());
        }

        finally {
            cleanupJDBC(resultSet, stat);
        }

    }

    /**
     * execute arbitrary SQL on database, returning int
     * 
     * @param sql
     * @throws OmnigeneException
     * @throws RemoteException
     * @return int number of rows returned
     */
    public int executeUpdate(String sql) {

        Query query = getSession().createSQLQuery(sql);
        int ret = query.executeUpdate();
        getSession().flush();
        getSession().clear();
        return ret;

    }

    /**
     * get the next available task LSID identifer from the database
     * 
     * @return int next identifier in sequence
     */
    public int getNextTaskLSIDIdentifier() {

        Query query = getSession().createSQLQuery("select next value for lsid_identifier_seq from dual");
        Number result = (Number) query.uniqueResult();
        if (result != null) {
            return result.intValue();
        }
        else {
            throw new OmnigeneException("Unable to retrieve lsid_identifier_seq");
        }
    }

    /**
     * get the next available suite LSID identifer from the database
     * 
     * @throws OmnigeneException
     * @throws RemoteException
     * @return int next identifier in sequence
     */
    public int getNextSuiteLSIDIdentifier() throws OmnigeneException {
        Query query = getSession().createSQLQuery("select next value for lsid_suite_identifier_seq from dual");
        Number result = (Number) query.uniqueResult();

        // only one record
        if (result != null) {
            return result.intValue();
        }
        else {
            throw new OmnigeneException("Unable to retrieve lsid_suite_identifier_seq");
        }
    }

    /**
     * get the next available LSID version for a given identifer from the
     * database
     * 
     * @throws OmnigeneException
     * @throws RemoteException
     * @return int next version in sequence
     */
    public String getNextTaskLSIDVersion(LSID lsid) throws OmnigeneException {

        try {
            LSID newLSID = lsid.copy();
            newLSID.setVersion(newLSID.getIncrementedMinorVersion());
            String sql = "select count(*) from lsids where lsid = :newLSID";
            Query query = getSession().createSQLQuery(sql);
            query.setString("newLSID", newLSID.toString());
            int count = (Integer) query.uniqueResult();

            String nextVersion = "1";
            if (count > 0) {
                newLSID.setVersion(lsid.getVersion() + ".0");
                nextVersion = getNextTaskLSIDVersion(newLSID);
            }
            else {
                // not found: must be version 1
                nextVersion = newLSID.getVersion();
            }
            return nextVersion;
        }
        catch (Exception e) {
            log.error(e);
            throw new OmnigeneException(e);
        }
    }

    /**
     * get the next available LSID version for a given identifer from the
     * database
     * 
     * @throws OmnigeneException
     * @throws RemoteException
     * @return int next version in sequence
     */
    public String getNextSuiteLSIDVersion(LSID lsid) throws OmnigeneException {
        try {
            LSID newLSID = lsid.copy();
            newLSID.setVersion(newLSID.getIncrementedMinorVersion());
            String sql = "select count(*) from lsids where lsid = :newLSID";
            Query query = getSession().createSQLQuery(sql);
            query.setString("newLSID", newLSID.toString());
            int count = (Integer) query.uniqueResult();

            String nextVersion = "1";
            if (count > 0) {
                newLSID.setVersion(lsid.getVersion() + ".0");
                // System.out.println("AHDAO.getNextLSIDVersion: recursing with
                // " + newLSID.getVersion());
                nextVersion = getNextSuiteLSIDVersion(newLSID);
            }
            else {
                // not found: must be version 1
                nextVersion = newLSID.getVersion();
                // System.out.println("AHDAO.getNextLSIDVersion: returning " +
                // nextVersion);
            }
            return nextVersion;

        }
        catch (Exception e) {
            log.error("AnalysisHypersonicDAO: getNextSuiteLSIDVersion failed: " + e);
            throw new OmnigeneException(e);
        }

    }

}
