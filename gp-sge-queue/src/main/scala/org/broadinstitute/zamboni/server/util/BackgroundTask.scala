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

package org.broadinstitute.zamboni.server.util

import log.Log
import java.util.concurrent.atomic.AtomicBoolean
import java.lang.{Math, Throwable}
import BackgroundTask._

object BackgroundTask {
  // Sleep no more than 5 mins after exception regardless of how many consecutive exceptions.
  val MAX_SLEEP_AFTER_EXCEPTION_SEC = 60 * 5
}
/**
 * Abstract base class for a background task that should run periodically.  If it fails with an unhandled exception,
 * another thread is created and started.  Provides the ability to terminate gracefully, but if that fails,
 * attempts to forcefully stop the thread.
 */
abstract class BackgroundTask(val shutdownTimeoutSeconds: Int = 5,
                              val loopSleepSeconds: Int = 0,
                              /** This value added to Thread.NORM_PRIORITY */
                              val priorityOffset: Int = 0) extends Runnable {
  def loopBody: Unit
  val name = getClass.getSimpleName

  protected val stopFlag = new AtomicBoolean(false)

  protected var thread: Option[Thread] = None

  private val identityHashCode = System.identityHashCode(this)

  // Count number of consecutive times loopBody did not return properly, in order to throttle Log.internalError
  private var numConsecutiveFailures = 0L

  private def fullIdentifier: String = {
    List(name, identityHashCode.toString, thread.get.getId.toString).mkString("(", ", ", ")")
  }

  def run: Unit = {
    try {
      Log.info("Starting thread " + fullIdentifier)
      if (Thread.currentThread != thread.get)
        throw new IllegalStateException(Thread.currentThread + " != " + thread.get)
      while (!shouldStop) {
        try {
          loopBody
          numConsecutiveFailures = 0
          if (loopSleepSeconds > 0) Thread.sleep(loopSleepSeconds * 1000)
        }
        catch {
          case ex: InterruptedException =>
            Log.info("Caught InterruptedException in " + name + " loop -- rethrowing.")
            throw ex
          case ex: Throwable => {
            Log.internalError("Exception in " + name + ".  Restarting... ", ex)
            numConsecutiveFailures += 1
            sleepAfterException
          }
        }
      }
      Log.info("Stopping thread " + thread.get.getName)
    }
    catch {
      case ex: InterruptedException => Log.info("Caught InterruptedException in " + name + " outside of loop.")
    }
    Log.info("Thread " + fullIdentifier + " is ending.")
  }

  protected def shouldStop: Boolean = {
    stopFlag.get || Thread.interrupted
  }

  def start = {
    if (thread.isDefined && thread.get.isAlive) throw new IllegalStateException(thread.get.getName + " is already running")
    thread = Some(new Thread(this, name))
    thread.get.setDaemon(true)
    thread.get.setPriority(Thread.NORM_PRIORITY + priorityOffset)
    thread.get.setUncaughtExceptionHandler(new UncaughtExceptionHandler)
    thread.get.start
  }

  def stop = {
    if (thread.isEmpty) Log.info("Thread " + name + " is not running, so will not be stopped.")
    else {
      stopFlag.set(true)
      // Add 1 nanosec to ensure non-infinite wait
      thread.get.join(shutdownTimeoutSeconds * 1000, 1)
      if (thread.get.isAlive) {
        Log.warning("Thread " + name + " could not be shut down gracefully -- interrupting.")
        thread.get.interrupt
        // Add 1 nanosec to ensure non-infinite wait
        thread.get.join(shutdownTimeoutSeconds * 1000 , 1)
        if (thread.get.isAlive)
          Log.warning("Thread " + name + " still alive after interrupt.  Giving up trying to kill it.")
      }
    }
  }

  /**
   * If many consecutive exceptions, sleep for an increasing duration in order to avoid swamping mail server.
   */
  private def sleepAfterException = {
    if (numConsecutiveFailures > 1) {
      val sleepSec = Math.min(MAX_SLEEP_AFTER_EXCEPTION_SEC, Math.pow(2, numConsecutiveFailures - 1)).toLong
      Log.error(name + " had " + numConsecutiveFailures + " consecutive exceptions; sleeping " + sleepSec + " seconds.")
      Thread.sleep(sleepSec * 1000L)
    }
  }

  def isAlive = thread.isDefined && thread.get.isAlive

  private class UncaughtExceptionHandler extends Thread.UncaughtExceptionHandler {
    def uncaughtException(t: Thread, ex: Throwable): Unit = {
      try {
        Log.internalError(t.getName + " had an uncaught exception", ex)
      }
      catch {
        case ex: Throwable => ex.printStackTrace
      }
      if (thread.get ne t) throw new IllegalStateException("unpossible")
      thread = None
      numConsecutiveFailures += 1
      sleepAfterException
      start
    }
  }
}