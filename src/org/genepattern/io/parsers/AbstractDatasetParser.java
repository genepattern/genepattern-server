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
/*
 * AbstractDatasetFileReader.java
 *
 * Created on May 1, 2002, 7:21 PM
 */

package org.genepattern.io.parsers;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.genepattern.data.DataObjector;
import org.genepattern.data.Dataset;
import org.genepattern.data.DefaultDataset;
import org.genepattern.data.DefaultMatrix;
import org.genepattern.data.DefaultNamesPanel;
import org.genepattern.data.annotation.AnnotationFactory;
import org.genepattern.data.annotation.PrimeAnnotationFactory;
import org.genepattern.io.DefaultSummaryInfo;
import org.genepattern.io.SummaryError;
import org.genepattern.io.SummaryInfo;
import org.genepattern.util.GPpropertiesManager;
import org.genepattern.util.ReusableStringTokenizer;
import org.genepattern.util.StringUtils;

/**
 * Subclasses can read res and gct files Note that once instanciated the only
 * public method of interest is getDataset()
 * 
 * @author KOhm
 * @version 1.0
 */
public abstract class AbstractDatasetParser extends AbstractDataParser {

	/**
	 * Creates new AbstractDatasetParser
	 * 
	 * @param lenient
	 *            true then this will parse leniently
	 * @param extensions
	 *            supported file extensions
	 */
	protected AbstractDatasetParser(final boolean lenient,
			final String[] extensions) {
		super(extensions);
	}

	//    /** Creates new AbstractDatasetParser using the lieniency defined in the
	// properties*/
	//    protected AbstractDatasetParser(final InputStream input, final String
	// ds_name, final String[] extensions) throws IOException,
	// org.genepattern.io.ParseException{
	//        this(input, ds_name, IS_LENIENT, extensions);
	//    }
	//    /** Creates new AbstractDatasetParser using the lieniency defined in the
	// properties*/
	//    protected AbstractDatasetParser(final String ds_name, final String[]
	// extensions) throws IOException, org.genepattern.io.ParseException{
	//        this(null, ds_name, IS_LENIENT, extensions);
	//    }

	// abstract methods
	/**
	 * creates a new instance of a Concrete implementation of
	 * AbstractInnerParser
	 * 
	 * @param reader
	 *            the line reader
	 * @return AbstractInnerParser
	 */
	abstract protected AbstractInnerParser createInnerParser(
			final LineReader reader);

	// DataParser interface signature methods

	/**
	 * Parses the InputStream to create a DataObjector instance with the
	 * specified name
	 * 
	 * FIXME not thread safe. Need to eliminate all the variables that store
	 * state should have no instance fields
	 * 
	 * @return the resulting data object
	 * @param name
	 *            the name for the DataObjector
	 * @param in
	 *            the input stream
	 * @throws IOException
	 *             if a problem occurs due to an I/O operation
	 * @throws org.genepattern.io.ParseException
	 *             if there is a problem with the format or content of the data
	 */
	public DataObjector parse(final InputStream in, final String name)
			throws IOException, java.text.ParseException {
		final AbstractInnerParser parser = createInnerParser(new LineReader(in));
		parser.dataset_name = name;
		return parser.readStream();
		//return parser.dataset;
	}

	/**
	 * determines if the data in the input stream can be decoded by this
	 * implementation Note that this assumes that its reading from the beginning
	 * of the stream.
	 * 
	 * @param in
	 *            the input stream
	 * @throws IOException
	 *             if a problem occurs due to an I/O operation
	 * @return true if this implementation can decode the data in the
	 *         InputStream
	 */
	public boolean canDecode(final InputStream in) throws IOException {
		final AbstractInnerParser parser = createInnerParser(new LineReader(in));
		try {
			parser.readHeader();
			return true;
		} catch (java.text.ParseException ex) {
			REPORTER.logWarning(getClass() + " cannot decode stream "
					+ ex.getMessage());
		} catch (NumberFormatException ex) {
			REPORTER.logWarning(getClass() + " cannot decode stream "
					+ ex.getMessage());
		} catch (EOFException ex) {
			REPORTER.logWarning(getClass() + " cannot decode stream "
					+ ex.getMessage());
		}
		return false;
	}

	/**
	 * reads the header lines and creates a summary
	 * 
	 * @param in
	 *            the input stream
	 * @throws IOException
	 *             if an error occures durring an i/o operation
	 * @throws org.genepattern.io.ParseException
	 *             if there is a problem with the format of the data
	 * @return SummaryInfo, the summary of some of the attributes of this data
	 */
	public SummaryInfo createSummary(final InputStream in) throws IOException,
			java.text.ParseException {
		final AbstractInnerParser parser = createInnerParser(new LineReader(in));
		final Map primary = new HashMap();
		final Map secondary = new HashMap();
		Exception exception = null;
		try {
			//total = 0; // reset
			secondary.put("Size=", (long) (Math.ceil(in.available() / 1024.0))
					+ " KB");
			parser.readHeader(primary, secondary);
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
			return new SummaryError(null, exception, this, primary, secondary);
		return new DefaultSummaryInfo(primary, secondary, Dataset.DATA_MODEL);
	}

	/**
	 * reads the header lines and returns them as a String
	 * 
	 * @param in
	 *            input stream
	 * @throws IOException
	 *             if an error occures durring an i/o operation
	 * @throws org.genepattern.io.ParseException
	 *             if a problem with the data format
	 * @return String the header
	 */
	public String getFullHeader(final InputStream in) throws IOException,
			java.text.ParseException {
		final StringBuffer buf = new StringBuffer();
		final AbstractInnerParser parser = createInnerParser(new LineRecorder(
				in, buf));
		try {
			parser.readHeader();
		} catch (java.text.ParseException ex) {
			REPORTER.logWarning(getClass() + " cannot decode stream "
					+ ex.getMessage());
		} catch (NumberFormatException ex) {
			REPORTER.logWarning(getClass() + " cannot decode stream "
					+ ex.getMessage());
		}
		return buf.toString();
	}

	// fields
	/** the array of strings that can represent a "Not a number" number */
	protected static final String[] NAN_REPRESENTATIONS;

	/** the array of strings that can represent negative infinity */
	protected static final String[] NEGATIVE_INFINITYS;

	/** the array of strings that can represent positive infinity */
	protected static final String[] POSITIVE_INFINITYS;

	/** the default value for is_lenient */
	protected static final boolean IS_LENIENT;

	/**
	 * the key for specifying which strings represent not a number for the
	 * AbstractDatasetParser The tokens are seperated by commas ',' and the
	 * tokens are case-insensitive.
	 */
	protected static final String NAN_REPRESENTATIONS_KEY = "gp.parser.dataset.representations.nan";

	/**
	 * the key for specifying which strings represent negative infinity for the
	 * AbstractDatasetParser The tokens are seperated by commas ',' and the
	 * tokens are case-insensitive.
	 */
	protected static final String NEGATIVE_INFINITYS_KEY = "gp.parser.dataset.representations.-infinity";

	/**
	 * the key for specifying which strings represent positive infinity for the
	 * AbstractDatasetParser The tokens are seperated by commas ',' and the
	 * tokens are case-insensitive.
	 */
	protected static final String POSITIVE_INFINITYS_KEY = "gp.parser.dataset.representations.infinity";

	/**
	 * the key for specifying if the file parser should be strict about
	 * interpreting the anomalous data numbers such as "NaN", "-Infinity", etc.
	 * and don't need to have gene names etc. The tokens are case-insensitive:
	 * "true", "True", "TRUE" are the same.
	 * 
	 * @see org.genepattern.io.parsers.AbstractDatasetParser
	 */
	protected static final String STRICT_DATASET_PARSING_KEY = "gp.parser.dataset.strict";
	/** static initializer */
	static {
		// initialize the arrays
		final char delim = ',';
		boolean strict = false;
		try {
			strict = GPpropertiesManager
					.getBooleanProperty(STRICT_DATASET_PARSING_KEY);
		} catch (java.text.ParseException ex) {
			REPORTER.logWarning(ex.toString());
		}
		IS_LENIENT = !strict;

		final String infinities = GPpropertiesManager.getProperty(
				POSITIVE_INFINITYS_KEY, "Infinity");
		POSITIVE_INFINITYS = StringUtils.splitStrings(infinities, delim);

		final String neginfinities = GPpropertiesManager.getProperty(
				NEGATIVE_INFINITYS_KEY, "-Infinity");
		NEGATIVE_INFINITYS = StringUtils.splitStrings(neginfinities, delim);

		final String nans = GPpropertiesManager.getProperty(
				POSITIVE_INFINITYS_KEY, "NaN");
		NAN_REPRESENTATIONS = StringUtils.splitStrings(nans, delim);

	}

	// I N N E R C L A S S E S
	/** inner class parser where most of the work is done */
	protected abstract static class AbstractInnerParser {

		AbstractInnerParser(final boolean is_lenient, final LineReader reader) {
			this.is_lenient = is_lenient;
			this.reader = reader;
		}

		/**
		 * reads the stream and creates a <CODE>Dataset</CODE> object
		 * 
		 * @throws org.genepattern.io.ParseException
		 *             if problem with format of data
		 * @throws NumberFormatException
		 *             if problem with reading some of the numberic data
		 * @throws IOException
		 *             if problem durring I/O operations
		 * @return Dataset the dataset object
		 */
		protected final Dataset readStream() throws java.text.ParseException,
				NumberFormatException, IOException {
			//total = 0; // reset
			// read header... if created tmp_file then read from that
			final File tmp_file = readHeader();

			//FIXME must set the LineReader.num and total to reflect the
			// numnber
			// of lines read by readHeader
			final LineReader reader = (tmp_file == null) ? this.reader
					: new LineReader(tmp_file);
			this.reader = reader;
			parseDataLines();
			reader.close();
			fixDuplicateLabels();
			//        fixExceptionalValues();

			AnnotationFactory rfactory = PrimeAnnotationFactory
					.createAnnotationFactory(this.row_labels,
							this.row_descriptions);
			AnnotationFactory cfactory = PrimeAnnotationFactory
					.createAnnotationFactory(this.column_labels,
							this.column_descriptions);
			return new DefaultDataset(this.dataset_name, new DefaultMatrix(
					this.row_count, this.column_count, this.data_array),
					new DefaultNamesPanel(this.row_labels, rfactory),
					new DefaultNamesPanel(this.column_labels, cfactory));
		}

		//abstract methods
		/**
		 * reader method may return a file if the rest of the input stream was
		 * dumped
		 * 
		 * @param primary
		 *            were primary summary info goes
		 * @param secondary
		 *            were secondary summary info goes
		 * @throws org.genepattern.io.ParseException
		 *             if problem with the data's format
		 * @throws NumberFormatException
		 *             problem with the format of a number
		 * @throws IOException
		 *             problem durring I/O operation
		 * @return File if a temp file was created, otherwise null
		 */
		abstract protected File readHeader(final Map primary,
				final Map secondary) throws java.text.ParseException,
				NumberFormatException, IOException;

		/** get the max number of tokens exzpected on a line */
		abstract int getMaxNumEntriesPerLine();

		/** subclass processes each line */
		abstract void processDataTokens(int row, String[] tokens, int start,
				int end) throws NumberFormatException;

		/**
		 * the start number for the first column
		 * 
		 * @return int the index of the first data column
		 */
		abstract protected int getStartColumn();

		/**
		 * returns true if the label column is before the description column
		 * 
		 * @return true if the label is before the description
		 */
		abstract protected boolean isLabelFirst();

		/**
		 * final check to make sure the data is ok
		 * 
		 * @param row
		 *            the row were the EOF was found
		 */
		abstract protected void processEOF(int row);

		// helper methods
		/**
		 * like the abstract method of the same name but does not expect to
		 * record the attributes
		 * 
		 * @throws org.genepattern.io.ParseException
		 *             if problem with data's format
		 * @throws NumberFormatException
		 *             if expected number is not one
		 * @throws IOException
		 *             if problem durring I/O operation
		 * @return File the temp file to read from or null
		 */
		protected File readHeader() throws java.text.ParseException,
				NumberFormatException, IOException {
			return readHeader(null, null);
		}

		/**
		 * Parses the token into a float If is_lenient is true then converts
		 * anomalous values into their primative representations
		 * 
		 * This method is intended to be used in processDataTokens(...)
		 * 
		 * @param value
		 *            text version of the value
		 * @param r
		 *            its' row
		 * @param c
		 *            its' column position
		 * @return float the primitive value
		 */
		protected final float parse(final String value, final int r, final int c) {
			try {
				return Float.parseFloat(value);
			} catch (NumberFormatException ex) {
				if (this.is_lenient) {
					if (containsString(value, NAN_REPRESENTATIONS)) {
						REPORTER.logWarning("Warning: parsed \"" + value
								+ "\" at row " + r + " and column " + c
								+ ",\n which " + "represents 'not a number'\n"
								+ "in dataset: " + this.dataset_name);

						return Float.NaN;

					} else if (containsString(value, NEGATIVE_INFINITYS)) {
						REPORTER.logWarning("Warning: parsed \"" + value
								+ "\" at row " + r + " and column " + c
								+ ",\n which "
								+ "represents 'negative infinity'\n"
								+ "in dataset: " + this.dataset_name);

						return Float.NEGATIVE_INFINITY;

					} else if (containsString(value, POSITIVE_INFINITYS)) {
						REPORTER.logWarning("Warning: parsed \"" + value
								+ "\" at row " + r + " and column " + c
								+ ",\n which "
								+ "represents 'positive infinity'\n"
								+ "in dataset: " + this.dataset_name);

						return Float.POSITIVE_INFINITY;
					}
				}
				// if not lenient or value is not NaN, -Infinity, Infinity then
				// rethrow the exception
				throw ex;
			}
		}

		/**
		 * helper method that determines if any element in the array equals the
		 * target ignoring case
		 */
		private static final boolean containsString(final String target,
				final String[] array) {
			final int num = array.length;
			for (int i = 0; i < num; i++) {
				if (target.equalsIgnoreCase(array[i]))
					return true;
			}
			return false;
		}

		/**
		 * reads a line
		 * 
		 * @throws IOException
		 *             if an error happens while I/O operation
		 * @throws org.genepattern.io.ParseException
		 *             if a problem with the data's format is detected
		 */
		protected void parseDataLines() throws IOException,
				java.text.ParseException {
			final boolean label_first = isLabelFirst();
			String curLine = reader.readLine();
			if (curLine == null)
				processEOF(0);
			final int start = getStartColumn();
			final int max_num_per_line = getMaxNumEntriesPerLine();
			final String[] tokens = new String[max_num_per_line + start - 2];
			final int row_offset = this.lines_read;
			final String[] labels = this.row_labels;
			final String[] descs = this.row_descriptions;
			int tok_count = 0;
			int row = 1;
			final String empty = "";
			final String gene = "gene";
			int no_label_count = 0;
			final String delim = "\t";
			boolean no_desc = false, no_label = false;
			int count = 0;
			final ReusableStringTokenizer tokenizer = this.tokenizer;
			try {
				for (int r = row - 1; curLine != null; curLine = reader
						.readLine()) {
					this.lines_read++;
					tokenizer.resetTo(curLine, delim, false);
					tok_count = tokenizer.countTokens();
					if (tok_count == max_num_per_line) {
						if (label_first) {
							labels[r] = tokenizer.nextToken();
							descs[r] = tokenizer.nextToken();
						} else {
							descs[r] = tokenizer.nextToken();
							labels[r] = tokenizer.nextToken();
						}
						no_desc = no_label = false;
					} else if (tok_count == (max_num_per_line - 1)) {// assume
																	 // no
																	 // description
						labels[r] = tokenizer.nextToken();
						descs[r] = empty;
						no_desc = true;
						no_label = false;
					} else if (this.is_lenient
							&& tok_count == (max_num_per_line - 2)) {
						// assume no name nor description
						labels[r] = gene + no_label_count; // gene0 gene1 ...
						descs[r] = empty;
						no_desc = no_label = true;
					} else {
						throw new java.text.ParseException(
								(tok_count > max_num_per_line ? "More" : "Less")
										+ " data elements  ("
										+ tok_count
										+ ") than expected ("
										+ max_num_per_line
										+ ") on data row "
										+ row + getExceptionMessageTrailer(), 0);
					}
					count = start;
					for (; tokenizer.hasMoreTokens(); count++) {
						tokens[count] = tokenizer.nextToken().trim();
						//System.out.println (count+" tok="+tokens[count]);
					}
					count--;
					processDataTokens(row, tokens, start, count);
					row++;
					r++;
				}
			} catch (ArrayIndexOutOfBoundsException ex) {
				if (row > this.row_count)
					throw new java.text.ParseException(
							"Too many lines of data! Read " + (row)
									+ " expected " + this.row_count
									+ getExceptionMessageTrailer(), 0);
				int c = count - start + 1;
				if ((c + 1) > this.column_count)
					throw new java.text.ParseException(
							"Too many data value(s)at line " + this.lines_read
									+ ": expected " + this.column_count
									+ " got " + (c + 1)
									+ getExceptionMessageTrailer(), 0);
				REPORTER.logWarning("row=" + row + " NUM_ROWS="
						+ this.row_count + " c=" + c + " NUM_COLS="
						+ this.column_count);
				throw ex; // other internal problem...
			} catch (NumberFormatException ex) {
				if (no_desc || no_label)
					throw new java.text.ParseException("Less data elements ("
							+ tok_count + ") than expected ("
							+ max_num_per_line + ")\n"
							+ "Failed at: data element "
							+ (this.current_column) + ", token:\""
							+ tokens[this.current_column] + "\""
							+ getExceptionMessageTrailer(), 0);
				else {
					throw new java.text.ParseException(
							"Expected a number got text at line "
									+ this.lines_read + " data element "
									+ (this.current_column) + " token:\n\""
									+ tokens[this.current_column] + "\"\n"
									+ " with Label: \"" + labels[row - 1]
									+ "\"\n" + "Description: \""
									+ descs[row - 1] + "\""
									+ getExceptionMessageTrailer(), 0);
				}
			}
			row--;
			processEOF(row);
			if (row < this.row_count)
				throw new java.text.ParseException("Missing data rows! Read "
						+ (row) + " expected " + this.row_count
						+ getExceptionMessageTrailer(), 0);
			else if (row > this.row_count)
				throw new java.text.ParseException("Too many data rows! Read "
						+ (row) + " expected " + this.row_count
						+ getExceptionMessageTrailer(), 0);
		}

		/**
		 * The trailer for the error message. The line number or etc.
		 * 
		 * @return String the trailer
		 */
		protected final String getExceptionMessageTrailer() {
			return "\nLine " + this.lines_read + ": " + getPrettyCurrentLine()
					+ "\nName: " + this.dataset_name;
		}

		/**
		 * for debug or exception message text
		 * 
		 * @return String the nice looking current line
		 */
		protected final String getPrettyCurrentLine() {
			final String curLine = reader.getCurrentLine();
			if (curLine == null)
				return "[Empty Line]";
			final String tab = "<TAB>";
			final char char_tab = '\t';
			StringBuffer line = new StringBuffer(curLine);
			for (int i = line.length() - 1; i >= 0; i--) {
				if (char_tab == line.charAt(i)) {
					line.replace(i, i + 1, tab);
				}
			}
			// break the long lines up
			final char left = '<', right = '>';
			for (int i = 90; i < line.length(); i += 100) {
				int limit = line.length() - 5;
				for (; i < limit; i++) {
					if (line.charAt(i) == left && line.charAt(i + 4) == right) {
						line.insert(i, '\n');
						break;
					}
				}
			}
			return line.toString();
		}

		/** checks for duplicate accessions and fixes them by appending _1 _2 etc */
		protected final void fixDuplicateLabels() {
			// check rows
			String[] labels = this.row_labels;
			if (labels != null)
				fixDuplicateLabels(labels);
			//check columns
			labels = this.column_labels;
			if (labels != null)
				fixDuplicateLabels(labels);
		}

		private final void fixDuplicateLabels(final String[] labels) {
			final int len = labels.length;
			final java.util.TreeMap map = new java.util.TreeMap();
			final Integer zero = new Integer(0);
			Integer value;
			boolean do_over = false;
			//System.out.println("Seaching for duplicate labels:");
			for (int i = 0; i < len; i++) {
				this.num_labels_checked++;
				//            fParams.notifyListeners(IAlgListener.kAlgIterated);
				do {
					value = (Integer) map.put(labels[i], zero);
					if (value != null) {
						int v = value.intValue() + 1;
						// replace the old Integer with one greater
						map.put(labels[i], new Integer(v));
						//change the current label to be unique
						REPORTER.logWarning(v + " duplicates found for ("
								+ labels[i] + ")");
						labels[i] += "_" + v;
						do_over = true;
					} else
						do_over = false;
				} while (do_over); // if changed labels[i] then add it also
			}
			//System.out.println("dups done");
		}

		/**
		 * changes the NaN values to be something else i.e. Infinity or
		 * -Infinity
		 */
		protected void fixExceptionalValues() {
			final int climit = this.column_count, rlimit = this.row_count;
			//        final float [][] data = this.getData();
			final float nan = Float.NaN;
			REPORTER.logWarning("num rows=" + rlimit + " cols=" + climit);
			// processing each column
			final boolean[] is_nan = new boolean[rlimit];
			for (int c = 0; c < climit; c++) {
				int pos = 0, neg = 0;
				float sum = 0f;
				boolean nan_found = false;
				for (int r = 0; r < rlimit; r++) {
					is_nan[r] = false;
					final float val = data_array[r * this.column_count + c];
					if (val == nan) {
						is_nan[r] = true;
					} else {
						sum += val;
						if (val > 0f) {
							pos++;
						} else if (val < 0f) {
							neg++;
						}
					}
				} // end row loop
				if (nan_found) {
					//fix the column
					for (int r = 0; r < rlimit; r++) {
						if (is_nan[r])
							this.setDataAt(r, c, Float.POSITIVE_INFINITY);
					}
				}
			} // end column loop

		}

		/**
		 * The total number of lines read from the file.
		 * 
		 * @return int the line count
		 */
		protected final int getFileLinesRead() {
			return lines_read + num_labels_checked;
		}

		/**
		 * sets the value at the row and column in the data_array
		 * 
		 * @param r
		 *            the row position
		 * @param c
		 *            the olumn position
		 * @param value
		 *            the value to set
		 */
		protected final void setDataAt(final int r, final int c, float value) {
			data_array[r * column_count + c] = value;
		}

		/**
		 * calculates the total number of lines + labels to check for dups for
		 * 
		 * @param r
		 *            the number of rows
		 * @param c
		 *            the number of columns
		 */
		protected final void setRowColumnCounts(final int r, final int c) {
			if (total != 0)
				throw new IllegalArgumentException(
						"the total number of things to do has been already calculated");
			total = (r * 2) + c;
			row_count = r;
			column_count = c;
			column_labels = new String[c];
			row_labels = new String[r];
			column_descriptions = new String[c];
			row_descriptions = new String[r];
			//java.util.Arrays.fill(column_descriptions, "");
			data_array = new float[r * c];
		}

		// fields
		/** the line reader */
		protected LineReader reader;

		/** name for the dataset */
		protected String dataset_name = "*Not named*";

		/** number of columns */
		protected int column_count;

		/** number of rows */
		protected int row_count;

		/** where the column labels are stored */
		protected String[] column_labels;

		/** where the row labels are stored */
		protected String[] row_labels;

		/** where the column descriptions are stored */
		protected String[] column_descriptions;

		/** where the row descriptions are stored */
		protected String[] row_descriptions;

		/** the data */
		protected float[] data_array;

		//    /** the line that was last read */
		//    protected String curLine = null;
		/** keep track of progress this way */
		protected int lines_read = 0;

		/** the current column being processed */
		protected int current_column = 0;

		/** keeps track of the progress of checking for duplicate labels */
		private int num_labels_checked = 0;

		/**
		 * the total number of lines to read + lines to check for duplicate
		 * labels
		 */
		private int total = 0;

		/** the line tokenizer */
		protected final ReusableStringTokenizer tokenizer = new ReusableStringTokenizer();

		/** the buffered reader */
		//private BufferedReader reader;
		/** the Dataset */
		//protected Dataset dataset;
		/**
		 * if this is true then the parser will try to ignore certain
		 * irregularities but could cause problems with the integrety of the
		 * data
		 */
		protected final boolean is_lenient;
	}
}