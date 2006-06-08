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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.genepattern.data.matrix.DoubleMatrix2D;
import org.genepattern.data.matrix.ObjectMatrix2D;

/**
 * Implementation of IExpressionData interface
 * 
 * @author Joshua Gould
 */
public class ExpressionData implements IExpressionData {
    protected DoubleMatrix2D dataset;

    protected Map matrices;

    protected MetaData rowMetaData;

    protected MetaData columnMetaData;

    /**
     * Creates a new <tt>ExpressionData</tt> instance
     * 
     * @param dataset
     *            The dataset
     * @param _rowDescriptions
     *            The row descriptions or <tt>null</tt>
     * @param _columnDescriptions
     *            The column descriptions or <tt>null</tt>>
     */
    public ExpressionData(DoubleMatrix2D dataset, String[] _rowDescriptions,
            String[] _columnDescriptions) {
        this.dataset = dataset;
        this.rowMetaData = new MetaData(dataset.getRowCount());

        if (_rowDescriptions != null) {
            if (_rowDescriptions.length != dataset.getRowCount()) {
                throw new IllegalArgumentException(
                        "Length of row descriptions not equal to number of rows in matrix.");
            }
            rowMetaData.setMetaData(ExpressionConstants.DESC, _rowDescriptions);
        }

        this.columnMetaData = new MetaData(dataset.getColumnCount());
        if (_columnDescriptions != null) {
            if (_columnDescriptions.length != dataset.getColumnCount()) {
                throw new IllegalArgumentException(
                        "Length of column descriptions not equal to number of columns in matrix.");
            }
            columnMetaData.setMetaData(ExpressionConstants.DESC,
                    _columnDescriptions);
        }
        matrices = new HashMap();
    }

    /**
     * * Creates a new <tt>ExpressionData</tt> instance
     * 
     * @param dataset
     *            The dataset
     * @param rowMetaData
     *            the row meta data
     * @param columnMetaData
     *            the column meta data
     */
    public ExpressionData(DoubleMatrix2D dataset, MetaData rowMetaData,
            MetaData columnMetaData, Map matrices) {
        this.dataset = dataset;
        this.rowMetaData = rowMetaData;
        this.columnMetaData = columnMetaData;
        this.matrices = matrices;
        if (this.matrices == null) {
            this.matrices = new HashMap();
        }
    }

    /**
     * Gets the matrix for the given key
     * 
     * @param key
     *            The key
     * @return The matrix or <tt>null</tt> if the key is not found
     */
    public ObjectMatrix2D getMatrix(String key) {
        return (ObjectMatrix2D) matrices.get(key);
    }

    /**
     * Sets the matrix for the given key
     * 
     * @param key
     *            The key
     * @throws IllegalArgumentException
     *             if matrix.getRowCount != getRowCount() or
     *             matrix.getColumnCount != getColumnCount()
     */
    public void setMatrix(String key, ObjectMatrix2D matrix) {
        if (matrix.getRowCount() != getRowCount()) {
            throw new IllegalArgumentException(
                    "Number of rows in given matrix must be equal to the number of rows.");
        }
        if (matrix.getColumnCount() != getColumnCount()) {
            throw new IllegalArgumentException(
                    "Number of columns in given matrix must be equal to the number of columns.");
        }

        matrices.put(key, matrix);
    }

    /**
     * Constructs and returns a new <tt>ExpressionData</tt> instance that
     * contains the indicated cells. Names can be in arbitrary order.
     * 
     * @param rowNames
     *            The row names of the cells in the new matrix. To indicate that
     *            the new matrix should contain all rows, set this parameter to
     *            null.
     * @param columnNames
     *            The column names of the cells in the new matrix. To indicate
     *            that the new matrix should contain all columns, set this
     *            parameter to null.
     * @return the new ExpressionData instance
     * @throws IllegalArgumentException
     *             if a name occcurs more than once.
     * @see #slice(int[], int[])
     */
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

    /**
     * Constructs and returns a new <tt>ExpressionData</tt> instance that
     * contains the indicated cells. Indices can be in arbitrary order.
     * 
     * @param rowIndices
     *            The rows of the cells in the new matrix. To indicate that the
     *            new matrix should contain all rows, set this parameter to
     *            null.
     * @param columnIndices
     *            The columns of the cells in the new matrix. To indicate that
     *            the new matrix should contain all columns, set this parameter
     *            to null.
     * @return the new ExpressionData
     * @throws IllegalArgumentException
     *             if an index occcurs more than once.
     * @see #slice(String[], String[])
     */
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

        MetaData newRowMetaData = rowMetaData.slice(rowIndices);
        MetaData newColumnMetaData = columnMetaData.slice(columnIndices);
        HashMap newMatrices = new HashMap();
        for (Iterator it = matrices.keySet().iterator(); it.hasNext();) {
            String key = (String) it.next();
            ObjectMatrix2D m = (ObjectMatrix2D) matrices.get(key);
            newMatrices.put(key, m.slice(rowIndices, columnIndices));
        }

        return new ExpressionData(newDoubleMatrix2D, newRowMetaData,
                newColumnMetaData, newMatrices);
    }

    /**
     * Sets the row description at the given row
     * 
     * @param row
     *            The row
     * @param description
     *            The description
     */
    public void setRowDescription(int row, String description) {
        rowMetaData.setMetaData(row, ExpressionConstants.DESC, description);
    }

    /**
     * Sets the column description at the given column
     * 
     * @param column
     *            The column
     * @param description
     *            The description
     */
    public void setColumnDescription(int column, String description) {
        columnMetaData.setMetaData(column, ExpressionConstants.DESC,
                description);
    }

    /**
     * Sets the column name at the specified index
     * 
     * @param column
     *            The column index.
     * @param name
     *            The new column name value
     */
    public void setColumnName(int column, String name) {
        dataset.setColumnName(column, name);
    }

    /**
     * Sets the row name at the specified index
     * 
     * @param row
     *            The row index
     * @param name
     *            The new row name value
     */
    public void setRowName(int row, String name) {
        dataset.setRowName(row, name);
    }

    public String getColumnDescription(int column) {
        if (columnMetaData.contains(ExpressionConstants.DESC)) {
            return columnMetaData.getMetaData(column, ExpressionConstants.DESC);
        }
        return null;
    }

    public String getRowDescription(int row) {
        if (rowMetaData.contains(ExpressionConstants.DESC)) {
            return rowMetaData.getMetaData(row, ExpressionConstants.DESC);
        }
        return null;
    }

    /**
     * Gets the array containing the row descriptions or <tt>null</tt> if no
     * row descriptions are set
     * 
     * @return The row descriptions.
     */
    public String[] getRowDescriptions() {
        return rowMetaData.contains(ExpressionConstants.DESC) ? rowMetaData
                .getArray(ExpressionConstants.DESC) : null;
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
        rowMetaData.setMetaData(ExpressionConstants.DESC, descs);
    }

    /**
     * Gets the array containing the column descriptions or <tt>null</tt> if
     * no column descriptions are set
     * 
     * @return The column descriptions.
     */
    public String[] getColumnDescriptions() {
        return columnMetaData.contains(ExpressionConstants.DESC) ? columnMetaData
                .getArray(ExpressionConstants.DESC)
                : null;
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
        columnMetaData.setMetaData(ExpressionConstants.DESC, descs);
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
     * Allocates a new array contains the column names
     * 
     * @return The column names.
     */
    public String[] getColumnNames() {
        return dataset.getColumnNames();
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

    public boolean containsData(String name) {
        return matrices.containsKey(name);
    }

    public String[] getDataNames() {
        return (String[]) matrices.keySet().toArray(new String[0]);
    }

    public String[] getRowMetadataNames() {
        return rowMetaData.getNames();
    }

    public String[] getColumnMetadataNames() {
        return columnMetaData.getNames();
    }

    public Object getData(int row, int column, String name) {
        ObjectMatrix2D matrix = (ObjectMatrix2D) matrices.get(name);
        return matrix != null ? matrix.get(row, column) : null;
    }

    public boolean containsRowMetadata(String name) {
        return rowMetaData.contains(name);
    }

    public boolean containsColumnMetadata(String name) {
        return columnMetaData.contains(name);
    }

    public String getRowMetadata(int row, String name) {
        return rowMetaData.getMetaData(row, name);
    }

    public String getColumnMetadata(int column, String name) {
        return columnMetaData.getMetaData(column, name);
    }
}
