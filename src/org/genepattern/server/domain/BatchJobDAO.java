/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.domain;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.webservice.server.Analysis.JobSortOrder;
import org.genepattern.webservice.JobInfo;
import org.hibernate.Query;
import org.hibernate.SQLQuery;

public class BatchJobDAO extends BaseDAO {
    private static final Logger log = Logger.getLogger(BatchJobDAO.class);
    
    /** @deprecated pass in a Hibernate session */
    public BatchJobDAO() {
        super(org.genepattern.server.database.HibernateUtil.instance());
    }
    
    public BatchJobDAO(final HibernateSessionManager mgr) {
        super(mgr);
    }

    public BatchJob findById(Integer id) {
        log.debug("getting BatchJob instance with id: " + id);
        try {
            return (BatchJob) mgr.getSession().load("org.genepattern.server.domain.BatchJob", id);
        }
        catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }
    }

    @SuppressWarnings("unchecked")
    public List<BatchJob> getOlderThanDateForUser(final String userId, final Date date) {
        Query query = mgr.getSession().getNamedQuery("getOlderThanDateForUser");
        query.setString("userId", userId);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        query.setCalendar("olderThanDate", cal);
        return query.list();
    }
    
    @SuppressWarnings("unchecked")
    public List<BatchJob> getOlderThanDate(Date date) {
        Query query = mgr.getSession().getNamedQuery("getOlderThanDate");
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        query.setCalendar("olderThanDate", cal);
        return query.list();
    }
    
    /**
     * some JDBC drivers return Integer others return BigDecimal.
     */
    private int getIntFromResultObj(Object batchIdObj) throws IllegalArgumentException {
        if (batchIdObj instanceof Number) {
            return ((Number) batchIdObj).intValue();
        }
        throw new IllegalArgumentException("batchIdObj is not a Number, batchIdObj="+batchIdObj);
    }

    @SuppressWarnings("rawtypes")
    public void markDeletedIfLastJobDeleted(int lastJobDeleted) {
        Query query = mgr.getSession().getNamedQuery("getBatchOwnerOfJob");
        query.setInteger("jobId", lastJobDeleted);
        List batchIds = query.list();
        if (batchIds.size() > 0) {
            int batchId = getIntFromResultObj(batchIds.get(0));
            Query countMemberJobs = mgr.getSession().getNamedQuery("countJobsInBatch");
            countMemberJobs.setInteger("jobId", lastJobDeleted);
            countMemberJobs.setInteger("batchId", batchId);
            int remainingJobCount = getIntFromResultObj (countMemberJobs.list().get(0));
            if (remainingJobCount == 0) {
                BatchJob batchJob = (BatchJob) mgr.getSession().get(BatchJob.class, batchId);
                batchJob.setDeleted(true);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public List<BatchJob> findByUserId(String userId) {
        Query query = mgr.getSession().getNamedQuery("getBatchJobsForUser");
        query.setString("userId", userId);
        return query.list();
    }

    public JobInfo[] getBatchJobs(String userId, String batchFilter, int firstJob, int numJobs, JobSortOrder jobSortOrder, boolean jobSortAscending) {
        Query baseQueryText = mgr.getSession().getNamedQuery("getJobsInBatch");

        StringBuffer orderedQuery = new StringBuffer(baseQueryText.getQueryString());
        switch (jobSortOrder) {
        case JOB_NUMBER:
            orderedQuery.append(" ORDER BY JOB_NO");
            break;
        case JOB_STATUS:
            orderedQuery.append(" ORDER BY STATUS_ID");
            break;
        case SUBMITTED_DATE:
            orderedQuery.append(" ORDER BY DATE_SUBMITTED");
            break;
        case COMPLETED_DATE:
            orderedQuery.append(" ORDER BY DATE_COMPLETED");
            break;
        case USER:
            orderedQuery.append(" ORDER BY USER_ID");
            break;
        case MODULE_NAME:
            orderedQuery.append(" ORDER BY TASK_NAME");
            break;
        }
        orderedQuery.append(jobSortAscending ? " ASC" : " DESC");
        SQLQuery query = mgr.getSession().createSQLQuery(orderedQuery.toString());
        query.setInteger("batchId", Integer.parseInt(undecorate(batchFilter)));
        query.setBoolean("deleted", false);
        query.addEntity(AnalysisJob.class);
        query.setMaxResults(numJobs);
        query.setFirstResult(firstJob);

        @SuppressWarnings("unchecked")
        List<AnalysisJob> queryResults = query.list();
        List<JobInfo> results = new ArrayList<JobInfo>();
        for (AnalysisJob aJob : queryResults) {
            JobInfo ji = new JobInfo(aJob);
            results.add(ji);
        }
        return results.toArray(new JobInfo[] {});

    }

    public Integer getNumBatchJobs(final String batchFilter) {
        try {
            final String batchIdStr = undecorate(batchFilter);
            final int batchId = Integer.parseInt(batchIdStr);
            BatchJob batchJob = (BatchJob) mgr.getSession().load("org.genepattern.server.domain.BatchJob", batchId);
            return batchJob.getBatchJobs().size();
        }
        catch (RuntimeException re) {
            //the batch is gone.  Perhaps deleted or purged
            return 0;
        }
    }

    public String undecorate(String batchFilter) {
        if (batchFilter.startsWith(BatchJob.BATCH_KEY)) {
            return batchFilter.substring(BatchJob.BATCH_KEY.length());
        }
        else {
            return batchFilter;
        }
    }

}
