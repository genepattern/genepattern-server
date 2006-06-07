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

import org.genepattern.data.matrix.DoubleMatrix2D;
import org.genepattern.data.matrix.IntMatrix2D;
import org.genepattern.data.matrix.ObjectMatrix2D;

/**
 * Implementation of IResExpressionData interface
 * 
 * @author Joshua Gould
 */
public class ResExpressionData extends ExpressionData implements
        IResExpressionData {
    protected final static String CALLS = "calls";

    /**
     * Constructs a new res expression data instance
     * 
     * @param expressionData
     *            the expression data
     * @param calls
     *            the calls
     * @param geneDescriptions
     *            the row(gene) descriptions
     * @param sampleDescriptions
     *            the sample(column) descriptions
     */
    public ResExpressionData(DoubleMatrix2D expressionData,
            ObjectMatrix2D calls, String[] geneDescriptions,
            String[] sampleDescriptions) {
        super(expressionData, geneDescriptions, sampleDescriptions);
        if (calls.getRowCount() != expressionData.getRowCount()) {
            throw new IllegalArgumentException(
                    "Number of rows in calls not equal to number of rows in expression dataset.");
        }
        setMatrix(CALLS, calls);
    }

    public String getCall(int row, int column) {
        return (String) getMatrix(CALLS).get(row, column);
    }

    public ObjectMatrix2D getCallMatrix() {
        return getMatrix(CALLS);
    }

}
