package org.genepattern.webservice;

import java.io.Serializable;

/**
 *  Includes server, task name and JobInfo about a analysis job.
 *
 * @author     Hui Gong
 * @version    $Revision 1.2 $
 */

public class AnalysisJob implements Serializable {
   private String server;
   private String taskName;
   private JobInfo job;
   private String lsid;


   /**  Constructs a new <tt>AnalysisJob</tt> .  */
   public AnalysisJob() { }


   /**
    *  Constructs a new <tt>AnalysisJob</tt>
    *
    * @param  server    The server on which this analysis job was run
    * @param  taskName  The task name
    * @param  job       The <tt>JobInfo</tt>
    */
   public AnalysisJob(String server, String taskName, JobInfo job) {
      this.server = server;
      this.taskName = taskName;
      this.job = job;
   }


   /**
    *  Sets the server on which this analysis job was run
    *
    * @param  s  the server
    */
   public void setServer(String s) {
      this.server = s;
   }


   /**
    *  Sets the task name of the task that produced this analysis job.
    *
    * @param  s  The task name
    */
   public void setTaskName(String s) {
      this.taskName = s;
   }


   /**
    *  Sets the <tt>JobInfo</tt> for this analysis job
    *
    * @param  job  The <tt>JobInfo</tt>
    */
   public void setJobInfo(JobInfo job) {
      this.job = job;
   }



   /**
    *  Sets the LSID of the task that produced this analysis job.
    *
    * @param  lsid  The task LSID
    */
   public void setLSID(String lsid) {
      this.lsid = lsid;
   }


   /**
    *  Gets the server on which this analysis job was run
    *
    * @return    the server
    */
   public String getServer() {
      return this.server;
   }


   /**
    *  Gets the LSID of the task that produced this analysis job.
    *
    * @return    The task LSID
    */

   public String getLSID() {
      return lsid;
   }


   /**
    *  Gets the task name of the task that produced this analysis job.
    *
    * @return    The task name
    */
   public String getTaskName() {
      return this.taskName;
   }


   /**
    *  Gets the <tt>JobInfo</tt> for this analysis job
    *
    * @return    The <tt>JobInfo</tt>
    */
   public JobInfo getJobInfo() {
      return this.job;
   }

}
