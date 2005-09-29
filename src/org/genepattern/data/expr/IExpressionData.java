package org.genepattern.data.expr;

/**
 * An interface for expression data.
 * 
 * @author Joshua Gould
 */
public interface IExpressionData {

	/**
	 * Gets the expression value at the given row and column.
	 * 
	 * @param row
	 *            The row
	 * @param column
	 *            The column
	 * @return The expression value
	 */
	public double getValue(int row, int column);

	/**
	 * Gets the row name at the given row
	 * 
	 * @param row
	 *            The row
	 * @return The row name
	 */
	public String getRowName(int row);

	/**
	 * Gets the number of rows in the expression data matrix
	 * 
	 * @return The number of rows
	 */
	public int getRowCount();

	/**
	 * Gets the number of column in the expression data matrix
	 * 
	 * @return The number of column
	 */
	public int getColumnCount();

	/**
	 * Gets the column name at the given column
	 * 
	 * @param column
	 *            The column
	 * @return The column name
	 */
	public String getColumnName(int column);

	/**
	 * Gets the row description at the given row or <tt>null</tt> if no row
	 * description exists.
	 * 
	 * @param row
	 *            The row
	 * @return The row description
	 */
	public String getRowDescription(int row);

	/**
	 * Gets the column description at the given column or <tt>null</tt> if no
	 * column description exists.
	 * 
	 * @param column
	 *            The column
	 * @return The column description
	 */
	public String getColumnDescription(int column);

	/**
	 * Gets the row index for the row name .
	 * 
	 * @param rowName
	 *            the row name.
	 * @return the row index, or -1 if the row name is not contained in this
	 *         matrix
	 */
	public int getRowIndex(String rowName);

	/**
	 * Gets the column index for the column name .
	 * 
	 * @param columnName
	 *            the column name.
	 * @return the column index, or -1 if the column name is not contained in
	 *         this matrix
	 */
	public int getColumnIndex(String columnName);

}
