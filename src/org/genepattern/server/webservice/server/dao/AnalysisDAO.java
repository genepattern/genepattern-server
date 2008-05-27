/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2008) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webservice.server.dao;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.genepattern.server.JobIDNotFoundException;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.AnalysisJob;
import org.genepattern.server.domain.AnalysisJobDAO;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.domain.JobStatusDAO;
import org.genepattern.server.domain.Lsid;
import org.genepattern.server.domain.TaskMaster;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.webservice.server.Analysis.JobSortOrder;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.TaskInfoAttributes;
import org.hibernate.LockMode;
import org.hibernate.Query;

/**
 * AnalysisHypersonicDAO.java
 * 
 * @author rajesh kuttan, Hui Gong
 * @version
 */
public class AnalysisDAO extends BaseDAO {

    static Logger log = Logger.getLogger(AnalysisDAO.class);

    /** Creates new AnalysisHypersonicAccess */
    public AnalysisDAO() {
    }

    /**
     * 
     */
    public JobInfo addNewJob(int taskID, String user_id, String parameter_info, int parentJobNumber) {
	Integer jobNo = addNewJob(taskID, user_id, parameter_info, null, new Integer(parentJobNumber), null);
	return getJobInfo(jobNo);

    }

    /**
     * Submit a new job
     * 
     * @param taskID
     *                the task id or UNPROCESSABLE_TASKID if the task is a temporary pipeline
     * @param user_id
     *                the user id
     * @param parameter_info
     *                the parameter info
     * @param taskName
     *                the task name if the task is a temporary pipeline
     * @param parentJobNumber
     *                the parent job number of <tt>null</tt> if the job has no parent
     * @throws OmnigeneException
     * @throws RemoteException
     * @return Job ID
     */
    public Integer addNewJob(int taskID, String user_id, String parameter_info, String taskName,
	    Integer parentJobNumber, String task_lsid) {
	return this.addNewJob(taskID, user_id, parameter_info, taskName, parentJobNumber, task_lsid,
		JobStatus.JOB_PENDING);
    }

    public Integer addNewJob(int taskID, String user_id, String parameter_info, String taskName,
	    Integer parentJobNumber, String task_lsid, int status) {
	int updatedRecord = 0;

	String lsid = null;

	// Check taskID is valid
	if (taskID != UNPROCESSABLE_TASKID) {
	    String hqlString = "select taskName, lsid from org.genepattern.server.domain.TaskMaster where taskId = :taskId";
	    Query query = getSession().createQuery(hqlString);
	    query.setInteger("taskId", taskID);
	    Object[] results = (Object[]) query.uniqueResult();
	    taskName = (String) results[0];
	    lsid = (String) results[1];
	} else {
	    if (task_lsid != null)
		lsid = task_lsid;
	}

	AnalysisJob aJob = new AnalysisJob();
	aJob.setTaskId(taskID);
	aJob.setSubmittedDate(Calendar.getInstance().getTime());
	aJob.setParameterInfo(parameter_info);
	aJob.setUserId(user_id);
	aJob.setTaskName(taskName);
	aJob.setParent(parentJobNumber);
	aJob.setTaskLsid(lsid);

	JobStatus js = (new JobStatusDAO()).findById(status);
	aJob.setJobStatus(js);

	return (Integer) getSession().save(aJob);

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
		Lsid lsid = new Lsid(sLSID);
		getSession().save(lsid);
	    }

	    return taskID;
	} catch (Exception e) {
	    log.error(e);
	    throw new OmnigeneException(e);
	}
    }

    /**
     * Removes a job from the database and deletes the job directory.
     * 
     * @param jobID
     */
    public void deleteJob(int jobID) {
	// recursively delete the job directory
	File jobDir = new File(GenePatternAnalysisTask.getJobDir(Integer.toString(jobID)));
	Delete del = new Delete();
	del.setDir(jobDir);
	del.setIncludeEmptyDirs(true);
	del.setProject(new Project());
	del.execute();
	AnalysisJob aJob = (AnalysisJob) getSession().get(AnalysisJob.class, jobID);
	getSession().delete(aJob);
    }

    /**
     * 
     */
    public JobInfo[] getChildren(int jobId) throws OmnigeneException {

	java.util.List results = new java.util.ArrayList();

	String hql = " from org.genepattern.server.domain.AnalysisJob  where parent = :jobNo ";
	hql += " ORDER BY jobNo ASC";

	Query query = getSession().createQuery(hql);
	query.setInteger("jobNo", jobId);
	query.setFetchSize(50);
	List<AnalysisJob> aJobs = query.list();
	for (AnalysisJob aJob : aJobs) {
	    JobInfo ji = new JobInfo(aJob);
	    results.add(ji);
	}
	return (JobInfo[]) results.toArray(new JobInfo[0]);
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

	String hql = " from org.genepattern.server.domain.AnalysisJob where jobNo = :jobNo";
	Query query = getSession().createQuery(hql);
	query.setInteger("jobNo", jobNo);
	AnalysisJob aJob = (AnalysisJob) query.uniqueResult();
	// If jobNo not found
	if (aJob == null) {
	    throw new JobIDNotFoundException("AnalysisDAO:getJobInfo JobID " + jobNo + " not found");
	}

	return new JobInfo(aJob);
    }

    /**
     * Fetches list of JobInfo based on completion date on or before a specified date
     * 
     * @param date
     * @return <CODE>JobInfo[]</CODE>
     */
    public JobInfo[] getJobInfo(java.util.Date date) {
	String hql = "from org.genepattern.server.domain.AnalysisJob as j where j.completedDate < :completedDate";
	hql += " ORDER BY jobNo ASC";
	Query query = getSession().createQuery(hql);
	Calendar cal = Calendar.getInstance();
	cal.setTime(date);
	query.setCalendar("completedDate", cal);
	List<AnalysisJob> aJobs = query.list();
	JobInfo[] results = new JobInfo[aJobs.size()];
	for (int i = 0, size = aJobs.size(); i < size; i++) {
	    JobInfo ji = new JobInfo(aJobs.get(i));
	    results[i] = ji;
	}
	return results;

    }

    /**
     * Get the total number of job entries in the database.
     * @return
     */
    public long getNumJobs() {
        return getNumJobs(null);
    }

    /** 
     * Get the total number of jobs owned by the given user.
     * @param userId
     * @return
     */
    public int getNumJobs(String userId) {
        //if userId is null or empty get all of the jobs
        String hql = "select count(*) from AnalysisJob";
        if (userId != null) {
            hql += " where userId = :userId";
        }
        Query query = getSession().createQuery(hql);
        if (userId != null) {
            query.setString("userId", userId);
        }
        Object rval = query.uniqueResult();
        return ((Integer) rval).intValue();
    }

    public JobInfo[] getJobs(String username, int maxJobNumber, int maxEntries, boolean allJobs)
	    throws OmnigeneException {
	return getJobs(username, maxJobNumber, maxEntries, allJobs, JobSortOrder.JOB_NUMBER, false);
    }

    public JobInfo[] getJobs(String username, int maxJobNumber, int maxEntries, boolean allJobs,
	    JobSortOrder sortOrder, boolean ascending) throws OmnigeneException {
        return getPagedJobs(username, 1, maxEntries, allJobs, sortOrder, ascending);
    }
    public JobInfo[] getPagedJobs(String username, int firstResult, int maxResults, boolean allJobs, JobSortOrder sortOrder, boolean ascending)
    throws OmnigeneException 
    {
	StringBuffer hql = new StringBuffer(
		" from org.genepattern.server.domain.AnalysisJob where ((parent = null) OR (parent = -1)) ");
	if (username != null) {
	    hql.append(" AND userId = :username ");
	}
	//if (maxJobNumber != -1) {
	//    hql.append(" AND jobNo <= :maxJobNumber ");
	//}
	if (!allJobs) {
	    hql.append(" AND deleted = :deleted ");
	}
	switch (sortOrder) {
	case JOB_NUMBER:
	    hql.append(" ORDER BY jobNo");
	    break;
	case JOB_STATUS:
	    hql.append(" ORDER BY jobStatus");
	    break;
	case SUBMITTED_DATE:
	    hql.append(" ORDER BY submittedDate");
	    break;
	case COMPLETED_DATE:
	    hql.append(" ORDER BY completedDate");
	    break;
	case USER:
	    hql.append(" ORDER BY userId");
	    break;
	case MODULE_NAME:
	    hql.append(" ORDER BY taskName");
	    break;
	}

	hql.append(ascending ? " ASC" : " DESC");
	Query query = getSession().createQuery(hql.toString());
	query.setFirstResult(firstResult);
	query.setFetchSize(50);
	query.setMaxResults(maxResults);

	if (username != null) {
	    query.setString("username", username);
	}
	//if (maxJobNumber != -1) {
	//    query.setInteger("maxJobNumber", maxJobNumber);
	//}
	if (!allJobs) {
	    query.setBoolean("deleted", false);
	}

	List<JobInfo> results = new ArrayList<JobInfo>();
	List<AnalysisJob> aJobs = query.list();
	for (AnalysisJob aJob : aJobs) {
	    JobInfo ji = new JobInfo(aJob);
	    results.add(ji);
	}
	return results.toArray(new JobInfo[] {});

    }

    /**
     * get the next available suite LSID identifer from the database
     * 
     * @throws OmnigeneException
     * @throws RemoteException
     * @return int next identifier in sequence
     */
    public int getNextSuiteLSIDIdentifier() throws OmnigeneException {
	return HibernateUtil.getNextSequenceValue("lsid_suite_identifier_seq");
    }

    /**
     * get the next available LSID version for a given identifer from the database
     * 
     * @throws OmnigeneException
     * @throws RemoteException
     * @return int next version in sequence
     */
    public String getNextSuiteLSIDVersion(LSID lsid) throws OmnigeneException {
	try {
	    LSID newLSID = lsid.copy();
	    newLSID.setVersion(newLSID.getIncrementedMinorVersion());
	    int count = getLsidCount(newLSID);

	    String nextVersion = "1";
	    if (count > 0) {
		newLSID.setVersion(lsid.getVersion() + ".0");
		// log.debug("getNextSuiteLSIDVersion: recursing with " + newLSID.getVersion());
		nextVersion = getNextSuiteLSIDVersion(newLSID);
	    } else {
		// not found: must be version 1
		nextVersion = newLSID.getVersion();
		// log.debug("getNextLSIDVersion: returning " + nextVersion);
	    }
	    return nextVersion;
	} catch (Exception e) {
	    log.error("getNextSuiteLSIDVersion failed: " + e);
	    throw new OmnigeneException(e);
	}
    }

    /**
     * get the next available task LSID identifer from the database
     * 
     * @return int next identifier in sequence
     */
    public int getNextTaskLSIDIdentifier() {
	return HibernateUtil.getNextSequenceValue("lsid_identifier_seq");
    };

    /**
     * get the next available LSID version for a given identifer from the database
     * 
     * @throws OmnigeneException
     * @throws RemoteException
     * @return int next version in sequence
     */
    public String getNextTaskLSIDVersion(LSID lsid) throws OmnigeneException {

	try {
	    LSID newLSID = lsid.copy();
	    newLSID.setVersion(newLSID.getIncrementedMinorVersion());
	    int count = this.getLsidCount(newLSID);

	    String nextVersion = "1";
	    if (count > 0) {
		newLSID.setVersion(lsid.getVersion() + ".0");
		nextVersion = getNextTaskLSIDVersion(newLSID);
	    } else {
		// not found: must be version 1
		nextVersion = newLSID.getVersion();
	    }
	    return nextVersion;
	} catch (Exception e) {
	    log.error(e);
	    throw new OmnigeneException(e);
	}
    }

    public JobInfo getParent(int jobId) throws OmnigeneException {

	String hql = " select parent from org.genepattern.server.domain.AnalysisJob as parent, "
		+ " org.genepattern.server.domain.AnalysisJob as child "
		+ " where child.jobNo = :jobNo and parent.jobNo = child.parent ";
	Query query = getSession().createQuery(hql);
	query.setInteger("jobNo", jobId);
	AnalysisJob parent = (AnalysisJob) query.uniqueResult();
	if (parent != null) {
	    return new JobInfo(parent);
	}
	return null;

    }

    /**
     * 
     */
    public String getTemporaryPipelineName(int jobNumber) throws OmnigeneException {
	String hql = "select taskName from org.genepattern.server.domain.AnalysisJob where jobNo = :jobNumber";
	Query q = getSession().createQuery(hql);
	q.setInteger("jobNumber", jobNumber);
	return (String) q.uniqueResult();
    }

    /**
     * 
     */
    public Integer recordClientJob(int taskID, String user_id, String parameter_info, int parentJobNumber)
	    throws OmnigeneException {
	Integer jobNo = null;
	try {

	    Integer parent = null;
	    if (parentJobNumber != -1) {
		parent = new Integer(parentJobNumber);
	    }
	    jobNo = addNewJob(taskID, user_id, parameter_info, null, parent, null);

	    AnalysisJobDAO aHome = new AnalysisJobDAO();
	    org.genepattern.server.domain.AnalysisJob aJob = aHome.findById(jobNo);

	    JobStatus newStatus = (JobStatus) getSession().get(JobStatus.class, JobStatus.JOB_FINISHED);
	    aJob.setStatus(newStatus);
	    aJob.setDeleted(true);

	    return jobNo;
	} catch (OmnigeneException e) {
	    if (jobNo != null) {
		deleteJob(jobNo);
	    }
	    throw e;
	}
    }

    /**
     * reset any previous running (but incomplete) jobs to waiting status, clear their output files
     * 
     * @return true if there were running jobs
     * @author Jim Lerner
     * 
     */
    public boolean resetPreviouslyRunningJobs() {

	String hql = "update org.genepattern.server.domain.AnalysisJob set "
		+ " jobStatus.statusId = :waitStatus where jobStatus.statusId = :processingStatus ";
	Query query = getSession().createQuery(hql);
	query.setInteger("waitStatus", JOB_WAITING_STATUS);
	query.setInteger("processingStatus", PROCESSING_STATUS);

	getSession().flush();
	getSession().clear();
	boolean exist = (query.executeUpdate() > 0);
	return exist;
    }

    /**
     * 
     */
    public void setJobDeleted(int jobNumber, boolean deleted) throws OmnigeneException {

	AnalysisJob aJob = (AnalysisJob) getSession().get(AnalysisJob.class, jobNumber);
	aJob.setDeleted(deleted);
	getSession().update(aJob); // Not really neccessary
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

	AnalysisJob aJob = (AnalysisJob) getSession().get(AnalysisJob.class, jobNo);
	JobStatus js = (JobStatus) getSession().get(JobStatus.class, jobStatusID);
	aJob.setJobStatus(js);
	aJob.setParameterInfo(parameters);
	aJob.setCompletedDate(now());
	getSession().update(aJob); // Not really neccessary
	return 1;

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
    public int updateJobStatus(Integer jobNo, Integer jobStatusID) throws OmnigeneException {

	AnalysisJob job = (AnalysisJob) getSession().get(AnalysisJob.class, jobNo, LockMode.UPGRADE_NOWAIT);
	org.genepattern.server.domain.JobStatus js = (org.genepattern.server.domain.JobStatus) getSession().get(
		JobStatus.class, jobStatusID);

	log.debug("/tSetting job status");

	job.setJobStatus(js);
	getSession().update(job); // Not really neccessary
	return 1;

    }

    /**
     * Updates task parameters
     * 
     * @param taskID
     *                task ID
     * @param parameter_info
     *                parameters as a xml string
     * @throws OmnigeneException
     * @throws RemoteException
     * @return No. of updated records
     */
    public int updateTask(int taskId, String parameter_info, String taskInfoAttributes, String user_id, int access_id)
	    throws OmnigeneException {

	try {
	    TaskMaster task = (TaskMaster) getSession().get(TaskMaster.class, taskId);

	    String oldLSID = task.getLsid();

	    task.setParameterInfo(parameter_info);
	    task.setTaskinfoattributes(taskInfoAttributes);
	    task.setUserId(user_id);
	    task.setAccessId(access_id);

	    TaskInfoAttributes tia = TaskInfoAttributes.decode(taskInfoAttributes);
	    String sLSID = null;
	    LSID lsid = null;
	    if (tia != null) {
		sLSID = tia.get(GPConstants.LSID);
	    }
	    if (sLSID != null && !sLSID.equals("")) {
		lsid = new LSID(sLSID);
		task.setLsid(sLSID);
	    } else {
		task.setLsid(null);
	    }

	    getSession().update(task);

	    if (oldLSID != null) {
		// delete the old LSID record
		String deleteHql = "delete from org.genepattern.server.domain.Lsid where lsid = :lsid";
		Query deleteQuery = getSession().createQuery(deleteHql);
		deleteQuery.setString("lsid", oldLSID);
		deleteQuery.executeUpdate();

	    }

	    if (sLSID != null) {
		Lsid lsidHibernate = new Lsid(lsid.toString());
		getSession().save(lsidHibernate);

	    }

	    return 1;
	} catch (Exception e) {
	    log.error(e);
	    throw new OmnigeneException(e);
	}
    }

    /**
     * Updates task description and parameters
     * 
     * @param taskID
     *                task ID
     * @param description
     *                task description
     * @param parameter_info
     *                parameters as a xml string
     * @return No. of updated records
     * @throws OmnigeneException
     * @throws RemoteException
     */
    public int updateTask(int taskId, String taskDescription, String parameter_info, String taskInfoAttributes,
	    String user_id, int access_id) throws OmnigeneException {

	try {

	    TaskMaster task = (TaskMaster) getSession().get(TaskMaster.class, taskId);

	    String oldLSID = task.getLsid();

	    task.setParameterInfo(parameter_info);
	    task.setDescription(taskDescription);
	    task.setTaskinfoattributes(taskInfoAttributes);
	    task.setUserId(user_id);
	    task.setAccessId(access_id);

	    TaskInfoAttributes tia = TaskInfoAttributes.decode(taskInfoAttributes);
	    String sLSID = null;
	    LSID lsid = null;
	    if (tia != null) {
		sLSID = tia.get(GPConstants.LSID);
	    }
	    if (sLSID != null && !sLSID.equals("")) {
		lsid = new LSID(sLSID);
		task.setLsid(sLSID);
	    } else {
		task.setLsid(null);
	    }

	    getSession().update(task); // Not neccessary ?

	    if (oldLSID != null) {
		// delete the old LSID record
		String deleteHql = "delete from org.genepattern.server.domain.Lsid where lsid = :lsid";
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

	    return 1;
	} catch (Exception e) {
	    log.error(e);
	    throw new OmnigeneException(e);
	}
    }

    private int getLsidCount(LSID lsid) {
	int count = 0;

	final String sql = "select count(*) from lsids where lsid = :newLSID";
	Query query = getSession().createSQLQuery(sql);
	query.setString("newLSID", lsid.toString());
	query.setReadOnly(true);
	Object result = query.uniqueResult();
	if (result instanceof Integer) {
	    count = (Integer) result;
	} else if (result instanceof BigInteger) {
	    count = ((BigInteger) result).intValue();
	} else if (result instanceof BigDecimal) {
	    try {
		count = ((BigDecimal) result).intValueExact();
	    } catch (ArithmeticException e) {
		log.error("Invalid conversion from BigDecimal to int", e);
	    }
	} else {
	    log.error("Unknown type returned from query: " + result.getClass().getName());
	}
	return count;
    }

}
