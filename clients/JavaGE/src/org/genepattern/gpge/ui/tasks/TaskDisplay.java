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


package org.genepattern.gpge.ui.tasks;

import java.util.Iterator;

/**
 * The interface for objects that can display a task
 * 
 * @author Joshua Gould
 */
public interface TaskDisplay {

	/**
	 * Gets an iterator of the input file parameters names as <tt>String</tt>
	 * instances. The parameter is formatted for display in a user interface
	 * 
	 * @return the input file parameters
	 */
	public Iterator getInputFileParameters();

	/**
	 * Gets an iterator of the input file types names as <tt>String[]</tt>
	 * instances. The iterator corresponds to elements in
	 * getInputFileParameters. If an input file parameter does not have any type
	 * information defined, then the corresponding element in the returned
	 * element will be an empty String array
	 * 
	 * @return the input file types
	 */
	public Iterator getInputFileTypes();

	/**
	 * Sets the value of the given parameter to the given value
	 * 
	 * @param parameterName
	 *            the parameter name as returned by getInputFileParameters
	 * @param sendable a <tt>Sendable</tt> object
	 */
	public void sendTo(String parameterName, Sendable sendable);

}
