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

package org.genepattern.data.expr;

/**
 * @author Joshua Gould
 */
public abstract class AbstractExpressionData implements IExpressionData {
    public String getValueAsString(int row, int column) {
        return String.valueOf(getValue(row, column));
    }

    public boolean containsData(String name) {
        return false;
    }

    public Object getData(int row, int column, String name) {
        return null;
    }

    public boolean containsRowMetadata(String name) {
        return false;
    }

    public boolean containsColumnMetadata(String name) {
        return false;
    }

    public String getRowMetadata(int row, String name) {
        return null;
    }

    public String getColumnMetadata(int column, String name) {
        return null;
    }

    public abstract double getValue(int row, int column);

    public abstract String getRowName(int row);

    public abstract int getRowCount();

    public abstract int getColumnCount();

    public abstract String getColumnName(int column);

    public abstract int getRowIndex(String rowName);

    public abstract int getColumnIndex(String columnName);
}
