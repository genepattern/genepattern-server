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