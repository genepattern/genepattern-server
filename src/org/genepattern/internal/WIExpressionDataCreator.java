package org.genepattern.internal;

import org.genepattern.io.ParseException;
import org.genepattern.io.expr.IExpressionDataCreator;


/**
 *  Creates an org.genepattern.data.Dataset which is used in KNN,
 *  WeightedVoting, and other modules.
 *
 * @author    Joshua Gould
 */
public class WIExpressionDataCreator
		 implements IExpressionDataCreator {

	int rows;
	int columns;
	String[] rowDescriptions;
	String[] columnDescriptions;
	String[] rowNames;
	String[] columnNames;
	float[] data;


	public void data(int row, int column, double d)
			 throws ParseException {
		data[row * columns + column] = (float) d;
	}


	public Object create() {
		return WIUtil.createWIDataset(rows, columns, rowNames,
				rowDescriptions,
				columnNames,
				columnDescriptions,
				data);
	}


	public void columnName(int j, String s)
			 throws ParseException {
		columnNames[j] = s;
	}


	public void call(int i, int j, int c)
			 throws ParseException { }


	public void rowName(int i, String s)
			 throws ParseException {
		rowNames[i] = s;
	}


	public void rowDescription(int i, String s)
			 throws ParseException {
		rowDescriptions[i] = s;
	}


	public void columnDescription(int j, String s)
			 throws ParseException {
		columnDescriptions[j] = s;
	}


	public void init(int rows, int columns, boolean hasRowDesc,
			boolean hasColDesc, boolean hasCalls)
			 throws ParseException {
		this.rows = rows;
		this.columns = columns;
		data = new float[rows * columns];
		rowNames = new String[rows];
		columnNames = new String[columns];

		if(hasRowDesc) {
			rowDescriptions = new String[rows];
		}

		if(hasColDesc) {
			columnDescriptions = new String[columns];
		}
	}
}
