/*
 * DatasetPropertiesWrapper.java
 *
 * Created on May 7, 2003, 9:40 AM
 */

package org.genepattern.io.encoder;

import java.util.Collections;
import java.util.Map;

import org.genepattern.data.AbstractObject;
import org.genepattern.data.DataModel;
import org.genepattern.data.Dataset;
import org.genepattern.data.FeaturesetProperties;
import org.genepattern.data.NamesPanel;

/**
 * Adaptor class that makes a Dataset look like a FeaturesetProperties object.
 * 
 * @author kohm
 */
public class DatasetPropertiesWrapper extends AbstractObject implements
		FeaturesetProperties {

	/** Creates a new instance of DatasetPropertiesWrapper */
	public DatasetPropertiesWrapper(final Dataset dataset) {
		super(dataset.getName());
		this.dataset = dataset;
		this.col_cnt = dataset.getColumnCount() + 2;
		this.row_cnt = dataset.getRowCount();
		this.first_float_type_column = 2;
		this.columns_panel = dataset.getColumnPanel();
		this.rows_panel = dataset.getRowPanel();

		this.simple_objects = new SimpleObject[col_cnt];
		simple_objects[0] = new DatasetPropertiesWrapper.SimpleStringName(0);
		simple_objects[1] = new DatasetPropertiesWrapper.SimpleStringDescr(1);
		for (int i = 2; i < col_cnt; i++) {
			simple_objects[i] = new SimpleFloat(i - 2);
		}
	}

	public void addTableModelListener(
			javax.swing.event.TableModelListener tableModelListener) {
	}

	public Map getAttributes() {
		return Collections.EMPTY_MAP;
	}

	public Class getColumnClass(final int col) {
		return ((col < first_float_type_column) ? String.class : Float.TYPE);
	}

	public int getColumnCount() {
		return col_cnt;
	}

	public String getColumnDescription(final int columnIndex) {
		switch (columnIndex) {
		case 0:
			return "Name for each row";
		case 1:
			return "Description for each row";
		default:
			return this.columns_panel.getAnnotation(columnIndex - 2);
		}
	}

	public String[] getColumnDescriptions(final String[] array) {
		throw new UnsupportedOperationException();
		//populateArrayWithDescriptions(this.columns_panel, array);
		//return array;
	}

	public String getColumnName(final int col) {
		switch (col) {
		case 0:
			return "Names";
		case 1:
			return "Description";
		default:
			//System.out.println("("+getColumnCount()+") columns_panel has
			// "+columns_panel.getCount()+" getting name "+(col-2));
			return this.columns_panel.getName(col - 2);
		}
	}

	public String[] getColumnNames(final String[] array) {
		throw new UnsupportedOperationException();
		//populateArrayWithNames(this.columns_panel, array);
		//return array;
	}

	/**
	 * returns a DataModel that defines the type of model this implementation
	 * represents
	 * 
	 * @return the Dataset's DataModel
	 */
	public final DataModel getDataModel() {
		return dataset.getDataModel();
	}

	/** returns the Model that this data object represents */
	public final String getModel() {
		return dataset.getDataModel().toString();
	}

	public final int getRowCount() {
		return row_cnt;
	}

	/** gets the specified row description if none then returns null */
	public final String getRowDescription(final int row) {
		return this.rows_panel.getAnnotation(row);
	}

	public final String[] getRowDescriptions(final String[] array) {
		populateArrayWithDescriptions(this.rows_panel, array);
		return array;
	}

	public final String getRowName(final int row) {
		return this.rows_panel.getName(row);
	}

	public final String[] getRowNames(final String[] array) {
		populateArrayWithNames(this.rows_panel, array);
		return array;
	}

	public final Object getValueAt(final int row, final int col) {
		return simple_objects[col].get(row);
	}

	public boolean hasColumnDescriptions() {
		return true;
	}

	public boolean hasRowDescriptions() {
		return true;
	}

	public boolean hasRowNames() {
		return true;
	}

	public final boolean isCellEditable(final int row, final int col) {
		return false;
	}

	public void removeTableModelListener(
			javax.swing.event.TableModelListener tableModelListener) {
	}

	public void setValueAt(Object obj, int param, int param2) {
		throw new UnsupportedOperationException(
				"Cannot modify the underlying Dataset!");
	}

	// overidden Object methods

	public String toString() {
		return dataset.toString();
	}

	public boolean equals(final Object obj) {
		if (obj instanceof DatasetPropertiesWrapper) {
			return dataset.equals(((DatasetPropertiesWrapper) obj).dataset);
		}
		return false;
	}

	public int hashCode() {
		return dataset.hashCode();
	}

	// helpers
	/** populates the array with the labels from the NamesPanel */
	private void populateArrayWithNames(final NamesPanel panel,
			final String[] array) {
		final int limit = array.length;
		if (panel.getCount() != limit) {
			final boolean too_small = (limit < panel.getCount());
			throw new ArrayIndexOutOfBoundsException("The size(" + limit
					+ ") of the array is "
					+ ((too_small) ? "smaller" : "larger")
					+ " than the number(" + panel.getCount() + ") of names!");
		}
		for (int i = 0; i < limit; i++) {
			array[i] = panel.getName(i);
		}
	}

	/** populates the array with the escriptions from the NamesPanel */
	private void populateArrayWithDescriptions(final NamesPanel panel,
			final String[] array) {
		final int limit = array.length;
		if (panel.getCount() != limit) {
			final boolean too_small = (limit < panel.getCount());
			throw new ArrayIndexOutOfBoundsException("The size(" + limit
					+ ") of the array is "
					+ ((too_small) ? "smaller" : "larger")
					+ " than the number(" + panel.getCount()
					+ ") of descriptions!");
		}
		for (int i = 0; i < limit; i++) {
			array[i] = panel.getAnnotation(i);
		}
	}

	// fields
	/** the dataset */
	private final Dataset dataset;

	/** the number of columns */
	private final int col_cnt;

	/** the number of rows */
	private final int row_cnt;

	/** the first column where floats are */
	private final int first_float_type_column;

	/** the NamesPanel for the rows */
	private final NamesPanel rows_panel;

	/** the NamesPanel for the columns */
	private final NamesPanel columns_panel;

	/**
	 * one SimpleObject for each column this is returned by getValueAt(r,c) as a
	 * wrapper for primitives
	 */
	private final SimpleObject[] simple_objects;

	// I N N E R C L A S S E S
	/** object returned from getValueAt() when the column has floats */
	public class SimpleFloat extends AbstractSimpleObject {
		SimpleFloat(final int column) {
			super(column);
		}

		/**
		 * returns an object that represents the value at the index
		 * 
		 * @param row the row index
		 */
		public final Object get(final int row) {
			this.value = dataset.getElement(row, column);
			return this;
		}

		/**
		 * returns a String representation of the value
		 * 
		 * @return a String representation of the value
		 */
		public String toString() {
			return String.valueOf(value);
		}

		// fields
		/** the float value */
		private float value;
	}

	/** object returned from getValueAt() when the column has row labels */
	public class SimpleStringName extends AbstractSimpleObject {
		SimpleStringName(final int column) {
			super(column);
		}

		/**
		 * returns the String at the index
		 * 
		 * @param row the row index
		 */
		public final Object get(final int row) {
			return rows_panel.getName(row);
		}

		/**
		 * return null shouldn't be used
		 * 
		 * @return null
		 */
		public String toString() {
			return null;
		}
	}

	/** object returned from getValueAt() when the column has row descriptions */
	public class SimpleStringDescr extends AbstractSimpleObject {
		SimpleStringDescr(final int column) {
			super(column);
		}

		/**
		 * returns the String at the index
		 * 
		 * @param row the row index
		 */
		public final Object get(final int row) {
			return rows_panel.getAnnotation(row);
		}

		/**
		 * return null shouldn't be used
		 * 
		 * @return null
		 */
		public final String toString() {
			return null;
		}
	}
}