package org.genepattern.io.expr.gct;

import java.io.IOException;
import java.io.InputStream;

import org.genepattern.io.AbstractReader;
import org.genepattern.io.expr.IExpressionDataCreator;
import org.genepattern.io.expr.IExpressionDataReader;
import org.genepattern.io.expr.ReaderUtil;

/**
 * Writer for gct documents.
 * 
 * @author Joshua Gould
 */
public class GctReader extends AbstractReader implements IExpressionDataReader {

	public GctReader() {
		super(new String[] { "gct" }, "gct");
	}

	public boolean canRead(InputStream in) throws IOException {
		GctParser parser = new GctParser();
		return parser.canDecode(in);
	}

	public Object read(String fileName, IExpressionDataCreator creator)
			throws IOException, org.genepattern.io.ParseException {
		return ReaderUtil.read(new GctParser(), fileName, creator);
	}

}