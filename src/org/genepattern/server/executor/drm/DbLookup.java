/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.drm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.server.DbException;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.executor.drm.dao.JobRunnerJob;
import org.genepattern.server.executor.drm.dao.JobRunnerJobDao;
import org.hibernate.Query;

/**
 * Database implementation of the DrmLookup interface, using standard GP database and Hibernate mapping classes.
 * @author pcarr
 *
 */
public class DbLookup implements DrmLookup {
    private static final Logger log = Logger.getLogger(DbLookup.class);

    private final HibernateSessionManager mgr;
    private final String jobRunnerClassname;
    private final String jobRunnerName;

    public DbLookup(final HibernateSessionManager mgr, final String jobRunnerClassname, final String jobRunnerName) {
        this.mgr=mgr;
        this.jobRunnerClassname=jobRunnerClassname;
        this.jobRunnerName=jobRunnerName;
    }

    @Override
    public List<DrmJobRecord> getRunningDrmJobRecords() {
        final boolean isInTransaction=mgr.isInTransaction();
        final String[] runningJobStates = {
                DrmJobState.QUEUED.name(),
                DrmJobState.QUEUED_HELD.name(),
                DrmJobState.RUNNING.name(),
                DrmJobState.SUSPENDED.name(),
                DrmJobState.REQUEUED.name(),
                DrmJobState.REQUEUED_HELD.name()
        };
        try {
            mgr.beginTransaction();
            final String hql="from "+JobRunnerJob.class.getName()
                    +" where jobRunnerClassname = :jobRunnerClassname and jobRunnerName = :jobRunnerName "
                    +" and jobState in (:runningJobStates)"
                    +" order by gpJobNo";

            Query query=mgr.getSession().createQuery(hql);
            query.setString("jobRunnerClassname", jobRunnerClassname);
            query.setString("jobRunnerName", jobRunnerName);
            query.setParameterList("runningJobStates", runningJobStates);
            List<JobRunnerJob> dbRows=query.list();
            if (dbRows==null || dbRows.size()==0) {
                return Collections.emptyList();
            }
            List<DrmJobRecord> records=new ArrayList<DrmJobRecord>();
            for(final JobRunnerJob dbRow : dbRows) {
                records.add( JobRunnerJob.toDrmJobRecord(dbRow) );
            }
            
            return records;
        }
        catch (Throwable t) {
            log.error(t);
            return Collections.emptyList();
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

    @Override
    public DrmJobRecord lookupJobRecord(final Integer gpJobNo) {
        JobRunnerJob jobRunnerJob=null;
        try {
            jobRunnerJob=selectJobRunnerJob(mgr, gpJobNo);
        }
        catch (DbException e) {
            //ignore
        }
        return JobRunnerJob.toDrmJobRecord(jobRunnerJob);
    }

    @Override
    public void insertJobRecord(final DrmJobSubmission jobSubmission) {
        final JobRunnerJob job = new JobRunnerJob.Builder(jobRunnerClassname, jobSubmission).jobRunnerName(jobRunnerName).build();
        insertJobRunnerJob(mgr, job);
    }
    
    @Override
    public void updateJobStatus(final DrmJobRecord drmJobRecord, final DrmJobStatus drmJobStatus) {
        try {
            new JobRunnerJobDao().updateJobStatus(mgr, drmJobRecord.getGpJobNo(), drmJobStatus);
        }
        catch (DbException e) {
            //ignore, exception logged in JobRunnerJobDao
        }
    }

    public static void insertJobRunnerJob(final HibernateSessionManager mgr, final JobRunnerJob jobRecord) {
        new JobRunnerJobDao().insertJobRunnerJob(mgr, jobRecord);
    }

    public JobRunnerJob selectJobRunnerJob(final HibernateSessionManager mgr, final Integer gpJobNo) throws DbException {
        return new JobRunnerJobDao().selectJobRunnerJob(mgr, gpJobNo);
    }
    
}
