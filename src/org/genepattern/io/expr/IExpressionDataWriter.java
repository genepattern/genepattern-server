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


package org.genepattern.io.expr;

import java.io.IOException;

import org.genepattern.data.expr.IExpressionData;

/**
 * Interface for expression data writers.
 * 
 * @author Joshua Gould
 */
public interface IExpressionDataWriter {

	/**
	 * Writes an <code>IExpressionData</code> instance to a stream.
	 * 
	 * @param data
	 *            the data
	 * @param os
	 *            the output stream
	 * @exception IOException
	 *                if an I/O error occurs during writing.
	 */
	public void write(IExpressionData data, java.io.OutputStream os)
			throws java.io.IOException;

	/**
	 * Returns a String identifying the format that this writer encodes.
	 * 
	 * @return the format name, as a String.
	 */
	public String getFormatName();

	/**
	 * Appends the correct file extension to the pathname if it does not exist.
	 * 
	 * @param pathname
	 *            a pathname string
	 * @return The corrected pathname
	 */
	public String checkFileExtension(String pathname);

}

