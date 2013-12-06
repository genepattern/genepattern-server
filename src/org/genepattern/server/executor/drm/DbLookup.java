package org.genepattern.server.executor.drm;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.executor.drm.dao.JobRunnerJob;
import org.hibernate.Query;

public class DbLookup implements DrmLookup {
    private static final Logger log = Logger.getLogger(DbLookup.class);

    private final String jobRunnerClassname;
    private final String jobRunnerName;
    
    public DbLookup(final String jobRunnerClassname, final String jobRunnerName) {
        this.jobRunnerClassname=jobRunnerClassname;
        this.jobRunnerName=jobRunnerName;
    }
    
    @Override
    public List<String> getRunningDrmJobIds() {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            final String hql="select extJobId from "+JobRunnerJob.class.getName()+" where jobRunnerClassname = :jobRunnerClassname and jobRunnerName = :jobRunnerName";
            Query query=HibernateUtil.getSession().createQuery(hql);
            query.setString("jobRunnerClassname", jobRunnerClassname);
            query.setString("jobRunnerName", jobRunnerName);
            List<String> rval=query.list();
            return rval;
        }
        catch (Throwable t) {
            log.error("Error getting job ids for jobRunnerClassname="+jobRunnerClassname+", jobRunnerName="+jobRunnerName, t);
            return Collections.emptyList();
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

    @Override
    public String lookupDrmJobId(final Integer gpJobNo) {
        JobRunnerJob jobRunnerJob=selectJobRunnerJob(gpJobNo);
        if (jobRunnerJob != null) {
            final String extJobId=jobRunnerJob.getExtJobId();
            if (extJobId != null) {
                return extJobId;
            }
            else {
                //empty string means, there is a record, but the extJobId was not set
                return "";
            }
        }
        //null means there is not record in the db
        return null;
    }

    @Override
    public Integer lookupGpJobNo(final String drmJobId) {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            final String hql="select jrj.gpJobNo from "+JobRunnerJob.class.getName()+" jrj where jrj.jobRunnerClassname = :jobRunnerClassname and jrj.jobRunnerName = :jobRunnerName and jrj.extJobId = :extJobId";
            Query query=HibernateUtil.getSession().createQuery(hql);
            query.setString("jobRunnerClassname", jobRunnerClassname);
            query.setString("jobRunnerName", jobRunnerName);
            query.setString("extJobId", drmJobId);
            List<Integer> rval=query.list();
            if (rval != null && rval.size()>0) {
                if (rval.size()>1) {
                    log.error("Unexpected result from query, should not have more than 1 entry into table, but found "+rval.size());
                }
                return rval.get(0);
            }
            //null means no entry found in db
            return null;
        }
        catch (Throwable t) {
            log.error(t);
            return null;
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

    @Override
    public void insertJobRecord(DrmJobSubmission jobSubmission) {
        insertDrmRecord(jobSubmission);
    }

    @Override
    public void updateJobStatus(Integer gpJobNo, DrmJobStatus drmJobStatus) {
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
    
    /**
     * For debugging, get all records
     * @return
     */
    protected List<JobRunnerJob> getAll() {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            final String hql="from "+JobRunnerJob.class.getName();
            Query query=HibernateUtil.getSession().createQuery(hql);
            List<JobRunnerJob> rval=query.list();
            return rval;
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

    protected JobRunnerJob selectJobRunnerJob(final Integer gpJobNo) {
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
    
    //protected void insertDrmRecord(final File workingDir, final Integer gpJobNo) {
    protected void insertDrmRecord(final DrmJobSubmission jobSubmission) {
        //final JobRunnerJob job = new JobRunnerJob(jobRunnerClassname, jobRunnerName, workingDir, gpJobNo);
        final JobRunnerJob job = new JobRunnerJob.Builder(jobRunnerClassname, jobSubmission).jobRunnerName(jobRunnerName).build();
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            HibernateUtil.getSession().saveOrUpdate(job);
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        catch (Throwable t) {
            log.error("Error adding record for gpJobNo="+jobSubmission.getGpJobNo(), t);
            HibernateUtil.rollbackTransaction();
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

}
