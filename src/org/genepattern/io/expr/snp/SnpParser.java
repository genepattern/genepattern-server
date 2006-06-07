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

import org.genepattern.io.ParseException;
import org.genepattern.io.expr.IExpressionDataHandler;
import org.genepattern.io.expr.IExpressionDataParser;

/**
 * Class for reading a snp document using callbacks.
 * 
 * @author Joshua Gould
 */
public class SnpParser implements IExpressionDataParser {
    IExpressionDataHandler handler;

    LineNumberReader reader;

    private String[] tokens;

    private int columns;

    public boolean canDecode(InputStream is) throws IOException {
        reader = new LineNumberReader(new BufferedReader(new InputStreamReader(
                is)));
        try {
            return readHeader(true);
        } catch (ParseException ioe) {
            return false;
        }
    }

    /**
     * Parse a snp document. The application can use this method to instruct the
     * snp reader to begin parsing a snp document from any valid input source.
     * Applications may not invoke this method while a parse is in progress
     * (they should create a new SnpParser instead ). Once a parse is complete,
     * an application may reuse the same SnpParser object, possibly with a
     * different input source. During the parse, the SnpParser will provide
     * information about the snp document through the registered event handler.
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
        readHeader(false);
        readData();
    }

    private void readData() throws ParseException, IOException {
        int row = 0;
        List lines = new ArrayList();
        for (String s = reader.readLine(); s != null; s = reader.readLine(), row++) {
            if (s.trim().equals("")) {// ignore blank lines
                continue;
            }
            lines.add(s);
        }
        String[] rowMetaData = new String[] { "Chromosome", "PhysicalPosition" };
        String[] matrices = new String[] { "Calls" };

        handler.init(row, columns / 2, rowMetaData, new String[] {}, matrices);
        for (int i = 3, columnIndex = 0; i < tokens.length; i += 2, columnIndex++) {
            handler.columnName(columnIndex, tokens[i]);
        }

        for (int i = 0; i < row; i++) {

            String[] dataTokens = ((String) lines.get(i)).split("\t");

            if (handler != null) {
                handler.rowMetaData(row, 0, dataTokens[0]);
            }
            String rowName = dataTokens[1];

            if (handler != null) {
                handler.rowName(row, rowName);
            }

            for (int columnIndex = 0, tokenIndex = 2; columnIndex < columns; columnIndex++, tokenIndex += 2) {
                try {
                    double data = Double.parseDouble(dataTokens[tokenIndex]);
                    if (handler != null) {
                        handler.data(row, columnIndex, data);
                    }
                } catch (NumberFormatException nfe) {
                    throw new ParseException("Data at line number "
                            + reader.getLineNumber() + " and column "
                            + columnIndex + " is not a number.");
                }

            }

        }

    }

    private boolean readHeader(boolean testOnly) throws ParseException,
            IOException {

        // SNP Chromosome PhysicalPosition AffyControl AffyControl Call Glioma
        // 26 Glioma 26 Call HCC827 HCC827 Call
        // AFFX-5Q-123 0.000 NoCall 0.000 NoCall 0.000 NoCall

        tokens = reader.readLine().split("\t");

        if (tokens == null || tokens.length == 0 || tokens.length == 1) {
            throw new ParseException("Unable to parse line 1");
        }
        if (testOnly) {
            return ("SNP".equalsIgnoreCase(tokens[0])
                    && "Chromosome".equalsIgnoreCase(tokens[1]) && "PhysicalPosition"
                    .equalsIgnoreCase(tokens[2]));

        }
        columns = tokens.length - 3;
        if ((columns % 2) != 0) {
            throw new ParseException("Unable to parse line 1");
        }

        if (columns <= 0) {
            throw new ParseException(
                    "Number of samples must be greater than zero");
        }
        return true;
    }

    public void setHandler(IExpressionDataHandler handler) {
        this.handler = handler;
    }

    public String getFormatName() {
        return "snp";
    }

    public List getFileSuffixes() {
        return Collections.unmodifiableList(Arrays
                .asList(new String[] { "snp" }));
    }
}
