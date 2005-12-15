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


package org.genepattern.data.expr;

import org.genepattern.data.matrix.DoubleMatrix2D;

/**
 * Implementation of IExpressionData interface
 * 
 * @author Joshua Gould
 */
public class ExpressionData implements IExpressionData {
	protected DoubleMatrix2D dataset;

	protected String[] rowDescriptions;

	protected String[] columnDescriptions;

	public ExpressionData(DoubleMatrix2D dataset, String[] _rowDescriptions,
			String[] _columnDescriptions) {
		this.dataset = dataset;
		if (_rowDescriptions != null) {
			if (_rowDescriptions.length != dataset.getRowCount()) {
				throw new IllegalArgumentException(
						"Length of row descriptions not equal to number of rows in matrix.");
			}
			this.rowDescriptions = _rowDescriptions;
		}

		if (_columnDescriptions != null) {
			if (_columnDescriptions.length != dataset.getColumnCount()) {
				throw new IllegalArgumentException(
						"Length of column descriptions not equal to number of columns in matrix.");
			}
			this.columnDescriptions = _columnDescriptions;
		}
	}

	public ExpressionData slice(String[] rowNames, String[] columnNames) {
		int[] rowIndices = rowNames != null ? new int[rowNames.length] : null;
		int[] columnIndices = columnNames != null ? new int[columnNames.length]
				: null;
		if (rowIndices != null) {
			for (int i = 0, rows = rowIndices.length; i < rows; i++) {
				int index = dataset.getRowIndex(rowNames[i]);
				if (index == -1) {
					throw new IllegalArgumentException("Row name "
							+ rowNames[i] + " not found.");
				}
				rowIndices[i] = index;
			}
		}

		if (columnIndices != null) {
			for (int i = 0, cols = columnIndices.length; i < cols; i++) {
				int index = dataset.getColumnIndex(columnNames[i]);
				if (index == -1) {
					throw new IllegalArgumentException("Column name "
							+ columnNames[i] + " not found.");
				}
				columnIndices[i] = index;
			}
		}
		return slice(rowIndices, columnIndices);
	}

	public ExpressionData slice(int[] rowIndices, int[] columnIndices) {
		if (rowIndices == null) {
			rowIndices = new int[dataset.getRowCount()];
			for (int i = dataset.getRowCount(); --i >= 0;) {
				rowIndices[i] = i;
			}
		}
		if (columnIndices == null) {
			columnIndices = new int[dataset.getColumnCount()];
			for (int i = dataset.getColumnCount(); --i >= 0;) {
				columnIndices[i] = i;
			}
		}

		DoubleMatrix2D newDoubleMatrix2D = dataset.slice(rowIndices,
				columnIndices);
		String[] newRowAnnotations = null;

		if (rowDescriptions != null) {
			newRowAnnotations = new String[rowIndices.length];
			for (int i = 0, length = rowIndices.length; i < length; i++) {
				newRowAnnotations[i] = rowDescriptions[rowIndices[i]];
			}
		}

		String[] newColumnAnnotations = null;

		if (columnDescriptions != null) {
			newColumnAnnotations = new String[columnIndices.length];
			for (int j = 0, length = columnIndices.length; j < length; j++) {
				newColumnAnnotations[j] = columnDescriptions[columnIndices[j]];
			}
		}

		return new ExpressionData(newDoubleMatrix2D, newRowAnnotations,
				newColumnAnnotations);
	}

	public void setRowDescription(int row, String description) {
		rowDescriptions[row] = description;
	}

	public void setColumnDescription(int column, String description) {
		columnDescriptions[column] = description;
	}

	public void setColumnName(int column, String name) {
		dataset.setColumnName(column, name);
	}

	public void setRowName(int row, String name) {
		dataset.setRowName(row, name);
	}

	public String getColumnDescription(int column) {
		if (columnDescriptions != null) {
			return columnDescriptions[column];
		}
		return null;
	}

	public String getRowDescription(int row) {
		if (rowDescriptions != null) {
			return rowDescriptions[row];
		}
		return null;
	}

	public int getRowCount() {
		return dataset.getRowCount();
	}

	public int getColumnCount() {
		return dataset.getColumnCount();
	}

	public double getValue(int row, int column) {
		return dataset.get(row, column);
	}

	public String getValueAsString(int row, int column) {
		return String.valueOf(dataset.get(row, column));
	}

	public String getColumnName(int column) {
		return dataset.getColumnName(column);
	}

	public String getRowName(int row) {
		return dataset.getRowName(row);
	}

	/**
	 * Allocates a new array contains the row names
	 * 
	 * @return The row names.
	 */
	public String[] getRowNames() {
		return dataset.getRowNames();
	}

	/**
	 * Gets the array containing the row descriptions or <tt>null</tt> if no
	 * row descriptions are set
	 * 
	 * @return The row descriptions.
	 */
	public String[] getRowDescriptions() {
		return rowDescriptions;
	}

	/**
	 * Sets the row descriptions
	 * 
	 * @param descs
	 * @throws IllegalArgumentException
	 *             if desc.length != getRowCount()
	 */
	public void setRowDescriptions(String[] descs) {
		if (descs != null && descs.length != getRowCount()) {
			throw new IllegalArgumentException(
					"Length of descriptions must be equal to the number of rows.");
		}
		this.rowDescriptions = descs;
	}

	/**
	 * Allocates a new array contains the column names
	 * 
	 * @return The column names.
	 */
	public String[] getColumnNames() {
		return dataset.getColumnNames();
	}

	/**
	 * Gets the array containing the column descriptions or <tt>null</tt> if
	 * no column descriptions are set
	 * 
	 * @return The column descriptions.
	 */
	public String[] getColumnDescriptions() {
		return columnDescriptions;
	}

	/**
	 * Sets the column descriptions
	 * 
	 * @param descs
	 * @throws IllegalArgumentException
	 *             if desc.length != getColumnCount()
	 */
	public void setColumnDescriptions(String[] descs) {
		if (descs != null && descs.length != getColumnCount()) {
			throw new IllegalArgumentException(
					"Length of descriptions must be equal to the number of columns.");
		}
		this.columnDescriptions = descs;
	}

	/**
	 * Gets the 2-dimensional matrix that holds the expression values
	 * 
	 * @return The expression matrix
	 */
	public DoubleMatrix2D getExpressionMatrix() {
		return dataset;
	}

	/**
	 * Gets the underlying double[][] array
	 * 
	 * @return the array
	 */
	public double[][] getArray() {
		return dataset.getArray();
	}

	/**
	 * Gets the underlying array at the given row
	 * 
	 * @param row
	 *            Row index
	 * @return the row array
	 */
	public double[] getRow(int row) {
		return dataset.getRow(row);
	}

	/**
	 * Gets the row index for the row name .
	 * 
	 * @param rowName
	 *            the row name.
	 * @return the row index, or -1 if the row name is not contained in this
	 *         matrix
	 */
	public int getRowIndex(String rowName) {
		return dataset.getRowIndex(rowName);
	}

	/**
	 * Gets the column index for the column name .
	 * 
	 * @param columnName
	 *            the column name.
	 * @return the column index, or -1 if the column name is not contained in
	 *         this matrix
	 */
	public int getColumnIndex(String columnName) {
		return dataset.getColumnIndex(columnName);
	}

	/**
	 * Sets a single element.
	 * 
	 * @param value
	 *            A(row,column).
	 * @param rowIndex
	 *            The row index.
	 * @param columnIndex
	 *            The column index.
	 */

	public void set(int rowIndex, int columnIndex, double value) {
		dataset.set(rowIndex, columnIndex, value);
	}

	/**
	 * Sets a single element.
	 * 
	 * @param value
	 *            A(row,column).
	 * @param rowName
	 *            The row name.
	 * @param columnName
	 *            The column name.
	 */

	public void set(String rowName, String columnName, double value) {
		dataset.set(rowName, columnName, value);
	}

	/**
	 * Gets a single element
	 * 
	 * @param row
	 *            Row index.
	 * @param column
	 *            Column index.
	 * @return The value at A[row,column]
	 */
	public double get(int row, int column) {
		return dataset.get(row, column);
	}
}
