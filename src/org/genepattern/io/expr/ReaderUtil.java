package org.genepattern.io.expr;
import java.io.FileInputStream;
import java.io.IOException;

import org.genepattern.io.ParseException;
/**
 *  Utility class for expression data readers.
 *
 * @author    Joshua Gould
 */
public class ReaderUtil {

	private ReaderUtil() { }


	public static Object read(IExpressionDataParser parser, String pathname, IExpressionDataCreator handler) throws IOException, ParseException {
		FileInputStream is = null;
		try {
			parser.setHandler(handler);
			is = new FileInputStream(pathname);
			parser.parse(is);
			return handler.create();
		} finally {
			if(is != null) {
				is.close();
			}
		}
	}

}
