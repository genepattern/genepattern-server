package org.genepattern.server.jobqueue;

import java.util.ArrayList;
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

    static public void addJobToQueue(JobInfo jobInfo, JobQueueStatus.Status statusId) 
    throws Exception
    { 
        JobQueue record = new JobQueue();
        record.setJobNo(jobInfo.getJobNumber());
        record.setParentJobNo(jobInfo._getParentJobNumber());
        record.setDateSubmitted(jobInfo.getDateSubmitted());
        record.setStatus(statusId.toString());
        
        boolean inTransaction = HibernateUtil.isInTransaction();
        
        try {
            HibernateUtil.beginTransaction();
            HibernateUtil.getSession().save( record );
            if (!inTransaction) {
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
            query.setString("status", JobQueueStatus.Status.PENDING.toString());
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
    
    static public void setJobStatus(int jobNo, JobQueueStatus.Status status) 
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
            query.setParameter("status", JobQueueStatus.Status.WAITING.toString());
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
            query.setString("status", JobQueueStatus.Status.PENDING.toString());
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

}
