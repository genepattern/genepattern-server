package org.genepattern.io.expr.res;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.genepattern.data.expr.IExpressionData;
import org.genepattern.data.expr.IResExpressionData;
import org.genepattern.data.matrix.DoubleMatrix2D;
import org.genepattern.io.expr.*;


/**
 *  Writes res files.
 *
 * @author    Joshua Gould
 */
public class ResWriter implements IExpressionDataWriter {
   final static String FORMAT_NAME = "res";


   public String checkFileExtension(String filename) {
      if(!filename.toLowerCase().endsWith(".res")) {
         filename += ".res";
      }
      return filename;
   }


   public void write(IExpressionData data, OutputStream os) throws IOException {
      if(!(data instanceof IResExpressionData)) {
         throw new IOException("Can't write in res format. Data does not have calls.");
      }

      IResExpressionData expressionData = (IResExpressionData) data;
      PrintWriter out = new PrintWriter(os);
      int rows = data.getRowCount();
      int columns = data.getColumnCount();

      out.print("Description");// Line format: Description (tab) Accession (tab) (sample 1 name) (tab) (tab) (sample 2 name) (tab) (tab) ... (sample N name)
      out.print("\t");
      out.print("Accession");
      out.print("\t");
      out.print(data.getColumnName(0));
      for(int j = 1; j < columns; j++) {
         out.print("\t\t");
         out.print(data.getColumnName(j));
      }
      out.print("\n");

      out.print("\t");

      String columnDescription = data.getColumnDescription(0);
      if(columnDescription == null) {
         columnDescription = "";
      }
      out.print(columnDescription);// Line format: (tab) (sample 1 description) (tab) (tab) (sample 2 description) (tab) (tab) ... (sample N description)
      for(int j = 1; j < columns; j++) {
         out.print("\t\t");
         columnDescription = data.getColumnDescription(j);
         if(columnDescription == null) {
            columnDescription = "";
         }
         out.print(columnDescription);
      }

      out.print("\n");
      out.print(rows);

      // Line format: (gene description) (tab) (gene name) (tab) (sample 1 data) (tab) (sample 1 A/P call) (tab) (sample 2 data) (tab) (sample 2 A/P call) (tab) ... (sample N data) (tab) (sample N A/P call)

      for(int i = 0; i < rows; i++) {
         out.print("\n");
         String rowDescription = data.getRowDescription(i);
         if(rowDescription == null) {
            rowDescription = "";
         }
         out.print(rowDescription);
         out.print("\t");
         out.print(data.getRowName(i));
         for(int j = 0; j < columns; j++) {
            out.print("\t");
            out.print(data.getValueAsString(i, j));
            out.print("\t");
            String call = expressionData.getCallAsString(i, j);
            out.print(call);
         }
      }
      out.flush();
      out.close();
   }


   public String getFormatName() {
      return FORMAT_NAME;
   }

}

