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


import java.io.File
import AccountingLine._
import java.util.Date

object AccountingLine {
  def toLong(s: String) = java.lang.Long.valueOf(s).asInstanceOf[Long]
  def toInt(s: String) = java.lang.Integer.valueOf(s).asInstanceOf[Int]
  def toDouble(s: String) = java.lang.Double.valueOf(s).asInstanceOf[Double]
  def toDate(s: String) = new Date(java.lang.Long.valueOf(s).asInstanceOf[Long] * 1000)

  // If job was terminated by signal, exit_status == signal number + SIGNAL_OFFSET
  val SIGNAL_OFFSET = 128
}



class AccountingLine(line: String, file: File) {
  val fields = line.split(":")
  // Make everything lazy so that parsing only happens if necessary.  If performance still is bad, the tuple assignments
  // could be broken up so that each is individually lazy.
  lazy val jobId = fields(5)
  lazy val (qname, hostname, group, owner, jobName, account) =
    (fields(0), fields(1), fields(2), fields(3), fields(4), fields(6))
  lazy val priority = toInt(fields(7))
  lazy val submissionTime = toDate(fields(8))
  lazy val startTime = toDate(fields(9))
  lazy val endTime = toDate(fields(10))

  val failed = fields(11)
  lazy val exit_status = toInt(fields(12))
  lazy val ru_wallclock = toLong(fields(13))

  lazy val (ru_utime, ru_stime) = (toDouble(fields(14)), toDouble(fields(15)))

  lazy val ru_maxrss = toDouble(fields(16)) // I don't know why this isn't a long

  lazy val (ru_ixrss, ru_ismrss, ru_idrss, ru_isrss, ru_minflt, ru_majflt, ru_nswap) =
    (toLong(fields(17)), toLong(fields(18)), toLong(fields(19)), toLong(fields(20)),toLong(fields(21)), toLong(fields(22)), toLong(fields(23)))
  lazy val ru_inblock = toDouble(fields(24)) // I don't know why this isn't a long
  lazy val (ru_oublock,ru_msgsnd, ru_msgrcv, ru_nsignals, ru_nvcsw, ru_nivcsw) =
    (toLong(fields(25)), toLong(fields(26)), toLong(fields(27)), toLong(fields(28)), toLong(fields(29)), toLong(fields(30)))

  lazy val (project, department, granted_pe) = (fields(31), fields(32), fields(33))

  lazy val (slots, task_number) = (toLong(fields(34)), toLong(fields(35)))

  lazy val (cpu, mem, io) = (toDouble(fields(36)), toDouble(fields(37)), toDouble(fields(38)))

  lazy val category = fields(39)
  lazy val iow = toDouble(fields(40))
  lazy val pe_taskid = fields(41)
  lazy val maxvmem = toDouble(fields(42)) // Again, who knows why this isn't a long
  lazy val arid = fields(43)
  lazy val ar_submission_time = toLong(fields(44))

  def signal = if (exit_status >= AccountingLine.SIGNAL_OFFSET) exit_status - AccountingLine.SIGNAL_OFFSET else 0

  if (fields.length != 45)
    throw new RuntimeException("Wrong number of fields (" + fields.length + ") in SGE accounting file" + file.getAbsolutePath)
}



