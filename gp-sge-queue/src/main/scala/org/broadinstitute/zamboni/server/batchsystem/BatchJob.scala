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
import org.broadinstitute.zamboni.server.workflowexecution.ZamboniJob
import java.util.Date
import collection.mutable.HashMap


/**
 * Encapsulates all configuration and status information for a batch job.
 */
class BatchJob(val batchSystem : BatchSystem) extends ZamboniJob with Cloneable {

  private var workingDirectory : Option[String] = None
  private var theCommand : Option[String] = None
  private var args : Option[Array[String]] = None
  private var outputPath : Option[String] = None
  private var errorPath : Option[String] = None
  private var emailAddresses : Option[Array[String]] = None  //emails to which job completion status should be sent
  private var priority : Option[Int] = None //batch-system-specific priority setting. SGE "-p 1023" arg.

  private var theJobName : Option[String] = None
  private var queueName : Option[String] = None

  private var exclusive : Option[Boolean] = None //LSF (-x) & SGE (-l excl=true)
  private var maxRunningTime : Option[Int] = None //minutes

  private var memoryReservation : Option[Int] = None //megabytes - SGE "-l h_vmem=..." arg, LSF -M arg
  private var maxMem : Option[Int] = None  //megabytes -  SGE "-l virtual_free=..." arg

  private var slotReservation : Option[Int] = None //number of CPU slots
  private var maxSlots : Option[Int] = None //max number of CPU slots - hard limit


  private var restartable: Option[Boolean] = None

  val resourceRequirements = new HashMap[String, String] 

  private var jobId : Option[String] = None
  private var jobCompletionStatus : Option[JobCompletionStatus] = None

  private var returnCode : Option[Int] = None

  var submitTime: Option[Date] = None

  var startTime: Option[Date] = None

  // Some batch systems may report the end time.  If not, then the time when Zamboni recognizes that the job is
  // done will be put here.
  var endTime: Option[Date] = None

  private lazy val resourceUsage = new scala.collection.mutable.HashMap[String, String] //stores resource usage stats that become available during or after job execution

  /**
   * Returns a BatchJob that is a clone of this job, except for the return state and job id which are left uninitialized.  
   */
  def createUnstartedCopy : BatchJob = {
    val bj = batchSystem.newBatchJob()
    bj.setWorkingDirectory(workingDirectory)
    bj.setCommand(theCommand)
    bj.setArgs(args)
    bj.setOutputPath(outputPath)
    bj.setErrorPath(errorPath)
    bj.setEmailAddresses(emailAddresses)
    bj.setPriority(priority)
    bj.setJobName(jobName)
    bj.setQueueName(queueName)
    bj.setExclusive(exclusive)
    bj.setMaxRunningTime(maxRunningTime)
    bj.setMemoryReservation(memoryReservation)
    bj.setMaxMemory(maxMem)
    bj.setSlotReservation(slotReservation)
    bj.setMaxSlots(maxSlots)
    bj.setRestartable(restartable)
    for ((key, value) <- resourceRequirements) {
      bj.addResourceRequirement(key, value)
    }
    return bj
  }

  def getWorkingDirectory = workingDirectory
  def setWorkingDirectory(wd : Option[String]) = { this.workingDirectory = wd }

  /** The command to execute (required) */
  def getCommand = theCommand
  def setCommand(cmd : Option[String]) = { this.theCommand = cmd }

  /** Args for the command */
  def getArgs = args
  def setArgs(args : Option[Array[String]]) = {
    args match {
      case None    => this.args = None
      case Some(a) => this.args = Some(a.filterNot(_ == null))
    }
  }

  /** File that will collect stdout (optional) */
  def getOutputPath = outputPath
  def setOutputPath(path : Option[String]) = { this.outputPath = path }

  /** File that will collect stderr (optional). If not provided, stderr will be written to outputPath */
  def getErrorPath = errorPath
  def setErrorPath(path : Option[String]) = { this.errorPath = path }

  /** Sets the email addresses to be notifies when the job completes (optional) */
  def getEmailAddresses = emailAddresses
  def setEmailAddresses( emailAddresses : Option[Array[String]] ) = { this.emailAddresses = emailAddresses }

  /** Batch-system-specific  priority setting. SGE "-p 1023" arg. TODO create a generic scale or enum for setting priorities in a batch-system independent way? */
  def getPriority = priority
  def setPriority(priority : Option[Int]) = { this.priority = priority }



  /** Job name */
  def getJobName = theJobName
  def setJobName(name : Option[String]) = { this.theJobName = name }

  /** Job queue name */
  def getQueueName = queueName
  def setQueueName(name : Option[String]) = { this.queueName = name }

  /** Whether to run the job in exclusive mode */
  def isExclusive = exclusive
  def setExclusive(exclusive : Option[Boolean]) = { this.exclusive  = exclusive }

  def isRestartable = restartable
  def setRestartable(restartable: Option[Boolean]) = this.restartable = restartable

  /** Hard memory limit in megabytes. Note: this is per slot. If you get 7 slots, your hard memory limit will be 7 * getMaxMemory */
  def getMaxMemory = maxMem
  def setMaxMemory(maxMemory : Option[Int]) = { this.maxMem = maxMemory }

  /** Reserves memory in megabytes. The job won't run unless/until this amount of memory is available on a node. */
  def getMemoryReservation = memoryReservation
  def setMemoryReservation(memRes : Option[Int]) = { this.memoryReservation = memRes }

  /** Hard running time limit in *minutes* */
  def getMaxRunningTime = maxRunningTime
  def setMaxRunningTime( maxRunningTime : Option[Int]) = { this.maxRunningTime = maxRunningTime }

  /** CPU slot limit - number of CPU's */
  def getMaxSlots = maxSlots
  def setMaxSlots( maxSlots : Option[Int]) = { this.maxSlots = maxSlots }

  /** Reserves CPU slots. */
  def getSlotReservation = slotReservation
  def setSlotReservation( slots : Option[Int]) = { this.slotReservation = slots }

  /** Whether to run the job in exclusive mode */
  def getJobId = this.jobId
  def setJobId(id : Option[String]) = { this.jobId = id }

  /** The job completion status - this is set when the job reaches a terminal status */
  def getCompletionStatus = jobCompletionStatus
  def setCompletionStatus(jobCompletionStatus : Option[JobCompletionStatus]) = { this.jobCompletionStatus = jobCompletionStatus }

  def getReturnCode = returnCode
  def setReturnCode(returnCode : Option[Int]) = { this.returnCode = returnCode}

  def getResourceUsage = resourceUsage

  /** Stores value.toString */
  def addResourceUsageStats( key : String, value : Any ) = { this.resourceUsage.put(key, value.toString) }

  def addResourceRequirement( key : String, value : String) : BatchJob = { this.resourceRequirements.put(key, value); this}


  /** The BatchSystem object that created this job. */ 
  def getBatchSystem = batchSystem

  def getStatus = getBatchSystem.getStatus(this)

  // Implementations of ZamboniJob abstract methods
  def command = getCommand.get
  def jobName = getJobName.getOrElse("no-job-name")

  def commandLine = command + args.get.mkString(" ", " ", "")

  /*
	val errorMessage : String; //available on LSF by grepping through the output file
	var startDate : Date; //available on LSF by grepping through the output file
	var endDate : Date; //available on LSF by grepping through the output file
	val node : String; //available on LSF by grepping through the output file
	*/

  override def toString : String = {
      getClass.getSimpleName + "[ batchSystemName=" + batchSystem.getName +
              ", jobName=" + (if (theJobName.isDefined) theJobName.get else "null") +
              ", id=" + (if (jobId.isDefined) jobId.get else "null") +
              ", command=[" + commandLine + "]" + 
              (if(getCompletionStatus != None) ", completionStatus=" + getCompletionStatus.get + ", returnCode=" + getReturnCode  else "") + "]"
  }


}
