package org.genepattern.io.expr.gct;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Collection;
import java.util.HashSet;

import org.genepattern.io.expr.IExpressionDataHandler;
import org.genepattern.io.expr.IExpressionDataParser;

/**
 * Class for reading a gct document using callbacks.
 * 
 * @author Joshua Gould
 */
public class GctParser implements IExpressionDataParser {
	final static String VERSION = "#1.2";

	LineNumberReader reader;

	int rows, columns;

	int version;

	IExpressionDataHandler handler;

	public boolean canDecode(InputStream is) throws IOException {
		reader = new LineNumberReader(new BufferedReader(new InputStreamReader(
				is)));
		try {
			readHeader();
			return true;
		} catch (org.genepattern.io.ParseException pe) {
			return false;
		}
	}

	/**
	 * Parse a gct document. The application can use this method to instruct the
	 * gct reader to begin parsing an gct document from any valid input source.
	 * Applications may not invoke this method while a parse is in proggcts
	 * (they should create a new GctParser instead ). Once a parse is complete,
	 * an application may reuse the same GctParser object, possibly with a
	 * different input source. During the parse, the GctParser will provide
	 * information about the gct document through the registered event handler.
	 * This method is synchronous: it will not return until parsing has ended.
	 * If a client application wants to terminate parsing early, it should throw
	 * an exception.
	 * 
	 * @param is
	 *            The input stream
	 * @throws org.genepattern.io.ParseException -
	 *             Any parse exception, possibly wrapping another exception.
	 * @throws IOException -
	 *             An IO exception from the parser, possibly from a byte stream
	 *             or character stream supplied by the application.
	 */
	public void parse(InputStream is) throws org.genepattern.io.ParseException,
			IOException {
		reader = new LineNumberReader(new BufferedReader(new InputStreamReader(
				is)));
		readHeader();
		readData();
	}

	void readData() throws org.genepattern.io.ParseException, IOException {
		int rowIndex = 0;
		int expectedColumns = columns + 2;
		Collection rowNamesSet = new HashSet(rows);
		for (String s = reader.readLine(); s != null; s = reader.readLine(), rowIndex++) {
			if (rowIndex >= rows) {
				if (s.trim().equals("")) {// ignore blank lines at the end of
										  // the file
					rowIndex--;
					continue;
				}

				int rowsRead = rowIndex + 1;
				throw new org.genepattern.io.ParseException(
						"More data rows than expected on line "
								+ reader.getLineNumber() + ". Read " + rowsRead
								+ ", expected " + rows + ".");
			}
			String[] tokens = s.split("\t");
			if (tokens.length != expectedColumns) {
				throw new org.genepattern.io.ParseException(tokens.length
						+ getColumnString(tokens.length) + " on line "
						+ reader.getLineNumber() + ". Expected "
						+ expectedColumns + ".");
			}
			String rowName = tokens[0];
			if (rowNamesSet.contains(rowName)) {
				throw new org.genepattern.io.ParseException(
						"Duplicate row name " + rowName + " on line "
								+ reader.getLineNumber());
			}
			rowNamesSet.add(rowName);
			if (handler != null) {
				handler.rowName(rowIndex, rowName);
				handler.rowDescription(rowIndex, tokens[1]);
			}
			if (handler != null) {
				for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
					try {
						handler.data(rowIndex, columnIndex, Double
								.parseDouble(tokens[columnIndex + 2]));
					} catch (NumberFormatException nfe) {
						throw new org.genepattern.io.ParseException(
								"Data at line number " + reader.getLineNumber()
										+ " and column " + columnIndex
										+ " is not a number.");
					}
				}
			}
		}
		if (rowIndex != rows) {
			throw new org.genepattern.io.ParseException(
					"Missing data rows. Read " + rowIndex + " "
							+ getRowString(rowIndex) + ", expected " + rows);
		}
	}

	void readHeader() throws org.genepattern.io.ParseException, IOException {
		String versionLine = reader.readLine().trim();
		if (!versionLine.equals(VERSION)) {
			throw new org.genepattern.io.ParseException(
					"Unknown version on line 1.");
		}
		String dimensionsLine = reader.readLine().trim();// 2nd header line:
														 // <numRows> <tab>
														 // <numCols>

		String[] dimensions = dimensionsLine.split("\t");
		if (dimensions.length != 2) {
			throw new org.genepattern.io.ParseException(
					"Line number "
							+ reader.getLineNumber()
							+ " should contain the number of rows and the number of columns separated by a tab.");
		}

		try {
			rows = Integer.parseInt(dimensions[0]);
			columns = Integer.parseInt(dimensions[1]);
		} catch (NumberFormatException nfe) {
			throw new org.genepattern.io.ParseException(
					"Line number "
							+ reader.getLineNumber()
							+ " should contain the number of rows and the number of columns separated by a tab.");
		}

		if (rows <= 0 || columns <= 0) {
			throw new org.genepattern.io.ParseException(
					"Number of rows and columns must be greater than 0.");
		}
		if (handler != null) {
			handler.init(rows, columns, true, false, false);
		}
		String columnNamesLine = reader.readLine();
		String[] columnNames = columnNamesLine.split("\t");
		// columnNames[0] = 'Name'
		// columnNames[1] ='Description'
		int expectedColumns = columns + 2;
		if (columnNames.length != expectedColumns) {
			throw new org.genepattern.io.ParseException("Expected "
					+ expectedColumns + " tokens got " + columnNames.length
					+ " tokens on line " + reader.getLineNumber() + ".");
		}
		Collection columnNamesSet = new HashSet(columns);
		for (int j = 0; j < columns; j++) {
			String columnName = columnNames[j + 2];
			if (columnNamesSet.contains(columnName)) {
				throw new org.genepattern.io.ParseException(
						"Duplicate column name " + columnName);
			}
			columnNamesSet.add(columnName);
			if (handler != null) {
				handler.columnName(j, columnName);
			}
		}
	}

	public void setHandler(IExpressionDataHandler handler) {
		this.handler = handler;
	}

	protected String getRowString(int rows) {
		if (rows == 1) {
			return "row";
		}
		return "rows";
	}

	protected String getColumnString(int cols) {
		if (cols == 1) {
			return "column";
		}
		return "columns";
	}
}

