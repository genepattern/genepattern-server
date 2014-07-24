package org.genepattern.server.executor.drm.dao;

import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.server.DbException;
import org.genepattern.server.database.HibernateUtil;

public class JobRunnerJobDao {
    private static final Logger log = Logger.getLogger(JobRunnerJobDao.class);

    public void insertJobRunnerJob(final JobRunnerJob jobRecord) {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            HibernateUtil.getSession().saveOrUpdate(jobRecord);
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        catch (Throwable t) {
            log.error("Error adding record for gpJobNo="+jobRecord.getGpJobNo(), t);
            HibernateUtil.rollbackTransaction();
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

    public JobRunnerJob selectJobRunnerJob(final Integer gpJobNo) throws DbException {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            JobRunnerJob existing = (JobRunnerJob) HibernateUtil.getSession().get(JobRunnerJob.class, gpJobNo);
            return existing;
        }
        catch (Throwable t) {
            log.error("Error getting entry for gpJobNo="+gpJobNo,t);
            throw new DbException("Error getting entry for gpJobNo="+gpJobNo,t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    public void updateJobStatus(final Integer gpJobNo, final DrmJobStatus drmJobStatus) {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            JobRunnerJob existing = (JobRunnerJob) HibernateUtil.getSession().get(JobRunnerJob.class, gpJobNo);
            if (existing==null) {
                log.error("No existing record for "+gpJobNo);
                return;
            }
            //JobRunnerJob is immutable ... so evict it from the session before saving a new instance as an update
            HibernateUtil.getSession().evict(existing);
            JobRunnerJob update = new JobRunnerJob.Builder(existing).drmJobStatus(drmJobStatus).build();
            HibernateUtil.getSession().saveOrUpdate(update);
            HibernateUtil.commitTransaction();
        }
        catch (Throwable t) {
            log.error("Error updating entry for gpJobNo="+gpJobNo,t);
            HibernateUtil.rollbackTransaction();
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

}
