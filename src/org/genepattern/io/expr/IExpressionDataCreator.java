package org.genepattern.io.expr;
/**
 *  An interface for creating new instances of expression data objects
 *
 * @author    Joshua Gould
 */
public interface IExpressionDataCreator extends IExpressionDataHandler {

	public Object create();

}
