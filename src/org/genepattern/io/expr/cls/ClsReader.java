package org.genepattern.io.expr.cls;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.genepattern.data.matrix.ClassVector;
import org.genepattern.io.*;

/**
 *  Class for reading cls documents.
 *
 * @author    Joshua Gould
 */
public class ClsReader extends AbstractReader {

	public ClsReader() {
		super(new String[]{"cls"}, "cls");
	}


	public ClassVector read(String pathname) throws IOException, ParseException {
		ClsParser parser = new ClsParser();
		MyHandler handler = new MyHandler();
		parser.setHandler(handler);
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(pathname);
			parser.parse(fis);
		} finally {
			if(fis != null) {
				fis.close();
			}
		}
		return new ClassVector(handler.x);
	}


	public boolean canRead(InputStream in) throws IOException {
		ClsParser parser = new ClsParser();
		return parser.canDecode(in);
	}


	/**
	 * @author    Joshua Gould
	 */
	private static class MyHandler implements IClsHandler {
		String[] x;


		public void assignments(String[] x) {
			this.x = x;
		}
	}
}
