package org.genepattern.io.expr.gct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.genepattern.data.expr.ExpressionData;
import org.genepattern.data.matrix.DoubleMatrix2D;
import org.genepattern.io.*;
import org.genepattern.io.expr.*;


/**
 *  Writer for gct documents.
 *
 * @author    Joshua Gould
 */
public class GctReader extends AbstractReader implements IExpressionDataReader {

	public GctReader() {
		super(new String[]{"gct"}, "gct");
	}


	public boolean canRead(InputStream in) throws IOException {
		GctParser parser = new GctParser();
		return parser.canDecode(in);
	}


	public Object read(String fileName, IExpressionDataCreator creator) throws IOException, org.genepattern.io.ParseException {
		return ReaderUtil.read(new GctParser(), fileName, creator);
	}

}
