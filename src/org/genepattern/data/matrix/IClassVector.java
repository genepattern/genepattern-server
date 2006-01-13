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

package org.genepattern.data.matrix;

/**
 * An interface for nominal class information
 * 
 * @author Joshua Gould
 */
public interface IClassVector {
	/**
	 * Gets the class assignment
	 * 
	 * @param index
	 *            The index
	 * @return The assignment
	 */
	public int getAssignment(int index);

	/**
	 * Gets the class name for the specified assignment
	 * 
	 * @param assignment
	 *            The assignment
	 * @return The class name.
	 */
	public String getClassName(int assignment);

	/**
	 * Gets the number of different possible values taken by the class
	 * assignments. Note that this can be greater than the actual number of
	 * classes contained in this class vector.
	 * 
	 * @return The number of classes.
	 */
	public int getClassCount();

	/**
	 * Gets the number of assignments in this class vector
	 * 
	 * @return the number of assignments
	 */
	public int size();
}
