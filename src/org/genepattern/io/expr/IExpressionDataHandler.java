package org.genepattern.io.expr;
import org.genepattern.io.ParseException;

/**
 *  An interface for receiving notification of the content of a expression data
 *  document
 *
 * @author    Joshua Gould
 */
public interface IExpressionDataHandler {

	public void init(int rows, int cols, boolean hasRowDescriptions, boolean hasColumnDescriptions, boolean hasCalls) throws ParseException;


	public void data(int i, int j, double d) throws ParseException;


	public void call(int i, int j, int call) throws ParseException;


	public void columnName(int j, String name) throws ParseException;


	public void rowName(int i, String name) throws ParseException;


	public void rowDescription(int i, String desc) throws ParseException;


	public void columnDescription(int j, String desc) throws ParseException;
}
