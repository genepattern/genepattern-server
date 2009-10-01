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

import java.util.Iterator;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.AnalysisJob;
import org.genepattern.server.domain.AnalysisJobDAO;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.OmnigeneException;
import org.hibernate.HibernateException;
import org.hibernate.Query;

/**
 * Runnable AnalysisTask - Adapts a Runnable to run within a pre-created thread.
 * This object class is used by AnalysisManager.
 */

public class AnalysisTask implements Runnable {
    /**
     * maximum number of concurrent jobs to run before next one will have to
     * wait
     */
    private static int NUM_THREADS = 20;

    private static Logger log = Logger.getLogger(AnalysisTask.class);

    private final static String TASK_NAME = GenePatternAnalysisTask.TASK_NAME;

    private static AnalysisTask instance;

    static {
        try {
            NUM_THREADS = Integer.parseInt(System.getProperty(GPConstants.NUM_THREADS, "20"));
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
    private AnalysisJobDAO dao = new AnalysisJobDAO();

    public AnalysisTask(int threadCount) {
        // create semaphore when thread count > 0
        if (threadCount > 0) {
            sem = new Semaphore(threadCount);
        }
    }

//    /**
//     * Add an object to queue
//     * 
//     * @param o
//     *            The feature to be added to the JobToQueue attribute
//     */
//    public void addJobToQueue(Object o) {
//        jobQueue.add((JobInfo) o);
//    }

    /** Clears the AnalysisTask's queue. */
    public void clear() {
        jobQueue.clear();
    }

    public String getTaskName() {
        return TASK_NAME;
    }

    /**
     * This is a place holder for doing process, which should be done in beginning
     * and end of each job
     * 
     * @param o
     *            The JobInfo object.
     */
    public void onJobProcessFrameWork(JobInfo jobInfo) {
        // if max job running, then wait until some thread to finish
        doAcquire();
        JobThread jobThread = new JobThread(jobInfo);
        jobThread.start();
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
                    jobQueue = this.updateJobStatusToProcessing(jobQueue);
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

            JobInfo o = null;
            if (!jobQueue.isEmpty()) {
                o = jobQueue.remove(0);
            }

            if (o == null) {
                continue;
            }
            
            try {
                JobInfo jobInfo = (JobInfo) o;
                onJobProcessFrameWork(jobInfo);
            } 
            catch (Exception ex) {
                log.error(ex);
            }
        }
    }

    /**
     * Wake up the job queue thread. The object is synchronized to obtain
     * ownership of the monitor.
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

        // Validating taskID is not done here bcos.
        // assuming once job is submitted, it should be executed even if
        // taskid is removed from task master
        HibernateUtil.beginTransaction();

        String hql = "from org.genepattern.server.domain.AnalysisJob "
                + " where jobStatus.statusId = :statusId order by submittedDate ";
        Query query = HibernateUtil.getSession().createQuery(hql);

        if (maxJobCount > 0) {
            query.setMaxResults(maxJobCount);
        }

        query.setInteger("statusId", JobStatus.JOB_PENDING);

        for (Iterator<AnalysisJob> iter = query.list().iterator(); iter.hasNext();) {
            AnalysisJob aJob = iter.next();
            JobInfo singleJobInfo = new JobInfo(aJob);
            // Add waiting job info to vector, for AnalysisTask
            jobVector.add(singleJobInfo);
        }

        return jobVector;
    }

    private Vector<JobInfo> updateJobStatusToProcessing(Vector<JobInfo> jobs) {
        Vector<JobInfo> updatedJobs = new Vector<JobInfo>();

        JobStatus newStatus = (JobStatus) HibernateUtil.getSession().get(JobStatus.class, JobStatus.JOB_PROCESSING);
        Iterator<JobInfo> iter = jobs.iterator();

        while (iter.hasNext()) {
            HibernateUtil.beginTransaction();
            try {
                JobInfo jobInfo = iter.next();
                AnalysisJob aJob = dao.findById(jobInfo.getJobNumber());
                aJob.setStatus(newStatus);
                /**
                 * Commit each time through since any large CLOBs (>4k) will
                 * fail if they are committed inside a transaction with ANY
                 * other object
                 */
                HibernateUtil.commitTransaction();
                HibernateUtil.beginTransaction();
                updatedJobs.add(jobInfo);
            } 
            catch (HibernateException he) {
                HibernateUtil.rollbackTransaction();
                // don't add it to updated jobs, record the failure and move on
                log.error("Error updating job status to processing in AnalysisTask", he);
            }
        }
        return updatedJobs;
    }

    private class JobThread extends Thread {
        private JobInfo jobInfo;

        public JobThread(JobInfo o) {
            this.jobInfo = o;
        }

        @Override
        public void run() {
            genePattern.runJob(jobInfo);// run job
            doRelease();// signal completion of thread
        }
    }

    public static AnalysisTask getInstance() {
        return instance;
    }

    public static void startQueue() {
        if (instance == null) {
            instance = new AnalysisTask(NUM_THREADS);
            Thread runner = new Thread(instance);
            runner.start();
        }
    }

}
