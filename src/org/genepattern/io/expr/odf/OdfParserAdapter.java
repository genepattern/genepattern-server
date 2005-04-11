package org.genepattern.io.expr.odf;

import java.io.IOException;
import java.io.InputStream;

import org.genepattern.io.IOdfHandler;
import org.genepattern.io.OdfParser;
import org.genepattern.io.ParseException;
import org.genepattern.io.expr.IExpressionDataHandler;
import org.genepattern.io.expr.IExpressionDataParser;

/**
 * Description of the Class
 * 
 * @author Joshua Gould
 */
class OdfParserAdapter implements IExpressionDataParser {
	IExpressionDataHandler expressionHandler;

	MyHandler myHandler = new MyHandler();

	public boolean canDecode(InputStream is) throws IOException {
		OdfParser parser = new OdfParser();
		myHandler.testOnly = true;
		parser.setHandler(myHandler);
		try {
			parser.parse(is);
		} catch (EndParseException epe) {
		} catch (org.genepattern.io.ParseException e) {
			return false;
		}
		return true;
	}

	public void parse(InputStream is) throws org.genepattern.io.ParseException,
			IOException {
		OdfParser parser = new OdfParser();
		myHandler.testOnly = false;
		parser.setHandler(myHandler);
		parser.parse(is);
	}

	public void setHandler(IExpressionDataHandler handler) {
		this.expressionHandler = handler;
	}

	static class EndParseException extends org.genepattern.io.ParseException {

		public EndParseException() {
			super((String) null);
		}
	}

	class MyHandler implements IOdfHandler {
		boolean testOnly = false;

		String[] rowNames, columnNames;

		String[] rowDescriptions, columnDescriptions;

		String[] columnTypes;

		int rowNamesColumn = -1;

		int rowDescriptionsColumn = -1;

		int rows, columns;

		int dataOffset;

		boolean rowNamesColumnSpecified = false;

		boolean rowDescriptionsColumnSpecified = false;

		public void data(int i, int j, String s)
				throws org.genepattern.io.ParseException {
			if (j == rowNamesColumn) {
				if (expressionHandler != null) {
					expressionHandler.rowName(i, s);
				}
			} else if (j == rowDescriptionsColumn) {
				if (expressionHandler != null) {
					expressionHandler.rowDescription(i, s);
				}
			} else {
				try {
					if (expressionHandler != null) {
						expressionHandler.data(i, j - dataOffset, Double
								.parseDouble(s));
					}
				} catch (NumberFormatException nfe) {
					throw new org.genepattern.io.ParseException("Data at row "
							+ i + " and column " + j + " is not a number.");
				}
			}
		}

		public void endHeader() throws org.genepattern.io.ParseException {
			if (rowNamesColumnSpecified && rowNames != null) {
				throw new org.genepattern.io.ParseException(
						"Row names specifed in header and in data block.");
			}

			if (rowDescriptionsColumnSpecified && rowDescriptions != null) {
				throw new org.genepattern.io.ParseException(
						"Row descriptions specifed in header and in data block.");
			}
			if (rowNames == null) {
				rowNames = new String[rows];
			}
			if (rowDescriptions == null) {
				rowDescriptions = new String[rows];
			}
			if (columnNames == null) {
				throw new org.genepattern.io.ParseException(
						"No column names found.");
			}
			if (columnDescriptions == null) {
				throw new org.genepattern.io.ParseException(
						"No column descriptions found.");
			}
			if (columnTypes == null) {
				throw new org.genepattern.io.ParseException(
						"No column types found.");
			}
			if (columnNames.length != columnDescriptions.length) {
				String[] temp = new String[columnNames.length];
				int stop = Math.min(columnNames.length,
						columnDescriptions.length);
				for (int i = 0; i < stop; i++) {
					temp[i] = columnDescriptions[i];
				}
				//  for(int i = stop; i < columnDescriptions.length; i++) {
				//    temp[i] = columnNames[i];
				//}
				columnDescriptions = temp;

				//  throw new org.genepattern.io.ParseException("Length of column
				// names(" + columnNames.length + ") not equal to length of
				// column descriptions(" + columnDescriptions.length + ").");
			}

			if (columnNames.length != columnTypes.length) {
				throw new org.genepattern.io.ParseException(
						"Length of column names is not equal to the length of column types.");
			}

			if (!rowDescriptionsColumnSpecified && rowDescriptions == null) {
				throw new org.genepattern.io.ParseException(
						"No row descriptions found.");
			}

			if (!rowNamesColumnSpecified && rowNames == null) {
				throw new org.genepattern.io.ParseException(
						"No row names found.");
			}

			if (rowNamesColumnSpecified && !columnTypes[0].equals("String")) {// backward
																			  // compatibilty
				throw new org.genepattern.io.ParseException(
						"0th column type must be String.");
			}
			if (rowDescriptionsColumnSpecified
					&& !columnTypes[1].equals("String")) {// backward
														  // compatibilty
				throw new org.genepattern.io.ParseException(
						"1st column type must be String.");
			}

			if (rowNamesColumnSpecified && rowNamesColumn != 0) {
				throw new org.genepattern.io.ParseException(
						"RowNamesColumn must be column 0.");
			}

			if (rowDescriptionsColumnSpecified && rowDescriptionsColumn != 1) {
				throw new org.genepattern.io.ParseException(
						"RowNamesColumn must be column 1.");
			}

			if (rowNamesColumnSpecified) {
				dataOffset++;
			}
			if (rowDescriptionsColumnSpecified) {
				dataOffset++;
			}
			for (int i = dataOffset; i < columnTypes.length; i++) {// for
																   // compatibilty
																   // with other
																   // odf parser
				if (!columnTypes[i].equals("float")) {
					throw new ParseException("The column type of column " + i
							+ " must be float.");
				}
			}

			columns = columnNames.length;

			if (rowNamesColumn > 0) {// for compatibilty with other odf parser
				throw new ParseException("Row names column must be 0th column.");
			}

			int actualColumns = columns - dataOffset;
			if (expressionHandler != null) {
				expressionHandler.init(rows, actualColumns,
						rowDescriptionsColumnSpecified, true, false);
			}
			if (expressionHandler != null) {
				for (int j = 0; j < actualColumns; j++) {
					expressionHandler
							.columnName(j, columnNames[j + dataOffset]);
					if (columnDescriptions != null) {
						expressionHandler.columnDescription(j,
								columnDescriptions[j + dataOffset]);
					} else {
						expressionHandler.columnDescription(j, "");
					}

				}
			}
		}

		public void header(String key, String[] values) throws ParseException {
			if (key.equalsIgnoreCase("COLUMN_NAMES")) {
				columnNames = values;
			} else if (key.equalsIgnoreCase("ROW_NAMES")) {
				rowNames = values;
			} else if (key.equalsIgnoreCase("ROW_DESCRIPTIONS")) {
				rowDescriptions = values;
			} else if (key.equalsIgnoreCase("COLUMN_DESCRIPTIONS")) {
				columnDescriptions = values;
			} else if (key.equalsIgnoreCase("COLUMN_TYPES")) {
				columnTypes = values;
			} else {
				throw new ParseException("Unknown header " + key);
			}
		}

		public void header(String key, String value) throws ParseException {
			if (key.equalsIgnoreCase("DataLines")) {
				rows = Integer.parseInt(value);
			} else if (key.equalsIgnoreCase("Model")) {
				if (!value.equalsIgnoreCase("Dataset")) {
					throw new ParseException(
							"Expecting an odf file with Model=Dataset.");
				}
				if (testOnly) {
					throw new EndParseException();
				}
			} else if (key.equalsIgnoreCase("RowNamesColumn")) {
				rowNamesColumn = parsePositiveInt(key, value);
				rowNamesColumnSpecified = true;
			} else if (key.equalsIgnoreCase("RowDescriptionsColumn")) {
				rowDescriptionsColumn = parsePositiveInt(key, value);
				rowDescriptionsColumnSpecified = true;
			} else {
				throw new ParseException("Unknown header " + key);
			}
		}

		private int parsePositiveInt(String key, String value)
				throws ParseException {
			try {
				int i = Integer.parseInt(value);
				if (i < 0) {
					throw new ParseException(key + " must be >= 0.");
				}
				return i;
			} catch (NumberFormatException nfe) {
				throw new ParseException(key + " keyword is not an integer.");
			}
		}

	}
}

