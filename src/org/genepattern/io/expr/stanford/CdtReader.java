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