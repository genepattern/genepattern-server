package org.genepattern.server.executor.drm;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.executor.drm.dao.JobRunnerJob;
import org.genepattern.webservice.JobInfo;
import org.hibernate.Query;

public class DbLookup implements DrmLookup {
    private static final Logger log = Logger.getLogger(DbLookup.class);

    private final String jobRunnerClassname;
    private final String jobRunnerName;
    
    public DbLookup(final String jobRunnerClassname, final String jobRunnerName) {
        this.jobRunnerClassname=jobRunnerClassname;
        this.jobRunnerName=jobRunnerName;
    }
    
    //for debugging, get all records
    public List<JobRunnerJob> getAll() {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            final String hql="from org.genepattern.server.executor.drm.dao.JobRunnerJob";
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

    @Override
    public List<String> getRunningDrmJobIds() {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            final String hql="select extJobId from org.genepattern.server.executor.drm.dao.JobRunnerJob where jobRunnerClassname = :jobRunnerClassname and jobRunnerName = :jobRunnerName";
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
    public String lookupDrmJobId(JobInfo jobInfo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void insertDrmRecord(File workingDir, JobInfo jobInfo) {
        insertDrmRecord(workingDir, jobInfo.getJobNumber());
    }

    @Override
    public Integer lookupGpJobNo(String drmJobId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateDrmRecord(Integer gpJobNo, DrmJobStatus drmJobStatus) {
        // TODO Auto-generated method stub
        
    }
    
    public void insertDrmRecord(File workingDir, Integer gpJobNo) {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            final JobRunnerJob job = new JobRunnerJob(jobRunnerClassname, jobRunnerName, workingDir, gpJobNo);
            HibernateUtil.getSession().save(job);
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        catch (Throwable t) {
            log.error("Error adding record for gpJobNo="+gpJobNo, t);
            HibernateUtil.rollbackTransaction();
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }


}
