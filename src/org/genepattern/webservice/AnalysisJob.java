package org.genepattern.webservice;

import java.io.Serializable;

/**
 *  Client side job information - includes server and JobInfo.
 *
 * @author        Hui Gong
 * @version       $Revision 1.2 $
 */

public class AnalysisJob implements Serializable {
   private String server;
   private JobInfo job;



   /**  Constructs a new <tt>AnalysisJob</tt> . */
   public AnalysisJob() { }


   /**
    *  Constructs a new <tt>AnalysisJob</tt>
    *
    * @param  server    The server on which this analysis job was run
    * @param  job       The <tt>JobInfo</tt>
    */
   public AnalysisJob(String server, JobInfo job) {
      this.server = server;
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
      job.setTaskName(s);
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
      job.setTaskLSID(lsid);
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
      return job.getTaskLSID();
   }


   /**
    *  Gets the task name of the task that produced this analysis job.
    *
    * @return    The task name
    */
   public String getTaskName() {
      return job.getTaskName();
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
