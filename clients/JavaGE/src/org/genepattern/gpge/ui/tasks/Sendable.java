package org.genepattern.gpge.ui.tasks;

/**
 * The interface that objects must implement to enable "Send To" behavior
 * 
 * @author Joshua Gould
 * 
 */
public interface Sendable {

	/**
	 * Gets the text that will be displayed to the user after the "Send To" has
	 * completed
	 * 
	 * @return the UI string
	 */
	public String toUIString();
}
