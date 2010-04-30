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

package org.genepattern.server;

import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.AnalysisJob;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.OmnigeneException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;

/**
 * Runnable AnalysisTask - Adapts a Runnable to run within a pre-created thread. This object class is used by
 * AnalysisManager.
 * 
 */
public class AnalysisTask implements Runnable {
    private static Logger log = Logger.getLogger(AnalysisTask.class);

    /**
     * maximum number of concurrent jobs to run before next one will have to wait
     */
    private static int NUM_THREADS = 20;
    private final static String TASK_NAME = GenePatternAnalysisTask.TASK_NAME;
    private static AnalysisTask instance;
    private static Thread runner;

    static {
        try {
            NUM_THREADS = Integer.parseInt(System.getProperty(GPConstants.NUM_THREADS, "20"));
            instance = new AnalysisTask(NUM_THREADS);
        } 
        catch (Exception e) {
            log.error(e);
        }
    }

    private Object jobQueueWaitObject = new Object();
    private Vector<JobInfo> jobQueue = new Vector<JobInfo>();
    // Semaphore to maintain simultaneous job count
    private Semaphore sem;
    private GenePatternAnalysisTask genePattern = new GenePatternAnalysisTask();

    public AnalysisTask(int threadCount) {
        // create semaphore when thread count > 0
        if (threadCount > 0) {
            sem = new Semaphore(threadCount);
        }
    }

    /**
     * Add an object to queue
     * 
     * @param o, The feature to be added to the JobToQueue attribute
     */
    public void addJobToQueue(Object o) {
        jobQueue.add((JobInfo) o);
    }

    /** Clears the AnalysisTask's queue. */
    public void clear() {
        jobQueue.clear();
    }

    public String getTaskName() {
        return TASK_NAME;
    }

    /**
     * This is placeholder for doing process, which should be done in begining and end of each job
     * 
     * @param jobInfo, The JobInfo object.
     */
    public void onJobProcessFrameWork(Object jobInfo) {
        // if max job running, then wait until some thread to finish
        doAcquire();
        new JobThread(jobInfo).start();
    }

    /** Main AnalysisTask's thread method. */
    public void run() {
        log.debug("Starting AnalysisTask thread");
        while (true) {
            // Load input data to input queue
            synchronized (jobQueueWaitObject) {
                if (jobQueue.isEmpty()) {
                    // Fetch another batch of jobs.
                    jobQueue = this.getWaitingJobs(NUM_THREADS);
                    if (jobQueue != null && !jobQueue.isEmpty()) {
                        jobQueue = this.updateJobStatusToProcessing(jobQueue);
                    }
                }

                if (jobQueue.isEmpty()) {
                    try {
                        // jobQueueWaitObject.wait(waitTime);
                        jobQueueWaitObject.wait();
                    } 
                    catch (InterruptedException ie) {
                    }
                }
            }

            Object o = null;
            if (!jobQueue.isEmpty()) {
                o = jobQueue.remove(0);
            }
            if (o == null) {
                continue;
            }
            try {
                onJobProcessFrameWork(o);
            } 
            catch (Exception ex) {
                log.error(ex);
            }
        }
    }

    /**
     * Wake up the job queue thread. The object is synchronized to obtain ownership of the monitor.
     */
    public void wakeupJobQueue() {
        synchronized (jobQueueWaitObject) {
            jobQueueWaitObject.notify();
        }
    }

    private void doAcquire() {
        if (sem != null) {
            sem.acquire();
        }
    }

    private void doRelease() {
        if (sem != null) {
            sem.release();
        }
    }

    private Vector<JobInfo> getWaitingJobs(int maxJobCount) throws OmnigeneException {
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
        for(JobInfo jobInfo : jobs) {
            try {
                // Commit each time through since any large CLOBs (>4k) will fail if they are committed inside a
                // transaction with ANY other object
                HibernateUtil.beginTransaction();
                setJobStatus(jobInfo.getJobNumber(), JobStatus.JOB_PROCESSING);
                updatedJobs.add(jobInfo);
                HibernateUtil.commitTransaction();
            } 
            catch (Throwable t) {
                // don't add it to updated jobs, record the failure and move on
                log.error("Error updating job status to processing in AnalysisTask", t);
                HibernateUtil.rollbackTransaction();
            } 
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

    private class JobThread extends Thread {
        private Object obj;

        public JobThread(Object o) {
            this.obj = o;
        }

        @Override
        public void run() {
            if (genePattern == null) {
                log.error("job not run, genePattern == null!");
            }
            else {
                genePattern.onJob(obj);                
            }
            doRelease();// signal completion of thread
        }
    }

    public static AnalysisTask getInstance() {
        return instance;
    }

    public static synchronized void startQueue() {
        if (runner == null) {
            runner = new Thread(instance);
            runner.setName("AnalysisTaskThread");
            runner.setDaemon(true);
            runner.start();
        }
        else {
            log.error("can only call startQueue once!");
        }
    }
    
    public static synchronized void stopQueue() {
        if (runner != null) {
            runner.stop();
            runner = null;
        }
    }

}
