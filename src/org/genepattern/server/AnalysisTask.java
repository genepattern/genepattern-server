/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server;

import java.util.List;
import java.util.Vector;
import java.util.Iterator; 

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
 *
 * @author Rajesh Kuttan
 * @version
 */

public class AnalysisTask implements Runnable {
    private static Logger log = Logger.getLogger(AnalysisTask.class);

    private Object jobQueueWaitObject = new Object();
    private Vector jobQueue = new Vector<JobInfo>();
    
    // Semaphore to maintain simultaneous job count
    private Semaphore sem = null;
    
    volatile boolean runFlag = true;
    
    private final static String TASK_NAME = GenePatternAnalysisTask.TASK_NAME;
    
    private GenePatternAnalysisTask genePattern = new GenePatternAnalysisTask();
    
    private static AnalysisTask instance;
    
    
    /**
     * maximum number of concurrent tasks to run before next one will have to wait
     */
    public static int NUM_THREADS = 20;

    static {
	try {
	    NUM_THREADS = Integer.parseInt(System.getProperty(GPConstants.NUM_THREADS, "20"));
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
    
    
    private AnalysisTask() {
    }
    
    public String getTaskName() {
        return TASK_NAME;
    }
    
    /**
     * Add an object to queue
     *
     * @param o
     *            The feature to be added to the JobToQueue attribute
     */
    public void addJobToQueue(Object o) {
        jobQueue.add(o);
    }
    
    /** Clears the AnalysisTask's queue. */
    public void clear() {
        jobQueue.clear();
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
    
    
    private AnalysisJobDAO dao = new AnalysisJobDAO();
	
    private Vector<JobInfo>  updateJobStatusToProcessing(Vector<JobInfo> jobs){
    	int maxJobCount = 20;
    	int i=0;
    	Vector<JobInfo> updatedJobs = new Vector<JobInfo>();
    	
    	JobStatus newStatus = (JobStatus) HibernateUtil.getSession().get(JobStatus.class, JobStatus.JOB_PROCESSING);
    	Iterator iter = jobs.iterator();
    	
        while (iter.hasNext() && i++ <= maxJobCount) {
            HibernateUtil.beginTransaction();
            try {
            	JobInfo jobby = (JobInfo) iter.next();
                
            	AnalysisJob aJob = dao.findById(jobby.getJobNumber());
            	aJob.setStatus(newStatus);
            	/**
            	 * Commit each time through since any large CLOBs (>4k) will fail if
            	 * they are committed inside a transaction with ANY other object
            	 */
            	log.debug("ONE LITTLE COMMIT\n\n");
            	HibernateUtil.commitTransaction();
            	HibernateUtil.beginTransaction();
            
            	updatedJobs.add(jobby);
            }catch (HibernateException he){
            	 HibernateUtil.rollbackTransaction();
            	// don't add it to updated jobs, record the failure and move on
            	log.error("Error updating job status to processing in AnalysisTask", he);
            }
        }
        return updatedJobs;
    }
    
    
    
    private Vector getWaitingJobs(int maxJobCount) throws OmnigeneException {
        Vector jobVector = new Vector();

        // initializing maxJobCount, if it has invalid value
        if (maxJobCount <= 0) {
            maxJobCount = 1;
        }

        // Validating taskID is not done here bcos.
        // assuming once job is submitted, it should be executed even if
        // taskid is removed from task master
        HibernateUtil.beginTransaction();
        
        String hql = "from org.genepattern.server.domain.AnalysisJob "
                + " where jobStatus.statusId = :statusId order by submittedDate ";
        Query query = HibernateUtil.getSession().createQuery(hql);
        query.setInteger("statusId", JobStatus.JOB_PENDING);

        List results = query.list();

        int i = 1;
        Iterator iter = results.iterator();
        
        while (iter.hasNext() && i++ <= maxJobCount) {
            AnalysisJob aJob = (AnalysisJob) iter.next();
            JobInfo singleJobInfo = new JobInfo(aJob);
            // Add waiting job info to vector, for AnalysisTask
            jobVector.add(singleJobInfo);
        }

        return jobVector;

    }
    
    
    /** Main AnalysisTask's thread method. */
    public void run() {
        log.debug("Starting AnalysisTask thread");
        int waitTime = Integer.parseInt(System.getProperty(
                "AnalysisTaskQueuePollingFrequency", "0"));
        
        while (true) {
            
            // Load input data to input queue
            synchronized (jobQueueWaitObject) {
                
                if (jobQueue.isEmpty()) {
                    // Fetch another batch of jobs.
                	jobQueue =  this.getWaitingJobs(NUM_THREADS);
                	jobQueue = this.updateJobStatusToProcessing(jobQueue);
                }
                
                if (jobQueue.isEmpty()) {
                    try {
                        //jobQueueWaitObject.wait(waitTime);
                        jobQueueWaitObject.wait();
                    } catch (InterruptedException ie) {
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
                
            } catch (Exception ex) {
                log.error(ex);
            }
            
        }
        
    }
    
    /**
     * This is placeholder for doing process, which should be done in begining
     * and end of each job
     *
     * @param o
     *            Description of the Parameter
     */
    public void onJobProcessFrameWork(Object o) {
        doAcquire();// if max job running, then wait until some thread to finish
        new JobThread(o).start();
    }
    
    public String toString() {
        return TASK_NAME;
    }
    
    public AnalysisTask(int threadCount) {        
        // create semaphore when thread count >0
        if (threadCount > 0) {
            sem = new Semaphore(threadCount);
        }
    }
    
    private void doAcquire() {
        if (sem != null) {
            if(log.isDebugEnabled()) {
                log.debug("Acquiring semaphore");
            }
            sem.acquire();
        }
    }
    
    private void doRelease() {
        if (sem != null) {
            log.debug("Releasing semaphore");
            sem.release();
        }
    }
    
    public static AnalysisTask getInstance() {
        return instance;
    }
    
    private class JobThread extends Thread {
        
        Object obj = null;
        
        public JobThread(Object o) {
            this.obj = o;
        }
        
        public void run() {
            
            if(log.isDebugEnabled()) {
                JobInfo ji = (JobInfo) obj;
                log.debug("Starting job thread for: " + ji.getJobNumber() + " (" + ji.getTaskName() + ")");
            }
            genePattern.onJob(obj);// run job
            doRelease();// signal completion of thread
            
        }
    }
    
    public static void startQueue() {
        if (instance == null) {
        	instance = new AnalysisTask(NUM_THREADS);
            Thread runner = new Thread(instance);
            runner.start();
        }
    }
    
}
