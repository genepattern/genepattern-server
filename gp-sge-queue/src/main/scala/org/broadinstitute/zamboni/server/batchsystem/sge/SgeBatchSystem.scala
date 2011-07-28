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

import org.broadinstitute.zamboni.server.batchsystem._
import org.broadinstitute.zamboni.server.util.ZamboniDef._
import org.ggf.drmaa._
import org.broadinstitute.zamboni.server.util.log.Log
import io.Source
import java.util.{Date, LinkedList, HashSet}
import java.io.{FileWriter, BufferedWriter, PrintWriter, File}
import scala.collection.JavaConversions._
import SgeBatchSystem._

object SgeBatchSystem {
  private def jobInfoToString(jobInfo: JobInfo): String = {
    val tailInfo: List[String] =
      if (jobInfo.hasExited) List("exitStatus=" + jobInfo.getExitStatus)
      else if (jobInfo.hasSignaled) List("terminatingSignal=" + jobInfo.getTerminatingSignal)
      else Nil

    val resourceUsage = jobInfo.getResourceUsage
    val resourceUsageString = if (resourceUsage != null) resourceUsage.iterator.map(
                                                                (entry: (_, _)) => {
                                                                  entry._1.toString + "=" + entry._2.toString
                                                                }).mkString("[", ", ", "]")
        else "null"

    "JobInfo" + (List("jobId=" + jobInfo.getJobId,
      "hasCoreDump="+jobInfo.hasCoreDump,
      "hasExited="+jobInfo.hasExited,
      "hasSignaled="+jobInfo.hasSignaled,
      "wasAborted="+jobInfo.wasAborted,
      "resourceUsage="+resourceUsageString
    ):::tailInfo).mkString("(", ", ", ")")
  }

  //val contactFile = new File("conf", "sge_contact.txt")
  val contactFile = new File( System.getProperty("SGE_SESSION_FILE", System.getProperty("resources", ".") + "/conf/sge_contact.txt" ) );
  
  // This is empty in production, but for testing it may be set to something else via sge.project
  //val project = System.getProperty("SGE_PROJECT", "default_sge_project");
  val project = None;
  // $SGE_ROOT/$SGE_CELL/common/accounting 
  val sgeRoot = System.getProperty("SGE_ROOT", "sge_root");
  val sgeCell = System.getProperty("SGE_CELL", "sge_cell");
  val sgeAccountingFile : File = new File( sgeRoot + "/" + sgeCell + "/common/accounting" );
}

/**
 * Implementation that supports SGE.
 *
 * Uses the DRMAA java library which relies upon
 *
 * When running on vpicard01,02,03 do
 * > use GridEngine
 * This will set the $DRMAA_LIBRARY_PATH env variable.
 *
 *
 * @param batchSystemName An arbitrary name for this BatchSystem instance.
 * @param sessionId Uniquely identifies this server so status of jobs previously submitted are recovered.
 */
class SgeBatchSystem(batchSystemName : String) extends AbstractBatchSystem(batchSystemName) {
  private val NO_SUITABLE_QUEUES_MESSAGE = "no suitable queues"
  private val SGE_BATCH_SYSTEM_EXCEPTION_MSG = "SGE Batch System exception while submitting job"

  private lazy val lostJobMonitor = { val ljm = new LostJobMonitor(this); ljm.start; ljm}
  protected[sge] val (jobMonitor, session) = {
    val jm = new JobMonitor(this)
    jm.start

    val factory : SessionFactory  = SessionFactory.getFactory()
    val s = factory.getSession()
    var contact: String = null
    var cell: String = null
    val cellEnvVar: String = System.getenv("SGE_CELL")
    if (contactFile.exists) {
      val it = Source.fromFile(contactFile).getLines
      if (it.hasNext) contact = it.next
      if (it.hasNext) cell = it.next
    }
    Log.info("Initializing SGEBatchSystem session")
    if (cellEnvVar == null) Log.warning("SGE_CELL environment variable is not set")
    if (cell != null && cell != cellEnvVar) throw new RuntimeException("SGE_CELL environment variable(" + cellEnvVar +
        ") != value from contact file(" + cell + ")")
    s.init(contact)
    if (contact != null && s.getContact != contact) throw new RuntimeException("SGE Contact string: " + s.getContact + " != " + contact)
    if (contact == null || cell == null) {
      contactFile.getParentFile.mkdir
      val pw = new PrintWriter(contactFile)
      pw.println(s.getContact)
      if (cellEnvVar != null) pw.println(cellEnvVar)
      pw.close
    }
    (jm, s)
  }
  
	/**
	 * Factory method that returns a new BatchJob object which can be configured and
	 * submitted to the batch system.
	 */
  override def newBatchJob(
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
          restartable: Option[Boolean] = true) : BatchJob = {
    super.newBatchJob(workingDirectory, command, args, outputPath, errorPath, emailAddresses, priority, jobName,
      queueName, exclusive, maxRunningTime, memoryReservation, maxMemory, slotReservation, maxSlots, restartable)
    //set defaults
  }


	/**
   * For unit testing, submit a job but do not monitor it.
   * This is desirable when simulating server startup.  As part of test setup, a job is submitted, but because it
   * is not monitored, it will remain in the SGE session so that it can be picked up by the JobMonitor when
   * the server initialization code runs.
   */
	def submitNoMonitor(job: BatchJob): Unit = {
      super.submit(job)

    try {
      val jt = session.createJobTemplate;
      jt.setWorkingDirectory(job.getWorkingDirectory.get) // Set the directory where the job is executed.
      jt.setRemoteCommand(job.getCommand.get) // Set the command string to execute as the job.



      if (job.getArgs != None) {
        val list = new LinkedList[String]
        job.getArgs.get.foreach(a => list.add(a))
        jt.setArgs(list)
      }

      if (job.getOutputPath != None) jt.setOutputPath(":" + job.getOutputPath.get) //Sets how to direct the job's standard output.
      if (job.getErrorPath != None) jt.setErrorPath(":" + job.getErrorPath.get) //Sets how to direct the job's standard error.
      if (job.getOutputPath == None || job.getErrorPath == None) jt.setJoinFiles(true) //Sets whether the error stream should be intermixed with the output stream.

      if (job.getJobName != None) jt.setJobName(job.getJobName.get)


      var nativeSpec = ""
      if (job.isExclusive != None && job.isExclusive.get) {
        nativeSpec += " -l excl=true"
      }

      if (job.getQueueName != None) {
        nativeSpec += " -q " + job.getQueueName.get
      }

      if (job.getMaxRunningTime != None) {
        nativeSpec += " -l h_rt=" + (job.getMaxRunningTime.get * 60) //convert to seconds
      }

      if (job.getMaxMemory != None) {
        nativeSpec += " -l h_vmem=" + job.getMaxMemory.get + "m"
      }

      if (job.getMemoryReservation != None) {
        nativeSpec += " -l virtual_free=" + job.getMemoryReservation.get + "m"
      }

      for ((key, value) <- job.resourceRequirements) {
        nativeSpec += " -l " + key
        if (value != null) nativeSpec += "=" + value

      }

      if (job.getPriority != None) {
        nativeSpec += " -p " + job.getPriority.get
      }

      nativeSpec += " -V"

      if (job.getMaxSlots != None && job.getSlotReservation != None) {
        nativeSpec += " -pe smp_pe " + job.getSlotReservation.get + "-" + job.getMaxSlots.get
      } else if (job.getSlotReservation != None) {
        nativeSpec += " -pe smp_pe " + job.getSlotReservation.get
      } else if (job.getMaxSlots != None) {
        nativeSpec += " -pe smp_pe " + job.getMaxSlots.get
      }

      if (job.getEmailAddresses != None) {
        val hashSet = new HashSet[String]
        job.getEmailAddresses.get.foreach(a => hashSet.add(a))
        jt.setEmail(hashSet) //Set the list of email addresses used to report the job completion and status.
        nativeSpec += " -m e"
      }

      if (job.isRestartable.getOrElse(false)) nativeSpec += " -r y" 

      if (!project.isEmpty) nativeSpec += " -P " + project

      //TODO add "-b y" arg for binary commands? (https://confluence.broadinstitute.org/display/PIC/CentOS+--+SGE+notes)

      if (!nativeSpec.isEmpty()) {
        jt.setNativeSpecification(nativeSpec) //Set an opaque string that is passed by the end user to DRMAA to specify site-specific resources and/or policies.
      }

      Log.debug("Submitting SGE job: " + jobTemplateToString(jt))
      if (job.getOutputPath.isDefined) writeCommandToLog(jt, new File(job.getOutputPath.get))

      val jobId = session.runJob(jt)
      session.deleteJobTemplate(jt)

      if (jobId == null || jobId.isEmpty) {
        throw new Exception("SGE API did not return a jobId for job: " + job);
      }
      else {
        job.setJobId(Some(jobId))
        job.submitTime = nowTimestamp
      }


    /*
     JobTemplate methods that weren't used:
     void	setDeadlineTime(PartialTimestamp deadline) //Sets a deadline after which the DRMS will terminate the job.

     void	setSoftRunDurationLimit(long softRunLimit) //Sets an estimate as to how long the job will need to remain in a running state to complete.
     void	setHardRunDurationLimit(long hardRunLimit)  //Sets how long the job may be in a running state before its limit has been exceeded, and therefore is terminated by the DRMS.

     void	setSoftWallclockTimeLimit(long softWallclockLimit) //Sets an estimate as to how much wall clock time job will need to complete.
     void	setHardWallclockTimeLimit(long hardWallclockLimit) //Sets when the job's wall clock time limit has been exceeded.

     void	setInputPath(java.lang.String inputPath)  //Set the job's standard input path.
     void	setJobCategory(java.lang.String category)  //Set an opaque string specifying how to resolve site-specific resources and/or policies.
     void	setJobEnvironment(java.util.Properties env) //Set the environment values that define the remote environment.

     void	setJobSubmissionState(int state)    //Set the job state at submission.


     void	setStartTime(PartialTimestamp startTime)              //Set the earliest time when the job may be eligible to be run.
     void	setTransferFiles(FileTransferMode mode)                 //Sets how to transfer files between hosts.
    */

    } catch {
      case exe : TryLaterException         => throw new BatchTransientException(SGE_BATCH_SYSTEM_EXCEPTION_MSG, exe)
      case exe : DrmCommunicationException => throw new BatchTransientException(SGE_BATCH_SYSTEM_EXCEPTION_MSG, exe)
      case exe : BatchTransientException   => throw exe;
      case exe : Exception                 => throw new BatchPermanentException(SGE_BATCH_SYSTEM_EXCEPTION_MSG, exe)
    }

  }

  /** Submits a BatchJob to be run in the batch system. */
  override def submit(job : BatchJob) : BatchJob = {

    submitNoMonitor(job)

    jobMonitor.addJob( job )

    job
  }

  private def writeCommandToLog(jt: JobTemplate, outputPath: File) : Unit = {
    val sb = new StringBuilder
    sb.append("SGE command issued: ").append(jt.getRemoteCommand).append(" ")
    jt.getArgs.foreach(a => sb.append(a).append(" "))

    val writer = new BufferedWriter(new FileWriter(outputPath))
    try {
      writer.write(sb.toString)
      writer.newLine
    }
    finally {
      writer.close
    }
  }

  /** JobTemplate.toString is suppose to emit all properties, but it doesn't. */
  private def jobTemplateToString(jt: JobTemplate): String = {
    val sb = new StringBuilder
    sb.append(jt.toString).append("{")
    val props = List(("args", jt.getArgs.toString),
      ("blockEmail", jt.getBlockEmail.toString),
//      ("deadlineTime", jt.getDeadlineTime.toString),
      ("email", safeToString(jt.getEmail)),
      ("errorPath", jt.getErrorPath),
//      ("hardRunDurationLimit", jt.getHardRunDurationLimit.toString),
      ("inputPath", jt.getInputPath),
      ("jobCategory", jt.getJobCategory),
      ("jobEnvironment", safeToString(jt.getJobEnvironment)),
      ("jobName", jt.getJobName),
      ("submissionState", jt.getJobSubmissionState.toString),
      ("joinFiles", jt.getJoinFiles.toString),
      ("nativeSpec", jt.getNativeSpecification),
      ("outputPath", jt.getOutputPath),
      ("remoteCommand", jt.getRemoteCommand),
//      ("softRunDurationLImit", jt.getSoftRunDurationLimit.toString),
      ("startTime", safeToString(jt.getStartTime)),
      ("transferFiles", safeToString(jt.getTransferFiles)),
      ("workingDirectory", jt.getWorkingDirectory))
    for (prop <- props) {
      sb.append(prop._1).append(": ").append(prop._2).append("; ")
    }
    sb.toString
  }

  private def safeToString(obj: AnyRef): String = {
    if (obj == null) "null"
    else obj.toString
  }

	/** Gets the current status of a job. */
  override def getStatus( batchJob : BatchJob ) : JobStatus = {
    var status = super.getStatus(batchJob)

    try {
      val jobId = batchJob.getJobId.get
      val programStatus : Int = session.getJobProgramStatus(jobId)

      status = programStatus match {
        case Session.QUEUED_ACTIVE => JobStatus.PENDING //job is queued and active
        case Session.SYSTEM_ON_HOLD => JobStatus.SYSTEM_SUSPENDED //job is queued and in system hold
        case Session.USER_ON_HOLD => JobStatus.USER_SUSPENDED //job is queued and in user hold
        case Session.USER_SYSTEM_ON_HOLD => JobStatus.SYSTEM_SUSPENDED  //job is queued and in user and system hold
        case Session.RUNNING => JobStatus.RUNNING //job is running
        case Session.SYSTEM_SUSPENDED => JobStatus.SYSTEM_SUSPENDED //job is system suspended
        case Session.USER_SUSPENDED => JobStatus.USER_SUSPENDED  //job is user suspended
        case Session.DONE => JobStatus.ENDED //job finished normally
        case Session.FAILED => JobStatus.ENDED   //job finished, but failed.
        case _ /* Session.UNDETERMINED */ => JobStatus.UNKNOWN //process status cannot be determined
      }
    } catch {
      case e: DrmCommunicationException => {
        // Treat this exception as "try again later"
        Log.warning("Trouble communicating with SGE server. SGE job " + batchJob.getJobId, e)
        status = JobStatus.COMMUNICATION_EXCEPTION
      }
      case e : InvalidJobException => {
        Log.info("SGE lost track of job " + batchJob.getJobId + ", returning LOST_JOB")
        status = JobStatus.LOST_JOB
      }
      case e : NoSuchElementException => {
        Log.internalError("Attempted to get status of malformed job " + batchJob, e);
        status = JobStatus.UNKNOWN;
      }
      case e: Throwable => {
        Log.internalError("unrecognized exception checking status of job.  Setting status to UNKNOWN", e)
        status = JobStatus.UNKNOWN
      }
    }

    return status

  }

  private def resourceToDate(resource: String, map: java.util.Map[_, _]): Option[Date] = {
    if (map == null) None
    else {
      val valueString = map.get(resource)
      if (valueString != null) {
        val seconds = valueString.toString.toDouble
        new Date(seconds.toLong * 1000)
      } else None
    }

  }

  override def jobEnded(batchJob : BatchJob): Unit = {
    try {
      if (getStatus(batchJob) == JobStatus.UNKNOWN || getStatus(batchJob) == JobStatus.LOST_JOB) {
        // Job may not have ended, but at this point UNDETERMINED status has lasted for so long
        // that it is time to give up, or session lost track of job and it cannot be found in accounting file.
        Log.error("SGE lost track of job " + batchJob + "; setting status to UNKNOWN_COMPLETION_STATUS")
        batchJob.setCompletionStatus(JobCompletionStatus.UNKNOWN_COMPLETION_STATUS)
      }
      else {
        val jobInfo = session.wait(batchJob.getJobId.get, 1)

        //copy data from jobInfo to batchJob obj.
        if (jobInfo.wasAborted)
          if (jobInfo.getResourceUsage == null || jobInfo.getResourceUsage.containsKey("exit_status")) {
            //jobInfo.getResourceUsage == null if the job is killed while its still pending
            batchJob.setCompletionStatus(JobCompletionStatus.KILLED_BY_USER)
          } else {
            batchJob.setCompletionStatus(JobCompletionStatus.COULD_NOT_START)
          }

        else if (jobInfo.hasSignaled)
          batchJob.setCompletionStatus(JobCompletionStatus.EXCEEDED_RESOURCE) // TODO: Why is signal assumed to mean this?
        else if (jobInfo.hasExited) {
          val returnCode = jobInfo.getExitStatus
          batchJob.setReturnCode(returnCode)
          if (returnCode != 0)
            batchJob.setCompletionStatus(JobCompletionStatus.FAILED)
          else
            batchJob.setCompletionStatus(JobCompletionStatus.SUCCEEDED)
        } else {
          Log.warning("Job ended called for SGE job in strange state, trying accounting file: " + jobInfoToString(jobInfo))
          handleLostJob(batchJob)
          return ()
        }

        if (jobInfo.getResourceUsage != null) {
          val resourceUsageEntries = jobInfo.getResourceUsage.entrySet.iterator
          while (resourceUsageEntries.hasNext) {
            val e = resourceUsageEntries.next
            batchJob.addResourceUsageStats(e.getKey.toString, e.getValue.toString)
          }
          batchJob.endTime = resourceToDate("end_time", jobInfo.getResourceUsage)
          batchJob.startTime = resourceToDate("start_time", jobInfo.getResourceUsage)
        }

        Log.debug("Ended Job stats: ")
        Log.debug("   resource usage: " + jobInfo.getResourceUsage)
        Log.debug("   return code: " + batchJob.getReturnCode)
        Log.debug("   was aborted: " + jobInfo.wasAborted)
        Log.debug("   has signalled: " + jobInfo.hasSignaled)
        Log.debug("   has exited: " + jobInfo.hasExited)
        if (jobInfo.hasSignaled)
          Log.debug("   signal was: " + jobInfo.getTerminatingSignal)
      }
    }
    catch {
      case ex: Throwable => {
        Log.internalError("Exception getting terminal status for job " + batchJob.getJobId.get, ex)
        batchJob.setCompletionStatus(JobCompletionStatus.UNKNOWN_COMPLETION_STATUS)
      }
    }
    super.jobEnded( batchJob )

  }

  /**
   * Called by LostJobMonitor if looking up in accounting file doesn't work.
   */
  private[sge] def lostJobEnded(batchJob: BatchJob) = {
    batchJob.setCompletionStatus(JobCompletionStatus.UNKNOWN_COMPLETION_STATUS)
    super.jobEnded( batchJob )
  }

  def handleLostJob(batchJob: BatchJob) = {
    lostJobMonitor.addJob(batchJob)
  }

  def jobEndedFromAccounting(batchJob: BatchJob, accountingLine: AccountingLine) = {
    copyAccountingLineToBatchJob(batchJob, accountingLine)
    super.jobEnded( batchJob )
  }

  private def copyAccountingLineToBatchJob(batchJob: BatchJob, accountingLine: AccountingLine) = {
    if (accountingLine.exit_status >= AccountingLine.SIGNAL_OFFSET)
      batchJob.setCompletionStatus(JobCompletionStatus.EXCEEDED_RESOURCE) // TODO: Why is signal assumed to mean this?
    else {
      batchJob.setReturnCode(accountingLine.exit_status)
      batchJob.setCompletionStatus(
        if (accountingLine.failed != "0") JobCompletionStatus.COULD_NOT_START
        else if (accountingLine.exit_status != 0) JobCompletionStatus.FAILED else JobCompletionStatus.SUCCEEDED)
      // 0 start and end time implies job was never launched, e.g. because of unsatisfiable resource request.
      if (accountingLine.startTime.getTime != 0) batchJob.startTime = accountingLine.startTime
      if (accountingLine.endTime.getTime != 0) batchJob.endTime = accountingLine.endTime
    }
    // Mimic as much of resource usage map as possible
    // acct_cpu, acct_io, acct_iow, acct_maxvmem, acct_mem, vmem do not appear to be available
    batchJob.addResourceUsageStats("cpu", accountingLine.cpu)
    batchJob.addResourceUsageStats("end_time", accountingLine.endTime.getTime / 1000.0)
    batchJob.addResourceUsageStats("exit_status", accountingLine.exit_status)
    batchJob.addResourceUsageStats("io", accountingLine.io)
    batchJob.addResourceUsageStats("iow", accountingLine.iow)
    batchJob.addResourceUsageStats("maxvmem", accountingLine.maxvmem)
    batchJob.addResourceUsageStats("mem", accountingLine.mem)
    batchJob.addResourceUsageStats("priority", accountingLine.priority)
    batchJob.addResourceUsageStats("ru_idrss", accountingLine.ru_idrss)
    batchJob.addResourceUsageStats("ru_inblock", accountingLine.ru_inblock)
    batchJob.addResourceUsageStats("ru_ismrss", accountingLine.ru_ismrss)
    batchJob.addResourceUsageStats("ru_isrss", accountingLine.ru_isrss)
    batchJob.addResourceUsageStats("ru_ixrss", accountingLine.ru_ixrss)
    batchJob.addResourceUsageStats("ru_majflt", accountingLine.ru_majflt)
    batchJob.addResourceUsageStats("ru_maxrss", accountingLine.ru_maxrss)
    batchJob.addResourceUsageStats("ru_minflt", accountingLine.ru_minflt)
    batchJob.addResourceUsageStats("ru_msgrcv", accountingLine.ru_msgrcv)
    batchJob.addResourceUsageStats("ru_msgsnd", accountingLine.ru_msgsnd)
    batchJob.addResourceUsageStats("ru_nivcsw", accountingLine.ru_nivcsw)
    batchJob.addResourceUsageStats("ru_nsignals", accountingLine.ru_nsignals)
    batchJob.addResourceUsageStats("ru_nswap", accountingLine.ru_nswap)
    batchJob.addResourceUsageStats("ru_nvcsw", accountingLine.ru_nvcsw)
    batchJob.addResourceUsageStats("ru_oublock", accountingLine.ru_oublock)
    batchJob.addResourceUsageStats("ru_stime", accountingLine.ru_stime)
    batchJob.addResourceUsageStats("ru_utime", accountingLine.ru_utime)
    batchJob.addResourceUsageStats("ru_wallclock", accountingLine.ru_wallclock)
    batchJob.addResourceUsageStats("signal", accountingLine.signal)
    batchJob.addResourceUsageStats("startTime", accountingLine.startTime.getTime / 1000.0)
    batchJob.addResourceUsageStats("submissionTime", accountingLine.submissionTime.getTime / 1000.0)
    batchJob.addResourceUsageStats("failed", accountingLine.failed)
  }


  override def kill( batchJob : BatchJob ): Boolean = {
    super.kill(batchJob)
    Log.info("SGEBatchSystem.kill(" + batchJob +") called.")

    try {
      session.control(batchJob.getJobId.get, Session.TERMINATE)
      true
    }
    catch {
      case ex: InvalidJobException => {
        Log.warning("InvalidJobException trying to kill " + batchJob + ".  Probably the job has already ended.")
        false
      }
    }
  }


  def shutDown = {

    Log.info("SGEBatchSystem.shutDown() called.")

    jobMonitor.stop
    lostJobMonitor.stop

    if(session != null) {
      session.exit() //clean up the SGE session.
    }
    Log.info("SGEBatchSystem shut down.")
  }

  /**
   * Tells the batch system to track the the given BatchJobs.
   */
  override def restore( batchJobs : BatchJob* )
  {
    super.restore(batchJobs: _*)

    for(job <- batchJobs) {
      jobMonitor.addJob(job)
    }
  }


  def isMonitoringJobs = jobMonitor.isMonitoringJobs
  def numJobsMonitoring = jobMonitor.numJobsMonitoring
  def numLostJobsMonitoring = lostJobMonitor.numJobsMonitoring
}


