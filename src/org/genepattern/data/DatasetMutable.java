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
 * Title: enter desc
 * 
 * @author Aravind Subramanian
 * @version %I%, %G%
 */

public class DatasetMutable extends AbstractDataset {

	//    /** static initilizer */
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
	//        final String[] prop_names = new String [] {"name", "matrix", "rows",
	// "columns"};
	//        // defines how to create an immutable bean - via its' constructor
	//        encoder.setPersistenceDelegate(DatasetMutable.class,
	//                        new java.beans.DefaultPersistenceDelegate(prop_names));
	//         // tried to fix a problem in Java 1.4.0 where the
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

	/** empty constructor */
	public DatasetMutable() {
		super(null, null, true, null, null);
		// note no finalCheck();
	}

	/**
	 * Class constructor
	 */
	public DatasetMutable(final String name, final Matrix matrix,
			final NamesPanel rows, final NamesPanel columns) {
		super(name, matrix, true/* is mutable */, rows, columns);
		// note no finalCheck(); since the matrix and row and column names can
		// be
		// changed
	}

	//    /**
	//     * Class constructor
	//     */
	//    public DatasetMutable(final String name, final Matrix matrix, final
	// String[] row_names, final String[] col_names) {
	//        // note cannot use Arrays.toList(row_names) because of a problem
	//        // serializing to XML
	//        // see static initilizer above
	//            this(name, matrix, ArrayUtils.stringArrayToList(row_names),
	//                 ArrayUtils.stringArrayToList(col_names));
	//    }
	/**
	 * Class constructor
	 */
	public DatasetMutable(Dataset ds) {
		this(check(ds).getName(), ds.getMatrix(), ds.getRowPanel(), ds
				.getColumnPanel());
	}

	//    /** processes the panel to make it mutable */
	//    protected static final NamesPanelMutable processPanel(NamesPanel panel) {
	//        return new NamesPanelMutable(panel.getListOfNames(),
	// ((AbstractNamesPanel)panel).internalGetAnnotationFactory());
	//    }

	/** creates a clone of this */
	public Object clone() {
		final DatasetMutable dataset = new DatasetMutable(getName() + "_clone",
				internalGetMatrix(), getRowPanel(), getColumnPanel());
		return dataset;
	}

	// implemented abstract methods (mainly from Dataset)

	/**
	 * Must be called by all public instance methods of this class.
	 * 
	 * @throws IllegalStateException
	 *             if the class has not been initialized properly
	 */
	protected final void checkInit() { // does nothing
	}

	/**
	 * subclasses can decide what to do if the matrix is mutable for example:
	 * Nothing just return it (possibly for mutable Datasets) Create a new
	 * mutable matrix Create a DefaultMatrix
	 *  
	 */
	protected Matrix processMatrix(Matrix matrix) {
		if (matrix != null) // could keep the old one if it was immutable until
			return new MatrixMutable(matrix); // some operation would cause a
											  // change
		else
			return null;
	}

	/**
	 * subclasses can decide what to do if the NamesPanel is mutable for
	 * example: Nothing just return it (possibly for mutable Datasets) Create a
	 * new mutable NamesPanel Create an immutable NamesPanel (Immutable Datasets
	 * with mutable NamesPanels)
	 */
	protected NamesPanel processNamesPanel(NamesPanel panel) {
		if (panel != null)
			return new NamesPanelMutable((AbstractNamesPanel) panel);
		else
			return null;
	}

	public Id getId() {
		checkInit();
		return null; // why??
	}

	// setter methods

	/** sets the name */
	public void setName(final String name) {
		internalSetName(name);
	}

	/** sets the matrix (if not mutable a new MatrixMutable is created) */
	public void setMatrix(final Matrix matrix) {
		internalSetMatrix(processMatrix(matrix));
	}

	/** sets the rows */
	public void setRows(final NamesPanelMutable rows) {
		internalSetRows(rows);
	}

	/** sets the columns */
	public void setColumns(final NamesPanelMutable columns) {
		internalSetColumns(columns);
	}

	// other mutators
	/** deletes the rows specified by the array of row indicies */
	public void deleteRows(final int[] row_inds) {
		MatrixMutable matrix = (MatrixMutable) getMatrix();
		NamesPanelMutable row_names = (NamesPanelMutable) this.getRowPanel();
		matrix.removeRows(row_inds);
		row_names.removeEntries(row_inds);
	}

	/** deletes the rows specified by the array of row indicies */
	public void deleteRows(final IntRanges ranges) {
		MatrixMutable matrix = (MatrixMutable) getMatrix();
		NamesPanelMutable row_names = (NamesPanelMutable) this.getRowPanel();
		matrix.removeRows(ranges);
		row_names.removeEntries(ranges);
	}

	//    /** deletes the columns specified by the array of row indicies */
	//    public void deleteColumns(final int[] col_inds) {
	//        MatrixMutable matrix = (MatrixMutable)getMatrix();
	//        NamesPanelMutable col_names = (NamesPanelMutable)this.getColumnPanel();
	//        matrix.removeColumns(col_inds);
	//        col_names.removeEntries(col_inds);
	//    }
	/** deletes the columns specified by the array of row indicies */
	public void deleteColumns(final IntRanges ranges) {
		MatrixMutable matrix = (MatrixMutable) getMatrix();
		NamesPanelMutable col_names = (NamesPanelMutable) this.getColumnPanel();
		matrix.removeColumns(ranges);
		col_names.removeEntries(ranges);
	}

	// helper methods
	/**
	 * test if the data is consistent i.e. matrix\.getRowCount() ==
	 * rowNames\.length etc.
	 */
	public boolean isDataConsistant() {
		try {
			check(internalGetName(), internalGetMatrix(), getRowPanel(),
					getColumnPanel());
			finalCheck(getRowPanel().getCount(), getColumnPanel().getCount());
			return true;
		} catch (RuntimeException ex) {
			return false;
		}
	}

} // End DatasetMutable
