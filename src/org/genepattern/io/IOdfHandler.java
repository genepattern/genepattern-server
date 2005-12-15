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


package org.genepattern.io;

/**
 * An interface for receiving notification of the content of an odf document
 * 
 * @author Joshua Gould
 */
public interface IOdfHandler {

	public void endHeader() throws ParseException;

	public void header(String key, String[] values) throws ParseException;

	public void header(String key, String value) throws ParseException;

	public void data(int row, int column, String s) throws ParseException;
}

