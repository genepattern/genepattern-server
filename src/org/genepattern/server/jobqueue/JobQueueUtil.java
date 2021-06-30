/*******************************************************************************
 * Copyright (c) 2003-2021 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.jobqueue;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.executor.events.GpJobAddedEvent;
import org.genepattern.server.executor.events.JobEventBus;
import org.genepattern.server.job.status.Status;
import org.genepattern.webservice.JobInfo;
import org.hibernate.Query;
import org.hibernate.Session;
import java.math.BigInteger;

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
    
    static public List<JobQueue> getPendingJobs(final HibernateSessionManager mgr, int maxJobCount) 
    throws Exception
    {
      
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
        
    }
    

    static public String getJobOwner(Session session, int jobNo) {
        String hql = "select a.userId from org.genepattern.server.domain.AnalysisJob a where a.jobNo = :jobNo";
        
        Query query = session.createQuery(hql);
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
    
    
   
    
     
    static public Boolean canStartJob(final HibernateSessionManager mgr, int jobNo) 
            throws Exception   {
        try {
            
            // get the user & # of currently running jobs for users with jobs in the pending queue
             boolean inTransaction = mgr.isInTransaction();
            
            if (!inTransaction)  mgr.beginTransaction();
            Session session = mgr.getSession();
            
            String owner = getJobOwner(session, jobNo);
            GpContext serverContext = GpContext.createContextForUser(owner);
            int maxSimultaneousJobs = ServerConfigurationFactory.instance().getGPIntegerProperty(serverContext, "max_simultaneous_jobs",20);
           
            String hql2 = "select job_no from analysis_job where user_id = '"+owner+"' and (job_no in (select job_no from job_queue where status = 'DISPATCHING') )or (status_id in (2,5)) ";
            Query query2 = session.createSQLQuery(hql2);
            List<Integer> runJobs = query2.list();
            int  running = runJobs.size();
            
            // if we are over, check if its the special case of pipelines.  A pipeline will start a pending job
            // for itself and each of its children so if there are > maxSimultaneous steps in a pipeline, it will never
            // be able to start.  So we check again now to see if the provided jobNo is a pipeline or in a pipeline and then
            // remove the other pipeline jobs from the running count and test again
            // the +2 is to allow the pipeline and children to count as 1 and not zero 
            if (running >= maxSimultaneousJobs) {
                String hql3 = "select job_no from analysis_job where (parent != -1) and (parent in (select parent_job_no from job_queue where job_no = "+jobNo+"))  and (job_no in (select job_no from job_queue where status = 'DISPATCHING') )or (status_id in (2,5)) ";
                Query query3 = session.createSQLQuery(hql2);
                int relatedJobs = query3.list().size();
                if (relatedJobs >= 2 ){
                    running = running -relatedJobs +1;
                }
                
            }
            
            
            
            if (!inTransaction) mgr.closeCurrentSession();
            return running <  maxSimultaneousJobs;
        }
        catch (Throwable t) {
            t.printStackTrace();
            log.error("Error getting list of non-startable jobs from queue", t);
            return true;
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
