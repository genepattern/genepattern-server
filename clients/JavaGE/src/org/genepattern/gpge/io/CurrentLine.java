/*
 * CurrentLine.java
 *
 * Created on August 21, 2002, 10:53 PM
 */

package org.genepattern.gpge.io;

/**
 * just a wrapper for a String
 * 
 * @author kohm
 */
public class CurrentLine {

	/** Creates a new instance of CurrentLine */
	public CurrentLine() {
	}

	// fields
	/** the current line as read */
	public String line = null;

	/** the line number */
	public int num = -1;

	/** the total number of lines including skipped ones */
	public int total_num = -1;
}