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

import java.io.{RandomAccessFile, File}
import collection.Iterator
import java.util.Date
import AccountingReader._

object AccountingReader {
  val MaxLineLengthGuess = 600 // Longest I saw in production was 475
  val CloseEnoughBytes = 10 * MaxLineLengthGuess


  // If looking for a job with a particular submission date, start iterating from the place in the file where
  // there is a job with end date <= submissionDate - dateWiggleMsec, because the lines in the file are not
  // completely ordered so that end date of line N is always < submission date of line N+1, but they are close to
  // being ordered that way.
  val DateWiggleMsec = 12L * 60 * 60 * 1000

  // When doing binary search, assume more likely to find the desired location later in the file.
  val BinSearchMid = 3.0/4

  def latestTime(accountingLine: AccountingLine): Long = {
    if (accountingLine.endTime.getTime == 0) accountingLine.submissionTime.getTime
    else accountingLine.endTime.getTime
  }
}

class AccountingReader(val file: File, val submissionDate: Date = new Date(0)) extends Iterator[AccountingLine] {
  val reader = new RandomAccessFile(file, "r")
  private var eof = false

  var start = 0L
  if (submissionDate.getTime != 0) {
    // Seek to appropriate location
    var end = reader.length
    while (end - start > CloseEnoughBytes) {
      if (end < start) throw new IllegalStateException
      var middle: Long = ((end - start) * BinSearchMid + start).asInstanceOf[Long]
      reader.seek(middle)
      reader.readLine // Skip to next line in case landed in middle of line
      middle = reader.getFilePointer
      val accountingLine = advance
      if (accountingLine.isEmpty) end = middle
      // If job was never launched, endTime = 0, so in that case look at submissionTime
      else if (latestTime(accountingLine.get) < submissionDate.getTime - DateWiggleMsec) start = middle
      else end = middle
    }
  }
  reader.seek(start)

  private var currentLine = advance

  /** Skip header lines, close file at EOF */
  private def readLine: String = {
    if (eof) null
    else {
      val line = reader.readLine
      if (line == null) {
        eof = true
        reader.close
        null
      } else if (!line.startsWith("#")) line
      else readLine // tail recursion is optimized out
    }
  }

  private def advance: Option[AccountingLine] = {
    if (eof) None
    else {
      val line = readLine
      if (line == null) {
        None
      } else Some(new AccountingLine(line, file))
    }
  }

  def next(): AccountingLine = {
    val ret = currentLine.get
    currentLine = advance
    ret
  }

  def hasNext: Boolean = {
    currentLine.isDefined
  }
}


