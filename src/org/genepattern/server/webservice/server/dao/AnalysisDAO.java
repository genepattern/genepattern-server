/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.genepattern.server.JobIDNotFoundException;
import org.genepattern.server.auth.GroupPermission;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.AnalysisJob;
import org.genepattern.server.domain.AnalysisJobDAO;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.domain.JobStatusDAO;
import org.genepattern.server.domain.Lsid;
import org.genepattern.server.domain.TaskMaster;
import org.genepattern.server.executor.JobSubmissionException;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.webservice.server.Analysis.JobSortOrder;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterFormatConverter;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.TaskInfoCache;
import org.hibernate.LockMode;
import org.hibernate.Query;

/**
 * AnalysisDAO.java
 * 
 * @author rajesh kuttan, Hui Gong
 * @version
 */
public class AnalysisDAO extends BaseDAO {
    public static Logger log = Logger.getLogger(AnalysisDAO.class);

    public AnalysisDAO() {
    }

    /**
     * Get the list of recent jobs for the current user. Jobs are sorted in descending order.
     * 
     * @param userId - jobs owned by this user
     * @param numJobsToShow - the total number of jobs to show
     * @param jobSortOrder - the sort order
     * @return
     */
    public List<JobInfo> getRecentJobsForUser(String userId, int numJobsToShow, JobSortOrder jobSortOrder) {
        final int pageNum = 1;
        final boolean ascending = false;
        return getPagedJobsOwnedByUser(userId, pageNum, numJobsToShow, jobSortOrder, ascending);
    }
    
    /**
     * Get the list of recent jobs for the current user. Jobs are sorted in descending order.
     * 
     * @param userId - jobs owned by this user
     * @param numJobsToShow - the total number of jobs to show
     * @param jobSortOrder - the sort order
     * @return
     */
    public List<JobInfo> getIncompleteJobsForUser(String userId) {
        String hql = "from org.genepattern.server.domain.AnalysisJob where user_id = :userId and deleted = false and parent = -1 and (status_id = 1 or status_id = 2) and date_submitted > :date";
        Query query = getSession().createQuery(hql);
        query.setString("userId", userId);
        
        // Within the last 30 days
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -30);
        query.setDate("date", cal.getTime());

        List<AnalysisJob> results = query.list(); 
        List<JobInfo> jobInfos = convertResults(results);
        return jobInfos;
    }

    /**
     * For an admin user, get the list of all jobs, paged and sorted.
     * 
     * @param pageNum, the current page, numbering starts at 1.
     * @param pageSize, the number of jobs on a page.
     * @param jobSortOrder
     * @param ascending
     * @return
     */
    public List<JobInfo> getAllPagedJobsForAdmin(final int pageNum, final int pageSize, final JobSortOrder jobSortOrder, final boolean ascending) {
        Query query = getPagedAnalysisJobsQuery("getAllPagedJobs", pageNum, pageSize, jobSortOrder, ascending);
        List<AnalysisJob> results = query.list();
        List<JobInfo> jobInfos = convertResults(results);
        return jobInfos;
    }
    
    /**
     * For the current (non-admin) user, get the list of all jobs which are readable by this user, paged and sorted.
     * 
     * @param userId, the current user id
     * @param groupIds, the list of groups to which the current user belongs
     * @param pageNum
     * @param pageSize
     * @param jobSortOrder
     * @param ascending
     * @return
     */
    public List<JobInfo> getAllPagedJobsForUser(final String userId, final Set<String> groupIds, final int pageNum, final int pageSize, final JobSortOrder jobSortOrder, final boolean ascending) {
        Query query = getPagedAnalysisJobsQuery("getPagedJobsForUser", pageNum, pageSize, jobSortOrder, ascending);
        query.setString("userId", userId);
        query.setParameterList("groupIds", groupIds);
        List<AnalysisJob> results = query.list();
        List<JobInfo> jobInfos = convertResults(results);
        return jobInfos;
    }
    
    /**
     * Get the list of jobs owned by the current user, paged and sorted.
     * 
     * @param userId
     * @param pageNum
     * @param pageSize
     * @param jobSortOrder
     * @param ascending
     * @return
     */
    public List<JobInfo> getPagedJobsOwnedByUser(final String userId, final int pageNum, final int pageSize, final JobSortOrder jobSortOrder, final boolean ascending) {
        Query query = getPagedAnalysisJobsQuery("getPagedJobsOwnedByUser", pageNum, pageSize, jobSortOrder, ascending);
        query.setString("userId", userId);
        List<AnalysisJob> results = query.list();
        List<JobInfo> jobInfos = convertResults(results);
        return jobInfos;
    }
    
    /**
     * Get the list of jobs which are in the given group, paged and sorted.
     * 
     * @param groupId
     * @param pageNum
     * @param pageSize
     * @param jobSortOrder
     * @param ascending
     * @return
     */
    public List<JobInfo> getPagedJobsInGroup(final String groupId, final int pageNum, final int pageSize, final JobSortOrder jobSortOrder, final boolean ascending) {
        Query query = getPagedAnalysisJobsQuery("getPagedJobsForGroup", pageNum, pageSize, jobSortOrder, ascending);
        query.setString("groupId", groupId);
        List<AnalysisJob> results = query.list();
        List<JobInfo> jobInfos = convertResults(results);
        return jobInfos;
    }
    
    /**
     * 
     * @param queryName
     * @param pageNum, the current page number (starting with page 1)
     * @param pageSize, the number of root jobs to display per page
     * @param jobSortOrder
     * @param ascending
     * @return
     */
    private Query getPagedAnalysisJobsQuery(
            final String queryName, final int pageNum, final int pageSize, final JobSortOrder jobSortOrder, final boolean ascending) 
    {
        int firstResult = (pageNum-1) * pageSize;
        int maxResults = pageSize;
        
        Query namedQuery = getSession().getNamedQuery(queryName);
        StringBuffer hql = new StringBuffer(namedQuery.getQueryString());
        appendSortOrder(hql, jobSortOrder, ascending);
        
        Query query = getSession().createQuery(hql.toString());
        query.setBoolean("deleted", false);
        query.setFirstResult(firstResult);
        query.setMaxResults(maxResults);
        return query;
    }

    private void appendSortOrder(StringBuffer hql, JobSortOrder jobSortOrder, boolean ascending) {
        switch (jobSortOrder) {
        case JOB_NUMBER:
            hql.append(" ORDER BY a.jobNo");
            break;
        case JOB_STATUS:
            hql.append(" ORDER BY a.jobStatus");
            break;
        case SUBMITTED_DATE:
            hql.append(" ORDER BY a.submittedDate");
            break;
        case COMPLETED_DATE:
            hql.append(" ORDER BY a.completedDate");
            break;
        case USER:
            hql.append(" ORDER BY a.userId");
            break;
        case MODULE_NAME:
            hql.append(" ORDER BY a.taskName");
            break;
        }
        hql.append(ascending ? " ASC" : " DESC");
    }

    private List<JobInfo> convertResults(List<AnalysisJob> analysisJobs) {
        List<JobInfo> jobInfos = new ArrayList<JobInfo>(analysisJobs.size());
        for(AnalysisJob analysisJob : analysisJobs) {
            JobInfo jobInfo = new JobInfo(analysisJob);
            jobInfos.add(jobInfo);
        }
        return jobInfos;
    }

    /**
     * Add a new job, first getting a TaskInfo object with the given taskId.
     */
    private Integer addNewJob(String userId, int taskId, ParameterInfo[] parameterInfoArray, Integer parentJobNumber) 
    throws JobSubmissionException
    {
        TaskInfo taskInfo = null;
        try {
            taskInfo = new AdminDAO().getTask(taskId);
        }
        catch (Throwable t) {
            throw new JobSubmissionException("Error adding new job, not able to get taskInfo for taskId="+taskId, t);
        }
        return addNewJob(userId, taskInfo, parameterInfoArray, parentJobNumber);
    }

    public Integer addNewJob(String userId, TaskInfo taskInfo, ParameterInfo[] parameterInfoArray, Integer parentJobNumber) 
    throws JobSubmissionException
    { 
        if (taskInfo == null) {
            throw new JobSubmissionException("Error adding job to queue, taskInfo is null");
        }
        
        if (taskInfo.getID() < 0) {
            throw new JobSubmissionException("Error adding job to queue, invalid taskId, taskInfo.getID="+taskInfo.getID());
        }

        Integer jobId = null;
        try {
            String parameter_info = ParameterFormatConverter.getJaxbString(parameterInfoArray);

            AnalysisJob aJob = new AnalysisJob();
            aJob.setTaskId(taskInfo.getID());
            aJob.setSubmittedDate(Calendar.getInstance().getTime());
            aJob.setParameterInfo(parameter_info);
            aJob.setUserId(userId);
            aJob.setTaskName(taskInfo.getName());
            aJob.setParent(parentJobNumber);
            aJob.setTaskLsid(taskInfo.getLsid());

            JobStatus js = (new JobStatusDAO()).findById(JobStatus.JOB_PENDING);
            aJob.setJobStatus(js);

            jobId = (Integer) getSession().save(aJob);
        }
        catch (Throwable t) {
            throw new JobSubmissionException("Error adding job to queue, taskId="+taskInfo.getID()+
                    ", taskName="+taskInfo.getName()+
                    ", taskLsid="+taskInfo.getLsid(), t);
        }
        
        return jobId;
    }

    public Set<GroupPermission> getGroupPermissions(int jobId) {
        String sqlString = "select group_id, permission_flag from job_group where job_no = :jobId";
        Query sqlQuery = getSession().createSQLQuery(sqlString);
        sqlQuery.setInteger("jobId", jobId);
        List<Object[]> results = sqlQuery.list();
        Set<GroupPermission> rval = new HashSet<GroupPermission>();
        for(Object[] tuple : results) {
            String groupId = (String) (tuple[0]);
            //convert to integer
            if (tuple[1] instanceof Number) {
                Number permissionFlag = (Number) (tuple[1]);
                GroupPermission gp = new GroupPermission(groupId, permissionFlag.intValue());
                rval.add(gp);
            }
            else {
                log.error("Invalid return type for sql query: "+sqlString+". tuple[1]="+tuple[1]);
            }
        }
        return rval;
    }

    /**
     * Update the 'JOB_GROUP' table with the given group permissions.
     * @param jobId
     * @param groupPermissions
     */
    public void setGroupPermissions(int jobId, Set<GroupPermission> groupPermissions) {
        Query sqlQuery = HibernateUtil.getSession().createSQLQuery("delete from JOB_GROUP where job_no = :jobId");
        sqlQuery.setInteger("jobId", jobId);
        int numDeleted = sqlQuery.executeUpdate();
        
        if (groupPermissions == null) {
            return;
        }
        
        for(GroupPermission gp : groupPermissions) {
            //insert into JOB_GROUP (job_no, group_id, permission_flag) values (<int: jobId>, <String: gp.groupId>, <int: gp.permission.flag>); 
            String sqlInsertStatement = 
                "insert into JOB_GROUP (job_no, group_id, permission_flag) values (:jobId, :groupId, :permissionFlag)";
            sqlQuery = HibernateUtil.getSession().createSQLQuery(sqlInsertStatement);
            sqlQuery.setInteger("jobId", jobId);
            sqlQuery.setString("groupId", gp.getGroupId());
            sqlQuery.setInteger("permissionFlag", gp.getPermission().getFlag());
            
            sqlQuery.executeUpdate();
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
    public int addNewTask(String taskName, String user_id, int access_id, String description, String parameter_info, String taskInfoAttributes) 
    throws OmnigeneException {
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
            TaskInfoCache.instance().removeFromCache(taskID);
            return taskID;
        } 
        catch (Exception e) {
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
    
    public void deleteJob(AnalysisJob aJob) {
        // recursively delete the job directory
        File jobDir = new File(GenePatternAnalysisTask.getJobDir(Integer.toString(aJob.getJobNo())));
        Delete del = new Delete();
        del.setDir(jobDir);
        del.setIncludeEmptyDirs(true);
        del.setProject(new Project());
        del.execute();
        getSession().delete(aJob);
    }

    /**
     * 
     */
    public JobInfo[] getChildren(int jobId) throws OmnigeneException {
        List<JobInfo> childJobInfos = new ArrayList<JobInfo>();

        String hql = " from org.genepattern.server.domain.AnalysisJob  where parent = :jobNo " +
                     " ORDER BY jobNo ASC";

        Query query = getSession().createQuery(hql);
        query.setInteger("jobNo", jobId);
        query.setFetchSize(50);
        List<AnalysisJob> aJobs = query.list();
        for (AnalysisJob aJob : aJobs) {
            try {
                JobInfo ji = new JobInfo(aJob);
                childJobInfos.add(ji);
            }
            catch (Throwable t) {
                log.error("Error creating jobInfo for analysisJob, aJob.jobNo="+aJob.getJobNo()+": "+t.getLocalizedMessage());
            }
        }
        return (JobInfo[]) childJobInfos.toArray(new JobInfo[0]);
    }

    /**
     * Get the root job number for the given job.
     * If the given job has no parent, return its job number, otherwise recursively get the job number
     * of the parent until you are at the root.
     * @param jobNo
     * @return
     */
    public int getRootJobNumber(int jobNo) {
        String hql = "select a.parent from org.genepattern.server.domain.AnalysisJob a where a.jobNo = :jobNo";
        Query query = getSession().createQuery(hql);
        query.setInteger("jobNo", jobNo);
        List<Integer> rval = query.list();
        if (rval.size() != 1) {
            log.error("getRootJobNumber("+jobNo+"): couldn't query AnalysisJob.parent from database");
            return -1;
        }
        Integer parentJobNo = rval.get(0);
        if (parentJobNo == null ||  parentJobNo.intValue() < 0) {
            return jobNo;
        }
        return getRootJobNumber(parentJobNo);
    }

    public String getJobOwner(int jobNo) {
        String hql = "select a.userId from org.genepattern.server.domain.AnalysisJob a where a.jobNo = :jobNo";
        Query query = getSession().createQuery(hql);
        query.setInteger("jobNo", jobNo);
        List<String> rval = query.list();
        if (rval.size() != 1) {
            log.error("getJobOwner: couldn't get jobOwner for job_id: "+jobNo);
            log.debug("", new Exception());
            return "";
        }
        return rval.get(0);
    }

    public AnalysisJob getAnalysisJob(int jobNo) {
        String hql = " from org.genepattern.server.domain.AnalysisJob where jobNo = :jobNo";
        Query query = getSession().createQuery(hql);
        query.setInteger("jobNo", jobNo);
        AnalysisJob aJob = (AnalysisJob) query.uniqueResult();
        // If jobNo not found
        if (aJob == null) {
            throw new JobIDNotFoundException("AnalysisDAO:getJobInfo JobID " + jobNo + " not found");
        }
        return aJob;
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
        AnalysisJob aJob = getAnalysisJob(jobNo);
        if (aJob.getDeleted()) {
            log.error("AnalysisDAO.getJobInfo("+jobNo+"): job is deleted!");
            return null;
        }
        return new JobInfo(aJob);
    }
    
    public List<Integer> getAnalysisJobIds(Date date) {
        String hql = "select jobNo from org.genepattern.server.domain.AnalysisJob as j where j.completedDate < :completedDate";
        hql += " ORDER BY jobNo ASC";
        Query query = getSession().createQuery(hql);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        query.setCalendar("completedDate", cal);
        List<Integer> jobIds = query.list();
        return jobIds;
        
    }
    public List<Integer> getAnalysisJobIdsForUser(final String userId, final Date date) {
        String hql = "select jobNo from org.genepattern.server.domain.AnalysisJob as j where j.userId = :userId and j.completedDate < :completedDate";
        hql += " ORDER BY jobNo ASC";
        Query query = getSession().createQuery(hql);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        query.setCalendar("completedDate", cal);
        query.setString("userId", userId);
        List<Integer> jobIds = query.list();
        return jobIds;
        
    }
    public List<AnalysisJob> getAnalysisJobs(Date date) {
        String hql = "from org.genepattern.server.domain.AnalysisJob as j where j.completedDate < :completedDate";
        hql += " ORDER BY jobNo ASC";
        Query query = getSession().createQuery(hql);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        query.setCalendar("completedDate", cal);
        List<AnalysisJob> aJobs = query.list();
        return aJobs;
    }

    /**
     * Fetches list of JobInfo based on completion date on or before a specified date
     * 
     * @param date
     * @return <CODE>JobInfo[]</CODE>
     */
    public JobInfo[] getJobInfo(java.util.Date date) {
        List<AnalysisJob> aJobs = getAnalysisJobs(date);
        JobInfo[] results = new JobInfo[aJobs.size()];
        for (int i = 0, size = aJobs.size(); i < size; i++) {
            JobInfo ji = new JobInfo(aJobs.get(i));
            results[i] = ji;
        }
        return results;
    }

    /** 
     * Get the total number of all top-level, non deleted jobs.
     * Top level means the job is not a step in a pipeline.

     * @return
     */
    public int getNumJobsTotal() {
        Query query = getSession().getNamedQuery("getNumJobs");
        query.setBoolean("deleted", false);
        Object rval = query.uniqueResult();
        return getCount(rval);
    }

    /**
     * Get the number of jobs owned by the user.
     * 
     * @param userId
     * @return
     */
    public int getNumJobsByUser(final String userId) {
        Query query = getSession().getNamedQuery("getNumJobsByUser");
        query.setBoolean("deleted", false);
        query.setString("userId", userId);
        Object rval = query.uniqueResult();
        return getCount(rval);
    }
    
    /**
     * Get the number of jobs that are either owned by the user or that are in one of the given groups.
     * 
     * @param userId 
     * @param groupIds - a list of zero or more groups
     * 
     * @return
     */
    public int getNumJobsByUser(String userId, Set<String> groupIds) {
        if (groupIds == null || groupIds.size() == 0) {
            return getNumJobsByUser(userId);
        }
        Query query = getSession().getNamedQuery("getNumJobsByUserIncludingGroups");
        query.setBoolean("deleted", false);
        query.setString("userId", userId);
        query.setParameterList("groupId", groupIds);
        Object rval = query.uniqueResult();
        return getCount(rval);
    }

    /**
     * Get the number of jobs in at least one of the given groups. Don't count duplicates.
     * 
     * @param groupIds
     * @return
     * @throws OmnigeneException
     */
    public int getNumJobsInGroups(Set<String> groupIds) throws OmnigeneException {
        Query query = getSession().getNamedQuery("getNumJobsInGroups");
        query.setBoolean("deleted", false);
        query.setParameterList("groupId", groupIds);
        Object rval = query.uniqueResult();
        return getCount(rval);
    }
    
    /**
     * Helper method for converting from query#getUniqeResult to an int.
     * 
     * @param rval
     * @return
     */
    public static int getCount(Object rval) {
        if (rval instanceof Long) {
            return ((Long)rval).intValue();
        }
        else if (rval instanceof Integer) {
            return ((Integer)rval).intValue();
        }
        else if (rval instanceof Number) {
            return ((Number)rval).intValue();
        }
        else {
            throw new OmnigeneException("Unknown type returned from hibernate query: "+rval);
        }
    }

    public JobInfo[] getJobs(String ownedByUsername, int maxJobNumber, int maxEntries, boolean includeDeletedJobs)
	    throws OmnigeneException {
	return getJobs(ownedByUsername, maxJobNumber, maxEntries, includeDeletedJobs, JobSortOrder.JOB_NUMBER, false);
    }

    public JobInfo[] getJobs(String ownedByUsername, int maxJobNumber, int maxEntries, boolean includeDeletedJobs,
	    JobSortOrder sortOrder, boolean ascending) throws OmnigeneException {
        return getPagedJobs(ownedByUsername, maxJobNumber, maxEntries, includeDeletedJobs, sortOrder, ascending);
    }
    
    public JobInfo[] getJobs(String username, Set<String> groups, int firstResult, int maxResults, boolean includeDeletedJobs, JobSortOrder sortOrder, boolean ascending) {
        return getPagedJobs(username, groups, firstResult, maxResults, includeDeletedJobs, sortOrder, ascending);
    }
    
    /**
     * Get all of the jobs in at least one of the given groups.
     * 
     * @param groups
     * @param firstResult
     * @param maxResults
     * @param includeDeletedJobs
     * @param sortOrder
     * @param ascending
     * @return
     */
    public JobInfo[] getJobsInGroup(Set<String> groups, int firstResult, int maxResults, boolean includeDeletedJobs, JobSortOrder sortOrder, boolean ascending) {
        boolean getAllJobs = false;
        boolean filterByGroup = true;
        boolean includeGroups = true;
        return getPagedJobs(getAllJobs, filterByGroup, includeGroups, null, groups, firstResult, maxResults, includeDeletedJobs, sortOrder, ascending);
    }

    private JobInfo[] getPagedJobs(String ownedByUsername, int firstResult, int maxResults, boolean includeDeletedJobs, JobSortOrder sortOrder, boolean ascending)
    throws OmnigeneException 
    {
        return getPagedJobs(ownedByUsername, null, firstResult, maxResults, includeDeletedJobs, sortOrder, ascending);
    }

    /**
     * Get the next page of JobInfos.
     * 
     * Use cases:
     * 1. All jobs owned by a given user
     *    ... where a.username = <username>
     * 2. All jobs owned by a given user, or visible to one of the given groups. (The intention is to pass in the set of groups that the given user is a member of). 
     *    ... where a.username = <username> or a.job_no = 
     * 3. All jobs. Should only be called for an admin user.
     * 
     * [option: Included deleted jobs in the report]
     * 
     * @param username
     * @param groups
     * @param firstResult
     * @param maxResults
     * @param includeDeletedJobs
     * @param sortOrder
     * @param ascending
     * @return
     * @throws OmnigeneException
     */
    private JobInfo[] getPagedJobs(String username, Set<String> groups, int firstResult, int maxResults, boolean includeDeletedJobs, JobSortOrder sortOrder, boolean ascending)
    throws OmnigeneException 
    {
        //three exclusive states: [owned_by_user | all_jobs | viewable_by_user]
        boolean getAllJobs = username == null;
        //boolean getJobsOwnedByUsername = false;
        boolean includeGroups = false;
        if (!getAllJobs) {
            includeGroups = groups != null && groups.size() > 0;
        }
        
        return getPagedJobs(getAllJobs, includeGroups, username, groups, firstResult, maxResults, includeDeletedJobs, sortOrder, ascending);
    }
    
    private JobInfo[] getPagedJobs(boolean getAllJobs, boolean includeGroups, String username, Set<String> groups, int firstResult, int maxResults, boolean includeDeletedJobs, JobSortOrder sortOrder, boolean ascending) {
        boolean filterByGroup = false;
        return getPagedJobs(getAllJobs, filterByGroup, includeGroups, username, groups, firstResult, maxResults, includeDeletedJobs, sortOrder, ascending);
    }

    private JobInfo[] getPagedJobs(boolean getAllJobs, boolean filterByGroup, boolean includeGroups, String username, Set<String> groups, int firstResult, int maxResults, boolean includeDeletedJobs, JobSortOrder sortOrder, boolean ascending) {

        StringBuffer hql = new StringBuffer(
                getAnalysisJobQuery(filterByGroup, groups, includeGroups, getAllJobs, includeDeletedJobs)
        );

        switch (sortOrder) {
        case JOB_NUMBER:
            hql.append(" ORDER BY a.jobNo");
            break;
        case JOB_STATUS:
            hql.append(" ORDER BY a.jobStatus");
            break;
        case SUBMITTED_DATE:
            hql.append(" ORDER BY a.submittedDate");
            break;
        case COMPLETED_DATE:
            hql.append(" ORDER BY a.completedDate");
            break;
        case USER:
            hql.append(" ORDER BY a.userId");
            break;
        case MODULE_NAME:
            hql.append(" ORDER BY a.taskName");
            break;
        }

        hql.append(ascending ? " ASC" : " DESC");
        Query query = getSession().createQuery(hql.toString());
        if (firstResult >= 0) {
            query.setFirstResult(firstResult);
        }
        if (maxResults < Integer.MAX_VALUE) {
            query.setMaxResults(maxResults);
        }
        if (username != null) {
            query.setString("username", username);
        }
        if (!includeDeletedJobs) {
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

    //TODO: use named queries (see AnalysisJob.hbm.xml) because it is easier to understand what is going on
    private String getAnalysisJobQuery(boolean filterByGroup, Set<String> groups, boolean includeGroups, boolean getAllJobs, boolean includeDeletedJobs) {
        /* 
        Example SQL query
        select (distinct a.job_no) ...
        or ...
        select a.job_no, a.user_id, a.task_name, g.group_id, g.permission_flag
          from analysis_job a 
            left outer join job_group g on a.job_no = g.job_no
          where
            a.deleted != 'true'
            and
            (a.parent != null or a. parent = -1)
            and
            ( a.user_id = 'test' or g.group_id in ('public') )
          order by user_id, group_id 
         */
        
        /*
         Example SQL query when filtering by group
         select a.job_no, a.user_id, a.task_name, g.group_id, g.permission_flag 
           from analysis_job a
             left outer join job_group g on a.job_no = g.job_no
         where
           a.deleted != 'true'
           and
           (a.parent != null or a.parent = -1)
           and
           g.group_id in ('administrators', 'public')
         */
        
        /*
         Example SQL query using subselect,
         Note: HQL and JPA QL support subqueries in the where clause only.
         select * from analysis_job a
         where
         a.job_no IN
         (select distinct a.job_no
          from analysis_job a 
          left outer join job_group p on a.job_no= p.job_no 
          where 
          ((a.parent = null) OR (a.parent = -1))  
          AND 
          a.deleted = 0 
          AND 
          ( a.user_id = 'pcarr'
            or 
            p.group_id in ( '*', 'gpdev' ) )  
         );   
         */
        
        StringBuffer hql = new StringBuffer("select a from org.genepattern.server.domain.AnalysisJob as a where a.jobNo IN (");

        hql.append(" select ");
        if (includeGroups) {
            hql.append(" distinct ");
        }
        hql.append(" a.jobNo from org.genepattern.server.domain.AnalysisJob as a ");
        if (includeGroups) {
            hql.append(" left join a.permissions as p ");
        }
        hql.append("where ((a.parent = null) OR (a.parent = -1)) ");
        if (!includeDeletedJobs) {
            hql.append(" AND a.deleted = :deleted ");
        }
        if (includeGroups) {
            if (filterByGroup) {
                hql.append(" AND ( p.groupId in ( ");                
            }
            else {
                hql.append(" AND ( a.userId = :username or p.groupId in ( ");
            }
            boolean first = true;
            for (String group_id : groups) {
                if (first) {
                    first = false;
                }
                else {
                    hql.append(", ");
                }
                hql.append("'"+group_id+"'");
            }
            hql.append(" ) ) ");
        }
        else if (!getAllJobs && !filterByGroup) {
            hql.append(" AND a.userId = :username ");
        }
        hql.append(" )");
        return hql.toString();
    }

    /**
     * get the next available suite LSID identifier from the database
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

    public Integer getParentJobId(int jobId) {
        String hql = " select parent from org.genepattern.server.domain.AnalysisJob where jobNo = :jobNo";
        Query query = getSession().createQuery(hql);
        query.setInteger("jobNo", jobId);
        Integer parentJobId = (Integer) query.uniqueResult();
        if (parentJobId != null) {
            return parentJobId;
        }
        log.error("Unable to get parent job id for job: "+jobId);
        return -1;
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
    public Integer recordClientJob(int taskID, String user_id, ParameterInfo[] parameterInfoArray, int parentJobNumber)
    throws OmnigeneException {
        Integer jobNo = null;
        try {
            jobNo = this.addNewJob(user_id, taskID, parameterInfoArray, parentJobNumber);
        } 
        catch (JobSubmissionException e) {
            throw new OmnigeneException(e);
        }
        try {
            AnalysisJobDAO aHome = new AnalysisJobDAO();
            AnalysisJob aJob = aHome.findById(jobNo);

            JobStatus newStatus = (JobStatus) getSession().get(JobStatus.class, JobStatus.JOB_FINISHED);
            aJob.setStatus(newStatus);
            aJob.setDeleted(true);

            return jobNo;
        } 
        catch (OmnigeneException e) {
            log.error("Error in recordClientJob(taskID="+taskID+", user_id="+user_id+", parentJobNumber="+parentJobNumber+")", e);
            if (jobNo != null) {
                deleteJob(jobNo);
            }
            throw e;            
        }
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
    
    public int updateParameterInfo(Integer jobNo, String parameterInfo) {
        String hqlUpdate = "update org.genepattern.server.domain.AnalysisJob job set job.parameterInfo = :parameterInfo where jobNo = :jobNo";
        Query query = HibernateUtil.getSession().createQuery( hqlUpdate );
        query.setString("parameterInfo", parameterInfo);
        query.setInteger("jobNo", jobNo);
        return query.executeUpdate();
    }

    /**
     * Updates task parameters
     * 
     * @param taskID, task ID
     * @param parameter_info, parameters as a xml string
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
            } 
            else {
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
            
            TaskInfoCache.instance().removeFromCache(taskId);

            return 1;
        } 
        catch (Exception e) {
            log.error(e);
            throw new OmnigeneException(e);
        }
    }

    /**
     * Updates task description and parameters
     * 
     * @param taskID, task ID
     * @param description, task description
     * @param parameter_info, parameters as a xml string
     * @return No. of updated records
     * @throws OmnigeneException
     * @throws RemoteException
     */
    public int updateTask(int taskId, String taskDescription, String parameter_info, String taskInfoAttributes, String user_id, int access_id) 
    throws OmnigeneException {
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
            } 
            else {
                task.setLsid(null);
            }

            // Not neccessary ?
            Object mergedTaskObj = getSession().merge(task);
            if (mergedTaskObj != task) {
                //we are here most likely as a result of importing a pipeline, which has a nested pipeline which includes
                //a duplicate module
                String errorMessage = "Duplicate installation of task";
                if (mergedTaskObj instanceof TaskMaster) {
                    TaskMaster mergedTask = (TaskMaster) mergedTaskObj;
                    errorMessage +=  mergedTask.getTaskName() + ", lsid=" + mergedTask.getLsid();
                    task = mergedTask;
                }
                log.error(errorMessage);
            }

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
                Object mergedLsidHibernate = getSession().merge(lsidHibernate);
                if (mergedLsidHibernate != lsidHibernate) {
                    String warningMessage = "Duplicate installation of task, lsid="+lsid.toString();
                    if (mergedLsidHibernate instanceof Lsid) {
                        lsidHibernate = (Lsid) mergedLsidHibernate;
                    }
                    log.warn(warningMessage);
                }
            }
            getSession().flush();
            getSession().clear();
            
            TaskInfoCache.instance().removeFromCache(taskId);

            return 1;
        } 
        catch (Exception e) {
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
        } 
        else if (result instanceof BigInteger) {
            count = ((BigInteger) result).intValue();
        }
        else if (result instanceof BigDecimal) {
            try {
                count = ((BigDecimal) result).intValueExact();
            } 
            catch (ArithmeticException e) {
                log.error("Invalid conversion from BigDecimal to int", e);
            }
        } 
        else {
            log.error("Unknown type returned from query: " + result.getClass().getName());
        }
        return count;
    }

}
