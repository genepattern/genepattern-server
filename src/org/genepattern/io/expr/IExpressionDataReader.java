package org.genepattern.io.expr;
import java.util.List;

/**
 *  Interface for expression data readers.
 *
 * @author    Joshua Gould
 */
public interface IExpressionDataReader {
	public Object read(String pathname, IExpressionDataCreator creator) throws org.genepattern.io.ParseException, java.io.IOException;


	public String getFormatName();


	public List getFileSuffixes();


	public boolean canRead(java.io.InputStream is) throws java.io.IOException;
}

