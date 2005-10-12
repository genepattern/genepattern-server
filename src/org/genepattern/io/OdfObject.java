package org.genepattern.io;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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

	private String[] columnNames;

	private Class[] columnClasses;

	private String[] columnTypes;

	private Object[][] columns;

	private int dataLines;

	public List getHeaders() {
		return keyValuePairs;

	}

	public int getColumnIndex(String columnName) {
		for (int j = 0; j < columnNames.length; j++) {
			if (columnNames[j].equalsIgnoreCase(columnName)) {
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
	
	private class MyHandler implements IOdfHandler {
		public void header(String key, String[] values) throws ParseException {
			if (key.equals("COLUMN_NAMES")) {
				columnNames = values;
			} else if (key.equals("COLUMN_TYPES")) {
				columnTypes = values;
			}
		}

		public void header(String key, String value) throws ParseException {
			if (key.equals("DataLines")) {
				dataLines = Integer.parseInt(value);
			}
			keyValuePairs.add(new Entry(key, value));
		}

		public void data(int i, int column, String s) throws ParseException {
			Object obj = columns[column];
			if (obj instanceof Double[]) {
				((Double[]) obj)[i] = Double.valueOf(s);
			} else if (obj instanceof Integer[]) {
				((Integer[]) obj)[i] = Integer.valueOf(s);
			} else if (obj instanceof String[]) {
				((String[]) obj)[i] = s;
			} else if (obj instanceof Boolean[]) {
				((Boolean[]) obj)[i] = Boolean.valueOf(s);
			} else {
				throw new ParseException("Unknown column type");
			}
		}

		public void endHeader() throws ParseException {
			columns = new Object[columnTypes.length][];
			columnClasses = new Class[columnTypes.length];

			for (int i = 0; i < columnTypes.length; i++) {
				if (columnTypes[i].equals("String")) {
					columns[i] = new String[dataLines];
					columnClasses[i] = String.class;
				} else if (columnTypes[i].equals("double")
						|| columnTypes[i].equals("float")) {
					columns[i] = new Double[dataLines];
					columnClasses[i] = Double.class;
				} else if (columnTypes[i].equals("int")) {
					columns[i] = new Integer[dataLines];
					columnClasses[i] = Integer.class;
				} else if (columnTypes[i].equals("boolean")) {
					columns[i] = new Boolean[dataLines];
					columnClasses[i] = Boolean.class;
				} else {
					throw new ParseException("Unknown column type");
				}

			}
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
		return columns[c][r];
	}

	public int getRowCount() {
		return dataLines;
	}

	public Class getColumnClass(int c) {
		return columnClasses[c];
	}

	public int getColumnCount() {
		return columns.length;
	}

	public String getColumnName(int c) {
		return columnNames[c];
	}

	public Object getArray(String columnName) {
		for (int i = 0; i < columnNames.length; i++) {
			if (columnNames[i].equalsIgnoreCase(columnName)) {
				return columns[i];
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

	public String[] getColumnNames() {
		return columnNames;
	}

}
