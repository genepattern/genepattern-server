/*
 * The MIT License
 *
 * Copyright (c) 2010 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.broadinstitute.zamboni.server.batchsystem


import java.util.Iterator
import org.broadinstitute.zamboni.server.util.log.Log
import java.util.concurrent.ConcurrentHashMap
import org.broadinstitute.zamboni.server.util.{BackgroundTask, MakeDaemon}

object JobMonitor {
  val TIME_BETWEEN_STATUS_CHECKS = 20 // sec
  private val JOIN_TIME = 30 // sec
  private val UNKNOWN_STATUS_GIVE_UP_TIME = 60 * 60 * 1000 // millisec
  // unknownStatusTime is in msec since the epoch.
  private class JobToMonitor(val batchJob: BatchJob, var unknownStatusTime: Option[Long] = None) {
    override def equals(obj: Any): Boolean = obj match {
      case other: JobToMonitor => this eq other
      case _ => false
    }

    override def hashCode: Int = System.identityHashCode(this)
  }
}

/**
 * Top level interface for the batch system
 */
class JobMonitor(val batchSystem : BatchSystem)
        extends BackgroundTask(shutdownTimeoutSeconds = JobMonitor.JOIN_TIME,
          loopSleepSeconds = JobMonitor.TIME_BETWEEN_STATUS_CHECKS,
          priorityOffset = -1) {
  import JobMonitor._


  private val jobsToMonitor = new ConcurrentHashMap[JobToMonitor, JobToMonitor]

  def addJob(job : BatchJob) {
    if (job.getJobId.isEmpty) {
      Log.internalError("Batch job with empty job id not being added to JobMonitor -- check for hung workflow: " + job)
    } else {
      val jobToAdd = new JobToMonitor(job)
      jobsToMonitor.put(jobToAdd, jobToAdd)
      Log.info("JobMonitor adding job: " + job)
    }
  }

  def loopBody = {
      val iter : Iterator[JobToMonitor] = jobsToMonitor.keySet.iterator

      var seen = 0;
      while(iter.hasNext && !shouldStop) {
        val jobToMonitor = iter.next
        if (jobToMonitor.batchJob.getJobId.isEmpty) {
          Log.internalError("Batch job with empty job id being removed from JobMonitor -- check for hung workflow: " + jobToMonitor.batchJob)
          iter.remove
        } else {
          val status = batchSystem.getStatus(jobToMonitor.batchJob)
          seen = seen + 1
          if (status == JobStatus.LOST_JOB) {
            batchSystem.handleLostJob(jobToMonitor.batchJob)
            iter.remove
          } else if (JobStatus.isTerminal(status)) {

            batchSystem.jobEnded(jobToMonitor.batchJob)
            Log.info("JobMonitor removing job: " + jobToMonitor.batchJob + " completionStatus=" +
              jobToMonitor.batchJob.getCompletionStatus )
            iter.remove
          } else if (status == JobStatus.UNKNOWN) {
            if (jobToMonitor.unknownStatusTime.isEmpty) {
              jobToMonitor.unknownStatusTime = Some(System.currentTimeMillis)
              Log.info("UNKNOWN status for batch job, waiting a little while: " + jobToMonitor.batchJob)
            }
            else if (System.currentTimeMillis - jobToMonitor.unknownStatusTime.get > UNKNOWN_STATUS_GIVE_UP_TIME) {
              batchSystem.jobEnded(jobToMonitor.batchJob)
              Log.info("JobMonitor removing job with unknown status: " + jobToMonitor.batchJob +
                " completionStatus=" + jobToMonitor.batchJob.getCompletionStatus )
              iter.remove
            }
            // Else wait a little longer
          } else {
            // Got a non-UNKNOWN status, so clear timer.
            jobToMonitor.unknownStatusTime = None
          }
        }
      }
      if (seen > 0) Log.info("JobMonitor examined " + seen + " job(s).")
  }

  def isMonitoringJobs = !jobsToMonitor.isEmpty
  def numJobsMonitoring = jobsToMonitor.size
}
