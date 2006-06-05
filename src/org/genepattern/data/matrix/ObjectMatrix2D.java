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

package org.genepattern.data.matrix;

/**
 * A dense 2-dimensional matrix holding objects.
 * 
 * @author Joshua Gould
 */
public class ObjectMatrix2D {
    Object[][] matrix;

    /**
     * Creates a new matrix
     * 
     * @param rows
     *            The number of rows.
     * @param columns
     *            The number of columns.
     */
    public ObjectMatrix2D(int rows, int columns) {
        if (rows <= 0) {
            throw new IllegalArgumentException(
                    "Number of rows must be greater than 0");
        }
        if (columns <= 0) {
            throw new IllegalArgumentException(
                    "Number of columns must be greater than 0");
        }
        matrix = new Object[rows][columns];
    }

    /**
     * Creates a new matrix
     * 
     * @param matrix
     *            the data
     */
    public ObjectMatrix2D(Object[][] matrix) {
        this.matrix = matrix;
    }

    /**
     * Gets the underlying int[][] array
     * 
     * @return the array
     */
    public Object[][] getArray() {
        return matrix;
    }

    /**
     * Gets the underlying array at the given row
     * 
     * @param row
     *            Row index
     * @return the row array
     */
    public Object[] getRow(int row) {
        return matrix[row];
    }

    /**
     * Constructs and returns a new matrix that contains the indicated cells.
     * Indices can be in arbitrary order.
     * 
     * @param rowIndices
     *            The rows of the cells in the new matrix. To indicate that the
     *            new matrix should contain all rows, set this parameter to
     *            null.
     * @param columnIndices
     *            The columns of the cells in the new matrix. To indicate that
     *            the new matrix should contain all columns, set this parameter
     *            to null.
     * @return the new matrix
     */
    public ObjectMatrix2D slice(int[] rowIndices, int[] columnIndices) {
        if (rowIndices == null) {
            rowIndices = new int[getRowCount()];
            for (int i = getRowCount(); --i >= 0;) {
                rowIndices[i] = i;
            }
        }
        if (columnIndices == null) {
            columnIndices = new int[getColumnCount()];
            for (int i = getColumnCount(); --i >= 0;) {
                columnIndices[i] = i;
            }
        }

        Object[][] sMatrix = new Object[rowIndices.length][columnIndices.length];

        for (int i = 0, rows = rowIndices.length; i < rows; i++) {
            for (int j = 0, cols = columnIndices.length; j < cols; j++) {
                sMatrix[i][j] = this.matrix[rowIndices[i]][columnIndices[j]];
            }
        }
        return new ObjectMatrix2D(sMatrix);
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

    public void set(int rowIndex, int columnIndex, Object value) {
        matrix[rowIndex][columnIndex] = value;
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
    public Object get(int row, int column) {
        return matrix[row][column];
    }

    /**
     * Gets the row dimension.
     * 
     * @return m, the number of rows.
     */
    public int getRowCount() {
        return matrix.length;
    }

    /**
     * Gets the column dimension.
     * 
     * @return n, the number of columns.
     */
    public int getColumnCount() {
        return matrix[0].length;
    }

    /**
     * Matrix transpose.
     * 
     * @return A'
     */
    public ObjectMatrix2D transpose() {
        ObjectMatrix2D X = new ObjectMatrix2D(getColumnCount(), getRowCount());
        Object[][] C = X.getArray();
        for (int i = 0, m = getRowCount(); i < m; i++) {
            for (int j = 0, n = getColumnCount(); j < n; j++) {
                C[j][i] = matrix[i][j];
            }
        }
        return X;
    }
}
