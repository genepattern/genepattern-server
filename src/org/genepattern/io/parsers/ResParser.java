/*
 * This software and its documentation are copyright 1999 by the
 * Whitehead Institute for Biomedical Research.  All rights are reserved.
 * 
 * This software is made available for use on a case by case basis, and
 * only with specific written permission from The Whitehead Institute.
 * It may not be redistributed nor posted to any bulletin board, included
 * in any shareware distributions, or the like, without specific written
 * permission from The Whitehead Institute.  This code may be customized
 * by individual users, although such versions may not be redistributed
 * without specific written permission from The Whitehead Institute.
 * 
 * This software is supplied without any warranty or guaranteed support
 * whatsoever.  The Whitehead Institute can not be responsible for its
 * use, misuse, or functionality.
 *
 */

package org.genepattern.io.parsers;

import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.genepattern.data.Dataset;
import org.genepattern.io.DefaultSummaryInfo;
import org.genepattern.io.SummaryError;
import org.genepattern.io.SummaryInfo;

/**
 * Can parse gct input streams and creates a Dataset
 * 
 * @author kohm
 */
public class ResParser extends AbstractDatasetParser {

	/** Creates a new instance of ResParser with system defined leniency */
	public ResParser() {
		super(IS_LENIENT, FILE_EXTENSIONS);//this(null, null);
	}

	/**
	 * creates a new instance of a Concrete implementation of
	 * AbstractInnerParser.
	 * 
	 * @param reader
	 *            the line reader
	 * @return AbstractInnerParser
	 */
	protected AbstractInnerParser createInnerParser(final LineReader reader) {
		return new InnerParser(IS_LENIENT, reader);
	}

	// fields
	/** the first token on the first line */
	public static final String DESCRIPTION = "Description";

	/** the second token on the first line */
	public static final String ACCESSION = "Accession";

	/** the file extensions String array */
	private static final String[] FILE_EXTENSIONS = new String[] { "res" };

	/**
	 * reads the header lines and creates a summary
	 * 
	 * @param in
	 *            the input stream
	 * @throws IOException
	 *             if an error occures durring an i/o operation
	 * @throws ParseException
	 *             if there is a problem with the format of the data
	 * @return SummaryInfo, the summary of some of the attributes of this data
	 */
	public SummaryInfo createSummary(final InputStream in) throws IOException,
			java.text.ParseException {
		final InnerParser parser = (InnerParser) createInnerParser(new LineReader(
				in));
		Exception exception = null;
		Map map = new HashMap();
		try {
			//total = 0; // reset
			map.put("Size=", (long) (Math.ceil(in.available() / 1024.0))
					+ " KB");
			parser.getSummary(map);
		} catch (java.text.ParseException ex) {
			exception = ex;
			REPORTER.logWarning(getClass() + " cannot decode stream "
					+ ex.getMessage());
		} catch (NumberFormatException ex) {
			exception = ex;
			REPORTER.logWarning(getClass() + " cannot decode stream "
					+ ex.getMessage());
		} catch (IOException ex) {
			System.err.println("createSummary error:\n");
			ex.printStackTrace();
			throw ex;
		}
		if (exception != null)
			return new SummaryError(null, exception, this, map, new HashMap());
		return new DefaultSummaryInfo(map, new HashMap(), Dataset.DATA_MODEL);
	}

	// I N N E R C L A S S E S
	/** This is the concrete implementation of the AbstractInnerParser */
	protected final static class InnerParser extends AbstractInnerParser {

		InnerParser(final boolean is_lenient, final LineReader reader) {
			super(is_lenient, reader);
		}

		/**
		 * subclass calls this method to start reading
		 * 
		 * @param primary
		 *            where the main summary information is stored
		 * @param secondary
		 *            where the less important summary information is stored
		 * @throws NumberFormatException
		 *             if couldn't parse a number where a number was expected
		 * @throws ParseException
		 *             if a format problem was detected in the data
		 * @throws IOException
		 *             if error occurs during an I/O operation
		 * @return a temp file if needed or null
		 */
		protected void getSummary(Map map) throws IOException,
				java.text.ParseException {

			final LineReader reader = this.reader;
			reader.setExpectEOF(false);

			String floatStr = null;
			int countStartLine = 0;

			// 1st header line: Description <tab> Accession <tab> <sample 1>
			// <tab> <tab> <sample 2> ...
			// (column names)
			String curLine = reader.readLine();
			countStartLine++;
			lines_read++;
			int columns = 0;
			int rows = 0;
			if (curLine.length() > 0) {
				StringTokenizer st = new StringTokenizer(curLine, "\t");
				columns = st.countTokens() - 2;
			}

			// 2nd header line, chip scaling factors or blank
			// <tab> CH1999021515AA <tab> <tab> CH1999021306AA/scale
			// factor=0.9564 <tab> <tab>
			reader.setSkipBlankLines(false);
			curLine = reader.readLine();

			// 3rd header line optional, number of rows or blank or first data
			// line
			curLine = reader.readLine();
			lines_read++;

			// Try parsing to see if there is a numRows field

			if (curLine.trim().length() > 0) {
				String intStr = null;
				try {
					// parseInt does not work if there is extra white-space
					intStr = curLine.trim();
					rows = Integer.parseInt(intStr);
				} catch (NumberFormatException e) {
					throw new java.text.ParseException(
							"Parser expecting number of rows on third line", 0);
				}
			} else {
				throw new java.text.ParseException(
						"Parser expecting number of rows on third line", 0);
			}
			map.put("Rows=", "" + rows);
			map.put("Columns=", "" + columns);
		}

		/**
		 * subclass calls this method to start reading
		 * 
		 * @param primary
		 *            where the main summary information is stored
		 * @param secondary
		 *            where the less important summary information is stored
		 * @throws NumberFormatException
		 *             if couldn't parse a number where a number was expected
		 * @throws ParseException
		 *             if a format problem was detected in the data
		 * @throws IOException
		 *             if error occurs during an I/O operation
		 * @return a temp file if needed or null
		 */
		protected final File readHeader(final Map primary, final Map secondary)
				throws java.text.ParseException, NumberFormatException,
				IOException {
			final LineReader reader = this.reader;
			reader.setExpectEOF(false);
			// have to create a temp file if there is no numRows
			File tmp_file = null;
			String floatStr = null;
			int countStartLine = 0;

			// 1st header line: Description <tab> Accession <tab> <sample 1>
			// <tab> <tab> <sample 2> ...
			// (column names)
			String curLine = reader.readLine();
			countStartLine++;
			lines_read++;

			final java.util.List colArr = new ArrayList();
			if (curLine.length() > 0) {
				StringTokenizer st = new StringTokenizer(curLine, "\t");
				if (st.hasMoreTokens()) {
					final String tk = st.nextToken().trim(); // Description
					if (!ResParser.DESCRIPTION.equalsIgnoreCase(tk)) {
						throw new java.text.ParseException(
								"Not a 'res' file - first line "
										+ "does not start with \""
										+ DESCRIPTION + "\"!", 0);
					}
				}
				if (st.hasMoreTokens()) {
					final String tk = st.nextToken().trim(); // Accession
					if (!ACCESSION.equalsIgnoreCase(tk)) {
						throw new java.text.ParseException(
								"Not a 'res' file - first line second token"
										+ "is not \"" + ACCESSION + "\"!", 0);
					}
				}
				while (st.hasMoreTokens()) {
					// System.out.println(st.nextToken());
					colArr.add(st.nextToken());
				}
				// if( primary != null ) primary.put("COLUMN_NAMES:", colArr);
			}

			// 2nd header line, chip scaling factors or blank
			// <tab> CH1999021515AA <tab> <tab> CH1999021306AA/scale
			// factor=0.9564 <tab> <tab>
			reader.setSkipBlankLines(false);
			curLine = reader.readLine();

			final java.util.List descArr = new ArrayList();
			if (curLine.length() > 0) {
				final StringTokenizer st = new StringTokenizer(curLine, "\t");

				while (st.hasMoreTokens()) {
					// System.out.println(st.nextToken());
					descArr.add(st.nextToken());
				}
				// if( primary != null ) primary.put("COLUMN_DESCRIPTIONS:",
				// descArr);
			}

			reader.setSkipBlankLines(true);
			countStartLine++;
			lines_read++;
			//FIXME must process line into tokens
			//String sample_descriptions = curLine;
			//if( primary != null ) primary.put("COLUMN_DESCRIPTIONS:",
			// col_descs);

			// mark the current position, we may have to come back here
			//long tmpDataOffset = fBytesRead;

			// 3rd header line optional, number of rows or blank or first data
			// line
			curLine = reader.readLine();
			lines_read++;
			int numRows = 0, numCols = colArr.size();

			// Try parsing to see if there is a numRows field
			boolean haveNumRows = false;
			if (curLine.trim().length() > 0) {
				String intStr = null;
				try {
					// parseInt does not work if there is extra white-space
					intStr = curLine.trim();
					numRows = Integer.parseInt(intStr);
					haveNumRows = true;
					// exception here means ("ResParser::no or invalid numRows
					// specified");
				} catch (NumberFormatException e) {
					REPORTER
							.logWarning("ResParser::Expecting a number - the number of rows\nNo or invalid number of rows specified:\n"
									+ curLine);
					if (secondary != null)
						secondary
								.put("Warning=",
										"Parser expecting number of rows on third line");
					// rereading is safer than using
					// reader.mark(some_buffer_size_too_small)
					// before reading this line
					// an then in here (caught Exception) doing: reader.reset();

					// so count em...

					tmp_file = org.genepattern.io.StorageUtils
							.createTempFile("res-like");
					final BufferedWriter writer = new BufferedWriter(
							new FileWriter(tmp_file));
					// count lines and dumps to a temp file
					int count = 0;
					String tmp_line = intStr;
					try {
						do {
							count++;
							writer.write(tmp_line);
							writer.newLine();
							tmp_line = reader.readLine();
						} while (tmp_line != null);
					} catch (EOFException ex) {
					}
					numRows = count;
					writer.close();
					reader.close();
				}
			} else
				countStartLine++;

			setRowColumnCounts(numRows, numCols);
			if (primary != null) {
				primary.put("Rows=", new Integer(numRows));
				primary.put("Columns=", new Integer(numCols));
			}

			// Now that parent has been initialized, assign column labels
			this.column_labels = new String[this.column_count];
			colArr.toArray(column_labels);

			this.column_descriptions = new String[this.column_count];
			descArr.toArray(column_descriptions);

			reader.setExpectEOF(true);

			//        // At this point, curLine should contain the first data line
			//        // data line: <row desc> <tab> <row label> <tab> <ex1> <tab>
			// <call1> <tab> <ex2> <tab> <call2>
			//        System.out.println("ResParser::Starting to read the data
			// portion");
			return tmp_file;

		}

		/** get the max number of tokens exzpected on a line */
		int getMaxNumEntriesPerLine() {
			return 2 + this.column_count * 2;
		}

		/**
		 * the start number for the first column
		 * 
		 * @return int, index of column where main data begins
		 */
		protected int getStartColumn() {
			return 1;
		}

		/**
		 * subclass processes each line
		 * 
		 * @throws NumberFormatException
		 *             if couldn't parse a number where a number was expected
		 */
		void processDataTokens(final int row, final String[] tokens,
				final int start, final int end) throws NumberFormatException {
			final int r = row - 1;
			//final int limit = end - start + 1;
			int i = start;
			try {
				// skip every other one (ignore AP calls)
				for (int c = 0; i <= end; i += 2, c++) {
					setDataAt(r, c, parse(tokens[i], r, c));
				}
			} finally { // for debug purposes (current_column is used in the
						// Exception catch block)
				current_column = i;
			}
		}

		/**
		 * final check to make sure the data is ok
		 * 
		 * @param row
		 *            the row where end of file was found
		 */
		protected void processEOF(int row) {
		}

		/**
		 * returns true if the label column is before the description column
		 * 
		 * @return true if the label column is before the description column
		 */
		protected boolean isLabelFirst() {
			return false;
		}
	}

}