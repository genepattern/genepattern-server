package org.genepattern.io.expr.stanford;

import java.io.IOException;
import java.io.InputStream;

import org.genepattern.io.AbstractReader;
import org.genepattern.io.expr.IExpressionDataCreator;
import org.genepattern.io.expr.IExpressionDataReader;
import org.genepattern.io.expr.ReaderUtil;

/**
 * Reader for cdt documents.
 * 
 * @author Joshua Gould
 */
public class CdtReader extends AbstractReader implements IExpressionDataReader {

	public CdtReader() {
		super(new String[] { "cdt" }, "cdt");
	}

	public boolean canRead(InputStream in) throws IOException {
		CdtParser parser = new CdtParser();
		return parser.canDecode(in);
	}

	public Object read(String fileName, IExpressionDataCreator creator)
			throws IOException, org.genepattern.io.ParseException {
		return ReaderUtil.read(new CdtParser(), fileName, creator);
	}

}