package org.genepattern.io.expr;
import org.genepattern.data.expr.*;
import org.genepattern.data.matrix.*;
import org.genepattern.io.*;

/**
 *  Expression data creator that creates an instance of
 *  edu.mit.broad.data.expr.ExpressionData
 *
 * @author    Joshua Gould
 */
public class ExpressionDataCreator implements IExpressionDataCreator {
	IntMatrix2D calls;
	DoubleMatrix2D data;
	String[] rowDescriptions;
	String[] columnDescriptions;



	public Object create() {
		if(calls != null) {
			return new ResExpressionData(data, calls, rowDescriptions, columnDescriptions);
		} else {
			return new ExpressionData(data, rowDescriptions, columnDescriptions);
		}
	}


	public void call(int row, int column, int call) throws ParseException {
		calls.set(row, column, call);
	}



	public void data(int row, int column, double d) throws ParseException {
		data.set(row, column, d);
	}


	public void columnName(int j, String s) throws ParseException {
		data.setColumnName(j, s);
	}



	public void rowName(int i, String s) throws ParseException {
		data.setRowName(i, s);
	}


	public void rowDescription(int i, String s) throws ParseException {
		rowDescriptions[i] = s;
	}


	public void columnDescription(int j, String s) throws ParseException {
		columnDescriptions[j] = s;
	}


	public void init(int rows, int columns, boolean hasRowDesc, boolean hasColDesc, boolean hasCalls) throws ParseException {
		data = new DoubleMatrix2D(rows, columns);
		if(hasRowDesc) {
			rowDescriptions = new String[rows];
		}
		if(hasColDesc) {
			columnDescriptions = new String[columns];
		}
		if(hasCalls) {
			calls = new IntMatrix2D(rows, columns);
		}
	}
}
