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

/**
 * Matrix interface
 * 
 * @author Joshua Gould
 * 
 */
public interface IMatrix {

    /**
     * Gets the value at the given row and column.
     * 
     * @param row
     *            The row
     * @param column
     *            The column
     * @return The value
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
