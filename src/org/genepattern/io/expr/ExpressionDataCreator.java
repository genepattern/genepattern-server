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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.genepattern.data.expr.ExpressionData;
import org.genepattern.data.expr.MetaData;
import org.genepattern.data.matrix.DoubleMatrix2D;
import org.genepattern.data.matrix.ObjectMatrix2D;
import org.genepattern.io.ParseException;

/**
 * Expression data creator that creates an instance of
 * org.genepattern.data.expr.ExpressionData
 * 
 * @author Joshua Gould
 */
public class ExpressionDataCreator implements IExpressionDataCreator {
    protected List matrices;

    protected double[][] data;

    protected String[] rowNames;

    protected String[] columnNames;

    MetaData rowMetaData;

    MetaData columnMetaData;

    protected boolean keepRowMetaData = true;

    protected boolean keepColumnMetaData = true;

    protected String[] matrixNames;

    private String[] rowMetaDataNames;

    private String[] columnMetaDataNames;

    public ExpressionDataCreator() {
        this(true, true);
    }

    /**
     * Creates a new <tt>ExpressionDataCreator</tt> instance
     * 
     * @param keepRowMetaData
     *            whether the ExpressionData returned from <tt>
     *      create</tt>
     *            should include row meta data
     * @param keepColumnMetaData
     *            whether the ExpressionData returned from <tt>
     *      create</tt>
     *            should include column meta data
     */
    public ExpressionDataCreator(boolean keepRowMetaData,
            boolean keepColumnDescriptions) {
        this.keepRowMetaData = keepRowMetaData;
        this.keepColumnMetaData = keepColumnMetaData;
    }

    public Object create() {
        DoubleMatrix2D matrix = new DoubleMatrix2D(data, rowNames, columnNames);
        HashMap matrices = new HashMap();
        for (int i = 0; i < matrices.size(); i++) {
            matrices.put(matrixNames[i], (ObjectMatrix2D) matrices.get(i));
        }
        return new ExpressionData(matrix, rowMetaData, columnMetaData, matrices);
    }

    public void data(int row, int column, double d) throws ParseException {
        data[row][column] = d;
    }

    public void rowMetaData(int row, int depth, String s) throws ParseException {
        rowMetaData.setMetaData(row, rowMetaDataNames[depth], s);
    }

    public void columnMetaData(int column, int depth, String s)
            throws ParseException {
        columnMetaData.setMetaData(column, columnMetaDataNames[depth], s);
    }

    public void data(int row, int column, int depth, String s)
            throws ParseException {
        ((ObjectMatrix2D) matrices.get(depth)).set(row, column, s);
    }

    public void columnName(int j, String s) throws ParseException {
        columnNames[j] = s;
    }

    public void rowName(int i, String s) throws ParseException {
        rowNames[i] = s;
    }

    public void init(int rows, int columns, String[] rowMetaDataNames,
            String[] columnMetaDataNames, String[] matrixNames)
            throws ParseException {
        this.data = new double[rows][columns];
        this.rowMetaDataNames = rowMetaDataNames;
        this.columnMetaDataNames = columnMetaDataNames;
        this.rowNames = new String[rows];
        this.columnNames = new String[columns];
        this.matrixNames = matrixNames;
        this.rowMetaData = new MetaData(rows);
        this.columnMetaData = new MetaData(columns);
        this.matrices = new ArrayList();
        for (int i = 0; i < matrixNames.length; i++) {
            matrices.add(new ObjectMatrix2D(rows, columns));
        }
    }

}
