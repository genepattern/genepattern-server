package org.genepattern.server.executor.sge;

import java.io.File;

import org.apache.log4j.Logger;
import org.broadinstitute.zamboni.server.batchsystem.BatchJob;
import org.broadinstitute.zamboni.server.batchsystem.sge.SgeBatchSystem;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfigurationException;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.webservice.JobInfo;

/**
 * Helper methods for creating Zamboni BatchJob instances from GP JobInfo instances; 
 * and for recording (jobInfo <-> batchJob) entries in the GP database.
 * 
 * @author pcarr
 */
public class BatchJobUtil {
    public static Logger log = Logger.getLogger(BatchJobUtil.class);
    
    private BatchJobUtil() {
    }

    /**
     * Create a new BatchJob instance, setting values from the ones in the given GP jobInfo.
     * Note: set the batchJob.jobName using the following rule
     *     batchJob.jobName = "GP_" + gpJobInfo.jobNumber
     * @param gpJobInfo
     * @return
     */
    static public BatchJob createBatchJob(SgeBatchSystem sgeBatchSystem, JobInfo gpJobInfo) throws Exception {
        BatchJob sgeBatchJob = getNewBatchJob(sgeBatchSystem);
        //set workingDirectory from jobInfo
        File runDir = getWorkingDir(gpJobInfo);        
        sgeBatchJob.setWorkingDirectory( new scala.Some<String>(runDir.getPath()) );
        sgeBatchJob.setJobName( new scala.Some<String>( "GP_"+gpJobInfo.getJobNumber() ) );
        return sgeBatchJob;
    }

    /**
     * Create a BatchJob instance, from the given GP jobInfo, and from records in the GP DB.
     * @param gpJobInfo
     * @return
     */
    static public BatchJob findBatchJob(SgeBatchSystem sgeBatchSystem, JobInfo gpJobInfo) throws Exception {
        BatchJob sgeJob = createBatchJob(sgeBatchSystem, gpJobInfo);
        initSgeBatchJobFromDb(sgeJob, gpJobInfo);
        return sgeJob;
    }

    /**
     * Set any values on the given sgeBatchJob based on its matching record in the GP DB.
     * @param sgeBatchJob
     * @param gpJobInfo
     * @throws Exception
     */
    static private void initSgeBatchJobFromDb(BatchJob sgeBatchJob, JobInfo gpJobInfo) throws Exception {
        boolean alreadyInTransaction = HibernateUtil.isInTransaction();
        JobSge jobSge = null;
        try {
            jobSge = new JobSgeDAO().getJobRecord(gpJobInfo);
            if (jobSge != null) {
                sgeBatchJob.setJobId( scala.Option.apply(jobSge.getSgeJobId()) );
                sgeBatchJob.submitTime_$eq(scala.Option.apply(jobSge.getSgeSubmitTime()));
                sgeBatchJob.startTime_$eq(scala.Option.apply(jobSge.getSgeStartTime()));
                sgeBatchJob.endTime_$eq( scala.Option.apply( jobSge.getSgeEndTime() ));
            }
        }
        catch (Throwable t) {
            log.error("Error getting sgeJobId from DB, for gpJobNo="+gpJobInfo.getJobNumber(), t);
            throw new Exception(t);
        }
        finally {
            if (!alreadyInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
        if (jobSge == null) {
            throw new Exception("Error getting sgeJobId from DB, for gpJobNo="+gpJobInfo.getJobNumber()+": No record found in DB");
        }
    }

    /**
     * Record the SGE job in the GP Database.
     * @param gpJobInfo
     * @param sgeJob
     * @throws Exception
     */
    static public void createJobRecord(JobInfo gpJobInfo, BatchJob sgeJob) throws Exception {
        new JobRecorder().createSgeJobRecord(gpJobInfo, sgeJob);
    }
    
    /**
     * Update SGE job in the GP database, after it has completed.
     * @param gpJobId
     * @param sgeJob
     */
    static public void updateJobRecord(int gpJobId, BatchJob sgeJob) {
        new JobRecorder().updateSgeJobRecord(gpJobId, sgeJob);
    }

    /**
     * Get the GenePattern jobId for the given SGE BatchJob, based on naming convention.
     * @param sgeJob
     * @return the GP job id, or -1 if none found.
     */
    static public int getGpJobId(BatchJob sgeJob) {
        String jobName = "";
        scala.Option<String> opt = sgeJob.getJobName();
        if (opt.isDefined()) {
            jobName = opt.get();
        }
        int gpJobId = -1;
        if (jobName.startsWith("GP_")) {
            String gpJobIdStr = jobName.substring( "GP_".length() );
            try {
                gpJobId = Integer.parseInt(gpJobIdStr);
            }
            catch (NumberFormatException e) {
                log.error("Invalid gpJobId, expecting an integer. gpJobId="+gpJobIdStr);
            }
        }
        return gpJobId;
    }

    /**
     * Get the working directory for the given job.
     * 
     * @param jobInfo
     * @return
     * @throws Exception
     */
    static private File getWorkingDir(JobInfo jobInfo) throws Exception {
        boolean gp_3_3_3 = false;
        if (gp_3_3_3) {
            //TODO: if you are working with GP 3.3.3 or later, use JobManager#getWorkingDirectory
            //return JobManager.getWorkingDirectory(jobInfo);
        }
        
        //3.3.2 based 
        if (jobInfo == null) {
            throw new IllegalArgumentException("Can't get working directory for jobInfo=null");
        }
        if (jobInfo.getJobNumber() < 0) {
            throw new IllegalArgumentException("Can't get working directory for jobInfo.jobNumber="+jobInfo.getJobNumber());
        }

        File jobDir = null;
        try {
            GpContext jobContext = GpContext.getContextForJob(jobInfo);
            File rootJobDir = ServerConfigurationFactory.instance().getRootJobDir(jobContext);
            jobDir = new File(rootJobDir, ""+jobInfo.getJobNumber());
        }
        catch (ServerConfigurationException e) {
            throw new Exception(e.getLocalizedMessage());
        }
        return jobDir;
    }

    
    static private BatchJob getNewBatchJob(SgeBatchSystem sgeBatchSystem) {
        
        //init all variables to None
        scala.Option<String> workingDirectory = scala.Option.apply(null);
        scala.Option<String> command = scala.Option.apply(null);
        String[] args = new String[0];
        scala.Option<String> outputPath = scala.Option.apply(null);
        scala.Option<String>  errorPath = scala.Option.apply(null);
        scala.Option<String[]> emailAddresses = scala.Option.apply(null);
        scala.Option<Integer> priority = scala.Option.apply(null);
        scala.Option<String> jobName = scala.Option.apply(null);
        scala.Option<String> queueName = scala.Option.apply(null);
        scala.Option<Boolean> exclusive = scala.Option.apply(null);
        scala.Option<Integer> maxRunningTime = scala.Option.apply(null);
        scala.Option<Integer> memoryReservation = scala.Option.apply(null);
        scala.Option<Integer> maxMemory = scala.Option.apply(null);
        scala.Option<Integer> slotReservation = scala.Option.apply(null);
        scala.Option<Integer> maxSlots = scala.Option.apply(null);
        scala.Option<Boolean> restartable = scala.Option.apply(null);

        //set restartable to false (None causes an error in SgeBatchSystem.scala)
        restartable = scala.Option.apply(false);

        BatchJob sgeJob = sgeBatchSystem.newBatchJob(
                workingDirectory,
                command,
                args,
                outputPath,
                errorPath,
                emailAddresses,
                priority,
                jobName,
                queueName,
                exclusive,
                maxRunningTime,
                memoryReservation,
                maxMemory,
                slotReservation,
                maxSlots,
                restartable );
       
        return sgeJob;
    }

}
