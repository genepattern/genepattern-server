/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.drm.dao;

import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.server.DbException;
import org.genepattern.server.database.HibernateSessionManager;

public class JobRunnerJobDao {
    private static final Logger log = Logger.getLogger(JobRunnerJobDao.class);

    public void insertJobRunnerJob(final HibernateSessionManager mgr, final JobRunnerJob jobRecord) {

        final boolean isInTransaction=mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            mgr.getSession().saveOrUpdate(jobRecord);
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
        }
        catch (Throwable t) {
            log.error("Error adding record for gpJobNo="+jobRecord.getGpJobNo(), t);
            mgr.rollbackTransaction();
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

    /** @deprecated */
    public JobRunnerJob selectJobRunnerJob(final Integer gpJobNo) throws DbException {
        return selectJobRunnerJob(org.genepattern.server.database.HibernateUtil.instance(), gpJobNo);
    }
    
    public JobRunnerJob selectJobRunnerJob(final HibernateSessionManager mgr, final Integer gpJobNo) throws DbException {
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            JobRunnerJob existing = (JobRunnerJob) mgr.getSession().get(JobRunnerJob.class, gpJobNo);
            return existing;
        }
        catch (Throwable t) {
            log.error("Error getting entry for gpJobNo="+gpJobNo,t);
            throw new DbException("Error getting entry for gpJobNo="+gpJobNo,t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    /** @deprecated */
    public void updateJobStatus(final Integer gpJobNo, final DrmJobStatus drmJobStatus) throws DbException {
        updateJobStatus(org.genepattern.server.database.HibernateUtil.instance(), gpJobNo, drmJobStatus);
    }
    
    public void updateJobStatus(final HibernateSessionManager mgr, final Integer gpJobNo, final DrmJobStatus drmJobStatus) throws DbException {
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            JobRunnerJob existing = (JobRunnerJob) mgr.getSession().get(JobRunnerJob.class, gpJobNo);
            if (existing==null) {
                log.error("No existing record for "+gpJobNo);
                return;
            }
            updateJobStatus(mgr, existing, drmJobStatus);
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
        }
        catch (Throwable t) {
            log.error("Error updating entry for gpJobNo="+gpJobNo,t);
            mgr.rollbackTransaction();
            throw new DbException("Error updating entry for gpJobNo="+gpJobNo+": "+t.getLocalizedMessage());
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

    /** @deprecated */
    public JobRunnerJob updateJobStatus(final JobRunnerJob existing, final DrmJobStatus jobStatus) { 
        return updateJobStatus(org.genepattern.server.database.HibernateUtil.instance(), existing, jobStatus);
    }
    
    /**
     * Update the existing job_runner_job entry with new values from the given jobStatus.
     * @param existing
     * @param drmJobStatus
     * @return the updated JobRunnerJob instance
     */
    public JobRunnerJob updateJobStatus(final HibernateSessionManager mgr, final JobRunnerJob existing, final DrmJobStatus jobStatus) { 
        //JobRunnerJob is immutable ... so evict it from the session before saving a new instance as an update
        JobRunnerJob update;
        if (existing==null) {
            log.error("existing JobRunnerJob entry is null");
            update = new JobRunnerJob.Builder().drmJobStatus(jobStatus).build();
        }
        else {
            boolean isInTransaction=mgr.isInTransaction();
            if (isInTransaction) {
                mgr.getSession().evict(existing);
            }
            update = new JobRunnerJob.Builder(existing)
                .drmJobStatus(jobStatus)
            .build();
        }
        saveOrUpdate(mgr, update);
        return update;
    }

    /** @deprecated */
    public void saveOrUpdate(final JobRunnerJob jobRunnerJob) {
        saveOrUpdate(org.genepattern.server.database.HibernateUtil.instance(), jobRunnerJob);
    }

    public void saveOrUpdate(final HibernateSessionManager mgr, final JobRunnerJob jobRunnerJob) {
        if (jobRunnerJob==null) {
            log.error("No entry to update");
            return;
        }
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            mgr.getSession().saveOrUpdate(jobRunnerJob);
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
        }
        catch (Throwable t) {
            log.error("Error updating entry for gpJobNo="+jobRunnerJob.getGpJobNo(),t);
            mgr.rollbackTransaction();
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

}
