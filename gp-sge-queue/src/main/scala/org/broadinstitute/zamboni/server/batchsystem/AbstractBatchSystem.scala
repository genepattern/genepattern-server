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

import java.util.concurrent.{TimeUnit, LinkedBlockingQueue}
import org.broadinstitute.zamboni.server.util.ZamboniDef._
import java.util.Date
import org.broadinstitute.zamboni.server.util.log.Log

/**
 * Takes the batch system.
 */
abstract class AbstractBatchSystem(private val batchSystemName : String) extends BatchSystem {

	private val completedJobsQueue = new LinkedBlockingQueue[BatchJob]

  /** Returns the batch system name passed to the constructor. */
  def getName = {
    batchSystemName;
  }

  /**
	 * Factory method that returns a new BatchJob object which can be configured and
	 * submitted to the batch system.
	 */
	def newBatchJob(
          workingDirectory: Option[String] = None,
          command: Option[String] = None,
          args: Array[String] = Array[String](),
          outputPath: Option[String] = None,
          errorPath: Option[String] = None,
          emailAddresses: Option[Array[String]] = None,
          priority: Option[Int] = None,
          jobName:  Option[String] = None,
          queueName: Option[String] = None,
          exclusive: Option[Boolean] = None,
          maxRunningTime: Option[Int] = None,
          memoryReservation: Option[Int] = None,
          maxMemory: Option[Int] = None,
          slotReservation: Option[Int] = None,
          maxSlots: Option[Int] = None,
          restartable: Option[Boolean] = None) : BatchJob = {
    val job = new BatchJob(this)
    job.setWorkingDirectory(workingDirectory)
    job.setCommand(command)
    job.setArgs(args)
    job.setOutputPath(outputPath)
    job.setErrorPath(errorPath)
    job.setEmailAddresses(emailAddresses)
    job.setPriority(priority)
    job.setJobName(jobName)
    job.setQueueName(queueName)
    job.setExclusive(exclusive)
    job.setMaxRunningTime(maxRunningTime)
    job.setMaxMemory(maxMemory)
    job.setSlotReservation(slotReservation)
    job.setMaxSlots(maxSlots)
    job.setMemoryReservation(memoryReservation)
    job.setRestartable(restartable)
    job
  }

  /** Submits a BatchJob to be run in the batch system. */
	def submit(job : BatchJob) : BatchJob = {
    validateJob(job)

    job;
  }



  /** Gets the current status of a job. */
  def getStatus( submittedBatchJob : BatchJob ) : JobStatus = {
    validateJob(submittedBatchJob)

    return JobStatus.UNKNOWN
  }

  /** Kills the batch job. */
  def kill( submittedBatchJob : BatchJob ): Boolean = {
    validateJob(submittedBatchJob)
    true
  }


	/** Returns true if the ended-jobs queue is currently empty. */
	def isEndedJobsQueueEmpty = {
    completedJobsQueue.isEmpty
  }

	/** Returns the oldest ended job without taking it off the queue. */
	def peekEndedJobsQueue = {
    completedJobsQueue.peek
  }

	/** Returns the oldest ended job and removes it from the queue. */
	def popEndedJobsQueue = {
    completedJobsQueue.take
  }

  def numCompletedJobsInQueue = {
    completedJobsQueue.size
  }


  def pollEndedJobsQueue(timeoutSecs: Int) = {
    val ret = completedJobsQueue.poll(timeoutSecs, TimeUnit.SECONDS)
    if (ret == null) None
    else Some(ret)
  }

  /** Callback that gets called to when a job ends. */
  def jobEnded(endedBatchJob : BatchJob) = {
    if (endedBatchJob.endTime.isEmpty) endedBatchJob.endTime = new Date
    Log.info("Adding to completed jobs queue: " + endedBatchJob)
    completedJobsQueue.put(endedBatchJob)
  }

  /**
   * Tells the batch system to track the the given BatchJobs.
   */
  def restore( batchJobs : BatchJob* ) = {
    batchJobs.foreach( ( job ) => validateJob(job) )
  }
    


  override def toString : String = {
    getClass.getSimpleName + "[ batchSystemName=" + getName + " ]"
  }


  protected def validateJob( job : BatchJob ) = {
    if(job.getBatchSystem.getName != batchSystemName) {
      throw new IllegalArgumentException("BatchJob object was created by " + job.getBatchSystem.getName + " and should be used with that batch system rather than with " + batchSystemName )
    }
  }
}
