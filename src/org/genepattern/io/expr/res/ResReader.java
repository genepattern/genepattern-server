package org.genepattern.io.expr.res;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.genepattern.data.expr.ExpressionData;
import org.genepattern.data.matrix.DoubleMatrix2D;
import org.genepattern.data.matrix.IntMatrix2D;
import org.genepattern.io.*;
import org.genepattern.io.expr.*;


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
