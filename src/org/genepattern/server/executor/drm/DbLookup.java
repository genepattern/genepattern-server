package org.genepattern.server.executor.drm;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.executor.drm.dao.JobRunnerJob;
import org.hibernate.Query;

/**
 * Database implementation of the DrmLookup interface, using standard GP database and Hibernate mapping classes.
 * @author pcarr
 *
 */
public class DbLookup implements DrmLookup {
    private static final Logger log = Logger.getLogger(DbLookup.class);

    private static File asFile(final File parent, final String path) {
        File rel=asFile(path);
        if (rel==null) {
            return null;
        }
        if (rel.isAbsolute()) {
            return rel;
        }
        if (parent != null) {
            return new File(parent, path);
        }
        return rel;
    }
    
    private static File asFile(final String path) {
        if (path==null || path.length()==0) {
            //not set
            return null;
        }
        File f = new File(path);
        return f;
    }

    private static final DrmJobRecord fromJobRunnerJob(final JobRunnerJob jobRunnerJob) {
        if (jobRunnerJob==null) {
            //null means there is no record in the db
            return null;
        }
        final File workingDir=asFile(jobRunnerJob.getWorkingDir());
        DrmJobRecord.Builder builder = new DrmJobRecord.Builder(jobRunnerJob.getGpJobNo());
        builder = builder.extJobId(jobRunnerJob.getExtJobId());
        builder = builder.workingDir(workingDir);
        builder = builder.stdinFile(asFile(workingDir, jobRunnerJob.getStdinFile()));
        builder = builder.stdoutFile(asFile(workingDir, jobRunnerJob.getStdoutFile()));
        builder = builder.stderrFile(asFile(workingDir, jobRunnerJob.getStderrFile()));
        builder = builder.logFile(asFile(workingDir, jobRunnerJob.getLogFile()));
        return builder.build();
    }
    
    private final String jobRunnerClassname;
    private final String jobRunnerName;
    
    public DbLookup(final String jobRunnerClassname, final String jobRunnerName) {
        this.jobRunnerClassname=jobRunnerClassname;
        this.jobRunnerName=jobRunnerName;
    }

    @Override
    public List<DrmJobRecord> getRunningDrmJobRecords() {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        final String[] runningJobStates = {
                DrmJobState.QUEUED.name(),
                DrmJobState.QUEUED_HELD.name(),
                DrmJobState.RUNNING.name(),
                DrmJobState.SUSPENDED.name(),
                DrmJobState.REQUEUED.name(),
                DrmJobState.REQUEUED_HELD.name()
        };
        try {
            HibernateUtil.beginTransaction();
            final String hql="from "+JobRunnerJob.class.getName()
                    +" where jobRunnerClassname = :jobRunnerClassname and jobRunnerName = :jobRunnerName "
                    +" and jobState in (:runningJobStates)"
                    +" order by gpJobNo";

            Query query=HibernateUtil.getSession().createQuery(hql);
            query.setString("jobRunnerClassname", jobRunnerClassname);
            query.setString("jobRunnerName", jobRunnerName);
            query.setParameterList("runningJobStates", runningJobStates);
            List<JobRunnerJob> dbRows=query.list();
            if (dbRows==null || dbRows.size()==0) {
                return Collections.emptyList();
            }
            List<DrmJobRecord> records=new ArrayList<DrmJobRecord>();
            for(final JobRunnerJob dbRow : dbRows) {
                records.add( fromJobRunnerJob(dbRow) );
            }
            
            return records;
        }
        catch (Throwable t) {
            log.error(t);
            return Collections.emptyList();
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

    @Override
    public DrmJobRecord lookupJobRecord(final Integer gpJobNo) {
        final JobRunnerJob jobRunnerJob=selectJobRunnerJob(gpJobNo);
        return fromJobRunnerJob(jobRunnerJob);
    }

    @Override
    public void insertJobRecord(final DrmJobSubmission jobSubmission) {
        final JobRunnerJob job = new JobRunnerJob.Builder(jobRunnerClassname, jobSubmission).jobRunnerName(jobRunnerName).build();
        insertJobRunnerJob(job);
    }
    
    @Override
    public void updateJobStatus(final DrmJobRecord drmJobRecord, final DrmJobStatus drmJobStatus) {
        final Integer gpJobNo=drmJobRecord.getGpJobNo();
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            JobRunnerJob existing = (JobRunnerJob) HibernateUtil.getSession().get(JobRunnerJob.class, gpJobNo);
            if (existing==null) {
                //TODO: should throw an exception?
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

    public static void insertJobRunnerJob(final JobRunnerJob jobRecord) {
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
    
    public static JobRunnerJob selectJobRunnerJob(final Integer gpJobNo) {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            JobRunnerJob existing = (JobRunnerJob) HibernateUtil.getSession().get(JobRunnerJob.class, gpJobNo);
            return existing;
        }
        catch (Throwable t) {
            log.error("Error getting entry for gpJobNo="+gpJobNo,t);
            return null;
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
}
