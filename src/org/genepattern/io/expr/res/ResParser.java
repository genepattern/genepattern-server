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


package org.genepattern.io.expr.res;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

import org.genepattern.data.expr.ResExpressionData;
import org.genepattern.io.ParseException;
import org.genepattern.io.expr.IExpressionDataHandler;
import org.genepattern.io.expr.IExpressionDataParser;

/**
 * Class for reading a res document using callbacks.
 * 
 * @author Joshua Gould
 */
public class ResParser implements IExpressionDataParser {
	IExpressionDataHandler handler;

	int rows, columns;

	/** the first token on the first line */
	final static String DESCRIPTION = "Description";

	/** the second token on the first line */
	final static String ACCESSION = "Accession";

	LineNumberReader reader;

	/** whether to ensure that a call is one of A, P, M */
	boolean verifyCalls = true;

	public boolean canDecode(InputStream is) throws IOException {
		reader = new LineNumberReader(new BufferedReader(new InputStreamReader(
				is)));
		try {
			readHeader();
			return true;
		} catch (ParseException ioe) {
			return false;
		}
	}

	/**
	 * Parse a res document. The application can use this method to instruct the
	 * res reader to begin parsing an res document from any valid input source.
	 * Applications may not invoke this method while a parse is in progress
	 * (they should create a new ResParser instead ). Once a parse is complete,
	 * an application may reuse the same ResParser object, possibly with a
	 * different input source. During the parse, the ResParser will provide
	 * information about the res document through the registered event handler.
	 * This method is synchronous: it will not return until parsing has ended.
	 * If a client application wants to terminate parsing early, it should throw
	 * an exception.
	 * 
	 * @param is
	 *            The input stream
	 * @throws ParseException -
	 *             Any parse exception, possibly wrapping another exception.
	 * @throws IOException -
	 *             An IO exception from the parser, possibly from a byte stream
	 *             or character stream supplied by the application.
	 */
	public void parse(InputStream is) throws ParseException, IOException {
		reader = new LineNumberReader(new BufferedReader(new InputStreamReader(
				is)));
		readHeader();
		readData();
	}

	private void readData() throws ParseException, IOException {
		int expectedColumns = 2 * columns + 2;
		int rowIndex = 0;
		
		for (String s = reader.readLine(); s != null; s = reader.readLine(), rowIndex++) {
			if (rowIndex >= rows) {
				if (s.trim().equals("")) {// ignore blank lines at the end of
					// the file
					rowIndex--;
					continue;
				}
				int rowsRead = rowIndex + 1;
				throw new ParseException(
						"More data rows than expected on line "
								+ reader.getLineNumber() + ". Read " + rowsRead
								+ ", expected " + rows + ".");
			}
			String[] dataTokens = s.split("\t");
			if (dataTokens.length != expectedColumns) {
				throw new ParseException(dataTokens.length
						+ getColumnString(dataTokens.length) + " on line "
						+ reader.getLineNumber() + ". Expected "
						+ expectedColumns + ".");
			}
			if (handler != null) {
				handler.rowDescription(rowIndex, dataTokens[0]);
			}
			String rowName = dataTokens[1];
			
			if (handler != null) {
				handler.rowName(rowIndex, rowName);
			}

			for (int columnIndex = 0, tokenIndex = 2; columnIndex < columns; columnIndex++, tokenIndex += 2) {
				try {
					double data = Double.parseDouble(dataTokens[tokenIndex]);
					if (handler != null) {
						handler.data(rowIndex, columnIndex, data);
					}
				} catch (NumberFormatException nfe) {
					throw new ParseException("Data at line number "
							+ reader.getLineNumber() + " and column "
							+ columnIndex + " is not a number.");
				}
				String callString = dataTokens[tokenIndex + 1];
				int call = 0;

				if ("P".equals(callString)) {
					call = ResExpressionData.PRESENT;
				} else if ("A".equals(callString)) {
					call = ResExpressionData.ABSENT;
				} else if ("M".equals(callString)) {
					call = ResExpressionData.MARGINAL;
				} else {
					if (verifyCalls) {
						throw new ParseException("Unknown call, " + callString + ", on line " + reader.getLineNumber());
					} else {
						call = ResExpressionData.ABSENT;
					}
				}
				if (handler != null) {
					handler.call(rowIndex, columnIndex, call);
				}
			}
		}

		if (rowIndex != rows) {// check to see if there are less rows than
			// specified
			throw new ParseException("Missing data rows. Read " + rowIndex
					+ " " + getRowString(rowIndex) + ", expected " + rows);
		}
	}

	private void readHeader() throws ParseException, IOException {

		// 1st header line: Description <tab> Accession <tab> <sample 1> <tab>
		// <tab> <sample 2> ...
		// (column names)
		String sampleIdLine = reader.readLine();
		List sampleNamesList = new ArrayList();
		
		// sample names
		if (sampleIdLine != null && sampleIdLine.length() > 0) {
			String[] tokens = sampleIdLine.split("\t");
			if (tokens == null || tokens.length == 0 || tokens.length == 1) {
				throw new ParseException("Unable to parse line 1");
			}

			String desc = tokens[0];// Description
			if (!DESCRIPTION.equalsIgnoreCase(desc)) {
				// System.out
				// .println("Warning: First line should start with
				// 'Description'");
			}

			String acc = tokens[1];// Accession
			if (!ACCESSION.equalsIgnoreCase(acc)) {
				// System.out
				// .println("Warning: First line second token should be
				// 'Accession'");
			}

			int sampleIndex = 2;
			for (int j = 1, length = tokens.length; sampleIndex < length; sampleIndex += 2, j++) {
				String sampleName = tokens[sampleIndex].trim();
				sampleNamesList.add(sampleName);
			}
			if (sampleIndex - 1 != tokens.length) {
				for (int i = sampleIndex - 2; i < tokens.length; i++) {
					sampleNamesList.add(tokens[i]);
				}
				throw new ParseException(
						"Line 1 does not contain the correct number of tabs. Column names: "
								+ sampleNamesList);
			}
		} else {
			throw new ParseException("Missing column names on line "
					+ reader.getLineNumber());
		}

		columns = sampleNamesList.size();
		if (columns == 0) {
			throw new ParseException(
					"Number of columns must be greater than 0.");
		}

		// 2nd Line format: (tab) (sample 1 description) (tab) (tab) (sample 2
		// description) (tab) (tab) ... (sample N description)

		String sampleDescriptionsLine = reader.readLine();
		String[] sampleDescriptions = new String[columns];

		if (sampleDescriptionsLine != null
				&& sampleDescriptionsLine.length() > 0) {
			String[] sampleDescriptionsTokens = sampleDescriptionsLine
					.split("\t");
			int expectedTokens = columns * 2;
			if (sampleDescriptionsTokens.length != expectedTokens) {
				int tokens = (int) Math
						.ceil(sampleDescriptionsTokens.length / 2.0);
				/*
				 * System.out .println("Warning: Incorrect number of sample
				 * descriptions on line " + reader.getLineNumber() + ". Expected " +
				 * columns + " descriptions, got " + tokens + "
				 * descriptions.");// warn user
				 */
			}

			int index = 0;
			for (int j = 1, length = Math.min(expectedTokens,
					sampleDescriptionsTokens.length); j < length; j += 2) {
				sampleDescriptions[index++] = sampleDescriptionsTokens[j]
						.trim();
			}

			if (index < columns) {// if less sample descriptions than
									// required,
				// fill remaining with empty string
				for (int j = index; j < columns; j++) {
					sampleDescriptions[j] = "";
				}
			}
		} else {
			// this doesn't adhere to the format, but GeneCluster outputs a
			// blank sample descriptions line
			java.util.Arrays.fill(sampleDescriptions, "");
		}

		String numberOfRowsLine = reader.readLine();

		if (numberOfRowsLine != null && numberOfRowsLine.trim().length() > 0) {
			try {
				rows = Integer.parseInt(numberOfRowsLine.trim());
			} catch (NumberFormatException e) {
				throw new ParseException("Number of rows missing.");
			}
		} else {
			throw new ParseException("Number of rows missing.");
		}
		if (rows <= 0) {
			throw new ParseException("Number of rows must be greater than 0.");
		}
		if (handler != null) {
			handler.init(rows, columns, true, true, true);
		}
		if (handler != null) {
			for (int j = 0; j < columns; j++) {
				handler.columnName(j, (String) sampleNamesList.get(j));
			}
		}
		if (handler != null) {
			for (int j = 0; j < columns; j++) {
				handler.columnDescription(j, sampleDescriptions[j]);
			}
		}
		// At this point, curLine should contain the first data line
		// data line: <row desc> <tab> <row label> <tab> <ex1> <tab> <call1>
		// <tab> <ex2> <tab> <call2>
	}

	public void setHandler(IExpressionDataHandler handler) {
		this.handler = handler;
	}

	private String getRowString(int rows) {
		if (rows == 1) {
			return "row";
		}
		return "rows";
	}

	private String getColumnString(int cols) {
		if (cols == 1) {
			return "column";
		}
		return "columns";
	}

}
