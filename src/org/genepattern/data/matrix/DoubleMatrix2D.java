package org.genepattern.data.matrix;

import java.util.Arrays;
import java.util.List;
import Jama.Matrix;

/**
 * A labelled dense 2-dimensional matrix holding double elements.
 * 
 * @author Joshua Gould
 */
public class DoubleMatrix2D {
	ObjectIntMap rowNameToRowIndexMap;

	ObjectIntMap columnNameToColumnIndexMap;

	String[] rowNames;

	String[] columnNames;

	Matrix matrix;

	/**
	 * Creates a new matrix
	 * 
	 * @param data The data.
	 */
	public DoubleMatrix2D(double[][] data) {
		int rows = data.length;
		int columns = data[0].length;
		matrix = new Matrix(data);
		rowNameToRowIndexMap = new ObjectIntMap(rows);
		columnNameToColumnIndexMap = new ObjectIntMap(columns);
		this.rowNames = new String[rows];
		this.columnNames = new String[columns];
		fillInRows(0);
		fillInColumns(0);
	}
   
   /**
   * Creates a new matrix
   * 
   * @param data The data.
   * @param rowNames the row names
   * @param columnNames the column names
   */
	public DoubleMatrix2D(double[][] data, String[] rowNames, String[] columnNames) {
		int rows = data.length;
		int columns = data[0].length;
		this.matrix = new Matrix(data);
		this.rowNameToRowIndexMap = new ObjectIntMap(rows);
		this.columnNameToColumnIndexMap = new ObjectIntMap(columns);
		if(columnNames.length!=columns) {
			throw new IllegalArgumentException("Length of column names must be equal to number of columns in data.");
		}
		if(rowNames.length!=rows) {
			throw new IllegalArgumentException("Length of row names must be equal to number of rows in data.");
		}
		this.rowNames = new String[rows];
		this.columnNames = new String[columns];
		setRowNames(Arrays.asList(rowNames));
		setColumnNames(Arrays.asList(columnNames));
	}
		
		
	/**
	 * Creates a new matrix
	 * 
	 * @param rows
	 *            The number of rows.
	 * @param columns
	 *            The number of columns.
	 */
	public DoubleMatrix2D(int rows, int columns) {
		if (rows <= 0) {
			throw new IllegalArgumentException(
					"Number of rows must be greater than 0");
		}
		if (columns <= 0) {
			throw new IllegalArgumentException(
					"Number of columns must be greater than 0");
		}

		matrix = new Matrix(rows, columns);
		rowNameToRowIndexMap = new ObjectIntMap(rows);
		columnNameToColumnIndexMap = new ObjectIntMap(columns);
		rowNames = new String[rows];
		columnNames = new String[columns];
		fillInRows(0);
		fillInColumns(0);
	}

	/**
	 * Creates a new matrix
	 * 
	 * @param rows
	 *            The number of rows.
	 * @param columns
	 *            The number of columns.
	 * @param rowNames
	 *            The row names.
	 * @param columnNames
	 *            The column names.
	 */
	DoubleMatrix2D(int rows, int columns, List rowNames, List columnNames) {
		matrix = new Matrix(rows, columns);
		rowNameToRowIndexMap = new ObjectIntMap(rows);
		columnNameToColumnIndexMap = new ObjectIntMap(columns);
		this.rowNames = new String[rows];
		this.columnNames = new String[columns];
		setRowNames(rowNames);
		setColumnNames(columnNames);
	}

	/**
	 * Creates a new matrix.
	 * 
	 * @param matrix
	 *            The matrix
	 * @param rowNames
	 *            The row names.
	 * @param columnNames
	 *            The column names.
	 * @param rowNameToRowIndexMap
	 *            The row name to row index map.
	 * @param columnNameToColumnIndexMap
	 *            The column name to row index map.
	 */
	private DoubleMatrix2D(Matrix matrix, String[] rowNames,
			String[] columnNames, ObjectIntMap rowNameToRowIndexMap,
			ObjectIntMap columnNameToColumnIndexMap) {
		this.matrix = matrix;
		this.rowNames = rowNames;
		this.columnNames = columnNames;
		this.rowNameToRowIndexMap = rowNameToRowIndexMap;
		this.columnNameToColumnIndexMap = columnNameToColumnIndexMap;
	}

	/** Creates a new uninitalized matrix with 0 rows and 0 columns */

	private DoubleMatrix2D() {
	}

	/**
	 * Prints this matrix in delimitted format using the default number format.
	 * 
	 * @param ps
	 *            the print stream
	 * @param delimiter
	 *            the delimiter between columns
	 * @see #print(java.io.PrintStream, String, java.text.NumberFormat)
	 */
	public void print(java.io.PrintStream ps, String delimiter) {
		print(ps, delimiter, java.text.NumberFormat.getInstance());
	}

	/**
	 * Prints this matrix in delimitted format.
	 * 
	 * @param ps
	 *            the print stream
	 * @param delimiter
	 *            the delimiter between columns
	 * @param nf
	 *            the formatter
	 */
	public void print(java.io.PrintStream ps, String delimiter,
			java.text.NumberFormat nf) {
		for (int j = 0, columns = getColumnCount(); j < columns; j++) {
			ps.print(delimiter);
			ps.print(columnNames[j]);
		}

		int columns = getColumnCount();
		for (int i = 0, rows = getRowCount(); i < rows; i++) {
			ps.println();
			ps.print(rowNames[i]);
			ps.print(delimiter);
			for (int j = 0; j < columns - 1; j++) {
				ps.print(nf.format(matrix.get(i, j)));
				ps.print(delimiter);
			}
			ps.print(nf.format(matrix.get(i, columns - 1)));// don't print the
															// delimmiter after
															// the last column
		}
	}

	/**
	 * Matrix transpose.
	 * 
	 * @return A'
	 */
	public DoubleMatrix2D transpose() {
		DoubleMatrix2D transpose = new DoubleMatrix2D();
		transpose.rowNames = (String[]) this.columnNames.clone();
		transpose.columnNames = (String[]) this.rowNames.clone();
		transpose.columnNameToColumnIndexMap = (ObjectIntMap) rowNameToRowIndexMap
				.clone();
		transpose.rowNameToRowIndexMap = (ObjectIntMap) columnNameToColumnIndexMap
				.clone();
		transpose.matrix = this.matrix.transpose();
		return transpose;
	}

	/**
	 * Constructs and returns a new matrix that contains the indicated cells.
	 * Names can be in arbitrary order.
	 * 
	 * @param rowNames
	 *            The row names of the cells in the new matrix. To indicate that
	 *            the new matrix should contain all rows, set this parameter to
	 *            null.
	 * @param columnNames
	 *            The column names of the cells in the new matrix. To indicate
	 *            that the new matrix should contain all columns, set this
	 *            parameter to null.
	 * @return the new matrix
	 * @throws IllegalArgumentException
	 *             if an index occcurs more than once.
	 * @see #slice(int[], int[])
	 */
	public DoubleMatrix2D slice(String[] rowNames, String[] columnNames) {
		int[] rowIndices = null;
		if (rowNames != null) {
			rowIndices = new int[rowNames.length];
			for (int i = 0, length = rowNames.length; i < length; i++) {
				rowIndices[i] = rowNameToRowIndexMap.get(rowNames[i]);
			}
		}
		int[] columnIndices = null;
		if (columnNames != null) {
			columnIndices = new int[columnNames.length];
			for (int i = 0, length = columnNames.length; i < length; i++) {
				columnIndices[i] = columnNameToColumnIndexMap
						.get(columnNames[i]);
			}
		}
		return slice(rowIndices, columnIndices);
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
	 * @throws IllegalArgumentException
	 *             if an index occcurs more than once.
	 * @see #slice(String[], String[])
	 */
	public DoubleMatrix2D slice(int[] rowIndices, int[] columnIndices) {
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

		DoubleMatrix2D d = new DoubleMatrix2D();
		d.matrix = this.matrix.getMatrix(rowIndices, columnIndices);
		d.rowNames = new String[rowIndices.length];
		d.rowNameToRowIndexMap = new ObjectIntMap(rowIndices.length);

		d.columnNames = new String[columnIndices.length];
		d.columnNameToColumnIndexMap = new ObjectIntMap(columnIndices.length);

		for (int i = 0, length = rowIndices.length; i < length; i++) {
			d.rowNames[i] = rowNames[rowIndices[i]];
			if (d.rowNameToRowIndexMap.containsKey(d.rowNames[i])) {
				throw new IllegalArgumentException(
						"Duplicate indices are not allowed. Row name: "
								+ d.rowNames[i] + ", Row index: "
								+ rowIndices[i]);
			}
			d.rowNameToRowIndexMap.put(d.rowNames[i], i);
		}

		for (int j = 0, length = columnIndices.length; j < length; j++) {
			d.columnNames[j] = columnNames[columnIndices[j]];
			if (d.columnNameToColumnIndexMap.containsKey(d.columnNames[j])) {
				throw new IllegalArgumentException(
						"Duplicate indices are not allowed. Column name: "
								+ d.columnNames[j] + ", Column index: "
								+ columnIndices[j]);
			}
			d.columnNameToColumnIndexMap.put(d.columnNames[j], j);
		}
		return d;
	}

	/**
	 * Linear algebraic matrix multiplication, A * B
	 * 
	 * @param B
	 *            another matrix
	 * @return Matrix product, A * B
	 */
	public DoubleMatrix2D times(DoubleMatrix2D B) {
		Matrix product = matrix.times(B.matrix);
		DoubleMatrix2D C = new DoubleMatrix2D();
		C.matrix = product;
		C.rowNames = (String[]) this.rowNames.clone();
		C.columnNames = (String[]) B.columnNames.clone();
		C.columnNameToColumnIndexMap = (ObjectIntMap) B.columnNameToColumnIndexMap
				.clone();
		C.rowNameToRowIndexMap = (ObjectIntMap) this.rowNameToRowIndexMap
				.clone();
		return C;
	}

	/**
	 * Multiply a matrix by a scalar, C = s*A
	 * 
	 * @param d
	 *            a scalar
	 * @return s*A
	 */
	public DoubleMatrix2D times(double d) {
		Matrix scaledMatrix = matrix.times(d);
		DoubleMatrix2D C = new DoubleMatrix2D();
		C.matrix = scaledMatrix;
		C.rowNames = (String[]) this.rowNames.clone();
		C.columnNames = (String[]) this.columnNames.clone();
		C.columnNameToColumnIndexMap = (ObjectIntMap) this.columnNameToColumnIndexMap
				.clone();
		C.rowNameToRowIndexMap = (ObjectIntMap) this.rowNameToRowIndexMap
				.clone();
		return C;
	}

	/**
	 * Make a deep copy of this matrix
	 * 
	 * @return the copy.
	 */
	public DoubleMatrix2D copy() {
		Matrix matrixCopy = matrix.copy();
		DoubleMatrix2D doubleMatrixCopy = new DoubleMatrix2D();
		doubleMatrixCopy.matrix = matrixCopy;
		doubleMatrixCopy.rowNames = (String[]) this.rowNames.clone();
		doubleMatrixCopy.columnNames = (String[]) this.columnNames.clone();
		doubleMatrixCopy.columnNameToColumnIndexMap = (ObjectIntMap) this.columnNameToColumnIndexMap
				.clone();
		doubleMatrixCopy.rowNameToRowIndexMap = (ObjectIntMap) this.rowNameToRowIndexMap
				.clone();
		return doubleMatrixCopy;
	}

	/**
	 * C = A + B
	 * 
	 * @param B
	 *            another matrix
	 * @return A + B
	 */
	public DoubleMatrix2D plus(DoubleMatrix2D B) {
		DoubleMatrix2D C = new DoubleMatrix2D();
		C.matrix = matrix.plus(B.matrix);
		C.rowNames = (String[]) this.rowNames.clone();
		C.columnNames = (String[]) this.columnNames.clone();
		C.columnNameToColumnIndexMap = (ObjectIntMap) this.columnNameToColumnIndexMap
				.clone();
		C.rowNameToRowIndexMap = (ObjectIntMap) this.rowNameToRowIndexMap
				.clone();
		return C;
	}

	/**
	 * C = A - B
	 * 
	 * @param B
	 *            another matrix
	 * @return A - B
	 */
	public DoubleMatrix2D minus(DoubleMatrix2D B) {
		DoubleMatrix2D C = new DoubleMatrix2D();
		C.matrix = matrix.minus(B.matrix);
		C.rowNames = (String[]) this.rowNames.clone();
		C.columnNames = (String[]) this.columnNames.clone();
		C.columnNameToColumnIndexMap = (ObjectIntMap) this.columnNameToColumnIndexMap
				.clone();
		C.rowNameToRowIndexMap = (ObjectIntMap) this.rowNameToRowIndexMap
				.clone();
		return C;
	}

	/**
	 * Matrix determinant
	 * 
	 * @return The determinant.
	 */
	public double det() {
		return this.matrix.det();
	}

	/**
	 * Returns the effective numerical rank, obtained from Singular Value
	 * Decomposition.
	 * 
	 * @return The rank.
	 */
	public int rank() {
		return this.matrix.rank();
	}

	/**
	 * Computes the sum of the diagonal elements of matrix A; Sum(A[i,i]).
	 * 
	 * @return The trace.
	 */
	public double trace() {
		return this.matrix.trace();
	}

	/**
	 * Fills in row names that the user did not specify.
	 * 
	 * @param rowIndex
	 *            The row index to start filling in rows.
	 */
	private void fillInRows(int rowIndex) {
		for (int i = rowIndex, rows = getRowCount(); i < rows; i++) {
			String rowName = String.valueOf(i + 1);
			if (rowNameToRowIndexMap.containsKey(rowName)) {
				throw new IllegalArgumentException(
						"Duplicate row names are not allowed:" + rowName);
			}
			rowNameToRowIndexMap.put(rowName, i);
			rowNames[i] = rowName;
		}
	}

	/**
	 * Fills in names that the user did not specify.
	 * 
	 * @param columnIndex
	 *            The column index to start filling in columns.
	 */
	private void fillInColumns(int columnIndex) {
		for (int i = columnIndex, columns = getColumnCount(); i < columns; i++) {
			String name = "X" + String.valueOf(i + 1);
			if (columnNameToColumnIndexMap.containsKey(name)) {
				throw new IllegalArgumentException(
						"Duplicate names are not allowed:" + name);
			}
			columnNameToColumnIndexMap.put(name, i);
			columnNames[i] = name;
		}

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
		matrix.set(rowIndex, columnIndex, value);
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
		if (!rowNameToRowIndexMap.containsKey(rowName)) {
			throw new IllegalArgumentException("row name " + rowName
					+ " not found.");
		}
		if (!columnNameToColumnIndexMap.containsKey(columnName)) {
			throw new IllegalArgumentException("column name " + columnName
					+ " not found.");
		}

		matrix.set(rowNameToRowIndexMap.get(rowName),
				columnNameToColumnIndexMap.get(columnName), value);
	}

	/**
	 * Sets the column name at the specified index
	 * 
	 * @param columnIndex
	 *            The column index.
	 * @param name
	 *            The new columnName value
	 */
	public void setColumnName(int columnIndex, String name) {
		if (name == null) {
			throw new IllegalArgumentException(
					"Null column names are not allowed.");
		}

		if (columnNameToColumnIndexMap.containsKey(name)
				&& columnNameToColumnIndexMap.get(name) != columnIndex) {
			throw new IllegalArgumentException(
					"Duplicate column names are not allowed:" + name);
		}

		columnNameToColumnIndexMap.put(name, columnIndex);
		columnNames[columnIndex] = name;
	}

	/**
	 * Sets the row name at the specified index
	 * 
	 * @param rowIndex
	 *            The new rowName value
	 * @param name
	 *            The new rowName value
	 */
	public void setRowName(int rowIndex, String name) {
		if (name == null) {
			throw new IllegalArgumentException(
					"Null row names are not allowed.");
		}

		if (rowNameToRowIndexMap.containsKey(name)
				&& rowNameToRowIndexMap.get(name) != rowIndex) {
			throw new IllegalArgumentException(
					"Duplicate row names are not allowed:" + name);
		}

		rowNameToRowIndexMap.put(name, rowIndex);
		rowNames[rowIndex] = name;
	}

	/**
	 * Sets the row names to the specified value. Duplicate row names are not
	 * allowed. If the length of s is less than getRowCount(), the remaining row
	 * names will be filled in automatically
	 * 
	 * @param s
	 *            The list containing the row names
	 */
	private void setRowNames(List s) {
		if (s.size() > getRowCount()) {
			throw new IllegalArgumentException(
					"Invalid row names length. getRowCount():" + getRowCount()
							+ " row names length:" + s.size());
		}
		for (int i = 0, size = s.size(); i < size; i++) {
			String rowName = (String) s.get(i);

			if (rowNameToRowIndexMap.containsKey(rowName)
					&& rowNameToRowIndexMap.get(rowName) != i) {
				throw new IllegalArgumentException(
						"Duplicate row names are not allowed:" + rowName);
			}

			if (rowName == null) {
				throw new IllegalArgumentException(
						"Null row names are not allowed.");
			}
			rowNameToRowIndexMap.put(rowName, i);
			rowNames[i] = rowName;

		}
	}

	/**
	 * Sets the column names to the specified value. Duplicate column names are
	 * not allowed. If the length of s is less than getColumnCount(), the
	 * remaining names will be filled in automatically.
	 * 
	 * @param s
	 *            The list containing the names
	 */
	private void setColumnNames(List s) {
		if (s.size() > getColumnCount()) {
			throw new IllegalArgumentException(
					"Invalid column names length. getColumnCount():"
							+ getColumnCount() + " column names length:"
							+ s.size());
		}
		for (int i = 0; i < s.size(); i++) {
			String name = (String) s.get(i);
			if (columnNameToColumnIndexMap.containsKey(name)
					&& columnNameToColumnIndexMap.get(name) != i) {
				throw new IllegalArgumentException(
						"Duplicate column names are not allowed:" + name);
			}
			if (name == null) {
				throw new IllegalArgumentException(
						"Null column names are not allowed.");
			}
			columnNameToColumnIndexMap.put(name, i);
			columnNames[i] = name;
		}
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
		if (rowNameToRowIndexMap.containsKey(rowName)) {
			return rowNameToRowIndexMap.get(rowName);
		}
		return -1;
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
		if (columnNameToColumnIndexMap.containsKey(columnName)) {
			return columnNameToColumnIndexMap.get(columnName);
		}
		return -1;
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
		return matrix.get(row, column);
	}

	/**
	 * Gets the underlying array at the given row
	 * 
	 * @param row
	 *            Row index
	 * @return the row array
	 */
	public double[] getRow(int row) {
		return matrix.getArray()[row];
	}

	/**
	 * Gets the underlying double[][] array
	 * 
	 * @return the array
	 */
	public double[][] getArray() {
		return matrix.getArray();
	}

	/**
	 * Gets the row name at the specified index
	 * 
	 * @param rowIndex
	 *            The row index.
	 * @return The row name.
	 */

	public String getRowName(int rowIndex) {
		return rowNames[rowIndex];
	}

	/**
	 * Gets the column name at the specified index
	 * 
	 * @param columnIndex
	 *            The column index.
	 * @return The column name.
	 */
	public String getColumnName(int columnIndex) {
		return columnNames[columnIndex];
	}

	/**
	 * Allocates a new array contains the column names
	 * 
	 * @return The column names.
	 */
	public String[] getColumnNames() {
		return (String[]) columnNames.clone();
	}

	/**
	 * Allocates a new array contains the row names
	 * 
	 * @return The row names.
	 */
	public String[] getRowNames() {
		return (String[]) rowNames.clone();
	}

	/**
	 * Gets the row dimension.
	 * 
	 * @return m, the number of rows.
	 */
	public int getRowCount() {
		return matrix.getRowDimension();
	}

	/**
	 * Gets the column dimension.
	 * 
	 * @return n, the number of columns.
	 */
	public int getColumnCount() {
		return matrix.getColumnDimension();
	}

}

