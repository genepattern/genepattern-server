/*
 * WHITEHEAD INSTITUTE
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2001 by the
 * Whitehead Institute for Biomedical Research.  All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever.  The Whitehead Institute can not be responsible for its
 * use, misuse, or functionality.
 */

package org.genepattern.data;

import java.io.Serializable;
import java.util.Arrays;

import org.genepattern.util.ArrayUtils;
import org.genepattern.util.ReusableStringTokenizer;
import org.genepattern.util.XLogger;

//import edu.mit.genome.util.ImmutedException;

import gnu.trove.TFloatArrayList;

/**
 * This class represents an immutable matrix. There are no setXXX(...) methods.
 * See MatrixMutable for a subclass that is mutable. Note that all variables are
 * private. There are protected getInternalXXX(...) that are for use by
 * subclasses only. Classes within this package should not be using them.
 * 
 * None of the methods in this class should change the data, either in this
 * Matrix or in passed in 'other' instances. MatrixMutable objects can have
 * their state modified by the various methods present.
 *  * Lots of code/ideas copied form Kenji Hiranabe, Eiwa System Management,
 * Inc. GMatrix part of the javax.vecmath library (unoffical (his) impl)
 * http://java.sun.com/products/java-media/3D/forDevelopers/j3dapi/index.html
 * 
 * as note: made several of the row related methods unfinal as need to override
 * 
 * @author Aravind Subramanian
 * @version %I%, %G%
 */

public class DefaultMatrix implements Matrix, Serializable {

	/** static initilizer */
	//    static {
	//        /* Thoughts:
	//         * This class can be persisted through XMLEncoder because the property
	//         * names, prop_names, match the variables above. Also the order of the
	//         * variables (represented by the Strings) match one of the public
	// constructors
	//         * -- kwo
	//         */
	//        //fool Encoder into being instantiated dispite being an abstract class
	//        java.beans.Encoder encoder = new java.beans.Encoder() {};
	//        String [] prop_names = new String[] {"rowCount", "columnCount",
	// "elementData"};
	//        encoder.setPersistenceDelegate(DefaultMatrix.class,
	//                        new java.beans.DefaultPersistenceDelegate(prop_names));
	//    }
	/**
	 * Constructs an rowCount by columnCount all zero matrix. (as change) Note
	 * that even though row and column numbering begins with zero, rowCount and
	 * columnCount will be one larger than the maximum possible matrix index
	 * values.
	 * 
	 * @param nrows
	 *            number of rows in this matrix.
	 * @param ncols
	 *            number of columns in this matrix.
	 */
	protected DefaultMatrix(final int nrows, final int ncols,
			final boolean mutable) {
		this(mutable);
		if (nrows < 0)
			throw new NegativeArraySizeException(nrows + " < 0");
		if (ncols < 0)
			throw new NegativeArraySizeException(ncols + " < 0");

		this.rowCount = nrows;
		this.columnCount = ncols;
		elementData = new float[rowCount * columnCount];
		//java.util.Arrays.fill(elementData, 0.0f);
	}

	/**
	 * Constructs an RowCnt by ColCnt matrix initialized to the values in the
	 * matrix array. The array values are copied in one row at a time in row
	 * major fashion. The array should be at least RowCnt*ColCnt in length. Note
	 * that even though row and column numbering begins with zero, RowCnt and
	 * ColCnt will be one larger than the maximum possible matrix index values.
	 * 
	 * @param nrows
	 *            number of rows in this matrix.
	 * @param ncols
	 *            number of columns in this matrix.
	 * @param matrix
	 *            a 1D array that specifies a matrix in row major fashion
	 */
	public DefaultMatrix(final int nrows, final int ncols, final float[] matrix) {
		this(nrows, ncols, matrix, false);
	}

	protected DefaultMatrix(int nrows, int ncols, float[] matrix,
			boolean mutable) {
		this(nrows, ncols, mutable);
		System.arraycopy(matrix, 0, elementData, 0, this.elementData.length);
	}

	public DefaultMatrix(final Matrix matrixA, final Matrix matrixB) {
		this(new Matrix[] { matrixA, matrixB }, false);
	}

	/**
	 * data is not shared matrices must be of same number of columns (nrows can
	 * be arb - thats the point!)
	 */
	public DefaultMatrix(final Matrix[] matrixs) {
		this(matrixs, false);
	}

	protected DefaultMatrix(final Matrix[] matrixs, final boolean mutable) {
		this(mutable);

		int c = matrixs[0].getColumnCount();

		for (int i = 0; i < matrixs.length; i++) {
			if (matrixs[i].getColumnCount() != c)
				throw new java.lang.IllegalArgumentException("Unequal ncols: "
						+ c + " and " + matrixs[i].getColumnCount());
		}

		int nrows = 0;
		int ncols = 0;

		for (int i = 0; i < matrixs.length; i++) {
			nrows += matrixs[i].getRowCount();
		}

		this.rowCount = nrows;
		this.columnCount = c;
		this.elementData = new float[rowCount * columnCount];

		int pos = 0;
		for (int i = 0; i < matrixs.length; i++) {
			final float[] data = ((DefaultMatrix) matrixs[i]).elementData;
			final int len = data.length;
			System.arraycopy(data, 0, elementData, pos, len);
			pos += len;
		}
	}

	/**
	 * Constructs a new Matrix and copies the initial values from the parameter
	 * matrix.
	 * 
	 * @param matrix
	 *            the source of the initial values of the new Matrix
	 * 
	 * as added: if shared values, then this matrix shares the elements with the
	 * specified matrix else the array is systemarrycopied
	 */
	public DefaultMatrix(Matrix matrix) {
		this(matrix, false);
	}

	protected DefaultMatrix(final Matrix matrix, final boolean mutable) {
		this(mutable);
		this.rowCount = matrix.getRowCount();
		this.columnCount = matrix.getColumnCount();
		final float[] data = ((DefaultMatrix) matrix).elementData;
		if (matrix.isMutable() || this.subclass_can_change) {
			int newSize = rowCount * columnCount;
			this.elementData = new float[newSize];
			System.arraycopy(data, 0, elementData, 0, newSize);
		} else {
			this.elementData = data;
		}
	}

	protected DefaultMatrix(boolean mutable) {
		this.subclass_can_change = mutable;
	}

	/**
	 * parses the string to produce a 2D array of data assumes data is from the
	 * output of dump() puts the row count and column count in the int array
	 * row_col_cnt[0] is the row count row_col_cnt[1] is the column count
	 * 
	 * @throws NumberFormatException
	 *             if there is a problem with converting the tokens to floats
	 */
	//    public static final float[][] parseString(final String data, final int[]
	// row_col_cnt) throws NumberFormatException {
	//       final ReusableStringTokenizer r_tok = new ReusableStringTokenizer();
	//       final ReusableStringTokenizer val_tok = new ReusableStringTokenizer(); //
	// whitespace including tab
	//       final String ws = ReusableStringTokenizer.WHITESPACE;
	//       r_tok.resetTo(data, "[ ]", false);
	//       final int num_rows = r_tok.countTokens() - 1;
	//       final float [][] array = new float [num_rows][];
	//       // row index, column index, length of a row
	//       int r = 0, c = 0, row_len = 0;
	//       try {
	//           while( r_tok.hasMoreTokens() ) {
	//               final String row = r_tok.nextToken();
	//               val_tok.resetTo(row, ws, false);
	//               final int cnt = val_tok.countTokens();
	//               
	//               // sanity check
	//               if( row_len != 0 ) {
	//                   if( cnt != row_len ) {
	//                        throw new ArrayIndexOutOfBoundsException("Length of row "
	//                            +r+" is "+cnt+" but should be "+row_len+"!");
	//                   }
	//               } else // first time so set row_len
	//                   row_len = cnt;
	//               
	//               final float [] row_array = new float [cnt];
	//               array[r] = row_array;
	//               c = 0;
	//               while( val_tok.hasMoreTokens() ) {
	//                   final String token = val_tok.nextToken();
	//                   final float value = Float.parseFloat(token);
	//                   row_array[c] = value;
	//                   c++;
	//               }
	//               r++;
	//           }
	//       } catch (NumberFormatException ex) {
	//           throw new NumberFormatException(ex+" at row "+r+", column "+c);
	//       }
	//       row_col_cnt[0] = num_rows;
	//       row_col_cnt[1] = row_len; // which is the numbner of columns
	//       return array;
	//    }
	public static final float[] parseString(final String data_string,
			final int[] row_col_cnt) throws NumberFormatException {
		final String data = data_string.trim();
		final ReusableStringTokenizer r_tok = new ReusableStringTokenizer();
		final ReusableStringTokenizer val_tok = new ReusableStringTokenizer();
		final String ws = ReusableStringTokenizer.WHITESPACE;

		r_tok.resetTo(data, "[", false);
		final int num_rows = r_tok.countTokens() - 1;
		final TFloatArrayList vector = new TFloatArrayList(num_rows * 100);// est.
																		   // size
		int r = 0, c = 0, row_len = 0;
		try {
			while (r_tok.hasMoreTokens()) {
				final String row = r_tok.nextToken();
				val_tok.resetTo(row, ws + "[]", false);//val_tok.resetTo(row,
													   // ws, false);
				c = 0;
				while (val_tok.hasMoreTokens()) {
					final String token = val_tok.nextToken();
					final float value = Float.parseFloat(token);
					vector.add(value);
					c++;
				}
				// sanity check
				if (row_len != 0) {
					if (c != row_len) {
						throw new ArrayIndexOutOfBoundsException(
								"Length of row " + r + " is " + c
										+ " but should be " + row_len + "!");
					}
				} else
					// first time so set row_len
					row_len = c;

				r++;
			}
		} catch (NumberFormatException ex) {
			throw new NumberFormatException(ex + ":\nat row " + r + ", column "
					+ c);
		}

		row_col_cnt[0] = num_rows;
		row_col_cnt[1] = row_len; // which is the numbner of columns

		return vector.toNativeArray();
	}

	//    /** test parseString */
	//    public static void main(final String[] args) {
	//        //final String data = "[ [359.8975 301.44925 371.68536 296.83972
	// 284.50644 196.84131 383.2755 400.70026 324.9408 482.81595 270.24112
	// 268.4501 638.277 280.66357 236.82712 360.41672 297.84247 310.44394
	// 210.5185 378.10822 397.7781 270.57297 479.28885 293.8416 327.8654
	// 345.71387 247.01103 314.5929 224.64565 472.78412 384.7093 321.7087
	// 364.28226 253.39578 410.72562 296.64578 373.73352 423.47232] [13474.908
	// 14258.262 14715.771 13695.667 13647.065 13979.262 13960.111 14615.777
	// 14289.196 14841.32 13659.738 13796.405 15352.412 14118.692 13648.837
	// 14420.393 14274.621 13959.17 13757.032 14922.111 14389.235 13979.908
	// 14931.529 14548.869 14508.117 14459.163 13722.745 14503.085 13729.333
	// 14617.974 14565.457 14536.078 14758.634 14378.568 14761.889 14366.202
	// 14495.026 14384.444] ]";
	//        final String data = " [ [286.2857 226.95763 312.97974 201.29184 194.98112
	// 187.00543 345.4358 366.5634 246.33171 415.59534 202.4267 216.21548
	// 570.5884 275.50323 232.04291 308.34488 319.07904 261.2104 90.92851
	// 390.09662 327.3447 238.77219 422.84995 303.76337 287.00333 278.3014
	// 192.82605 305.4419 187.56792 507.79538 395.29858 332.05597 373.69162
	// 215.31554 402.3468 282.17657 364.83157 431.0131] [12352.847 12973.49
	// 13482.676 12335.109 12385.29 12693.725 12848.918 13464.493 13046.984
	// 13690.954 12494.255 12570.944 14318.386 12825.974 12395.598 13147.79
	// 13050.247 12742.468 12415.714 13696.25 13173.715 12765.154 13863.793
	// 13370.125 13324.183 13184.99 12455.911 13233.171 12358.426 13528.876
	// 13444.697 13248.735 13569.03 13113.938 13663.237 13187.144 13354.49
	// 13244.352] ]";
	//        final int[] row_col = new int[2];
	//        final float[] values = parseString(data, row_col);
	//        
	//        
	//        final int row_cnt = row_col[0], col_cnt = row_col[1] - 1;
	//        System.out.println("values["+row_cnt+"]["+(col_cnt + 1)+"]:");
	//        for(int i = 0, k = 0; i < row_cnt; i++) {
	//            for(int j = 0; j < col_cnt; j++) {
	//                System.out.print(values[k++]);
	//                System.out.print(", ");
	//            }
	//            System.out.println(values[k++]);
	//        }
	//    }
	/**
	 * get a new Matrix that is the transpose of this matrix.
	 */
	public final Matrix getTransposedMatrix() {
		final float[] elements = new float[elementData.length];
		final int num_cols = getColumnCount(), num_rows = getRowCount();
		for (int r = 0; r < num_rows; r++) {
			for (int c = 0; c < num_cols; c++) { //rc -> cr
				elements[c * num_rows + r] = elementData[r * num_cols + c];
			}
		}
		return new DefaultMatrix(num_cols, num_rows, elements);
	}

	/** returns true if this Matrix is unsafe and its values can be changed */
	public final boolean isMutable() {
		return subclass_can_change;
	}

	// internal or subclass-only methods
	// These methods throw an Exception if this is not mutable!

	/** gets the elementData */
	protected final float[] getInternalElementData() {
		if (!subclass_can_change)
			throw new IllegalAccessError(
					"Cannot get access to the elementData. Matrix is immutable!");
		return elementData;
	}

	/** sets the elementData */
	protected final void setInternalElementData(final float[] elementData) {
		if (!subclass_can_change)
			throw new IllegalAccessError(
					"Cannot change the elementData to another object. Matrix is immutable!");
		this.elementData = elementData;
	}

	/** sets the number of rows */
	protected final void setInternalRowCount(final int rowcnt) {
		if (!subclass_can_change)
			throw new IllegalAccessError(
					"Cannot set rowCount value. Matrix is immutable! ");
		this.rowCount = rowcnt;
	}

	/** sets the number of columns */
	protected final void setInternalColCount(final int colcnt) {
		if (!subclass_can_change)
			throw new IllegalAccessError(
					"Cannot set columnCount value. Matrix is immutable!");
		this.columnCount = colcnt;
	}

	/**
	 * returns the index into the data array given the row and column index
	 * (zero based)
	 */
	protected final int getIndex(final int row, final int column) {
		return getIndex(row, column, rowCount, columnCount);
	}

	/**
	 * returns the index into the data array given the row and column index
	 * (zero based) and row and column counts
	 */
	public static final int getIndex(final int row, final int column,
			final int row_cnt, final int col_cnt) {
		return row * col_cnt + column;
	}

	// end special subclass-only methods

	/**
	 * Copies a sub-matrix derived from this matrix into the target matrix. The
	 * upper left of the sub-matrix is located at (rowSource, colSource); the
	 * lower right of the sub-matrix is located at
	 * (lastRowSource,lastColSource). The sub-matrix is copied into the the
	 * target matrix starting at (rowDest, colDest).
	 * 
	 * @param rowSource
	 *            the top-most row of the sub-matrix
	 * @param colSource
	 *            the left-most column of the sub-matrix
	 * @param numRow
	 *            the number of rows in the sub-matrix
	 * @param numCol
	 *            the number of columns in the sub-matrix
	 * @param rowDest
	 *            the top-most row of the position of the copied sub-matrix
	 *            within the target matrix
	 * @param colDest
	 *            the left-most column of the position of the copied sub-matrix
	 *            within the target matrix
	 * @param target
	 *            the matrix into which the sub-matrix will be copied
	 */
	public final void copySubMatrix(final int rowSource, final int colSource,
			final int numRow, final int numCol, final int rowDest,
			final int colDest, final MatrixMutable target) {
		if (rowSource < 0 || colSource < 0 || rowDest < 0 || colDest < 0)
			throw new ArrayIndexOutOfBoundsException(
					"rowSource,colSource,rowDest,colDest < 0.");
		if (rowCount < numRow + rowSource || columnCount < numCol + colSource)
			throw new ArrayIndexOutOfBoundsException("Source Matrix too small.");
		if (target.getRowCount() < numRow + rowDest
				|| target.getColumnCount() < numCol + colDest)
			throw new ArrayIndexOutOfBoundsException("Target Matrix too small.");

		final float[] target_data = target.getElementData();
		for (int i = 0; i < numRow; i++)
			for (int j = 0; j < numCol; j++)
				target_data[(i + rowDest) * columnCount + (j + colDest)] = elementData[(i + rowSource)
						* columnCount + (j + colSource)];
	}

	/**
	 * creates a new DefaultMatrix out of the row and column indices Uses all of
	 * the columns or rows if the cols or rows array, respectively, is null or
	 * size 0.
	 */
	public final Matrix createSubMatrix(int[] rows, int[] cols) {
		if (rows != null && rows.length > 0) {
			if (!ArrayUtils.arrayValuesLessThan(rows, rowCount))
				throw new IllegalArgumentException(
						"Some values in the row index "
								+ "array are greater than the number of rows ("
								+ rowCount + "):\n" + ArrayUtils.toString(rows));
		} else {
			rows = ArrayUtils.rangeAsElements(0, rowCount);
		}

		if (cols != null && cols.length > 0) {
			if (!ArrayUtils.arrayValuesLessThan(cols, columnCount))
				throw new IllegalArgumentException(
						"Some values in the column index "
								+ "array are greater than the number of columns ("
								+ columnCount + "):\n"
								+ ArrayUtils.toString(cols));
		} else {
			cols = ArrayUtils.rangeAsElements(0, columnCount);
		}

		final int num_rows = rows.length, num_cols = cols.length;
		final DefaultMatrix matrix = new DefaultMatrix(num_rows, num_cols,
				false);
		final float[] data = matrix.elementData;

		for (int r = 0; r < num_rows; r++) {
			for (int c = 0; c < num_cols; c++) {
				data[matrix.getIndex(r, c)] = this.elementData[this.getIndex(
						rows[r], cols[c])];
			}
		}

		return matrix;
	}

	/**
	 * Returns the number of rows in this matrix.
	 * 
	 * @return number of rows in this matrix
	 */
	public final int getRowCount() {
		return rowCount;
	}

	/**
	 * Returns the number of colmuns in this matrix.
	 * 
	 * @return number of columns in this matrix
	 */
	public final int getColumnCount() {
		return columnCount;
	}

	/**
	 * Retrieves the value at the specified row and column of this matrix.
	 * 
	 * @param row
	 *            the row number to be retrieved (zero indexed)
	 * @param column
	 *            the column number to be retrieved (zero indexed)
	 * @return the value at the indexed element
	 */
	public final float getElement(final int row, final int column) {
		if (rowCount <= row)
			throw new ArrayIndexOutOfBoundsException("row:" + row
					+ " >= matrix's row count:" + rowCount);
		if (row < 0)
			throw new ArrayIndexOutOfBoundsException("row:" + row + " < 0");
		if (columnCount <= column)
			throw new ArrayIndexOutOfBoundsException("column:" + column
					+ " >= matrix's column count:" + columnCount);
		if (column < 0)
			throw new ArrayIndexOutOfBoundsException("column:" + column
					+ " < 0");

		return elementData[getIndex(row, column)];
	}

	/**
	 * gets the value at the specified index into the elementData array This
	 * should only be used by subclasses not intended for general use.
	 */
	public final float getElementAtIndex(final int i) {
		return elementData[i];
	}

	/**
	 * Places the values of the specified row into the array parameter.
	 * 
	 * @param row
	 *            the target row number
	 * @param array
	 *            the array into which the row values will be placed
	 */
	public final float[] getRow(final int row, final float[] array) {
		if (rowCount <= row)
			throw new ArrayIndexOutOfBoundsException("row:" + row
					+ " > matrix's rowCount:" + rowCount);
		if (row < 0)
			throw new ArrayIndexOutOfBoundsException("row:" + row + " < 0");
		if (array.length < columnCount)
			throw new ArrayIndexOutOfBoundsException("array length:"
					+ array.length + " smaller than matrix's columnCount:"
					+ columnCount);

		System.arraycopy(elementData, row * columnCount, array, 0, columnCount);
		return array;
	}

	/**
	 * Places the values of the specified row into the vector parameter.
	 * 
	 * @param row
	 *            the target row number
	 * @param vector
	 *            the vector into which the row values will be placed
	 */
	public final FloatVector getRow(final int row, final FloatVector vector) {
		if (rowCount <= row)
			throw new ArrayIndexOutOfBoundsException("row:" + row
					+ " > matrix's rowCount:" + rowCount);
		if (row < 0)
			throw new ArrayIndexOutOfBoundsException("row:" + row + " < 0");
		if (vector.getSize() < columnCount)
			vector.setSize(columnCount);
		//throw new ArrayIndexOutOfBoundsException("vector
		// size:"+vector.getSize()+" smaller than matrix's
		// columnCount:"+columnCount);

		// if may use package friendly accessibility, would do;
		System.arraycopy(elementData, row * columnCount, vector.elementData, 0,
				columnCount);
		return vector;
	}

	/**
	 * a safe copy is returned.
	 */
	public final FloatVector getRowv(final int row) {
		if (rowCount <= row)
			throw new ArrayIndexOutOfBoundsException("row:" + row
					+ " > matrix's rowCount:" + rowCount);
		if (row < 0)
			throw new ArrayIndexOutOfBoundsException("row:" + row + " < 0");

		FloatVector vector = new FloatVector(columnCount);
		System.arraycopy(elementData, row * columnCount, vector.elementData, 0,
				columnCount);

		return vector;
	}

	/**
	 * a safe copy is returned.
	 */
	public final float[] getRow(final int row) {
		if (rowCount <= row)
			throw new ArrayIndexOutOfBoundsException("row:" + row
					+ " > matrix's rowCount:" + rowCount);
		if (row < 0)
			throw new ArrayIndexOutOfBoundsException("row:" + row + " < 0");

		float[] ret = new float[columnCount];
		System.arraycopy(elementData, row * columnCount, ret, 0, columnCount);
		return ret;
	}

	/**
	 * Places the values of the specified column into the array parameter.
	 * 
	 * @param col
	 *            the target column number
	 * @param array
	 *            the array into which the column values will be placed
	 */
	public final float[] getColumn(final int col, final float[] array) {
		if (columnCount <= col)
			throw new ArrayIndexOutOfBoundsException("col:" + col
					+ " > matrix's columnCount:" + columnCount);
		if (col < 0)
			throw new ArrayIndexOutOfBoundsException("col:" + col + " < 0");
		if (array.length < rowCount)
			throw new ArrayIndexOutOfBoundsException("array.length:"
					+ array.length + " < matrix's rowCount=" + rowCount);

		for (int i = 0; i < rowCount; i++)
			array[i] = elementData[i * columnCount + col];
		return array;
	}

	/**
	 * Places the values of the specified column into the vector parameter.
	 * 
	 * @param col
	 *            the target column number
	 * @param vector
	 *            the vector into which the column values will be placed
	 */
	public final FloatVector getColumn(final int col, final FloatVector vector) {
		if (columnCount <= col)
			throw new ArrayIndexOutOfBoundsException("col:" + col
					+ " > matrix's columnCount:" + columnCount);
		if (col < 0)
			throw new ArrayIndexOutOfBoundsException("col:" + col + " < 0");
		if (vector.getSize() < rowCount)
			throw new ArrayIndexOutOfBoundsException("vector size:"
					+ vector.getSize() + " < matrix's rowCount:" + rowCount);
		for (int i = 0; i < rowCount; i++) {
			vector.setElement(i, elementData[i * columnCount + col]);
		}
		return vector;
	}

	public final FloatVector getColumnv(final int col) {
		if (col < 0)
			throw new ArrayIndexOutOfBoundsException("col:" + col + " < 0");

		FloatVector vector = new FloatVector(rowCount);
		System.arraycopy(elementData, col * rowCount, vector.elementData, 0,
				rowCount);

		return vector;
	}

	/**
	 * Places the values from this matrix into the matrix m1; m1 should be at
	 * least as large as this Matrix.
	 * 
	 * @param m1
	 *            The matrix that will hold the new values
	 */
	public final Matrix get(final MatrixMutable m1) {
		// need error check.
		final int m1_row_cnt = m1.getRowCount(), m1_col_cnt = m1
				.getColumnCount();
		if (m1_row_cnt < rowCount || m1_col_cnt < columnCount)
			throw new IllegalArgumentException(
					"m1 matrix is smaller than this matrix.");

		if (m1_col_cnt == columnCount) {
			System.arraycopy(elementData, 0, m1.getElementData(), 0, rowCount
					* columnCount);
		} else {
			for (int i = 0; i < rowCount; i++) {
				System.arraycopy(elementData, i * columnCount, m1
						.getElementData(), i * m1_col_cnt, columnCount);
			}
		}
		return m1;
	}

	/**
	 * dumps the contents of the values to a Sting on a single line (no line
	 * seperators)
	 */
	public String dump() {
		return dump("");
	}

	/**
	 * helper method that puts the contents of all values into a String
	 * 
	 * @param nl
	 *            the string that separates one row from the next
	 */
	public String dump(final String nl) {
		StringBuffer out = new StringBuffer("[");
		out.append(nl);

		for (int i = 0; i < rowCount; i++) {
			out.append("  [");
			for (int j = 0; j < columnCount; j++) {
				if (0 < j)
					out.append("\t");
				out.append(elementData[i * columnCount + j]);
			}
			if (i + 1 < rowCount) {
				out.append("]");
				out.append(nl);
			} else {
				out.append("] ]");
			}
		}
		return out.toString();
	}

	/**
	 * Returns a string that contains the values of this Matrix in a human
	 * readable form.
	 * 
	 * @return the String representation
	 */
	public final String toString() {
		String nl = System.getProperty("line.separator");
		return dump(nl);
	}

	/**
	 * returns a DataModel that defines the type of model this implementation
	 * represents
	 */
	public org.genepattern.data.DataModel getDataModel() {
		return DATA_MODEL;
	}

	/** returns the name of this matrix */
	public String getName() {
		return toString();
	}

	/**
	 * Returns a hash number based on the data values in this object. Two
	 * different Matrix objects with identical data values (ie, returns true for
	 * equals(Matrix) ) will return the same hash number. Two objects with
	 * different data members may return the same hash value, although this is
	 * not likely.
	 * 
	 * @return the integer hash value
	 */
	public final int hashCode() {
		if (hashcode == 0 || isMutable()) {
			int result = 17; // some prime #
			final int cnt = getRowCount() * getColumnCount();
			for (int i = 0; i < cnt; i++) {
				result = 37 * result + Float.floatToIntBits(elementData[i]);
			}
			if (isMutable()) // for mutable classes recalculate every time
				return result;
			else
				hashcode = result;
		}
		return hashcode;
	}

	//    public final int hashCode() {
	//        int hash = 0;
	//        for (int i = 0; i < rowCount*columnCount; i++) {
	//            
	//            long bits = Double.doubleToLongBits(elementData[i]);
	//            hash ^= (int)(bits ^ (bits >> 32));
	//        }
	//        return hash;
	//    }

	/**
	 * Returns true if all of the data members of Matrix4d m1 are equal to the
	 * corresponding data members in this Matrix4d.
	 * 
	 * @param m1
	 *            The matrix with which the comparison is made.
	 * @return true or false
	 */
	public final boolean equals(final Matrix m1) {
		if (m1 == this)
			return true;
		if (m1 == null || rowCount != m1.getRowCount()
				|| columnCount != m1.getColumnCount())
			return false;
		if (m1 instanceof DefaultMatrix)
			return java.util.Arrays.equals(elementData,
					((DefaultMatrix) m1).elementData);
		else { // this case could violate the transitive property of equality
			// is equavalent to being equals(Matrix) and the "if" part would be
			// equals(DefaultMatrix)
			final int row_cnt = rowCount, col_cnt = columnCount;
			for (int i = 0; i < row_cnt; i++)
				for (int j = 0; j < col_cnt; j++)
					if (elementData[i * col_cnt + j] != m1.getElement(i, j))
						return false;
		}
		return true;
		//        if (m1 == null || rowCount != m1.getRowCount() || columnCount !=
		// m1.getColumnCount())
		//            return false;
		//        if(m1 instanceof DefaultMatrix)
		//            return java.util.Arrays.equals(elementData,
		// ((DefaultMatrix)m1).elementData);
		//        else {
		//            final int row_cnt = rowCount, col_cnt = columnCount;
		//            for (int i = 0; i < row_cnt; i++)
		//                for (int j = 0; j < col_cnt; j++)
		//                    if (elementData[i*col_cnt + j] != m1.getElement(i, j))
		//                        return false;
		//        }
		//        return true;
	}

	/**
	 * Returns true if the Object o1 is of type Matrix and all of the data
	 * members of t1 are equal to the corresponding data members in this Matrix.
	 * 
	 * @param o1
	 *            the object with which the comparison is made.
	 */
	public final boolean equals(Object o1) {
		return (o1 instanceof Matrix) && equals((Matrix) o1);
	}

	//    /**
	//     * Returns true if the L-infinite distance between this matrix and
	//     * matrix m1 is less than or equal to the epsilon parameter,
	//     * otherwise returns false. The L-infinite distance is equal to
	//     * MAX[i=0,1,2, . . .n ; j=0,1,2, . . .n ; abs(this.m(i,j) - m1.m(i,j)] .
	//     * @deprecated The double version of this method should be used.
	//     * @param m1 The matrix to be compared to this matrix
	//     * @param epsilon the threshold value
	//     */
	//    public final boolean epsilonEquals(final Matrix m1, final float epsilon)
	// {
	//        if(m1.getRowCount() != rowCount)
	//            return false;
	//        if(m1.getColumnCount() != columnCount)
	//            return false;
	//        final int row_cnt = rowCount, col_cnt = columnCount;
	//        for (int r = 0; r < row_cnt; r++)
	//            for (int c = 0; c < col_cnt; c++)
	//                if (epsilon <
	//                 Math.abs(elementData[r*columnCount + c] - m1.getElement(r, c)))
	//                    return false;
	//
	//        return true;
	//    }
	/**
	 * Returns true if the L-infinite distance between this matrix and matrix m1
	 * is less than or equal to the epsilon parameter, otherwise returns false.
	 * The L-infinite distance is equal to MAX[i=0,1,2, . . .n ; j=0,1,2, . . .n ;
	 * abs(this.m(i,j) - m1.m(i,j)] .
	 * 
	 * @param m1
	 *            The matrix to be compared to this matrix
	 * @param epsilon
	 *            the threshold value
	 */
	public final boolean epsilonEquals(final Matrix m1, final double epsilon) {
		if (m1 == this)
			return true;
		if (m1.getRowCount() != this.getRowCount()
				|| m1.getColumnCount() != this.getColumnCount())
			return false;
		final int row_cnt = getRowCount(), col_cnt = getColumnCount();
		for (int r = 0; r < row_cnt; r++)
			for (int c = 0; c < col_cnt; c++)
				if (epsilon < Math.abs(elementData[r * col_cnt + c]
						- m1.getElement(r, c)))
					return false;

		return true;
	}

	/**
	 * Returns the trace of this matrix.
	 * 
	 * @return the trace of this matrix.
	 */
	public final double trace() {
		int min = rowCount < columnCount ? rowCount : columnCount;
		double trace = 0.0;
		for (int i = 0; i < min; i++)
			trace += elementData[i * columnCount + i];
		return trace;
	}

	protected final double getDiag(final int i) {
		return elementData[i * columnCount + i];
	}

	protected final double dpythag(final float a, final float b) {
		double absa = Math.abs(a);
		double absb = Math.abs(b);
		if (absa > absb) {
			if (absa == 0.0)
				return 0.0;
			double term = absb / absa;
			if (Math.abs(term) <= Double.MIN_VALUE)
				return absa;
			return (absa * Math.sqrt(1.0 + term * term));
		} else {
			if (absb == 0.0)
				return 0.0;
			double term = absa / absb;
			if (Math.abs(term) <= Double.MIN_VALUE)
				return absb;
			return (absb * Math.sqrt(1.0 + term * term));
		}
	}

	/**
	 * Siginifcance by row
	 * 
	 * XXX signfic needs review by pt
	 */
	public final FloatVector sigByRow(final double level) {
		FloatVector v = new FloatVector(rowCount);
		for (int r = 0; r < rowCount; r++) {
			v.setElement(r, this.getRowv(r).sig(level));
		}
		return v;
	}

	public final FloatVector sigByCol(final double level) {
		FloatVector v = new FloatVector(columnCount);
		for (int c = 0; c < columnCount; c++) {
			v.setElement(c, this.getColumnv(c).sig(level));
		}
		return v;
	}

	/**
	 * (the first max value) max element in the entire matrix absolute -> entri
	 * matrix else by row.
	 */
	public final Matrix.Cell max() {
		Matrix.Cell max = new Matrix.Cell();
		max.value = Float.MIN_VALUE;
		for (int r = 0; r < rowCount; r++) {
			for (int c = 0; c < columnCount; c++) {
				if (elementData[r * columnCount + c] > max.value) {
					max.row = r;
					max.col = c;
					max.value = elementData[r * columnCount + c];
				}
			}
		}
		return max;
	}

	/** the global first min value */
	public final Matrix.Cell min() {
		final Matrix.Cell min = new Matrix.Cell();
		min.value = Float.MAX_VALUE;
		for (int i = 0; i < rowCount; i++) {
			for (int j = 0; j < columnCount; j++) {
				if (elementData[i * columnCount + j] < min.value) {
					min.row = i;
					min.col = j;
					min.value = elementData[i * columnCount + j];
				}
			}
		}
		return min;
	}

	/** the min value by row for each row */
	public Matrix.Cell[] minByRow() {
		final int r_cnt = rowCount, c_cnt = columnCount;
		final Matrix.Cell[] mins = new Matrix.Cell[r_cnt];
		for (int r = 0; r < r_cnt; r++) {
			mins[r] = new Matrix.Cell();
			mins[r].value = Float.MAX_VALUE;
			for (int c = 0; c < c_cnt; c++) {
				final int index = getIndex(r, c);
				if (elementData[index] < mins[r].value) {
					mins[r].row = r;
					mins[r].col = c;
					mins[r].value = elementData[index];
				}
			}
		}
		return mins;
	}

	/** much more efficent than getting each row and calc'ing max */
	public final Matrix.Cell[] maxByRow() {
		final Matrix.Cell[] maxes = new Matrix.Cell[rowCount];
		for (int r = 0; r < rowCount; r++) {
			maxes[r] = new Matrix.Cell();
			maxes[r].value = Float.MIN_VALUE;
			for (int c = 0; c < columnCount; c++) {
				if (elementData[r * columnCount + c] > maxes[r].value) {
					maxes[r].row = r;
					maxes[r].col = c;
					maxes[r].value = elementData[r * columnCount + c];
				}
			}
		}
		return maxes;
	}

	/**
	 * absolute binning --- entrie matrix adds to the specified ranges.
	 */
	public final void bin(final Range[] ranges) {
		for (int i = 0; i < rowCount * columnCount; i++) {
			for (int j = 0; j < ranges.length; j++) {
				if (ranges[j].isMember(elementData[i]))
					ranges[j].increment();
			}
		}
	}

	public final void bin(final java.util.List ranges) {
		for (int i = 0; i < rowCount * columnCount; i++) {
			for (int j = 0; j < ranges.size(); j++) {
				Range range = (Range) ranges.get(j);
				if (range.isMember(elementData[i]))
					range.increment();
			}
		}
	}

	/**
	 * Discretize the matrix into the specified ranges.
	 * 
	 * @return A whole new discretized matrix. The floats are not shared. Matrix
	 *         will contain value from 0 to ranges-1
	 *  
	 */
	// More efficient to create a new matrix here (note diff with the other
	// discz api)
	// Iterate through matrix assigning each element to one and only one range.
	// Unlike binning, the number of discretized(binned) elements must exactly
	// equal the numb of elements in the matrix.
	public final Matrix discretize(final Range[] ranges) {

		DefaultMatrix newmatrix = new DefaultMatrix(getRowCount(),
				getColumnCount(), false);
		for (int i = 0; i < rowCount * columnCount; i++) {
			for (int j = 0; j < ranges.length; j++) {
				if (ranges[j].isMember(elementData[i])) { // checking this
														  // matrix
					newmatrix.elementData[i] = j; // but assigning to new matrix
					break;
				}
			}
		}

		return newmatrix;
	}

	/**
	 * 
	 * equalk numbf of lements actually equal numb of DIFF(float) elements.
	 * changes the matrix "in place" matrix must be not be immutable for this to
	 * work
	 * 
	 * each disc has classdensity elements
	 * 
	 * Sort the array Make it unique Now, the number of ranges = uniq.length /
	 * classdensity Create the ranges Iterate through untouched (unsorted,
	 * ununiqueized) array and file ranges.
	 *  
	 */
	public final Matrix discretize(final int classdensity) {

		DefaultMatrix newmatrix = new DefaultMatrix(this);

		log.debug("Starting uniq for discretize");
		float[] uniq = ArrayUtils.unique(newmatrix.elementData);
		log.debug("Done uniq for discretize");
		Arrays.sort(uniq);
		log.debug("Done sort for discretize");

		Range[] ranges = Range.createRanges(classdensity, uniq);

		log.debug("Number of ranges = " + ranges.length);

		for (int i = 0; i < rowCount * columnCount; i++) {
			for (int j = 0; j < ranges.length; j++) {
				if (ranges[j].isMember(this.elementData[i])) {
					newmatrix.elementData[i] = j;
					break;
				}
			}
		}

		return newmatrix;
	}

	// fields

	private static final long serialVersionUID = 8728211089911142600L;

	/*
	 * (javax.vecmath note) Implementation note: The size of the matrix does NOT
	 * automaticly grow. It does only when setSize is called. I believe this is
	 * the spec and makes less confusion, less user bugs.
	 */

	/**
	 * IMP IMP: if any fields are added to this class, make sure to update the
	 * constructors and the set(Matrix method
	 */

	/**
	 * The data of the Matrix.(1-D array. The (i,j) element is stored in
	 * elementData[i*columnCount + j])
	 */
	private float elementData[];

	/** The number of rows in this matrix. */
	private int rowCount;

	/** The number of columns in this matrix. */
	private int columnCount;

	/** can the subclass change the internal values */
	private final boolean subclass_can_change;

	protected static final transient XLogger log = XLogger
			.getLogger(Matrix.class);

	/** the hash code value or 0 if not calculated */
	private int hashcode = 0;

	// I N N E R C L A S S E S

	/**
	 * Represents a single matrix cell.
	 */
	public static class Cell implements Serializable {
		private static final long serialVersionUID = 991018889913842280L;

		public int row;

		public int col;

		public float value;
	} // End Cell

} // End Matrix
