package org.genepattern.gpge.ui.tasks;
/**
 * The interface for objects that can display a task
 * @author Joshua Gould
 *
 */
public interface TaskDisplay {

	/**
	 * Gets an iterator of the input file parameters
	 * 
	 * @return the input file parameters
	 */
	public java.util.Iterator getInputFileParameters();
	
	/**
	 *  Sets the value of the given parameter to the given node
	 *
	 * @param  parameterName  the parameter name
	 * @param  node           a tree node
	 */
	public void setInputFile(String parameterName,
			javax.swing.tree.TreeNode node);

}
