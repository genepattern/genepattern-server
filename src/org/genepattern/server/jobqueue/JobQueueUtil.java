/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.jobqueue;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.executor.events.GpJobAddedEvent;
import org.genepattern.server.executor.events.JobEventBus;
import org.genepattern.server.job.status.Status;
import org.genepattern.webservice.JobInfo;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 * Helper class for managing internal job status.
 * 
 * @author pcarr
 */
public class JobQueueUtil {
    private static Logger log = Logger.getLogger(JobQueueUtil.class);

    /** @deprecated pass in a Hibernate session */
    static public void addJobToQueue(JobInfo jobInfo, JobQueue.Status statusId) throws Exception {
        addJobToQueue(org.genepattern.server.database.HibernateUtil.instance(), 
                jobInfo, statusId);
    }
    
    static public void addJobToQueue(final HibernateSessionManager mgr, JobInfo jobInfo, JobQueue.Status statusId) 
    throws Exception
    {
        final boolean isInTransaction = mgr.isInTransaction();

        JobQueue record = new JobQueue();
        record.setJobNo(jobInfo.getJobNumber());
        record.setParentJobNo(jobInfo._getParentJobNumber());
        record.setDateSubmitted(jobInfo.getDateSubmitted());
        record.setStatus(statusId.toString());

        try {
            mgr.beginTransaction();
            mgr.getSession().saveOrUpdate( record );
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
        }
        catch (Throwable t) {
            mgr.rollbackTransaction();
            throw new Exception("Error saving internal job status to db: "+t.getLocalizedMessage());
        }
        
        fireGpJobAddedEvent(jobInfo);
    }
    
    /**
     * Fire a new job added event.
     */
    protected static void fireGpJobAddedEvent(final JobInfo jobInfo) {
        Status jobStatus = new Status.Builder()
            .jobInfo(jobInfo)
        .build();
        GpJobAddedEvent event = new GpJobAddedEvent(jobInfo.getTaskLSID(), jobStatus);
        JobEventBus.instance().post(event);
    }

    /** @deprecated pass in a Hibernate session */
    static public List<JobQueue> getPendingJobs(int maxJobCount) throws Exception {
        return getPendingJobs(org.genepattern.server.database.HibernateUtil.instance(),
                maxJobCount);
    }
    
    static public List<JobQueue> getPendingJobs(final HibernateSessionManager mgr, int maxJobCount) 
    throws Exception
    {
        boolean inTransaction = mgr.isInTransaction();
        try {
            String hql = "from "+JobQueue.class.getName()+" where status = :status order by dateSubmitted ";
            mgr.beginTransaction();
            Session session = mgr.getSession();
            Query query = session.createQuery(hql);
            if (maxJobCount > 0) {
                query.setMaxResults(maxJobCount);
            }
            query.setString("status", JobQueue.Status.PENDING.toString());
            @SuppressWarnings("unchecked")
            List<JobQueue> records = query.list();
            return records;
        }
        catch (Throwable t) {
            log.error("Error getting list of pending jobs from queue", t);
            return new ArrayList<JobQueue>();
        }
        finally {
            if (!inTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    /** @deprecated pass in a Hibernate session */
    static public void setJobStatus(int jobNo, JobQueue.Status status) throws Exception {
        setJobStatus(org.genepattern.server.database.HibernateUtil.instance(), 
                jobNo, status);
    }

    static public void setJobStatus(final HibernateSessionManager mgr, int jobNo, JobQueue.Status status) 
    throws Exception
    {
        boolean inTransaction = mgr.isInTransaction();
        try {
            String hql = "from " + JobQueue.class.getName() + " where jobNo = :jobNo";

            mgr.beginTransaction();
            Session session = mgr.getSession();
            Query query = session.createQuery(hql);
            query.setParameter("jobNo", jobNo);
            JobQueue record = (JobQueue) query.uniqueResult();

            if (record != null) {
                record.setStatus(status.toString());
                mgr.getSession().update(record);
                if (!inTransaction) {
                    mgr.commitTransaction();
                }
            }
        }
        catch (Throwable t) {
            mgr.rollbackTransaction();
            throw new Exception("Error setting job status to "+status+" for jobNo="+jobNo+": "+t.getLocalizedMessage());
        }
        finally {
            if (!inTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    /** @deprecated pass in a Hibernate session */
    static public List<JobQueue> getWaitingJobs(int parentJobNo) throws Exception {
        return getWaitingJobs(org.genepattern.server.database.HibernateUtil.instance(), 
                parentJobNo); 
    }
    
    static public List<JobQueue> getWaitingJobs(final HibernateSessionManager mgr, int parentJobNo) throws Exception {
        boolean inTransaction = mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            String hql = "from " + JobQueue.class.getName() + " where parentJobNo = :parentJobNo and status = :status order by jobNo";
            Session session = mgr.getSession();
            Query query = session.createQuery(hql);
            query.setParameter("parentJobNo", parentJobNo);
            query.setParameter("status", JobQueue.Status.WAITING.toString());
            @SuppressWarnings("unchecked")
            List<JobQueue> records = query.list();
            return records;
        }
        catch (Throwable t) {
            throw new Exception("Error getting waiting jobs from db, for parentJobNo="+parentJobNo+": "+t.getLocalizedMessage());
        }
        finally {
            if (!inTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    /** @deprecated pass in a Hibernate session */
    static public int deleteJobQueueStatusRecord(int jobNo) {
        return deleteJobQueueStatusRecord(org.genepattern.server.database.HibernateUtil.instance(), 
                jobNo);
    }

    /**
     * Remove the entry for the given job from the JOB_QUEUE table.
     * 
     * @param jobNo
     * @return the number of deleted records, should be 1 for a successful delete or 0 if there was no record in the DB.
     * @throws Exception if there were an unexpected number of records in the DB
     */
    static public int deleteJobQueueStatusRecord(final HibernateSessionManager mgr, int jobNo) {
        boolean inTransaction = mgr.isInTransaction();
        log.debug("job #"+jobNo+", isInTransaction="+inTransaction);
        try {
            mgr.beginTransaction();
            String hql = "delete "+JobQueue.class.getName()+" jq where jq.jobNo = :jobNo";
            Query query = mgr.getSession().createQuery( hql );
            query.setInteger("jobNo", jobNo);
            int numDeleted = query.executeUpdate();
            log.debug("for job #"+jobNo+", numDeleted="+numDeleted);
            if (!inTransaction) {
                log.debug("for job #"+jobNo+", DB commit");
                mgr.commitTransaction();
            }
            if (numDeleted>1) {
                log.error("Deleted more than one entry from JOB_QUEUE with jobNo="+jobNo+", numDeleted="+numDeleted);
            }
            return numDeleted;
        }
        catch (Throwable t) {
            log.error("Error removing record from JOB_QUEUE for job_no="+jobNo, t);
            mgr.rollbackTransaction();
            return 0;
        }
        finally {
            if (!inTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    /** @deprecated pass in a Hibernate session */
    static public int changeJobStatus(JobQueue.Status from, JobQueue.Status to) {
        return changeJobStatus(org.genepattern.server.database.HibernateUtil.instance(), 
                from, to);
    }
    
    /**
     * Change the status for all the entries in the JOB_QUEUE table which have the given 'from' status
     * to the given 'to' status.
     * 
     * @param from, the original status
     * @param to, the new status
     * @return the number of updated rows
     */
    static public int changeJobStatus(final HibernateSessionManager mgr, JobQueue.Status from, JobQueue.Status to) {
        int num=0;
        boolean inTransaction = mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            final String hql = "update "+JobQueue.class.getName()+" jq set jq.status = :toStatus where jq.status = :fromStatus";
            Query query = mgr.getSession().createQuery( hql );
            query.setString("fromStatus", from.name());
            query.setString("toStatus", to.name());
            num=query.executeUpdate();
            if (!inTransaction) {
                mgr.commitTransaction();
            }
            return num;
        }
        catch (Throwable t) {
            log.error("Error changing JobQueue status from "+from+" to "+to, t);
            mgr.rollbackTransaction();
        }
        finally {
            if (!inTransaction) {
                mgr.closeCurrentSession();
            }
        }
        return num;
    }

}
