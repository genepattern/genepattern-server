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

package org.genepattern.io.expr;

import org.genepattern.io.ParseException;

/**
 * An interface for receiving notification of the content of a matrix document
 * 
 * @author Joshua Gould
 */
public interface IExpressionDataHandler {

    /**
     * 
     * @param rows
     *            the number of rows
     * @param columns
     *            the number of columns
     * @param rowMetaDataNames
     * @param columnMetaDataNames
     * @param matrices
     * @throws ParseException
     */
    public void init(int rows, int columns, String[] rowMetaDataNames,
            String[] columnMetaDataNames, String[] matrices)
            throws ParseException;

    public void data(int row, int column, int depth, String s)
            throws ParseException;

    public void data(int row, int column, double d) throws ParseException;

    public void columnName(int column, String name) throws ParseException;

    public void rowName(int row, String name) throws ParseException;

    public void rowMetaData(int row, int depth, String s) throws ParseException;

    public void columnMetaData(int column, int depth, String s)
            throws ParseException;

}