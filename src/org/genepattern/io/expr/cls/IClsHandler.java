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


package org.genepattern.io.expr.cls;

/**
 * An interface for receiving notification of the content of a cls document
 * 
 * @author Joshua Gould
 */
public interface IClsHandler {
	/**
	 * 
	 * @param x
	 *            the array containing the class name for each sample
	 */
	public void assignments(String[] x);

	/**
	 * 
	 * @param classes
	 *            the array containing all the class names contained in the cls
	 *            document
	 */
	public void classes(String[] classes);
}