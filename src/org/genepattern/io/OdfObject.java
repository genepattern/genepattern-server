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


package org.genepattern.io;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

/**
 * Generic odf representation
 * 
 * @author Joshua Gould
 */
public class OdfObject implements TableModel {

	private List keyValuePairs = new ArrayList();

	private List columnNames;

	// list of Class objects
	private List columnClasses;

	// list of String objects
	private List columnTypes;

	// list of column arrays
	private List columns;

	private int dataLines;

	public List getHeaders() {
		return keyValuePairs;
	}

	public int getColumnIndex(String columnName) {
		for (int j = 0; j < columnNames.size(); j++) {
			if (((String) columnNames.get(j)).equalsIgnoreCase(columnName)) {
				return j;
			}
		}
		return -1;
	}

	public OdfObject(String fileName) throws ParseException, IOException {
		InputStream is = null;
		try {
			OdfParser parser = new OdfParser();
			parser.setHandler(new MyHandler());
			is = new FileInputStream(fileName);
			parser.parse(is);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception x) {
				}
			}
		}
	}

	public void addColumn(int index, String columnName, String columnType,
			Object[] data) {
		if (data.length != dataLines) {
			throw new IllegalArgumentException(
					"Length of data array must be equal to " + dataLines);
		}
		columnNames.add(index, columnName);
		try {
			addColumnType(index, columnType);
		} catch (ParseException e) {
			throw new IllegalArgumentException("Unknown column type: "
					+ columnType);
		}
		columns.set(index, data);
	}

	private class MyHandler implements IOdfHandler {

		public void header(String key, String[] values) throws ParseException {
			if (key.equals("COLUMN_NAMES")) {
				columnNames = new ArrayList(Arrays.asList(values));
			} else if (key.equals("COLUMN_TYPES")) {
				columnTypes = new ArrayList(Arrays.asList(values));
			}
		}

		public void header(String key, String value) throws ParseException {
			if (key.equals("DataLines")) {
				dataLines = Integer.parseInt(value);
			}
			keyValuePairs.add(new Entry(key, value));
		}

		public void data(int row, int column, String s) throws ParseException {
			Object obj = columns.get(column);
			if (obj instanceof Double[]) {
				((Double[]) obj)[row] = Double.valueOf(s);
			} else if (obj instanceof Integer[]) {
				((Integer[]) obj)[row] = Integer.valueOf(s);
			} else if (obj instanceof String[]) {
				((String[]) obj)[row] = s;
			} else if (obj instanceof Boolean[]) {
				((Boolean[]) obj)[row] = Boolean.valueOf(s);
			} else {
				throw new ParseException("Unknown column type");
			}
		}

		public void endHeader() throws ParseException {
			if (columnTypes == null) {
				columnTypes = new ArrayList(columnNames.size());
				for (int i = 0, cols = columnNames.size(); i < cols; i++) {
					columnTypes.add("String");
				}
			}
			columns = new ArrayList(columnTypes.size());
			columnClasses = new ArrayList(columnTypes.size());
			for (int i = 0; i < columnTypes.size(); i++) {
				addColumnType((String) columnTypes.get(i));
			}

		}

	}

	private void addColumnType(String type) throws ParseException {
		if (type.equals("String")) {
			columns.add(new String[dataLines]);
			columnClasses.add(String.class);
		} else if (type.equals("double") || type.equals("float")) {
			columns.add(new Double[dataLines]);
			columnClasses.add(Double.class);
		} else if (type.equals("int")) {
			columns.add(new Integer[dataLines]);
			columnClasses.add(Integer.class);
		} else if (type.equals("boolean")) {
			columns.add(new Boolean[dataLines]);
			columnClasses.add(Boolean.class);
		} else {
			throw new ParseException("Unknown column type");
		}
	}

	private void addColumnType(int index, String type) throws ParseException {
		if (type.equals("String")) {
			columns.add(index, new String[dataLines]);
			columnClasses.add(index, String.class);
		} else if (type.equals("double") || type.equals("float")) {
			columns.add(index, new Double[dataLines]);
			columnClasses.add(index, Double.class);
		} else if (type.equals("int")) {
			columns.add(index, new Integer[dataLines]);
			columnClasses.add(index, Integer.class);
		} else if (type.equals("boolean")) {
			columns.add(index, new Boolean[dataLines]);
			columnClasses.add(index, Boolean.class);
		} else {
			throw new ParseException("Unknown column type");
		}
	}

	public String getHeader(String key) {
		for (int i = 0; i < keyValuePairs.size(); i++) {
			Entry e = (Entry) keyValuePairs.get(i);
			if (e.key.equals(key)) {
				return e.value;
			}
		}
		return "";
	}

	public boolean getBooleanHeader(String key) {
		for (int i = 0; i < keyValuePairs.size(); i++) {
			Entry e = (Entry) keyValuePairs.get(i);
			if (e.key.equals(key)) {
				return Boolean.valueOf(e.value).booleanValue();
			}
		}
		return false;
	}

	public double getDoubleHeader(String key) {
		for (int i = 0; i < keyValuePairs.size(); i++) {
			Entry e = (Entry) keyValuePairs.get(i);
			if (e.key.equals(key)) {
				return Double.parseDouble(e.value);
			}
		}
		return 0;
	}

	public int getIntHeader(String key) {
		for (int i = 0; i < keyValuePairs.size(); i++) {
			Entry e = (Entry) keyValuePairs.get(i);
			if (e.key.equals(key)) {
				return Integer.parseInt(e.value);
			}
		}
		return 0;
	}

	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
	}

	public void addTableModelListener(TableModelListener l) {
	}

	public void removeTableModelListener(TableModelListener l) {
	}

	public Object getValueAt(int r, int c) {
		Object[] obj = (Object[]) columns.get(c);
		return obj[r];
	}

	public int getRowCount() {
		return dataLines;
	}

	public Class getColumnClass(int c) {
		return (Class) columnClasses.get(c);
	}

	public int getColumnCount() {
		return columns.size();
	}

	public String getColumnName(int j) {
		return (String) columnNames.get(j);
	}

	/**
	 * Gets the column names
	 * 
	 * @return a unmodifiable list of column names
	 */
	public List getColumnNames() {
		return Collections.unmodifiableList(columnNames);
	}

	public Object getArray(String columnName) {
		for (int i = 0; i < columnNames.size(); i++) {
			if (((String) columnNames.get(i)).equalsIgnoreCase(columnName)) {
				return columns.get(i);
			}
		}
		return null;
	}

	public Boolean[] getBooleanArray(String columnName) {
		return (Boolean[]) getArray(columnName);
	}

	public Double[] getDoubleArray(String columnName) {
		return (Double[]) getArray(columnName);
	}

	public Integer[] getIntegerArray(String columnName) {
		return (Integer[]) getArray(columnName);
	}

	/**
	 * @author Joshua Gould
	 */
	public static class Entry {
		public final String key;

		public final String value;

		Entry(String key, String value) {
			this.key = key;
			this.value = value;
		}
	}

}
