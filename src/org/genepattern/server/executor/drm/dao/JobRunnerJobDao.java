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
    
    public void updateJobStatus(final Integer gpJobNo, final DrmJobStatus drmJobStatus) throws DbException {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            JobRunnerJob existing = (JobRunnerJob) HibernateUtil.getSession().get(JobRunnerJob.class, gpJobNo);
            if (existing==null) {
                log.error("No existing record for "+gpJobNo);
                return;
            }
            updateJobStatus(existing, drmJobStatus);
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        catch (Throwable t) {
            log.error("Error updating entry for gpJobNo="+gpJobNo,t);
            HibernateUtil.rollbackTransaction();
            throw new DbException("Error updating entry for gpJobNo="+gpJobNo+": "+t.getLocalizedMessage());
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

    /**
     * Update the existing job_runner_job entry with new values from the given jobStatus.
     * @param existing
     * @param drmJobStatus
     * @return the updated JobRunnerJob instance
     */
    public JobRunnerJob updateJobStatus(final JobRunnerJob existing, final DrmJobStatus jobStatus) {
        //JobRunnerJob is immutable ... so evict it from the session before saving a new instance as an update
        HibernateUtil.getSession().evict(existing);
        JobRunnerJob update = new JobRunnerJob.Builder(existing).drmJobStatus(jobStatus).build();
        saveOrUpdate(update);
        return update;
    }

    public void saveOrUpdate(final JobRunnerJob jobRunnerJob) {
        if (jobRunnerJob==null) {
            log.error("No entry to update");
            return;
        }
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            HibernateUtil.getSession().saveOrUpdate(jobRunnerJob);
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        catch (Throwable t) {
            log.error("Error updating entry for gpJobNo="+jobRunnerJob.getGpJobNo(),t);
            HibernateUtil.rollbackTransaction();
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

}
