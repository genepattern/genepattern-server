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

package org.broadinstitute.zamboni.server.batchsystem.sge

import org.broadinstitute.zamboni.server.batchsystem.BatchJob
import org.broadinstitute.zamboni.server.util.BackgroundTask
import LostJobMonitor._
import org.broadinstitute.zamboni.server.batchsystem.BatchJob
import java.util.concurrent.LinkedBlockingQueue
import org.broadinstitute.zamboni.server.util.log.Log
import org.broadinstitute.zamboni.server.ServerInitializer
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashMap
import java.util.Date

object LostJobMonitor {
  val TIME_BETWEEN_STATUS_CHECKS = 2 // sec
  private val JOIN_TIME = 30 // sec

  // Wait 60 sec before checking accounting file, because of latency writing accounting file
  val WAIT_BEFORE_CHECK_ACCOUNTING = 60 * 1000L // msec

  private class JobToMonitor(val batchJob: BatchJob) {
    val submitTime = System.currentTimeMillis

    override def toString() : String = {
      "JobToMonitor(batchJob=" + batchJob.toString + ", submitTime=" + submitTime + ")"
    }
  }

}

class LostJobMonitor(val batchSystem : SgeBatchSystem)
        extends BackgroundTask(shutdownTimeoutSeconds = JOIN_TIME,
          loopSleepSeconds = TIME_BETWEEN_STATUS_CHECKS,
          priorityOffset = -1) {

  private val jobsToMonitor = new LinkedBlockingQueue[JobToMonitor]

  //this should never be mutated asynchronously
  private val jobsToConsider    = new scala.collection.mutable.HashSet[JobToMonitor] with scala.collection.mutable.SynchronizedSet[JobToMonitor]

  def addJob(job : BatchJob) {
    jobsToMonitor.put(new JobToMonitor(job))
    Log.info("LostJobMonitor adding job: " + job)
  }

  def loopBody: Unit = {
    copyQueueToJobSet()
    considerJobs()
  }

  /**
   * Extract from the jobsToMonitor queue all jobs that are currently ready to be handled.  Sort these jobs
   * by timestamp and then return them.
   *
   * @return A sorted list of jobs that can be currently be dispatched as ended
   */
  private def copyQueueToJobSet() = {
    var jobToMonitor : JobToMonitor = null;
    try {
      while(jobsToMonitor.peek != null) {
        jobToMonitor = jobsToMonitor.take()
        jobsToConsider += jobToMonitor
      }
    }
    catch {
      case exc : Exception => Log.internalError("Exception while moving jobs from the lost jobs queue.  " +
                                (if(jobToMonitor != null) "Last handled JobToMonitor: " + jobToMonitor else ""), exc)
    }
  }

  /**
   * Given a list of batch jobs that are ready to be handled as ended, read and update those jobs that have been handled
   * and can be found in the SGE accounting file, otherwise report as ended to SGE (see individual cases)
   *
   * ripeJobs are jobs that were submitted greater than WAIT_BEFORE_CHECK_ACCOUNTING milliseconds ago
   */
  private def considerJobs() {
    if(jobsToConsider.size == 0) return

    val ripeWithTimeJobs = new HashMap[String, JobToMonitor]
    var smallestTime = Long.MaxValue

    try {
      jobsToConsider.synchronized {
        for(jobToMonitor <- jobsToConsider) {
          val batchJob = jobToMonitor.batchJob

          if (System.currentTimeMillis - jobToMonitor.submitTime > WAIT_BEFORE_CHECK_ACCOUNTING) {
            if (batchJob.submitTime.isEmpty) {
              Log.info("SGE session lost track of " + batchJob + "; checking accounting file.")
              Log.internalError("Lost batch job has no submit time: " + batchJob)
              batchSystem.jobEnded(batchJob)
              jobsToConsider -= jobToMonitor

            } else {
              if (batchJob.submitTime.get.getTime < smallestTime)
                smallestTime = batchJob.submitTime.get.getTime
              ripeWithTimeJobs += batchJob.getJobId.get -> jobToMonitor
            }
          }
        }

        if(ripeWithTimeJobs.size > 0) {
          val reader = new AccountingReader(ServerInitializer.singleton.sgeAccountingFile, new Date(smallestTime))

          Log.info((("SGE session lost track of the following jobs: " /: ripeWithTimeJobs)(_ + " " + _._2.batchJob.toString)) + "; checking accounting file.")

          while(reader.hasNext && !ripeWithTimeJobs.isEmpty) {
            val accountLine = reader.next
            val jobFound = ripeWithTimeJobs.get(accountLine.jobId)
            jobFound.map((jtm : JobToMonitor) => {
              batchSystem.jobEndedFromAccounting(jtm.batchJob, accountLine)
              ripeWithTimeJobs     -= (accountLine.jobId)
              jobsToConsider  -= jtm
            })
          }

          for(remainingJob <- ripeWithTimeJobs) {
             Log.warning(remainingJob + " could not be found in accounting file -- assuming job is gone.")
             batchSystem.lostJobEnded(remainingJob._2.batchJob)
             jobsToConsider -= remainingJob._2
          }
        }
      }
    }
    catch {
      case exc : Exception => Log.internalError("Exception while handling lost jobs", exc)
    }

  }

  def numJobsMonitoring = jobsToMonitor.size + jobsToConsider.size
}
