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



object JobStatus extends Enumeration {
  type JobStatus = Value
  val PENDING, RUNNING, UNKNOWN, USER_SUSPENDED, SYSTEM_SUSPENDED, ENDED,
  /** Trouble talking to SGE server.  Assume job is still running. */
  COMMUNICATION_EXCEPTION,
  /** Exception thrown checking status.  Presume job cannot be found. */
  LOST_JOB = Value

  def isTerminal(jobStatus: JobStatus) = {
    jobStatus == ENDED || jobStatus == LOST_JOB
  }
}


object JobCompletionStatus extends Enumeration {
  type JobCompletionStatus = Value
  val COULD_NOT_START, //couldn't run the command (eg. executable file not found)
  EXCEEDED_RESOURCE,   //killed by the system
  KILLED_BY_USER,
  FAILED,              //finished with non-zero return code
  SUCCEEDED,           //finished and returned 0
  UNKNOWN_COMPLETION_STATUS = //Batch system lost track of job
        Value
}





