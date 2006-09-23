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

import java.rmi.RemoteException;
import java.util.Vector;

import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.webservice.OmnigeneException;
import org.hibernate.HibernateException;

/**
 * Runnable AnalysisTask - Adapts a Runnable to run within a pre-created thread.
 * This object class is used by AnalysisManager.
 * 
 * @author Rajesh Kuttan
 * @version
 */

public class AnalysisTask implements Runnable {

    private Object jobQueueWaitObject = new Object();

    AnalysisDAO ds = new AnalysisDAO();
    private Vector jobQueue = new Vector();

    // Semaphore to maintain simultaneous job count
    private Semaphore sem = null;

    private static org.apache.log4j.Category log = org.apache.log4j.Logger.getInstance(AnalysisTask.class);

    volatile boolean runFlag = true;

    private final static String TASK_NAME = GenePatternAnalysisTask.TASK_NAME;

    private GenePatternAnalysisTask genePattern = new GenePatternAnalysisTask();

    private static AnalysisTask instance;

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

    /** Main AnalysisTask's thread method. */
    public void run() {
        log.debug("Starting AnalysisTask thread");

        while (true) {

            // Load input data to input queue
            synchronized (jobQueueWaitObject) {

                if (jobQueue.isEmpty()) {
                    try {
                        HibernateUtil.getSession().beginTransaction();
                        jobQueue = genePattern.getWaitingJobs();
                        HibernateUtil.getSession().getTransaction().commit();
                    }
                    finally {
                        if (HibernateUtil.getSession().isOpen()) {
                            HibernateUtil.getSession().close();
                        }
                    }

                }

                if (jobQueue.isEmpty()) {
                    try {
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

    private AnalysisTask(int threadCount) {

        // create semaphore when thread count >0
        if (threadCount > 0) {
            sem = new Semaphore(threadCount);
        }
        try {
            ds = new AnalysisDAO();
            ;
        }
        catch (OmnigeneException oe) {
            log.error(oe);
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

    public static AnalysisTask getInstance() {
        return instance;
    }

    private class JobThread extends Thread {

        Object obj = null;

        public JobThread(Object o) {
            this.obj = o;
        }

        public void run() {
            genePattern.onJob(obj);// run job
            doRelease();// signal completion of thread
        }
    }

    static {
        instance = new AnalysisTask(GenePatternAnalysisTask.NUM_THREADS);
        Thread runner = new Thread(instance);
        runner.start();
    }

}
