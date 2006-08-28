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

/**
 * AnalysisManager - Manager for AnalysisTask Runnable adapter
 * 
 * @version $Revision 1.4$
 * @author Rajesh Kuttan, Hui Gong
 */

import java.rmi.RemoteException;

import org.apache.log4j.Logger;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.server.webservice.server.dao.HibernateUtil;
import org.genepattern.webservice.OmnigeneException;

public class AnalysisManager {
    
    private static Logger log = Logger.getLogger(AnalysisManager.class);

	public static int defPriority = Thread.NORM_PRIORITY;

	private static AnalysisManager analysisManager = null;

	private static ThreadGroup threadGrp = new ThreadGroup(
			AnalysisManager.class.getName());

	// To make Singleton
	protected AnalysisManager() {
	}

	//Function to get singleton instance
	public static synchronized AnalysisManager getInstance() {
		if (analysisManager == null) {
			// System.out.println("creating new AnalysisManager");
			analysisManager = new AnalysisManager();
			try {
                HibernateUtil.getSession().beginTransaction();
                
				analysisManager.startAllAnalysisTask();
				AnalysisDAO ds = new AnalysisDAO();
				// were there interrupted jobs that need to be restarted?
				if (ds.resetPreviouslyRunningJobs()) {
					System.out
							.println("There were previously running tasks, notifying threads.");
					// yes, notify the threads to start processing
					synchronized (ds) {
						System.out.println("notifying ds of job to run");
						ds.notify();
					}
				}
                
                HibernateUtil.getSession().getTransaction().commit();
                
			} catch (OmnigeneException oe) {
				log.error(oe);
                 HibernateUtil.getSession().getTransaction().rollback();
			} 
		}
		return analysisManager;
	}

	/** Get the thread group */
	public ThreadGroup getThreadGroup() {
		return threadGrp;
	}

	/**
	 * Stop a running task
	 * 
	 * @param String
	 *            Task Name
	 */
	public boolean stop(String taskName) {
		// jgould: was previously used terminate AnalyisTask task and remove
		// from hashtable (key was taskName)
		return false;

	}

	public boolean stop(int taskID) {
		// jgould: was previously used terminate AnalyisTask task and remove
		// from hashtable (key was taskName)
		return false;
	}

	/** Stops the main thread */
	public void stopAllAnalysisTask() {
		// jgould: was previously used terminate all AnalyisTask tasks and
		// remove from hashtable (key was taskName)
	}

	/**
	 * Start a new Task
	 * 
	 * @param AnalysisTask
	 */
	public synchronized String startNewAnalysisTask(AnalysisTask task,
			int taskID) throws OmnigeneException {
		// jgould: was previously used to create new instances of AnalysisTask
		// based on taskName
		return AnalysisTask.getInstance().getTaskName();

	}

	/**
	 * Starts up an analysis task based on the task id
	 * 
	 * @param taskID
	 *            analysis task id
	 * @return task name
	 * @throws OmnigeneException
	 */
	public synchronized String startNewAnalysisTask(int taskID)
			throws OmnigeneException {
		// jgould: was previously used to create new instances of AnalysisTask
		// based on taskName
		return AnalysisTask.getInstance().getTaskName();
	}

	public synchronized void startAllAnalysisTask() throws OmnigeneException {
		// jgould: was previously used to create new instances of AnalysisTask
		// based on taskName
		AnalysisTask.getInstance(); // start thread
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
		;
	}
}