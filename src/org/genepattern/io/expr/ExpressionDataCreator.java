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

import org.genepattern.data.expr.ExpressionData;
import org.genepattern.data.expr.ResExpressionData;
import org.genepattern.data.matrix.DoubleMatrix2D;
import org.genepattern.data.matrix.IntMatrix2D;
import org.genepattern.io.ParseException;

/**
 *  Expression data creator that creates an instance of
 *  org.genepattern.data.expr.ExpressionData
 *
 *@author    Joshua Gould
 */
public class ExpressionDataCreator implements IExpressionDataCreator {
	protected IntMatrix2D calls;

	protected double[][] data;

	protected String[] rowDescriptions;

	protected String[] rowNames;

	protected String[] columnNames;

	protected String[] columnDescriptions;

	protected boolean keepRowDescriptions = true;

	protected boolean keepColumnDescriptions = true;

	public ExpressionDataCreator() {
		this(true, true);
	}

	/**
	 *  Creates a new <tt>ExpressionDataCreator</tt> instance
	 *
	 *@param  keepRowDescriptions     whether the ExpressionData returned from <tt>
	 *      create</tt> should include row descriptions
	 *@param  keepColumnDescriptions  whether the ExpressionData returned from <tt>
	 *      create</tt> should include column descriptions
	 */
	public ExpressionDataCreator(boolean keepRowDescriptions,
			boolean keepColumnDescriptions) {
		this.keepRowDescriptions = keepRowDescriptions;
		this.keepColumnDescriptions = keepColumnDescriptions;
	}

	public Object create() {
		DoubleMatrix2D matrix = new DoubleMatrix2D(data, rowNames, columnNames);
		if (calls != null) {
			return new ResExpressionData(matrix, calls, rowDescriptions,
					columnDescriptions);
		} else {
			return new ExpressionData(matrix, rowDescriptions,
					columnDescriptions);
		}
	}

	public void call(int row, int column, int call) throws ParseException {
		calls.set(row, column, call);
	}

	public void data(int row, int column, double d) throws ParseException {
		data[row][column] = d;
	}

	public void columnName(int j, String s) throws ParseException {
		columnNames[j] = s;
	}

	public void rowName(int i, String s) throws ParseException {
		rowNames[i] = s;
	}

	public void rowDescription(int i, String s) throws ParseException {
		if (keepRowDescriptions) {
			rowDescriptions[i] = s;
		}
	}

	public void columnDescription(int j, String s) throws ParseException {
		if (keepColumnDescriptions) {
			columnDescriptions[j] = s;
		}
	}

	public void init(int rows, int columns, boolean hasRowDesc,
			boolean hasColDesc, boolean hasCalls) throws ParseException {
		data = new double[rows][columns];
		rowNames = new String[rows];
		columnNames = new String[columns];
		if (hasRowDesc && keepRowDescriptions) {
			rowDescriptions = new String[rows];
		}
		if (hasColDesc && keepColumnDescriptions) {
			columnDescriptions = new String[columns];
		}
		if (hasCalls) {
			calls = new IntMatrix2D(rows, columns);
		}
	}
}
