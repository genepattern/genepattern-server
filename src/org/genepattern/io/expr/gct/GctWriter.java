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


package org.genepattern.io.expr.gct;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.genepattern.data.expr.IExpressionData;
import org.genepattern.io.expr.IExpressionDataWriter;

/**
 * Writes gct files.
 * 
 * @author Joshua Gould
 */
public class GctWriter implements IExpressionDataWriter {
	final static String FORMAT_NAME = "gct";

	public String checkFileExtension(String filename) {
		if (!filename.toLowerCase().endsWith(".gct")) {
			filename += ".gct";
		}
		return filename;
	}

	public void write(IExpressionData expressionData, OutputStream os)
			throws IOException {

		PrintWriter out = new PrintWriter(os);

		int rows = expressionData.getRowCount();
		int columns = expressionData.getColumnCount();

		String version = "#1.2";
		out.print(version);
		out.print("\n");

		out.print(rows + "\t" + columns);
		out.print("\n");

		out.print("Name");
		out.print("\t");
		out.print("Description");

		for (int j = 0; j < columns; j++) {
			out.print("\t");
			out.print(expressionData.getColumnName(j));
		}

		for (int i = 0; i < rows; i++) {
			out.print("\n");
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
		out.print("\n");
		out.flush();
	}

	public String getFormatName() {
		return FORMAT_NAME;
	}

}

