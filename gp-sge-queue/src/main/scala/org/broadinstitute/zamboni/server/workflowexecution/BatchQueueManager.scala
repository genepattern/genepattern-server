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

package org.broadinstitute.zamboni.server.workflowexecution

import org.broadinstitute.zamboni.server.util.log.Log
import org.broadinstitute.zamboni.server.ServerInitializer
import BatchQueueManager._
import org.broadinstitute.zamboni.server.db.ZamboniSchema
import org.broadinstitute.zamboni.server.batchsystem._
import org.broadinstitute.zamboni.server.util.BackgroundTask
import org.broadinstitute.zamboni.server.util.ZamboniDef._
import org.broadinstitute.zamboni.server.db.entity.StepJob
import org.broadinstitute.zamboni.server.db.entity._
import org.squeryl.PrimitiveTypeMode._
import org.broadinstitute.zamboni.server.notification.Notifier
import java.util.concurrent.{TimeUnit, LinkedBlockingQueue}
import java.io.{PrintWriter, StringWriter}
import org.broadinstitute.zamboni.util.ExceptionUtil

object BatchQueueManager {
  private val Permanent = "Permanent"
  private val Transient = "Transient"
  val TIME_BETWEEN_POSSIBLE_INTERRUPTS = 5 //seconds
  val DELAY_AFTER_ERROR = 2000 //milliseconds
  private val JOIN_TIME = 30
  
  private var theSingleton: Option[BatchQueueManager] = None

  private def singleton = {
    if (theSingleton.isEmpty) throw new RuntimeException("BatchQueueManager is not initialized.")
    theSingleton.get
  }

  def initSingleton() = {
    if (!theSingleton.isDefined) {
      theSingleton = Some(new BatchQueueManager())
      theSingleton.get.start
    }
  }

  def shutdownSingleton = {
    if (theSingleton.isDefined) {
      theSingleton.get.stop
      theSingleton = None
    }
  }

  def addJobToSubmit(stepJob : StepJob, batchJob : BatchJob, workflow : Workflow)  {
    theSingleton.get.addJobToSubmit(stepJob, batchJob, workflow)
  }

  def numQueuedJobs = singleton.numQueuedJobs

  class BatchQueueManager( )
          extends BackgroundTask(shutdownTimeoutSeconds = JOIN_TIME,
            priorityOffset = 0) {

    private val jobs = new LinkedBlockingQueue[(StepJob, BatchJob, Workflow)]

    def numQueuedJobs = jobs.size

    def addJobToSubmit(stepJob : StepJob, batchJob : BatchJob, workflow : Workflow) {
      jobs.put((stepJob, batchJob, workflow))
    }

    def logAndSendErrorMessage(exceptionType : String, batchJob : BatchJob, workflow : Workflow, exception : Exception) {
      try {
        Log.error(exceptionType + " batch error thrown for batch job " + batchJob, exception)

        val excStr = ExceptionUtil.throwableToString(exception)

        val maybeResubmit = if (exceptionType == Transient) "This job will be resubmitted.\n" else ""
        val msg = "The following " + exceptionType + " Exception occured while submitting " + batchJob + " : " + "\n" +
                   maybeResubmit + "\n" + excStr
        Notifier.notifyUnconditionally(workflow, Status.FAILED,  exceptionType + " batch error " + workflow.name + " (" + workflow.id + ")", msg)
      }
      catch {
        //This shouldn't happen but if we throw an uncaught exception in this function then the job being logged gets dropped without being updated
        case exe : Exception => Log.internalError("Exception while batch queue manager was reporting the following exception: " +
                                                   (if(exception != null) exception.getStackTraceString + "\n" + exception.getMessage else "null"))
      }
    }

    def failedStepCase(jobInfo : (StepJob, BatchJob, Workflow), status : Status.Value) {
        transaction {
          val stepJob  = jobInfo._1.selectThisForUpdate
          val batchJob = jobInfo._2
          val workflow = jobInfo._3
          stepJob.status = Status.FAILED
          stepJob.submissionDate = nowTimestamp
          ZamboniSchema.stepJobs.update(stepJob)

          RunnerCoordinator.queueMessage(new JobEndedMessage(workflow.id, stepJob.id, Some(batchJob), status))
        }
    }

    def loopBody: Unit = {

      val jobInfo = jobs.poll(TIME_BETWEEN_POSSIBLE_INTERRUPTS, TimeUnit.SECONDS)
      if (jobInfo != null) {
        val stepJob  = jobInfo._1
        val batchJob = jobInfo._2
        val workflow = jobInfo._3
        try {
          transaction {
            val stepJobLock = stepJob.selectThisForUpdate

            Log.info("Submitting batch job " + batchJob)

            batchJob.getBatchSystem.submit(batchJob)
            stepJob.externalId = batchJob.getJobId.get
            stepJob.status = Status.RUNNING
            stepJob.submissionDate = nowTimestamp
            ZamboniSchema.stepJobs.update(stepJob)
          }

        } catch {
          case exe : BatchTransientException =>
            logAndSendErrorMessage(Transient, batchJob, workflow, exe)
            jobs.put(jobInfo)
            Thread.sleep(DELAY_AFTER_ERROR)

          case exe : BatchPermanentException =>
            logAndSendErrorMessage(Permanent, batchJob, workflow, exe)
            failedStepCase(jobInfo, Status.FAILED)
            Thread.sleep(DELAY_AFTER_ERROR)

          case exe : Exception =>
            Log.internalError("Unrecognized exception thrown while submitting SGE job: " + jobInfo, exe)
            failedStepCase(jobInfo, Status.INTERNAL_ERROR)
            Thread.sleep(DELAY_AFTER_ERROR)
        }
      }
    }
  }
}
