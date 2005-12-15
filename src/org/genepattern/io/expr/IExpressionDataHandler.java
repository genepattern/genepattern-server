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

import org.genepattern.io.ParseException;

/**
 * An interface for receiving notification of the content of a expression data
 * document
 * 
 * @author Joshua Gould
 */
public interface IExpressionDataHandler {

	public void init(int rows, int cols, boolean hasRowDescriptions,
			boolean hasColumnDescriptions, boolean hasCalls)
			throws ParseException;

	public void data(int i, int j, double d) throws ParseException;

	public void call(int i, int j, int call) throws ParseException;

	public void columnName(int j, String name) throws ParseException;

	public void rowName(int i, String name) throws ParseException;

	public void rowDescription(int i, String desc) throws ParseException;

	public void columnDescription(int j, String desc) throws ParseException;
}