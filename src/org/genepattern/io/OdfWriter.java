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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class OdfWriter extends PrintWriter {
	private static final String TASKLOG = "gp_task_execution_log.txt";

	private String[] columnNames;

	private List headers;

	private String model;

	private int dataLines;

	private String[] columnTypes;

	public OdfWriter(String outputFileName, String[] columnNames, String model,
			int dataLines, boolean prependExecutionLog) throws IOException {
		super(new FileWriter(fixName(outputFileName)));
		if (prependExecutionLog) {
			appendExecutionLog(this);
		}
		this.columnNames = columnNames;
		this.model = model;
		this.dataLines = dataLines;
		this.headers = new ArrayList();

	}

	private static String fixName(String outputFileName) {
		if (!outputFileName.toLowerCase().endsWith(".odf")) {
			outputFileName += ".odf";
		}
		return outputFileName;
	}

	static class Header {
		public Header(String key2, String value2) {
			this.key = key2;
			this.value = value2;
		}

		String key;

		String value;
	}

	static class HeaderArray {
		public HeaderArray(String key2, String[] value2) {
			this.key = key2;
			this.value = value2;
		}

		String key;

		String[] value;
	}

	public void addHeader(String key, String value) {
		headers.add(new Header(key, value));
	}

	public void addHeader(String key, int value) {
		headers.add(new Header(key, String.valueOf(value)));
	}

	public void addHeader(String key, double value) {
		headers.add(new Header(key, String.valueOf(value)));
	}

	public void addHeader(String key, String[] values) {
		headers.add(new HeaderArray(key, values));
	}

	/**
	 * Prints a line, escaping the line if it starts with a # (comment
	 * character)
	 * 
	 * @param line
	 */
	public void eprintln(String line) {
		if (line.charAt(0) == '#') {
			line = "\\" + line;
		}
		this.println(line);
	}

	private void printArray(String key, String[] values) {
		this.print(key + ":");
		for (int j = 0; j < values.length; j++) {
			if (j > 0) {
				this.print("\t");
			}
			this.print(values[j]);
		}
		this.println();
	}

	public void printHeader() {
		this.println("ODF 1.0");
		int headerLines = headers.size() + 2; // DataLines, Model
		if (columnNames != null && columnNames.length > 0) {
			headerLines++;
		}
		if (columnTypes != null && columnTypes.length > 0) {
			headerLines++;
		}
		this.println("HeaderLines=" + headerLines);
		if (columnNames != null && columnNames.length > 0) {
			printArray("COLUMN_NAMES", columnNames);
		}
		if (columnTypes != null && columnTypes.length > 0) {
			printArray("COLUMN_TYPES", columnTypes);
		}
		this.println("Model=" + model);
		for (int i = 0, size = headers.size(); i < size; i++) {
			Object obj = headers.get(i);
			if (obj instanceof Header) {
				Header h = (Header) obj;
				this.print(h.key);
				this.print("=");
				this.println(h.value);
			} else {
				HeaderArray h = (HeaderArray) obj;
				printArray(h.key, h.value);
			}
		}
		this.println("DataLines=" + dataLines);

	}

	public static void appendExecutionLog(PrintWriter pw) {
		if (new File(TASKLOG).exists()) {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(TASKLOG));
				String s = null;
				while ((s = br.readLine()) != null) {
					pw.println(s);
				}
			} catch (IOException ioe) {
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException x) {
					}
				}
			}
		}
	}

	public void setColumnTypes(String[] columnTypes2) {
		this.columnTypes = columnTypes2;
	}
}
