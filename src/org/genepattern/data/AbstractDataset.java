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

/**
 * Title: Abstract class that implements some of the Dataset interface's methods
 * 
 * @author kohm
 * @version %I%, %G%
 */

public abstract class AbstractDataset implements Dataset {

	/** static initilizer */
	//    static {
	//        /* Thoughts:
	//         * This class can be persisted through XMLEncoder because the property
	//         * names, prop_names, match the variables above. Also the order of the
	//         * variables (represented by the Strings) match one of the public
	// constructors
	//         * -- kwo
	//         */
	//        //create instance of abstract class
	//        final java.beans.Encoder encoder = new java.beans.Encoder() {};
	//        final String[] prop_names = new String [] {"name", "matrix", "rowNames",
	// "colNames"};
	//        // defines how to create an immutable bean - via its' constructor
	//// encoder.setPersistenceDelegate(DefaultDataset.class,
	//// new java.beans.DefaultPersistenceDelegate(prop_names));
	//        
	//        // tried to fix a problem in Java 1.4.0 where the
	// java.util.Arrays$ArrayList
	//        // does not have a persistence delegate
	////
	//// // just because java.util.Arrays.ArrayList has private access
	//// // and cannot refere to the class field by
	//// // java.util.Arrays.ArrayList.class
	//// Object[] obs = new Object[]{new Object(), new Object()};
	//// Class arrayList_class = java.util.Arrays.asList(obs).getClass();
	////
	////
	//// encoder.setPersistenceDelegate(arrayList_class,
	//// encoder.getPersistenceDelegate(java.util.List.class));
	////
	//    }
	/**
	 * Prefered Class constructor
	 * 
	 * @param name
	 *            the name of this data object
	 * @param matrix
	 *            the matrix object
	 * @param mutable
	 *            is this mutable
	 * @param rows
	 *            the row count
	 * @param columns
	 *            the column count
	 */
	protected AbstractDataset(final String name, final Matrix matrix,
			final boolean mutable, final NamesPanel rows,
			final NamesPanel columns) {
		if (!mutable)
			check(name, matrix, rows, columns);
		this.name = name;
		this.matrix = processMatrix(matrix);
		this.is_mutable = mutable;
		this.rows = processNamesPanel(rows);
		this.columns = processNamesPanel(columns);
		if (!mutable)
			finalCheck(rows.getCount(), columns.getCount());
	}

	/**
	 * creates a clone of this object
	 * 
	 * @return Dataset
	 */
	abstract public Object clone();

	/**
	 * subclasses can decide what to do if the matrix is mutable for example:
	 * Nothing just return it (possibly for mutable Datasets) Create a new
	 * mutable matrix Create a DefaultMatrix
	 * 
	 * @param matrix
	 *            matrix from the constructor
	 * @return Matrix
	 */
	abstract protected Matrix processMatrix(Matrix matrix);

	/**
	 * subclasses can decide what to do if the NamesPanel is mutable for
	 * example: Nothing just return it (possibly for mutable Datasets) Create a
	 * new mutable NamesPanel Create an immutable NamesPanel (Immutable Datasets
	 * with mutable NamesPanels)
	 * 
	 * @param panel
	 *            the names panel from the constructor
	 * @return NamesPanel
	 */
	abstract protected NamesPanel processNamesPanel(final NamesPanel panel);

	/**
	 * helper method for constructor
	 * 
	 * @param ds
	 *            the dataset to process
	 * @return Dataset
	 */
	protected static final Dataset check(final Dataset ds) {
		if (ds == null)
			throw new NullPointerException("Dataset param cannot be null");
		return ds;
	}

	/**
	 * checks the name and matrix for problems and checks the row and column
	 * NamesPanels for problems too can be overriden by subclasses
	 * 
	 * @param name
	 *            the name to check
	 * @param matrix
	 *            the matrix to check
	 * @param rows
	 *            the rows names panel to check
	 * @param columns
	 *            the columns names panel to check
	 */
	protected void check(String name, Matrix matrix, NamesPanel rows,
			NamesPanel columns) {
		if (name == null)
			throw new NullPointerException("Name cannot be null!");
		if (matrix == null)
			throw new NullPointerException("Matrix cannot be null!");
		if (rows == null)
			throw new NullPointerException("Cannot have null rows!");
		if (columns == null)
			throw new NullPointerException("Cannot have null columns!");
	}

	/**
	 * checks that the matrix dimensions are consistent with the row and column
	 * names sizes This method should be called in the subclasses constructor
	 * after setting all the variables.
	 * 
	 * @param rows
	 *            the number of rows
	 * @param cols
	 *            the number of columns
	 */
	protected void finalCheck(final int rows, final int cols) {
		if (getRowCount() != rows)
			throw new IllegalArgumentException("Matrix: row " + getRowCount()
					+ " and rowNames: " + rows + " do not match in size");
		if (getColumnCount() != cols)
			throw new IllegalArgumentException("Matrix: column "
					+ getColumnCount() + " and colNames: " + cols
					+ " do not match in size");
	}

	/** Must be called by all public instance methods of this abstract class. */
	abstract protected void checkInit();

	// these methods are only needed by subclasses for getting (and setting)
	// private vars

	/**
	 * gets the name without a checkInit()
	 * 
	 * @return String
	 */
	protected final String internalGetName() {
		return name;
	}

	/**
	 * if this is mutable sets the name
	 * 
	 * @param name
	 *            the new name
	 */
	protected final void internalSetName(final String name) {
		if (is_mutable)
			this.name = name;
		else
			throw new UnsupportedOperationException(
					"Cannot set the name! Class is not mutable!");
	}

	/**
	 * gets the matrix without a checkInit()
	 * 
	 * @return Matrix
	 */
	protected final Matrix internalGetMatrix() {
		return matrix;
	}

	/**
	 * sets the matrix
	 * 
	 * @param matrix
	 *            the new matrix
	 */
	protected final void internalSetMatrix(final Matrix matrix) {
		if (is_mutable)
			this.matrix = matrix;
		else
			throw new UnsupportedOperationException(
					"Cannot set the matrix! Class is not mutable!");
	}

	/**
	 * sets the columns NamesPanel
	 * 
	 * @param new_cols
	 *            the new columns
	 */
	protected final void internalSetColumns(final NamesPanel new_cols) {
		if (is_mutable)
			this.columns = new_cols;
		else
			throw new UnsupportedOperationException(
					"Cannot set the column names! Class is not mutable!");
	}

	/**
	 * sets the columns NamesPanel
	 * 
	 * @param new_rows
	 *            the new rows
	 */
	protected final void internalSetRows(final NamesPanel new_rows) {
		if (is_mutable)
			this.rows = new_rows;
		else
			throw new UnsupportedOperationException(
					"Cannot set the rows names! Class is not mutable!");
	}

	// end special mutator methods for subclasses

	// gets the Id
	abstract public Id getId();

	// Dataset interface method signature

	/**
	 * returns a DataModel that defines the type of model this implementation
	 * represents
	 * 
	 * @return DataModel for <CODE>Dataset</CODE> s
	 */
	public org.genepattern.data.DataModel getDataModel() {
		return DATA_MODEL;
	}

	// the name of this Dataset
	public String getName() {
		return name;
	}

	public FloatVector getRow(int rown) {
		checkInit();
		return matrix.getRowv(rown);
	}

	public FloatVector getColumn(int coln) {
		checkInit();
		return matrix.getColumnv(coln);
	}

	public float getElement(int rown, int coln) {
		checkInit();
		return matrix.getElement(rown, coln);
	}

	public int getRowCount() {
		checkInit();
		return matrix.getRowCount();
	}

	public int getColumnCount() {
		checkInit();
		return matrix.getColumnCount();
	}

	public Matrix getMatrix() {
		checkInit();
		return matrix;
	}

	/** is this mutable */
	public boolean isMutable() {
		return is_mutable;
	}

	//Panels

	public String getRowName(final int row) {
		return rows.getName(row);
	}

	public String[] getRowNames() {
		checkInit();
		return rows.getNames();
	}

	public String getColumnName(final int col) {
		checkInit();
		return columns.getName(col);
	}

	public String[] getColumnNames() {
		checkInit();
		return columns.getNames();
	}

	//gets the column panel, which is where the column names,
	// descriptions/annotation are...();
	public NamesPanel getColumnPanel() {
		checkInit();
		return columns;
	}

	// gets the row panel, which is where the row names, descriptions/annotation
	// are...
	public NamesPanel getRowPanel() {
		checkInit();
		return rows;
	}

	// overridden methods from Object

	// returns the name of this object
	public final String toString() {
		return getName();
	}

	/// determines if the value of this object is equal to another Dataset
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof Dataset))
			return false;
		final Dataset dataset = (Dataset) obj;
		// assumes that getColumnPanel() getRowPanel() and getMatrix() will
		// not ever return null!!
		return (this.getColumnPanel().equals(dataset.getColumnPanel())
				&& this.getRowPanel().equals(dataset.getRowPanel()) && this
				.getMatrix().equals(dataset.getMatrix()));
	}

	/**
	 * calculates the hash code for a dataset based on the significant fields
	 * This is only calculated once for immutable Datasets but has to be
	 * recaclulated each time it is called for mutatable ones. Could try to
	 * optimize this by only recalculating the hascode after there was a change.
	 * However, this would not be fool-proof and would be somewhat more
	 * compicated thus will be avoided...
	 * 
	 * @return int
	 */
	public int hashCode() {
		final boolean mutable = isMutable();
		if (mutable || hashcode == 0) { // comppute it
			final NamesPanel rows = getRowPanel();
			final NamesPanel cols = getColumnPanel();
			final Matrix matrix = getMatrix();

			int result = 17;
			result = 37 * result + ((rows == null) ? 0 : rows.hashCode());
			result = 37 * result + ((cols == null) ? 0 : cols.hashCode());
			result = 37 * result + ((matrix == null) ? 0 : matrix.hashCode());

			if (mutable) // if mutable then recalculate each time ( i.e. don't
						 // set hashcode)
				return result;
			else
				// immutable object only need to calc this once!!
				hashcode = result;
		}
		return hashcode;
	}

	// fields

	/** the matrix of float expression values */
	private Matrix matrix;

	/** the name of this Dataset */
	private String name;

	/**
	 * false means that this classe's variables cannot have their values changed
	 * once set in the constructor
	 */
	protected final boolean is_mutable;

	/** the row NamesPanel */
	private NamesPanel rows;

	/** the column NamesPanel */
	private NamesPanel columns;

	/** the hash code value */
	private int hashcode = 0;
} // End AbstractDataset
