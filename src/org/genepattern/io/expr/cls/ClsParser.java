package org.genepattern.io.expr.cls;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Class for reading cls files using callbacks.
 * <P>
 * 
 * The CLS files are simple files created to load class information into
 * GeneCluster. These files use spaces to separate the fields.
 * </P>
 * <UL>
 * <LI>The first line of a CLS file contains numbers indicating the number of
 * samples and number of classes. The number of samples should correspond to the
 * number of samples in the associated RES or GCT data file.</LI>
 * 
 * <UL>
 * <LI>Line format: (number of samples) (space) (number of classes) (space) 1
 * </LI>
 * <LI>For example: 58 2 1</LI>
 * </UL>
 * 
 * <LI>The second line in a CLS file contains names for the class numbers. The
 * line should begin with a pound sign (#) followed by a space.</LI>
 * 
 * <UL>
 * <LI>Line format: # (space) (class 0 name) (space) (class 1 name)</LI>
 * 
 * <LI>For example: # cured fatal/ref.</LI>
 * </UL>
 * 
 * <LI>The third line contains numeric class labels for each of the samples.
 * The number of class labels should be the same as the number of samples
 * specified in the first line.</LI>
 * <UL>
 * <LI>Line format: (sample 1 class) (space) (sample 2 class) (space) ...
 * (sample N class)</LI>
 * <LI>For example: 0 0 0 ... 1
 * </UL>
 * 
 * </UL>
 * 
 * 
 * @author kohm
 * @author Joshua Gould
 */
public class ClsParser {
	final static String formatName = "cls";

	List suffixes = Collections.unmodifiableList(Arrays
			.asList(new String[] { "cls" }));

	BufferedReader reader;

	int numClasses;

	int numItems;

	IClsHandler handler;

	public boolean canDecode(InputStream in) throws IOException {
		try {
			this.reader = new BufferedReader(new java.io.InputStreamReader(in));
			processHeader();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Parses the <CODE>InputStream</CODE> to create a <CODE>ClassVector
	 * </CODE> instance
	 * 
	 * @param is
	 *            The input stream
	 * @exception org.genepattern.io.ParseException
	 *                If there is a problem with the data
	 * @throws IOException
	 *             if an I/O error occurs while reading the stream.
	 */
	public void parse(InputStream is) throws IOException,
			org.genepattern.io.ParseException {
		this.reader = new BufferedReader(new java.io.InputStreamReader(is));
		read();
	}

	void read() throws IOException, org.genepattern.io.ParseException {
		processHeader();// <num_data> <num_classes> 1
		String classifierLine = reader.readLine();
		String[] names = null;
		int[] levels = null;
		String dataLine = null;
		String[] assignments = null;
		Map classNumber2NameMap = new HashMap();

		if (hasClassNames(classifierLine)) {
			names = processClassifier(classifierLine, numClasses);
			for (int i = 0, length = names.length; i < length; i++) {
				classNumber2NameMap.put(new Integer(i), names[i]);
			}
			dataLine = reader.readLine();
			assignments = processData(dataLine, numClasses, numItems,
					classNumber2NameMap);
		} else {// assume classifier line was skipped (second line) so try it as
				// data
			for (int i = 0; i < numClasses; i++) {
				classNumber2NameMap.put(new Integer(i), "Class " + i);
			}
			dataLine = classifierLine;
			assignments = processData(dataLine, numClasses, numItems,
					classNumber2NameMap);
		}
		if (handler != null) {
			handler.assignments(assignments);
		}
	}

	private boolean hasClassNames(String classifierLine) {
		return (classifierLine != null && classifierLine.length() > 2 && classifierLine
				.startsWith("#"));
	}

	private void processHeader() throws IOException,
			org.genepattern.io.ParseException {
		String headerLine = reader.readLine();
		if (headerLine == null) {
			throw new org.genepattern.io.ParseException("No header line");
		}
		int[] hdrInts = new int[3];
		StringTokenizer tok = new StringTokenizer(headerLine, " \t");
		if (tok.countTokens() != 3) {
			throw new org.genepattern.io.ParseException(
					"Header line needs three numbers!\n" + "\"" + headerLine
							+ "\"");
		}
		try {
			for (int i = 0; i < 3; i++) {
				hdrInts[i] = Integer.parseInt(tok.nextToken().trim());
			}
		} catch (NumberFormatException e) {
			throw new org.genepattern.io.ParseException("Header line element '"
					+ e.getMessage() + "' is not a number!");
		}

		if (hdrInts[0] <= 0) {
			throw new org.genepattern.io.ParseException(
					"Header line missing first number, number of data points");
		}
		if (hdrInts[1] <= 0) {
			throw new org.genepattern.io.ParseException(
					"Header line missing second number, number of classes");
		}

		/*
		 * if(hdrInts[2] != 1) { throw new
		 * org.genepattern.io.ParseException("Third number on 1st line must
		 * '1'."); }
		 */
		numClasses = hdrInts[1];
		numItems = hdrInts[0];

	}

	private String[] processClassifier(String classifierLine, int num_classes)
			throws IOException {
		// optional: class label line, otherwise the data line
		// # Breast Colon Pancreas ...

		// remove the # because it could be "# CLASS1" or "#CLASS1"
		classifierLine = classifierLine
				.substring(classifierLine.indexOf('#') + 1);
		StringTokenizer st = new StringTokenizer(classifierLine);
		if (st.countTokens() != num_classes) {
			throw new IOException("First line specifies " + num_classes
					+ " classes, but found " + (st.countTokens()) + ".");
		}
		String[] names = new String[num_classes];
		for (int ic = 0; st.hasMoreTokens(); ic++) {
			names[ic] = st.nextToken();
		}
		return names;
	}

	/**
	 * Parses the data line
	 * 
	 * @param data_line
	 *            The line to parse
	 * @param num_classes
	 *            The number of classes
	 * @param num_data
	 *            The number of data points
	 * @return the assignments
	 * @exception IOException
	 *                Description of the Exception
	 * @exception org.genepattern.io.ParseException
	 *                Description of the Exception
	 */
	private String[] processData(String data_line, int num_classes,
			int num_data, Map classNumber2ClassNameMap) throws IOException,
			org.genepattern.io.ParseException {
		// data line <int0> <space> <int1> <space> <int2> <space> ...
		if (data_line == null) {
			throw new org.genepattern.io.ParseException(
					"Missing data (numbers seperated by spaces) in 3rd line");
		}
		try {
			String[] assignments = new String[num_data];
			int dataInd = 0;
			do {
				StringTokenizer st = new StringTokenizer(data_line);
				while (st.hasMoreTokens()) {
					int classNumber = Integer.parseInt(st.nextToken().trim());
					if (classNumber >= num_classes || classNumber < 0) {
						throw new org.genepattern.io.ParseException(
								"Header specifies "
										+ num_classes
										+ " classes, but data line contains a "
										+ classNumber
										+ ", a value "
										+ "that is too "
										+ (classNumber < 0 ? "small" : "large")
										+ "."
										+ "All data x for this file must be in the range 0-"
										+ (num_classes - 1) + ".");
					}
					String name = (String) classNumber2ClassNameMap
							.get(new Integer(classNumber));

					assignments[dataInd++] = name;
				}
				data_line = reader.readLine();
			} while (data_line != null);

			if (dataInd != num_data) {
				throw new org.genepattern.io.ParseException("Header specifies "
						+ num_data + " datapoints. But file contains "
						+ dataInd + " datapoints.");
			}

			if (numClasses != num_classes) {
				throw new org.genepattern.io.ParseException("Header specifies "
						+ num_classes + " classes. But file contains "
						+ numClasses + " classes.");
			}
			return assignments;
		} catch (NumberFormatException ex) {
			throw new NumberFormatException(
					"All data on the data line(s) (3rd and subsequent lines) "
							+ "must be numbers!");
		}
	}

	public void setHandler(IClsHandler handler) {
		this.handler = handler;
	}

	public String getFormatName() {
		return formatName;
	}

	public List getFileSuffixes() {
		return suffixes;
	}
}

/*
 * public FileMetaData getFileMetaData(InputStream in) throws IOException {
 * return new InnerParser(in).getFileMetaData(); }
 */
