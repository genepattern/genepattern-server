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

/**
 * AnalysisManager - Manager for AnalysisTask Runnable adapter
 *
 * @version $Revision 1.4$
 * @author Rajesh Kuttan, Hui Gong
 */

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.OmnigeneException;

public class AnalysisManager {
    private static Logger log = Logger.getLogger(AnalysisManager.class);
    
    public static int defPriority = Thread.NORM_PRIORITY;
    private static AnalysisManager analysisManager = null;
    private static ThreadGroup threadGrp = new ThreadGroup(AnalysisManager.class.getName());
    
    // To make Singleton
    private AnalysisManager() {
    }
    
    //Function to get singleton instance
    public static synchronized AnalysisManager getInstance() {
        if (analysisManager == null) {
            // System.out.println("creating new AnalysisManager");
            analysisManager = new AnalysisManager();
            
            //TODO: re-implement handling runtime exec jobs which need to be restarted
            //      this code belongs in the RuntimeExecCmdExecSvc class
            try {
                HibernateUtil.beginTransaction();
                
                analysisManager.startAllAnalysisTask();
                AnalysisDAO ds = new AnalysisDAO();
                // were there interrupted jobs that need to be restarted?
                if (ds.resetPreviouslyRunningJobs()) {
                    System.out.println("There were previously running tasks, notifying threads.");
                    // yes, notify the threads to start processing
                    synchronized (ds) {
                        System.out.println("notifying ds of job to run");
                        ds.notify();
                    }
                }
                
                HibernateUtil.commitTransaction();
                
            } catch (OmnigeneException oe) {
                log.error(oe);
                HibernateUtil.rollbackTransaction();
            }
        }
        return analysisManager;
    }
    
    /** Get the thread group */
    public ThreadGroup getThreadGroup() {
        return threadGrp;
    }
    
    /** no op. */
    public boolean stop(String taskName) {
        // jgould: was previously used terminate AnalyisTask task and remove from hashtable (key was taskName)
        return false;
    }
    
    public boolean stop(int taskID) {
        // jgould: was previously used terminate AnalyisTask task and remove from hashtable (key was taskName)
        return false;
    }

    /** no op. */
    public void stopAllAnalysisTask() {
        // jgould: was previously used terminate all AnalyisTask tasks and remove from hashtable (key was taskName)
    }
    
    /** no op. */
    public String startNewAnalysisTask(AnalysisTask task, int taskID) throws OmnigeneException {
        return GenePatternAnalysisTask.TASK_NAME;
    }
    
    /** no op. */
    public String startNewAnalysisTask(int taskID) throws OmnigeneException {
        return GenePatternAnalysisTask.TASK_NAME;
    }
    
    /** no op. */
    public synchronized void startAllAnalysisTask() throws OmnigeneException {
    }
    
}

//Helper class to hold AnalysisTask and its Thread class.

class TaskObject {
    private Thread threadInstance = null;
    
    private Object taskObj = null;
    
    private int taskID = -1;
    
    public TaskObject(Thread t, Object taskObj, int taskID) {
        this.threadInstance = t;
        this.taskObj = taskObj;
        this.taskID = taskID;
    }
    
    public Object getTaskObj() {
        return taskObj;
    }
    
    public int getTaskID() {
        return taskID;
    }
    
    public Thread getThread() {
        return threadInstance;
    }
    
    public void setNull() {
        this.threadInstance = null;
        this.taskObj = null;
    }
}
