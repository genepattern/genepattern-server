package org.genepattern.data.expr;

/**
 * An interface for expression data that includes Affymetrix absent, present,
 * and marginal calls.
 * 
 * @author Joshua Gould
 */
public interface IResExpressionData extends IExpressionData {
	public static int PRESENT = 1;

	public static int ABSENT = 0;

	public static int MARGINAL = 2;

	/**
	 * Gets the Affymetrix call at the given row and column. A value of
	 * IResExpressionData.PRESENT indicates a present call,
	 * IResExpressionData.ABSENT an absent call, and IResExpressionData.MARGINAL
	 * a marginal call.
	 * 
	 * @param row
	 *            The row index
	 * @param column
	 *            The column index
	 * @return The call
	 */
	public int getCall(int row, int column);

	/**
	 * Gets the Affymetrix call at the given row and column. A value of 'P'
	 * indicates a present call, 'A' an absent call, and 'M' a marginal call.
	 * 
	 * @param row
	 *            The row index
	 * @param column
	 *            The column index
	 * @return The call
	 */
	public String getCallAsString(int row, int column);

}

