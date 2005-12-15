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
