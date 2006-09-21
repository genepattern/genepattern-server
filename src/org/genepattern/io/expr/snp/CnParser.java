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

package org.genepattern.io.expr.snp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.genepattern.data.expr.ExpressionConstants;
import org.genepattern.io.ParseException;
import org.genepattern.io.expr.IExpressionDataHandler;
import org.genepattern.io.expr.IExpressionDataParser;

/**
 * Class for reading a cn/loh document using callbacks.
 * 
 * @author Joshua Gould
 */
public class CnParser implements IExpressionDataParser {
    IExpressionDataHandler handler;

    LineNumberReader reader;

    private String[] tokens;

    private int columns;

    private boolean cn = true;;

    public CnParser(boolean cn) {
        this.cn = cn;
    }

    public boolean canDecode(InputStream is) throws IOException {
        reader = new LineNumberReader(new BufferedReader(new InputStreamReader(is)));
        try {
            return readHeader(true);
        }
        catch (ParseException ioe) {
            return false;
        }
    }

    public void parse(InputStream is) throws ParseException, IOException {
        reader = new LineNumberReader(new BufferedReader(new InputStreamReader(is)));
        readHeader(false);
        readData();
    }

    private void readData() throws ParseException, IOException {
        List lines = new ArrayList();
        for (String s = reader.readLine(); s != null; s = reader.readLine()) {
            lines.add(s);
        }
        String[] rowMetaData = new String[] { ExpressionConstants.CHROMOSOME, ExpressionConstants.PHYSICAL_POSITION };
        String[] matrices = cn ? new String[0] : new String[] { "Data" };

        // <snp_id>\t<Chromosome>\t<Position>\t<sample1>\t<sample2>\t<sample3>\t....
        handler.init(lines.size(), columns, rowMetaData, new String[0], matrices);
        for (int i = 3, columnIndex = 0; i < tokens.length; i++, columnIndex++) {
            handler.columnName(columnIndex, tokens[i]);
        }

        for (int i = 0; i < lines.size(); i++) {
            String line = (String) lines.get(i);
            if (line.equals("")) {
                continue;
            }
            String[] tokens = line.split("\t");
            int numTokens = tokens.length;
            int expectedTokens = columns + 3;
            if (numTokens != expectedTokens) {
                throw new ParseException("Incomplete data on line " + i + ". Expected " + expectedTokens + ", got "
                        + numTokens + " tokens.");
            }
            String rowName = tokens[0];

            if (handler != null) {
                handler.rowName(i, rowName);
                handler.rowMetaData(i, 0, tokens[1]);
                handler.rowMetaData(i, 1, tokens[2]);
            }

            for (int columnIndex = 0, tokenIndex = 3; columnIndex < columns; columnIndex++, tokenIndex++) {
                try {
                    if (cn) {
                        double data = Double.parseDouble(tokens[tokenIndex]);
                        if (handler != null) {
                            handler.data(i, columnIndex, data);
                        }
                    }
                    else {
                        if (handler != null) {
                            handler.data(i, columnIndex, 0, tokens[tokenIndex]);
                        }
                    }
                }
                catch (NumberFormatException nfe) {
                    throw new ParseException("Data at line number " + i + " and column " + columnIndex
                            + " is not a number.");
                }
            }
        }
    }

    private boolean readHeader(boolean testOnly) throws ParseException, IOException {

        tokens = reader.readLine().split("\t");

        if (tokens == null || tokens.length == 0 || tokens.length == 1) {
            throw new ParseException("Unable to parse line 1");
        }
        if (testOnly) {
            return ("SNP".equalsIgnoreCase(tokens[0]) && "Chromosome".equalsIgnoreCase(tokens[1]) && "PhysicalPosition"
                    .equalsIgnoreCase(tokens[2]));

        }
        columns = tokens.length - 3;

        if (columns <= 0) {
            throw new ParseException("Number of samples must be greater than zero");
        }
        return true;
    }

    public void setHandler(IExpressionDataHandler handler) {
        this.handler = handler;
    }

    public String getFormatName() {
        return cn ? "cn" : "loh";
    }

    public List getFileSuffixes() {
        return Collections.unmodifiableList(Arrays.asList(new String[] { cn ? "cn" : "loh" }));
    }
}
