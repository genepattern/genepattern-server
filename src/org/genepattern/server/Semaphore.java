/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


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
