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


package org.genepattern.io.expr.cls;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.genepattern.data.matrix.ClassVector;
import org.genepattern.io.AbstractReader;
import org.genepattern.io.ParseException;

/**
 * Class for reading cls documents.
 * 
 * @author Joshua Gould
 */
public class ClsReader extends AbstractReader {

	public ClsReader() {
		super(new String[] { "cls" }, "cls");
	}

	public ClassVector read(String pathname) throws IOException, ParseException {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(pathname);
			return read(fis);
		} finally {
			if (fis != null) {
				fis.close();
			}
		}
	}

	public ClassVector read(InputStream is) throws IOException, ParseException {
		ClsParser parser = new ClsParser();
		MyHandler handler = new MyHandler();
		parser.setHandler(handler);
		parser.parse(is);
		return new ClassVector(handler.x, handler.classes);
	}

	public boolean canRead(InputStream in) {
		ClsParser parser = new ClsParser();
		return parser.canDecode(in);
	}

	/**
	 * @author Joshua Gould
	 */
	private static class MyHandler implements IClsHandler {
		String[] x;

		String[] classes;

		public void assignments(String[] x) {
			this.x = x;
		}

		public void classes(String[] c) {
			this.classes = c;
		}
	}
}