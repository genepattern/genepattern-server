/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webservice.server.dao;

import java.io.File;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.persistence.criteria.JoinType;

import org.apache.log4j.Logger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.genepattern.server.DataManager;
import org.genepattern.server.JobIDNotFoundException;
import org.genepattern.server.auth.GroupPermission;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.UserUploadFile;
import org.genepattern.server.dm.userupload.UserUploadManager;
import org.genepattern.server.domain.AnalysisJob;
import org.genepattern.server.domain.AnalysisJobDAO;
import org.genepattern.server.domain.AnalysisJobArchive;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.domain.Lsid;
import org.genepattern.server.domain.TaskMaster;
import org.genepattern.server.executor.JobSubmissionException;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.job.comment.JobComment;
import org.genepattern.server.job.tag.JobTag;
import org.genepattern.server.webservice.server.Analysis.JobSortOrder;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.util.LsidVersion;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterFormatConverter;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.TaskInfoCache;
import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.LoadQueryInfluencers;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.impl.CriteriaImpl;
import org.hibernate.impl.SessionImpl;
import org.hibernate.loader.OuterJoinLoader;
import org.hibernate.loader.criteria.CriteriaLoader;
import org.hibernate.persister.entity.OuterJoinLoadable;

/**
 * AnalysisDAO.java
 * 
 * @author rajesh kuttan, Hui Gong
 * @version
 */
public class AnalysisDAO extends BaseDAO {
    public static Logger log = Logger.getLogger(AnalysisDAO.class);
    

    /** @deprecated */
    public AnalysisDAO() {
    }

    public AnalysisDAO(HibernateSessionManager mgr) {
        super(mgr);
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
        //Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -30);
        query.setDate("date", cal.getTime());

        @SuppressWarnings("unchecked")
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
        @SuppressWarnings("unchecked")
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
        @SuppressWarnings("unchecked")
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
        @SuppressWarnings("unchecked")
        List<AnalysisJob> results = query.list();
        List<JobInfo> jobInfos = convertResults(results);
        return jobInfos;
    }

    /**
     * Get the list of jobs with a specific tag
     *
     * @param pageNum
     * @param pageSize
     * @param jobSortOrder
     * @param ascending
     * @return
     */
    public List<JobInfo> getPagedJobsWithTag(final String tag, final String userId, final String batchId, final Set<String> groupIds, final int pageNum, final int pageSize, final JobSortOrder jobSortOrder,
                                             final boolean ascending) {

        int firstResult = (pageNum - 1) * pageSize;
        int maxResults = pageSize;

        mgr.beginTransaction();

        List<JobInfo> jobInfos = null;

        if (batchId != null && batchId.length() > 0)
        {
            StringBuffer hql = new StringBuffer();
            hql.append("select distinct a from " + JobTag.class.getName() + " as jt inner join jt.analysisJob as a where"
                    + " jt.tagObj.tag like lower('%" + tag + "%') "
                    + " and a.deleted=:deleted and a.jobNo in (select aj from BatchJob as ba"
                    + " inner join ba.batchJobs as aj where ba.jobNo=:batchId)");

            appendSortOrder(hql, jobSortOrder, ascending);

            Query query = mgr.getSession().createQuery(hql.toString());
            query.setBoolean("deleted", false);
            query.setFirstResult(firstResult);
            query.setMaxResults(maxResults);
            query.setString("batchId", batchId);

            @SuppressWarnings("unchecked")
            List<AnalysisJob> jobTags = query.list();

            jobInfos = new ArrayList<JobInfo>(jobTags.size());
            for (AnalysisJob analysisJob : jobTags)
            {
                JobInfo jobInfo = new JobInfo(analysisJob);
                jobInfos.add(jobInfo);
            }
        }
        else
        {
            Criteria criteria = mgr.getSession().createCriteria(JobTag.class, "jobtag").createCriteria("analysisJob")
                    .createAlias("jobtag.tagObj", "tagObj")
                    .add(Restrictions.like("tagObj.tag", tag, MatchMode.ANYWHERE).ignoreCase())
                    .setFirstResult(firstResult).setMaxResults(maxResults);

            SortOrder sortOrder = generateSortOrder(jobSortOrder, ascending, null);
            if(sortOrder != null)
            {
                criteria.addOrder(sortOrder);
            }

            if (groupIds != null && groupIds.size() > 0)
            {
                criteria.createAlias("permissions", "permissions")
                        .add(Restrictions.in("permissions.group_id", groupIds.toArray()));
            }
            if(userId != null)
            {
                criteria.add(Restrictions.eq("userId", userId));
            }
            //return unique job results
            criteria.setProjection(
                    Projections.alias(Projections.distinct(
                            Projections.property("jobtag.analysisJob")), sortOrder.getPropertyName()));

            @SuppressWarnings("unchecked")
            List<AnalysisJob> analysisJobs = criteria.list();

            jobInfos = new ArrayList<JobInfo>(analysisJobs.size());
            for(AnalysisJob analysisJob : analysisJobs) {
                JobInfo jobInfo = new JobInfo(analysisJob);
                jobInfos.add(jobInfo);
            }
        }
        return jobInfos;
    }

    /**
     * Get the list of jobs with a specific tag
     *
     * @return
     */
    public int getJobsWithTagCount(final String tag, final String userId, final String batchId, final Set<String> groupIds) {
        mgr.beginTransaction();

        int jobCount = 0;

        if (batchId != null && batchId.length() > 0)
        {
            StringBuffer hql = new StringBuffer();
            hql.append("select count(distinct a) from " + JobTag.class.getName() + " as jt inner join jt.analysisJob as a where"
                    + " jt.tagObj.tag like '%" + tag + "%' "
                    + " and a.deleted=:deleted and a.jobNo in (select aj from BatchJob as ba"
                    + " inner join ba.batchJobs as aj where ba.jobNo=:batchId)");

            Query query = mgr.getSession().createQuery(hql.toString());
            query.setBoolean("deleted", false);
            query.setString("batchId", batchId);

            jobCount = getCount(query.uniqueResult());
        }
        else
        {
            Criteria criteria = mgr.getSession().createCriteria(JobTag.class, "jobtag")
                    .createAlias("jobtag.tagObj", "tagObj").createAlias("jobtag.analysisJob", "analysisJob")
                    .add(Restrictions.like("tagObj.tag", tag, MatchMode.ANYWHERE).ignoreCase());

            if (groupIds != null && groupIds.size() > 0)
            {
                criteria.createAlias("analysisJob.permissions", "permissions")
                        .add(Restrictions.in("permissions.groupId", groupIds.toArray()));
            }
            if(userId != null)
            {
                criteria.add(Restrictions.eq("analysisJob.userId", userId));
            }

            ProjectionList projectionList=Projections.projectionList();
            projectionList.add(Projections.countDistinct("analysisJob"));
            criteria.setProjection(projectionList);

            jobCount = getCount(criteria.uniqueResult());
        }
        return jobCount;
    }


    private static SortOrder generateSortOrder(JobSortOrder jobSortOrder, boolean ascending, String alias)
    {
        SortOrder order = null;
        if(alias == null)
        {
            alias = "";
        }
        else
        {
            alias += ".";
        }

        switch (jobSortOrder) {
            case JOB_NUMBER:
                order = ascending ? SortOrder.asc(alias+"jobNo") : SortOrder.desc(alias+"jobNo");
                break;
            case JOB_STATUS:
                order = ascending ?  SortOrder.asc(alias+"jobStatus") :  SortOrder.desc(alias+"jobStatus");
                break;
            case SUBMITTED_DATE:
                order = ascending ?  SortOrder.asc(alias+"submittedDate") :  SortOrder.desc(alias+"submittedDate");
                break;
            case COMPLETED_DATE:
                order = ascending ?  SortOrder.asc(alias+"completedDate") :  SortOrder.desc(alias+"completedDate");
                break;
            case USER:
                order = ascending ?  SortOrder.asc(alias+"userId") :  SortOrder.desc(alias+"userId");
                break;
            case MODULE_NAME:
                order = ascending ?  SortOrder.asc(alias+"taskName") :  SortOrder.desc(alias+"taskName");
                break;
        }

        return order;
    }

    /**
     * Get the list of jobs with a specific tag
     *
     * @param pageNum
     * @param pageSize
     * @param jobSortOrder
     * @param ascending
     * @return
     */
    public List<JobInfo> getPagedJobsWithComment(final String comment, final String userId, final String batchId, final Set<String> groupIds, final int pageNum, final int pageSize, final JobSortOrder jobSortOrder,
                                             final boolean ascending)
    {

        int firstResult = (pageNum - 1) * pageSize;
        int maxResults = pageSize;

        mgr.beginTransaction();

        List<JobInfo> jobInfos = null;

        if (batchId != null && batchId.length() > 0)
        {
            StringBuffer hql = new StringBuffer();
            hql.append("select distinct a from " + JobComment.class.getName() + " as jc inner join jc.analysisJob as a where"
                    + " jc.comment like lower('%" + comment + "%') "
                    + " and a.deleted=:deleted and a.jobNo in (select aj from BatchJob as ba"
                    + " inner join ba.batchJobs as aj where ba.jobNo=:batchId)");

            appendSortOrder(hql, jobSortOrder, ascending);

            Query query = mgr.getSession().createQuery(hql.toString());
            query.setBoolean("deleted", false);
            query.setFirstResult(firstResult);
            query.setMaxResults(maxResults);
            query.setString("batchId", batchId);

            @SuppressWarnings("unchecked")
            List<AnalysisJob> analysisJobs = query.list();

            jobInfos = new ArrayList<JobInfo>(analysisJobs.size());
            for (AnalysisJob analysisJob : analysisJobs)
            {
                JobInfo jobInfo = new JobInfo(analysisJob);
                jobInfos.add(jobInfo);
            }
        }
        else
        {
            Criteria criteria = mgr.getSession().createCriteria(JobComment.class, "jobComment")
                    .createCriteria("analysisJob")
                    .add(Restrictions.like("jobComment.comment", comment, MatchMode.ANYWHERE).ignoreCase())
                    .setFirstResult(firstResult).setMaxResults(maxResults);

            SortOrder sortOrder = generateSortOrder(jobSortOrder, ascending, null);
            if(sortOrder != null)
            {
                criteria.addOrder(sortOrder);
            }

            if (groupIds != null && groupIds.size() > 0)
            {
                criteria.createAlias("permissions", "permissions")
                        .add(Restrictions.in("permissions.group_id", groupIds.toArray()));
            }
            if(userId != null)
            {
                criteria.add(Restrictions.eq("userId", userId));
            }

            //return unique job results
            criteria.setProjection(
                    Projections.alias(Projections.distinct(
                            Projections.property("jobComment.analysisJob")), sortOrder.getPropertyName()));

            @SuppressWarnings("unchecked")
            List<AnalysisJob> analysisJobs = criteria.list();

            jobInfos = new ArrayList<JobInfo>(analysisJobs.size());
            for(AnalysisJob analysisJob : analysisJobs) {
                JobInfo jobInfo = new JobInfo(analysisJob);
                jobInfos.add(jobInfo);
            }
        }
        return jobInfos;
    }

    
    /**
     * Get the list of jobs with a specific module name
     *
     * @return
     */
    public int getJobsWithModuleCount(final String module, final String userId, final String batchId, final Set<String> groupIds)
    {
       
        mgr.beginTransaction();

        int jobCount = 0;

        if (batchId != null && batchId.length() > 0)
        {
            StringBuffer hql = new StringBuffer();
            hql.append("select distinct a from " + AnalysisJob.class.getName() + "  where"
                    + " a.taskName like lower('%" + module + "%') "
                    + " and ((a.parent = null) OR (a.parent = -1)) "
                    + " and a.deleted=:deleted and a.jobNo in (select aj from BatchJob as ba"
                    + " inner join ba.batchJobs as aj where ba.jobNo=:batchId)");

          
            Query query = mgr.getSession().createQuery(hql.toString());
            query.setBoolean("deleted", false);
            
            query.setString("batchId", batchId);

            
            jobCount = (int)query.uniqueResult();
        }
        else {
        
            Criteria criteria = mgr.getSession().createCriteria(AnalysisJob.class)
                    .add(Restrictions.like("taskName", module, MatchMode.ANYWHERE).ignoreCase());
            
            boolean userClause = true;
            if (groupIds != null && groupIds.size() > 0)
            {
                //criteria.createAlias("permissions", "permissions")
                //        .add(Restrictions.in("permissions.groupId", groupIds.toArray()));
                
                if (userId == null) {
                    criteria.createAlias("permissions", "permissions")
                        .add(Restrictions.in("permissions.groupId", groupIds.toArray()));
                } else {
                    // if we have both userId and groups then selectall was chosen by a non-admin so we 
                    // need a disjunction here
                    Disjunction userGroupDisjunction = Restrictions.disjunction();
                    userGroupDisjunction.add(Restrictions.eq("userId", userId));
                    criteria.createAlias("permissions", "permissions", 1);
                    userGroupDisjunction.add(Restrictions.in("permissions.groupId", groupIds.toArray()));
                    userClause = false;
                    criteria.add(userGroupDisjunction);
                }
                
            }
            if ((userId != null) && userClause)
            {
                criteria.add(Restrictions.eq("userId", userId));
            }
    
            Disjunction objDisjunction = Restrictions.disjunction();
            /* Add multiple condition separated by OR clause within brackets. */
            objDisjunction.add(Restrictions.eq("parent", null));
            objDisjunction.add(Restrictions.lt("parent", 0));
            criteria.add(objDisjunction);
            
            //return count of distinct analysis jobs
            ProjectionList projectionList=Projections.projectionList();
            projectionList.add(Projections.countDistinct("jobNo"));
            criteria.setProjection(projectionList);
    
            // showCriteriaSql(criteria);
            
            jobCount = getCount(criteria.uniqueResult());
        }
        return jobCount;
        
    }

    protected void showCriteriaSql(Criteria criteria){
        try {
            CriteriaImpl c = (CriteriaImpl) criteria;
            SessionImpl s = (SessionImpl) c.getSession();
            SessionFactoryImplementor factory = (SessionFactoryImplementor) s.getSessionFactory();
            String[] implementors = factory.getImplementors(c.getEntityOrClassName());
            LoadQueryInfluencers lqis = new LoadQueryInfluencers();
            CriteriaLoader loader = new CriteriaLoader((OuterJoinLoadable) factory.getEntityPersister(implementors[0]), factory, c, implementors[0], lqis);
            Field f = OuterJoinLoader.class.getDeclaredField("sql");
            f.setAccessible(true);
            String sql = (String) f.get(loader);
            
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
    
    /**
     * Get the list of jobs with a specific task name (module name)
     *
     * @param pageNum
     * @param pageSize
     * @param jobSortOrder
     * @param ascending
     * @return
     */
    public List<JobInfo> getPagedJobsWithModule(final String module, final String userId, final String batchId, final Set<String> groupIds, final int pageNum, final int pageSize, final JobSortOrder jobSortOrder,
                                             final boolean ascending)
    {

        int firstResult = (pageNum - 1) * pageSize;
        int maxResults = pageSize;
        
        
        mgr.beginTransaction();

        List<JobInfo> jobInfos = null;

        if (batchId != null && batchId.length() > 0)
        {
            StringBuffer hql = new StringBuffer();
            hql.append("select distinct a from " + AnalysisJob.class.getName() + "  where"
                    + " a.taskName like lower('%" + module + "%') "
                    + " and ((a.parent = null) OR (a.parent = -1))"
                    + " and a.deleted=:deleted and a.jobNo in (select aj from BatchJob as ba"
                    + " inner join ba.batchJobs as aj where ba.jobNo=:batchId)");

            appendSortOrder(hql, jobSortOrder, ascending);

            Query query = mgr.getSession().createQuery(hql.toString());
            query.setBoolean("deleted", false);
            query.setFirstResult(firstResult);
            query.setMaxResults(maxResults);
            query.setString("batchId", batchId);

            @SuppressWarnings("unchecked")
            List<AnalysisJob> analysisJobs = query.list();

            jobInfos = new ArrayList<JobInfo>(analysisJobs.size());
            for (AnalysisJob analysisJob : analysisJobs)
            {
                JobInfo jobInfo = new JobInfo(analysisJob);
                jobInfos.add(jobInfo);
            }
        }
        else
        {
            Criteria criteria = mgr.getSession().createCriteria(AnalysisJob.class, "aj")
                    .add(Restrictions.like("taskName", module, MatchMode.ANYWHERE).ignoreCase())
                    .setFirstResult(firstResult).setMaxResults(maxResults);

            SortOrder sortOrder = generateSortOrder(jobSortOrder, ascending, null);
            if(sortOrder != null)
            {
                criteria.addOrder(sortOrder);
            }

            boolean userClause  = true;
            if (groupIds != null && groupIds.size() > 0)
            {
                if (userId == null) {
                    criteria.createAlias("permissions", "permissions")
                        .add(Restrictions.in("permissions.groupId", groupIds.toArray()));
                } else {
                    // if we have both userId and groups then selectall was chosen by a non-admin so we 
                    // need to eliminate the current user's count which we add back in later
                    // because the permissions join will fail for this user's modules that have 
                    // not been shared to any groups
                    Disjunction userGroupDisjunction = Restrictions.disjunction();
                    userGroupDisjunction.add(Restrictions.eq("userId", userId));
                    criteria.createAlias("permissions", "permissions", 1); // 1 == left outer join
                    userGroupDisjunction.add(Restrictions.in("permissions.groupId", groupIds.toArray()));
                    
                    userClause = false;
                    criteria.add(userGroupDisjunction);
                }
            }
            if((userId != null) && userClause)
            {
                criteria.add(Restrictions.eq("userId", userId));
            }

            Disjunction objDisjunction = Restrictions.disjunction();
            /* Add multiple condition separated by OR clause within brackets. */
            objDisjunction.add(Restrictions.eq("parent", null));
            objDisjunction.add(Restrictions.lt("parent", 0));
            criteria.add(objDisjunction);
            
            //showCriteriaSql(criteria);
            @SuppressWarnings("unchecked")
            List<AnalysisJob> analysisJobs = criteria.list();

            jobInfos = new ArrayList<JobInfo>(analysisJobs.size());
            for(AnalysisJob analysisJob : analysisJobs) {
                JobInfo jobInfo = new JobInfo(analysisJob);
                jobInfos.add(jobInfo);
            }
        }
        return jobInfos;
    }
    
    
    
    /**
     * Get the list of jobs with a specific tag
     *
     * @return
     */
    public int getJobsWithCommentCount(final String comment, final String userId, final String batchId, final Set<String> groupIds)
    {
        mgr.beginTransaction();

        int jobCount = 0;

        if (batchId != null && batchId.length() > 0)
        {
            StringBuffer hql = new StringBuffer();
            hql.append("select count(distinct a) from " + JobComment.class.getName() + " as jc inner join jc.analysisJob as a where"
                    + " jc.comment like lower('%" + comment + "%') "
                    + " and a.deleted=:deleted and a.jobNo in (select aj from BatchJob as ba"
                    + " inner join ba.batchJobs as aj where ba.jobNo=:batchId)");

            Query query = mgr.getSession().createQuery(hql.toString());
            query.setBoolean("deleted", false);
            query.setString("batchId", batchId);

            jobCount = getCount(query.uniqueResult());
        }
        else
        {
            Criteria criteria = mgr.getSession().createCriteria(JobComment.class, "jobComment")
                    .createAlias("jobComment.analysisJob", "analysisJob")
                    .add(Restrictions.like("comment", comment, MatchMode.ANYWHERE).ignoreCase());

            if (groupIds != null && groupIds.size() > 0)
            {
                criteria.createAlias("analysisJob.permissions", "permissions")
                        .add(Restrictions.in("permissions.group_id", groupIds.toArray()));
            }
            if(userId != null)
            {
                criteria.add(Restrictions.eq("analysisJob.userId", userId));
            }

            //return count of distinct analysis jobs
            ProjectionList projectionList=Projections.projectionList();
            projectionList.add(Projections.countDistinct("analysisJob"));
            criteria.setProjection(projectionList);

            jobCount = getCount(criteria.uniqueResult());
        }
        return jobCount;
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
        @SuppressWarnings("unchecked")
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
            hql.append(orderBy("a.completedDate", ascending));
            hql.append(ascending ? " ASC" : " DESC");
            hql.append(", a.submittedDate");
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
    
    /**
     * Special-case for null dates in MySQL, GP-6887
     * From the MySQL doc ...
     *   "When using ORDER BY, NULL values are presented first, or last if you specify DESC to sort in descending order"
     * 
     * On the Job Summary page, NULL Completion Dates should appear first when sorting in descending order.
     * 
     * Example sql query which works in Oracle and MySQL
     * <pre>
       select * from analysis_job 
           order by 
               case when date_completed is NULL then 0 else 1 END,
               date_completed desc, 
               date_submitted desc
     * </pre>
     */
    protected String orderBy(final String colSpec, final boolean ascending) {
        if (ascending) {
            return " ORDER BY case when "+colSpec+" is NULL then 1 else 0 END, "+colSpec;
        }
        else {
            return " ORDER BY case when "+colSpec+" is NULL then 0 else 1 END, "+colSpec;
        }
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
            taskInfo = new AdminDAO(mgr).getTask(taskId);
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

            final JobStatus js=new JobStatus();
            js.setStatusId(JobStatus.JOB_PENDING);
            js.setStatusName(JobStatus.PENDING);
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
        @SuppressWarnings("unchecked")
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
        Query sqlQuery = mgr.getSession().createSQLQuery("delete from JOB_GROUP where job_no = :jobId");
        sqlQuery.setInteger("jobId", jobId);
        @SuppressWarnings("unused")
        int numDeleted = sqlQuery.executeUpdate();
        
        if (groupPermissions == null) {
            return;
        }
        
        for(GroupPermission gp : groupPermissions) {
            //insert into JOB_GROUP (job_no, group_id, permission_flag) values (<int: jobId>, <String: gp.groupId>, <int: gp.permission.flag>); 
            String sqlInsertStatement = 
                "insert into JOB_GROUP (job_no, group_id, permission_flag) values (:jobId, :groupId, :permissionFlag)";
            sqlQuery = mgr.getSession().createSQLQuery(sqlInsertStatement);
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
    public int addNewTask(final String taskName, final String user_id, final int access_id, final String description, final String parameter_info, final String taskInfoAttributes) 
    throws OmnigeneException {
        try {
            final TaskInfoAttributes tia = TaskInfoAttributes.decode(taskInfoAttributes);
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
        AnalysisJob aJob = (AnalysisJob) getSession().get(AnalysisJob.class, jobID);
        
        deleteJob(aJob);
//        final GpConfig gpConfig=ServerConfigurationFactory.instance();
//        String postJobDeleteScript = null;
//        try {
//            GpContext context = GpContext.createContextForJob(mgr, aJob.getUserId(), aJob.getJobNo());
//            postJobDeleteScript  = gpConfig.getGPProperty(context, "postJobDeleteScript", null);
//                                   
//        } catch (Throwable e){
//            e.printStackTrace();
//            log.error("Problem encountered with post job delete script: " +postJobDeleteScript ,e);
//        }
//        deleteJobDir(jobDir);
//        getSession().delete(aJob);
//        postDeleteScript( jobID,  jobDir, postJobDeleteScript);
    }
    
    public void deleteJob(AnalysisJob aJob) {
        
        // recursively delete the job directory
        File jobDir = new File(GenePatternAnalysisTask.getJobDir(Integer.toString(aJob.getJobNo())));
        String postJobDeleteScript = null;
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        GpContext context = null;
        try {
            context = GpContext.createContextForJob(mgr, aJob.getUserId(), aJob.getJobNo());
            postJobDeleteScript  = gpConfig.getGPProperty(context, "postJobDeleteScript", null);
        } catch (Throwable e){
            e.printStackTrace();
            
            log.error("a. Problem encountered with post job delete script: " +postJobDeleteScript + "  job: " + aJob.getJobNo() ,e);
        }
        
        deleteJobDir(jobDir);
        getSession().delete(aJob);
        postDeleteScript(aJob.getJobNo() ,  jobDir, postJobDeleteScript);
        // GP-8672 delete files uploaded with this job
        UserUploadManager.deleteUploadedFiles( mgr,  context,  aJob, false);
    }
    
    
    
    protected void postDeleteScript(int jobId, File jobDir, String postJobDeleteScript){
        try {
            
            String[] args = new String[2]; 
            args[0] = ""+jobId;
            args[1] = jobDir.getAbsolutePath();
            
            if (postJobDeleteScript != null){
                // Runtime.getRuntime().exec(postJobDeleteScript, args );
                ProcessBuilder pb = new ProcessBuilder(postJobDeleteScript, "bash", args[0], args[1]).inheritIO();
                Process p = pb.start();
                p.wait(10000);
            }
            
        } catch (Throwable e){
            e.printStackTrace();
            log.error("b. Problem encountered with post job delete script: " +postJobDeleteScript+ "  job: " + jobId ,e);
        }
    }

    /**
     * Recursively delete the job directory
     * @param jobDir
     */
    protected void deleteJobDir(File jobDir) {
        try {
            Delete del = new Delete();
            del.setDir(jobDir);
            del.setIncludeEmptyDirs(true);
            del.setProject(new Project());
            del.execute();
        }
        catch (Throwable t) {
            log.error("Error deleting job directory, name="+jobDir.getName()+", path="+jobDir.getPath(), t);
        }
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
        @SuppressWarnings("unchecked")
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
        @SuppressWarnings("unchecked")
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
        @SuppressWarnings("unchecked")
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
        
        System.out.println("QUERY STRING: " + query.getQueryString());
        
        AnalysisJob aJob = (AnalysisJob) query.uniqueResult();
        // If jobNo not found
        if (aJob == null) {
            throw new JobIDNotFoundException("AnalysisDAO:getJobInfo JobID " + jobNo + " not found");
        }
        return aJob;
    }

    
    public AnalysisJobArchive getAnalysisJobFromArchive(int jobNo) {
        String hql = " from org.genepattern.server.domain.AnalysisJobArchive where jobNo = :jobNo";
        Query query = getSession().createQuery(hql);
        query.setInteger("jobNo", jobNo);
        System.out.println("QUERY AJA: " + query.getQueryString());
        AnalysisJobArchive aJob = (AnalysisJobArchive) query.uniqueResult();
        
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
        
        return  getJobInfo(jobNo, false);
    }
    /**
     * Fetches JobInformation.  Allow deleted jobs to be used with the flag 
     * so that we can create provenance pipelines from deleted jobs
     * 
     * @param jobNo
     * @throws OmnigeneException
     * @throws RemoteException
     * @return <CODE>JobInfo</CODE>
     * @throws OmnigeneException
     */
    public JobInfo getJobInfo(int jobNo, boolean deletedOK) {
        AnalysisJob aJob = null;
        AnalysisJobArchive archiveJob = null;
        try {
            aJob = getAnalysisJob(jobNo);
            if (aJob.getDeleted() && !deletedOK) {
                log.error("AnalysisDAO.getJobInfo("+jobNo+"): job is deleted!");
                return null;
            }
            return new JobInfo(aJob);
            
        } catch(JobIDNotFoundException jobIDException) {
            
            //throw new JobIDNotFoundException("AnalysisDAO:getJobInfo JobID " + jobNo + " not found");
            if ((aJob == null) && deletedOK){
                archiveJob = getAnalysisJobFromArchive(jobNo);
                System.out.println("AnalysisJobArchive -- returned");
            }
            if (archiveJob.getDeleted() && !deletedOK) {
                log.error("AnalysisDAO.getJobInfo("+jobNo+"): job is deleted!");
                return null;
            }
            return new JobInfo(archiveJob);
        }
       
    }
  
    
    
    public List<Integer> getAnalysisJobIds(Date date) {
        String hql = "select jobNo from org.genepattern.server.domain.AnalysisJob as j where j.completedDate < :completedDate";
        hql += " ORDER BY jobNo ASC";
        Query query = getSession().createQuery(hql);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        query.setCalendar("completedDate", cal);
        @SuppressWarnings("unchecked")
        List<Integer> jobIds = query.list();
        return jobIds;
        
    }
    
//    select  distinct  a.job_No from analysis_job as a
//    where ((a.parent = null) OR (a.parent = -1))  
//    AND a.deleted = false  
//    AND a.job_no in (select job_no from job_group p where p.group_id in ( 'public', '*' ) )  
//    and a.user_Id = 'ted'
//    
    public List<Integer> getPublicAnalysisJobIdsForUser(final String userId, final Date date) {
        
        String hql = "select a.jobNo from org.genepattern.server.domain.AnalysisJob as a ";
        hql += " left join a.permissions as p  where a.userId = :userId and a.completedDate < :completedDate ";
        hql += " AND  ((a.parent = null) OR (a.parent = -1)) AND a.deleted = false ";
        hql += " AND   p.groupId in ('public','*')   ";
        hql += " ORDER BY a.jobNo ASC";
        Query query = getSession().createQuery(hql);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        query.setCalendar("completedDate", cal);
        query.setString("userId", userId);
       
          
        
        @SuppressWarnings("unchecked")
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
        @SuppressWarnings("unchecked")
        List<Integer> jobIds = query.list();
        return jobIds;
        
    }
    
    // TODO do the exclusion in the database
    public List<Integer> getNonPublicAnalysisJobIdsForUser(final String userId, final Date date) {
        List<Integer> allForUser = getAnalysisJobIdsForUser(userId, date);
        List<Integer> publicForUser = getPublicAnalysisJobIdsForUser(userId, date);
        
        
        
        allForUser.removeAll(publicForUser);
        
        
        
        return allForUser;
    }
        
    
    
    
    public List<AnalysisJob> getAnalysisJobs(Date date) {
        String hql = "from org.genepattern.server.domain.AnalysisJob as j where j.completedDate < :completedDate";
        hql += " ORDER BY jobNo ASC";
        Query query = getSession().createQuery(hql);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        query.setCalendar("completedDate", cal);
        @SuppressWarnings("unchecked")
        List<AnalysisJob> aJobs = query.list();
        return aJobs;
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
     * Get the number of 'processing' jobs for the given user.
       <pre>
       select count(*) from analysis_job where 
         user_id = '{userId}' 
         and (parent is null or parent < 0) and deleted = 0 and status_id not in  ( 3, 4 )
       </pre>
     * @param userId
     * @return
     */
    public int getNumProcessingJobsByUser(final String userId) {
        final String sqlString = "select count(*) from analysis_job where user_id = :userId "+
                "and (parent is null or parent < 0) and deleted = :deleted and status_id not in  ( 3, 4 )";
        Query sqlQuery = getSession().createSQLQuery(sqlString);
        sqlQuery.setString("userId", userId);
        sqlQuery.setBoolean("deleted", false);
        Object rval = sqlQuery.uniqueResult();
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
     * Get the number of jobs in at least one of the given groups. Don't count duplicates.
     *
     * @param groupIds
     * @return
     * @throws OmnigeneException
     */
    public int gettOTALrESULTS(Set<String> groupIds) throws OmnigeneException {
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
        @SuppressWarnings("unchecked")
        List<AnalysisJob> aJobs = query.list();
        for (AnalysisJob aJob : aJobs) {
            JobInfo ji = new JobInfo(aJob);
            results.add(ji);
        }
        return results.toArray(new JobInfo[] {});
    }

    
    private String getAnalysisJobQuery(boolean filterByGroup, Set<String> groups, boolean includeGroups, boolean getAllJobs, boolean includeDeletedJobs) {
        return getAnalysisJobQuery( filterByGroup, groups, includeGroups, getAllJobs, includeDeletedJobs, false);
    }
    private String getAnalysisJobIdQuery(boolean filterByGroup, Set<String> groups, boolean includeGroups, boolean getAllJobs, boolean includeDeletedJobs) {
        return getAnalysisJobQuery( filterByGroup, groups, includeGroups, getAllJobs, includeDeletedJobs, true);
    }
    
    
    //TODO: use named queries (see AnalysisJob.hbm.xml) because it is easier to understand what is going on
    private String getAnalysisJobQuery(boolean filterByGroup, Set<String> groups, boolean includeGroups, boolean getAllJobs, boolean includeDeletedJobs, boolean idsOnly) {
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
        
        StringBuffer hql;
        if (idsOnly) hql = new StringBuffer("select a.jobNo from org.genepattern.server.domain.AnalysisJob as a where a.jobNo IN (");
        else hql = new StringBuffer("select a from org.genepattern.server.domain.AnalysisJob as a where a.jobNo IN (");

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

    public String getNextTaskLSIDVersion(final LSID existingLsid, final LsidVersion.Increment versionIncrement) throws OmnigeneException {
        try {
            final LSID nextLsid = existingLsid.copy();
            final String requestedNextVersion=versionIncrement.nextVersion(existingLsid);
            nextLsid.setVersion(requestedNextVersion);

            final int count = this.getLsidCount(nextLsid);
            if ((count <= 0)  || (versionIncrement == LsidVersion.Increment.noincrement)) {
                if (log.isDebugEnabled()) {
                    log.debug("from '"+existingLsid.getVersion()+"' to '"+nextLsid.getVersion()+"' is available");
                }
                return requestedNextVersion;
            }

            log.warn("from '"+existingLsid.getVersion()+"' to '"+nextLsid.getVersion()+"' version already exists, fall back to original implementation");
            log.warn("existingLsid="+existingLsid+", versionIncrement="+versionIncrement);
            return getNextTaskLSIDVersion(existingLsid);
        } 
        catch (final Exception e) {
            e.printStackTrace();
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
        final String hql = " select parent from org.genepattern.server.domain.AnalysisJob as parent, "
                + " org.genepattern.server.domain.AnalysisJob as child "
                + " where child.jobNo = :jobNo and parent.jobNo = child.parent ";
        final Query query = getSession().createQuery(hql);
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
	getSession().update(job); // Not really necessary
	return 1;

    }
    
    public int updateParameterInfo(Integer jobNo, String parameterInfo) {
        String hqlUpdate = "update org.genepattern.server.domain.AnalysisJob job set job.parameterInfo = :parameterInfo where jobNo = :jobNo";
        Query query = mgr.getSession().createQuery( hqlUpdate );
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
    public int updateTask(int taskId, String taskDescription, String parameter_info, String taskInfoAttributes, String user_id, int access_id) 
            throws OmnigeneException {
        return updateTask(taskId, null, taskDescription, parameter_info, taskInfoAttributes, user_id, access_id);
    }
    /**
     * Updates task description and parameters
     * 
     * @param taskID, task ID
     * @param taskName, null or a task name
     * @param description, task description
     * @param parameter_info, parameters as a xml string
     * @return No. of updated records
     * @throws OmnigeneException
     * @throws RemoteException
     */
    public int updateTask(int taskId, String taskName, String taskDescription, String parameter_info, String taskInfoAttributes, String user_id, int access_id) 
    throws OmnigeneException {
        try {
            TaskMaster task = (TaskMaster) getSession().get(TaskMaster.class, taskId);
            String oldLSID = task.getLsid();
            task.setParameterInfo(parameter_info);
            task.setDescription(taskDescription);
            task.setTaskinfoattributes(taskInfoAttributes);
            task.setUserId(user_id);
            task.setAccessId(access_id);
            
            // JTL 10/22/2020  GP-8500
            // add taskName override now that we can do updates without changing the LSID version
            if ((taskName != null) && (!taskName.trim().isEmpty())){
                task.setTaskName(taskName);
            }

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

    private static class SortOrder extends Order
    {
        private static final long serialVersionUID = 4717635978676124641L;
        private String propertyName;

        protected SortOrder(String propertyName, boolean ascending)
        {
            super(propertyName, ascending);
            this.propertyName = propertyName;
        }

        public String getPropertyName()
        {
            return propertyName;
        }

        public static SortOrder asc(String propertyName)
        {
            SortOrder sortOrder = new SortOrder(propertyName, true);

            return sortOrder;
        }

        public static SortOrder desc(String propertyName)
        {
            SortOrder sortOrder = new SortOrder(propertyName, false);

            return sortOrder;
        }
    }
}


