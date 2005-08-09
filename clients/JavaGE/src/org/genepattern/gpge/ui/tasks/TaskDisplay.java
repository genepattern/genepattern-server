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
	 * Sets the value of the given parameter to the given node
	 * 
	 * @param parameterName
	 *            the parameter name as returned by getInputFileParameters
	 * @param node
	 *            a tree node
	 */
	public void setInputFile(String parameterName,
			javax.swing.tree.TreeNode node);

}
