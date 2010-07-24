/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2009) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.AnalysisJob;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.webservice.JobInfo;
import org.hibernate.Query;
import org.hibernate.SQLQuery;

/**
 * Polls the db for new PENDING jobs and submits them to GenePatternAnalysisTask for execution.
 */
public class AnalysisJobScheduler implements Runnable {
    private static Logger log = Logger.getLogger(AnalysisJobScheduler.class);

    public static ThreadGroup THREAD_GROUP = new ThreadGroup("GPAnalysisJob");
    public static ThreadFactory THREAD_FACTORY = new ThreadFactory() {
        public Thread newThread(Runnable r) {
            return new Thread(THREAD_GROUP, r);
        }
    };
    
    //TODO: replace jobQueue with pendingJobQueue
    private Vector<JobInfo> jobQueue = new Vector<JobInfo>();
    private final int BOUND = 10000;
    private final BlockingQueue<JobInfo> pendingJobQueue = new LinkedBlockingQueue<JobInfo>(BOUND);

    private Object jobQueueWaitObject = new Object();
    //the batch size, the max number of pending jobs to fetch from the db at a time
    private int batchSize = 20;
    private int numJobSubmissionThreads = 3;

    private Thread runner = null;
    private List<Thread> jobSubmissionThreads = null;

    public AnalysisJobScheduler() {
    }

    public void startQueue() {
        runner = new Thread(THREAD_GROUP, this);
        runner.setName("AnalysisTaskThread");
        runner.setDaemon(true);

        jobSubmissionThreads = new ArrayList<Thread>();
        for (int i=0; i<numJobSubmissionThreads; ++i) { 
            Thread jobSubmissionThread = new Thread(THREAD_GROUP, new ProcessingJobsHandler(pendingJobQueue));
            jobSubmissionThread.setName("AnalysisTaskJobSubmissionThread-"+i);
            jobSubmissionThread.setDaemon(true);
            jobSubmissionThreads.add(jobSubmissionThread);
            jobSubmissionThread.start();
        }
        runner.start();
    }
    
    public void stopQueue() {
        if (runner != null) {
            runner.interrupt();
            runner = null;
        }
        for(Thread jobSubmissionThread : jobSubmissionThreads) {
            if (jobSubmissionThread != null) {
                //TODO: we could set the status back to PENDING for any jobs left on the queue
                jobSubmissionThread.interrupt();
                jobSubmissionThread = null;
            }
        }
        jobSubmissionThreads.clear();
    }

    /** Main AnalysisTask's thread method. */
    public void run() {
        log.debug("Starting AnalysisTask thread");

        try {
            while (true) {
                // Load input data to input queue
                synchronized (jobQueueWaitObject) {
                    if (jobQueue.isEmpty()) {
                        // Fetch another batch of jobs.
                        jobQueue = this.getWaitingJobs(batchSize);
                        if (jobQueue != null && !jobQueue.isEmpty()) {
                            jobQueue = this.updateJobStatusToProcessing(jobQueue);
                        }
                    }

                    if (jobQueue.isEmpty()) {
                        jobQueueWaitObject.wait();
                    }
                }

                JobInfo o = null;
                if (!jobQueue.isEmpty()) {
                    o = jobQueue.remove(0);
                }
                if (o == null) {
                    continue;
                }
                pendingJobQueue.put(o);
            }
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
        
    private Vector<JobInfo> getWaitingJobs(int maxJobCount) {
        Vector<JobInfo> jobVector = new Vector<JobInfo>();
        try {
            HibernateUtil.beginTransaction();
            String hql = "from org.genepattern.server.domain.AnalysisJob where jobStatus.statusId = :statusId and deleted = false order by submittedDate ";
            Query query = HibernateUtil.getSession().createQuery(hql);
            if (maxJobCount > 0) {
                query.setMaxResults(maxJobCount);
            }
            query.setInteger("statusId", JobStatus.JOB_PENDING);
            List<AnalysisJob> jobList = query.list();
            for(AnalysisJob aJob : jobList) {
                JobInfo singleJobInfo = new JobInfo(aJob);
                jobVector.add(singleJobInfo);
            }
        }
        catch (Throwable t) {
            log.error("Error getting list of pending jobs from queue", t);
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
        return jobVector;
    }
        
    private Vector<JobInfo> updateJobStatusToProcessing(Vector<JobInfo> jobs) {
        Vector<JobInfo> updatedJobs = new Vector<JobInfo>();
        // Commit each time through since any large CLOBs (>4k) will fail if they are committed inside a
        // transaction with ANY other object
        // No longer doing this ... because we don't involve the CLOB in the update statement
        HibernateUtil.beginTransaction();
        try {
            for(JobInfo jobInfo : jobs) {
                setJobStatus(jobInfo.getJobNumber(), JobStatus.JOB_PROCESSING);
                updatedJobs.add(jobInfo);
            }
            HibernateUtil.commitTransaction();
        }
        catch (Throwable t) {
            // don't add it to updated jobs, record the failure and move on
            updatedJobs.clear();
            log.error("Error updating job status to processing", t);
            HibernateUtil.rollbackTransaction();
        } 
        return updatedJobs;
    }
        
    /**
     * @param jobNo
     * @param jobStatus
     * @return number of rows successfully updated
     * @throws Exception if the job status was not successfully updated
     */
    private int setJobStatus(int jobNo, int jobStatus) throws Exception {
        //SQL update statement:
        //    update analysis_job set status_id=statusId where job_no=jobNo
        String sqlUpdate = "update ANALYSIS_JOB set status_id=:jobStatus where job_no=:jobNo";
        SQLQuery sqlQuery = HibernateUtil.getSession().createSQLQuery(sqlUpdate);
        sqlQuery.setInteger("jobStatus", jobStatus);
        sqlQuery.setInteger("jobNo", jobNo);

        int rval = sqlQuery.executeUpdate();
        if (rval != 1) {
            throw new Exception("Did not update job status for jobNo="+jobNo);
        }
        return rval;
    }

    /**
     * Wake up the job queue thread. The object is synchronized to obtain ownership of the monitor.
     */
    public void wakeupJobQueue() {
        synchronized (jobQueueWaitObject) {
            jobQueueWaitObject.notify();
        }
    }

    private static class ProcessingJobsHandler implements Runnable {
        private final BlockingQueue<JobInfo> pendingJobQueue;
        private final GenePatternAnalysisTask genePattern = new GenePatternAnalysisTask();
        
        public ProcessingJobsHandler(BlockingQueue<JobInfo> pendingJobQueue) {
            this.pendingJobQueue = pendingJobQueue;
        }
        
        public void run() {
            try {
                while (true) {
                    submitJob(pendingJobQueue.take());
                }
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        private void submitJob(JobInfo jobInfo) {
            if (genePattern == null) {
                log.error("job not run, genePattern == null!");
            }
            else {
                genePattern.onJob(jobInfo);
            }
        }
    }

}
