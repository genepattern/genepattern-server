package org.genepattern.io.expr.res;
import java.io.IOException;
import java.io.InputStream;

import org.genepattern.io.AbstractReader;
import org.genepattern.io.ParseException;
import org.genepattern.io.expr.IExpressionDataCreator;
import org.genepattern.io.expr.IExpressionDataReader;
import org.genepattern.io.expr.ReaderUtil;


/**
 *  Reads res files.
 *
 * @author    Joshua Gould
 */
public class ResReader extends AbstractReader implements IExpressionDataReader {

   public ResReader() {
      super(new String[]{"res"}, "res");
   }


   public boolean canRead(InputStream in) throws IOException {
      ResParser parser = new ResParser();
      return parser.canDecode(in);
   }


   public Object read(String fileName, IExpressionDataCreator creator) throws IOException, ParseException {
      return ReaderUtil.read(new ResParser(), fileName, creator);
   }
}
