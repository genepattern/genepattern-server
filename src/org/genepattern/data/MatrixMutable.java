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

import org.genepattern.util.XLogger;

//import edu.mit.genome.util.ImmutedException;

/**
 * This is the mutable variant of the Matrix class.
 * 
 * Lots of code/ideas copied form Kenji Hiranabe, Eiwa System Management, Inc.
 * GMatrix part of the javax.vecmath library (unoffical (his) impl)
 * http://java.sun.com/products/java-media/3D/forDevelopers/j3dapi/index.html
 * 
 * as note: made several of the row related methods unfinal as need to override
 * 
 * @author Aravind Subramanian
 * @version %I%, %G%
 */

public class MatrixMutable extends DefaultMatrix {

	private static final long serialVersionUID = 8728211089911142600L;

	/*
	 * (javax.vecmath note) Implementation note: The size of the matrix does NOT
	 * automaticly grow. It does only when setSize is called. I believe this is
	 * the spec and makes less confusion, less user bugs.
	 */

	/**
	 * IMP IMP: if any fields are added to this class, make sure to update the
	 * constructors and the set(Matrix) method
	 */

	/**
	 * The data of the Matrix.(1-D array. The (i,j) element is stored in
	 * elementData[i*getColumnCount() + j]) Note this is just a copy of the
	 * super's variable done for convience
	 */
	protected float elementData[];

	private static final transient XLogger log = XLogger
			.getLogger(MatrixMutable.class);

	/**
	 * Class constructor Must call set() after this form of the constructor.
	 */
	public MatrixMutable() {
		super(true);
		elementData = getInternalElementData();
	}

	/**
	 * Constructs an getRowCount() by getColumnCount() all zero matrix. (as
	 * change) Note that even though row and column numbering begins with zero,
	 * getRowCount() and getColumnCount() will be one larger than the maximum
	 * possible matrix index values.
	 * 
	 * @param nrows
	 *            number of rows in this matrix.
	 * @param ncols
	 *            number of columns in this matrix.
	 */
	public MatrixMutable(int nrows, int ncols) {
		super(nrows, ncols, true);
		elementData = getInternalElementData();
	}

	/**
	 * Constructs an getRowCount() by getColumnCount() matrix initialized to the
	 * values in the matrix array. The array values are copied in one row at a
	 * time in row major fashion. The array should be at least
	 * getRowCount()*getColumnCount() in length. Note that even though row and
	 * column numbering begins with zero, getRowCount() and getColumnCount()
	 * will be one larger than the maximum possible matrix index values.
	 * 
	 * @param nrows
	 *            number of rows in this matrix.
	 * @param ncols
	 *            number of columns in this matrix.
	 * @param matrix
	 *            a 1D array that specifies a matrix in row major fashion
	 */
	public MatrixMutable(int nrows, int ncols, float[] matrix) {
		super(nrows, ncols, matrix, true);
		elementData = getInternalElementData();
	}

	public MatrixMutable(Matrix matrixA, Matrix matrixB) {
		super(new Matrix[] { matrixA, matrixB }, true);
		elementData = getInternalElementData();
	}

	/**
	 * data is not shared matrices must be of same number of columns (nrows can
	 * be arb - thats the point!)
	 */
	public MatrixMutable(Matrix[] matrixs) {
		super(matrixs, true);
		elementData = getInternalElementData();
	}

	/**
	 * Constructs a new Matrix and copies the initial values from the parameter
	 * matrix.
	 * 
	 * @param matrix
	 *            the source of the initial values of the new Matrix
	 * 
	 * as added: if shared values, then this matrix shares the elements with the
	 * specified amtric else the array is systemarrycopied
	 */
	public MatrixMutable(Matrix matrix) {
		super(matrix, true);
		elementData = getInternalElementData();
	}

	/**
	 * returns a cloned mutable matrix
	 */
	public Matrix deepClone() {
		log.debug("Making deepClone of Matrix");
		DefaultMatrix matrix = new DefaultMatrix(this);
		return matrix;
	}

	/** gets the elements data */
	protected final float[] getElementData() {
		return elementData;
	}

	/** helper method */
	private final void setData(float[] data) {
		this.setInternalElementData(data);
		this.elementData = data;
	}

	/**
	 * Changes the size of this matrix dynamically. If the size is increased no
	 * data values will be lost. If the size is decreased, only those data
	 * values whose matrix positions were eliminated will be lost.
	 * 
	 * @param nrows
	 *            number of desired rows in this matrix
	 * @param ncols
	 *            number of desired columns in this matrix
	 */
	public final void setSize(final int nrows, final int ncols) {

		if (nrows < 0)
			throw new NegativeArraySizeException("nrows < 0");
		if (ncols < 0)
			throw new NegativeArraySizeException("ncols < 0");

		final int oldRowCnt = getRowCount();
		final int oldColCnt = getColumnCount();
		final int oldSize = oldRowCnt * oldColCnt;
		final int newSize = nrows * ncols;
		final float[] oldData = elementData;
		float[] newData = null;

		setInternalRowCount(nrows);
		setInternalColCount(ncols);

		if (oldColCnt == ncols) {
			// no need to reload elements.
			if (nrows <= oldRowCnt)
				return;

			newData = new float[newSize]; // don't create this unless have to
			// copy the old data.
			System.arraycopy(oldData, 0, newData, 0, oldSize);

		} else {
			newData = new float[newSize];
			for (int i = 0; i < oldRowCnt; i++)
				System.arraycopy(oldData, i * oldColCnt, newData, i * ncols,
						oldColCnt);
		}
		setData(newData);
	}

	/**
	 * removes the rows specified as indices in the array
	 */
	public final void removeRows(final int[] row_inds) {
		// assuming later indices referre to later row indices
		// reverse loop should be less work for removeRow(int)
		for (int i = row_inds.length - 1; i >= 0; i--) { // rev loop
			removeRow(row_inds[i]);
		}
	}

	/**
	 * removes the rows specified as indices in the RangeInt
	 */
	public final void removeRows(final IntRanges ranges) {
		// reverse loop is be less work for removeRow(int) because it removes
		// later elements first
		for (IntRanges.ReverseIntIterator iter = ranges.getReverseIntIterator(); iter
				.hasMore();) {
			removeRow(iter.next());
		}
	}

	/**
	 * removes a row shifts the data up and doesn't create a new array
	 */
	public final void removeRow(final int row) {
		final int row_cnt = getRowCount(), col_cnt = getColumnCount();
		if (row + 1 < row_cnt) { // not last row
			final int start = getIndex(row, 0); // inclusive
			final int end = getIndex(row, col_cnt); // inclusive
			final int len = (row_cnt * col_cnt) - end;

			//System.out.println("removing row "+row+" dim(r,c)=("+row_cnt+",
			// "+col_cnt+") start="+start+" start next row="+end+" len="+len);
			System.arraycopy(elementData, end, elementData, start, len);
		}
		this.setInternalRowCount(getRowCount() - 1);
	}

	//    /**
	//     * removes a column
	//     * shifts the data up and doesn't create a new array
	//     */
	//    public final void removeColumn(final int row) {
	//        final int row_cnt = getRowCount(), col_cnt = getColumnCount();
	//        if(row + 1 < row_cnt) { // not last row
	//            final int start = getIndex(row, 0); // inclusive
	//            final int end = getIndex(row, col_cnt); // inclusive
	//            final int len = (row_cnt * col_cnt) - end;
	//            
	//            //System.out.println("removing row "+row+" dim(r,c)=("+row_cnt+",
	// "+col_cnt+") start="+start+" start next row="+end+" len="+len);
	//            System.arraycopy(elementData, end, elementData, start, len);
	//        }
	//        this.setInternalRowCount(getRowCount() - 1);
	//    }
	/**
	 * removes the columns specified as indices in the RangeInt
	 */
	public final void removeColumns(final IntRanges ranges) {
		// reverse loop is less work for removeColumns(RangeInt) because it
		// removes later elements first
		for (IntRanges.ReverseRangeIterator iter = ranges
				.getReverseRangeIterator(); iter.hasMore();) {
			removeColumns(iter.next());
		}
	}

	/**
	 * removes the columns defined by the RangeInt without creating another
	 * array
	 */
	public final void removeColumns(final RangeInt range) {
		final int row_cnt = getRowCount(), col_cnt = getColumnCount();
		final int c1 = range.min(); // first column
		final int c2 = range.max(); // last column to remove
		final int diff = c2 - c1 + 1;
		final int old_size = row_cnt * col_cnt;
		//System.out.println(" dim(r,c)=("+row_cnt+", "+col_cnt+")");
		for (int r = row_cnt - 1; r >= 0; r--) { // rev loop
			final int start = getIndex(r, c1); // inclusive
			final int end = start + diff; // inclusive
			final int len = old_size - end;

			//System.out.println("removing columns "+range+" start="+start+"
			// end="+end+" len="+len);
			System.arraycopy(elementData, end, elementData, start, len);
		}
		this.setInternalColCount(col_cnt - diff);
	}

	/**
	 * Sets the value of this matrix to the values found in the array parameter.
	 * The values are copied in one row at a time, in row major fashion. The
	 * array should be at least equal in length to the number of matrix rows
	 * times the number of matrix columns in this matrix.
	 * 
	 * @param matrix
	 *            the row major source array
	 */
	public final void set(float matrix[]) {
		int size = getRowCount() * getColumnCount();
		System.arraycopy(matrix, 0, elementData, 0, size);
	}

	/**
	 * Copies all the values in the array of FloatVector objects to the matrix
	 * The lengths of all the FloatVectors should be equal to the number of
	 * columns since each FloatVector is assumed to be a row.
	 */
	public final void setRowsTo(final FloatVector[] vecs) {
		final int num_rows = vecs.length;
		for (int i = 0; i < num_rows; i++) {
			setRow(i, vecs[i]);
		}
	}

	/**
	 * Sets the value of this matrix to the values found in matrix m1.
	 * 
	 * @param m1
	 *            the source matrix
	 */
	public final void set(Matrix m1, boolean sharedvalues) {
		// This implementation is in 'no automatic size grow' policy.
		// When size mismatch, exception will be thrown from the below.
		if (m1.getRowCount() < getRowCount()
				|| m1.getColumnCount() < getColumnCount())
			throw new ArrayIndexOutOfBoundsException(
					"m1 smaller than this matrix");

		//        if (sharedvalues) {
		//            setData(m1.elementData);
		//            setInternalRowCount(this, m1.getRowCount());
		//            setInternalColCount(this, m1.getColumnCount());
		//        }
		//        else {
		//            m1.get(this);//System.arraycopy(m1.elementData, 0, elementData, 0,
		// getRowCount()*getColumnCount());
		//        }
		// FIXME shared not working yet
		m1.get(this);
	}

	/**
	 * Modifies the value at the specified row and column of this matrix.
	 * 
	 * @param row
	 *            the row number to be modified (zero indexed)
	 * @param column
	 *            the column number to be modified (zero indexed)
	 * @param value
	 *            the new matrix element value
	 */
	public final void setElement(int row, int column, float value) {
		if (getRowCount() <= row)
			throw new ArrayIndexOutOfBoundsException("row:" + row
					+ " > matrix's getRowCount():" + getRowCount());
		if (row < 0)
			throw new ArrayIndexOutOfBoundsException("row:" + row + " < 0");
		if (getColumnCount() <= column)
			throw new ArrayIndexOutOfBoundsException("column:" + column
					+ " > matrix's getColumnCount():" + getColumnCount());
		if (column < 0)
			throw new ArrayIndexOutOfBoundsException("column:" + column
					+ " < 0");

		elementData[row * getColumnCount() + column] = value;
	}

	/**
	 * Copy the values from the array into the specified row of this matrix.
	 * 
	 * @param row
	 *            the row of this matrix into which the array values will be
	 *            copied.
	 * @param array
	 *            the source array
	 */
	public final void setRow(int row, float array[]) {
		final int col_cnt = getColumnCount();

		if (getRowCount() <= row)
			throw new ArrayIndexOutOfBoundsException("row:" + row
					+ " > matrix's getRowCount():" + getRowCount());
		if (row < 0)
			throw new ArrayIndexOutOfBoundsException("row:" + row + " < 0");
		if (array.length < col_cnt)
			throw new ArrayIndexOutOfBoundsException("array length:"
					+ array.length + " < matrix's getColumnCount()="
					+ getColumnCount());

		System.arraycopy(array, 0, elementData, row * col_cnt, col_cnt);
	}

	/**
	 * Copy the values from the array into the specified row of this matrix.
	 * 
	 * @param row
	 *            the row of this matrix into which the vector values will be
	 *            copied.
	 * @param vector
	 *            the source vector
	 */
	public final void setRow(final int row, final FloatVector vector) {
		final int col_cnt = getColumnCount();

		if (getRowCount() <= row)
			throw new ArrayIndexOutOfBoundsException("row:" + row
					+ " => matrix's getRowCount():" + getRowCount());
		if (row < 0)
			throw new ArrayIndexOutOfBoundsException("row:" + row + " < 0");
		final int vecSize = vector.getSize();
		if (vecSize < col_cnt)
			throw new ArrayIndexOutOfBoundsException("vector's size:" + vecSize
					+ " < matrix's getColumnCount()=" + getColumnCount());

		System.arraycopy(vector.elementData, 0, elementData, row * col_cnt,
				col_cnt);
		//        for (int i = 0; i < getColumnCount(); i++) {
		//            elementData[row*getColumnCount() + i] = vector.getElement(i);
		//            // if may use package friendly accessibility, would do;
		//            // System.arraycopy(vector.elementData, 0, elementData,
		// row*getColumnCount(), getColumnCount());
		//        }
	}

	/**
	 * Copy the values from the array into the specified column of this matrix.
	 * 
	 * @param col
	 *            the column of this matrix into which the array values will be
	 *            copied.
	 * @param array
	 *            the source array
	 */
	public final void setColumn(int col, float array[]) {

		if (getColumnCount() <= col)
			throw new ArrayIndexOutOfBoundsException("col:" + col
					+ " > matrix's getColumnCount()=" + getColumnCount());
		if (col < 0)
			throw new ArrayIndexOutOfBoundsException("col:" + col + " < 0");

		if (array.length < getRowCount())
			throw new ArrayIndexOutOfBoundsException("array length:"
					+ array.length + " < matrix's getRowCount():"
					+ getRowCount());
		for (int i = 0; i < getRowCount(); i++)
			elementData[i * getColumnCount() + col] = array[i];
	}

	/**
	 * Copy the values from the array into the specified column of this matrix.
	 * 
	 * @param col
	 *            the column of this matrix into which the vector values will be
	 *            copied.
	 * @param vector
	 *            the source vector
	 */
	public final void setColumn(int col, FloatVector vector) {

		if (getColumnCount() <= col)
			throw new ArrayIndexOutOfBoundsException("col:" + col
					+ " > matrix's getColumnCount()=" + getColumnCount());
		if (col < 0)
			throw new ArrayIndexOutOfBoundsException("col:" + col + " < 0");

		final int vecSize = vector.getSize();
		if (vecSize < getRowCount())
			throw new ArrayIndexOutOfBoundsException("vector size:" + vecSize
					+ " < matrix's getRowCount()=" + getRowCount());
		for (int i = 0; i < getRowCount(); i++)
			elementData[i * getColumnCount() + col] = vector.getElement(i);
	}

	/**
	 * Multiplies the transpose of matrix m1 times the transpose of matrix m2,
	 * and places the result into this.
	 * 
	 * @param m1
	 *            The matrix on the left hand side of the multiplication
	 * @param m2
	 *            The matrix on the right hand side of the multiplication
	 */
	public final void mulTransposeBoth(Matrix m1, Matrix m2) {
		mul(m2, m1);
		transpose();
	}

	/**
	 * Multiplies matrix m1 times the transpose of matrix m2, and places the
	 * result into this.
	 */
	public final void mulTransposeRight(Matrix m1, Matrix m2) {
		if (m1.getColumnCount() != m2.getColumnCount()
				|| getRowCount() != m1.getRowCount()
				|| getColumnCount() != m2.getRowCount())
			throw new ArrayIndexOutOfBoundsException("matrices mismatch");
		final int row_cnt = getRowCount(), col_cnt = getColumnCount();
		final int m2_col_cnt = m2.getColumnCount(), m1_col_cnt = m1
				.getColumnCount();

		for (int r = 0; r < row_cnt; r++) {
			for (int c = 0; c < col_cnt; c++) {
				float sum = (float) 0.0;
				for (int k = 0; k < m1_col_cnt; k++)
					sum += m1.getElementAtIndex(r * m1_col_cnt + k)
							* m2.getElementAtIndex(c * m2_col_cnt + k);
				this.elementData[r * col_cnt + c] = sum;
			}
		}
	}

	/**
	 * Multiplies the transpose of matrix m1 times the matrix m2, and places the
	 * result into this.
	 * 
	 * @param m1
	 *            The matrix on the left hand side of the multiplication
	 * @param m2
	 *            The matrix on the right hand side of the multiplication
	 */
	public final void mulTransposeLeft(Matrix m1, Matrix m2) {
		transpose(m1);
		mul(m2);
	}

	/**
	 * Transposes this matrix in place.
	 */
	public final void transpose() {
		final int row_cnt = getRowCount(), col_cnt = getColumnCount();
		// FIXME this is very inefficeint making another whole array
		final float[] new_data = new float[elementData.length];
		for (int r = 0; r < row_cnt; r++) {
			for (int c = 0; c < col_cnt; c++) {
				final int index_a = r * col_cnt + c, index_b = c * row_cnt + r;
				//final int index_a = getIndex(r, c), index_b = getIndex(c, r);
				// // cannot do it this way
				new_data[index_b] = elementData[index_a];
			}
		}
		// bad for gc also other objects could have a ref to current elementData
		//this.setData(new_data);
		// copy instead
		System.arraycopy(new_data, 0, elementData, 0, elementData.length);
		this.setInternalColCount(row_cnt);
		this.setInternalRowCount(col_cnt);
	}

	/**
	 * Places the matrix values of the transpose of matrix m1 into this matrix.
	 * <p>
	 * 
	 * @param m1
	 *            the matrix to be transposed (but not modified)
	 */
	public final void transpose(Matrix m1) {
		set(m1, false);
		transpose();
	}

	/**
	 * Sets this matrix to a uniform scale matrix; all of the values are reset.
	 * 
	 * @param scale
	 *            The new scale value
	 */
	public final void setScale(final float scale) {
		setZero();
		final int num_col = getColumnCount();
		final int min = getRowCount() < getColumnCount() ? getRowCount()
				: getColumnCount();
		for (int i = 0; i < min; i++)
			elementData[i * num_col + i] = scale;
	}

	protected final void setDiag(int i, float value) {
		elementData[i * getColumnCount() + i] = value;
	}

	protected final void swapRows(int i, int j) {

		for (int k = 0; k < getColumnCount(); k++) {
			float tmp = elementData[i * getColumnCount() + k];
			elementData[i * getColumnCount() + k] = elementData[j
					* getColumnCount() + k];
			elementData[j * getColumnCount() + k] = tmp;
		}
	}

	// column and row math methods
	/** adds a constant to all elements in the row */
	public final void addToRow(final int row, final float value) {
		final int start = getIndex(row, 0);
		final int col_end = start + getColumnCount();
		for (int index = start; index < col_end; index++) {
			elementData[index] += value;
		}
	}

	/** subtracts a constant from all elements in the row */
	public final void subFromRow(final int row, final float value) {
		final int start = getIndex(row, 0);
		final int col_end = start + getColumnCount();
		for (int index = start; index < col_end; index++) {
			elementData[index] -= value;
		}
	}

	/** multiplys a constant to all elements in the row */
	public final void mulToRow(final int row, final float value) {
		final int start = getIndex(row, 0);
		final int col_end = start + getColumnCount();
		for (int index = start; index < col_end; index++) {
			elementData[index] *= value;
		}
	}

	/** divides each element in the row by the constant */
	public final void divideRowBy(final int row, final float value) {
		final int start = getIndex(row, 0);
		final int col_end = start + getColumnCount();
		for (int index = start; index < col_end; index++) {
			elementData[index] /= value;
		}
	}

	// end column and row math
	/**
	 * Sets the value of this matrix to the result of multiplying itself with
	 * matrix m1 (this = this * m1).
	 * 
	 * @param m1
	 *            the other matrix
	 */
	public final void mul(Matrix m1) {
		// alias-safe.
		mul(this, m1);
	}

	/**
	 * Sets the value of this matrix to the result of multiplying the two
	 * argument matrices together (this = m1 * m2).
	 * 
	 * @param m1
	 *            the first matrix
	 * @param m2
	 *            the second matrix
	 */
	public final void mul(Matrix m1, Matrix m2) {

		// for alias-safety, decided to new double
		// [getColumnCount()*getRowCount()].
		// Is there any good way to avoid this big new ?
		if (getRowCount() != m1.getRowCount())
			throw new ArrayIndexOutOfBoundsException("getRowCount():"
					+ getRowCount() + " != m1.getRowCount():"
					+ m1.getRowCount());

		if (getColumnCount() != m2.getColumnCount())
			throw new ArrayIndexOutOfBoundsException("getColumnCount():"
					+ getColumnCount() + " != m2.getColumnCount():"
					+ m2.getColumnCount());

		if (m1.getColumnCount() != m2.getRowCount())
			throw new ArrayIndexOutOfBoundsException("m1.getColumnCount():"
					+ m1.getColumnCount() + " != m2.getRowCount():"
					+ m2.getRowCount());

		float[] newData = new float[getColumnCount() * getRowCount()];
		for (int i = 0; i < getRowCount(); i++) {
			for (int j = 0; j < getColumnCount(); j++) {
				float sum = (float) 0.0;
				for (int k = 0; k < m1.getColumnCount(); k++)
					sum += m1.getElementAtIndex(i * m1.getColumnCount() + k)
							* m2.getElementAtIndex(k * m2.getColumnCount() + j);
				newData[i * getColumnCount() + j] = sum;
			}
		}
		elementData = newData;
	}

	/**
	 * Computes the outer product of the two vectors; multiplies the the first
	 * vector by the transpose of the second vector and places the matrix result
	 * into this matrix. This matrix must be as big or bigger than
	 * getSize(v1)xgetSize(v2).
	 * 
	 * @param v1
	 *            the first vector, treated as a row vector
	 * @param v2
	 *            the second vector, treated as a column vector
	 */
	public final void mul(FloatVector v1, FloatVector v2) {
		if (getRowCount() < v1.getSize())
			throw new IllegalArgumentException("getRowCount():" + getRowCount()
					+ " < v1.getSize():" + v1.getSize());
		if (getColumnCount() < v2.getSize())
			throw new IllegalArgumentException("getColumnCount():"
					+ getColumnCount() + " < v2.getSize():" + v2.getSize());

		for (int i = 0; i < getRowCount(); i++)
			for (int j = 0; j < getColumnCount(); j++)
				elementData[i * getColumnCount() + j] = v1.getElement(i)
						* v2.getElement(j);
	}

	/**
	 * Sets the value of this matrix to sum of itself and matrix m1.
	 * 
	 * @param m1
	 *            the other matrix
	 */
	public final void add(Matrix m1) {
		if (getRowCount() != m1.getRowCount()
				|| getColumnCount() != m1.getColumnCount())
			throw new IllegalArgumentException("this:(" + getRowCount() + "x"
					+ getColumnCount() + ") != m1:(" + m1.getRowCount() + "x"
					+ m1.getColumnCount() + ").");

		for (int i = 0; i < getRowCount() * getColumnCount(); i++)
			elementData[i] += m1.getElementAtIndex(i);
	}

	/**
	 * Sets the value of this matrix to the matrix sum of matrices m1 and m2.
	 * 
	 * @param m1
	 *            the first matrix
	 * @param m2
	 *            the second matrix
	 */
	public final void add(Matrix m1, Matrix m2) {

		if (getRowCount() != m1.getRowCount()
				|| getColumnCount() != m1.getColumnCount())
			throw new IllegalArgumentException("this:(" + getRowCount() + "x"
					+ getColumnCount() + ") != m1:(" + m1.getRowCount() + "x"
					+ m1.getColumnCount() + ").");
		if (getRowCount() != m2.getRowCount()
				|| getColumnCount() != m2.getColumnCount())
			throw new IllegalArgumentException("this:(" + getRowCount() + "x"
					+ getColumnCount() + ") != m2:(" + m2.getRowCount() + "x"
					+ m2.getColumnCount() + ").");

		final int cnt = getRowCount() * getColumnCount();
		for (int i = 0; i < cnt; i++)
			elementData[i] = m1.getElementAtIndex(i) + m2.getElementAtIndex(i);
	}

	/**
	 * Sets the value of this matrix to the matrix difference of itself and
	 * matrix m1 (this = this - m1).
	 * 
	 * @param m1
	 *            the other matrix
	 */
	public final void sub(Matrix m1) {
		if (getRowCount() != m1.getRowCount()
				|| getColumnCount() != m1.getColumnCount())
			throw new IllegalArgumentException("this:(" + getRowCount() + "x"
					+ getColumnCount() + ") != m1:(" + m1.getRowCount() + "x"
					+ m1.getColumnCount() + ").");
		for (int i = 0; i < getRowCount() * getColumnCount(); i++)
			elementData[i] -= m1.getElementAtIndex(i);
	}

	/**
	 * Sets the value of this matrix to the matrix difference of matrices m1 and
	 * m2 (this = m1 - m2).
	 * 
	 * @param m1
	 *            the first matrix
	 * @param m2
	 *            the second matrix
	 */
	public final void sub(Matrix m1, Matrix m2) {
		if (getRowCount() != m1.getRowCount()
				|| getColumnCount() != m1.getColumnCount())
			throw new IllegalArgumentException("this:(" + getRowCount() + "x"
					+ getColumnCount() + ") != m1:(" + m1.getRowCount() + "x"
					+ m1.getColumnCount() + ").");
		if (getRowCount() != m2.getRowCount()
				|| getColumnCount() != m2.getColumnCount())
			throw new IllegalArgumentException("this:(" + getRowCount() + "x"
					+ getColumnCount() + ") != m2:(" + m2.getRowCount() + "x"
					+ m2.getColumnCount() + ").");

		for (int i = 0; i < getRowCount() * getColumnCount(); i++)
			elementData[i] = m1.getElementAtIndex(i) - m2.getElementAtIndex(i);
	}

	/**
	 * Negates the value of this matrix: this = -this.
	 */
	public final void negate() {

		for (int i = 0; i < getRowCount() * getColumnCount(); i++)
			elementData[i] = -elementData[i];
	}

	/**
	 * Sets the value of this matrix to the negation of the Matrix parameter.
	 * 
	 * @param m1
	 *            The source matrix
	 */
	public final void negate(Matrix m1) {

		set(m1, false);
		negate();
	}

	/**
	 * Sets all the values in this matrix to zero.
	 */
	public final void setZero() {
		final int limit = getRowCount() * getColumnCount();
		for (int i = 0; i < limit; i++)
			elementData[i] = 0.0f;
	}

	/**
	 * Subtracts this matrix from the identity matrix and puts the values back
	 * into this (this = I - this).
	 */
	public final void identityMinus() {
		negate();
		int min = getRowCount() < getColumnCount() ? getRowCount()
				: getColumnCount();
		for (int i = 0; i < min; i++)
			elementData[i * getColumnCount() + i] += (float) 1.0;
	}

	/**
	 * Sets this Matrix to the identity matrix.
	 */
	/*
	 * public final void setIdentity() {
	 * 
	 * setZero(); int min = getRowCount() < getColumnCount() ? getRowCount() :
	 * getColumnCount(); for (int i = 0; i < min; i++)
	 * elementData[i*getColumnCount() + i] = (float)1.0; }
	 */
}