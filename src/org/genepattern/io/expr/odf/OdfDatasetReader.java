package org.genepattern.io.expr.odf;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.genepattern.data.expr.ExpressionData;
import org.genepattern.data.matrix.DoubleMatrix2D;
import org.genepattern.io.*;
import org.genepattern.io.expr.*;



/**
 *  Reader for odf datasets
 *
 * @author    Joshua Gould
 */

public class OdfDatasetReader extends AbstractReader implements IExpressionDataReader {

	public OdfDatasetReader() {
		super(new String[]{"odf"}, "odf");
	}


	public boolean canRead(InputStream in) throws IOException {
		OdfParser parser = new OdfParser();
		return parser.canDecode(in);
	}


	public Object read(String fileName, IExpressionDataCreator creator) throws IOException, ParseException {
		return ReaderUtil.read(new OdfParserAdapter(), fileName, creator);
	}

}
