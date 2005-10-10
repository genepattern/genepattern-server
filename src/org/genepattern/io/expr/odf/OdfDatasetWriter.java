package org.genepattern.io.expr.odf;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.genepattern.data.expr.IExpressionData;
import org.genepattern.io.expr.IExpressionDataWriter;
import org.genepattern.io.OdfWriter;

/**
 * Writer for odf datasets
 * 
 * @author Joshua Gould
 */
public class OdfDatasetWriter implements IExpressionDataWriter {
	final static String FORMAT_NAME = "odf";

	private boolean prependExecutionLog = false;

	public String checkFileExtension(String filename) {
		if (!filename.toLowerCase().endsWith(".odf")) {
			filename += ".odf";
		}
		return filename;
	}

	public void setPrependExecutionLog(boolean b) {
		prependExecutionLog = b;
	}

	// note old odf parser requires row descriptions and column descriptions

	public void write(IExpressionData expressionData, OutputStream os)
			throws IOException {
		PrintWriter out = new PrintWriter(os);
		if (prependExecutionLog) {
			OdfWriter.appendExecutionLog(out);
		}
		int rows = expressionData.getRowCount();
		int columns = expressionData.getColumnCount();

		out.println("ODF 1.0");
		int headerLines = 7;

		out.println("HeaderLines=" + headerLines);
		out.println("Model=Dataset");
		out.print("COLUMN_NAMES:Name\tDescription\t");
		for (int j = 0; j < columns - 1; j++) {
			out.print(expressionData.getColumnName(j));
			out.print("\t");
		}
		out.println(expressionData.getColumnName(columns - 1));

		out.print("COLUMN_TYPES:");
		out.print("String\tString");

		for (int j = 0; j < columns - 1; j++) {
			out.print("\tfloat");
		}
		out.println("\tfloat");

		out
				.print("COLUMN_DESCRIPTIONS:Name for each row\tDescription for each row\t");

		for (int j = 0; j < columns - 1; j++) {
			String columnDescription = expressionData.getColumnDescription(j);
			if (columnDescription == null) {
				columnDescription = "";
			}
			out.print(columnDescription);
			out.print("\t");
		}
		String columnDescription = expressionData
				.getColumnDescription(columns - 1);
		if (columnDescription == null) {
			columnDescription = "";
		}

		out.println(columnDescription);

		out.println("RowNamesColumn=0");

		out.println("RowDescriptionsColumn=1");

		out.print("DataLines=" + rows);

		for (int i = 0; i < rows; i++) {
			out.println();
			out.print(expressionData.getRowName(i));
			out.print("\t");
			String rowDescription = expressionData.getRowDescription(i);
			if (rowDescription == null) {
				rowDescription = "";
			}
			out.print(rowDescription);

			for (int j = 0; j < columns; j++) {
				out.print("\t");
				out.print(expressionData.getValueAsString(i, j));
			}
		}
		out.flush();
		out.close();
	}

	public String getFormatName() {
		return FORMAT_NAME;
	}

}
