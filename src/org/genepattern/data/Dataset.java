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

//import org.genepattern.data.Matrix.MatrixBean;
/**
 * 
 * NOT a persistentobject -> algs workoff off this a lot and no need for all the
 * pob gunk. Plus its not really saved - the fixture is.
 * 
 * Interface that represents some section of the dataframe -. superset, subset
 * or a combination of some sort. They are immutable.
 * 
 * A Math object conceptually similar to R's DataFrame. Basically a labelled
 * Matrix.
 * 
 * 
 * FIXME should force the creation of equals(Object) and hashCode() all
 * implementations of Dataset should have these implemented also
 */

public interface Dataset extends org.genepattern.data.DataObjector, Cloneable {
	// fields
	/** the DataModel that should be returned from getDataModel() */
	public static final DataModel DATA_MODEL = new DataModel("Dataset", 1);

	// method signature
	/**
	 * The object id
	 * 
	 * @return Id
	 */
	public Id getId();

	/**
	 * the name of this Dataset
	 * 
	 * @return String
	 */
	public String getName();

	/**
	 * returns the number of rows
	 * 
	 * @return int
	 */
	public int getRowCount();

	/**
	 * returns the number of columns
	 * 
	 * @return int
	 */
	public int getColumnCount();

	/**
	 * gets the Vector of float primitives for the specified row
	 * 
	 * @param row
	 *            the row index
	 * @return FloatVector
	 */
	public FloatVector getRow(int row);

	/**
	 * gets the Vector of float primitives for the specified column
	 * 
	 * @param column
	 *            the column index
	 * @return FloatVector
	 */
	public FloatVector getColumn(int column);

	/**
	 * gets the float at the specified row and column
	 * 
	 * @param row
	 *            the row index
	 * @param column
	 *            the column index
	 * @return float
	 */
	public float getElement(int row, int column);

	/**
	 * should return immutable matrix
	 * 
	 * @return Matrix
	 */
	public Matrix getMatrix();

	/**
	 * gets the row panel, which is where the row names, descriptions/annotation
	 * are...
	 * 
	 * @return NamesPanel
	 */
	public NamesPanel getRowPanel();

	/**
	 * gets the column panel, which is where the column names,
	 * descriptions/annotation are...
	 * 
	 * @return NamesPanel
	 */
	public NamesPanel getColumnPanel();

	/**
	 * convenience method gets the row name at the index
	 * 
	 * @param row
	 *            he row index
	 * @return String
	 */
	public String getRowName(int row);

	/**
	 * convenience method gets the row names as an array
	 * 
	 * @return String[]
	 */
	public String[] getRowNames();

	/**
	 * convenience method gets the column name at the index
	 * 
	 * @param col
	 *            the column index
	 * @return String
	 */
	public String getColumnName(final int col);

	/**
	 * convenience method gets the column names as an array
	 * 
	 * @return String[]
	 */
	public String[] getColumnNames();

	/**
	 * creates a shallow clone of this object
	 * 
	 * @return Dataset, a clone of this one
	 */
	public Object clone();
} // End Dataset
