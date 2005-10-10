package org.genepattern.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class OdfWriter extends PrintWriter {
	private static final String TASKLOG = "gp_task_execution_log.txt";

	private String[] columnNames;

	private List headers;

	private String model;

	private int dataLines;

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

	public void addHeader(String key, String value) {
		headers.add(new Header(key, value));
	}

	public void addHeader(String key, int value) {
		headers.add(new Header(key, String.valueOf(value)));
	}

	public void addHeader(String key, double value) {
		headers.add(new Header(key, String.valueOf(value)));
	}

	public void printHeader() {
		this.println("ODF 1.0");
		this.println("HeaderLines=" + (headers.size() + 2));
		this.print("COLUMN_NAMES:");
		for (int j = 0; j < columnNames.length; j++) {
			if (j > 0) {
				this.print("\t");
			}
			this.print(columnNames[j]);
		}
		this.println();
		this.println("Model=" + model);
		for (int i = 0, size = headers.size(); i < size; i++) {
			Header h = (Header) headers.get(i);
			this.print(h.key);
			this.print("=");
			this.println(h.value);
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
}
