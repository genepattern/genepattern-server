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


package org.genepattern.io.expr.stanford;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.genepattern.data.expr.ExpressionData;
import org.genepattern.data.matrix.DoubleMatrix2D;
import org.genepattern.io.ParseException;
import org.genepattern.io.expr.IExpressionDataHandler;
import org.genepattern.io.expr.IExpressionDataParser;

public class CdtParser implements IExpressionDataParser {
	protected int rows;

	protected int columns;

	protected String[] columnNames;

	protected boolean name_col = false;

	protected boolean eweight = false;

	protected boolean eorder = false;

	protected boolean gweight = false;

	protected boolean gorder = false;

	protected String urlIdentifier;

	protected boolean aid = false;

	protected boolean gid_col = false;

	protected List rowNames = new ArrayList();

	protected List rowDescriptions = new ArrayList();

	protected List values = new ArrayList();

	protected List geneIds = new ArrayList();

	protected IExpressionDataHandler handler;

	private String[] arrayIds;

	public List getGeneIds() {
		return geneIds;
	}

	public String[] getArrayIds() {
		return arrayIds;
	}

	boolean hasColumnName() {
		return name_col;
	}

	boolean hasGid() {
		return gid_col;
	}

	boolean hasAid() {
		return aid;
	}

	boolean hasEweight() {
		return eweight;
	}

	boolean hasEorder() {
		return eorder;
	}

	boolean hasGweight() {
		return gweight;
	}

	boolean hasGorder() {
		return gorder;
	}

	public void setHandler(IExpressionDataHandler handler) {
		this.handler = handler;
	}

	public boolean canDecode(InputStream is) throws IOException {
		return false; // FIXME
	}

	public void parse(InputStream is) throws IOException, ParseException {
		LineNumberReader reader = new LineNumberReader(new BufferedReader(
				new InputStreamReader(is)));
		_parse(reader);

		if (handler != null) {
			handler.init(rows, columns, true, arrayIds != null, false);

			for (int i = 0; i < rows; i++) {
				double[] d = (double[]) values.get(i);
				handler.rowName(i, (String) rowNames.get(i));
				handler.rowDescription(i, (String) rowDescriptions.get(i));

				for (int j = 0; j < columns; j++) {
					handler.data(i, j, d[j]);
				}

			}
			for (int j = 0; j < columns; j++) {
				handler.columnName(j, columnNames[j]);
				if (arrayIds != null) {
					handler.columnDescription(j, arrayIds[j]);
				}
			}
		}
	}

	public ExpressionData getExpressionData() {
		double[][] data = new double[rows][];
		for (int i = 0; i < rows; i++) {
			double[] d = (double[]) values.get(i);
			data[i] = d;
		}

		DoubleMatrix2D matrix = new DoubleMatrix2D(data, (String[]) rowNames
				.toArray(new String[0]), columnNames);

		String[] rowDescriptionsArray = null;
		if (rowDescriptions != null
				&& rowDescriptions.size() == rowNames.size()) {
			rowDescriptionsArray = (String[]) rowDescriptions
					.toArray(new String[0]);
		}
		return new ExpressionData(matrix, rowDescriptionsArray, null);

	}

	protected void _parse(LineNumberReader infile) throws IOException,
			org.genepattern.io.ParseException {

		// first line is headers
		String line = infile.readLine();
		// in order to capture nulls in header, must return delimiters also
		StringTokenizer tokens = new StringTokenizer(line, "\t", true);

		// first token is ORF or something like that
		// want to save what this token is called for use with URL
		String token = tokens.nextToken();
		if (token.equalsIgnoreCase("GID")) {
			gid_col = true;
			// get rid of the delimiter
			token = tokens.nextToken();
			// get next one- identifier
			urlIdentifier = tokens.nextToken();
		} else {
			urlIdentifier = token;
		}
		// get rid of the delimiter
		token = tokens.nextToken();

		int count = 0;
		while (tokens.hasMoreTokens()) {
			token = tokens.nextToken();

			// check if cdt has name column
			if (token.equalsIgnoreCase("name")) {
				name_col = true;
				// get rid of the delimiter
				token = tokens.nextToken();
			}
			// check if cdt has gweight column
			else if (token.equalsIgnoreCase("gweight")) {
				gweight = true;
				// get rid of the delimiter
				token = tokens.nextToken();
			}
			// check if cdt has GORDER column
			else if (token.equalsIgnoreCase("gorder")) {
				gorder = true;
				// get rid of the delimiter
				token = tokens.nextToken();
			} else {
				count++;
			}
			// skip delimiter unless double tabs
			if (!token.equals("\t")) {
				if (tokens.hasMoreTokens()) {
					token = tokens.nextToken();
				}
			}

		}
		// set columns to count
		columns = count;

		// re-tokenize line
		tokens = new StringTokenizer(line, "\t", true);
		// skip gid if has
		if (this.hasGid()) {
			// skip name token
			token = tokens.nextToken();
			// get rid of the delimiter
			token = tokens.nextToken();
		}
		// skip first token - uid
		token = tokens.nextToken();
		// get rid of the delimiter
		token = tokens.nextToken();
		if (this.hasColumnName()) {
			// skip name token
			token = tokens.nextToken();
			// get rid of the delimiter
			token = tokens.nextToken();
		}
		if (this.hasGweight()) {
			token = tokens.nextToken();
			// get rid of the delimiter
			token = tokens.nextToken();
		}
		if (this.hasGorder()) {
			token = tokens.nextToken();
			// get rid of the delimiter
			token = tokens.nextToken();
		}
		// loop through header line again to collect columnNames
		columnNames = new String[columns];
		count = 0;
		while (tokens.hasMoreTokens()) {
			token = tokens.nextToken();
			if (!token.equals("\t")) {
				columnNames[count] = token;
				// get rid of delimiter
				if (tokens.hasMoreTokens()) {
					token = tokens.nextToken();
				}

			}
			count++;
		}
		// check to make sure that (count+1) = columns
		if (count != columns) {
			throw new org.genepattern.io.ParseException(
					"Unexpected number of columns on line "
							+ infile.getLineNumber());
		}

		// next just loop through lines and see if eweight, EORDER lines are
		// there

		// then count number of gene lines = maxGenes
		while ((line = infile.readLine()) != null) {
			line = line.trim();

			if (line.startsWith("AID")) {
				aid = true;
				setArrayIds(line);
			} else if (line.startsWith("EWEIGHT")) {
				eweight = true;
			} else if (line.startsWith("EORDER")) {
				eorder = true;
			} else { // this is start of genes
				parseLine(line);
				rows++;
				while ((line = infile.readLine()) != null) {
					parseLine(line);
					rows++;
				}
			}
		}

	}

	private void setArrayIds(String line) {
		// tokenize line
		StringTokenizer tokens = new StringTokenizer(line, "\t", true);
		// skip first token- is "AID"
		String token = tokens.nextToken();
		// System.out.println("aid: " + token);
		// get rid of the delimiter
		token = tokens.nextToken();
		// System.out.println("first delim: " + token);
		// skip url identifier column delimiter
		if (hasGid()) {
			// skip url identifier column delimiter
			token = tokens.nextToken();
		}
		if (name_col) {
			// System.out.println("name: " + token);
			// get rid of the delimiter
			token = tokens.nextToken();
		}
		if (gweight) {
			// System.out.println("gweight: " + token);
			// get rid of the delimiter
			token = tokens.nextToken();
		}
		if (gorder) {
			// System.out.println("gorder: " + token);
			// get rid of the delimiter
			token = tokens.nextToken();
		}

		// now start getting ids
		arrayIds = new String[columns];
		int i = 0;
		while (tokens.hasMoreTokens()) {
			token = tokens.nextToken();
			// System.out.println("token: " + token);
			// System.out.println("array id " + i + ": " + token);
			// make sure its the right form before getting substring
			if ((token.startsWith("ARRY")) && (token.endsWith("X"))) {
				try {
					// get number from token
					// arrayIdNumbers[i] = Integer.parseInt(token.substring(4,
					// token.length() -1));
					arrayIds[i] = token;
					// i++;
					if (tokens.hasMoreTokens()) {
						// get rid of the delimiter
						token = tokens.nextToken();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else { // assume its a blank column
				// this means 'token" was a delimiter, so the next one will
				// either be an arrayid or another delimiter
			}
			i++;
		}

	}

	protected void parseLine(String line) {
		// tokenize each line
		// must keep delimiter to catch null avlues
		StringTokenizer tokens = new StringTokenizer(line, "\t", true);
		String token = null;

		if (tokens.hasMoreTokens()) { // this will accomidate for files where
			// delimiter is couted twice on currenet
			// os
			// if GID col then skip first token
			if (this.hasGid()) {
				// may be null
				token = tokens.nextToken();
				geneIds.add(token);
				if (!token.equals("\t")) {
					// get rid of delimiter
					if (tokens.hasMoreTokens()) {
						token = tokens.nextToken();
					}
				}
			}
			// first token is gid
			rowNames.add(tokens.nextToken());

			// get rid of delimiter
			token = tokens.nextToken();

			// next token is Name
			if (this.hasColumnName()) {
				// may be null
				token = tokens.nextToken();
				if (token.equals("\t")) {
					// set to empty string
					rowDescriptions.add("");
				} else {
					rowDescriptions.add(token);
					// get rid of delimiter
					if (tokens.hasMoreTokens()) {
						token = tokens.nextToken();
					}
				}
			}
			if (this.hasGweight()) {
				// may be null
				token = tokens.nextToken();
				if (!token.equals("\t")) {
					// double gweight = Double.parseDouble(token));
					// get rid of delimiter
					if (tokens.hasMoreTokens()) {
						token = tokens.nextToken();
					}
				}
			}
			if (this.hasGorder()) {
				// may be null
				token = tokens.nextToken();
				if (!token.equals("\t")) {
					// get rid of delimiter
					if (tokens.hasMoreTokens()) {
						token = tokens.nextToken();
					}
				}
			}
		}
		// now get array of values
		double[] values = new double[columns];
		for (int i = 0; i < columns; i++) {
			if (tokens.hasMoreTokens()) {
				String f = tokens.nextToken();
				if (!f.equals("\t")) {
					try {
						values[i] = Double.parseDouble(f);
					} catch (NumberFormatException exc) {
						values[i] = Double.NaN;
					}
					// get rid of delimiter
					if (tokens.hasMoreTokens()) {
						token = tokens.nextToken();
					}

				}
				// else if null, will not add
			}
		}
		this.values.add(values);
	}

}
