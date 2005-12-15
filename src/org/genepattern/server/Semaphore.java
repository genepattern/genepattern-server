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


/*
 * Semaphore.java
 *
 * Created on January 30, 2003, 3:29 PM
 */

package org.genepattern.server;

/**
 * Semaphore.java
 * 
 * @author rajesh kuttan
 * @version
 */

public class Semaphore {

	private int count;

	/** Creates new Semaphore */
	public Semaphore(int count) {
		this.count = count;
	}

	// Returns true if locking condition is true
	public synchronized boolean isLocked() {
		return count <= 0;
	}

	public synchronized void acquire() {
		try {
			while (count <= 0) {
				wait();
			}

			count--;
		} catch (InterruptedException e) {
			//do nothing
		}
	}

	public synchronized void release() {
		++count;
		notify();
	}

}