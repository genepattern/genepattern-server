package org.genepattern.server.analysis;
import java.rmi.RemoteException;
import java.util.Vector;

import org.genepattern.analysis.OmnigeneException;
import org.genepattern.server.analysis.ejb.AnalysisJobDataSource;
import org.genepattern.server.analysis.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.util.BeanReference;




/**
 *  Runnable AnalysisTask - Adapts a Runnable to run within a pre-created
 *  thread. This object class is used by AnalysisManager.
 *
 * @author     Rajesh Kuttan
 * @version
 */

public class AnalysisTask implements Runnable {
   AnalysisJobDataSource ds = null;

   private Vector jobQueue = new Vector();
   //Semaphore to maintain simultaneous job count
   private Semaphore sem = null;
   private static org.apache.log4j.Category log = org.apache.log4j.Logger.getInstance(AnalysisTask.class);
   volatile boolean runFlag = true;
   private final static String TASK_NAME = GenePatternAnalysisTask.TASK_NAME;
   private GenePatternAnalysisTask genePattern = new GenePatternAnalysisTask();
   private static AnalysisTask instance;
   
   private AnalysisTask() {}
   
  
   
   public String getTaskName() {
      return TASK_NAME;  
   }
   
  
   /**
    *  Add an object to queue
    *
    * @param  o  The feature to be added to the JobToQueue attribute
    */
   public void addJobToQueue(Object o) {
      jobQueue.add(o);
   }


   /**  Clears the AnalysisTask's queue. */
   public void clear() {
      jobQueue.clear();
   }


   /**  Main AnalysisTask's thread method. */
   public void run() {
     
      while(true) {
       
         //Load input data to input queue
         synchronized(ds) {
           
            if(jobQueue.isEmpty()) {
               jobQueue = genePattern.getWaitingJobs();
              
            }
            if(jobQueue.isEmpty()) {
               try {
                  ds.wait();
               } catch(InterruptedException ie) {}
            }
         }
      
        
         Object o = null;
      
         if(!jobQueue.isEmpty()) {
            o = jobQueue.remove(0);
         }
      
         if(o == null) {
            continue;
         }
        
         try {
            onJobProcessFrameWork(o);

         } catch(Exception ex) {
            log.error(ex);
         }

      }

   }


   /**
    *  This is placeholder for doing process, which should be done in begining
    *  and end of each job
    *
    * @param  o  Description of the Parameter
    */
   public void onJobProcessFrameWork(Object o) {
      doAcquire();//if max job running, then wait until some thread to finish
      new JobThread(o).start();
   }


   public String toString() {
      return TASK_NAME;
   }


   private AnalysisTask(int threadCount) {
     
      //create semaphore when thread count >0
      if(threadCount > 0) {
         sem = new Semaphore(threadCount);
      }
      try {
         ds = BeanReference.getAnalysisJobDataSourceEJB();
      } catch(RemoteException re) {
         log.error(re);
      } catch(OmnigeneException oe) {
         log.error(oe);
      }

   }


   private void doAcquire() {
      if(sem != null) {
         sem.acquire();
      }
   }


   private void doRelease() {
      if(sem != null) {
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
         genePattern.onJob(obj);//run job
         doRelease();//signal completion of thread
      }
   }

   static {
      instance = new AnalysisTask(GenePatternAnalysisTask.NUM_THREADS);
      Thread runner = new Thread(instance);
      runner.start();
   }

}

