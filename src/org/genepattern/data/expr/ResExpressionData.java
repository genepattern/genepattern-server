package org.genepattern.data.expr;

import org.genepattern.data.matrix.DoubleMatrix2D;
import org.genepattern.data.matrix.IntMatrix2D;

/**
 * Implementation of IResExpressionData interface
 * 
 * @author Joshua Gould
 */
public class ResExpressionData extends ExpressionData implements
		IResExpressionData {
	IntMatrix2D calls;

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
	public ResExpressionData(DoubleMatrix2D expressionData, IntMatrix2D calls,
			String[] geneDescriptions, String[] sampleDescriptions) {
		super(expressionData, geneDescriptions, sampleDescriptions);
		this.calls = calls;
		if (calls.getRowCount() != expressionData.getRowCount()) {
			throw new IllegalArgumentException(
					"Number of rows in calls not equal to number of rows in expression dataset.");
		}
	}

	/**
	 * Constructs and returns a new res expression data instance that contains
	 * the indicated cells. Ensures that both the expression data and the calls
	 * are sliced. Indices can be in arbitrary order.
	 * 
	 * @param rowIndices
	 *            The rows of the cells in the new res expressionData. To
	 *            indicate that the new expressionData should contain all rows,
	 *            set this parameter to null.
	 * @param columnIndices
	 *            The columns of the cells in the new res expressionData. To
	 *            indicate that the new expressionData should contain all
	 *            columns, set this parameter to <code>null</code>.
	 * @return the sliced data
	 */
	public ExpressionData slice(int[] rowIndices, int[] columnIndices) {
		ExpressionData e = (ExpressionData) super.slice(rowIndices,
				columnIndices);
		IntMatrix2D newCalls = calls.slice(rowIndices, columnIndices);
		return new ResExpressionData(e.dataset, newCalls, e.rowDescriptions,
				e.columnDescriptions);
	}

	public int getCall(int row, int column) {
		return calls.get(row, column);
	}

	public String getCallAsString(int row, int column) {
		switch (calls.get(row, column)) {
		case IResExpressionData.ABSENT:
			return "A";
		case IResExpressionData.PRESENT:
			return "P";
		case IResExpressionData.MARGINAL:
			return "M";
		default:
			return "";

		}
	}
}

