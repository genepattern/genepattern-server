/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/


package org.genepattern.internal;

/**
 * A stop watch
 * 
 * @author Joshua Gould
 */
public class StopWatch {
	long start;

	/** Starts the watch */
	public void start() {
		start = System.currentTimeMillis();
	}

	/**
	 * Returns the elapsed time in seconds since start was last invoked
	 * 
	 * @return The elapsed time
	 */
	public double stop() {
		long end = System.currentTimeMillis();
		long elapsed = end - start;
		return elapsed / 1000.0;
	}
}