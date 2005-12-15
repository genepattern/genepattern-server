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


package org.genepattern.io.expr.odf;

import java.io.IOException;
import java.io.InputStream;

import org.genepattern.io.AbstractReader;
import org.genepattern.io.OdfParser;
import org.genepattern.io.ParseException;
import org.genepattern.io.expr.IExpressionDataCreator;
import org.genepattern.io.expr.IExpressionDataReader;
import org.genepattern.io.expr.ReaderUtil;

/**
 * Reader for odf datasets
 * 
 * @author Joshua Gould
 */

public class OdfDatasetReader extends AbstractReader implements
		IExpressionDataReader {

	public OdfDatasetReader() {
		super(new String[] { "odf" }, "odf");
	}

	public boolean canRead(InputStream in) throws IOException {
		OdfParser parser = new OdfParser();
		return parser.canDecode(in);
	}

	public Object read(String fileName, IExpressionDataCreator creator)
			throws IOException, ParseException {
		return ReaderUtil.read(new OdfParserAdapter(), fileName, creator);
	}

}