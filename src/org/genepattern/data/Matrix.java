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

import org.genepattern.data.DataModel;
import org.genepattern.util.ArrayUtils;
import org.genepattern.util.XLogger;

//import edu.mit.genome.util.ImmutedException;

/**
 * This interface defines what a matrix capabilites will be.
 * There are no setXXX(...) methods.
 * See MatrixMutable for an implementation that is mutable.
 *
 *  * Lots of code/ideas copied form Kenji Hiranabe, Eiwa System Management, Inc.
 * GMatrix part of the javax.vecmath library (unoffical (his) impl)
 * http://java.sun.com/products/java-media/3D/forDevelopers/j3dapi/index.html
 *
 *
 * @author Aravind Subramanian, Keith Ohm
 * @version %I%, %G%
 */

public interface Matrix extends org.genepattern.data.DataObjector, Serializable {
    
    // fields
    /** the DataModel that should be returned from getDataModel() */
    public static final DataModel DATA_MODEL = new DataModel("Matrix", 2);

    // method signature
    /** get a new Matrix that is the transpose of this matrix.
     * @return Matrix
     */
    public Matrix getTransposedMatrix();
    /** returns true if this Matrix is unsafe and its values can be changed
     * @return true  if this is a mutable data object
     */
    public boolean isMutable();
    
    /**
     * Copies a sub-matrix derived from this matrix into the target matrix.
     * The upper left of the sub-matrix is located at (rowSource, colSource);
     * the lower right of the sub-matrix is located at
     * (lastRowSource,lastColSource).  The sub-matrix is copied into the
     * the target matrix starting at (rowDest, colDest).
     * @param rowSource the top-most row of the sub-matrix
     * @param colSource the left-most column of the sub-matrix
     * @param numRow the number of rows in the sub-matrix
     * @param numCol the number of columns in the sub-matrix
     * @param rowDest the top-most row of the position of the copied sub-matrix
     *                  within the target matrix
     * @param colDest the left-most column of the position of the copied sub-matrix
     *                  within the target matrix
     * @param target the matrix into which the sub-matrix will be copied
     */
    public void copySubMatrix(int rowSource, int colSource, int numRow, int numCol,
                              int rowDest, int colDest, MatrixMutable target);
    /** creates a new DefaultMatrix out of the row and column indices
     * Uses all of the columns or rows if the cols or rows array, respectively, is null
     * or size 0.
     * @param rows array of row indices
     * @param cols array of olumn indices
     * @return Matrix
     */
    public Matrix createSubMatrix(int[] rows, int[] cols);
    /**
     * Returns the number of rows in this matrix.
     * @return number of rows in this matrix
     */
    public int getRowCount();
    
    /**
     * Returns the number of colmuns in this matrix.
     * @return number of columns in this matrix
     */
    public int getColumnCount() ;
    /**
     * Retrieves the value at the specified row and column of this matrix.
     * @param row the row number to be retrieved (zero indexed)
     * @param column the column number to be retrieved (zero indexed)
     * @return the value at the indexed element
     */
    public float getElement(int row, int column);
    /** This should not normaly be used.
     * @param i index into the data array
     * @return float
     * @see Matrix#getElement(int, int)
     */
    public float getElementAtIndex(int i);
    
    /** Places the values of the specified row into the array parameter.
     * @param row the target row number
     * @param array the array into which the row values will be placed
     * @return float[]
     */
    public float[] getRow(int row, float array[]);
    /** Places the values of the specified row into the vector parameter.
     * @param row the target row number
     * @param vector the vector into which the row values will be placed
     * @return FloatVector
     */
    public FloatVector getRow(int row, FloatVector vector);
     /** a safe copy is returned.
      * @param row the row index
      * @return FloatVector
      */
    public FloatVector getRowv(int row);
    /** a safe copy is returned.
     * @param row the row index
     * @return float[]
     */
    public float[] getRow(int row);
    /** Places the values of the specified column into the array parameter.
     * @param col the target column number
     * @param array the array into which the column values will be placed
     * @return float[]
     */
    public float[] getColumn(int col, float array[]);
    /** Places the values of the specified column into the vector parameter.
     * @param col the target column number
     * @param vector the vector into which the column values will be placed
     * @return FloatVector
     */
    public FloatVector getColumn(int col, FloatVector vector);
    /** Gets the column as a <CODE>FloatVector</CODE>.
     * @param col the olumn index
     * @return FloatVector
     */    
    public FloatVector getColumnv(int col);
    /** Places the values from this matrix into the matrix m1; m1
     * should be at least as large as this Matrix.
     * @param m1 The matrix that will hold the new values
     * @return Matrix
     */
    public Matrix get(MatrixMutable m1);
    /**
     * Returns a string that contains the values of this Matrix.
     * @return the String representation
     */
    public String toString() ;
    /**
     * Returns a hash number based on the data values in this
     * object.  Two different Matrix objects with identical data values
     * (ie, returns true for equals(Matrix) ) will return the same hash
     * number.  Two objects with different data members may return the
     * same hash value, although this is not likely.
     * @return the integer hash value
     */
    public int hashCode();
    /**
     * Returns true if all of the data members of Matrix4d m1 are
     * equal to the corresponding data members in this Matrix4d.
     *
     * @param m1 The matrix with which the comparison is made.
     * @return true or false
     */
    public boolean equals(Matrix m1);
    /** Returns true if the Object o1 is of type Matrix and all of the data
     * members of t1 are equal to the corresponding data members in this
     * Matrix.
     * @param o1 the object with which the comparison is made.
     * @return true if this matrix is equal to the other
     */
    public boolean equals(Object o1);
    /** Returns true if the L-infinite distance between this matrix and
     * matrix m1 is less than or equal to the epsilon parameter,
     * otherwise returns false. The L-infinite distance is equal to
     * MAX[i=0,1,2, . . .n ; j=0,1,2, . . .n ; abs(this.m(i,j) - m1.m(i,j)] .
     * @param m1 The matrix to be compared to this matrix
     * @param epsilon the threshold value
     * @return true if this matrix is equal to the other approximately
     */
    public boolean epsilonEquals(Matrix m1, double epsilon);
    /**
     * Returns the trace of this matrix.
     * @return the trace of this matrix.
     */
    public double trace();
    /** Siginifcance by row
     * @todo signfic needs review by pt
     * @param level DOCME
     * @return FloatVector
     */
    public FloatVector sigByRow(double level);
    /** Siginifcance by column.
     * @param level DOCME
     * @return FloatVector
     */    
    public FloatVector sigByCol(double level);
    /** (the first max value)
     * max element in the entire matrix
     * absolute -> entri matrix
     * else by row.
     * @return Cell
     */
    public Matrix.Cell max();
    /** the global first min value
     * @return Cell
     */
    public Matrix.Cell min();
    /** must more efficento than getting each row and doing max
     * @return Cell[]
     */
    public Matrix.Cell[] maxByRow();
    /** the min value by row for each row
     * @return Cell[]
     */
    public Matrix.Cell[] minByRow();
     /** absolute binning --- entrie matrix
      * adds to the specified ranges.
      * @param ranges array of ranges
      */
    public void bin(Range[] ranges) ;
    /** absolute binning --- entrie matrix
     * adds to the specified ranges.
     * @param ranges a list of ranges
     */    
    public void bin(java.util.List ranges);
    /** Discretize the matrix into the specified ranges.
     * @return A whole new discretized matrix. The floats are not shared.
     *         Matrix will contain value from 0 to ranges-1
     * @param ranges the array of ranges
     */
    public Matrix discretize(Range[] ranges);
     /** equalk numbf of lements actually equal numb of DIFF(float) elements.
      * changes the matrix "in place"
      * matrix must be not be immutable for this to work
      *
      * each disc has classdensity elements
      *
      * Sort the array
      * Make it unique
      * Now, the number of ranges = uniq.length / classdensity
      * Create the ranges
      * Iterate through untouched (unsorted, ununiqueized) array and file ranges.
      * @param classdensity DOCME
      * @return Matrix
      */
    public Matrix discretize(int classdensity);
    
    // Inner Classes
    
    /**
     * Represents a single matrix cell.
     */
    public static class Cell implements Serializable {
        private static  long serialVersionUID = 991018889913842280L;
        /** the row index of this cell */        
        public int row;
        /** the column index of this cell */        
        public int col;
        /** the value of this cell */        
        public float value;
    }
    
    /** This class is useful for XMLencode XMLdecode */    
    static class MatrixBean implements Serializable {
        
        /** Holds value of property rowCount. */
        private int rowCount;
        
        /** Holds value of property columnCount. */
        private int columnCount;
        
        /** Holds value of property array. */
        private float[] array;
        
        /** Getter for property rowCount.
         * @return Value of property rowCount.
         */
        public int getRowCount() {
            return this.rowCount;
        }
        
        /** Setter for property rowCount.
         * @param rowCount New value of property rowCount.
         */
        public void setRowCount(int rowCount) {
            this.rowCount = rowCount;
        }
        
        /** Getter for property columnCount.
         * @return Value of property columnCount.
         */
        public int getColumnCount() {
            return this.columnCount;
        }
        
        /** Setter for property columnCount.
         * @param columnCount New value of property columnCount.
         */
        public void setColumnCount(int columnCount) {
            this.columnCount = columnCount;
        }
        
        /** Getter for property array.
         * @return Value of property array.
         */
        public float[] getArray() {
            return this.array;
        }
        
        /** Setter for property array.
         * @param array New value of property array.
         */
        public void setArray(float[] array) {
            this.array = array;
        }
        
    }
    
 // End Cell
    
} // End Matrix
