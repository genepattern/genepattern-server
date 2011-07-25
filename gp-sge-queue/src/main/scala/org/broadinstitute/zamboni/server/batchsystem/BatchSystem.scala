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

import org.broadinstitute.zamboni.server.util.ZamboniDef._

/**
 * Top level interface for the batch system
 */
trait BatchSystem {

  /**
   * Returns the batch system name passed to the constructor.
   */
  def getName : String

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
          restartable: Option[Boolean] = None) : BatchJob

	/** Returns true if the ended-jobs queue is currently empty. */
	def isEndedJobsQueueEmpty : Boolean

	/** Returns the oldest ended job without taking it off the queue. */
	def peekEndedJobsQueue : BatchJob

	/** Returns the oldest ended job and removes it from the queue. */
	def popEndedJobsQueue : BatchJob

  /** Wait up to timeoutSecs to pop and return the first item in the queue, else return None after timeout has elapsed.  */
  def pollEndedJobsQueue(timeoutSecs: Int): Option[BatchJob]

	/** Callback that gets called to notify the BatchSystem that a job has ended. */
	def jobEnded(endedBatchJob : BatchJob)

	/** Submits a BatchJob to be run in the batch system. */
	def submit(job : BatchJob) : BatchJob

	/** Gets the current status of a job. */
	def getStatus( submittedBatchJob : BatchJob ) : JobStatus

	/**
   * Kills the batch job.
   * @return True if the job could be killed.  Typically a false return indicates that the job had already ended. 
   */
	def kill( submittedBatchJob : BatchJob ): Boolean

	/** This method should be called to clean up any native/external resources before the application exits. */
	def shutDown //used to clean up the SGE session.

  /**
   * Tells the batch system to track the the given BatchJobs. Assumes that these batchJobs. The BatchJob batchSystemName
   * must match this batch system's name.   
   */
  def restore( batchJobs : BatchJob* )

  def isMonitoringJobs: Boolean

  /** Called by JobMonitor to deal with a job with LOST_JOB status. */
  def handleLostJob(batchJob: BatchJob)
}

class BatchTransientException(message: String, cause: Throwable = null) extends RuntimeException(message: String, cause: Throwable)
class BatchPermanentException(message: String, cause: Throwable) extends RuntimeException(message: String, cause: Throwable)