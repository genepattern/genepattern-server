package org.genepattern.server.executor.sge;

import java.util.Date;

import org.apache.log4j.Logger;
import org.broadinstitute.zamboni.server.batchsystem.BatchJob;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.webservice.JobInfo;

/**
 * Map GP Jobs to SGE Jobs in the GP DB.
 * 
 * Note: If you ever want to de-couple this plugin from Hibernate, create an interface of the public methods of this class.
 * And implement as you wish.
 * 
 * @author pcarr
 */
public class JobRecorder {
    public static Logger log = Logger.getLogger(SgeCommandExecutor.class);

    /**
     * Record the SGE job into the GP database, so that we can lookup sge job id for a given gp job id.
     * 
     * @param gpJob
     * @param sgeJob
     * @throws Exception
     */
    public void createSgeJobRecord(JobInfo gpJob, BatchJob sgeJob) throws Exception {
        log.debug("recording sge job into database, gpJobNo="+gpJob.getJobNumber()+", sgeJobId="+sgeJob.getJobId());
        
        JobSge jobRecord = new JobSge();
        jobRecord.setGpJobNo(gpJob.getJobNumber());
        populateJobRecordFromSgeJob(jobRecord, sgeJob);
        saveOrUpdate(jobRecord);
    }

    /**
     * Update the record, after the job has completed.
     * @param gpJobNo
     * @param sgeJob
     */
    public void updateSgeJobRecord(int gpJobNo, BatchJob sgeJob) {
        JobSge jobRecord = new JobSge();
        jobRecord.setGpJobNo(gpJobNo);
        populateJobRecordFromSgeJob( jobRecord, sgeJob );
        saveOrUpdate(jobRecord);
    }
    
    /**
     * Remove the record from the DB. This is one way to free up entries
     * to prevent bugs with duplicate SGE job ids.
     * 
     * @param gpJob
     * @throws Exception
     */
    public void removeSgeJobRecord(JobInfo gpJob) throws Exception {
        JobSge jobRecord = new JobSge();
        jobRecord.setGpJobNo(gpJob.getJobNumber());
        delete(jobRecord);
    }
    
    private void populateJobRecordFromSgeJob(JobSge jobRecord, BatchJob sgeJob) {
        if (sgeJob.getJobId().isDefined()) {
            String jobId = (String) sgeJob.getJobId().get();
            jobRecord.setSgeJobId(jobId);
        } 
        if (sgeJob.submitTime().isDefined()) {
            jobRecord.setSgeSubmitTime( (Date) sgeJob.submitTime().get() );
        }
        if (sgeJob.startTime().isDefined()) {
            jobRecord.setSgeStartTime( (Date) sgeJob.startTime().get() );
        }
        if (sgeJob.endTime().isDefined()) {
            jobRecord.setSgeEndTime( (Date) sgeJob.endTime().get() );
        }
        if (sgeJob.getReturnCode().isDefined()) {
            jobRecord.setSgeReturnCode( ((Integer) sgeJob.getReturnCode().get()).intValue() );
        }
        if (sgeJob.getCompletionStatus().isDefined()) {
            jobRecord.setSgeJobCompletionStatus( ""+sgeJob.getCompletionStatus().get() );
        }
    }

    private void saveOrUpdate(JobSge jobRecord) { 
        log.debug("saveOrUpdate JOB_SGE, gpJobNo="+jobRecord.getGpJobNo());
        boolean alreadyInTransaction = HibernateUtil.isInTransaction();
        log.debug("alreadyInTransaction: "+alreadyInTransaction);
        try {
            JobSgeDAO dao = new JobSgeDAO();
            dao.saveOrUpdate(jobRecord);
            if (!alreadyInTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        catch (Throwable t) {
            log.error("Error recording job in DB, gpJobId="+jobRecord.getGpJobNo(), t);
            HibernateUtil.rollbackTransaction();
        }
        finally {
            if (!alreadyInTransaction) {
                log.debug("alreadyInTransaction: "+alreadyInTransaction+". closeCurrentSession");
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    private void delete(JobSge jobRecord) {
        log.debug("delete JOB_SGE, gpJobNo="+jobRecord.getGpJobNo());
        boolean alreadyInTransaction = HibernateUtil.isInTransaction();
        log.debug("alreadyInTransaction: "+alreadyInTransaction);
        JobSgeDAO dao = new JobSgeDAO();
        try {
            dao.delete(jobRecord);
            if (!alreadyInTransaction) {
                log.debug("alreadyInTransaction: "+alreadyInTransaction+". commitTransaction");
                HibernateUtil.commitTransaction();
            }
        }
        catch (Throwable t) {
            log.debug("Error deleting record from JOB_SGE, gpJobNo="+jobRecord.getGpJobNo(), t);
            HibernateUtil.rollbackTransaction();
        }
    }
}
