package org.genepattern.server.jobqueue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
//import org.genepattern.server.jobqueue.dao.JobQueueStatusDao;
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

    static public void addJobToQueue(JobInfo jobInfo, JobQueue.Status statusId) 
    throws Exception
    {
        addJobToQueue(jobInfo.getJobNumber(), jobInfo._getParentJobNumber(), jobInfo.getDateSubmitted(), statusId);
    }

    static public void addJobToQueue(int jobNo, int parent, Date dateSubmitted, JobQueue.Status statusId) 
    throws Exception
    { 
        final boolean isInTransaction = HibernateUtil.isInTransaction();

        JobQueue record = new JobQueue();
        record.setJobNo(jobNo);
        record.setParentJobNo(parent);
        record.setDateSubmitted(dateSubmitted);
        record.setStatus(statusId.toString());

        try {
            HibernateUtil.beginTransaction();
            HibernateUtil.getSession().save( record );
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        catch (Throwable t) {
            HibernateUtil.rollbackTransaction();
            throw new Exception("Error saving internal job status to db: "+t.getLocalizedMessage());
        }
    }
    
    static public List<JobQueue> getPendingJobs(int maxJobCount) 
    throws Exception
    {
        boolean inTransaction = HibernateUtil.isInTransaction();
        try {
            String hql = "from "+JobQueue.class.getName()+" where status = :status order by dateSubmitted ";
            HibernateUtil.beginTransaction();
            Session session = HibernateUtil.getSession();
            Query query = session.createQuery(hql);
            if (maxJobCount > 0) {
                query.setMaxResults(maxJobCount);
            }
            query.setString("status", JobQueue.Status.PENDING.toString());
            List<JobQueue> records = query.list();
            return records;
        }
        catch (Throwable t) {
            log.error("Error getting list of pending jobs from queue", t);
            return new ArrayList<JobQueue>();
        }
        finally {
            if (!inTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    static public void setJobStatus(int jobNo, JobQueue.Status status) 
    throws Exception
    {
        boolean inTransaction = HibernateUtil.isInTransaction();
        try {
            String hql = "from " + JobQueue.class.getName() + " where jobNo = :jobNo";

            HibernateUtil.beginTransaction();
            Session session = HibernateUtil.getSession();
            Query query = session.createQuery(hql);
            query.setParameter("jobNo", jobNo);
            JobQueue record = (JobQueue) query.uniqueResult();

            if (record != null) {
                record.setStatus(status.toString());
                HibernateUtil.getSession().update(record);
                if (!inTransaction) {
                    HibernateUtil.commitTransaction();
                }
            }
        }
        catch (Throwable t) {
            HibernateUtil.rollbackTransaction();
            throw new Exception("Error setting job status to "+status+" for jobNo="+jobNo+": "+t.getLocalizedMessage());
        }
        finally {
            if (!inTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    static public List<JobQueue> getWaitingJobs(int parentJobNo) throws Exception {
        boolean inTransaction = HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            String hql = "from " + JobQueue.class.getName() + " where parentJobNo = :parentJobNo and status = :status order by jobNo";
            Session session = HibernateUtil.getSession();
            Query query = session.createQuery(hql);
            query.setParameter("parentJobNo", parentJobNo);
            query.setParameter("status", JobQueue.Status.WAITING.toString());
            List<JobQueue> records = query.list();
            return records;
        }
        catch (Throwable t) {
            throw new Exception("Error getting waiting jobs from db, for parentJobNo="+parentJobNo+": "+t.getLocalizedMessage());
        }
        finally {
            if (!inTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    static public void deleteJobQueueStatusRecord(int jobNo) throws Exception {
        boolean inTransaction = HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            String hql = "delete "+JobQueue.class.getName()+" jq where jq.jobNo = :jobNo and jq.status = :status";
            Query query = HibernateUtil.getSession().createQuery( hql );
            query.setInteger("jobNo", jobNo);
            query.setString("status", JobQueue.Status.PENDING.toString());
            int numDeleted = query.executeUpdate();
            if (numDeleted == 1) {
                if (!inTransaction) {
                    HibernateUtil.commitTransaction();
                }
            }
            else {
                //TODO: log error
                HibernateUtil.rollbackTransaction();
            }
        }
        catch (Throwable t) {
            HibernateUtil.rollbackTransaction();
            throw new Exception(t);
        }
    }
    
    /**
     * Change the status for all the entries in the JOB_QUEUE table which have the given 'from' status
     * to the given 'to' status.
     * 
     * @param from, the original status
     * @param to, the new status
     * @return the number of updated rows
     */
    static public int changeJobStatus(JobQueue.Status from, JobQueue.Status to) {
        int num=0;
        boolean inTransaction = HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            final String hql = "update "+JobQueue.class.getName()+" jq set jq.status = :toStatus where jq.status = :fromStatus";
            Query query = HibernateUtil.getSession().createQuery( hql );
            query.setString("fromStatus", from.name());
            query.setString("toStatus", to.name());
            num=query.executeUpdate();
            if (!inTransaction) {
                HibernateUtil.commitTransaction();
            }
            return num;
        }
        catch (Throwable t) {
            log.error("Error changing JobQueue status from "+from+" to "+to, t);
            HibernateUtil.rollbackTransaction();
        }
        finally {
            if (!inTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
        return num;
    }

}
