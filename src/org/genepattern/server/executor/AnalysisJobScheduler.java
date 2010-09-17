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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
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
    
    private final int BOUND = 10000;
    private final BlockingQueue<Integer> pendingJobQueue = new LinkedBlockingQueue<Integer>(BOUND);

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
                List<Integer> waitingJobs = null;
                synchronized (jobQueueWaitObject) {
                    if (pendingJobQueue.isEmpty()) {
                        waitingJobs = AnalysisJobScheduler.getWaitingJobs(batchSize);
                        if (waitingJobs != null && !waitingJobs.isEmpty()) {
                            waitingJobs = updateJobStatus(waitingJobs, JobStatus.JOB_DISPATCHING);
                            if (waitingJobs != null) {
                                for(Integer jobId : waitingJobs) { 
                                    if (pendingJobQueue.contains(jobId)) {
                                        log.error("duplicate entry in pending jobs queue: "+jobId);
                                    }
                                    else {
                                        pendingJobQueue.put(jobId);
                                    }
                                }
                            }
                        }
                        else {
                            //insurance against deadlock, poll for new PENDING jobs every 60 seconds, regardless of whether notify has been called
                            final long timeout = 60*1000;
                            jobQueueWaitObject.wait(timeout);
                        }
                    }
                }
            }
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    static private List<Integer> getWaitingJobs(int maxJobCount) {
        try {
            HibernateUtil.beginTransaction();
            String hql = "select jobNo from org.genepattern.server.domain.AnalysisJob where jobStatus.statusId = :statusId and deleted = false order by submittedDate ";
            Query query = HibernateUtil.getSession().createQuery(hql);
            if (maxJobCount > 0) {
                query.setMaxResults(maxJobCount);
            }
            query.setInteger("statusId", JobStatus.JOB_PENDING);
            List<Integer> jobIds = query.list();
            return jobIds;
        }
        catch (Throwable t) {
            log.error("Error getting list of pending jobs from queue", t);
            return new ArrayList<Integer>();
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }

    static private List<Integer> updateJobStatus(List<Integer> jobIds, int jobStatusId) {
        List<Integer> updatedJobIds = new ArrayList<Integer>();
        HibernateUtil.beginTransaction();
        try {
            for(Integer jobId : jobIds) {
                AnalysisJobScheduler.setJobStatus(jobId, jobStatusId);
                updatedJobIds.add(jobId);
            }
            HibernateUtil.commitTransaction();
        }
        catch (Throwable t) {
            // don't add it to updated jobs, record the failure and move on
            updatedJobIds.clear();
            log.error("Error updating job status to processing", t);
            HibernateUtil.rollbackTransaction();
        } 
        return updatedJobIds;
    }

    /**
     * @param jobNo
     * @param jobStatus
     * @return number of rows successfully updated
     * @throws Exception if the job status was not successfully updated
     */
    static public int setJobStatus(int jobNo, int jobStatus) throws Exception {
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
        private final BlockingQueue<Integer> pendingJobQueue;
        private final GenePatternAnalysisTask genePattern = new GenePatternAnalysisTask();
        
        public ProcessingJobsHandler(BlockingQueue<Integer> pendingJobQueue) {
            this.pendingJobQueue = pendingJobQueue;
        }
        
        public void run() {
            try {
                while (true) {
                    Integer jobId = pendingJobQueue.take();
                    submitJob(jobId);
                }
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        private void submitJob(Integer jobId) {
            if (genePattern == null) {
                log.error("job not run, genePattern == null!");
                return;
            }
            try {
                genePattern.onJob(jobId);
            }
            catch (JobDispatchException e) {
                //TODO: implement genePattern.handleError(jobId);
                try {
                    HibernateUtil.beginTransaction();
                    setJobStatus(jobId, JobStatus.JOB_ERROR);
                    HibernateUtil.commitTransaction();
                }
                catch (Throwable t) {
                    log.error("", t);
                    HibernateUtil.rollbackTransaction();
                }
            }
        }
    }

}
