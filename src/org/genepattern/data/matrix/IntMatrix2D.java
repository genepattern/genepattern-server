package org.genepattern.data.matrix;
import java.util.List;
import Jama.Matrix;

/**
 *  A dense 2-dimensional matrix holding int elements.
 *
 * @author    Joshua Gould
 */
public class IntMatrix2D {
	int[][] matrix;


	/**
	 *  Creates a new matrix
	 *
	 * @param  rows     The number of rows.
	 * @param  columns  The number of columns.
	 */
	public IntMatrix2D(int rows, int columns) {
		if(rows <= 0) {
			throw new IllegalArgumentException("Number of rows must be greater than 0");
		}
		if(columns <= 0) {
			throw new IllegalArgumentException("Number of columns must be greater than 0");
		}

		matrix = new int[rows][columns];

	}


	IntMatrix2D(int[][] matrix) {
		this.matrix = matrix;
	}



	/**
	 *  Constructs and returns a new matrix that contains the indicated cells.
	 *  Indices can be in arbitrary order.
	 *
	 * @param  rowIndices     The rows of the cells in the new matrix. To indicate
	 *      that the new matrix should contain all rows, set this parameter to
	 *      null.
	 * @param  columnIndices  The columns of the cells in the new matrix. To
	 *      indicate that the new matrix should contain all columns, set this
	 *      parameter to null.
	 * @return                the new matrix
	 */
	public IntMatrix2D slice(int[] rowIndices, int[] columnIndices) {
		if(rowIndices == null) {
			rowIndices = new int[getRowCount()];
			for(int i = getRowCount(); --i >= 0; ) {
				rowIndices[i] = i;
			}
		}
		if(columnIndices == null) {
			columnIndices = new int[getColumnCount()];
			for(int i = getColumnCount(); --i >= 0; ) {
				columnIndices[i] = i;
			}
		}

		int[][] sMatrix = new int[rowIndices.length][columnIndices.length];

		for(int i = 0, rows = rowIndices.length; i < rows; i++) {
			for(int j = 0, cols = columnIndices.length; j < cols; j++) {
				sMatrix[i][j] = this.matrix[rowIndices[i]][columnIndices[j]];
			}
		}

		return new IntMatrix2D(sMatrix);
	}



	/**
	 *  Sets a single element.
	 *
	 * @param  value        A(row,column).
	 * @param  rowIndex     The row index.
	 * @param  columnIndex  The column index.
	 */

	public void set(int rowIndex, int columnIndex, int value) {
		matrix[rowIndex][columnIndex] = value;
	}


	/**
	 *  Gets a single element
	 *
	 * @param  row     Row index.
	 * @param  column  Column index.
	 * @return         The value at A[row,column]
	 */
	public int get(int row, int column) {
		return matrix[row][column];
	}


	/**
	 *  Gets the row dimension.
	 *
	 * @return    m, the number of rows.
	 */
	public int getRowCount() {
		return matrix.length;
	}


	/**
	 *  Gets the column dimension.
	 *
	 * @return    n, the number of columns.
	 */
	public int getColumnCount() {
		return matrix[0].length;
	}

}

